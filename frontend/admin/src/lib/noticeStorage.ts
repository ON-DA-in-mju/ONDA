import { supabase } from './supabase'

const BUCKET = 'notice-attachments'

export async function uploadNoticeFile(
  file: File,
  folder: string,
): Promise<{ url: string; name: string } | { message: string }> {
  const safeName = file.name.replace(/[^\w.\-가-힣]/g, '_')
  const path = `${folder}/${crypto.randomUUID()}-${safeName}`
  const { error } = await supabase.storage.from(BUCKET).upload(path, file, {
    contentType: file.type || 'application/octet-stream',
    upsert: false,
  })
  if (error) {
    return {
      message:
        /bucket/i.test(error.message)
          ? '첨부 저장소가 없습니다. migrate_notice_attachments_storage.sql 을 실행해 주세요.'
          : error.message,
    }
  }
  const { data } = supabase.storage.from(BUCKET).getPublicUrl(path)
  return { url: data.publicUrl, name: file.name }
}

export async function uploadDataUrlImage(
  dataUrl: string,
  folder: string,
): Promise<{ url: string } | { message: string }> {
  const match = dataUrl.match(/^data:(image\/[a-zA-Z0-9+.-]+);base64,(.+)$/)
  if (!match) return { message: '이미지 형식을 확인할 수 없습니다.' }
  const mime = match[1]
  const binary = atob(match[2])
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i)
  const ext = mime.split('/')[1]?.replace('+xml', '') || 'png'
  const file = new File([bytes], `image.${ext}`, { type: mime })
  const uploaded = await uploadNoticeFile(file, folder)
  if ('message' in uploaded) return uploaded
  return { url: uploaded.url }
}

export async function replaceDataImagesWithUploads(html: string, folder: string): Promise<string> {
  const images = [...html.matchAll(/src="(data:image\/[^"]+)"/g)]
  let next = html
  for (const match of images) {
    const dataUrl = match[1]
    const uploaded = await uploadDataUrlImage(dataUrl, folder)
    if ('message' in uploaded) throw new Error(uploaded.message)
    next = next.replace(dataUrl, uploaded.url)
  }
  return next
}
