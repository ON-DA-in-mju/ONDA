# Supabase 직접 연동 (관리자 웹)

실제 DB 스키마 기준입니다. (`users.role = STUDENT | DRIVER | ADMIN`)

## 환경 변수
`frontend/admin/.env.local`

```env
VITE_SUPABASE_URL=https://xxxx.supabase.co
VITE_SUPABASE_ANON_KEY=sb_publishable_...   # 또는 eyJ... anon JWT

# 네이버 지도 (대시보드 / 실시간 운행)
# 네이버 클라우드 콘솔 → Maps → Web Dynamic Map 인증키
VITE_NAVER_MAP_CLIENT_ID=YOUR_NAVER_MAP_KEY
# 신규 콘솔: ncpKeyId (기본) / 구 콘솔: ncpClientId
# VITE_NAVER_MAP_KEY_PARAM=ncpKeyId
# 웹 서비스 URL 예: http://localhost:5173

# 자동차 길찾기(Directions 5) — Vite 서버 전용 (브라우저에 노출되지 않음)
# NAVER_MAP_CLIENT_SECRET=YOUR_CLIENT_SECRET
```

※ `service_role` 키는 프론트에 넣지 마세요.

## 핵심 테이블
| 테이블 | 용도 |
|--------|------|
| `users` | Auth 프로필 (id = auth.users.id) |
| `notices` | 공지 (`title`, `content`, `author_id`, **`type`**, **`audience`**, **`starts_at`/`ends_at`**, **`is_push`**, **`status`**) |
| `reports` | 제보/문의 (title, content, status, **source** `STUDENT`\|`DRIVER`, category) |
| `routes` / `stops` / `route_stops` / `schedules` | 노선·정류장·시간표 |
| `buses` / `operations` / `vehicle_locations` | 차량·운행·GPS 좌표 |
| `operation_device_status` | 운행 중 기기 heartbeat (`gps_ok`, `gps_enabled`, `updated_at`) — GPS vs 네트워크 구분 |
| `notifications` / `operation_logs` | 알림·운행 로그 |
| `vehicles` / `maintenances` / `system_logs` | 정비·시스템 (부가) |

## Auth
- 로그인: Supabase Auth → **`public.users`** (profiles 테이블 없음)
- 역할: `STUDENT` | `DRIVER` | `ADMIN`
- 관리자 웹 로그인 허용: `ADMIN`, `DRIVER`

## RLS (학생 앱 SELECT)
학생은 **조회만** 가능. SQL Editor에서 실행:

1. (필요 시) `supabase/fix_rls_recursion.sql` — `is_admin` / `is_student` / users·operations 재귀 수정
2. **`supabase/rls_student_select.sql`** — STUDENT SELECT 추가

| 테이블 | STUDENT | DRIVER | ADMIN |
|--------|---------|--------|-------|
| `operations` | SELECT | SELECT(본인) + UPDATE(본인) | ALL |
| `schedules` | SELECT | (기존 정책) | (기존 정책) |
| `routes` | SELECT | (기존 정책) | (기존 정책) |
| `buses` | SELECT | (기존 정책) | (기존 정책) |
| `vehicle_locations` | SELECT | SELECT(본인 운행) + INSERT(본인) | (기존 정책) |
| `operation_device_status` | SELECT | SELECT/INSERT/UPDATE(본인 운행) | ALL |
| `notices` | SELECT(대상·게시중·기간) | SELECT(대상·게시중·기간) | ALL |

헬퍼: `public.is_student()` / `public.is_driver()` / `public.is_admin()` / `public.current_user_role()` (SECURITY DEFINER)

## GPS vs 네트워크 구분 (`operation_device_status`)
SQL Editor에서 **`supabase/migrate_operation_device_status.sql`** 실행  
(테이블 + RLS + Realtime publication 포함).

| 컬럼 | 의미 |
|------|------|
| `operation_id` | PK, `operations.id` |
| `gps_ok` | 최근 GPS fix 있음 (기사 앱 ~15초 기준) |
| `gps_enabled` | 기기 위치 서비스 on/off |
| `last_location_at` | 마지막 fix 시각 |
| `last_accuracy` | 마지막 fix 정확도(m) |
| `updated_at` | heartbeat 시각 (= 네트워크/앱 생존 신호) |

기사 앱이 운행 중 ~10초마다 upsert. 네트워크가 끊기면 upsert가 멈춰 `updated_at`이 오래진다.

### 학생 앱 판정 규약 (권장 임계값 30초)

| 조건 | 표시 |
|------|------|
| `now - status.updated_at > 30s` (또는 행 없음) | 네트워크 끊김(또는 앱 백그라운드/종료) |
| status 신선 + `gps_ok = false` | GPS 꺼짐/비활성·무수신 |
| status 신선 + `gps_ok = true` + `now - vehicle_locations.recorded_at > 30s` | GPS 수신 불량(위치 미갱신) |
| status·location 둘 다 신선 | 정상 |

