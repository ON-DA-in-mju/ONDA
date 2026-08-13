-- system_logs RLS 정리 (선택): 중복 정책 제거 + insert/select 명확화
-- Supabase SQL Editor에서 실행

grant select, insert on public.system_logs to authenticated;

drop policy if exists system_logs_insert_auth on public.system_logs;
drop policy if exists system_logs_select_auth on public.system_logs;
drop policy if exists system_logs_admin_select on public.system_logs;
drop policy if exists system_logs_authenticated_insert on public.system_logs;

create policy system_logs_admin_select on public.system_logs
  for select to authenticated
  using (public.is_admin());

create policy system_logs_authenticated_insert on public.system_logs
  for insert to authenticated
  with check (true);
