-- 운행 중단 종료 시각이 지나면 노선을 자동으로 운행 가능으로 되돌림
-- Supabase Dashboard → SQL Editor에서 실행

alter table public.routes
  add column if not exists suspended_until timestamptz;

comment on column public.routes.suspended_until is
  '운행 중단 처리의 종료 시각. 이 시각이 지나면 is_active=true 로 자동 복구';

create or replace function public.expire_suspended_routes()
returns uuid[]
language plpgsql
security definer
set search_path = public
as $$
declare
  expired_ids uuid[] := '{}';
begin
  select coalesce(array_agg(id), '{}')
    into expired_ids
  from public.routes
  where is_active = false
    and suspended_until is not null
    and suspended_until <= now();

  if expired_ids = '{}' then
    return expired_ids;
  end if;

  update public.routes
  set is_active = true,
      suspended_until = null,
      updated_at = now()
  where id = any(expired_ids);

  update public.operations o
  set status = 'SCHEDULED'
  where o.status = 'CANCELLED'
    and o.started_at is null
    and o.operation_date >= ((timezone('Asia/Seoul', now()))::date)
    and o.schedule_id in (
      select s.id from public.schedules s where s.route_id = any(expired_ids)
    )
    and exists (
      select 1
      from public.operation_logs l
      where l.operation_id = o.id
        and l.event_type = 'STATUS_CHANGED'
        and coalesce(l.log_message, '') like '%ADMIN_SUSPEND%'
    );

  return expired_ids;
end;
$$;

revoke all on function public.expire_suspended_routes() from public;
grant execute on function public.expire_suspended_routes() to authenticated;
grant execute on function public.expire_suspended_routes() to anon;
grant execute on function public.expire_suspended_routes() to service_role;

select 'routes.suspended_until + expire_suspended_routes() applied' as note;
