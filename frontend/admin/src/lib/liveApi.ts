import { isSupabaseConfigured, supabase } from './supabase'
import { todayDateKey } from '../types/assignment'
import type { OperationStatus } from '../types/database'

export type LiveGpsKind = 'ok' | 'none' | 'error'

export type LiveVehicle = {
  id: string
  driverId: string
  driverName: string
  vehicleName: string
  routeName: string
  operationId: string
  status: 'in_progress' | 'ended' | 'idle' | 'stopped'
  statusLabel: string
  tone: 'green' | 'orange' | 'blue' | 'gray' | 'red'
  lat: number | null
  lng: number | null
  accuracy: number | null
  stop: string
  gps: string
  gpsKind: LiveGpsKind
  updatedAt: number
  last: string
}

export type LiveStats = {
  ok: number
  none: number
  error: number
  total: number
  rate: number
  inProgress: number
  ended: number
  idle: number
  stopped?: number
}

export type LiveSnapshot = {
  vehicles: LiveVehicle[]
  stats: LiveStats
}

const emptyStats: LiveStats = {
  ok: 0,
  none: 0,
  error: 0,
  total: 0,
  rate: 0,
  inProgress: 0,
  ended: 0,
  idle: 0,
  stopped: 0,
}

/** 마지막 GPS가 이보다 오래되면 '오류'(빨간 점). 기사 전송 주기(~10초) + 네트워크 여유 */
const STALE_MS = 90_000

const OPS_SELECT = `
  id,
  external_id,
  operation_date,
  status,
  origin,
  destination,
  started_at,
  ended_at,
  updated_at,
  driver_id,
  bus_id,
  users:driver_id ( id, name, login_id, email ),
  buses:bus_id ( bus_name, vehicle_number ),
  schedules:schedule_id ( departure_time, routes:route_id ( route_name ) )
`

type OpLiveRow = {
  id: string
  external_id: string | null
  operation_date: string
  status: OperationStatus | string
  origin: string | null
  destination: string | null
  started_at: string | null
  ended_at: string | null
  updated_at: string | null
  driver_id: string
  bus_id: string
  users: { id: string; name: string; login_id: string | null; email: string | null } | null
  buses: { bus_name: string; vehicle_number: string } | null
  schedules: {
    departure_time: string
    routes: { route_name: string } | null
  } | null
}

type LocRow = {
  operation_id: string
  latitude: number
  longitude: number
  speed: number | null
  heading: number | null
  recorded_at: string | null
}

type OpLogGpsRow = {
  operation_id: string
  log_message: string
  created_at: string | null
}

function parseGpsFromLogMessage(message: string): { lat: number | null; lng: number | null } {
  const lat = message.match(/lat=(-?\d+(?:\.\d+)?)/i)?.[1]
  const lng = message.match(/lng=(-?\d+(?:\.\d+)?)/i)?.[1]
  return {
    lat: lat != null ? Number(lat) : null,
    lng: lng != null ? Number(lng) : null,
  }
}

