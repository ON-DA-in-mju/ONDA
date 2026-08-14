-- Driver/Student: stops · route_stops SELECT
-- Supabase SQL Editor에서 1회 실행 (없으면 정류장 노선 보기가 catalog fallback만 씀)

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

revoke all on function public.is_driver() from public;
grant execute on function public.is_driver() to authenticated, anon;

alter table public.stops enable row level security;
alter table public.route_stops enable row level security;

drop policy if exists stops_driver_select on public.stops;
create policy stops_driver_select on public.stops
  for select to authenticated
  using (public.is_driver() or public.is_admin() or public.is_student());

drop policy if exists route_stops_driver_select on public.route_stops;
create policy route_stops_driver_select on public.route_stops
  for select to authenticated
  using (public.is_driver() or public.is_admin() or public.is_student());

select 'stops / route_stops SELECT for driver·student·admin applied' as note;
