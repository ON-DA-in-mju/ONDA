-- notices / reports / routes / buses 읽기·쓰기 (authenticated)
-- SQL Editor에서 실행

alter table public.notices enable row level security;
alter table public.reports enable row level security;
alter table public.routes enable row level security;
alter table public.buses enable row level security;

drop policy if exists "notices_select_auth" on public.notices;
drop policy if exists "notices_insert_auth" on public.notices;
create policy "notices_select_auth" on public.notices
  for select to authenticated using (true);
create policy "notices_insert_auth" on public.notices
  for insert to authenticated with check (true);

drop policy if exists "reports_select_auth" on public.reports;
create policy "reports_select_auth" on public.reports
  for select to authenticated using (true);

drop policy if exists "routes_select_auth" on public.routes;
create policy "routes_select_auth" on public.routes
  for select to authenticated using (true);

drop policy if exists "buses_select_auth" on public.buses;
create policy "buses_select_auth" on public.buses
  for select to authenticated using (true);
  