function formatLast(updatedAt: number, now = Date.now()): string {
  const sec = Math.max(0, Math.floor((now - updatedAt) / 1000))
  if (sec < 5) return '방금 전'
  if (sec < 60) return `${sec}초 전`
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min}분 전`
  return `${Math.floor(min / 60)}시간 전`
}

function locationLabel(lat: number | null, lng: number | null, origin?: string | null): string {
  if (lat != null && lng != null) return `${lat.toFixed(5)}, ${lng.toFixed(5)}`
  if (origin?.trim()) return origin.trim()
  return '위치 없음'
}

function mapOpStatus(
  dbStatus: string,
  flags: { pendingSafeStop: boolean; confirmedStop: boolean },
): LiveVehicle['status'] {
  // 요청 접수~관리자 확정(중단)까지는 차량 정차 상태 → 안전 정차
  const safeStopActive = flags.pendingSafeStop || flags.confirmedStop
  if (safeStopActive && (dbStatus === 'IN_PROGRESS' || dbStatus === 'SCHEDULED')) {
    return 'stopped'
  }
  switch (dbStatus) {
    case 'IN_PROGRESS':
      return 'in_progress'
    case 'COMPLETED':
    case 'CANCELLED':
      return 'ended'
    default:
      return 'idle'
  }
}

function statusMeta(status: LiveVehicle['status']): { statusLabel: string; tone: LiveVehicle['tone'] } {
  switch (status) {
    case 'in_progress':
      return { statusLabel: '운행 중', tone: 'green' }
    case 'stopped':
      return { statusLabel: '안전 정차', tone: 'orange' }
    case 'ended':
      return { statusLabel: '종료', tone: 'gray' }
    default:
      return { statusLabel: '대기 중', tone: 'blue' }
  }
}

function classifyGps(
  row: {
    status: LiveVehicle['status']
    lat: number | null
    lng: number | null
    updatedAt: number
  },
  now: number,
): { gpsKind: LiveGpsKind; gps: string } {
  if (row.status !== 'in_progress') {
    return { gpsKind: 'none', gps: '미수신' }
  }
  const age = now - row.updatedAt
  if (row.lat == null || row.lng == null || age > STALE_MS) {
    return { gpsKind: 'error', gps: '오류' }
  }
  return { gpsKind: 'ok', gps: '정상' }
}

function buildStats(vehicles: LiveVehicle[]): LiveStats {
  const ok = vehicles.filter((v) => v.gpsKind === 'ok').length
  const none = vehicles.filter((v) => v.gpsKind === 'none').length
  const error = vehicles.filter((v) => v.gpsKind === 'error').length
  const total = vehicles.length
  const rate = total === 0 ? 0 : Math.round((ok / total) * 1000) / 10
  return {
    ok,
    none,
    error,
    total,
    rate,
    inProgress: vehicles.filter((v) => v.status === 'in_progress').length,
    ended: vehicles.filter((v) => v.status === 'ended').length,
    idle: vehicles.filter((v) => v.status === 'idle').length,
    stopped: vehicles.filter((v) => v.status === 'stopped').length,
  }
}

function latestLocationsByOp(rows: LocRow[]): Map<string, LocRow> {
  const map = new Map<string, LocRow>()
  for (const row of rows) {
    const prev = map.get(row.operation_id)
    if (!prev) {
      map.set(row.operation_id, row)
      continue
    }
    const prevTs = prev.recorded_at ? Date.parse(prev.recorded_at) : 0
    const nextTs = row.recorded_at ? Date.parse(row.recorded_at) : 0
    if (nextTs >= prevTs) map.set(row.operation_id, row)
  }
  return map
}

async function fetchSafeStopFlags(opIds: string[]): Promise<{
  pending: Set<string>
  confirmedStop: Set<string>
}> {
  const empty = { pending: new Set<string>(), confirmedStop: new Set<string>() }
  if (!opIds.length) return empty
  const { data, error } = await supabase
    .from('safe_stop_requests')
    .select('operation_id, decision')
    .in('operation_id', opIds)
    .in('decision', ['pending', 'stop'])
  if (error) {
    console.warn('[live] safe_stop_requests', error.message)
    return empty
  }
  const pending = new Set<string>()
  const confirmedStop = new Set<string>()
  for (const row of data ?? []) {
    if (!row.operation_id) continue
    if (row.decision === 'pending') pending.add(row.operation_id)
    if (row.decision === 'stop') confirmedStop.add(row.operation_id)
  }
  return { pending, confirmedStop }
}

function buildVehiclesFromOps(
  rows: OpLiveRow[],
  locMap: Map<string, LocRow>,
  safeStopFlags: { pending: Set<string>; confirmedStop: Set<string> },
  now = Date.now(),
): LiveVehicle[] {
  const departById = new Map(
    rows.map((r) => [r.id, r.schedules?.departure_time ?? '99:99'] as const),
  )

  const vehicles: LiveVehicle[] = rows.map((row) => {
    const loc = locMap.get(row.id)
    const status = mapOpStatus(String(row.status), {
      pendingSafeStop: safeStopFlags.pending.has(row.id),
      confirmedStop: safeStopFlags.confirmedStop.has(row.id),
    })
    const { statusLabel, tone } = statusMeta(status)
    const lat = loc?.latitude ?? null
    const lng = loc?.longitude ?? null
    const updatedAt = loc?.recorded_at
      ? Date.parse(loc.recorded_at)
      : row.updated_at
        ? Date.parse(row.updated_at)
        : row.started_at
          ? Date.parse(row.started_at)
          : row.ended_at
            ? Date.parse(row.ended_at)
            : 0
    const { gpsKind, gps } = classifyGps({ status, lat, lng, updatedAt: updatedAt || now }, now)
    return {
      id: row.id,
      driverId: row.users?.login_id || row.driver_id,
      driverName: row.users?.name || '',
      vehicleName: row.buses?.bus_name || row.buses?.vehicle_number || '미정',
      routeName: row.schedules?.routes?.route_name || '',
      operationId: row.external_id || row.id,
      status,
      statusLabel,
      tone: gpsKind === 'error' && status === 'in_progress' ? 'red' : tone,
      lat: lat != null && lng != null ? lat : null,
      lng: lat != null && lng != null ? lng : null,
      accuracy: null,
      stop: locationLabel(lat, lng, row.origin),
      gps,
      gpsKind,
      updatedAt: updatedAt || now,
      last: formatLast(updatedAt || now, now),
    }
  })

  vehicles.sort(
    (a, b) =>
      (departById.get(a.id) ?? '99:99').localeCompare(departById.get(b.id) ?? '99:99') ||
      a.routeName.localeCompare(b.routeName),
  )
  return vehicles
}

/** 운행 중 배차의 최신 GPS (vehicle_locations → operation_logs 폴백) */
async function fetchGpsLocMap(inProgressIds: string[]): Promise<Map<string, LocRow>> {
  const locMap = new Map<string, LocRow>()
  if (!inProgressIds.length) return locMap

  const { data: locs, error: locErr } = await supabase
    .from('vehicle_locations')
    .select('operation_id, latitude, longitude, speed, heading, recorded_at')
    .in('operation_id', inProgressIds)
    .order('recorded_at', { ascending: false })
    .limit(Math.max(50, inProgressIds.length * 5))

  if (locErr) console.warn('[live] vehicle_locations', locErr.message)
  else {
    for (const [k, v] of latestLocationsByOp((locs ?? []) as LocRow[])) {
      locMap.set(k, v)
    }
  }

  const missingOpIds = inProgressIds.filter((id) => !locMap.has(id))
  if (!missingOpIds.length) return locMap

  const { data: gpsLogs, error: gpsLogErr } = await supabase
    .from('operation_logs')
    .select('operation_id, log_message, created_at')
    .eq('event_type', 'LOCATION_UPDATED')
    .in('operation_id', missingOpIds)
    .order('created_at', { ascending: false })
    .limit(Math.max(30, missingOpIds.length * 3))

  if (gpsLogErr) {
    console.warn('[live] operation_logs gps', gpsLogErr.message)
    return locMap
  }

  for (const raw of (gpsLogs ?? []) as OpLogGpsRow[]) {
    if (locMap.has(raw.operation_id)) continue
    const parsed = parseGpsFromLogMessage(raw.log_message ?? '')
    if (parsed.lat == null || parsed.lng == null) continue
    if (!Number.isFinite(parsed.lat) || !Number.isFinite(parsed.lng)) continue
    locMap.set(raw.operation_id, {
      operation_id: raw.operation_id,
      latitude: parsed.lat,
      longitude: parsed.lng,
      speed: null,
      heading: null,
      recorded_at: raw.created_at,
    })
  }
  return locMap
}

/**
 * 목록·KPI용 — 오늘 operations(+안전정차)만. GPS 조회 없음 → 페이지 진입 즉시 표시.
 */
export async function fetchLiveVehiclesList(date = todayDateKey()): Promise<LiveSnapshot> {
  if (!isSupabaseConfigured) return fetchLiveFromDevApi()

  const { data: ops, error: opsErr } = await supabase
    .from('operations')
    .select(OPS_SELECT)
    .eq('operation_date', date)

  if (opsErr) {
    console.error('[live] operations', opsErr.message)
    return { vehicles: [], stats: emptyStats }
  }

  const rows = (ops ?? []) as unknown as OpLiveRow[]
  const opIds = rows.map((r) => r.id)
  const hasInProgress = rows.some((r) => String(r.status) === 'IN_PROGRESS')
  const safeStopFlags = await fetchSafeStopFlags(hasInProgress ? opIds : [])
  const vehicles = buildVehiclesFromOps(rows, new Map(), safeStopFlags)
  return { vehicles, stats: buildStats(vehicles) }
}

/**
 * 이미 그린 목록에 GPS 좌표만 붙인다. 지도 마커용 — 목록 표시를 막지 않음.
 */
export async function enrichLiveVehiclesGps(snapshot: LiveSnapshot): Promise<LiveSnapshot> {
  if (!isSupabaseConfigured) return snapshot
  const inProgressIds = snapshot.vehicles
    .filter((v) => v.status === 'in_progress' || v.status === 'stopped')
    .map((v) => v.id)
  if (!inProgressIds.length) return snapshot

  const locMap = await fetchGpsLocMap(inProgressIds)
  if (!locMap.size) return snapshot

  const now = Date.now()
  let changed = false
  const vehicles = snapshot.vehicles.map((v) => {
    const loc = locMap.get(v.id)
    if (!loc) return v
    const lat = Number(loc.latitude)
    const lng = Number(loc.longitude)
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return v
    const updatedAt = loc.recorded_at ? Date.parse(loc.recorded_at) : now
    const { gpsKind, gps } = classifyGps(
      { status: v.status, lat, lng, updatedAt: updatedAt || now },
      now,
    )
    const baseTone = statusMeta(v.status).tone
    changed = true
    return {
      ...v,
      lat,
      lng,
      updatedAt: updatedAt || now,
      gps,
      gpsKind,
      stop: locationLabel(lat, lng, v.stop === '위치 없음' ? null : v.stop),
      last: formatLast(updatedAt || now, now),
      tone: gpsKind === 'error' && v.status === 'in_progress' ? ('red' as const) : baseTone,
    }
  })
  if (!changed) return snapshot
  return { vehicles, stats: buildStats(vehicles) }
}

async function fetchLiveFromSupabase(date = todayDateKey()): Promise<LiveSnapshot | null> {
  if (!isSupabaseConfigured) return null

  const list = await fetchLiveVehiclesList(date)
  if (!list.vehicles.length) return list
  return enrichLiveVehiclesGps(list)
}

async function fetchLiveFromDevApi(): Promise<LiveSnapshot> {
  try {
    const res = await fetch('/api/live/vehicles', { cache: 'no-store' })
    if (!res.ok) return { vehicles: [], stats: emptyStats }
    const data = (await res.json()) as LiveSnapshot
    return {
      vehicles: Array.isArray(data.vehicles) ? data.vehicles : [],
      stats: data.stats ?? emptyStats,
    }
  } catch {
    return { vehicles: [], stats: emptyStats }
  }
}

/**
 * 실시간 차량 스냅샷.
 * Supabase가 설정돼 있으면 오늘 `operations` + 최신 `vehicle_locations` (+ 안전정차) 기준.
 * 미설정일 때만 로컬 Vite mock API로 폴백.
 */
export async function fetchLiveVehicles(): Promise<LiveSnapshot> {
  if (isSupabaseConfigured) {
    const fromDb = await fetchLiveFromSupabase()
    if (fromDb) return fromDb
    // RLS/네트워크 실패 시 빈 스냅샷 (목데이터로 채우지 않음)
    return { vehicles: [], stats: emptyStats }
  }
  return fetchLiveFromDevApi()
}

/** Realtime INSERT/UPDATE 한 건을 기존 목록에 반영 (지도는 setPosition만 쓰면 됨) */
export function applyLocationPatch(
  snapshot: LiveSnapshot,
  patch: {
    operation_id: string
    latitude: number
    longitude: number
    recorded_at?: string | null
  },
): LiveSnapshot {
  const now = Date.now()
  const updatedAt = patch.recorded_at ? Date.parse(patch.recorded_at) : now
  const lat = Number(patch.latitude)
  const lng = Number(patch.longitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return snapshot

  let changed = false
  const vehicles = snapshot.vehicles.map((v) => {
    if (v.id !== patch.operation_id) return v
    changed = true
    const { gpsKind, gps } = classifyGps(
      { status: v.status, lat, lng, updatedAt: updatedAt || now },
      now,
    )
    const baseTone = statusMeta(v.status).tone
    return {
      ...v,
      lat,
      lng,
      updatedAt: updatedAt || now,
      gps,
      gpsKind,
      stop: locationLabel(lat, lng),
      last: formatLast(updatedAt || now, now),
      tone: gpsKind === 'error' && v.status === 'in_progress' ? ('red' as const) : baseTone,
    }
  })
  if (!changed) return snapshot
  return { vehicles, stats: buildStats(vehicles) }
}

export type LiveLocationRow = {
  operation_id: string
  latitude: number
  longitude: number
  recorded_at?: string | null
}

/**
 * GPS INSERT + operations 변경을 Realtime으로 구독.
 * Realtime 미활성/권한 실패 시에도 폴링 백업과 함께 쓰면 됨.
 */
export function subscribeLiveUpdates(handlers: {
  onLocation?: (row: LiveLocationRow) => void
  onOperationChange?: () => void
}): () => void {
  if (!isSupabaseConfigured) return () => {}

  const channel = supabase
    .channel(`onda-live-${Date.now()}`)
    .on(
      'postgres_changes',
      { event: 'INSERT', schema: 'public', table: 'vehicle_locations' },
      (payload) => {
        const row = payload.new as Partial<LiveLocationRow>
        if (!row?.operation_id) return
        if (row.latitude == null || row.longitude == null) return
        handlers.onLocation?.({
          operation_id: String(row.operation_id),
          latitude: Number(row.latitude),
          longitude: Number(row.longitude),
          recorded_at: row.recorded_at ?? null,
        })
      },
    )
    .on(
      'postgres_changes',
      { event: 'UPDATE', schema: 'public', table: 'vehicle_locations' },
      (payload) => {
        const row = payload.new as Partial<LiveLocationRow>
        if (!row?.operation_id) return
        if (row.latitude == null || row.longitude == null) return
        handlers.onLocation?.({
          operation_id: String(row.operation_id),
          latitude: Number(row.latitude),
          longitude: Number(row.longitude),
          recorded_at: row.recorded_at ?? null,
        })
      },
    )
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: 'operations' },
      () => {
        handlers.onOperationChange?.()
      },
    )
    .subscribe((status) => {
      if (status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') {
        console.warn('[live] realtime', status)
      }
    })

  return () => {
    void supabase.removeChannel(channel)
  }
}
