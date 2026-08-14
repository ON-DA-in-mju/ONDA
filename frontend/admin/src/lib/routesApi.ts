import { colorForRoute, type MapRouteLayer, type RouteStopPin } from '../data/cityShuttleStops'
import { fetchNaverDrivingPath, metersToKm1 } from './naverDirectionsApi'
import type { SemesterType, Weekday } from '../types/database'
import { isSupabaseConfigured, supabase } from './supabase'
import { toDateKey } from './weekDate'

const WEEKDAY_KO: Record<Weekday, string> = {
  MON: '월',
  TUE: '화',
  WED: '수',
  THU: '목',
  FRI: '금',
  SAT: '토',
  SUN: '일',
}

const WEEKDAY_ORDER: Weekday[] = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']

export type RouteStopItem = RouteStopPin & { stopId: string; expectedMinutes: number | null }

export type RouteScheduleItem = {
  id: string
  departureTime: string
  weekday: Weekday
  weekdayLabel: string
  semester: SemesterType
  semesterLabel: string
}

export type RouteBusItem = {
  id: string
  name: string
  plate: string
  capacity: number
}

export type RouteCatalogItem = {
  id: string
  name: string
  isActive: boolean
  status: '운행 가능' | '운행 불가'
  type: string
  days: string
  hours: string
  desc: string
  start: string
  end: string
  busCount: number
  stopCount: number
  distanceKm: number | null
  durationMin: number | null
  intervalLabel: string
  stops: RouteStopItem[]
  schedules: RouteScheduleItem[]
  buses: RouteBusItem[]
  mapLayer: MapRouteLayer
  /** 운행 중단 종료 시각 (ISO). 지나면 자동 운행 가능 */
  suspendedUntil: string | null
}

function hhmm(value: string | null | undefined): string {
  if (!value) return ''
  return value.slice(0, 5)
}

function timeToMinutes(value: string): number | null {
  const t = hhmm(value)
  if (!/^\d{2}:\d{2}$/.test(t)) return null
  const [h, m] = t.split(':').map(Number)
  if (!Number.isFinite(h) || !Number.isFinite(m)) return null
  return h * 60 + m
}

function averageIntervalLabel(times: string[]): string {
  const mins = [...new Set(times.map((t) => timeToMinutes(t)).filter((n): n is number => n != null))].sort(
    (a, b) => a - b,
  )
  if (mins.length < 2) return '-'
  const gaps: number[] = []
  for (let i = 1; i < mins.length; i++) {
    const gap = mins[i] - mins[i - 1]
    if (gap > 0) gaps.push(gap)
  }
  if (!gaps.length) return '-'
  const avg = Math.round(gaps.reduce((a, b) => a + b, 0) / gaps.length)
  return avg > 0 ? `약 ${avg}분` : '-'
}

function haversineMeters(a: { lat: number; lng: number }, b: { lat: number; lng: number }): number {
  const R = 6371000
  const toRad = (d: number) => (d * Math.PI) / 180
  const dLat = toRad(b.lat - a.lat)
  const dLng = toRad(b.lng - a.lng)
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(s)))
}

function pathDistanceKm(stops: Array<{ lat: number; lng: number }>): number | null {
  const pts = stops.filter((s) => Number.isFinite(s.lat) && Number.isFinite(s.lng))
  if (pts.length < 2) return null
  let meters = 0
  for (let i = 1; i < pts.length; i++) meters += haversineMeters(pts[i - 1], pts[i])
  return Math.round(meters / 100) / 10
}

function daysLabel(weekdays: Weekday[]): string {
  const unique = WEEKDAY_ORDER.filter((d) => weekdays.includes(d))
  if (!unique.length) return '-'
  if (unique.length === 7) return '매일'
  return unique.map((d) => WEEKDAY_KO[d]).join('·')
}

function hoursLabel(times: string[]): string {
  const sorted = [...new Set(times.map(hhmm).filter(Boolean))].sort()
  if (!sorted.length) return '-'
  if (sorted.length === 1) return sorted[0]
  return `${sorted[0]} ~ ${sorted[sorted.length - 1]}`
}

function semesterLabel(semester: SemesterType): string {
  return semester === 'VACATION' ? '방학' : '학기 중'
}


