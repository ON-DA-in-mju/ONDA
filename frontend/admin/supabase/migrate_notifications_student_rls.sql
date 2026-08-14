-- notifications: 학생 본인 행 SELECT/UPDATE(읽음) RLS
-- Supabase SQL Editor에서 실행

alter table public.notifications enable row level security;

drop policy if exists notifications_student_select on public.notifications;
drop policy if exists notifications_student_update on public.notifications;
drop policy if exists notifications_admin_all on public.notifications;

-- 관리자 전체
create policy notifications_admin_all on public.notifications
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- 학생: 본인 알림만 조회
create policy notifications_student_select on public.notifications
  for select to authenticated
  using (
    public.is_student()
    and user_id = auth.uid()
  );

-- 학생: 본인 알림 읽음 처리
create policy notifications_student_update on public.notifications
  for update to authenticated
  using (
    public.is_student()
    and user_id = auth.uid()
  )
  with check (user_id = auth.uid());

select 'notifications student RLS ok' as note;
