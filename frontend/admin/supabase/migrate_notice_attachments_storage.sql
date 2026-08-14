-- 공지 첨부/이미지 Storage 버킷
-- Supabase Dashboard → SQL Editor에서 실행
-- 버킷이 이미 있으면 insert 는 무시됩니다.

insert into storage.buckets (id, name, public)
values ('notice-attachments', 'notice-attachments', true)
on conflict (id) do nothing;

drop policy if exists notice_attachments_public_read on storage.objects;
create policy notice_attachments_public_read on storage.objects
  for select
  to public
  using (bucket_id = 'notice-attachments');

drop policy if exists notice_attachments_admin_insert on storage.objects;
create policy notice_attachments_admin_insert on storage.objects
  for insert
  to authenticated
  with check (bucket_id = 'notice-attachments' and public.is_admin());

drop policy if exists notice_attachments_admin_update on storage.objects;
create policy notice_attachments_admin_update on storage.objects
  for update
  to authenticated
  using (bucket_id = 'notice-attachments' and public.is_admin())
  with check (bucket_id = 'notice-attachments' and public.is_admin());

drop policy if exists notice_attachments_admin_delete on storage.objects;
create policy notice_attachments_admin_delete on storage.objects
  for delete
  to authenticated
  using (bucket_id = 'notice-attachments' and public.is_admin());

select 'notice-attachments storage bucket + policies applied' as note;
