-- ONDA Admin · 실제 Supabase 스키마 참고용
-- 운영 DB는 이미 users / routes / buses / notices / reports ... 로 구성되어 있습니다.
-- 아래 profiles 기반 DDL 은 레거시입니다. 운영 DB에 새로 실행하지 마세요.
-- 타입 소스 오브 트루스: src/types/database.ts
--
-- 시드:
--   supabase/seed_mju_2026.sql        — 노선·공식 시간표
--   supabase/seed_demo_scenario.sql   — 시연 계정·배차·차량·안전정차 테이블
--   (컬럼 추가) users.login_id / operations.external_id,round,origin_stop_id,destination_stop_id,expected_end_time
--   (컬럼 추가) reports.source (STUDENT|DRIVER), reports.category
--   (테이블 추가) safe_stop_requests
--   마이그레이션: supabase/migrate_reports_source.sql
--   마이그레이션: supabase/migrate_notices_fields.sql (type/audience/기간/is_push/status + RLS)
--   마이그레이션: supabase/migrate_notices_audience.sql (운영 DB에 audience 컬럼만 없을 때)
--   마이그레이션: supabase/migrate_operation_device_status.sql (GPS vs 네트워크 heartbeat + RLS + Realtime)
--   마이그레이션: supabase/migrate_operation_stop_progress.sql (정류장 진행 스냅샷 + RLS + Realtime)
--   마이그레이션: supabase/migrate_routes_route_stops.sql (공지 기준 노선·정류장 순서)
--   마이그레이션: supabase/migrate_vehicle_maintenances.sql (정비 이력 + buses 관리자 쓰기)
--   시드: supabase/seed_vehicle_maintenances.sql (차량·정비 mock 데이터)

-- users.role: STUDENT | DRIVER | ADMIN
-- notices: title, content, author_id, type, audience[], starts_at, ends_at, is_push, status
-- operation_device_status: operation_id, gps_ok, gps_enabled, last_location_at, updated_at
-- operation_stop_progress: operation_id, last_arrived_stop_id, last_passed_stop_id, last_arrived_index, last_passed_index, updated_at
-- reports.status: PENDING | PROCESSING | COMPLETED
-- reports.source: STUDENT | DRIVER
-- buses.status: ACTIVE | INACTIVE | MAINTENANCE
-- operations.status: SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED

select 'See src/types/database.ts and seed_demo_scenario.sql' as note;
