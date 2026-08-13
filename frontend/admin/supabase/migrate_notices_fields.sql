-- notices: 공지 등록 UI / 공통 명세 필드 + RLS
-- Supabase Dashboard → SQL Editor에서 실행 (postgres)
-- 기존 title, content, author_id, created_at, updated_at 유지. 노선 컬럼 없음.

-- =============================================================================
-- 1) 컬럼
-- =============================================================================
-- type: 긴급/중요/운행 변경/일반
alter table public.notices
  add column if not exists type text not null default 'GENERAL';

-- audience: 학생·기사 다중 선택 (둘 다 가능)
alter table public.notices
  add column if not exists audience text[] not null default array['STUDENT']::text[];

-- 게시 기간 (상시 게시 시 둘 다 NULL)
alter table public.notices
  add column if not exists starts_at timestamptz;

alter table public.notices
  add column if not exists ends_at timestamptz;

-- 푸시 동시 발송 여부
alter table public.notices
  add column if not exists is_push boolean not null default false;

-- 목록용 게시 상태
alter table public.notices
  add column if not exists status text not null default 'PUBLISHED';

-- 기존 행은 컬럼 DEFAULT 적용: type=GENERAL, audience={STUDENT}, status=PUBLISHED,
-- starts_at/ends_at NULL(상시), is_push=false

-- =============================================================================
-- 2) 제약
-- =============================================================================
do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'notices_type_check'
  ) then
    alter table public.notices
      add constraint notices_type_check
      check (type in ('URGENT', 'IMPORTANT', 'OPERATION_CHANGE', 'GENERAL'));
  end if;

  if not exists (
    select 1 from pg_constraint where conname = 'notices_status_check'
  ) then
    alter table public.notices
      add constraint notices_status_check
      check (status in ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'ENDED'));
  end if;

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

  if not exists (
    select 1 from pg_constraint where conname = 'notices_period_check'
  ) then
    alter table public.notices
      add constraint notices_period_check
      check (ends_at is null or starts_at is null or ends_at >= starts_at);
  end if;
end $$;

comment on column public.notices.type is 'URGENT | IMPORTANT | OPERATION_CHANGE | GENERAL';
comment on column public.notices.audience is '대상 역할 배열: STUDENT, DRIVER (다중 선택)';
comment on column public.notices.starts_at is '게시 시작. 상시 게시면 NULL';
comment on column public.notices.ends_at is '게시 종료. 상시 게시면 NULL';
comment on column public.notices.is_push is '등록 시 푸시 동시 발송 여부';
comment on column public.notices.status is 'DRAFT | SCHEDULED | PUBLISHED | ENDED';

create index if not exists notices_status_idx on public.notices (status);
create index if not exists notices_type_idx on public.notices (type);
create index if not exists notices_starts_at_idx on public.notices (starts_at);
create index if not exists notices_audience_gin_idx on public.notices using gin (audience);

-- =============================================================================
-- 3) 헬퍼 (RLS 재귀 방지)
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

-- 앱에 노출 가능한 게시 구간인지 (상시 = starts/ends 둘 다 NULL)
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
-- 4) RLS
-- =============================================================================
alter table public.notices enable row level security;

drop policy if exists notices_admin_all on public.notices;
drop policy if exists notices_student_select on public.notices;
drop policy if exists notices_driver_select on public.notices;
-- 레거시 정책명이 있을 수 있어 정리
drop policy if exists "Enable read access for all users" on public.notices;
drop policy if exists notices_select on public.notices;
drop policy if exists notices_all on public.notices;
drop policy if exists notices_select_auth on public.notices;
drop policy if exists notices_insert_auth on public.notices;

create policy notices_admin_all on public.notices
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- 학생: 대상에 STUDENT 포함 + 게시/예약 + 기간 내
create policy notices_student_select on public.notices
  for select to authenticated
  using (
    public.is_student()
    and 'STUDENT' = any (audience)
    and status in ('PUBLISHED', 'SCHEDULED')
    and public.notice_in_publish_window(starts_at, ends_at)
  );

-- 기사: 대상에 DRIVER 포함 + 동일 조건
create policy notices_driver_select on public.notices
  for select to authenticated
  using (
    public.is_driver()
    and 'DRIVER' = any (audience)
    and status in ('PUBLISHED', 'SCHEDULED')
    and public.notice_in_publish_window(starts_at, ends_at)
  );

-- =============================================================================
-- 5) 확인
-- =============================================================================
select column_name, data_type, column_default, is_nullable
from information_schema.columns
where table_schema = 'public' and table_name = 'notices'
order by ordinal_position;

select policyname, cmd, roles
from pg_policies
where schemaname = 'public' and tablename = 'notices'
order by policyname;

select 'notices fields + RLS applied' as note;
