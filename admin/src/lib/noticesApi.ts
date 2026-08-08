import { isSupabaseConfigured, supabase } from '../lib/supabase'
import type { Database } from '../types/database'

export type NoticeRow = Database['public']['Tables']['notices']['Row']

/** 공지 목록 — Supabase 직접 조회 (미설정 시 null → 페이지에서 mock 사용) */
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

export async function createNotice(
  payload: Database['public']['Tables']['notices']['Insert'],
): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) {
    return { ok: false, message: 'Supabase 미설정' }
  }

  const { error } = await supabase.from('notices').insert(payload)
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}
