import { isSupabaseConfigured, supabase } from './supabase'
import type { AssignmentStatus, TodayAssignment } from '../types/assignment'
import { DRIVER_OPTIONS, todayDateKey } from '../types/assignment'
import type { OperationStatus as DbOperationStatus, Weekday } from '../types/database'
import type { Database } from '../types/database'

const OPS_SELECT = `
  id,
  external_id,
  operation_date,
  status,
  round,
  origin,
  destination,
  expected_end_time,
  driver_id,
  bus_id,
  schedule_id,
  users:driver_id ( id, name, login_id, email ),
  buses:bus_id ( bus_name ),
  schedules:schedule_id ( departure_time, routes:route_id ( route_name ) )
`

type OpRow = {
  id: string
  external_id: string | null
  operation_date: string
  status: string
  round: number | null
  origin: string | null
  destination: string | null
  expected_end_time: string | null
  driver_id: string
  bus_id: string
  schedule_id: string
  users: { id: string; name: string; login_id: string | null; email: string } | null
  buses: { bus_name: string } | null
  schedules: {
    departure_time: string
    routes: { route_name: string } | null
  } | null
}

function hhmm(value: string | null | undefined): string {
  if (!value) return ''
  return value.slice(0, 5)
}

function mapDbStatus(status: string): AssignmentStatus {
  switch (status) {
    case 'IN_PROGRESS':
      return 'in_progress'
    case 'COMPLETED':
    case 'CANCELLED':
      return 'ended'
    default:
      return 'scheduled'
  }
}

function mapUiStatusToDb(status: AssignmentStatus | undefined): DbOperationStatus | undefined {
  if (!status) return undefined
  switch (status) {
    case 'in_progress':
      return 'IN_PROGRESS'
    case 'ended':
      return 'COMPLETED'
    default:
      return 'SCHEDULED'
  }
}

function rowToAssignment(row: OpRow): TodayAssignment {
  return {
    id: row.external_id || row.id,
    date: row.operation_date,
    driverId: row.users?.login_id || row.driver_id,
    driverName: row.users?.name || '',
    routeName: row.schedules?.routes?.route_name || '',
    vehicleName: row.buses?.bus_name || '',
    departTime: hhmm(row.schedules?.departure_time),
    expectedEndTime: hhmm(row.expected_end_time),
    origin: row.origin || '',
    destination: row.destination || '',
    round: row.round ?? 1,
    status: mapDbStatus(row.status),
  }
}

function scheduleWeekday(date: string): Weekday {
  const d = new Date(`${date}T12:00:00`)
  const names: Weekday[] = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
  const w = names[d.getDay()]
  return w === 'SAT' || w === 'SUN' ? 'MON' : w
}

async function resolveDriverId(loginOrUuid: string): Promise<string | null> {
  if (loginOrUuid.includes('-') && loginOrUuid.length > 20) return loginOrUuid
  const { data, error } = await supabase
    .from('users')
    .select('id')
    .eq('login_id', loginOrUuid)
    .maybeSingle()
  if (error || !data?.id) return null
  return data.id
}

async function resolveBusId(vehicleName: string): Promise<string | null> {
  const { data, error } = await supabase
    .from('buses')
    .select('id')
    .eq('bus_name', vehicleName)
    .maybeSingle()
  if (error || !data?.id) return null
  return data.id
}

async function resolveScheduleId(routeName: string, departTime: string, date: string): Promise<string | null> {
  const { data: route, error: routeErr } = await supabase
    .from('routes')
    .select('id')
    .eq('route_name', routeName)
    .maybeSingle()
  if (routeErr || !route?.id) return null

  const depart = departTime.length === 5 ? `${departTime}:00` : departTime
  const weekday = scheduleWeekday(date)
  const { data: sch, error } = await supabase
    .from('schedules')
    .select('id')
    .eq('route_id', route.id)
    .eq('departure_time', depart)
    .eq('weekday', weekday)
    .eq('semester', 'SEMESTER')
    .maybeSingle()
  if (error || !sch?.id) return null
  return sch.id
}

async function findOpUuid(idOrExternal: string): Promise<string | null> {
  const byId = await supabase.from('operations').select('id').eq('id', idOrExternal).maybeSingle()
  if (byId.data?.id) return byId.data.id
  const byExt = await supabase
    .from('operations')
    .select('id')
    .eq('external_id', idOrExternal)
    .maybeSingle()
  return byExt.data?.id ?? null
}

export async function fetchAssignments(params?: {
  date?: string
  driverId?: string
}): Promise<TodayAssignment[]> {
  if (!isSupabaseConfigured) return []

  const date = params?.date ?? todayDateKey()
  let query = supabase.from('operations').select(OPS_SELECT).eq('operation_date', date)

  if (params?.driverId) {
    const driverUuid = await resolveDriverId(params.driverId)
    if (!driverUuid) return []
    query = query.eq('driver_id', driverUuid)
  }

  const { data, error } = await query.order('expected_end_time', { ascending: true })
  if (error) {
    console.error('[assignments]', error.message)
    return []
  }
  const mapped = ((data ?? []) as unknown as OpRow[]).map(rowToAssignment)
  // 출발시각 기준 정렬 (schedules.departure_time)
  return mapped.sort((a, b) => a.departTime.localeCompare(b.departTime) || a.routeName.localeCompare(b.routeName))
}

