-- 공지 조회수: 사용자당 1회만 카운트
-- Supabase Dashboard → SQL Editor에서 실행

alter table public.notices
  add column if not exists view_count integer not null default 0;

comment on column public.notices.view_count is '고유 조회수 (사용자당 1회)';

create table if not exists public.notice_views (
  notice_id uuid not null references public.notices(id) on delete cascade,
  user_id uuid not null,
  viewed_at timestamptz not null default now(),
  primary key (notice_id, user_id)
);

create index if not exists notice_views_user_idx on public.notice_views (user_id);

alter table public.notice_views enable row level security;

drop policy if exists notice_views_admin_select on public.notice_views;
create policy notice_views_admin_select on public.notice_views
  for select to authenticated
  using (public.is_admin());

drop policy if exists notice_views_own_select on public.notice_views;
create policy notice_views_own_select on public.notice_views
  for select to authenticated
  using (user_id = auth.uid());

-- 조회수 증가는 RPC만 사용 (직접 insert 금지)
create or replace function public.increment_notice_view(p_notice_id uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_count integer;
begin
  if v_uid is null then
    raise exception 'not authenticated';
  end if;
  if p_notice_id is null then
    raise exception 'notice id required';
  end if;

  with ins as (
    insert into public.notice_views (notice_id, user_id)
    values (p_notice_id, v_uid)
    on conflict (notice_id, user_id) do nothing
    returning notice_id
  )
  update public.notices n
  set view_count = coalesce(n.view_count, 0) + 1
  from ins
  where n.id = ins.notice_id;

  select coalesce(view_count, 0) into v_count
  from public.notices
  where id = p_notice_id;

  return coalesce(v_count, 0);
end;
$$;

revoke all on function public.increment_notice_view(uuid) from public;
grant execute on function public.increment_notice_view(uuid) to authenticated;

notify pgrst, 'reload schema';

select 'notices.view_count + increment_notice_view applied' as note;
