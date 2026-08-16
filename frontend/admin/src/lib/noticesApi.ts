import { isSupabaseConfigured, supabase } from './supabase'
import type { Database } from '../types/database'
import {
  isMissingAudienceColumnError,
  MISSING_AUDIENCE_COLUMN_MESSAGE,
  normalizeNoticeAudience,
} from './noticeAudience'

export type NoticeRow = Database['public']['Tables']['notices']['Row']

/** 공지 목록 — notices(title, content, author_id) */
export async function fetchNotices(): Promise<{ rows: NoticeRow[]; error?: string }> {
  if (!isSupabaseConfigured) {
    return { rows: [], error: 'Supabase가 설정되지 않았습니다.' }
  }

  const { data, error } = await supabase
    .from('notices')
    .select('*')
    .order('created_at', { ascending: false })

  if (error) {
    console.error('[notices]', error.message)
    return { rows: [], error: error.message }
  }
  return {
    rows: (data ?? []).map((row) => ({
      ...row,
      audience: normalizeNoticeAudience((row as NoticeRow & { audience?: unknown }).audience),
    })),
  }
}

export async function createNotice(payload: {
  title: string
  content: string
  author_id?: string | null
  type?: Database['public']['Tables']['notices']['Insert']['type']
  audience?: Database['public']['Tables']['notices']['Insert']['audience']
  starts_at?: string | null
  ends_at?: string | null
  is_push?: boolean
  status?: Database['public']['Tables']['notices']['Insert']['status']
}): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) {
    return { ok: false, message: 'Supabase 미설정' }
  }

  const { data, error } = await supabase.from('notices').insert({
    title: payload.title,
    content: payload.content,
    author_id: payload.author_id ?? null,
    type: payload.type,
    audience: payload.audience,
    starts_at: payload.starts_at ?? null,
    ends_at: payload.ends_at ?? null,
    status: payload.status ?? 'PUBLISHED',
  }).select('id')
  if (error) {
    return {
      ok: false,
      message: isMissingAudienceColumnError(error.message)
        ? MISSING_AUDIENCE_COLUMN_MESSAGE
        : error.message,
    }
  }
  if (!data?.length) {
    return { ok: false, message: '등록 권한이 없어 공지가 저장되지 않았습니다.' }
  }
  return { ok: true }
}

/** 공지 삭제 = DB 행 유지, status=ENDED 로 앱 비노출 + 관리자 로그 보존 */
export async function endNotice(id: string): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) {
    return { ok: false, message: 'Supabase 미설정' }
  }
  if (!id.trim()) {
    return { ok: false, message: '삭제할 공지를 선택하세요.' }
  }

  const { data: row, error: readError } = await supabase
    .from('notices')
    .select('id, starts_at')
    .eq('id', id)
    .maybeSingle()

  if (readError) return { ok: false, message: readError.message }
  if (!row) return { ok: false, message: '공지를 찾을 수 없습니다.' }

  const nowMs = Date.now()
  const nowIso = new Date(nowMs).toISOString()
  const startMs = row.starts_at ? Date.parse(row.starts_at) : NaN
  const endsAt =
    Number.isFinite(startMs) && startMs > nowMs ? new Date(startMs).toISOString() : nowIso

  const { data, error } = await supabase
    .from('notices')
    .update({
      status: 'ENDED',
      ends_at: endsAt,
      updated_at: nowIso,
    })
    .eq('id', id)
    .select('id')

  if (error) return { ok: false, message: error.message }
  if (!data?.length) return { ok: false, message: '종료 권한이 없거나 이미 삭제되었습니다.' }
  return { ok: true }
}

/** 공지 완전 삭제 — notices 행 및 cascade 된 조회 이력까지 제거 */
export async function deleteNotice(id: string): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) {
    return { ok: false, message: 'Supabase 미설정' }
  }
  if (!id.trim()) {
    return { ok: false, message: '삭제할 공지를 선택하세요.' }
  }

  const { data, error } = await supabase.from('notices').delete().eq('id', id).select('id')
  if (error) return { ok: false, message: error.message }
  if (!data?.length) return { ok: false, message: '삭제 권한이 없거나 이미 없는 공지입니다.' }
  return { ok: true }
}

export async function updateNotice(
  id: string,
  payload: {
    title: string
    content: string
    type?: Database['public']['Tables']['notices']['Insert']['type']
    audience?: Database['public']['Tables']['notices']['Insert']['audience']
    starts_at?: string | null
    ends_at?: string | null
    is_push?: boolean
    status?: Database['public']['Tables']['notices']['Insert']['status']
  },
): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) {
    return { ok: false, message: 'Supabase 미설정' }
  }
  if (!id.trim()) {
    return { ok: false, message: '수정할 공지를 선택하세요.' }
  }

  const { data, error } = await supabase
    .from('notices')
    .update({
      title: payload.title,
      content: payload.content,
      type: payload.type,
      audience: payload.audience,
      starts_at: payload.starts_at ?? null,
      ends_at: payload.ends_at ?? null,
      status: payload.status ?? 'PUBLISHED',
      updated_at: new Date().toISOString(),
    })
    .eq('id', id)
    .select('id')

  if (error) {
    return {
      ok: false,
      message: isMissingAudienceColumnError(error.message)
        ? MISSING_AUDIENCE_COLUMN_MESSAGE
        : error.message,
    }
  }
  if (!data?.length) return { ok: false, message: '수정 권한이 없거나 공지를 찾을 수 없습니다.' }
  return { ok: true }
}

export async function incrementNoticeView(id: string): Promise<number | null> {
  if (!isSupabaseConfigured || !id.trim()) return null
  const { data, error } = await supabase.rpc('increment_notice_view', { p_notice_id: id })
  if (error) {
    console.warn('[notices] view increment', error.message)
    return null
  }
  return typeof data === 'number' ? data : null
}
