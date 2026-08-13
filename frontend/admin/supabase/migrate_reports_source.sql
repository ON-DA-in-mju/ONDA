-- reports: 학생 제보 / 기사 문의 구분 컬럼
-- Supabase SQL Editor에서 실행하세요.

alter table public.reports
  add column if not exists source text not null default 'STUDENT';

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'reports_source_check'
  ) then
    alter table public.reports
      add constraint reports_source_check
      check (source in ('STUDENT', 'DRIVER'));
  end if;
end $$;

alter table public.reports
  add column if not exists category text;

comment on column public.reports.source is '문의/제보 출처: STUDENT | DRIVER';
comment on column public.reports.category is '문의 유형 (예: account, assignment, gps …)';

create index if not exists reports_source_idx on public.reports (source);
