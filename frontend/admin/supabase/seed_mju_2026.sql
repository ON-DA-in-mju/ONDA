-- 2026 명지대 자연캠 통학·셔틀 (mju_pier_ 공지 이미지 기반)
-- Supabase SQL Editor에서 실행하세요.
-- users / profiles 무관. routes · stops · schedules 시드.

-- 1) 노선 — 이름 기준 upsert (기존 행 있으면)
insert into public.routes (route_name, direction, description, is_active, start_location, end_location)
select v.route_name, v.direction, v.description, true, v.start_location, v.end_location
from (
  values
    ('기흥역 통학버스', '기흥역 ↔ 캠퍼스', '학기 중 평일만 운행. 주말·공휴일·계절학기·방학 미운행.', '캠퍼스', '기흥역 5번 출구'),
    ('명지대역 셔틀', '명지대역', '버스관리사무소 → 이마트·상공회의소 → 역북동 → 명지대역 → … → 제3공학관. 18시 이후 명진당까지.', '버스관리사무소', '제3공학관'),
    ('시내 셔틀', '시내', '버스관리사무소 → 시내 순환 → 제3공학관. 주말·방학은 생활관 기점.', '버스관리사무소', '제3공학관')
) as v(route_name, direction, description, start_location, end_location)
where not exists (
  select 1 from public.routes r where r.route_name = v.route_name
);

update public.routes r
set description = v.description,
    direction = v.direction,
    start_location = v.start_location,
    end_location = v.end_location,
    is_active = true,
    updated_at = now()
from (
  values
    ('기흥역 통학버스', '기흥역 ↔ 캠퍼스', '학기 중 평일만 운행. 주말·공휴일·계절학기·방학 미운행.', '캠퍼스', '기흥역 5번 출구'),
    ('명지대역 셔틀', '명지대역', '버스관리사무소 → 이마트·상공회의소 → 역북동 → 명지대역 → … → 제3공학관.', '버스관리사무소', '제3공학관'),
    ('시내 셔틀', '시내', '시내 순환 셔틀. 주말·방학은 생활관(명현관) 기점.', '버스관리사무소', '제3공학관')
) as v(route_name, direction, description, start_location, end_location)
where r.route_name = v.route_name;

-- 2) 주요 정류장
insert into public.stops (id, stop_name, latitude, longitude)
values
  ('22222222-2222-2222-2222-222222222201', '기흥역 5번 출구', 37.2754, 127.1159),
  ('22222222-2222-2222-2222-222222222202', '버스관리사무소', 37.2245, 127.1878),
  ('22222222-2222-2222-2222-222222222203', '이마트·상공회의소 앞', 37.2301, 127.1889),
  ('22222222-2222-2222-2222-222222222204', '역북동행정복지센터', 37.2335, 127.1895),
  ('22222222-2222-2222-2222-222222222205', '명지대역', 37.2381, 127.1905),
  ('22222222-2222-2222-2222-222222222206', '용인 CGV', 37.2348, 127.2092),
  ('22222222-2222-2222-2222-222222222207', '중앙공영주차장', 37.2340, 127.2060),
  ('22222222-2222-2222-2222-222222222208', '명진당', 37.2228, 127.1875),
  ('22222222-2222-2222-2222-222222222209', '제3공학관', 37.2215, 127.1868),
  ('22222222-2222-2222-2222-222222222210', '생활관(명현관)', 37.2205, 127.1855)
on conflict (id) do update set
  stop_name = excluded.stop_name,
  latitude = excluded.latitude,
  longitude = excluded.longitude,
  updated_at = now();

-- 3) 기존 시드 스케줄 삭제 후 재삽입 (위 3노선만)
delete from public.schedules
where route_id in (
  select id from public.routes
  where route_name in ('기흥역 통학버스', '명지대역 셔틀', '시내 셔틀')
);

-- 헬퍼: 평일 × 시각 insert
-- 기흥역 SEMESTER 평일
with r as (
  select id from public.routes where route_name = '기흥역 통학버스' limit 1
), times as (
  select unnest(array[
    '08:15','08:25','09:05','09:10','10:00','10:05','12:00','13:00','14:00','15:15','16:15','17:15','18:15','19:15'
  ]::time[]) as t
), days as (
  select unnest(array['MON','TUE','WED','THU','FRI']::public.weekday[]) as d
)
insert into public.schedules (route_id, departure_time, weekday, semester)
select r.id, times.t, days.d, 'SEMESTER'::public.semester_type
from r, times, days;

