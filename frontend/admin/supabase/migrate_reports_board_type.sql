-- reports: 제보(REPORT) / 소통 글쓰기(POST) 구분
-- Supabase SQL Editor에서 실행하세요.
--
-- report_reactions 에는 구분 컬럼이 필요 없습니다.
-- 반응은 report_id 로 연결되고, 제보/글 구분은 reports.board_type 으로 확인합니다.

alter table public.reports
  add column if not exists board_type text not null default 'REPORT';

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'reports_board_type_check'
  ) then
    alter table public.reports
      add constraint reports_board_type_check
      check (board_type in ('REPORT', 'POST'));
  end if;
end $$;

comment on column public.reports.board_type is
  '커뮤니티 구분: REPORT=상황 제보, POST=학생 소통 글쓰기';

create index if not exists reports_board_type_idx
  on public.reports (board_type, created_at desc);

-- 관리자 제보 목록은 board_type = REPORT 만 보면 됩니다.
-- (앱/SQL에서 필터). 기존 행은 default REPORT 로 유지됩니다.
