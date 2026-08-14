-- 소통 글(POST) 댓글
-- Supabase SQL Editor에서 실행하세요.
--
-- 글 1개 : 댓글 N개 구조라 reports에 컬럼을 넣는 방식은 맞지 않고
-- report_comments 테이블이 필요합니다. report_id 로 reports(id)에 연결합니다.

create table if not exists public.report_comments (
  id uuid primary key default gen_random_uuid(),
  report_id uuid not null references public.reports (id) on delete cascade,
  user_id uuid not null references public.users (id) on delete cascade,
  content text not null,
  is_deleted boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint report_comments_content_len check (char_length(trim(content)) between 1 and 500)
);

create index if not exists report_comments_report_id_idx
  on public.report_comments (report_id, created_at asc);

create index if not exists report_comments_user_id_idx
  on public.report_comments (user_id);

alter table public.report_comments enable row level security;

drop policy if exists "report_comments_select_auth" on public.report_comments;
create policy "report_comments_select_auth" on public.report_comments
  for select to authenticated
  using (true);

drop policy if exists "report_comments_insert_own" on public.report_comments;
create policy "report_comments_insert_own" on public.report_comments
  for insert to authenticated
  with check (
    auth.uid() = user_id
    and exists (
      select 1 from public.reports r
      where r.id = report_id
        and coalesce(r.source, 'STUDENT') = 'STUDENT'
        and coalesce(r.board_type, 'REPORT') in ('REPORT', 'POST')
    )
  );

drop policy if exists "report_comments_update_own" on public.report_comments;
create policy "report_comments_update_own" on public.report_comments
  for update to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "report_comments_delete_own" on public.report_comments;
create policy "report_comments_delete_own" on public.report_comments
  for delete to authenticated
  using (auth.uid() = user_id);

grant select, insert, update, delete on public.report_comments to authenticated;

comment on table public.report_comments is
  '학생 제보(REPORT)·소통 글(POST) 공통 댓글 — report_id → reports.id, 구분은 reports.board_type';
