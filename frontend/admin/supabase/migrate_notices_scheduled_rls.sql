-- notices RLS: SCHEDULED 도 게시 기간(시작~종료) 안이면 앱에 노출
-- Supabase SQL Editor에서 실행

drop policy if exists notices_student_select on public.notices;
drop policy if exists notices_driver_select on public.notices;

create policy notices_student_select on public.notices
  for select to authenticated
  using (
    public.is_student()
    and 'STUDENT' = any (audience)
    and status in ('PUBLISHED', 'SCHEDULED')
    and public.notice_in_publish_window(starts_at, ends_at)
  );

create policy notices_driver_select on public.notices
  for select to authenticated
  using (
    public.is_driver()
    and 'DRIVER' = any (audience)
    and status in ('PUBLISHED', 'SCHEDULED')
    and public.notice_in_publish_window(starts_at, ends_at)
  );

select 'notices scheduled window RLS updated' as note;
