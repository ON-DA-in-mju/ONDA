-- reports.view_count + report_views (유저당 1회 조회)
-- Supabase SQL Editor에서 실행

alter table public.reports
  add column if not exists view_count integer not null default 0;

comment on column public.reports.view_count is
  '고유 조회수 (report_views 행 수). 같은 사용자는 1회만 증가';

create table if not exists public.report_views (
  report_id uuid not null references public.reports (id) on delete cascade,
  user_id uuid not null references public.users (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (report_id, user_id)
);

create index if not exists report_views_user_id_idx
  on public.report_views (user_id);

alter table public.report_views enable row level security;

drop policy if exists "report_views_select_auth" on public.report_views;
create policy "report_views_select_auth" on public.report_views
  for select to authenticated
  using (true);

drop policy if exists "report_views_insert_own" on public.report_views;
create policy "report_views_insert_own" on public.report_views
  for insert to authenticated
  with check (auth.uid() = user_id);

grant select, insert on public.report_views to authenticated;

-- view_count 동기화 트리거
create or replace function public.sync_report_view_count()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if tg_op = 'INSERT' then
    update public.reports
    set view_count = (
      select count(*)::integer from public.report_views v where v.report_id = new.report_id
    )
    where id = new.report_id;
    return new;
  elsif tg_op = 'DELETE' then
    update public.reports
    set view_count = (
      select count(*)::integer from public.report_views v where v.report_id = old.report_id
    )
    where id = old.report_id;
    return old;
  end if;
  return null;
end;
$$;

drop trigger if exists trg_report_views_sync_count on public.report_views;
create trigger trg_report_views_sync_count
  after insert or delete on public.report_views
  for each row execute function public.sync_report_view_count();

-- 기존 행 보정 (있으면면 0 유지)
update public.reports r
set view_count = coalesce((
  select count(*)::integer from public.report_views v where v.report_id = r.id
), 0);

-- 조회 기록 RPC: 이미 본 사용자는 증가 없이 현재 카운트 반환
create or replace function public.record_report_view(p_report_id uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  new_count integer;
begin
  if uid is null or p_report_id is null then
    return coalesce((select view_count from public.reports where id = p_report_id), 0);
  end if;

  insert into public.report_views (report_id, user_id)
  values (p_report_id, uid)
  on conflict (report_id, user_id) do nothing;

  select view_count into new_count from public.reports where id = p_report_id;
  return coalesce(new_count, 0);
end;
$$;

revoke all on function public.record_report_view(uuid) from public;
grant execute on function public.record_report_view(uuid) to authenticated;

select 'report_views + view_count ready' as note;
