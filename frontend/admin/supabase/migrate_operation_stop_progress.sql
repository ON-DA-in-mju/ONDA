-- operation_stop_progress: 운행 중 정류장 진행 스냅샷 (운행당 1행)
-- operations(배차)와 분리. vehicle_locations / operation_device_status 와 같은 실시간 테이블.
-- Supabase Dashboard → SQL Editor에서 실행 (postgres)
--
-- last_*_index: 순환 노선에서 같은 stop_id 가 두 번 나올 수 있어 복원 키.
-- last_*_stop_id: 표시·FK 용. 기사 앱이 덮어씀, 학생/관리자는 SELECT.

-- =============================================================================
-- 1) 테이블
-- =============================================================================
create table if not exists public.operation_stop_progress (
  operation_id uuid primary key references public.operations (id) on delete cascade,
  last_arrived_stop_id uuid references public.stops (id) on delete set null,
  last_passed_stop_id uuid references public.stops (id) on delete set null,
  last_arrived_index integer not null default -1,
  last_passed_index integer not null default -1,
  updated_at timestamptz not null default now()
);

comment on table public.operation_stop_progress is
  '운행당 최신 정류장 진행 1행. 기사 앱 upsert, 학생 앱 화면 복원';
comment on column public.operation_stop_progress.last_arrived_stop_id is
  '마지막으로 도착 인식한 정류장. 아직이면 null';
comment on column public.operation_stop_progress.last_passed_stop_id is
  '지나감이 확정된 정류장. 아직이면 null';
comment on column public.operation_stop_progress.last_arrived_index is
  '노선 순서 기준 도착 인덱스. -1=미도착';
comment on column public.operation_stop_progress.last_passed_index is
  '노선 순서 기준 지나감 인덱스. -1=아직 없음';
comment on column public.operation_stop_progress.updated_at is
  '진행 갱신 시각';

create index if not exists operation_stop_progress_updated_at_idx
  on public.operation_stop_progress (updated_at desc);

create or replace function public.touch_operation_stop_progress()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists trg_touch_operation_stop_progress on public.operation_stop_progress;
create trigger trg_touch_operation_stop_progress
  before insert or update on public.operation_stop_progress
  for each row
  execute function public.touch_operation_stop_progress();

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
-- 3) RLS
-- =============================================================================
alter table public.operation_stop_progress enable row level security;

drop policy if exists osp_admin_all on public.operation_stop_progress;
drop policy if exists osp_student_select on public.operation_stop_progress;
drop policy if exists osp_driver_select on public.operation_stop_progress;
drop policy if exists osp_driver_insert on public.operation_stop_progress;
drop policy if exists osp_driver_update on public.operation_stop_progress;

create policy osp_admin_all on public.operation_stop_progress
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

create policy osp_student_select on public.operation_stop_progress
  for select to authenticated
  using (public.is_student());

create policy osp_driver_select on public.operation_stop_progress
  for select to authenticated
  using (
    exists (
      select 1 from public.operations o
      where o.id = operation_id and o.driver_id = auth.uid()
    )
  );

create policy osp_driver_insert on public.operation_stop_progress
  for insert to authenticated
  with check (
    exists (
      select 1 from public.operations o
      where o.id = operation_id and o.driver_id = auth.uid()
    )
  );

create policy osp_driver_update on public.operation_stop_progress
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

grant select, insert, update on public.operation_stop_progress to authenticated;

-- =============================================================================
-- 4) Realtime
-- =============================================================================
do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'operation_stop_progress'
  ) then
    execute 'alter publication supabase_realtime add table public.operation_stop_progress';
  end if;
end $$;

-- =============================================================================
-- 5) 확인
-- =============================================================================
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema = 'public' and table_name = 'operation_stop_progress'
order by ordinal_position;

select policyname, cmd
from pg_policies
where schemaname = 'public' and tablename = 'operation_stop_progress'
order by policyname;

select pubname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'operation_stop_progress';

select 'operation_stop_progress ready' as note;
