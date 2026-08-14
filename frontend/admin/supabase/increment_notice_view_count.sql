-- notices.view_count 증가 (학생/기사 앱 공지 상세 열람 시)
-- Supabase SQL Editor에서 실행

alter table public.notices
  add column if not exists view_count integer not null default 0;

comment on column public.notices.view_count is '앱에서 공지 상세를 연 횟수';

create or replace function public.increment_notice_view_count(p_notice_id uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  new_count integer;
begin
  if p_notice_id is null then
    return 0;
  end if;

  update public.notices
  set view_count = coalesce(view_count, 0) + 1
  where id = p_notice_id
    and coalesce(status, 'PUBLISHED') in ('PUBLISHED', 'SCHEDULED')
  returning view_count into new_count;

  return coalesce(new_count, 0);
end;
$$;

revoke all on function public.increment_notice_view_count(uuid) from public;
grant execute on function public.increment_notice_view_count(uuid) to authenticated;

select 'increment_notice_view_count ready' as note;
