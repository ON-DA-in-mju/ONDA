-- Fix: users RLS infinite recursion
-- Cause: policies on users/operations that SELECT users under RLS again
-- Run once in Supabase Dashboard → SQL Editor (postgres role)
--
-- 주의: is_admin() 은 buses/routes/notices 등 정책이 의존하므로 DROP 하지 말 것.
--       반환 타입이 boolean 그대로면 CREATE OR REPLACE 만으로 충분.

-- 0) 반환 타입이 바뀌는 함수만 제거 (current_user_role: 기존 타입 ≠ text)
drop function if exists public.current_user_role();

-- 1) SECURITY DEFINER helper (bypasses RLS when reading own role)
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

-- is_admin: DROP 금지 → 본문만 교체 (의존 정책 유지)
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.users
    where id = auth.uid() and role = 'ADMIN'
  )
$$;

revoke all on function public.is_admin() from public;
grant execute on function public.is_admin() to authenticated, anon;

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

-- 2) Rebuild users policies without self-recursion
alter table public.users enable row level security;

do $$
declare
  pol record;
begin
  for pol in
    select policyname from pg_policies
    where schemaname = 'public' and tablename = 'users'
  loop
    execute format('drop policy if exists %I on public.users', pol.policyname);
  end loop;
end $$;

-- 본인 행 조회
create policy users_select_self on public.users
  for select to authenticated
  using (id = auth.uid());

-- 관리자는 전체 조회 (helper 사용 → recursion 없음)
create policy users_select_admin on public.users
  for select to authenticated
  using (public.is_admin());

-- 본인 수정 (login_id 등)
create policy users_update_self on public.users
  for update to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

-- 관리자 전체 수정/삽입
create policy users_write_admin on public.users
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- 3) Rebuild operations policies with is_admin()
alter table public.operations enable row level security;

drop policy if exists operations_admin_all on public.operations;
drop policy if exists operations_driver_select on public.operations;
drop policy if exists operations_driver_update on public.operations;

create policy operations_admin_all on public.operations
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

create policy operations_driver_select on public.operations
  for select to authenticated
  using (driver_id = auth.uid() or public.is_admin());

create policy operations_driver_update on public.operations
  for update to authenticated
  using (driver_id = auth.uid())
  with check (driver_id = auth.uid());

-- 학생 앱: 실시간 운행 현황 조회 (SELECT only) — is_student() 로 재귀 방지
drop policy if exists operations_student_select on public.operations;
create policy operations_student_select on public.operations
  for select to authenticated
  using (public.is_student());

select 'RLS fix applied' as note;
