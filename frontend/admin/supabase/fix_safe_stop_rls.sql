-- safe_stop_requests: ADMIN 판별을 is_admin()으로 (users RLS 재귀 방지)
-- SQL Editor에서 1회 실행 (이미 is_admin() 함수가 있어야 함)

alter table public.safe_stop_requests enable row level security;

drop policy if exists safe_stop_admin_all on public.safe_stop_requests;
create policy safe_stop_admin_all on public.safe_stop_requests
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists safe_stop_driver_rw on public.safe_stop_requests;
create policy safe_stop_driver_rw on public.safe_stop_requests
  for all to authenticated
  using (driver_id = auth.uid())
  with check (driver_id = auth.uid());
