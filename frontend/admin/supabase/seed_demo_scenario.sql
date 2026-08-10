-- ON-DA 시연 mock → DB (기존 테이블명 유지, 필요 컬럼·행만 추가)
-- Supabase SQL Editor에서 실행하세요. (Auth users + RLS + operations 포함)
-- 기준: 관리자 Vite mock / 기사 MockUsers·MockTodayOperations

-- =============================================================================
-- 0) 필요 컬럼 (없으면 추가) — 테이블명 변경 없음
-- =============================================================================
alter table public.users
  add column if not exists login_id text;

create unique index if not exists users_login_id_uidx
  on public.users (login_id)
  where login_id is not null;

comment on column public.users.login_id is '기사 앱 mock id (user01~user05). Auth email과 별도.';

alter table public.operations
  add column if not exists external_id text,
  add column if not exists round integer default 1,
  add column if not exists origin text,
  add column if not exists destination text,
  add column if not exists expected_end_time time;

create unique index if not exists operations_external_id_uidx
  on public.operations (external_id)
  where external_id is not null;

comment on column public.operations.external_id is '로컬 mock 배차 id (op-0905, d02-op-0840 …)';

-- =============================================================================
-- 1) ADMIN이 operations 쓸 수 있도록 RLS (정책명 충돌 시 drop 후 재생성)
-- =============================================================================
alter table public.operations enable row level security;

drop policy if exists operations_admin_all on public.operations;
create policy operations_admin_all on public.operations
  for all to authenticated
  using (
    exists (
      select 1 from public.users u
      where u.id = auth.uid() and u.role = 'ADMIN'
    )
  )
  with check (
    exists (
      select 1 from public.users u
      where u.id = auth.uid() and u.role = 'ADMIN'
    )
  );

drop policy if exists operations_driver_select on public.operations;
create policy operations_driver_select on public.operations
  for select to authenticated
  using (
    driver_id = auth.uid()
    or exists (
      select 1 from public.users u
      where u.id = auth.uid() and u.role = 'ADMIN'
    )
  );

drop policy if exists operations_driver_update on public.operations;
create policy operations_driver_update on public.operations
  for update to authenticated
  using (driver_id = auth.uid())
  with check (driver_id = auth.uid());

-- =============================================================================
-- 2) 정류장 (mock stops + 배차 origin/destination)
-- =============================================================================
insert into public.stops (id, stop_name, latitude, longitude)
values
  ('22222222-2222-2222-2222-222222222201', '기흥역 5번 출구', 37.2754, 127.1159),
  ('22222222-2222-2222-2222-222222222205', '명지대역', 37.2381, 127.1905),
  ('22222222-2222-2222-2222-222222222211', '채플관 앞', 37.2240, 127.1872),
  ('22222222-2222-2222-2222-222222222212', '학생회관', 37.2225, 127.1888),
  ('22222222-2222-2222-2222-222222222213', '용인시청', 37.2342, 127.2095),
  ('22222222-2222-2222-2222-222222222214', '자연캠퍼스', 37.2248, 127.1870)
on conflict (id) do update set
  stop_name = excluded.stop_name,
  latitude = excluded.latitude,
  longitude = excluded.longitude,
  updated_at = now();

-- =============================================================================
-- 3) 차량 buses (배차 표기 N호차 통일) + vehicles 부가 표
-- =============================================================================
insert into public.buses (id, bus_name, vehicle_number, capacity, status)
values
  ('33333333-3333-3333-3333-333333333301', '1호차', '72버 1234', 45, 'ACTIVE'),
  ('33333333-3333-3333-3333-333333333302', '2호차', '73버 1122', 45, 'MAINTENANCE'),
  ('33333333-3333-3333-3333-333333333303', '3호차', '72버 5678', 45, 'ACTIVE'),
  ('33333333-3333-3333-3333-333333333304', '4호차', '75버 9900', 45, 'INACTIVE')
on conflict (id) do update set
  bus_name = excluded.bus_name,
  vehicle_number = excluded.vehicle_number,
  capacity = excluded.capacity,
  status = excluded.status,
  updated_at = now();

insert into public.vehicles (id, name, plate, status, mileage, next_maintenance)
values
  ('55555555-5555-5555-5555-555555555501', '온다 1호기', '72버 1234', '운행 중', '84,220km', '2026.08.20'),
  ('55555555-5555-5555-5555-555555555502', '온다 2호기', '73버 1122', '정비 예정', '91,040km', '2026.08.08'),
  ('55555555-5555-5555-5555-555555555503', '온다 3호기', '72버 5678', '운행 중', '67,510km', '2026.09.01'),
  ('55555555-5555-5555-5555-555555555504', '온다 6호기', '75버 9900', '통신 이상', '102,300km', '2026.08.07')
on conflict (id) do update set
  name = excluded.name,
  plate = excluded.plate,
  status = excluded.status,
  mileage = excluded.mileage,
  next_maintenance = excluded.next_maintenance;

-- =============================================================================
-- 4) 시연 배차용 스케줄 시각 (공식 시간표에 없던 시각 보강)
--    08:40 기흥 / 11:10 명지대 / 12:00 시내
-- =============================================================================
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
    select 1
    from public.schedules s
    where s.route_id = r.id
      and s.departure_time = t.departure_time::time
      and s.weekday = d.weekday::public.weekday
      and s.semester = 'SEMESTER'
  );

