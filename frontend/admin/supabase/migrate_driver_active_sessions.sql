-- driver_active_sessions: 기사 계정당 활성 기기 1대 (마지막 로그인 기기만 유효)
-- Supabase Dashboard → SQL Editor에서 실행 (postgres)
--
-- 기사 앱: 로그인 시 upsert. 다른 기기는 2초 폴링으로 감지 → GPS 중단 + 강제 로그아웃.

-- =============================================================================
-- 1) 테이블
-- =============================================================================
create table if not exists public.driver_active_sessions (
  driver_id uuid primary key references public.users (id) on delete cascade,
  device_id text not null,
  claimed_at timestamptz not null default now()
);

comment on table public.driver_active_sessions is
  '기사 계정당 활성 앱 기기 1행. 마지막 로그인이 세션을 가져간다';
comment on column public.driver_active_sessions.driver_id is
  'users.id = auth.uid()';
comment on column public.driver_active_sessions.device_id is
  '기사 앱 설치당 UUID';
comment on column public.driver_active_sessions.claimed_at is
  '이 기기가 세션을 가져온 시각';

create or replace function public.touch_driver_active_sessions()
returns trigger
language plpgsql
as $$
begin
  new.claimed_at := now();
  return new;
end;
$$;

drop trigger if exists trg_touch_driver_active_sessions on public.driver_active_sessions;
create trigger trg_touch_driver_active_sessions
  before insert or update on public.driver_active_sessions
  for each row
  execute function public.touch_driver_active_sessions();

-- =============================================================================
-- 2) 헬퍼 (없으면 생성)
-- =============================================================================
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

create or replace function public.is_driver()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.users
    where id = auth.uid() and role = 'DRIVER'
  )
$$;

revoke all on function public.is_admin() from public;
revoke all on function public.is_driver() from public;
grant execute on function public.is_admin() to authenticated, anon;
grant execute on function public.is_driver() to authenticated, anon;

-- =============================================================================
-- 3) RLS
-- =============================================================================
alter table public.driver_active_sessions enable row level security;

drop policy if exists das_admin_all on public.driver_active_sessions;
drop policy if exists das_driver_select on public.driver_active_sessions;
drop policy if exists das_driver_insert on public.driver_active_sessions;
drop policy if exists das_driver_update on public.driver_active_sessions;

create policy das_admin_all on public.driver_active_sessions
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

create policy das_driver_select on public.driver_active_sessions
  for select to authenticated
  using (driver_id = auth.uid() and public.is_driver());

create policy das_driver_insert on public.driver_active_sessions
  for insert to authenticated
  with check (driver_id = auth.uid() and public.is_driver());

create policy das_driver_update on public.driver_active_sessions
  for update to authenticated
  using (driver_id = auth.uid() and public.is_driver())
  with check (driver_id = auth.uid() and public.is_driver());

grant select, insert, update on public.driver_active_sessions to authenticated;

-- =============================================================================
-- 4) 확인
-- =============================================================================
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema = 'public' and table_name = 'driver_active_sessions'
order by ordinal_position;

select policyname, cmd
from pg_policies
where schemaname = 'public' and tablename = 'driver_active_sessions'
order by policyname;

select 'driver_active_sessions ready' as note;
