-- report_comments: 소프트 삭제 (is_deleted)
-- 이미 migrate_report_comments.sql 을 실행했다면 이것만 추가 실행하세요.

alter table public.report_comments
  add column if not exists is_deleted boolean not null default false;

comment on column public.report_comments.is_deleted is
  'true 이면 화면에 "삭제된 댓글입니다." 로 표시 (행은 유지)';

create index if not exists report_comments_is_deleted_idx
  on public.report_comments (report_id, is_deleted);