Realtime 구독: `operation_device_status` + `vehicle_locations`

## 공지 notices 확장
SQL Editor에서 **`supabase/migrate_notices_fields.sql`** 실행.

| 컬럼 | 값 |
|------|-----|
| `type` | `URGENT` \| `IMPORTANT` \| `OPERATION_CHANGE` \| `GENERAL` |
| `audience` | `text[]` — `STUDENT`, `DRIVER` (다중, 최소 1개) |
| `starts_at` / `ends_at` | `timestamptz`, 상시 게시면 둘 다 `NULL` |
| `is_push` | 푸시 동시 발송 여부 |
| `status` | `DRAFT` \| `SCHEDULED` \| `PUBLISHED` \| `ENDED` |

노선 컬럼 없음. 학생/기사는 `status = PUBLISHED` 이고 대상·게시 기간에 맞는 행만 SELECT.

## Realtime
SQL Editor에서 실행 (RLS/데이터 변경 없음):

| 파일 | 테이블 | 용도 |
|------|--------|------|
| `supabase/realtime_vehicle_locations.sql` | `vehicle_locations` | GPS INSERT 구독 |
| `supabase/realtime_operations.sql` | `operations` | 운행 시작/종료 UPDATE 구독 |
| `supabase/realtime_operation_device_status.sql` | `operation_device_status` | GPS/네트워크 heartbeat 구독 |

확인:
```sql
select * from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename in ('vehicle_locations', 'operations', 'operation_device_status');
```

## 노선·시간표 시드 (2026 mju_pier_ 공지)
이미 운영 DB에 **schedules 700건** 반영됨 (기흥역·명지대역·시내).

재반영이 필요하면:
1. `node scripts/seed-mju-schedules.mjs`
2. 또는 SQL Editor에서 `supabase/seed_mju_2026.sql`
3. **routes / route_stops 공지 구간 정리:** SQL Editor에서 `supabase/migrate_routes_route_stops.sql`
4. **변형 노선 분리(18시 이후·주말/방학):** `node scripts/apply-routes-variants.mjs` 또는 `supabase/migrate_routes_variants.sql`

관리자 웹은 `schedules` + `routes` 를 조회해 공식 시간표 표를 표시합니다.

## 시연 mock 시드 (기사 앱 · 오늘 배차)
로컬 Vite/`MockUsers`/`MockTodayOperations` 시나리오를 DB로 옮긴 것.

1. **SQL Editor**에서 `supabase/seed_demo_scenario.sql` 실행  
   - `users.login_id`, `operations.external_id` 등 컬럼 추가  
   - `operations` / `safe_stop_requests` RLS  
   - Auth 계정 · 1~4호차 · 정류장 · 오늘 배차 6건
2. (선택) `node scripts/seed-demo-scenario.mjs` — 계정·버스·정류장·당일 operations 롤오버

| 구분 | 값 |
|------|-----|
| 기사 로그인 | email `user01@mju.ac.kr` ~ `user05@…` / 비밀번호 `123456` (`login_id` = user01~05) |
| 관리자 | `admin@mju.ac.kr` / `Admin1234!` |
| 학생 테스트 | email `60201234@mju.ac.kr` / 비밀번호 `onda1234` · 학번 `60201234` · role `STUDENT` |
| 오늘 배차 | user01 3건 · user02 3건 (`external_id` = op-0905 …) |
| user03~05 | 배정 0건 (빈 화면 시나리오) |

status DB enum: `SCHEDULED` → `IN_PROGRESS` → `COMPLETED` / `CANCELLED`  
(기사 UI의 waiting·departing_soon 은 출발시각 기준 파생)

## 오늘 배차 앱 연동 (Vite mock 아님)
- 관리자 웹: `src/lib/assignmentsApi.ts` → Supabase `operations`
- 기사 앱: Supabase Auth + REST `operations` (`BuildConfig.SUPABASE_*` from `local.properties`)
- 기사 로그인 예: `user01` / `123456` (이메일은 `user01@mju.ac.kr`로 변환)

## 코드
- `src/types/database.ts` — 스키마 타입
- `src/lib/supabase.ts` — 클라이언트
- `src/lib/api.ts` — notices/users/routes/buses/reports 조회
- `src/state/AuthContext.tsx` — **users** 테이블 연동
- `src/data/mjuTimetable.ts` — 공지 기반 노선·시간표

## 확인
1. 로그인 화면에 **Supabase 연결됨** 표시
2. 회원가입(ADMIN) 후 로그인
3. 공지 관리 → 등록 → Table Editor `notices` 행 확인
4. 노선/사용자/제보 목록에 DB 건수 표시
