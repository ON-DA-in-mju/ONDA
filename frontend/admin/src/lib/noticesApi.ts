import { isSupabaseConfigured, supabase } from './supabase'
import type { Database } from '../types/database'

export type NoticeRow = Database['public']['Tables']['notices']['Row']

/** 공지 목록 — notices(title, content, author_id) */
export async function fetchNotices(): Promise<NoticeRow[] | null> {
  if (!isSupabaseConfigured) return null

  const { data, error } = await supabase
    .from('notices')
    .select('*')
    .order('created_at', { ascending: false })

  if (error) {
    console.error('[notices]', error.message)
    return null
  }
  return data
}

export async function createNotice(payload: {
  title: string
  content: string
  author_id?: string | null
}): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) {
    return { ok: false, message: 'Supabase 미설정' }
  }

  const { error } = await supabase.from('notices').insert({
    title: payload.title,
    content: payload.content,
    author_id: payload.author_id ?? null,
  })
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}
