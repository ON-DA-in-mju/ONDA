-- notices → supabase_realtime publication + replica identity
-- 학생/기사 앱 Realtime 구독용. Supabase SQL Editor에서 실행

-- UPDATE/DELETE 이벤트가 RLS 환경에서도 잘 전달되도록
alter table public.notices replica identity full;

do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'notices'
  ) then
    execute 'alter publication supabase_realtime add table public.notices';
  end if;
end $$;

select pubname, schemaname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'notices';

select 'notices realtime publication ok' as note;
