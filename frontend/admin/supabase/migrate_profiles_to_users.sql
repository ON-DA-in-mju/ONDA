-- profiles → users (실제 ONDA 스키마 기준)
-- user_role: STUDENT | DRIVER | ADMIN
-- users 컬럼: id, name, phone, role, student_no, profile_image, created_at, updated_at, email
-- SQL Editor에서 실행하세요.

-- 1) profiles 데이터를 users로 복사/갱신
do $$
begin
  if exists (
    select 1 from information_schema.tables
    where table_schema = 'public' and table_name = 'profiles'
  ) then
    insert into public.users (id, email, name, role, phone, created_at, updated_at)
    select
      p.id,
      p.email,
      p.name,
      case
        when p.role::text in ('STUDENT', 'DRIVER', 'ADMIN') then p.role::text::public.user_role
        else 'ADMIN'::public.user_role
      end,
      p.phone,
      p.created_at,
      p.updated_at
    from public.profiles p
    on conflict (id) do update set
      email = excluded.email,
      name = excluded.name,
      role = excluded.role,
      phone = excluded.phone,
      updated_at = excluded.updated_at;
  end if;
end $$;

-- 2) auth 가입 시 public.users 자동 생성
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_role text;
begin
  v_role := upper(coalesce(new.raw_user_meta_data->>'role', 'ADMIN'));
  if v_role not in ('STUDENT', 'DRIVER', 'ADMIN') then
    v_role := 'ADMIN';
  end if;

  insert into public.users (id, email, name, role, phone)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data->>'name', split_part(new.email, '@', 1)),
    v_role::public.user_role,
    new.raw_user_meta_data->>'phone'
  )
  on conflict (id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 3) users RLS
alter table public.users enable row level security;

drop policy if exists "users_select_auth" on public.users;
drop policy if exists "users_update_own" on public.users;

create policy "users_select_auth" on public.users
  for select to authenticated using (true);

create policy "users_update_own" on public.users
  for update to authenticated using (auth.uid() = id);

-- 4) profiles가 남아 있으면 삭제 (없으면 무시)
drop table if exists public.profiles cascade;
