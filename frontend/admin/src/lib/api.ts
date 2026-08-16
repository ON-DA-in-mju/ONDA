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

export async function fetchNotices(): Promise<{ rows: NoticeRow[]; error?: string }> {
  if (!isSupabaseConfigured) {
    return { rows: [], error: 'Supabase가 설정되지 않았습니다.' }
  }
  const { data, error } = await supabase.from('notices').select('*').order('created_at', { ascending: false })
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
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
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

/** 공지 완전 삭제 — notices 행 및 cascade 된 조회 이력까지 제거 */
export async function deleteNotice(id: string): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  if (!id.trim()) return { ok: false, message: '삭제할 공지를 선택하세요.' }

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
  if (!isSupabaseConfigured) return null
  // 관리자 제보 관리: 학생 상황 제보(REPORT)만. 소통 글(POST)·기사 문의 제외.
  const studentOnly = await supabase
    .from('reports')
    .select('*')
    .eq('board_type', 'REPORT')
    .eq('source', 'STUDENT')
    .order('created_at', { ascending: false })
  if (!studentOnly.error) return studentOnly.data as ReportRow[]

  console.warn('[reports] student filter failed, fallback:', studentOnly.error.message)
  const reportOnly = await supabase
    .from('reports')
    .select('*')
    .eq('board_type', 'REPORT')
    .order('created_at', { ascending: false })
  if (!reportOnly.error) return reportOnly.data as ReportRow[]

  console.warn('[reports] board_type filter failed, fallback:', reportOnly.error?.message)
  return selectAll<ReportRow>('reports')
}

/** 학생 상황 제보 전체 건수 (대시보드 KPI) */
export async function countStudentReports(): Promise<{
  total: number
  pending: number
} | null> {
  if (!isSupabaseConfigured) return null

  const tryCount = async (filters: { boardType?: boolean; source?: boolean }) => {
    let q = supabase.from('reports').select('*', { count: 'exact', head: true })
    if (filters.boardType) q = q.eq('board_type', 'REPORT')
    if (filters.source) q = q.eq('source', 'STUDENT')
    return q
  }

  const attempts = [
    { boardType: true, source: true },
    { boardType: true, source: false },
    { boardType: false, source: true },
    { boardType: false, source: false },
  ] as const

  let total: number | null = null
  for (const filters of attempts) {
    const { count, error } = await tryCount(filters)
    if (!error) {
      total = count ?? 0
      break
    }
  }
  if (total == null) return null

  let pending = 0
  const pendingAttempts = [
    { boardType: true, source: true },
    { boardType: true, source: false },
    { boardType: false, source: false },
  ] as const
  for (const filters of pendingAttempts) {
    let q = supabase
      .from('reports')
      .select('*', { count: 'exact', head: true })
      .eq('status', 'PENDING')
    if (filters.boardType) q = q.eq('board_type', 'REPORT')
    if (filters.source) q = q.eq('source', 'STUDENT')
    const { count, error } = await q
    if (!error) {
      pending = count ?? 0
      break
    }
  }

  return { total, pending }
}

/** reports INSERT/UPDATE/DELETE Realtime 구독 */
export function subscribeStudentReports(onChange: () => void): () => void {
  if (!isSupabaseConfigured) return () => {}

  const channel = supabase
    .channel(`onda-admin-reports-${Date.now()}`)
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: 'reports' },
      () => {
        onChange()
      },
    )
    .subscribe((status) => {
      if (status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') {
        console.warn('[reports] realtime', status)
      }
    })

  return () => {
    void supabase.removeChannel(channel)
  }
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

export async function fetchReportById(id: string): Promise<ReportRow | null> {
  if (!isSupabaseConfigured) return null
  const { data, error } = await supabase.from('reports').select('*').eq('id', id).maybeSingle()
  if (error || !data) return null
  return data as ReportRow
}

export async function deleteReport(id: string): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const { error } = await supabase.from('reports').delete().eq('id', id)
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}
