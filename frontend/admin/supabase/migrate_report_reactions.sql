  -- 제보 좋아요/싫어요 (학생 1인당 제보당 1개 반응)
  -- Supabase SQL Editor에서 실행하세요.

  create table if not exists public.report_reactions (
    id uuid primary key default gen_random_uuid(),
    report_id uuid not null references public.reports (id) on delete cascade,
    user_id uuid not null references public.users (id) on delete cascade,
    reaction text not null check (reaction in ('LIKE', 'DISLIKE')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (report_id, user_id)
  );

  create index if not exists report_reactions_report_id_idx
    on public.report_reactions (report_id);

  create index if not exists report_reactions_user_id_idx
    on public.report_reactions (user_id);

  alter table public.report_reactions enable row level security;

  -- 읽기: 로그인 사용자 전체 (집계·내 반응 표시용)
  drop policy if exists "report_reactions_select_auth" on public.report_reactions;
  create policy "report_reactions_select_auth" on public.report_reactions
    for select to authenticated
    using (true);

  -- 등록: 본인만
  drop policy if exists "report_reactions_insert_own" on public.report_reactions;
  create policy "report_reactions_insert_own" on public.report_reactions
    for insert to authenticated
    with check (auth.uid() = user_id);

  -- 수정: 본인만
  drop policy if exists "report_reactions_update_own" on public.report_reactions;
  create policy "report_reactions_update_own" on public.report_reactions
    for update to authenticated
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

  -- 삭제: 본인만
  drop policy if exists "report_reactions_delete_own" on public.report_reactions;
  create policy "report_reactions_delete_own" on public.report_reactions
    for delete to authenticated
    using (auth.uid() = user_id);

  grant select, insert, update, delete on public.report_reactions to authenticated;