-- 명지대역 SEMESTER 평일
with r as (
  select id from public.routes where route_name = '명지대역 셔틀' limit 1
), times as (
  select unnest(array[
    '08:00','08:15','08:20','08:25','08:35','08:45','08:50','09:00','09:15','09:25','09:30','09:35','09:40','09:55',
    '10:00','10:20','10:30','10:40','10:45','11:00','11:25','11:30','11:45','11:55','12:05','12:20','12:30','12:45',
    '13:00','13:25','13:40','14:00','14:10','14:15','14:30','14:50','15:00','15:10','15:25','15:30','15:55',
    '16:10','16:25','16:30','16:50','17:00','17:10','17:20','17:30','17:45','18:00','19:00','19:20','19:30'
  ]::time[]) as t
), days as (
  select unnest(array['MON','TUE','WED','THU','FRI']::public.weekday[]) as d
)
insert into public.schedules (route_id, departure_time, weekday, semester)
select r.id, times.t, days.d, 'SEMESTER'::public.semester_type
from r, times, days;

-- 시내 SEMESTER 평일
with r as (
  select id from public.routes where route_name = '시내 셔틀' limit 1
), times as (
  select unnest(array[
    '08:05','08:55','10:10','11:20','13:10','14:20','15:40','16:35','18:10','20:00'
  ]::time[]) as t
), days as (
  select unnest(array['MON','TUE','WED','THU','FRI']::public.weekday[]) as d
)
insert into public.schedules (route_id, departure_time, weekday, semester)
select r.id, times.t, days.d, 'SEMESTER'::public.semester_type
from r, times, days;

-- 시내 학기중 주말
with r as (
  select id from public.routes where route_name = '시내 셔틀' limit 1
), times as (
  select unnest(array[
    '08:20','09:20','10:20','11:20','12:20','13:20','15:20','16:20','17:20','18:00'
  ]::time[]) as t
), days as (
  select unnest(array['SAT','SUN']::public.weekday[]) as d
)
insert into public.schedules (route_id, departure_time, weekday, semester)
select r.id, times.t, days.d, 'SEMESTER'::public.semester_type
from r, times, days;

-- 명지대역 VACATION(계절학기) 평일
with r as (
  select id from public.routes where route_name = '명지대역 셔틀' limit 1
), times as (
  select unnest(array[
    '08:00','08:15','08:20','08:25','08:35','08:45','08:50','09:00','09:15','09:25','09:35','09:40','09:55',
    '10:00','10:20','10:40','10:45','11:00','11:25','11:45','11:55','12:05','12:20','12:45','13:00',
    '13:40','14:00','14:10','14:15','14:50','15:00','15:10','15:25','15:55','16:10','16:25','16:50',
    '17:00','17:10','17:20','17:30','17:45','18:00'
  ]::time[]) as t
), days as (
  select unnest(array['MON','TUE','WED','THU','FRI']::public.weekday[]) as d
)
insert into public.schedules (route_id, departure_time, weekday, semester)
select r.id, times.t, days.d, 'VACATION'::public.semester_type
from r, times, days;

-- 시내 VACATION 평일+주말 (계절학기·방학)
with r as (
  select id from public.routes where route_name = '시내 셔틀' limit 1
), times as (
  select unnest(array[
    '08:05','08:55','10:10','11:20','13:10','14:20','15:40','16:35','18:10','19:00','20:00'
  ]::time[]) as t
), days as (
  select unnest(array['MON','TUE','WED','THU','FRI']::public.weekday[]) as d
)
insert into public.schedules (route_id, departure_time, weekday, semester)
select r.id, times.t, days.d, 'VACATION'::public.semester_type
from r, times, days;

with r as (
  select id from public.routes where route_name = '시내 셔틀' limit 1
), times as (
  select unnest(array[
    '08:20','09:20','10:20','11:20','12:20','13:20','15:20','16:20','17:20','18:00'
  ]::time[]) as t
), days as (
  select unnest(array['SAT','SUN']::public.weekday[]) as d
)
insert into public.schedules (route_id, departure_time, weekday, semester)
select r.id, times.t, days.d, 'VACATION'::public.semester_type
from r, times, days;

-- 확인
select r.route_name, s.semester, s.weekday, count(*) as trips
from public.schedules s
join public.routes r on r.id = s.route_id
where r.route_name in ('기흥역 통학버스', '명지대역 셔틀', '시내 셔틀')
group by 1, 2, 3
order by 1, 2, 3;
