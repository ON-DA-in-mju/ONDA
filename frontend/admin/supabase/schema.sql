-- ONDA Admin · 실제 Supabase 스키마 참고용
-- 운영 DB는 이미 users / routes / buses / notices / reports ... 로 구성되어 있습니다.
-- 아래 profiles 기반 DDL 은 레거시입니다. 운영 DB에 새로 실행하지 마세요.
-- 타입 소스 오브 트루스: src/types/database.ts
--
-- 시드:
--   supabase/seed_mju_2026.sql        — 노선·공식 시간표
--   supabase/seed_demo_scenario.sql   — 시연 계정·배차·차량·안전정차 테이블
--   (컬럼 추가) users.login_id / operations.external_id,round,origin,destination,expected_end_time
--   (테이블 추가) safe_stop_requests

-- users.role: STUDENT | DRIVER | ADMIN
-- notices: title, content, author_id
-- reports.status: PENDING | PROCESSING | COMPLETED
-- buses.status: ACTIVE | INACTIVE | MAINTENANCE
-- operations.status: SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED

select 'See src/types/database.ts and seed_demo_scenario.sql' as note;
