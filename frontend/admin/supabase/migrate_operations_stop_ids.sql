-- operations.origin / destination (text) → origin_stop_id / destination_stop_id (FK → stops)
-- 팀원 스키마와 맞추기용. 이미 stop_id 컬럼이 있으면 환경에서는 no-op에 가깝게 동작.
-- Run in Supabase SQL Editor.

-- 1) 새 컬럼
alter table public.operations
  add column if not exists origin_stop_id uuid references public.stops (id) on delete set null,
  add column if not exists destination_stop_id uuid references public.stops (id) on delete set null;

-- 2) 구 text 컬럼이 남아 있으면 경우 stop_name 매칭으로 백필
do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'operations' and column_name = 'origin'
  ) then
    update public.operations o
    set origin_stop_id = s.id
    from public.stops s
    where o.origin_stop_id is null
      and o.origin is not null
      and trim(o.origin) <> ''
      and s.stop_name = trim(o.origin);
  end if;

  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'operations' and column_name = 'destination'
  ) then
    update public.operations o
    set destination_stop_id = s.id
    from public.stops s
    where o.destination_stop_id is null
      and o.destination is not null
      and trim(o.destination) <> ''
      and s.stop_name = trim(o.destination);
  end if;
end $$;

-- 3) (선택) 구 컬럼 제거 — 팀원이 이미 제거했다면 스킵됨
alter table public.operations drop column if exists origin;
alter table public.operations drop column if exists destination;

create index if not exists operations_origin_stop_id_idx on public.operations (origin_stop_id);
create index if not exists operations_destination_stop_id_idx on public.operations (destination_stop_id);

select 'operations stop_id columns ready' as note;