function unwrap<T>(value: T | T[] | null | undefined): T | null {
  if (!value) return null
  return Array.isArray(value) ? value[0] ?? null : value
}

type StopJoin = { id: string; stop_name: string; latitude: number; longitude: number }
type BusJoin = { id: string; bus_name: string; vehicle_number: string; capacity: number }

export async function fetchRouteCatalog(): Promise<RouteCatalogItem[]> {
  if (!isSupabaseConfigured) return []

  await import('./forceSuspendApi').then((api) => api.expireSuspendedRoutes(true)).catch(() => undefined)

  const [routesRes, stopsRes, schedulesRes, opsRes] = await Promise.all([
    supabase.from('routes').select('*').order('route_name', { ascending: true }),
    supabase
      .from('route_stops')
      .select('route_id, stop_order, expected_minutes, stops:stop_id ( id, stop_name, latitude, longitude )')
      .order('stop_order', { ascending: true }),
    supabase.from('schedules').select('id, route_id, departure_time, weekday, semester'),
    supabase
      .from('operations')
      .select('bus_id, status, operation_date, schedules:schedule_id ( route_id ), buses:bus_id ( id, bus_name, vehicle_number, capacity )')
      .in('status', ['SCHEDULED', 'IN_PROGRESS'])
      .gte('operation_date', toDateKey(new Date())),
  ])

  if (routesRes.error) {
    console.error('[routes]', routesRes.error.message)
    return []
  }
  if (stopsRes.error) console.error('[route_stops]', stopsRes.error.message)
  if (schedulesRes.error) console.error('[schedules]', schedulesRes.error.message)
  if (opsRes.error) console.error('[operations]', opsRes.error.message)

  const stopRows = (stopsRes.data ?? []) as Array<{
    route_id: string
    stop_order: number
    expected_minutes: number | null
    stops: StopJoin | StopJoin[] | null
  }>
  const scheduleRows = (schedulesRes.data ?? []) as Array<{
    id: string
    route_id: string
    departure_time: string
    weekday: Weekday
    semester: SemesterType
  }>
  const opRows = (opsRes.data ?? []) as Array<{
    bus_id: string | null
    status: string
    operation_date: string
    schedules: { route_id: string } | { route_id: string }[] | null
    buses: BusJoin | BusJoin[] | null
  }>

  const stopsByRoute = new Map<string, RouteStopItem[]>()
  for (const row of stopRows) {
    const stop = unwrap(row.stops)
    if (!stop?.id) continue
    const list = stopsByRoute.get(row.route_id) ?? []
    list.push({
      id: `${row.route_id}:${stop.id}:${row.stop_order}`,
      stopId: stop.id,
      name: stop.stop_name,
      lat: stop.latitude,
      lng: stop.longitude,
      order: row.stop_order,
      expectedMinutes: row.expected_minutes,
    })
    stopsByRoute.set(row.route_id, list)
  }

  const schedulesByRoute = new Map<string, RouteScheduleItem[]>()
  for (const row of scheduleRows) {
    const list = schedulesByRoute.get(row.route_id) ?? []
    list.push({
      id: row.id,
      departureTime: hhmm(row.departure_time),
      weekday: row.weekday,
      weekdayLabel: WEEKDAY_KO[row.weekday] ?? row.weekday,
      semester: row.semester,
      semesterLabel: semesterLabel(row.semester),
    })
    schedulesByRoute.set(row.route_id, list)
  }

  const busesByRoute = new Map<string, Map<string, RouteBusItem>>()
  for (const row of opRows) {
    const schedule = unwrap(row.schedules)
    const bus = unwrap(row.buses)
    const routeId = schedule?.route_id
    const busId = bus?.id || row.bus_id
    if (!routeId || !busId) continue
    const map = busesByRoute.get(routeId) ?? new Map<string, RouteBusItem>()
    if (!map.has(busId)) {
      map.set(busId, {
        id: busId,
        name: bus?.bus_name || '-',
        plate: bus?.vehicle_number || '-',
        capacity: bus?.capacity ?? 0,
      })
    }
    busesByRoute.set(routeId, map)
  }

  const items: RouteCatalogItem[] = (routesRes.data ?? []).map((route) => {
    const stops = (stopsByRoute.get(route.id) ?? []).slice().sort((a, b) => a.order - b.order)
    const schedules = (schedulesByRoute.get(route.id) ?? [])
      .slice()
      .sort(
        (a, b) =>
          WEEKDAY_ORDER.indexOf(a.weekday) - WEEKDAY_ORDER.indexOf(b.weekday) ||
          a.semester.localeCompare(b.semester) ||
          a.departureTime.localeCompare(b.departureTime),
      )
    const buses = [...(busesByRoute.get(route.id)?.values() ?? [])]
    const times = schedules.map((s) => s.departureTime)
    const durationMin = stops
      .map((s) => s.expectedMinutes)
      .filter((n): n is number => n != null && n > 0)
      .sort((a, b) => b - a)[0] ?? null

    return {
      id: route.id,
      name: route.route_name,
      isActive: route.is_active,
      status: route.is_active ? '운행 가능' : '운행 불가',
      type: route.direction || '-',
      days: daysLabel(schedules.map((s) => s.weekday)),
      hours: hoursLabel(times),
      desc: route.description || [route.start_location, route.end_location].filter(Boolean).join(' → ') || '-',
      start: route.start_location || stops[0]?.name || '-',
      end: route.end_location || stops.at(-1)?.name || '-',
      busCount: buses.length,
      stopCount: stops.length,
      distanceKm: pathDistanceKm(stops),
      durationMin,
      intervalLabel: averageIntervalLabel(times),
      stops,
      schedules,
      buses,
      mapLayer: {
        id: route.id,
        name: route.route_name,
        color: colorForRoute(route.route_name, route.id),
        stops,
      },
      suspendedUntil: route.suspended_until ?? null,
    }
  })

  return items
}

