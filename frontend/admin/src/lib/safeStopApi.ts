import { isSupabaseConfigured, supabase } from './supabase'
import { todayDateKey } from '../types/assignment'
import type { Database } from '../types/database'

export type SafeStopDecision = 'pending' | 'continue' | 'stop' | 'cancelled'

export type SafeStopRequest = {
  id: string
  driverId: string
  driverName: string
  vehicleName: string
  routeName: string
  operationId: string
  reason: string
  detailReason: string
  requestedAt: string
  date: string
  decision: SafeStopDecision
  createdAt: number
  decidedAt?: number
}

type SafeStopRow = Database['public']['Tables']['safe_stop_requests']['Row'] & {
  users: { id: string; name: string; login_id: string | null } | null
  operations: {
    id: string
    external_id: string | null
    operation_date: string
    buses: { bus_name: string } | null
    schedules: { routes: { route_name: string } | null } | null
  } | null
}

const SELECT = `
  id,
  operation_id,
  driver_id,
  reason,
  detail_reason,
  decision,
  requested_at,
  decided_at,
  created_at,
  users:driver_id ( id, name, login_id ),
  operations:operation_id (
    id,
    external_id,
    operation_date,
    buses:bus_id ( bus_name ),
    schedules:schedule_id ( routes:route_id ( route_name ) )
  )
`

function hhmm(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) {
    // already HH:mm
    return iso.slice(0, 5)
  }
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

function rowToRequest(row: SafeStopRow): SafeStopRequest {
  const op = row.operations
  return {
    id: row.id,
    driverId: row.users?.login_id || row.driver_id,
    driverName: row.users?.name || '',
    vehicleName: op?.buses?.bus_name || '미정',
    routeName: op?.schedules?.routes?.route_name || '-',
    operationId: op?.external_id || op?.id || row.operation_id || '',
    reason: row.reason,
    detailReason: row.detail_reason || '',
    requestedAt: hhmm(row.requested_at),
    date: op?.operation_date || todayDateKey(),
    decision: row.decision,
    createdAt: row.created_at ? Date.parse(row.created_at) : Date.now(),
    decidedAt: row.decided_at ? Date.parse(row.decided_at) : undefined,
  }
}

async function fetchFromDevApi(params?: {
  driverId?: string
  pendingOnly?: boolean
}): Promise<SafeStopRequest[]> {
  try {
    const qs = new URLSearchParams()
    if (params?.driverId) qs.set('driverId', params.driverId)
    if (params?.pendingOnly) qs.set('pending', '1')
    const res = await fetch(`/api/safe-stop?${qs}`, { cache: 'no-store' })
    if (!res.ok) return []
    const data = (await res.json()) as SafeStopRequest[]
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

export async function fetchSafeStopRequests(params?: {
  driverId?: string
  pendingOnly?: boolean
  limit?: number
}): Promise<SafeStopRequest[]> {
  if (!isSupabaseConfigured) return fetchFromDevApi(params)

  let q = supabase.from('safe_stop_requests').select(SELECT).order('created_at', { ascending: false })

  if (params?.pendingOnly) q = q.eq('decision', 'pending')
  if (params?.limit != null) q = q.limit(params.limit)
  else q = q.limit(50)
  if (params?.driverId) {
    // login_id 또는 uuid
    if (params.driverId.includes('-') && params.driverId.length > 20) {
      q = q.eq('driver_id', params.driverId)
    } else {
      const { data: user } = await supabase
        .from('users')
        .select('id')
        .eq('login_id', params.driverId)
        .maybeSingle()
      if (!user?.id) return []
      q = q.eq('driver_id', user.id)
    }
  }

  const { data, error } = await q
  if (error) {
    console.error('[safe_stop]', error.message)
    return []
  }
  return ((data ?? []) as unknown as SafeStopRow[]).map(rowToRequest)
}

export async function decideSafeStop(
  id: string,
  decision: 'continue' | 'stop' | 'cancelled',
): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) {
    try {
      const res = await fetch(`/api/safe-stop/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ decision }),
      })
      const data = (await res.json()) as { ok?: boolean; message?: string }
      if (!res.ok) return { ok: false, message: data.message || 'failed' }
      return { ok: true }
    } catch {
      return { ok: false, message: 'network error' }
    }
  }

  const { data: current, error: findErr } = await supabase
    .from('safe_stop_requests')
    .select('id, decision, operation_id')
    .eq('id', id)
    .maybeSingle()

  if (findErr) return { ok: false, message: findErr.message }
  if (!current) return { ok: false, message: '요청을 찾을 수 없습니다' }
  if (current.decision !== 'pending') return { ok: false, message: '이미 처리된 요청입니다' }

  const { error } = await supabase
    .from('safe_stop_requests')
    .update({
      decision,
      decided_at: new Date().toISOString(),
    })
    .eq('id', id)

  if (error) return { ok: false, message: error.message }

  // 중단 승인 시 operations를 즉시 COMPLETED로 바꾸지 않음
  // → 실시간 목록에서는 계속 '안전 정차'로 보이고, 기사 측 종료 처리 시 완료로 전환
  if (decision === 'continue' && current.operation_id) {
    // 계속 운행: 운행 중 유지(이미 IN_PROGRESS면 변경 없음)
    const { error: opErr } = await supabase
      .from('operations')
      .update({
        status: 'IN_PROGRESS',
        ended_at: null,
      })
      .eq('id', current.operation_id)
    if (opErr) console.warn('[safe_stop] operation resume', opErr.message)
  }

  return { ok: true }
}
