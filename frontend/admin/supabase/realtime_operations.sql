-- operations → supabase_realtime publication
-- 학생 앱 운행 시작/종료 UPDATE Realtime 구독용. RLS/데이터는 변경하지 않음.
-- Supabase Dashboard → SQL Editor에서 실행

do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'operations'
  ) then
    execute 'alter publication supabase_realtime add table public.operations';
  end if;
end $$;

-- 확인
select pubname, schemaname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'operations';