export async function fetchRouteDrivingKm(stops: Array<{ lat: number; lng: number }>): Promise<number | null> {
  const pts = stops.filter((s) => Number.isFinite(s.lat) && Number.isFinite(s.lng))
  if (pts.length < 2) return null
  const driving = await fetchNaverDrivingPath(pts.map((s) => ({ lat: s.lat, lng: s.lng })))
  if (driving.distanceMeters != null && driving.distanceMeters > 0) {
    return metersToKm1(driving.distanceMeters)
  }
  if (driving.path.length >= 2) return pathDistanceKm(driving.path)
  return pathDistanceKm(pts)
}

export async function createRoute(payload: {
  name: string
  direction?: string
  description?: string
  start?: string
  end?: string
  isActive?: boolean
}): Promise<{ ok: boolean; id?: string; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const name = payload.name.trim()
  if (!name) return { ok: false, message: '노선명을 입력하세요.' }

  const { data, error } = await supabase
    .from('routes')
    .insert({
      route_name: name,
      direction: payload.direction?.trim() || null,
      description: payload.description?.trim() || null,
      start_location: payload.start?.trim() || null,
      end_location: payload.end?.trim() || null,
      is_active: payload.isActive ?? true,
    })
    .select('id')
    .maybeSingle()

  if (error) return { ok: false, message: error.message }
  if (!data?.id) return { ok: false, message: '노선이 저장되지 않았습니다.' }
  return { ok: true, id: data.id }
}

export async function updateRoute(
  id: string,
  payload: {
    name?: string
    direction?: string
    description?: string
    start?: string
    end?: string
    isActive?: boolean
  },
): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const { error } = await supabase
    .from('routes')
    .update({
      ...(payload.name != null ? { route_name: payload.name.trim() } : {}),
      ...(payload.direction != null ? { direction: payload.direction.trim() || null } : {}),
      ...(payload.description != null ? { description: payload.description.trim() || null } : {}),
      ...(payload.start != null ? { start_location: payload.start.trim() || null } : {}),
      ...(payload.end != null ? { end_location: payload.end.trim() || null } : {}),
      ...(payload.isActive != null ? { is_active: payload.isActive } : {}),
      ...(payload.isActive === true ? { suspended_until: null } : {}),
    })
    .eq('id', id)
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}

