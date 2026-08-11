-- =============================================================================
-- 3정규형(3NF) 정리
-- 선행: migrate_rdb_normalize.sql (FK·stop_id 컬럼)
-- Supabase Dashboard → SQL Editor (postgres)에서 실행
--
-- 제거하는 전이종속 / 중복:
--   operations.origin, destination          ← stops.stop_name (via *_stop_id)
--   routes.start_location, end_location     ← route_stops 첫/끝 정류장
--   vehicles.plate                          ← buses.vehicle_number (via bus_id)
--   maintenances.plate                      ← buses.vehicle_number (via bus_id)
--   system_logs.actor                       ← users.name (via actor_id)
--   notices.audience[]                      ← notice_audiences (1NF 다중값 해소)
--
-- 앱 호환 읽기 모델: v_operations, v_routes, v_vehicles, v_maintenances, v_notices
-- =============================================================================

create or replace function public._table_exists(p_name text)
returns boolean language sql stable as $$
  select exists (
    select 1 from information_schema.tables
    where table_schema = 'public' and table_name = p_name
  );
$$;

create or replace function public._column_exists(p_table text, p_column text)
returns boolean language sql stable as $$
  select exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = p_table and column_name = p_column
  );
$$;

-- =============================================================================
-- 1) operations: 텍스트 → stop FK 백필 강화, 텍스트 컬럼 DROP
-- =============================================================================
alter table public.operations add column if not exists origin_stop_id uuid;
alter table public.operations add column if not exists destination_stop_id uuid;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'operations_origin_stop_id_fkey') then
    alter table public.operations
      add constraint operations_origin_stop_id_fkey
      foreign key (origin_stop_id) references public.stops(id);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'operations_destination_stop_id_fkey') then
    alter table public.operations
      add constraint operations_destination_stop_id_fkey
      foreign key (destination_stop_id) references public.stops(id);
  end if;
end $$;

-- 이름 매칭 백필
do $$
begin
  if public._column_exists('operations', 'origin') then
    update public.operations o
    set origin_stop_id = s.id
    from public.stops s
    where o.origin_stop_id is null and o.origin is not null and s.stop_name = o.origin;
  end if;
  if public._column_exists('operations', 'destination') then
    update public.operations o
    set destination_stop_id = s.id
    from public.stops s
    where o.destination_stop_id is null and o.destination is not null and s.stop_name = o.destination;
  end if;
end $$;

-- 여전히 NULL이면 schedule → route → route_stops 첫/끝으로 보정
update public.operations o
set origin_stop_id = rs.stop_id
from public.schedules sch
join lateral (
  select stop_id from public.route_stops
  where route_id = sch.route_id
  order by stop_order asc
  limit 1
) rs on true
where o.schedule_id = sch.id
  and o.origin_stop_id is null;

update public.operations o
set destination_stop_id = rs.stop_id
from public.schedules sch
join lateral (
  select stop_id from public.route_stops
  where route_id = sch.route_id
  order by stop_order desc
  limit 1
) rs on true
where o.schedule_id = sch.id
  and o.destination_stop_id is null;

alter table public.operations drop column if exists origin;
alter table public.operations drop column if exists destination;

comment on column public.operations.origin_stop_id is '3NF: 출발 정류장 (이름은 stops 조인)';
comment on column public.operations.destination_stop_id is '3NF: 도착 정류장 (이름은 stops 조인)';

-- =============================================================================
-- 2) routes: start/end 텍스트 DROP (route_stops가 단일 진실)
-- =============================================================================
-- 텍스트만 있고 route_stops가 비어 있으면 정류장·연결 보강
do $$
declare
  r record;
  sid uuid;
  eid uuid;
begin
  if not (public._column_exists('routes', 'start_location') and public._column_exists('routes', 'end_location')) then
    return;
  end if;

  for r in
    select id, start_location, end_location from public.routes
    where start_location is not null or end_location is not null
  loop
    if not exists (select 1 from public.route_stops where route_id = r.id) then
      if r.start_location is not null then
        select id into sid from public.stops where stop_name = r.start_location limit 1;
        if sid is null then
          insert into public.stops (stop_name, latitude, longitude)
          values (r.start_location, 0, 0) returning id into sid;
        end if;
        if not exists (
          select 1 from public.route_stops where route_id = r.id and stop_order = 1
        ) then
          insert into public.route_stops (route_id, stop_id, stop_order)
          values (r.id, sid, 1);
        end if;
      end if;
      if r.end_location is not null and r.end_location is distinct from r.start_location then
        select id into eid from public.stops where stop_name = r.end_location limit 1;
        if eid is null then
          insert into public.stops (stop_name, latitude, longitude)
          values (r.end_location, 0, 0) returning id into eid;
        end if;
        if not exists (
          select 1 from public.route_stops where route_id = r.id and stop_order = 2
        ) then
          insert into public.route_stops (route_id, stop_id, stop_order)
          values (r.id, eid, 2);
        end if;
      elsif r.end_location is not null and r.end_location = r.start_location and sid is not null then
        if not exists (
          select 1 from public.route_stops where route_id = r.id and stop_order = 2
        ) then
          insert into public.route_stops (route_id, stop_id, stop_order)
          values (r.id, sid, 2);
        end if;
      end if;
    end if;
  end loop;
