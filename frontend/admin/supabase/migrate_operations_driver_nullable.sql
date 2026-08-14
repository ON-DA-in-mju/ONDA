-- 운행을 기사 없이 먼저 만들고, 나중에 배정할 수 있도록 driver_id 를 nullable 로 변경
-- Supabase Dashboard → SQL Editor에서 실행
-- 이미 nullable 이면 그대로 통과합니다.

alter table public.operations
  alter column driver_id drop not null;

comment on column public.operations.driver_id is
  '배정된 기사. 운행만 생성한 뒤에는 null, 기사 배정 화면에서 지정';

select 'operations.driver_id is now nullable' as note;
