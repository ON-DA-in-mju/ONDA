-- Seed today's operations (6 demo trips) AFTER running fix_rls_recursion.sql is OK
-- Prefer: 1) fix_rls_recursion.sql  2) this file  3) node scripts/seed-demo-scenario.mjs
-- Run in Supabase SQL Editor

-- Ensure demo columns exist
alter table public.operations
  add column if not exists external_id text,
  add column if not exists round integer default 1,
  add column if not exists origin text,
  add column if not exists destination text,
  add column if not exists expected_end_time time;

create unique index if not exists operations_external_id_uidx
  on public.operations (external_id)
  where external_id is not null;

alter table public.users
  add column if not exists login_id text;

create unique index if not exists users_login_id_uidx
  on public.users (login_id)
  where login_id is not null;

-- Extra schedule times used by demo ops
insert into public.schedules (route_id, departure_time, weekday, semester)
select r.id, t.departure_time::time, d.weekday::public.weekday, 'SEMESTER'::public.semester_type
from public.routes r
cross join (
  values
    ('기흥역 통학버스', '08:40:00'),
    ('명지대역 셔틀', '11:10:00'),
    ('시내 셔틀', '12:00:00')
) as t(route_name, departure_time)
cross join (
  values ('MON'), ('TUE'), ('WED'), ('THU'), ('FRI')
) as d(weekday)
where r.route_name = t.route_name
  and not exists (
    select 1 from public.schedules s
    where s.route_id = r.id
      and s.departure_time = t.departure_time::time
      and s.weekday = d.weekday::public.weekday
      and s.semester = 'SEMESTER'
  );

with tpl as (
  select * from (
    values
      ('op-0905', 'user01@mju.ac.kr', '기흥역 통학버스', '09:05:00', '2호차', '09:25:00', '채플관 앞', '기흥역 5번 출구', 1),
      ('op-1000', 'user01@mju.ac.kr', '명지대역 셔틀', '10:00:00', '1호차', '10:25:00', '자연캠퍼스', '명지대역', 1),
      ('op-1200', 'user01@mju.ac.kr', '시내 셔틀', '12:00:00', '3호차', '12:40:00', '채플관 앞', '용인시청', 1),
      ('d02-op-0840', 'user02@mju.ac.kr', '기흥역 통학버스', '08:40:00', '1호차', '09:10:00', '채플관 앞', '기흥역 5번 출구', 1),
      ('d02-op-1110', 'user02@mju.ac.kr', '명지대역 셔틀', '11:10:00', '1호차', '11:40:00', '자연캠퍼스', '명지대역', 1),
      ('d02-op-1420', 'user02@mju.ac.kr', '시내 셔틀', '14:20:00', '4호차', '15:00:00', '채플관 앞', '용인시청', 1)
  ) as t(external_id, email, route_name, depart, bus_name, end_t, origin, destination, round)
),
wd as (
  select (
    case
      when extract(isodow from current_date)::int between 1 and 5 then
        case extract(isodow from current_date)::int
          when 1 then 'MON' when 2 then 'TUE' when 3 then 'WED'
          when 4 then 'THU' else 'FRI'
        end
      else 'MON'
    end
  )::public.weekday as weekday
),
resolved as (
  select
    t.external_id,
    u.id as driver_id,
    b.id as bus_id,
    s.id as schedule_id,
    t.end_t::time as expected_end_time,
    t.origin,
    t.destination,
    t.round
  from tpl t
  join public.users u on u.email = t.email
  join public.buses b on b.bus_name = t.bus_name
  join public.routes r on r.route_name = t.route_name
  join wd on true
  join public.schedules s
    on s.route_id = r.id
   and s.departure_time = t.depart::time
   and s.weekday = wd.weekday
   and s.semester = 'SEMESTER'
)
insert into public.operations (
  id, schedule_id, driver_id, bus_id, operation_date, status,
  external_id, round, origin, destination, expected_end_time
)
select
  case r.external_id
    when 'op-0905' then '44444444-4444-4444-4444-444444440001'::uuid
    when 'op-1000' then '44444444-4444-4444-4444-444444440002'::uuid
    when 'op-1200' then '44444444-4444-4444-4444-444444440003'::uuid
    when 'd02-op-0840' then '44444444-4444-4444-4444-444444440004'::uuid
    when 'd02-op-1110' then '44444444-4444-4444-4444-444444440005'::uuid
    when 'd02-op-1420' then '44444444-4444-4444-4444-444444440006'::uuid
  end,
  r.schedule_id,
  r.driver_id,
  r.bus_id,
  current_date,
  'SCHEDULED'::public.operation_status,
  r.external_id,
  r.round,
  r.origin,
  r.destination,
  r.expected_end_time
from resolved r
on conflict (id) do update set
  schedule_id = excluded.schedule_id,
  driver_id = excluded.driver_id,
  bus_id = excluded.bus_id,
  operation_date = excluded.operation_date,
  status = 'SCHEDULED'::public.operation_status,
  external_id = excluded.external_id,
  round = excluded.round,
  origin = excluded.origin,
  destination = excluded.destination,
  expected_end_time = excluded.expected_end_time,
  started_at = null,
  ended_at = null,
  updated_at = now();

select o.external_id, o.operation_date, u.email, b.bus_name, o.origin, o.destination
from public.operations o
join public.users u on u.id = o.driver_id
join public.buses b on b.id = o.bus_id
where o.operation_date = current_date
order by o.external_id;
