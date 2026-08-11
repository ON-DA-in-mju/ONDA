-- operation_device_status: 운행 중 기기 heartbeat (GPS vs 네트워크 구분용)
-- Supabase Dashboard → SQL Editor에서 실행 (postgres)
-- vehicle_locations 와 별개. 네트워크가 살아 있을 때만 upsert 가능 → updated_at 이 생존 신호.

-- =============================================================================
-- 1) 테이블
-- =============================================================================
create table if not exists public.operation_device_status (
  operation_id uuid primary key references public.operations (id) on delete cascade,
  gps_ok boolean not null default false,
  gps_enabled boolean not null default false,
  last_location_at timestamptz,
  last_accuracy real,
  updated_at timestamptz not null default now()
);

comment on table public.operation_device_status is
  '운행당 최신 1행. updated_at=네트워크 생존, gps_ok/gps_enabled=GPS 상태';
comment on column public.operation_device_status.gps_ok is
  '최근 fix 있음 (기사 앱 기준, 예: 15초 이내)';
comment on column public.operation_device_status.gps_enabled is
  '기기 위치 서비스(GPS) on/off';
comment on column public.operation_device_status.last_location_at is
  '마지막 GPS fix 시각';
comment on column public.operation_device_status.updated_at is
  'heartbeat 시각. 오래되면 네트워크/앱 오프라인으로 판정';

create or replace function public.touch_operation_device_status()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists trg_touch_operation_device_status on public.operation_device_status;
create trigger trg_touch_operation_device_status
  before insert or update on public.operation_device_status
  for each row
  execute function public.touch_operation_device_status();

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
revoke all on function public.is_student() from public;
revoke all on function public.is_driver() from public;
grant execute on function public.is_admin() to authenticated, anon;
grant execute on function public.is_student() to authenticated, anon;
grant execute on function public.is_driver() to authenticated, anon;

-- =============================================================================
-- 3) RLS — operation_device_status
-- =============================================================================
alter table public.operation_device_status enable row level security;

drop policy if exists ods_admin_all on public.operation_device_status;
drop policy if exists ods_student_select on public.operation_device_status;
drop policy if exists ods_driver_select on public.operation_device_status;
drop policy if exists ods_driver_insert on public.operation_device_status;
drop policy if exists ods_driver_update on public.operation_device_status;

create policy ods_admin_all on public.operation_device_status
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

create policy ods_student_select on public.operation_device_status
  for select to authenticated
  using (public.is_student());

create policy ods_driver_select on public.operation_device_status
  for select to authenticated
  using (
    exists (
      select 1 from public.operations o
      where o.id = operation_id and o.driver_id = auth.uid()
    )
  );

create policy ods_driver_insert on public.operation_device_status
  for insert to authenticated
  with check (
    exists (
      select 1 from public.operations o
      where o.id = operation_id and o.driver_id = auth.uid()
    )
  );

create policy ods_driver_update on public.operation_device_status
  for update to authenticated
  using (
    exists (
      select 1 from public.operations o
      where o.id = operation_id and o.driver_id = auth.uid()
    )
  )
  with check (
    exists (
      select 1 from public.operations o
      where o.id = operation_id and o.driver_id = auth.uid()
    )
  );

grant select, insert, update on public.operation_device_status to authenticated;

-- =============================================================================
-- 4) vehicle_locations — 기사 INSERT (본인 운행) + 학생 SELECT 보강
-- =============================================================================
alter table public.vehicle_locations enable row level security;

drop policy if exists vehicle_locations_driver_insert on public.vehicle_locations;
create policy vehicle_locations_driver_insert on public.vehicle_locations
  for insert to authenticated
  with check (
    exists (
      select 1 from public.operations o
      where o.id = operation_id and o.driver_id = auth.uid()
    )
  );

drop policy if exists vehicle_locations_student_select on public.vehicle_locations;
create policy vehicle_locations_student_select on public.vehicle_locations
  for select to authenticated
  using (public.is_student());

drop policy if exists vehicle_locations_driver_select on public.vehicle_locations;
create policy vehicle_locations_driver_select on public.vehicle_locations
  for select to authenticated
  using (
    exists (
      select 1 from public.operations o
      where o.id = operation_id and o.driver_id = auth.uid()
    )
    or public.is_admin()
  );

-- =============================================================================
-- 5) Realtime publication
-- =============================================================================
do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'operation_device_status'
  ) then
    execute 'alter publication supabase_realtime add table public.operation_device_status';
  end if;
end $$;

-- =============================================================================
-- 6) 확인
-- =============================================================================
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema = 'public' and table_name = 'operation_device_status'
order by ordinal_position;

select policyname, cmd
from pg_policies
where schemaname = 'public' and tablename = 'operation_device_status'
order by policyname;

select pubname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'operation_device_status';

select 'operation_device_status ready' as note;
