-- operation_device_status → supabase_realtime publication
-- (migrate_operation_device_status.sql 에 포함됨. 단독 재실행용)
-- Supabase Dashboard → SQL Editor에서 실행

do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'operation_device_status'
  ) then
    execute 'alter publication supabase_realtime add table public.operation_device_status';
  end if;
end $$;

select pubname, schemaname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'operation_device_status';
