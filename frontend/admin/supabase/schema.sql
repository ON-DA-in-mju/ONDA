-- ONDA 실제 스키마 기준 참고용
-- (이미 Table Editor에 테이블이 있으므로 전체 재실행하지 마세요)
-- 필요 시 Auth 트리거/RLS만 migrate_profiles_to_users.sql 을 사용하세요.

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