-- =============================================================================
-- 5) Auth + public.users (시연 계정)
--    비밀번호: Auth 최소 6자 → 123456 (기사 mock 1234 대체)
--    이미 있으면 이메일은 재사용하고 login_id/role만 맞춤
-- =============================================================================
create extension if not exists pgcrypto;

do $$
declare
  v record;
  uid uuid;
begin
  for v in
    select * from (
      values
        ('admin@mju.ac.kr', 'Admin1234!', '관리자', 'ADMIN', 'admin', null::text),
        ('operator1@mju.ac.kr', '123456', '김운영', 'ADMIN', 'operator1', '010-2000-0001'),
        ('operator2@mju.ac.kr', '123456', '이운영', 'ADMIN', 'operator2', '010-2000-0002'),
        ('user01@mju.ac.kr', '123456', '박사용', 'DRIVER', 'user01', '010-1111-2222'),
        ('user02@mju.ac.kr', '123456', '최사용', 'DRIVER', 'user02', '010-3333-4444'),
        ('user03@mju.ac.kr', '123456', '정사용', 'DRIVER', 'user03', '010-5555-6666'),
        ('user04@mju.ac.kr', '123456', '한사용', 'DRIVER', 'user04', '010-7777-8888'),
        ('user05@mju.ac.kr', '123456', '임사용', 'DRIVER', 'user05', '010-9999-0000')
    ) as x(email, pwd, name, role, login_id, phone)
  loop
    select id into uid from auth.users where email = v.email limit 1;
    if uid is null then
      uid := gen_random_uuid();
      insert into auth.users (
        instance_id, id, aud, role, email, encrypted_password,
        email_confirmed_at, raw_app_meta_data, raw_user_meta_data,
        created_at, updated_at, confirmation_token, recovery_token,
        email_change_token_new, email_change
      ) values (
        coalesce((select id from auth.instances limit 1), '00000000-0000-0000-0000-000000000000'),
        uid,
        'authenticated',
        'authenticated',
        v.email,
        crypt(v.pwd, gen_salt('bf')),
        now(),
        '{"provider":"email","providers":["email"]}'::jsonb,
        jsonb_build_object('name', v.name, 'role', v.role, 'login_id', v.login_id),
        now(), now(), '', '', '', ''
      );

      begin
        insert into auth.identities (
          id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
        ) values (
          gen_random_uuid(), uid,
          jsonb_build_object('sub', uid::text, 'email', v.email),
          'email', uid::text, now(), now(), now()
        )
        on conflict do nothing;
      exception when others then
        raise notice 'auth.identities skip %: %', v.email, sqlerrm;
      end;
    end if;

    insert into public.users (id, name, email, role, phone, login_id, student_no)
    values (uid, v.name, v.email, v.role::public.user_role, v.phone, v.login_id, v.login_id)
    on conflict (id) do update set
      name = excluded.name,
      email = excluded.email,
      role = excluded.role,
      phone = coalesce(excluded.phone, public.users.phone),
      login_id = excluded.login_id,
      student_no = coalesce(excluded.student_no, public.users.student_no),
      updated_at = now();
  end loop;

  -- API 시드로 student_no만 채워진 기존 행 보정
  update public.users
  set login_id = student_no
  where login_id is null
    and student_no in ('admin', 'operator1', 'operator2', 'user01', 'user02', 'user03', 'user04', 'user05');
end $$;

-- =============================================================================
-- 6) 오늘 배차 템플릿 6건 → operations (user01 3 + user02 3)
--    날짜가 바뀌면 이 섹션을 다시 실행하거나 Node 스크립트로 롤오버
-- =============================================================================
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
-- 시연 템플릿은 요일 무관 → 주말이면 MON 스케줄에 연결
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
  -- 안정적 UUID (external_id 해시 대신 고정 네임스페이스)
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

-- =============================================================================
-- 7) 안전 정차 요청 테이블 (Vite mock 대응, 없으면 생성)
-- =============================================================================
create table if not exists public.safe_stop_requests (
  id uuid primary key default gen_random_uuid(),
  operation_id uuid references public.operations (id) on delete cascade,
  driver_id uuid not null references public.users (id),
  reason text not null,
  detail_reason text,
  decision text not null default 'pending'
    check (decision in ('pending', 'continue', 'stop', 'cancelled')),
  requested_at timestamptz not null default now(),
  decided_at timestamptz,
  created_at timestamptz not null default now()
);

alter table public.safe_stop_requests enable row level security;

drop policy if exists safe_stop_admin_all on public.safe_stop_requests;
create policy safe_stop_admin_all on public.safe_stop_requests
  for all to authenticated
  using (
    exists (select 1 from public.users u where u.id = auth.uid() and u.role = 'ADMIN')
  )
  with check (
    exists (select 1 from public.users u where u.id = auth.uid() and u.role = 'ADMIN')
  );

drop policy if exists safe_stop_driver_rw on public.safe_stop_requests;
create policy safe_stop_driver_rw on public.safe_stop_requests
  for all to authenticated
  using (driver_id = auth.uid())
  with check (driver_id = auth.uid());

-- =============================================================================
-- 확인
-- =============================================================================
select u.login_id, u.email, u.name, u.role
from public.users u
where u.login_id is not null
order by u.login_id;

select o.external_id, o.operation_date, o.status, u.login_id as driver, b.bus_name, o.origin, o.destination
from public.operations o
join public.users u on u.id = o.driver_id
join public.buses b on b.id = o.bus_id
where o.operation_date = current_date
order by o.external_id;
