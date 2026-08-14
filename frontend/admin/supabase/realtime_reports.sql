-- reports → supabase_realtime publication
-- 관리자 대시보드 '전체 학생 제보' 건수 Realtime 반영용
-- Supabase SQL Editor에서 실행

alter table public.reports replica identity full;

do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'reports'
  ) then
    execute 'alter publication supabase_realtime add table public.reports';
  end if;
end $$;

select pubname, schemaname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'reports';

select 'reports realtime publication ok' as note;
