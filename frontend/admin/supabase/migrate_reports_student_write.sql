-- 학생 앱이 reports 테이블에 제보를 등록·수정·삭제할 수 있도록 RLS 보강
-- Supabase SQL Editor에서 실행하세요.

alter table public.reports enable row level security;

-- 읽기: 로그인 사용자 전체 (기존 정책과 동일하게 재생성)
drop policy if exists "reports_select_auth" on public.reports;
create policy "reports_select_auth" on public.reports
  for select to authenticated
  using (true);

-- 등록: 본인 user_id 로만 insert
drop policy if exists "reports_insert_auth" on public.reports;
create policy "reports_insert_auth" on public.reports
  for insert to authenticated
  with check (auth.uid() = user_id);

-- 수정: 본인 제보만
drop policy if exists "reports_update_own" on public.reports;
create policy "reports_update_own" on public.reports
  for update to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- 삭제: 본인 제보만
drop policy if exists "reports_delete_own" on public.reports;
create policy "reports_delete_own" on public.reports
  for delete to authenticated
  using (auth.uid() = user_id);

-- 삭제: 관리자(ADMIN)는 전체 삭제 가능
drop policy if exists "reports_delete_admin" on public.reports;
create policy "reports_delete_admin" on public.reports
  for delete to authenticated
  using (
    exists (
      select 1 from public.users u
      where u.id = auth.uid() and upper(u.role::text) = 'ADMIN'
    )
  );

-- source / category 컬럼이 없는 구 DB 대비
alter table public.reports
  add column if not exists source text not null default 'STUDENT';

alter table public.reports
  add column if not exists category text;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'reports_source_check'
  ) then
    alter table public.reports
      add constraint reports_source_check
      check (source in ('STUDENT', 'DRIVER'));
  end if;
end $$;

create index if not exists reports_source_idx on public.reports (source);
create index if not exists reports_created_at_idx on public.reports (created_at desc);