end $$;

alter table public.routes drop column if exists start_location;
alter table public.routes drop column if exists end_location;

-- =============================================================================
-- 3) vehicles / maintenances: plate DROP
-- =============================================================================
do $$
begin
  if public._table_exists('vehicles') then
    if public._column_exists('vehicles', 'bus_id') and public._column_exists('vehicles', 'plate') then
      update public.vehicles v
      set bus_id = b.id
      from public.buses b
      where v.bus_id is null and b.vehicle_number = v.plate;
    end if;
    alter table public.vehicles drop column if exists plate;
  end if;

  if public._table_exists('maintenances') then
    if public._column_exists('maintenances', 'bus_id') and public._column_exists('maintenances', 'plate') then
      update public.maintenances m
      set bus_id = b.id
      from public.buses b
      where m.bus_id is null and m.plate is not null and b.vehicle_number = m.plate;
    end if;
    alter table public.maintenances drop column if exists plate;
  end if;
end $$;

create unique index if not exists buses_vehicle_number_uidx on public.buses (vehicle_number);

-- =============================================================================
-- 4) system_logs: actor 텍스트 DROP
-- =============================================================================
do $$
begin
  if public._table_exists('system_logs') then
    alter table public.system_logs add column if not exists actor_id uuid;
    if not exists (select 1 from pg_constraint where conname = 'system_logs_actor_id_fkey') then
      alter table public.system_logs
        add constraint system_logs_actor_id_fkey
        foreign key (actor_id) references public.users(id) on delete set null;
    end if;
    -- actor 문자열에 이메/이름이 있으면 느슨한 매칭 시도
    if public._column_exists('system_logs', 'actor') then
      update public.system_logs sl
      set actor_id = u.id
      from public.users u
      where sl.actor_id is null
        and sl.actor is not null
        and (sl.actor = u.name or sl.actor like u.name || '%');
      alter table public.system_logs drop column if exists actor;
    end if;
  end if;
end $$;

-- =============================================================================
-- 5) notices.audience[] → notice_audiences (1NF)
-- =============================================================================
create table if not exists public.notice_audiences (
  notice_id uuid not null references public.notices(id) on delete cascade,
  audience text not null check (audience in ('STUDENT', 'DRIVER', 'ADMIN')),
  primary key (notice_id, audience)
);

create index if not exists notice_audiences_audience_idx on public.notice_audiences (audience);

alter table public.notice_audiences enable row level security;

do $$
begin
  if not exists (
    select 1 from pg_policies where tablename = 'notice_audiences' and policyname = 'notice_audiences_select'
  ) then
    create policy notice_audiences_select on public.notice_audiences
      for select to authenticated using (true);
  end if;
  if not exists (
    select 1 from pg_policies where tablename = 'notice_audiences' and policyname = 'notice_audiences_admin_write'
  ) then
    create policy notice_audiences_admin_write on public.notice_audiences
      for all to authenticated
      using (exists (select 1 from public.users u where u.id = auth.uid() and u.role::text = 'ADMIN'))
      with check (exists (select 1 from public.users u where u.id = auth.uid() and u.role::text = 'ADMIN'));
  end if;
end $$;

do $$
begin
  if public._column_exists('notices', 'audience') then
    insert into public.notice_audiences (notice_id, audience)
    select n.id, a
    from public.notices n
    cross join lateral unnest(coalesce(n.audience, array['STUDENT']::text[])) as a
    where a in ('STUDENT', 'DRIVER', 'ADMIN')
    on conflict do nothing;

    -- audience 컬럼을 참조하는 RLS 정책 먼저 제거
    drop policy if exists notices_student_select on public.notices;
    drop policy if exists notices_driver_select on public.notices;

    alter table public.notices drop column if exists audience;
  end if;
end $$;

-- 대상은 notice_audiences 조인으로 판별 (3NF)
drop policy if exists notices_student_select on public.notices;
drop policy if exists notices_driver_select on public.notices;

