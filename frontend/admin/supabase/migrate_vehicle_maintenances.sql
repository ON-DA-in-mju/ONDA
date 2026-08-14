-- 차량·정비 관리: mock 필드(vehicles / maintenances)를 DB로 저장
-- Supabase Dashboard → SQL Editor에서 실행
--
-- vehicles: 차량명, 번호판, 상태, 주행거리, 다음 정비
-- maintenances: 정비일, 번호판, 항목, 유형, 정비사, 비용, 상태, 메모

create table if not exists public.vehicles (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  plate text not null,
  status text not null default '운행 가능',
  mileage text,
  next_maintenance text,
  bus_id uuid references public.buses(id) on delete set null,
  created_at timestamptz not null default now()
);

alter table public.vehicles add column if not exists name text;
alter table public.vehicles add column if not exists plate text;
alter table public.vehicles add column if not exists status text default '운행 가능';
alter table public.vehicles add column if not exists mileage text;
alter table public.vehicles add column if not exists next_maintenance text;
alter table public.vehicles add column if not exists bus_id uuid;
alter table public.vehicles add column if not exists created_at timestamptz default now();

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'vehicles_bus_id_fkey') then
    alter table public.vehicles
      add constraint vehicles_bus_id_fkey
      foreign key (bus_id) references public.buses(id) on delete set null;
  end if;
end $$;

create unique index if not exists vehicles_plate_uidx on public.vehicles (plate);

update public.vehicles v
set bus_id = b.id
from public.buses b
where v.bus_id is null and b.vehicle_number = v.plate;

insert into public.vehicles (name, plate, status, bus_id)
select
  b.bus_name,
  b.vehicle_number,
  case b.status
    when 'ACTIVE' then '운행 가능'
    when 'MAINTENANCE' then '정비 예정'
    else '운행 불가'
  end,
  b.id
from public.buses b
where b.vehicle_number is not null
  and btrim(b.vehicle_number) <> ''
  and not exists (
    select 1 from public.vehicles v
    where v.plate = b.vehicle_number or v.bus_id = b.id
  );

create table if not exists public.maintenances (
  id uuid primary key default gen_random_uuid(),
  bus_id uuid references public.buses(id) on delete set null,
  vehicle_id uuid references public.vehicles(id) on delete set null,
  maintained_at date not null,
  plate text,
  item text not null,
  type text not null default '정기',
  mechanic text,
  cost integer not null default 0,
  status text not null default '예정',
  memo text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.maintenances add column if not exists bus_id uuid;
alter table public.maintenances add column if not exists vehicle_id uuid;
alter table public.maintenances add column if not exists plate text;
alter table public.maintenances add column if not exists memo text;
alter table public.maintenances add column if not exists updated_at timestamptz default now();
alter table public.maintenances add column if not exists maintained_at date;
alter table public.maintenances add column if not exists item text;
alter table public.maintenances add column if not exists type text;
alter table public.maintenances add column if not exists mechanic text;
alter table public.maintenances add column if not exists cost integer default 0;
alter table public.maintenances add column if not exists status text default '예정';

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'maintenances' and column_name = 'plate'
  ) then
    alter table public.maintenances alter column plate drop not null;
  end if;
end $$;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'maintenances_bus_id_fkey') then
    alter table public.maintenances
      add constraint maintenances_bus_id_fkey
      foreign key (bus_id) references public.buses(id) on delete set null;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'maintenances_vehicle_id_fkey') then
    alter table public.maintenances
      add constraint maintenances_vehicle_id_fkey
      foreign key (vehicle_id) references public.vehicles(id) on delete set null;
  end if;
end $$;

update public.maintenances m
set bus_id = b.id
from public.buses b
where m.bus_id is null and m.plate is not null and b.vehicle_number = m.plate;

update public.maintenances m
set vehicle_id = v.id
from public.vehicles v
where m.vehicle_id is null and m.plate is not null and v.plate = m.plate;

create index if not exists maintenances_vehicle_id_idx on public.maintenances (vehicle_id);
create index if not exists maintenances_bus_id_idx on public.maintenances (bus_id);
create index if not exists maintenances_maintained_at_idx on public.maintenances (maintained_at desc);

comment on table public.vehicles is '차량 관리 mock 필드: name, plate, status, mileage, next_maintenance';
comment on table public.maintenances is '정비 이력. plate/vehicle_id 기준';
comment on column public.maintenances.status is '예정 | 점검중 | 완료';
comment on column public.maintenances.type is '정기 | 수리 | 점검 | 소모품';

alter table public.vehicles enable row level security;
alter table public.maintenances enable row level security;
alter table public.buses enable row level security;

drop policy if exists vehicles_select_auth on public.vehicles;
drop policy if exists vehicles_admin_all on public.vehicles;
create policy vehicles_select_auth on public.vehicles
  for select to authenticated using (true);
create policy vehicles_admin_all on public.vehicles
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists maintenances_select_auth on public.maintenances;
drop policy if exists maintenances_admin_all on public.maintenances;
create policy maintenances_select_auth on public.maintenances
  for select to authenticated using (true);
create policy maintenances_admin_all on public.maintenances
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists buses_admin_insert on public.buses;
drop policy if exists buses_admin_update on public.buses;
create policy buses_admin_insert on public.buses
  for insert to authenticated
  with check (public.is_admin());
create policy buses_admin_update on public.buses
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());

grant select, insert, update, delete on public.vehicles to authenticated;
grant select, insert, update, delete on public.maintenances to authenticated;
grant select, insert, update on public.buses to authenticated;

-- =============================================================================
-- mock 차량 + 정비 이력(금액 포함) + 예정 정비
-- =============================================================================
update public.vehicles v
set
  name = s.name,
  status = s.status,
  mileage = s.mileage,
  next_maintenance = s.next_maintenance::date,
  bus_id = coalesce(v.bus_id, b.id)
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
where v.plate = s.plate;

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
where not exists (select 1 from public.vehicles v where v.plate = s.plate);

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
    ('2026-07-13', '75버 9900', '타이어 위치 교환', '정기', '박정비', 60000, '예정'),
    ('2026-07-24', '73버 1122', '브레이크 패드 점검', '점검', '이운영', 80000, '예정'),
    ('2026-07-24', '74버 7788', '엔진오일 교환', '정기', '박정비', 120000, '예정'),
    ('2026-07-24', '72버 5678', '타이어 위치 교환', '정기', '박정비', 60000, '예정')
) as s(maintained_at, plate, item, type, mechanic, cost, status)
join public.vehicles v on v.plate = s.plate
where not exists (
  select 1
  from public.maintenances m
  where coalesce(m.plate, '') = s.plate
    and m.maintained_at = s.maintained_at::date
    and m.item = s.item
);

select
  (select count(*) from public.vehicles) as vehicles,
  (select count(*) from public.maintenances) as maintenances,
  'mock 정비 이력·금액 seeded' as note;
