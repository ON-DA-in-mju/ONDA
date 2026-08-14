import { isSupabaseConfigured, supabase } from './supabase'

export type RouteOption = {
  id: string
  name: string
}

export const ALL_ROUTES = '__all_routes__'

export async function fetchRouteOptions(): Promise<RouteOption[]> {
  if (!isSupabaseConfigured) return []
  const { data, error } = await supabase.from('routes').select('id, route_name').order('route_name', { ascending: true })
  if (error) {
    console.warn('[suspend] routes', error.message)
    return []
  }
  return (data ?? []).map((r) => ({ id: r.id, name: r.route_name }))
}

type OpSuspendRow = {
  id: string
  operation_date: string
  bus_id: string
  status: string
  schedule_id: string
  schedules: { departure_time: string | null; route_id: string | null } | null
}

function parseDepartLocal(operationDate: string, departureTime: string | null): Date | null {
  if (!operationDate || !departureTime) return null
  const hm = departureTime.slice(0, 5)
  const d = new Date(`${operationDate}T${hm}:00`)
  return Number.isNaN(d.getTime()) ? null : d
}

function unwrapSchedule(
  value: OpSuspendRow['schedules'] | OpSuspendRow['schedules'][] | null,
): { departure_time: string | null; route_id: string | null } | null {
  if (!value) return null
  return Array.isArray(value) ? value[0] ?? null : value
}

async function markRoutesInactive(routeId: string, untilIso: string) {
  const withUntil = { is_active: false, suspended_until: untilIso }
  const first =
    routeId === ALL_ROUTES
      ? await supabase.from('routes').update(withUntil)
      : await supabase.from('routes').update(withUntil).eq('id', routeId)
  if (first.error && /suspended_until/i.test(first.error.message)) {
    const fallback = { is_active: false }
    return routeId === ALL_ROUTES
      ? supabase.from('routes').update(fallback)
      : supabase.from('routes').update(fallback).eq('id', routeId)
  }
  return first
}

/**
 * 선택 노선 + 시작~종료 시각의 SCHEDULED 배차(미배정·이미 배정 포함)를 CANCELLED.
 * IN_PROGRESS(현재 운행 중)는 그대로 둔다.
 * 대상 노선은 routes.is_active=false (노선 관리: 운행 불가). 종료 시각이 지나면 자동 복구.
 */
export async function suspendOperationsInRange(params: {
  routeId: string
  startIso: string
  endIso: string
  reason: string
}): Promise<{ ok: boolean; message?: string; count?: number }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }

  const start = new Date(params.startIso)
  const end = new Date(params.endIso)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return { ok: false, message: '시작 시각과 종료 시각을 확인해 주세요.' }
  }
  if (end < start) {
    return { ok: false, message: '종료 시각은 시작 시각 이후여야 합니다.' }
  }
  if (!params.routeId || params.routeId === '-') {
    return { ok: false, message: '노선을 선택해 주세요.' }
  }

  const untilIso = end.toISOString()

  let scheduleIds: string[] | null = null
  if (params.routeId !== ALL_ROUTES) {
    const { data: scheds, error: schedErr } = await supabase.from('schedules').select('id').eq('route_id', params.routeId)
    if (schedErr) return { ok: false, message: schedErr.message }
    scheduleIds = (scheds ?? []).map((s) => s.id)
    if (!scheduleIds.length) {
      const { error: routeErr } = await markRoutesInactive(params.routeId, untilIso)
      if (routeErr) return { ok: false, message: routeErr.message }
      return {
        ok: true,
        count: 0,
        message: '해당 노선을 운행 불가로 표시했습니다. 취소할 예정 배차는 없습니다. 종료 시각이 지나면 자동으로 운행 가능으로 돌아갑니다.',
      }
    }
  }

  let query = supabase
    .from('operations')
    .select('id, operation_date, bus_id, status, schedule_id, schedules:schedule_id(departure_time, route_id)')
    .eq('status', 'SCHEDULED')

  if (scheduleIds) query = query.in('schedule_id', scheduleIds)

  const { data, error } = await query
  if (error) return { ok: false, message: error.message }

  const rows = (data ?? []) as unknown as OpSuspendRow[]
  const matched = rows.filter((row) => {
    const schedule = unwrapSchedule(row.schedules)
    const depart = parseDepartLocal(row.operation_date, schedule?.departure_time ?? null)
    if (!depart) return false
    return depart >= start && depart <= end
  })
  const targetIds = matched.map((r) => r.id)

  let count = 0
  if (targetIds.length) {
    const { data: updated, error: updErr } = await supabase
      .from('operations')
      .update({ status: 'CANCELLED' })
      .in('id', targetIds)
      .eq('status', 'SCHEDULED')
      .select('id')

    if (updErr) return { ok: false, message: updErr.message }
    count = updated?.length ?? 0
    if (count > 0) {
      await supabase.from('operation_logs').insert(
        (updated ?? []).map((u) => ({
          operation_id: u.id,
          event_type: 'STATUS_CHANGED' as const,
          log_message: `ADMIN_SUSPEND reason=${params.reason}`,
        })),
      )
    }
  }

  const { error: routeErr } = await markRoutesInactive(params.routeId, untilIso)
  if (routeErr) return { ok: false, message: routeErr.message }
  const routeNote =
    params.routeId === ALL_ROUTES
      ? ' 전체 노선을 운행 불가로 표시했습니다. 종료 시각이 지나면 자동으로 운행 가능으로 돌아갑니다.'
      : ' 선택한 노선을 운행 불가로 표시했습니다. 종료 시각이 지나면 자동으로 운행 가능으로 돌아갑니다.'

  const runningNote = ' 운행 중인 배차는 중단하지 않았습니다.'
  if (count === 0) {
    return {
      ok: true,
      count: 0,
      message: `해당 구간에 취소할 예정 배차는 없습니다.${routeNote}${runningNote}`,
    }
  }
  return {
    ok: true,
    count,
    message: `${count}건의 예정·배정 배차를 운행 불가로 처리했습니다.${routeNote}${runningNote}`,
  }
}

