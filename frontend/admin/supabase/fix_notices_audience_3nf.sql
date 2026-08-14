-- migrate_3nf.sql 재실행 전, audience 드롭만 실패한 경우 이 패치만 먼저 실행해도 됨
-- (전체 migrate_3nf.sql 재실행도 가능 — 이 내용이 이미 포함됨)

create table if not exists public.notice_audiences (
  notice_id uuid not null references public.notices(id) on delete cascade,
  audience text not null check (audience in ('STUDENT', 'DRIVER', 'ADMIN')),
  primary key (notice_id, audience)
);

-- 데이터 백필 (audience 컬럼이 아직 있을 때만)
do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'notices' and column_name = 'audience'
  ) then
    insert into public.notice_audiences (notice_id, audience)
    select n.id, a
    from public.notices n
    cross join lateral unnest(coalesce(n.audience, array['STUDENT']::text[])) as a
    where a in ('STUDENT', 'DRIVER', 'ADMIN')
    on conflict do nothing;

    drop policy if exists notices_student_select on public.notices;
    drop policy if exists notices_driver_select on public.notices;

    alter table public.notices drop column if exists audience;
  end if;
end $$;

drop policy if exists notices_student_select on public.notices;
drop policy if exists notices_driver_select on public.notices;

create policy notices_student_select on public.notices
  for select to authenticated
  using (
    exists (
      select 1 from public.users u
      where u.id = auth.uid() and u.role::text = 'STUDENT'
    )
    and exists (
      select 1 from public.notice_audiences na
      where na.notice_id = notices.id and na.audience = 'STUDENT'
    )
    and (coalesce(notices.status, 'PUBLISHED') = 'PUBLISHED' or notices.status is null)
    and (notices.starts_at is null or notices.starts_at <= now())
    and (notices.ends_at is null or notices.ends_at >= now())
  );

create policy notices_driver_select on public.notices
  for select to authenticated
  using (
    exists (
      select 1 from public.users u
      where u.id = auth.uid() and u.role::text = 'DRIVER'
    )
    and exists (
      select 1 from public.notice_audiences na
      where na.notice_id = notices.id and na.audience = 'DRIVER'
    )
    and (coalesce(notices.status, 'PUBLISHED') = 'PUBLISHED' or notices.status is null)
    and (notices.starts_at is null or notices.starts_at <= now())
    and (notices.ends_at is null or notices.ends_at >= now())
  );

select 'notices audience → notice_audiences patch ok' as note;
