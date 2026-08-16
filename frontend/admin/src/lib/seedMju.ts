import { isSupabaseConfigured, supabase } from './supabase'
import { expandScheduleRows, MJU_ROUTES } from '../data/mjuTimetable'
import { OPERATIONAL_ROUTE_NAMES, VARIANT_ROUTE_DEFS } from './routeVariants'
import { MYONGJI_STATION_AFTER18_ROUTE_NAME, MYONGJI_STATION_ROUTE_NAME } from '../data/cityShuttleStops'
import type { Database, SemesterType, Weekday } from '../types/database'

type RouteRow = Database['public']['Tables']['routes']['Row']
type ScheduleRow = Database['public']['Tables']['schedules']['Row']

const TARGET_NAMES = [...OPERATIONAL_ROUTE_NAMES]
const BASE_AND_VARIANT_DEFS = [
  ...MJU_ROUTES.map((r) => ({
    name: r.name,
    direction: r.direction,
    description: r.description,
    start_location: r.start_location,
    end_location: r.end_location,
  })),
  ...VARIANT_ROUTE_DEFS.map((r) => ({
    name: r.name,
    direction: r.direction,
    description: r.description,
    start_location: r.start_location,
    end_location: r.end_location,
  })),
]

/**
 * UI에서 노선 선택 시 함께 조회할 DB 노선명.
 * - 명지대역 셔틀: 18시 이후는 같은 평일 운행의 시간대 변형
 * - 시내 셔틀 / 시내 셔틀 (주말·공휴일·방학): 서로 겹치지 않음 (평일·학기중 vs 주말·공휴일·방학)
 */
export function routeNamesForFilter(routeName?: string): string[] {
  if (!routeName) return [...TARGET_NAMES]
  if (routeName === MYONGJI_STATION_ROUTE_NAME) {
    return [MYONGJI_STATION_ROUTE_NAME, MYONGJI_STATION_AFTER18_ROUTE_NAME]
  }
  return [routeName]
}

export function routeMatchesFilter(actualName: string | null | undefined, filterName: string): boolean {
  if (!actualName) return false
  return routeNamesForFilter(filterName).includes(actualName)
}

export type ScheduleWithRoute = ScheduleRow & {
  routes: Pick<RouteRow, 'id' | 'route_name' | 'direction' | 'description' | 'is_active'> | null
}

/** 노선 + 시간표 조인 조회 (명지대 3노선 중심) */
export async function fetchSchedulesWithRoutes(params?: {
  semester?: SemesterType
  weekday?: Weekday
  routeName?: string
}): Promise<ScheduleWithRoute[] | null> {
  if (!isSupabaseConfigured) return null

  const { data: routeRows, error: routeErr } = await supabase
    .from('routes')
    .select('id, route_name')
    .in('route_name', routeNamesForFilter(params?.routeName))

  if (routeErr) {
    console.error('[routes]', routeErr.message)
    return null
  }
  if (!routeRows?.length) return []

  const routeIds = routeRows.map((r) => r.id)

  // start_location/end_location 은 운영 DB에 아직 없을 수 있어 조인 select에서 제외
  let q = supabase
    .from('schedules')
    .select('*, routes(id, route_name, direction, description, is_active)')
    .in('route_id', routeIds)
    .order('departure_time', { ascending: true })

  if (params?.semester) q = q.eq('semester', params.semester)
  if (params?.weekday) q = q.eq('weekday', params.weekday)

  const { data, error } = await q
  if (error) {
    console.error('[schedules]', error.message)
    return null
  }

  return (data ?? []) as unknown as ScheduleWithRoute[]
}

/**
 * mju_pier_ 추출 시간표를 routes / schedules 에 upsert.
 * 로그인된 ADMIN 세션 + RLS(is_admin / authenticated) 필요.
 */
