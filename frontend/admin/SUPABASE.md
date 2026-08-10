# Supabase 직접 연동 (관리자 웹)

실제 DB 스키마 기준입니다. (`users.role = STUDENT | DRIVER | ADMIN`)

## 환경 변수
`frontend/admin/.env.local`

```env
VITE_SUPABASE_URL=https://xxxx.supabase.co
VITE_SUPABASE_ANON_KEY=sb_publishable_...   # 또는 eyJ... anon JWT
```

※ `service_role` 키는 프론트에 넣지 마세요.

## 핵심 테이블
| 테이블 | 용도 |
|--------|------|
| `users` | Auth 프로필 (id = auth.users.id) |
| `notices` | 공지 (title, content, author_id) |
| `reports` | 제보 (title, content, status) |
| `routes` / `stops` / `route_stops` / `schedules` | 노선·정류장·시간표 |
| `buses` / `operations` / `vehicle_locations` | 차량·운행·GPS |
| `notifications` / `operation_logs` | 알림·운행 로그 |
| `vehicles` / `maintenances` / `system_logs` | 정비·시스템 (부가) |

## Auth
- 로그인: Supabase Auth → **`public.users`** (profiles 테이블 없음)
- 역할: `STUDENT` | `DRIVER` | `ADMIN`
- 관리자 웹 로그인 허용: `ADMIN`, `DRIVER`

## 노선·시간표 시드 (2026 mju_pier_ 공지)
이미 운영 DB에 **schedules 700건** 반영됨 (기흥역·명지대역·시내).

재반영이 필요하면:
1. `node scripts/seed-mju-schedules.mjs`
2. 또는 SQL Editor에서 `supabase/seed_mju_2026.sql`

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
| 오늘 배차 | user01 3건 · user02 3건 (`external_id` = op-0905 …) |
| user03~05 | 배정 0건 (빈 화면 시나리오) |

status DB enum: `SCHEDULED` → `IN_PROGRESS` → `COMPLETED` / `CANCELLED`  
(기사 UI의 waiting·departing_soon 은 출발시각 기준 파생)

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
