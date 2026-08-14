-- 차량·정비 관리 원래 mock 데이터
-- migrate_vehicle_maintenances.sql 실행 후, SQL Editor에서 실행
-- 반복 실행해도 같은 건은 다시 넣지 않습니다.

-- =============================================================================
-- 1) 차량 (mock vehicles + 정비 이력에만 있던 번호판)
-- =============================================================================
insert into public.vehicles (name, plate, status, mileage, next_maintenance, bus_id)
select s.name, s.plate, s.status, s.mileage, s.next_maintenance::date, b.id
from (
  values
    ('온다 1호기', '72버 1234', '운행 중', '84,220km', '2026-08-20'),
    ('온다 2호기', '73버 1122', '정비 예정', '91,040km', '2026-08-08'),
    ('온다 3호기', '72버 5678', '운행 중', '67,510km', '2026-09-01'),
    ('온다 4호기', '74버 7788', '운행 가능', '78,400km', '2026-08-24'),
    ('온다 5호기', '73버 3344', '운행 가능', '55,210km', '2026-09-10'),
    ('온다 6호기', '75버 9900', '통신 이상', '102,300km', '2026-08-07')
) as s(name, plate, status, mileage, next_maintenance)
left join public.buses b on b.vehicle_number = s.plate
on conflict (plate) do update set
  name = excluded.name,
  status = excluded.status,
  mileage = excluded.mileage,
  next_maintenance = excluded.next_maintenance::date,
  bus_id = coalesce(public.vehicles.bus_id, excluded.bus_id);

-- =============================================================================
-- 2) 정비 이력 (mock maintenances)
-- =============================================================================
insert into public.maintenances (
  vehicle_id, bus_id, plate, maintained_at, item, type, mechanic, cost, status, created_at
)
select
  v.id,
  v.bus_id,
  s.plate,
  s.maintained_at::date,
  s.item,
  s.type,
  s.mechanic,
  s.cost,
  s.status,
  (s.maintained_at || ' 09:00:00+09')::timestamptz
from (
  values
    ('2026-07-20', '72버 1234', '엔진오일 교환', '정기', '김기사', 120000, '완료'),
    ('2026-07-19', '73버 1122', '브레이크 패드 교체', '수리', '이운영', 450000, '완료'),
    ('2026-07-18', '72버 5678', '타이어 교체', '수리', '박정비', 300000, '완료'),
    ('2026-07-17', '75버 9900', '에어컨 필터 교체', '정기', '김기사', 50000, '예정'),
    ('2026-07-16', '74버 7788', '배터리 점검', '점검', '박정비', 30000, '점검중'),
    ('2026-07-15', '73버 3344', '냉각수 보충', '정기', '이운영', 20000, '완료'),
    ('2026-07-14', '72버 1234', '차량 하부 점검', '점검', '김기사', 40000, '완료'),
    ('2026-07-13', '75버 9900', '타이어 위치 교환', '정기', '박정비', 60000, '예정')
) as s(maintained_at, plate, item, type, mechanic, cost, status)
join public.vehicles v on v.plate = s.plate
where not exists (
  select 1
  from public.maintenances m
  where m.plate = s.plate
    and m.maintained_at = s.maintained_at::date
    and m.item = s.item
);

-- =============================================================================
-- 3) 예정 정비 (원래 화면 하드코딩 3건)
-- =============================================================================
insert into public.maintenances (
  vehicle_id, bus_id, plate, maintained_at, item, type, mechanic, cost, status, created_at
)
select
  v.id,
  v.bus_id,
  s.plate,
  s.maintained_at::date,
  s.item,
  s.type,
  s.mechanic,
  s.cost,
  '예정',
  now()
from (
  values
    ('2026-07-24', '73버 1122', '브레이크 패드 점검', '점검', '이운영', 80000),
    ('2026-07-24', '74버 7788', '엔진오일 교환', '정기', '박정비', 120000),
    ('2026-07-24', '72버 5678', '타이어 위치 교환', '정기', '박정비', 60000)
) as s(maintained_at, plate, item, type, mechanic, cost)
join public.vehicles v on v.plate = s.plate
where not exists (
  select 1
  from public.maintenances m
  where m.plate = s.plate
    and m.maintained_at = s.maintained_at::date
    and m.item = s.item
);

select
  (select count(*) from public.vehicles) as vehicles,
  (select count(*) from public.maintenances) as maintenances;
