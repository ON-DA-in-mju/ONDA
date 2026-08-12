import { isSupabaseConfigured, supabase } from './supabase'
import type { Database } from '../types/database'
import {
  isMissingAudienceColumnError,
  MISSING_AUDIENCE_COLUMN_MESSAGE,
  normalizeNoticeAudience,
} from './noticeAudience'

export type NoticeRow = Database['public']['Tables']['notices']['Row']
export type UserRow = Database['public']['Tables']['users']['Row']
export type RouteRow = Database['public']['Tables']['routes']['Row']
export type BusRow = Database['public']['Tables']['buses']['Row']
export type ReportRow = Database['public']['Tables']['reports']['Row']
export type StopRow = Database['public']['Tables']['stops']['Row']
export type ScheduleRow = Database['public']['Tables']['schedules']['Row']

async function selectAll<T>(table: keyof Database['public']['Tables'], order = 'created_at'): Promise<T[] | null> {
  if (!isSupabaseConfigured) return null
  const { data, error } = await supabase.from(table).select('*').order(order, { ascending: false })
  if (error) {
    console.error(`[${table}]`, error.message)
    return null
  }
  return data as T[]
}

export async function fetchNotices(): Promise<NoticeRow[] | null> {
  const rows = await selectAll<NoticeRow>('notices')
  if (!rows) return null
  return rows.map((row) => ({
    ...row,
    audience: normalizeNoticeAudience((row as NoticeRow & { audience?: unknown }).audience),
  }))
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
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const { error } = await supabase.from('notices').insert({
    title: payload.title,
    content: payload.content,
    author_id: payload.author_id ?? null,
    type: payload.type,
    audience: payload.audience,
    starts_at: payload.starts_at ?? null,
    ends_at: payload.ends_at ?? null,
    is_push: payload.is_push ?? false,
    status: payload.status ?? 'PUBLISHED',
  })
  if (error) {
    return {
      ok: false,
      message: isMissingAudienceColumnError(error.message)
        ? MISSING_AUDIENCE_COLUMN_MESSAGE
        : error.message,
    }
  }
  return { ok: true }
}

/** 공지 삭제 = hard delete 아님. status=ENDED 로 앱 비노출, 관리자 목록에는 종료로 남김 */
export async function endNotice(id: string): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  if (!id.trim()) return { ok: false, message: '삭제할 공지를 선택하세요.' }

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
  // ends_at >= starts_at 제약: 예약(미래 시작) 공지는 ends_at 을 starts_at 이상으로 맞춤
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
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  if (!id.trim()) return { ok: false, message: '수정할 공지를 선택하세요.' }

  const { data, error } = await supabase
    .from('notices')
    .update({
      title: payload.title,
      content: payload.content,
      type: payload.type,
      audience: payload.audience,
      starts_at: payload.starts_at ?? null,
      ends_at: payload.ends_at ?? null,
      is_push: payload.is_push ?? false,
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

export async function fetchUsers(): Promise<UserRow[] | null> {
  return selectAll<UserRow>('users')
}

export async function fetchRoutes(): Promise<RouteRow[] | null> {
  return selectAll<RouteRow>('routes')
}

export async function fetchBuses(): Promise<BusRow[] | null> {
  return selectAll<BusRow>('buses')
}

export async function fetchReports(): Promise<ReportRow[] | null> {
  return selectAll<ReportRow>('reports')
}

export async function fetchStops(): Promise<StopRow[] | null> {
  return selectAll<StopRow>('stops')
}

export async function fetchSchedules(): Promise<ScheduleRow[] | null> {
  return selectAll<ScheduleRow>('schedules')
}

export async function updateReportStatus(
  id: string,
  status: Database['public']['Tables']['reports']['Row']['status'],
): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const { error } = await supabase.from('reports').update({ status }).eq('id', id)
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}