export async function seedMjuTimetableToDb(): Promise<{ ok: boolean; message: string; counts?: Record<string, number> }> {
  if (!isSupabaseConfigured) {
    return { ok: false, message: 'Supabase 미설정' }
  }

  const {
    data: { session },
  } = await supabase.auth.getSession()
  if (!session) {
    return { ok: false, message: '로그인이 필요합니다. ADMIN 계정으로 로그인한 뒤 다시 시도하세요.' }
  }

  // 1) routes upsert by name (기본 3 + 변형 2)
  for (const r of BASE_AND_VARIANT_DEFS) {
    const { data: existing, error: findErr } = await supabase
      .from('routes')
      .select('id')
      .eq('route_name', r.name)
      .maybeSingle()

    if (findErr) return { ok: false, message: `routes 조회 실패: ${findErr.message}` }

    if (existing?.id) {
      const { error } = await supabase
        .from('routes')
        .update({
          direction: r.direction,
          description: r.description,
          is_active: true,
        })
        .eq('id', existing.id)
      if (error) return { ok: false, message: `routes 수정 실패: ${error.message}` }
    } else {
      const { error } = await supabase.from('routes').insert({
        route_name: r.name,
        direction: r.direction,
        description: r.description,
        is_active: true,
      })
      if (error) return { ok: false, message: `routes 등록 실패: ${error.message}` }
    }
  }

  const { data: routeRows, error: routeErr } = await supabase
    .from('routes')
    .select('id, route_name')
    .in('route_name', [...TARGET_NAMES])

  if (routeErr || !routeRows?.length) {
    return { ok: false, message: routeErr?.message ?? '노선 조회 실패' }
  }

  const idByName = new Map(routeRows.map((r) => [r.route_name, r.id]))
  const routeIds = routeRows.map((r) => r.id)

  // 2) 기존 해당 노선 스케줄 삭제
  const { error: delErr } = await supabase.from('schedules').delete().in('route_id', routeIds)
  if (delErr) return { ok: false, message: `schedules 삭제 실패: ${delErr.message}` }

  // 3) 새 스케줄 bulk insert
  const expanded = expandScheduleRows()
  const inserts = expanded
    .map((row) => {
      const route_id = idByName.get(row.routeName)
      if (!route_id) return null
      return {
        route_id,
        departure_time: row.departure_time,
        weekday: row.weekday,
        semester: row.semester,
      }
    })
    .filter(Boolean) as {
    route_id: string
    departure_time: string
    weekday: Weekday
    semester: SemesterType
  }[]

  const chunk = 200
  for (let i = 0; i < inserts.length; i += chunk) {
    const slice = inserts.slice(i, i + chunk)
    const { error } = await supabase.from('schedules').insert(slice)
    if (error) return { ok: false, message: `schedules 등록 실패 (${i}): ${error.message}` }
  }

  const counts: Record<string, number> = {}
  for (const row of expanded) {
    counts[row.routeName] = (counts[row.routeName] ?? 0) + 1
  }

  return {
    ok: true,
    message: `DB 반영 완료 · schedules ${inserts.length}건`,
    counts,
  }
}

/** 명지대 3노선 스케줄이 로컬 기준과 다르면 자동 재동기화 */
export async function ensureMjuTimetableSynced(): Promise<{
  ok: boolean
  changed: boolean
  message: string
}> {
  if (!isSupabaseConfigured) {
    return { ok: false, changed: false, message: 'Supabase 미설정' }
  }

  const expected = expandScheduleRows()
  const expectedVac = expected.filter((r) => r.semester === 'VACATION').length
  const expectedSem = expected.filter((r) => r.semester === 'SEMESTER').length

  const current = await fetchSchedulesWithRoutes()
  if (current === null) {
    return { ok: false, changed: false, message: '스케줄 조회 실패' }
  }

  const vac = current.filter((r) => r.semester === 'VACATION').length
  const sem = current.filter((r) => r.semester === 'SEMESTER').length

  if (current.length === expected.length && vac === expectedVac && sem === expectedSem) {
    return { ok: true, changed: false, message: '시간표 동기화 상태 정상' }
  }

  const seeded = await seedMjuTimetableToDb()
  return {
    ok: seeded.ok,
    changed: seeded.ok,
    message: seeded.ok
      ? `시간표 자동 동기화 완료 · ${seeded.message}`
      : seeded.message,
  }
}
