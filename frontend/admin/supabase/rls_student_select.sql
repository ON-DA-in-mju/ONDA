-- Student app: authenticated STUDENT can SELECT live ops / master data
-- Run in Supabase Dashboard → SQL Editor (postgres role)
-- Write (INSERT/UPDATE/DELETE) 권한은 부여하지 않음.
--
-- Depends on: public.current_user_role() / public.is_admin()
--   (없으면 먼저 fix_rls_recursion.sql 실행)

-- =============================================================================
-- 0) Helper: is_student() — users RLS 재귀 방지 (SECURITY DEFINER)
-- =============================================================================
create or replace function public.is_student()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.users
    where id = auth.uid() and role = 'STUDENT'
  )
$$;

revoke all on function public.is_student() from public;
grant execute on function public.is_student() to authenticated, anon;

-- current_user_role 이 없는 환경 대비 (있으면 경우 본문만 교체)
create or replace function public.current_user_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select role::text from public.users where id = auth.uid() limit 1
$$;

revoke all on function public.current_user_role() from public;
grant execute on function public.current_user_role() to authenticated, anon;

-- =============================================================================
-- 1) operations — STUDENT SELECT only
-- =============================================================================
alter table public.operations enable row level security;

drop policy if exists operations_student_select on public.operations;
create policy operations_student_select on public.operations
  for select to authenticated
  using (public.is_student());

-- =============================================================================
-- 2) schedules / routes / buses / vehicle_locations — STUDENT SELECT
--    (기존 admin/driver 정책은 유지, 학생용 SELECT만 추가)
-- =============================================================================
alter table public.schedules enable row level security;
alter table public.routes enable row level security;
alter table public.buses enable row level security;
alter table public.vehicle_locations enable row level security;

drop policy if exists schedules_student_select on public.schedules;
create policy schedules_student_select on public.schedules
  for select to authenticated
  using (public.is_student());

drop policy if exists routes_student_select on public.routes;
create policy routes_student_select on public.routes
  for select to authenticated
  using (public.is_student());

drop policy if exists buses_student_select on public.buses;
create policy buses_student_select on public.buses
  for select to authenticated
  using (public.is_student());

drop policy if exists vehicle_locations_student_select on public.vehicle_locations;
create policy vehicle_locations_student_select on public.vehicle_locations
  for select to authenticated
  using (public.is_student());

do $$
begin
  if exists (
    select 1 from information_schema.tables
    where table_schema = 'public' and table_name = 'operation_stop_progress'
  ) then
    execute 'alter table public.operation_stop_progress enable row level security';
    execute 'drop policy if exists osp_student_select on public.operation_stop_progress';
    execute $p$
      create policy osp_student_select on public.operation_stop_progress
        for select to authenticated
        using (public.is_student())
    $p$;
  end if;
end $$;

-- =============================================================================
-- 3) 확인용 (SQL Editor에서 결과 확인)
-- =============================================================================
select schemaname, tablename, policyname, cmd, roles
from pg_policies
where schemaname = 'public'
  and tablename in ('operations', 'schedules', 'routes', 'buses', 'vehicle_locations', 'operation_stop_progress')
  and policyname like '%student%'
order by tablename, policyname;

select 'student SELECT RLS applied' as note;
