-- RDB 관계 강화 + 정규화 (안전 재실행 가능)
-- Supabase Dashboard → SQL Editor (postgres)에서 실행
--
-- 현황: 핵심 FK는 이미 존재. 이 스크립트는 누락 보완·CASCADE·유니크·stop FK 컬럼 추가.
-- 3NF(전이종속 텍스트 컬럼 제거)는 migrate_3nf.sql 에서 수행.

-- =============================================================================
-- 0) 헬퍼
-- =============================================================================
create or replace function public._ensure_fk(
  p_name text,
  p_sql text
) returns void
language plpgsql
as $$
begin
  if not exists (select 1 from pg_constraint where conname = p_name) then
    execute p_sql;
  end if;
end;
$$;

create or replace function public._table_exists(p_name text)
returns boolean
language sql
stable
as $$
  select exists (
    select 1 from information_schema.tables
    where table_schema = 'public' and table_name = p_name
  );
$$;

-- =============================================================================
-- 1) 핵심 FK 보장 (없으면 추가)
-- =============================================================================
select public._ensure_fk('schedules_route_id_fkey',
  'alter table public.schedules add constraint schedules_route_id_fkey foreign key (route_id) references public.routes(id)');

select public._ensure_fk('route_stops_route_id_fkey',
  'alter table public.route_stops add constraint route_stops_route_id_fkey foreign key (route_id) references public.routes(id) on delete cascade');

select public._ensure_fk('route_stops_stop_id_fkey',
  'alter table public.route_stops add constraint route_stops_stop_id_fkey foreign key (stop_id) references public.stops(id)');

select public._ensure_fk('operations_schedule_id_fkey',
  'alter table public.operations add constraint operations_schedule_id_fkey foreign key (schedule_id) references public.schedules(id)');

select public._ensure_fk('operations_driver_id_fkey',
  'alter table public.operations add constraint operations_driver_id_fkey foreign key (driver_id) references public.users(id)');

select public._ensure_fk('operations_bus_id_fkey',
  'alter table public.operations add constraint operations_bus_id_fkey foreign key (bus_id) references public.buses(id)');

select public._ensure_fk('vehicle_locations_operation_id_fkey',
  'alter table public.vehicle_locations add constraint vehicle_locations_operation_id_fkey foreign key (operation_id) references public.operations(id) on delete cascade');

select public._ensure_fk('operation_logs_operation_id_fkey',
  'alter table public.operation_logs add constraint operation_logs_operation_id_fkey foreign key (operation_id) references public.operations(id) on delete cascade');

select public._ensure_fk('notices_author_id_fkey',
  'alter table public.notices add constraint notices_author_id_fkey foreign key (author_id) references public.users(id)');

select public._ensure_fk('reports_user_id_fkey',
  'alter table public.reports add constraint reports_user_id_fkey foreign key (user_id) references public.users(id)');

select public._ensure_fk('notifications_user_id_fkey',
  'alter table public.notifications add constraint notifications_user_id_fkey foreign key (user_id) references public.users(id)');

-- =============================================================================
-- 2) CASCADE 강화 (자식 행은 운행 삭제 시 함께 제거)
-- =============================================================================
do $$
begin
  if exists (select 1 from pg_constraint where conname = 'vehicle_locations_operation_id_fkey') then
    alter table public.vehicle_locations drop constraint vehicle_locations_operation_id_fkey;
  end if;
  alter table public.vehicle_locations
    add constraint vehicle_locations_operation_id_fkey
    foreign key (operation_id) references public.operations(id) on delete cascade;

  if exists (select 1 from pg_constraint where conname = 'operation_logs_operation_id_fkey') then
    alter table public.operation_logs drop constraint operation_logs_operation_id_fkey;
  end if;
  alter table public.operation_logs
    add constraint operation_logs_operation_id_fkey
    foreign key (operation_id) references public.operations(id) on delete cascade;
end $$;

-- =============================================================================
-- 3) 유니크 (중복 방지)
-- =============================================================================
create unique index if not exists schedules_route_time_day_sem_uidx
  on public.schedules (route_id, departure_time, weekday, semester);

create unique index if not exists route_stops_route_order_uidx
  on public.route_stops (route_id, stop_order);

create unique index if not exists operations_external_id_uidx
  on public.operations (external_id)
  where external_id is not null;

-- =============================================================================
-- 4) operations: 정류장 FK 컬럼 (텍스트 origin/destination 은 migrate_3nf 에서 제거)
-- =============================================================================
alter table public.operations add column if not exists origin text;
alter table public.operations add column if not exists destination text;
alter table public.operations add column if not exists origin_stop_id uuid;
alter table public.operations add column if not exists destination_stop_id uuid;
alter table public.operations add column if not exists external_id text;

