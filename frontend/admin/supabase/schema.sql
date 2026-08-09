-- ONDA Admin · Supabase schema (직접 연동용)
-- Supabase Dashboard → SQL Editor 에서 실행하세요.

create extension if not exists "pgcrypto";

do $$ begin
  create type public.admin_role as enum ('ADMIN', 'SCHOOL_ADMIN', 'COMPANY_ADMIN');
exception when duplicate_object then null;
end $$;

-- 관리자 프로필 (auth.users 와 1:1)
create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  email text not null unique,
  name text not null,
  role public.admin_role not null default 'ADMIN',
  phone text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.notices (
  id uuid primary key default gen_random_uuid(),
  type text not null,
  title text not null,
  body text not null,
  target text not null default '전체',
  status text not null default '게시중',
  views int not null default 0,
  starts_at timestamptz,
  ends_at timestamptz,
  push boolean not null default false,
  created_by uuid references public.profiles (id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.reports (
  id uuid primary key default gen_random_uuid(),
  type text not null,
  target text not null,
  body text not null,
  status text not null default '처리 대기',
  likes int not null default 0,
  memo text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.routes (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  type text not null,
  status text not null default '운행 중',
  days text not null default '월~금',
  hours text not null default '07:00 ~ 22:00',
  buses text not null default '0대',
  description text,
  created_at timestamptz not null default now()
);

create table if not exists public.vehicles (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  plate text not null unique,
  status text not null default '운행 중',
  mileage text,
  next_maintenance date,
  created_at timestamptz not null default now()
);

create table if not exists public.maintenances (
  id uuid primary key default gen_random_uuid(),
  maintained_at date not null,
  plate text not null,
  item text not null,
  type text not null,
  mechanic text,
  cost int not null default 0,
  status text not null default '예정',
  created_at timestamptz not null default now()
);

create table if not exists public.system_logs (
  id uuid primary key default gen_random_uuid(),
  logged_at timestamptz not null default now(),
  type text not null,
  action text not null,
  actor text,
  ip text,
  target text,
  result text not null default '성공'
);

-- 회원가입 시 profiles 자동 생성
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, name, role, phone)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data->>'name', split_part(new.email, '@', 1)),
    coalesce((new.raw_user_meta_data->>'role')::public.admin_role, 'ADMIN'),
    new.raw_user_meta_data->>'phone'
  );
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- RLS
alter table public.profiles enable row level security;
alter table public.notices enable row level security;
alter table public.reports enable row level security;
alter table public.routes enable row level security;
alter table public.vehicles enable row level security;
alter table public.maintenances enable row level security;
alter table public.system_logs enable row level security;

-- 로그인된 관리자만 읽기/쓰기 (데모용 — 나중에 role 기반으로 강화)
create policy "profiles_select_own" on public.profiles
  for select to authenticated using (true);

create policy "profiles_update_own" on public.profiles
  for update to authenticated using (auth.uid() = id);

create policy "notices_all_auth" on public.notices
  for all to authenticated using (true) with check (true);

create policy "reports_all_auth" on public.reports
  for all to authenticated using (true) with check (true);

create policy "routes_all_auth" on public.routes
  for all to authenticated using (true) with check (true);

create policy "vehicles_all_auth" on public.vehicles
  for all to authenticated using (true) with check (true);

create policy "maintenances_all_auth" on public.maintenances
  for all to authenticated using (true) with check (true);

create policy "system_logs_select_auth" on public.system_logs
  for select to authenticated using (true);

create policy "system_logs_insert_auth" on public.system_logs
  for insert to authenticated with check (true);
