-- routes.start_location / end_location (코드·시드와 스키마 맞춤)
-- 운영 DB에 컬럼이 없으면 Supabase SQL Editor에서 1회 실행

alter table public.routes
  add column if not exists start_location text,
  add column if not exists end_location text;
