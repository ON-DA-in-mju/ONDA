# Supabase 직접 연동 (관리자 웹)

## 실제 DB 테이블 (사용)
- `users` (role: STUDENT | DRIVER | ADMIN)
- `notices` (title, content, author_id)
- `reports` (user_id, title, content, status)
- `routes`, `stops`, `route_stops`, `schedules`
- `buses`, `operations`, `vehicle_locations`
- `notifications`, `operation_logs`

## 환경 변수
`frontend/admin/.env.local`

```env
VITE_SUPABASE_URL=https://xxxx.supabase.co
VITE_SUPABASE_ANON_KEY=sb_publishable_...
```

## SQL (순서)
1. `supabase/migrate_profiles_to_users.sql` — profiles→users, 가입 트리거
2. `supabase/rls_admin_read.sql` — notices/reports/routes/buses 읽기(+공지 등록)

## 확인
1. Authentication → Users 에 계정
2. Table Editor → `users` 에 같은 id 행
3. 공지 관리에서 「공지 등록」 → `notices` 행 추가
4. 제보/사용자/노선/차량 화면에 「Supabase 연결됨」 표시