create policy notices_student_select on public.notices
  for select to authenticated
  using (
    exists (
      select 1 from public.users u
      where u.id = auth.uid() and u.role::text = 'STUDENT'
    )
    and exists (
      select 1 from public.notice_audiences na
      where na.notice_id = notices.id and na.audience = 'STUDENT'
    )
    and (
      coalesce(notices.status, 'PUBLISHED') = 'PUBLISHED'
      or notices.status is null
    )
    and (notices.starts_at is null or notices.starts_at <= now())
    and (notices.ends_at is null or notices.ends_at >= now())
  );

create policy notices_driver_select on public.notices
  for select to authenticated
  using (
    exists (
      select 1 from public.users u
      where u.id = auth.uid() and u.role::text = 'DRIVER'
    )
    and exists (
      select 1 from public.notice_audiences na
      where na.notice_id = notices.id and na.audience = 'DRIVER'
    )
    and (
      coalesce(notices.status, 'PUBLISHED') = 'PUBLISHED'
      or notices.status is null
    )
    and (notices.starts_at is null or notices.starts_at <= now())
    and (notices.ends_at is null or notices.ends_at >= now())
  );

-- =============================================================================
-- 6) 읽기 전용 뷰 (표시용 조인 — 저장은 정규화 유지)
-- =============================================================================
create or replace view public.v_operations as
select
  o.*,
  os.stop_name as origin,
  ds.stop_name as destination
from public.operations o
left join public.stops os on os.id = o.origin_stop_id
left join public.stops ds on ds.id = o.destination_stop_id;

create or replace view public.v_routes as
select
  r.*,
  fs.stop_name as start_location,
  ls.stop_name as end_location
from public.routes r
left join lateral (
  select s.stop_name
  from public.route_stops rs
  join public.stops s on s.id = rs.stop_id
  where rs.route_id = r.id
  order by rs.stop_order asc
  limit 1
) fs on true
left join lateral (
  select s.stop_name
  from public.route_stops rs
  join public.stops s on s.id = rs.stop_id
  where rs.route_id = r.id
  order by rs.stop_order desc
  limit 1
) ls on true;

do $$
begin
  if public._table_exists('vehicles') then
    execute $v$
      create or replace view public.v_vehicles as
      select
        v.*,
        b.vehicle_number as plate,
        b.bus_name
      from public.vehicles v
      left join public.buses b on b.id = v.bus_id
    $v$;
  end if;
  if public._table_exists('maintenances') then
    execute $v$
      create or replace view public.v_maintenances as
      select
        m.*,
        b.vehicle_number as plate,
        b.bus_name
      from public.maintenances m
      left join public.buses b on b.id = m.bus_id
    $v$;
  end if;
  if public._table_exists('system_logs') then
    execute $v$
      create or replace view public.v_system_logs as
      select
        sl.*,
        u.name as actor
      from public.system_logs sl
      left join public.users u on u.id = sl.actor_id
    $v$;
  end if;
end $$;

create or replace view public.v_notices as
select
  n.*,
  coalesce(
    (select array_agg(na.audience order by na.audience)
     from public.notice_audiences na
     where na.notice_id = n.id),
    array[]::text[]
  ) as audience
from public.notices n;

grant select on public.v_operations to anon, authenticated;
grant select on public.v_routes to anon, authenticated;
grant select on public.v_notices to anon, authenticated;

do $$
begin
  if exists (select 1 from information_schema.views where table_schema='public' and table_name='v_vehicles') then
    grant select on public.v_vehicles to anon, authenticated;
  end if;
  if exists (select 1 from information_schema.views where table_schema='public' and table_name='v_maintenances') then
    grant select on public.v_maintenances to anon, authenticated;
  end if;
  if exists (select 1 from information_schema.views where table_schema='public' and table_name='v_system_logs') then
    grant select on public.v_system_logs to anon, authenticated;
  end if;
end $$;

-- =============================================================================
-- 7) 정리
-- =============================================================================
drop function if exists public._table_exists(text);
drop function if exists public._column_exists(text, text);

select '3nf migration applied' as note;

-- 검증: 남아 있는 전이종속 후보 컬럼이 없어야 함
select table_name, column_name
from information_schema.columns
where table_schema = 'public'
  and (
    (table_name = 'operations' and column_name in ('origin', 'destination'))
    or (table_name = 'routes' and column_name in ('start_location', 'end_location'))
    or (table_name = 'vehicles' and column_name = 'plate')
    or (table_name = 'maintenances' and column_name = 'plate')
    or (table_name = 'system_logs' and column_name = 'actor')
    or (table_name = 'notices' and column_name = 'audience')
  );
