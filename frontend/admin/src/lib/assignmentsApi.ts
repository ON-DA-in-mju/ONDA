import { isSupabaseConfigured, supabase } from './supabase'
import type { AssignmentStatus, TodayAssignment } from '../types/assignment'
import { DRIVER_OPTIONS, todayDateKey } from '../types/assignment'
import type { OperationStatus as DbOperationStatus, Weekday } from '../types/database'
import type { Database } from '../types/database'
import { semesterForDate } from './academicCalendar'
import { resolveOperationalRouteName } from './routeVariants'

const OPS_SELECT = `
  id,
  external_id,
  operation_date,
  status,
  round,
  expected_end_time,
  driver_id,
  bus_id,
  schedule_id,
  users:driver_id ( id, name, login_id, email ),
  buses:bus_id ( bus_name ),
  schedules:schedule_id ( departure_time, routes:route_id ( route_name ) ),
  origin_stop:origin_stop_id ( stop_name ),
  destination_stop:destination_stop_id ( stop_name )
`

type OpRow = {
  id: string
  external_id: string | null
  operation_date: string
  status: string
  round: number | null
  expected_end_time: string | null
  driver_id: string | null
  bus_id: string
  schedule_id: string
  users: { id: string; name: string; login_id: string | null; email: string } | null
  buses: { bus_name: string } | null
  schedules: {
    departure_time: string
    routes: { route_name: string } | null
  } | null
  origin_stop: { stop_name: string } | null
  destination_stop: { stop_name: string } | null
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
    driverId: row.users?.login_id || row.driver_id || '',
    driverName: row.users?.name || '',
    routeName: row.schedules?.routes?.route_name || '',
    vehicleName: row.buses?.bus_name || '',
    departTime: hhmm(row.schedules?.departure_time),
    expectedEndTime: hhmm(row.expected_end_time),
    origin: row.origin_stop?.stop_name || '',
    destination: row.destination_stop?.stop_name || '',
    round: row.round ?? 1,
    status: mapDbStatus(row.status),
  }
}

function weekdayOfDate(date: string): Weekday {
  const d = new Date(`${date}T12:00:00`)
  const names: Weekday[] = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
  return names[d.getDay()] ?? 'MON'
}

function toMinutes(value: string | null | undefined): number | null {
  const t = hhmm(value)
  if (!/^\d{2}:\d{2}$/.test(t)) return null
  const [h, m] = t.split(':').map(Number)
  if (!Number.isFinite(h) || !Number.isFinite(m)) return null
  return h * 60 + m
}