function todayDateKey(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/**
 * 노선을 다시 운행 가능으로 돌릴 때, 운행 중단으로 CANCELLED 된 오늘 이후 배차를 SCHEDULED 로 복구.
 * 기사·차량·시각은 그대로 둔다. 이미 출발했거나 지난 날짜 배차는 건드리지 않는다.
 */
export async function restoreSuspendedOperationsForRoute(
  routeId: string,
): Promise<{ ok: boolean; count?: number; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  if (!routeId) return { ok: false, message: '노선이 없습니다.' }

  const { data: scheds, error: schedErr } = await supabase.from('schedules').select('id').eq('route_id', routeId)
  if (schedErr) return { ok: false, message: schedErr.message }
  const scheduleIds = (scheds ?? []).map((s) => s.id)
  if (!scheduleIds.length) return { ok: true, count: 0 }

  const { data: cancelled, error: opErr } = await supabase
    .from('operations')
    .select('id')
    .eq('status', 'CANCELLED')
    .in('schedule_id', scheduleIds)
    .gte('operation_date', todayDateKey())
    .is('started_at', null)

  if (opErr) return { ok: false, message: opErr.message }
  const ids = (cancelled ?? []).map((row) => row.id)
  if (!ids.length) return { ok: true, count: 0 }

  const { data: logs } = await supabase
    .from('operation_logs')
    .select('operation_id, log_message')
    .in('operation_id', ids)
    .eq('event_type', 'STATUS_CHANGED')

  const suspendIds = new Set(
    (logs ?? [])
      .filter((row) => (row.log_message ?? '').includes('ADMIN_SUSPEND'))
      .map((row) => row.operation_id),
  )
  const restoreIds = ids.filter((id) => suspendIds.has(id))
  if (!restoreIds.length) return { ok: true, count: 0 }

  const { data: updated, error: updErr } = await supabase
    .from('operations')
    .update({ status: 'SCHEDULED' })
    .in('id', restoreIds)
    .eq('status', 'CANCELLED')
    .select('id')

  if (updErr) return { ok: false, message: updErr.message }
  const count = updated?.length ?? 0
  if (count > 0) {
    await supabase.from('operation_logs').insert(
      (updated ?? []).map((u) => ({
        operation_id: u.id,
        event_type: 'STATUS_CHANGED' as const,
        log_message: 'ADMIN_RESUME',
      })),
    )
  }
  return { ok: true, count }
}

let lastExpireAt = 0

/**
 * 운행 중단 종료 시각이 지난 노선을 운행 가능으로 되돌리고, 취소 배차를 복구한다.
 */
export async function expireSuspendedRoutes(force = false): Promise<string[]> {
  if (!isSupabaseConfigured) return []
  const now = Date.now()
  if (!force && now - lastExpireAt < 5_000) return []
  lastExpireAt = now

  const rpc = await supabase.rpc('expire_suspended_routes')
  if (!rpc.error) {
    const ids = Array.isArray(rpc.data) ? (rpc.data as string[]) : []
    return ids.filter(Boolean)
  }

  const { data, error } = await supabase
    .from('routes')
    .select('id')
    .eq('is_active', false)
    .not('suspended_until', 'is', null)
    .lte('suspended_until', new Date().toISOString())

  if (error || !data?.length) return []
  const ids = data.map((row) => row.id)
  const { error: updErr } = await supabase
    .from('routes')
    .update({ is_active: true, suspended_until: null })
    .in('id', ids)
  if (updErr) return []

  for (const id of ids) {
    await restoreSuspendedOperationsForRoute(id)
  }
  return ids
}
