-- user_favorites: 학생 즐겨찾기 노선/정류장
-- Supabase SQL Editor에서 실행

create table if not exists public.user_favorites (
  user_id uuid not null references public.users (id) on delete cascade,
  target_type text not null check (target_type in ('ROUTE', 'STOP')),
  target_id text not null,
  created_at timestamptz not null default now(),
  primary key (user_id, target_type, target_id)
);

create index if not exists user_favorites_user_id_idx
  on public.user_favorites (user_id);

comment on table public.user_favorites is
  '학생 즐겨찾기. ROUTE=앱 노선 UI id, STOP=stops.id 또는 stop guide id';

alter table public.user_favorites enable row level security;

drop policy if exists "user_favorites_select_own" on public.user_favorites;
create policy "user_favorites_select_own" on public.user_favorites
  for select to authenticated
  using (auth.uid() = user_id);

drop policy if exists "user_favorites_insert_own" on public.user_favorites;
create policy "user_favorites_insert_own" on public.user_favorites
  for insert to authenticated
  with check (auth.uid() = user_id);

drop policy if exists "user_favorites_delete_own" on public.user_favorites;
create policy "user_favorites_delete_own" on public.user_favorites
  for delete to authenticated
  using (auth.uid() = user_id);

grant select, insert, delete on public.user_favorites to authenticated;

select 'user_favorites ready' as note;
