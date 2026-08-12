-- operation_device_status: 기사 앱 운행 중 GPS/기기 heartbeat (학생 앱이 GPS vs 네트워크 이상 구분)
-- Run in Supabase Dashboard → SQL Editor
--
-- Upsert key: operation_id (PK)
-- Driver: INSERT/UPDATE own in-progress ops
-- Student/Admin: SELECT

create table if not exists public.operation_device_status (
  operation_id uuid primary key references public.operations (id) on delete cascade,
  gps_ok boolean not null default false,
  gps_enabled boolean not null default false,
  last_location_at timestamptz null,
  last_accuracy double precision null,
  updated_at timestamptz not null default now()
);

create index if not exists operation_device_status_updated_at_idx
  on public.operation_device_status (updated_at desc);

alter table public.operation_device_status enable row level security;

-- Admin full access
drop policy if exists operation_device_status_admin_all on public.operation_device_status;
create policy operation_device_status_admin_all on public.operation_device_status
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- Driver upsert (insert + update) for own operations
drop policy if exists operation_device_status_driver_insert on public.operation_device_status;
create policy operation_device_status_driver_insert on public.operation_device_status
  for insert to authenticated
  with check (
    exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
  );

drop policy if exists operation_device_status_driver_update on public.operation_device_status;
create policy operation_device_status_driver_update on public.operation_device_status
  for update to authenticated
  using (
    exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
  )
  with check (
    exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
  );

-- Driver select own
drop policy if exists operation_device_status_driver_select on public.operation_device_status;
create policy operation_device_status_driver_select on public.operation_device_status
  for select to authenticated
  using (
    public.is_admin()
    or public.is_student()
    or exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
  );

-- Realtime (student subscribe)
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

select 'operation_device_status ready' as note;
