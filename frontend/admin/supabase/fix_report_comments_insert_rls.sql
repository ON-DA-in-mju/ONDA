-- report_comments INSERT RLS: 제보(REPORT) + 소통 글(POST) 모두 댓글 허용
-- 기존 정책은 board_type = 'POST' 만 허용해서 제보 댓글이 42501 로 실패했음.
-- Supabase SQL Editor에서 실행

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

comment on table public.report_comments is
  '학생 제보(REPORT)·소통 글(POST) 공통 댓글 — report_id → reports.id, 구분은 reports.board_type';

-- 구분 확인용 (선택)
-- select rc.id, rc.content, r.board_type, r.title
-- from public.report_comments rc
-- join public.reports r on r.id = rc.report_id
-- order by rc.created_at desc
-- limit 20;

select 'report_comments insert allows REPORT+POST' as note;