export async function replaceRouteStops(
  routeId: string,
  stopIds: string[],
): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const ids = stopIds.map((id) => id.trim()).filter(Boolean)

  const { data: previous } = await supabase
    .from('route_stops')
    .select('stop_id, stop_order, expected_minutes')
    .eq('route_id', routeId)
    .order('stop_order', { ascending: true })

  const { error: delErr } = await supabase.from('route_stops').delete().eq('route_id', routeId)
  if (delErr) return { ok: false, message: delErr.message }

  if (ids.length) {
    const prevByIndex = previous ?? []
    const rows = ids.map((stop_id, i) => ({
      route_id: routeId,
      stop_id,
      stop_order: i + 1,
      expected_minutes: prevByIndex[i]?.stop_id === stop_id ? prevByIndex[i].expected_minutes : null,
    }))
    const { error: insErr } = await supabase.from('route_stops').insert(rows)
    if (insErr) return { ok: false, message: insErr.message }

    const { data: named } = await supabase.from('stops').select('id, stop_name').in('id', ids)
    const nameById = new Map((named ?? []).map((s) => [s.id, s.stop_name]))
    await supabase
      .from('routes')
      .update({
        start_location: nameById.get(ids[0]) ?? null,
        end_location: nameById.get(ids[ids.length - 1]) ?? null,
      })
      .eq('id', routeId)
  }

  return { ok: true }
}

export type StopCatalogItem = {
  id: string
  name: string
  lat: number
  lng: number
  routes: string
  routeIds: string[]
}

export async function fetchStopCatalog(): Promise<StopCatalogItem[]> {
  if (!isSupabaseConfigured) return []

  const [stopsRes, linksRes, routesRes] = await Promise.all([
    supabase.from('stops').select('*').order('stop_name', { ascending: true }),
    supabase.from('route_stops').select('stop_id, route_id'),
    supabase.from('routes').select('id, route_name'),
  ])

  if (stopsRes.error) {
    console.error('[stops]', stopsRes.error.message)
    return []
  }

  const routeNameById = new Map((routesRes.data ?? []).map((r) => [r.id, r.route_name]))
  const routeIdsByStop = new Map<string, string[]>()
  for (const row of linksRes.data ?? []) {
    const list = routeIdsByStop.get(row.stop_id) ?? []
    if (!list.includes(row.route_id)) list.push(row.route_id)
    routeIdsByStop.set(row.stop_id, list)
  }

  return (stopsRes.data ?? []).map((stop) => {
    const routeIds = routeIdsByStop.get(stop.id) ?? []
    const names = routeIds.map((id) => routeNameById.get(id)).filter((n): n is string => Boolean(n))
    return {
      id: stop.id,
      name: stop.stop_name,
      lat: stop.latitude,
      lng: stop.longitude,
      routes: names.length ? names.join(', ') : '-',
      routeIds,
    }
  })
}

export async function upsertStop(payload: {
  id?: string
  name: string
  lat: number
  lng: number
  routeId?: string
}): Promise<{ ok: boolean; id?: string; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const name = payload.name.trim()
  if (!name) return { ok: false, message: '정류장명을 입력하세요.' }
  if (!Number.isFinite(payload.lat) || !Number.isFinite(payload.lng)) {
    return { ok: false, message: '위도·경도를 확인하세요.' }
  }

  let stopId = payload.id
  if (stopId) {
    const { error } = await supabase
      .from('stops')
      .update({ stop_name: name, latitude: payload.lat, longitude: payload.lng })
      .eq('id', stopId)
    if (error) return { ok: false, message: error.message }
  } else {
    const { data, error } = await supabase
      .from('stops')
      .insert({ stop_name: name, latitude: payload.lat, longitude: payload.lng })
      .select('id')
      .maybeSingle()
    if (error) return { ok: false, message: error.message }
    if (!data?.id) return { ok: false, message: '정류장이 저장되지 않았습니다.' }
    stopId = data.id
  }

  if (payload.routeId && stopId) {
    const { data: existing } = await supabase
      .from('route_stops')
      .select('id')
      .eq('route_id', payload.routeId)
      .eq('stop_id', stopId)
      .maybeSingle()
    if (!existing?.id) {
      const { data: last } = await supabase
        .from('route_stops')
        .select('stop_order')
        .eq('route_id', payload.routeId)
        .order('stop_order', { ascending: false })
        .limit(1)
        .maybeSingle()
      const { error: linkErr } = await supabase.from('route_stops').insert({
        route_id: payload.routeId,
        stop_id: stopId,
        stop_order: (last?.stop_order ?? 0) + 1,
      })
      if (linkErr) return { ok: false, message: linkErr.message }
    }
  }

  return { ok: true, id: stopId }
}
