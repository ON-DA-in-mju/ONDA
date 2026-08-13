-- operation_logs: GPS(LOCATION_UPDATED) 등 로그 최대 100건 유지 + 기사/관리자 RLS
-- Supabase SQL Editor에서 1회 실행

create or replace function public.trim_operation_logs(max_rows int default 100)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from public.operation_logs ol
  using (
    select id
    from public.operation_logs
    order by created_at desc nulls last, id desc
    offset greatest(coalesce(max_rows, 100), 0)
  ) old
  where ol.id = old.id;
end;
$$;

revoke all on function public.trim_operation_logs(int) from public;
grant execute on function public.trim_operation_logs(int) to authenticated;

create or replace function public.trg_trim_operation_logs()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.trim_operation_logs(100);
  return null;
end;
$$;

drop trigger if exists operation_logs_trim_ai on public.operation_logs;
create trigger operation_logs_trim_ai
  after insert on public.operation_logs
  for each statement
  execute function public.trg_trim_operation_logs();

-- 기존 초과분 정리
select public.trim_operation_logs(100);

alter table public.operation_logs enable row level security;

drop policy if exists operation_logs_admin_all on public.operation_logs;
create policy operation_logs_admin_all on public.operation_logs
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists operation_logs_driver_insert on public.operation_logs;
create policy operation_logs_driver_insert on public.operation_logs
  for insert to authenticated
  with check (
    exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
  );

drop policy if exists operation_logs_driver_select on public.operation_logs;
create policy operation_logs_driver_select on public.operation_logs
  for select to authenticated
  using (
    exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
  );

drop policy if exists operation_logs_driver_delete on public.operation_logs;
create policy operation_logs_driver_delete on public.operation_logs
  for delete to authenticated
  using (
    exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
    or public.is_admin()
  );

-- vehicle_locations: 기사 GPS insert / 관리자·기사 조회
alter table public.vehicle_locations enable row level security;

drop policy if exists vehicle_locations_admin_all on public.vehicle_locations;
create policy vehicle_locations_admin_all on public.vehicle_locations
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists vehicle_locations_driver_insert on public.vehicle_locations;
create policy vehicle_locations_driver_insert on public.vehicle_locations
  for insert to authenticated
  with check (
    exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
  );

drop policy if exists vehicle_locations_select on public.vehicle_locations;
create policy vehicle_locations_select on public.vehicle_locations
  for select to authenticated
  using (
    public.is_admin()
    or exists (
      select 1
      from public.operations o
      where o.id = operation_id
        and o.driver_id = auth.uid()
    )
  );
