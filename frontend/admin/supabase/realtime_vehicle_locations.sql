-- vehicle_locations → supabase_realtime publication
-- 학생 앱 Realtime 구독용. RLS/데이터는 변경하지 않음.
-- Supabase Dashboard → SQL Editor에서 실행

-- 이미 등록돼 있으면 중복 추가 에러 방지
do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'vehicle_locations'
  ) then
    execute 'alter publication supabase_realtime add table public.vehicle_locations';
  end if;
end $$;

-- 확인
select pubname, schemaname, tablename
from pg_publication_tables
where pubname = 'supabase_realtime'
  and tablename = 'vehicle_locations';
