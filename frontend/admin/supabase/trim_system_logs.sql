-- system_logs: 최대 100건 유지 + 관리자 조회 + 기사 앱 삽입 허용
-- Supabase SQL Editor에서 1회 실행

-- 1) max cap
create or replace function public.trim_system_logs(max_rows int default 100)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from public.system_logs sl
  using (
    select id
    from public.system_logs
    order by logged_at desc nulls last, id desc
    offset greatest(coalesce(max_rows, 100), 0)
  ) old
  where sl.id = old.id;
end;
$$;

revoke all on function public.trim_system_logs(int) from public;
grant execute on function public.trim_system_logs(int) to authenticated;

create or replace function public.trg_trim_system_logs()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.trim_system_logs(100);
  return null;
end;
$$;

drop trigger if exists system_logs_trim_ai on public.system_logs;
create trigger system_logs_trim_ai
  after insert on public.system_logs
  for each statement
  execute function public.trg_trim_system_logs();

-- 기존 초과분 정리
select public.trim_system_logs(100);

-- 2) table grants (RLS와 별개로 역할에 INSERT/SELECT 권한 필요)
grant select, insert on public.system_logs to authenticated;
grant usage, select on all sequences in schema public to authenticated;

-- 3) RLS policies
alter table public.system_logs enable row level security;

drop policy if exists system_logs_admin_select on public.system_logs;
create policy system_logs_admin_select on public.system_logs
  for select to authenticated
  using (public.is_admin());

-- 주의: system_logs에는 driver_id 같은 참조키가 없어서,
-- 초기 통합 단계에서는 authenticated 전체 insert를 허용합니다.
drop policy if exists system_logs_authenticated_insert on public.system_logs;
create policy system_logs_authenticated_insert on public.system_logs
  for insert to authenticated
  with check (true);