function minutesToHm(total: number): string {
  const wrapped = ((total % (24 * 60)) + 24 * 60) % (24 * 60)
  const h = Math.floor(wrapped / 60)
  const m = wrapped % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/** [start, end) 구간이 겹치면 true. 끝점만 맞닿으면 겹치지 않음. */
function intervalsOverlap(aStart: number, aEnd: number, bStart: number, bEnd: number): boolean {
  return aStart < bEnd && bStart < aEnd
}

function driverNullableHint(error: { code?: string; message: string }): string {
  const msg = error.message || ''
  if (error.code === '23502' || /null value .+ driver_id/i.test(msg) || /driver_id .+ not-null/i.test(msg)) {
    return 'operations.driver_id 가 NOT NULL 입니다. migrate_operations_driver_nullable.sql 을 실행해 주세요.'
  }
  return msg
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
  const resolvedName = resolveOperationalRouteName({
    baseRouteName: routeName,
    departureTime: departTime,
    date,
  })
  const namesToTry = [...new Set([resolvedName, routeName])]
  const depart = departTime.length === 5 ? `${departTime}:00` : departTime
  const weekday = weekdayOfDate(date)
  const semester = semesterForDate(date)

  for (const name of namesToTry) {
    const { data: route, error: routeErr } = await supabase
      .from('routes')
      .select('id')
      .eq('route_name', name)
      .maybeSingle()
    if (routeErr || !route?.id) continue

    const withSemester = await supabase
      .from('schedules')
      .select('id')
      .eq('route_id', route.id)
      .eq('departure_time', depart)
      .eq('weekday', weekday)
      .eq('semester', semester)
      .maybeSingle()
    if (withSemester.data?.id) return withSemester.data.id

    const anySemester = await supabase
      .from('schedules')
      .select('id')
      .eq('route_id', route.id)
      .eq('departure_time', depart)
      .eq('weekday', weekday)
      .maybeSingle()
    if (anySemester.data?.id) return anySemester.data.id
  }
  return null
}

async function resolveStopId(stopName: string): Promise<string | null> {
  const name = stopName.trim()
  if (!name) return null
  const { data, error } = await supabase.from('stops').select('id').eq('stop_name', name).maybeSingle()
  if (error || !data?.id) return null
  return data.id
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

type ConflictOpRow = {
  id: string
  expected_end_time: string | null
  status: string
  schedules: { departure_time: string } | { departure_time: string }[] | null
}

function scheduleDepartFromEmbed(schedules: ConflictOpRow['schedules']): string {
  if (!schedules) return ''
  if (Array.isArray(schedules)) return hhmm(schedules[0]?.departure_time)
  return hhmm(schedules.departure_time)
}

/**
 * 같은 날, 같은 기사의 출발~종료가 겹치면 안내 문구.
 * 끝점만 맞닿는 경우(09:00 종료 / 09:00 출발)는 허용.
 */
async function findDriverTimeConflict(params: {
  driverUuid: string
  date: string
  departTime: string
  expectedEndTime: string
  excludeOpId?: string
}): Promise<string | null> {
  const start = toMinutes(params.departTime)
  const end = toMinutes(params.expectedEndTime)
  if (start == null || end == null) {
    return '출발·종료 시각이 없어 시간 겹침을 확인할 수 없습니다. 운행의 종료 시각을 먼저 지정해 주세요.'
  }
  if (end <= start) {
    return '종료 시각이 출발 시각보다 이후여야 합니다.'
  }

  const { data, error } = await supabase
    .from('operations')
    .select('id, expected_end_time, status, schedules:schedule_id ( departure_time )')
    .eq('operation_date', params.date)
    .eq('driver_id', params.driverUuid)
    .neq('status', 'CANCELLED')

  if (error) {
    console.error('[assignments] overlap', error.message)
    return `시간 겹침 확인 실패: ${error.message}`
  }

  for (const row of (data ?? []) as unknown as ConflictOpRow[]) {
    if (params.excludeOpId && row.id === params.excludeOpId) continue
    const otherStart = toMinutes(scheduleDepartFromEmbed(row.schedules))
    if (otherStart == null) continue
    const otherEndRaw = toMinutes(row.expected_end_time)
    const otherEnd = otherEndRaw ?? otherStart + 60
    if (intervalsOverlap(start, end, otherStart, otherEnd)) {
      return `같은 날 이 기사님의 운행 시간이 겹칩니다. (${minutesToHm(otherStart)}–${minutesToHm(otherEnd)})`
    }
  }
  return null
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
  return mapped.sort((a, b) => a.departTime.localeCompare(b.departTime) || a.routeName.localeCompare(b.routeName))
}

export async function fetchAssignmentsInRange(
  startDate: string,
  endDate: string,
): Promise<TodayAssignment[]> {
  if (!isSupabaseConfigured) return []
  const { data, error } = await supabase
    .from('operations')
    .select(OPS_SELECT)
    .gte('operation_date', startDate)
    .lte('operation_date', endDate)
  if (error) {
    console.error('[assignments] range', error.message)
    return []
  }
  const mapped = ((data ?? []) as unknown as OpRow[]).map(rowToAssignment)
  return mapped.sort(
    (a, b) => a.date.localeCompare(b.date) || a.departTime.localeCompare(b.departTime) || a.routeName.localeCompare(b.routeName),
  )
}

export type RouteStopOption = {
  id: string
  name: string
  order: number
  expectedMinutes: number | null
}

export async function fetchRouteStopsByName(routeName: string): Promise<RouteStopOption[]> {
  if (!isSupabaseConfigured || !routeName.trim()) return []
  const { data: route, error: routeErr } = await supabase
    .from('routes')
    .select('id')
    .eq('route_name', routeName)
    .maybeSingle()
  if (routeErr || !route?.id) return []

  const { data, error } = await supabase
    .from('route_stops')
    .select('stop_order, expected_minutes, stops:stop_id ( id, stop_name )')
    .eq('route_id', route.id)
    .order('stop_order', { ascending: true })
  if (error) {
    console.error('[route_stops]', error.message)
    return []
  }

  return (data ?? [])
    .map((row) => {
      const stop = row.stops as { id: string; stop_name: string } | { id: string; stop_name: string }[] | null
      const s = Array.isArray(stop) ? stop[0] : stop
      if (!s?.id) return null
      return {
        id: s.id,
        name: s.stop_name,
        order: row.stop_order,
        expectedMinutes: row.expected_minutes,
      }
    })
    .filter((x): x is RouteStopOption => Boolean(x))
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

  const originStopId = payload.origin ? await resolveStopId(payload.origin) : null
  if (payload.origin && !originStopId) {
    return { ok: false, message: `출발 정류장 없음: ${payload.origin} (stops.stop_name)` }
  }
  const destinationStopId = payload.destination ? await resolveStopId(payload.destination) : null
  if (payload.destination && !destinationStopId) {
    return { ok: false, message: `도착 정류장 없음: ${payload.destination} (stops.stop_name)` }
  }

  const conflict = await findDriverTimeConflict({
    driverUuid: driverId,
    date: payload.date,
    departTime: payload.departTime,
    expectedEndTime: payload.expectedEndTime,
  })
  if (conflict) return { ok: false, message: conflict }

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
    origin_stop_id: originStopId,
    destination_stop_id: destinationStopId,
    expected_end_time: payload.expectedEndTime.length === 5 ? `${payload.expectedEndTime}:00` : payload.expectedEndTime,
  }

  const { data, error } = await supabase.from('operations').insert(insert).select(OPS_SELECT).maybeSingle()
  if (error) return { ok: false, message: error.message }
  if (!data) return { ok: false, message: 'insert 결과 없음' }
  return { ok: true, entry: rowToAssignment(data as unknown as OpRow) }
}

export type CreateUnassignedOperationInput = {
  date: string
  routeName: string
  departTime: string
  expectedEndTime: string
  vehicleName: string
  origin?: string
  destination?: string
  round?: number
  scheduleId?: string
}

/** 기사 없이 운행(operations)만 생성 */
export async function createUnassignedOperation(
  payload: CreateUnassignedOperationInput,
): Promise<{ ok: boolean; entry?: TodayAssignment; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }

  const end = toMinutes(payload.expectedEndTime)
  const start = toMinutes(payload.departTime)
  if (start == null || end == null) {
    return { ok: false, message: '출발·종료 시각을 확인해 주세요.' }
  }
  if (end <= start) {
    return { ok: false, message: '종료 시각이 출발 시각보다 이후여야 합니다.' }
  }

  const busId = await resolveBusId(payload.vehicleName)
  if (!busId) return { ok: false, message: `차량 없음: ${payload.vehicleName} (buses.bus_name)` }

  const scheduleId =
    payload.scheduleId?.trim() || (await resolveScheduleId(payload.routeName, payload.departTime, payload.date))
  if (!scheduleId) {
    return {
      ok: false,
      message: `스케줄 없음: ${payload.routeName} ${payload.departTime} (해당 날짜 요일의 schedules가 있어야 함)`,
    }
  }

  const originStopId = payload.origin ? await resolveStopId(payload.origin) : null
  if (payload.origin && !originStopId) {
    return { ok: false, message: `출발 정류장 없음: ${payload.origin} (stops.stop_name)` }
  }
  const destinationStopId = payload.destination ? await resolveStopId(payload.destination) : null
  if (payload.destination && !destinationStopId) {
    return { ok: false, message: `도착 정류장 없음: ${payload.destination} (stops.stop_name)` }
  }

  const stamp = payload.departTime.replace(':', '')
  const externalId = `op-${payload.date}-${stamp}-${Date.now().toString(36)}`

  const insert: Database['public']['Tables']['operations']['Insert'] = {
    schedule_id: scheduleId,
    driver_id: null,
    bus_id: busId,
    operation_date: payload.date,
    status: 'SCHEDULED',
    external_id: externalId,
    round: payload.round ?? 1,
    origin_stop_id: originStopId,
    destination_stop_id: destinationStopId,
    expected_end_time:
      payload.expectedEndTime.length === 5 ? `${payload.expectedEndTime}:00` : payload.expectedEndTime,
  }

  const { data, error } = await supabase.from('operations').insert(insert).select(OPS_SELECT).maybeSingle()
  if (error) return { ok: false, message: driverNullableHint(error) }
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
  if (patch.origin != null) {
    if (!patch.origin.trim()) {
      dbPatch.origin_stop_id = null
    } else {
      const stopId = await resolveStopId(patch.origin)
      if (!stopId) return { ok: false, message: `출발 정류장 없음: ${patch.origin}` }
      dbPatch.origin_stop_id = stopId
    }
  }
  if (patch.destination != null) {
    if (!patch.destination.trim()) {
      dbPatch.destination_stop_id = null
    } else {
      const stopId = await resolveStopId(patch.destination)
      if (!stopId) return { ok: false, message: `도착 정류장 없음: ${patch.destination}` }
      dbPatch.destination_stop_id = stopId
    }
  }
  if (patch.round != null) dbPatch.round = patch.round
  if (patch.expectedEndTime != null) {
    dbPatch.expected_end_time =
      patch.expectedEndTime.length === 5 ? `${patch.expectedEndTime}:00` : patch.expectedEndTime
  }
  if (patch.driverId) {
    const nextDriverId = await resolveDriverId(patch.driverId)
    if (!nextDriverId) return { ok: false, message: `기사 없음: ${patch.driverId}` }

    const { data: current, error: currentErr } = await supabase
      .from('operations')
      .select('id, operation_date, expected_end_time, schedules:schedule_id ( departure_time )')
      .eq('id', uuid)
      .maybeSingle()
    if (currentErr || !current) {
      return { ok: false, message: currentErr?.message || '운행 조회 실패' }
    }

    const currentRow = current as unknown as ConflictOpRow & { operation_date: string }
    const departTime = scheduleDepartFromEmbed(currentRow.schedules)
    const expectedEndTime = patch.expectedEndTime || hhmm(currentRow.expected_end_time)
    const conflict = await findDriverTimeConflict({
      driverUuid: nextDriverId,
      date: currentRow.operation_date,
      departTime,
      expectedEndTime,
      excludeOpId: uuid,
    })
    if (conflict) return { ok: false, message: conflict }
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

/** 기존 배차에 기사만 변경 — 출발~종료 겹치면 저장 거부 */
export async function assignDriverToOperation(
  operationId: string,
  driverLoginId: string,
): Promise<{ ok: boolean; entry?: TodayAssignment; message?: string }> {
  return updateAssignment(operationId, { driverId: driverLoginId })
}

export function addMinutesToHm(hm: string, addMinutes: number): string {
  const start = toMinutes(hm)
  if (start == null) return hm
  return minutesToHm(start + addMinutes)
}

function addDaysToDateKey(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number)
  const next = new Date(y, (m || 1) - 1, (d || 1) + days)
  return todayDateKey(next)
}

type CopySourceRow = {
  id: string
  schedule_id: string
  bus_id: string
  round: number | null
  expected_end_time: string | null
  origin_stop_id: string | null
  destination_stop_id: string | null
  status: string
}

export type CopyPreviousWeekResult = {
  ok: boolean
  sourceDate: string
  copied: number
  skippedDuplicate: number
  sourceCount: number
  message: string
}

/** 7일 전 운행을 대상 날짜에 기사 없이 생성. 같은 시간표·차량은 건너뜀. */
export async function copyOperationsFromPreviousWeek(
  targetDate: string,
): Promise<CopyPreviousWeekResult> {
  const sourceDate = addDaysToDateKey(targetDate, -7)
  const empty: CopyPreviousWeekResult = {
    ok: false,
    sourceDate,
    copied: 0,
    skippedDuplicate: 0,
    sourceCount: 0,
    message: '',
  }
  if (!isSupabaseConfigured) return { ...empty, message: 'Supabase 미설정' }
  if (sourceDate === targetDate) return { ...empty, message: '날짜가 올바르지 않습니다.' }

  const { data: sourceData, error: sourceErr } = await supabase
    .from('operations')
    .select('id, schedule_id, bus_id, round, expected_end_time, origin_stop_id, destination_stop_id, status')
    .eq('operation_date', sourceDate)
    .neq('status', 'CANCELLED')

  if (sourceErr) return { ...empty, message: sourceErr.message }

  const sourceRows = ((sourceData ?? []) as CopySourceRow[]).filter((row) => row.schedule_id && row.bus_id)
  if (!sourceRows.length) {
    return {
      ...empty,
      ok: true,
      message: `7일 전(${sourceDate})에 불러올 운행이 없습니다. 그 날짜에 운행을 먼저 만들어 주세요.`,
    }
  }

  const { data: targetData, error: targetErr } = await supabase
    .from('operations')
    .select('schedule_id, bus_id, status')
    .eq('operation_date', targetDate)
    .neq('status', 'CANCELLED')

  if (targetErr) return { ...empty, message: targetErr.message }

  const existingKeys = new Set(
    ((targetData ?? []) as { schedule_id: string; bus_id: string }[])
      .filter((row) => row.schedule_id && row.bus_id)
      .map((row) => `${row.schedule_id}::${row.bus_id}`),
  )

  let copied = 0
  let skippedDuplicate = 0
  const stamp = Date.now().toString(36)

  for (let i = 0; i < sourceRows.length; i++) {
    const row = sourceRows[i]
    const key = `${row.schedule_id}::${row.bus_id}`
    if (existingKeys.has(key)) {
      skippedDuplicate += 1
      continue
    }

    const insert: Database['public']['Tables']['operations']['Insert'] = {
      schedule_id: row.schedule_id,
      driver_id: null,
      bus_id: row.bus_id,
      operation_date: targetDate,
      status: 'SCHEDULED',
      external_id: `op-copy-${targetDate}-${stamp}-${i}`,
      round: row.round ?? 1,
      origin_stop_id: row.origin_stop_id,
      destination_stop_id: row.destination_stop_id,
      expected_end_time: row.expected_end_time,
    }

    const { error: insertErr } = await supabase.from('operations').insert(insert)
    if (insertErr) {
      return {
        ok: false,
        sourceDate,
        copied,
        skippedDuplicate,
        sourceCount: sourceRows.length,
        message: driverNullableHint(insertErr),
      }
    }
    existingKeys.add(key)
    copied += 1
  }

  const parts = [`7일 전(${sourceDate}) 운행 ${copied}건을 생성했습니다. 기사는 기사 배정에서 지정하세요.`]
  if (skippedDuplicate) parts.push(`이미 있는 ${skippedDuplicate}건은 건너뛰었습니다.`)

  return {
    ok: true,
    sourceDate,
    copied,
    skippedDuplicate,
    sourceCount: sourceRows.length,
    message: parts.join(' '),
  }
}
