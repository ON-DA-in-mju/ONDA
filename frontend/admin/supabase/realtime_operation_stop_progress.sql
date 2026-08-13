-- operation_stop_progress → supabase_realtime publication
-- (migrate_operation_stop_progress.sql 에 포함됨. 단독 재실행용)

do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'operation_stop_progress'
  ) then
    execute 'alter publication supabase_realtime add table public.operation_stop_progress';
  end if;
end $$;

select pubname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'operation_stop_progress';
