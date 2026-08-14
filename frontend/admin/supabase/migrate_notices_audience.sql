-- notices.audience: 학생/기사 대상 저장 + RLS로 해당 역할만 조회
-- 운영 DB에 type/status/starts_at 은 있는데 audience 가 없는 경우를 위한 보완 마이그레이션
-- Supabase Dashboard → SQL Editor에서 실행

-- =============================================================================
-- 1) 컬럼
-- =============================================================================
alter table public.notices
  add column if not exists audience text[] not null default array['STUDENT']::text[];

comment on column public.notices.audience is '대상 역할 배열: STUDENT, DRIVER (다중 선택)';

create index if not exists notices_audience_gin_idx on public.notices using gin (audience);

-- =============================================================================
-- 2) 제약
-- =============================================================================
do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'notices_audience_check'
  ) then
    alter table public.notices
      add constraint notices_audience_check
      check (
        cardinality(audience) >= 1
        and audience <@ array['STUDENT', 'DRIVER']::text[]
      );
  end if;
end $$;

-- =============================================================================
-- 3) 역할 헬퍼 (없으면 생성, 있으면 본문 교체)
-- =============================================================================
create or replace function public.is_driver()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.users
    where id = auth.uid() and role = 'DRIVER'
  )
$$;

revoke all on function public.is_driver() from public;
grant execute on function public.is_driver() to authenticated, anon;

create or replace function public.is_student()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.users
    where id = auth.uid() and role = 'STUDENT'
  )
$$;

revoke all on function public.is_student() from public;
grant execute on function public.is_student() to authenticated, anon;

create or replace function public.notice_in_publish_window(
  p_starts_at timestamptz,
  p_ends_at timestamptz
)
returns boolean
language sql
stable
as $$
  select
    (p_starts_at is null or p_starts_at <= now())
    and (p_ends_at is null or p_ends_at >= now())
$$;

revoke all on function public.notice_in_publish_window(timestamptz, timestamptz) from public;
grant execute on function public.notice_in_publish_window(timestamptz, timestamptz) to authenticated, anon;

-- =============================================================================
-- 4) RLS: 선택한 대상에게만 노출
-- =============================================================================
alter table public.notices enable row level security;

drop policy if exists notices_admin_all on public.notices;
drop policy if exists notices_student_select on public.notices;
drop policy if exists notices_driver_select on public.notices;
drop policy if exists "Enable read access for all users" on public.notices;
drop policy if exists notices_select on public.notices;
drop policy if exists notices_all on public.notices;

create policy notices_admin_all on public.notices
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

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

notify pgrst, 'reload schema';

-- =============================================================================
-- 5) 확인
-- =============================================================================
select column_name, data_type, column_default
from information_schema.columns
where table_schema = 'public' and table_name = 'notices' and column_name = 'audience';

select id, title, audience, status
from public.notices
order by created_at desc
limit 8;

select 'notices.audience + target RLS applied' as note;
