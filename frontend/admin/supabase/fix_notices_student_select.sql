-- notices 학생 SELECT RLS 복구
-- Supabase SQL Editor에서 실행
-- 학생 앱에 테스트_1 / 테스트_2 / 테스트_4 가 안 보일 때

create or replace function public.notice_in_publish_window(
  p_starts_at timestamptz,
  p_ends_at timestamptz
) returns boolean
language sql
stable
as $$
  select
    (p_starts_at is null or p_starts_at <= now())
    and (p_ends_at is null or p_ends_at >= now());
$$;

alter table public.notices enable row level security;

drop policy if exists notices_student_select on public.notices;

create policy notices_student_select on public.notices
  for select to authenticated
  using (
    public.is_student()
    and coalesce(status::text, 'PUBLISHED') in ('PUBLISHED', 'SCHEDULED')
    and public.notice_in_publish_window(starts_at, ends_at)
    and audience is not null
    and 'STUDENT' = any (audience)
  );

-- 기대: 테스트_1, 테스트_2, 테스트_4 (3건)
select id, title, status, audience
from public.notices
where coalesce(status::text, 'PUBLISHED') in ('PUBLISHED', 'SCHEDULED')
  and audience is not null
  and 'STUDENT' = any (audience)
order by created_at desc;

select 'notices student select RLS fixed' as note;
