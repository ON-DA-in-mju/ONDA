-- report_reactions → supabase_realtime publication
-- 다른 계정의 좋아요/싫어요가 학생 앱에 바로 반영되도록 한다.
-- Supabase SQL Editor에서 실행

alter table public.report_reactions replica identity full;

do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'report_reactions'
  ) then
    execute 'alter publication supabase_realtime add table public.report_reactions';
  end if;
end $$;

select pubname, schemaname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'report_reactions';

select 'report_reactions realtime publication ok' as note;
