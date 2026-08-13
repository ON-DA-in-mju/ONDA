# Supabase 직접 연동 (관리자 웹)

## 실제 DB 테이블 (사용)
- `users` (role: STUDENT | DRIVER | ADMIN)
- `notices` + `notice_audiences` (대상은 별도 테이블 — 1NF)
- `reports` (user_id, operation_id?, route_id?)
- `routes`, `stops`, `route_stops`, `schedules`
- `buses`, `operations`, `vehicle_locations`
- `notifications`, `operation_logs`
- (선택) `vehicles`, `maintenances`, `system_logs`

## 환경 변수
`frontend/admin/.env.local`

```env
VITE_SUPABASE_URL=https://xxxx.supabase.co
VITE_SUPABASE_ANON_KEY=sb_publishable_...
```

## 정규화 목표: 3NF

| 형태 | 내용 |
|------|------|
| 1NF | 원자값. `notices.audience[]` → `notice_audiences(notice_id, audience)` |
| 2NF | 복합키 부분종속 없음 (PK는 단일 UUID 또는 명시적 복합 PK) |
| 3NF | 비키 속성이 다른 비키에 종속되지 않음 |

### 제거한 전이종속 (저장 금지)
| 삭제 컬럼 | 단일 진실 |
|-----------|-----------|
| `operations.origin` / `destination` | `origin_stop_id` / `destination_stop_id` → `stops` |
| `routes.start_location` / `end_location` | `route_stops` 첫·끝 순서 |
| `vehicles.plate` | `bus_id` → `buses.vehicle_number` |
| `maintenances.plate` | `bus_id` → `buses.vehicle_number` |
| `system_logs.actor` | `actor_id` → `users.name` |

표시용 이름은 **뷰**에서만 조인: `v_operations`, `v_routes`, `v_vehicles`, `v_maintenances`, `v_system_logs`, `v_notices`.

## RDB 관계 (FK)

```
auth.users ──< users
routes ──< schedules ──< operations ──< vehicle_locations
  │                         │  │
  │                         │  └──< operation_logs
  │                         ├──> users (driver_id)
  │                         ├──> buses (bus_id)
  │                         └──> stops (origin_stop_id / destination_stop_id)
  └──< route_stops >── stops

notices ──< notice_audiences
users ──< notices (author_id)
users ──< reports ──> operations? / routes?
buses ──< vehicles? / maintenances?
```

## SQL (순서)
1. `supabase/migrate_profiles_to_users.sql`
2. `supabase/rls_admin_read.sql`
3. `supabase/migrate_rdb_normalize.sql` — FK·유니크·stop_id 컬럼
4. `supabase/migrate_3nf.sql` — 전이종속 컬럼 제거 + 뷰 + notice_audiences

## 확인
1. SQL Editor에서 `migrate_3nf.sql` 실행
2. 결과의 “남아 있는 전이종속 후보” 쿼리가 **0행**
3. `v_operations` / `v_routes` select로 표시명 확인
4. 신규 시드·API는 텍스트 origin이 아니라 `*_stop_id` 사용