select public._ensure_fk('operations_origin_stop_id_fkey',
  'alter table public.operations add constraint operations_origin_stop_id_fkey foreign key (origin_stop_id) references public.stops(id)');

select public._ensure_fk('operations_destination_stop_id_fkey',
  'alter table public.operations add constraint operations_destination_stop_id_fkey foreign key (destination_stop_id) references public.stops(id)');

update public.operations o
set origin_stop_id = s.id
from public.stops s
where o.origin_stop_id is null and o.origin is not null and s.stop_name = o.origin;

update public.operations o
set destination_stop_id = s.id
from public.stops s
where o.destination_stop_id is null and o.destination is not null and s.stop_name = o.destination;

-- =============================================================================
-- 5) vehicles ↔ buses (테이블 있을 때만)
-- =============================================================================
do $$
begin
  if public._table_exists('vehicles') then
    alter table public.vehicles add column if not exists bus_id uuid;
    perform public._ensure_fk('vehicles_bus_id_fkey',
      'alter table public.vehicles add constraint vehicles_bus_id_fkey foreign key (bus_id) references public.buses(id)');
    update public.vehicles v
    set bus_id = b.id
    from public.buses b
    where v.bus_id is null and b.vehicle_number = v.plate;
    create unique index if not exists vehicles_bus_id_uidx on public.vehicles (bus_id) where bus_id is not null;
    execute $c$comment on column public.vehicles.bus_id is 'buses 1:1 연결 (plate ≈ vehicle_number)'$c$;
  end if;
end $$;

-- =============================================================================
-- 6) maintenances → buses (테이블 있을 때만)
-- =============================================================================
do $$
begin
  if public._table_exists('maintenances') then
    alter table public.maintenances add column if not exists bus_id uuid;
    perform public._ensure_fk('maintenances_bus_id_fkey',
      'alter table public.maintenances add constraint maintenances_bus_id_fkey foreign key (bus_id) references public.buses(id)');
    update public.maintenances m
    set bus_id = b.id
    from public.buses b
    where m.bus_id is null and m.plate is not null and b.vehicle_number = m.plate;
    execute $c$comment on column public.maintenances.bus_id is '정비 대상 버스 FK'$c$;
    execute $c$comment on column public.maintenances.plate is '표시용 번호판 (정규 키: bus_id)'$c$;
  end if;
end $$;

-- =============================================================================
-- 7) reports 문맥 FK
-- =============================================================================
alter table public.reports add column if not exists operation_id uuid;
alter table public.reports add column if not exists route_id uuid;

select public._ensure_fk('reports_operation_id_fkey',
  'alter table public.reports add constraint reports_operation_id_fkey foreign key (operation_id) references public.operations(id) on delete set null');

select public._ensure_fk('reports_route_id_fkey',
  'alter table public.reports add constraint reports_route_id_fkey foreign key (route_id) references public.routes(id) on delete set null');

-- =============================================================================
-- 8) system_logs.actor_id (테이블 있을 때만)
-- =============================================================================
do $$
begin
  if public._table_exists('system_logs') then
    alter table public.system_logs add column if not exists actor_id uuid;
    perform public._ensure_fk('system_logs_actor_id_fkey',
      'alter table public.system_logs add constraint system_logs_actor_id_fkey foreign key (actor_id) references public.users(id) on delete set null');
  end if;
end $$;

-- =============================================================================
-- 9) schedules CHECK (이미 enum이면 스킵)
-- =============================================================================
do $$
declare
  weekday_udt text;
  semester_udt text;
begin
  select udt_name into weekday_udt
  from information_schema.columns
  where table_schema = 'public' and table_name = 'schedules' and column_name = 'weekday';

  select udt_name into semester_udt
  from information_schema.columns
  where table_schema = 'public' and table_name = 'schedules' and column_name = 'semester';

  if weekday_udt in ('text', 'varchar', 'bpchar')
     and not exists (select 1 from pg_constraint where conname = 'schedules_weekday_check') then
    alter table public.schedules
      add constraint schedules_weekday_check
      check (weekday in ('MON','TUE','WED','THU','FRI','SAT','SUN'));
  end if;

  if semester_udt in ('text', 'varchar', 'bpchar')
     and not exists (select 1 from pg_constraint where conname = 'schedules_semester_check') then
    alter table public.schedules
      add constraint schedules_semester_check
      check (semester in ('SEMESTER','VACATION'));
  end if;
end $$;

-- =============================================================================
-- 10) 정리 + 확인
-- =============================================================================
drop function if exists public._ensure_fk(text, text);
drop function if exists public._table_exists(text);

select conrelid::regclass as table_name, conname, pg_get_constraintdef(oid) as def
from pg_constraint
where contype = 'f' and connamespace = 'public'::regnamespace
order by 1, 2;

select 'rdb normalize migration applied' as note;