export async function createAssignment(
  payload: Omit<TodayAssignment, 'id'> & { id?: string },
): Promise<{ ok: boolean; entry?: TodayAssignment; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }

  const driverId = await resolveDriverId(payload.driverId)
  if (!driverId) return { ok: false, message: `기사 없음: ${payload.driverId}` }

  const busId = await resolveBusId(payload.vehicleName)
  if (!busId) return { ok: false, message: `차량 없음: ${payload.vehicleName} (buses.bus_name)` }

  const scheduleId = await resolveScheduleId(payload.routeName, payload.departTime, payload.date)
  if (!scheduleId) {
    return {
      ok: false,
      message: `스케줄 없음: ${payload.routeName} ${payload.departTime} (schedules에 해당 시각이 있어야 함)`,
    }
  }

  const externalId =
    payload.id?.trim() ||
    `op-${payload.driverId}-${payload.departTime.replace(':', '')}-${Date.now().toString(36)}`

  const insert: Database['public']['Tables']['operations']['Insert'] = {
    schedule_id: scheduleId,
    driver_id: driverId,
    bus_id: busId,
    operation_date: payload.date,
    status: mapUiStatusToDb(payload.status) ?? 'SCHEDULED',
    external_id: externalId,
    round: payload.round ?? 1,
    origin: payload.origin,
    destination: payload.destination,
    expected_end_time: payload.expectedEndTime.length === 5 ? `${payload.expectedEndTime}:00` : payload.expectedEndTime,
  }

  const { data, error } = await supabase.from('operations').insert(insert).select(OPS_SELECT).maybeSingle()
  if (error) return { ok: false, message: error.message }
  if (!data) return { ok: false, message: 'insert 결과 없음' }
  return { ok: true, entry: rowToAssignment(data as unknown as OpRow) }
}

export async function updateAssignment(
  id: string,
  patch: Partial<TodayAssignment>,
): Promise<{ ok: boolean; entry?: TodayAssignment; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }

  const uuid = await findOpUuid(id)
  if (!uuid) return { ok: false, message: '배차 없음' }

  const dbPatch: Database['public']['Tables']['operations']['Update'] = {}
  if (patch.status) dbPatch.status = mapUiStatusToDb(patch.status)
  if (patch.origin != null) dbPatch.origin = patch.origin
  if (patch.destination != null) dbPatch.destination = patch.destination
  if (patch.round != null) dbPatch.round = patch.round
  if (patch.expectedEndTime != null) {
    dbPatch.expected_end_time =
      patch.expectedEndTime.length === 5 ? `${patch.expectedEndTime}:00` : patch.expectedEndTime
  }
  if (patch.driverId) {
    const nextDriverId = await resolveDriverId(patch.driverId)
    if (!nextDriverId) return { ok: false, message: `기사 없음: ${patch.driverId}` }
    dbPatch.driver_id = nextDriverId
  }
  if (patch.vehicleName) {
    const busId = await resolveBusId(patch.vehicleName)
    if (!busId) return { ok: false, message: `차량 없음: ${patch.vehicleName}` }
    dbPatch.bus_id = busId
  }
  if (patch.status === 'in_progress') dbPatch.started_at = new Date().toISOString()
  if (patch.status === 'ended') dbPatch.ended_at = new Date().toISOString()

  const { data, error } = await supabase
    .from('operations')
    .update(dbPatch)
    .eq('id', uuid)
    .select(OPS_SELECT)
    .maybeSingle()
  if (error) return { ok: false, message: error.message }
  if (!data) return { ok: false, message: 'update 결과 없음' }
  return { ok: true, entry: rowToAssignment(data as unknown as OpRow) }
}

export async function deleteAssignment(id: string): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const uuid = await findOpUuid(id)
  if (!uuid) return { ok: false, message: '배차 없음' }
  const { error } = await supabase.from('operations').delete().eq('id', uuid)
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}

export type DriverOption = { id: string; name: string }

/** 기사 역할 사용자 — login_id 기준 (없으면 email local-part) */
export async function fetchDriverOptions(): Promise<DriverOption[]> {
  const fallback = DRIVER_OPTIONS.map((d) => ({ id: d.id, name: d.name }))
  if (!isSupabaseConfigured) return fallback

  const { data, error } = await supabase
    .from('users')
    .select('login_id, name, email')
    .eq('role', 'DRIVER')
    .order('name', { ascending: true })

  if (error) {
    console.error('[drivers]', error.message)
    return fallback
  }

  const rows = (data ?? [])
    .map((u) => {
      const id = (u.login_id || u.email?.split('@')[0] || '').trim()
      if (!id) return null
      return { id, name: u.name || id }
    })
    .filter((x): x is DriverOption => Boolean(x))

  return rows.length ? rows : fallback
}

/** 기존 배차에 기사만 변경 */
export async function assignDriverToOperation(
  operationId: string,
  driverLoginId: string,
): Promise<{ ok: boolean; entry?: TodayAssignment; message?: string }> {
  return updateAssignment(operationId, { driverId: driverLoginId })
}
