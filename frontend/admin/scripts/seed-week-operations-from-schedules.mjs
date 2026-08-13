/**
 * schedules → operations (학기/계절학기/방학 규칙)
 *
 * 학사:
 * - 3/1·9/1 개강 → 15주 정규 학기 → 4주 계절학기 → 나머지 방학
 *
 * 노선:
 * - 정규 학기: SEMESTER 스케줄 전체
 * - 계절학기: SEMESTER 스케줄 중 기흥역 제외
 * - 방학·주말·공휴일: 시내 셔틀 (주말·공휴일·방학)만
 *
 * Usage:
 *   node scripts/seed-week-operations-from-schedules.mjs
 *   node scripts/seed-week-operations-from-schedules.mjs 2026-08-10 2026-08-16
 *   node scripts/seed-week-operations-from-schedules.mjs 2026-08-01 2026-08-31 --all
 */
import fs from 'node:fs'
import path from 'node:path'
import { createClient } from '@supabase/supabase-js'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')
const env = Object.fromEntries(
  fs
    .readFileSync(path.join(root, '.env.local'), 'utf8')
    .split(/\r?\n/)
    .filter(Boolean)
    .map((l) => {
      const i = l.indexOf('=')
      return [l.slice(0, i), l.slice(i + 1)]
    }),
)

const url = env.VITE_SUPABASE_URL.replace(/\/$/, '')
const anon = env.VITE_SUPABASE_ANON_KEY

const args = process.argv.slice(2).filter((a) => a !== '--all')
const clearAll = process.argv.includes('--all')
const WEEK_START = args[0] || '2026-08-10'
const WEEK_END = args[1] || '2026-08-16'

const GIHEUNG = '기흥역 통학버스'
const CITY_VAC = '시내 셔틀 (주말·공휴일·방학)'
const ROUTE_NAMES = [
  GIHEUNG,
  '명지대역 셔틀',
  '명지대역 셔틀 (18시 이후)',
  '시내 셔틀',
  CITY_VAC,
]
const JS_TO_WEEKDAY = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
const SEMESTER_WEEKS = 15
const SEASONAL_WEEKS = 4

function pad2(n) {
  return String(n).padStart(2, '0')
}

function addDaysYmd(year, month, day, add) {
  const d = new Date(year, month - 1, day)
  d.setDate(d.getDate() + add)
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

/** SEMESTER | SEASONAL | VACATION */
function termForDate(dateKey) {
  const y = Number(dateKey.slice(0, 4))
  for (const [m, d] of [
    [3, 1],
    [9, 1],
  ]) {
    const semStart = `${y}-${pad2(m)}-${pad2(d)}`
    const semEnd = addDaysYmd(y, m, d, SEMESTER_WEEKS * 7 - 1)
    if (dateKey >= semStart && dateKey <= semEnd) return 'SEMESTER'
    const seasonalStart = addDaysYmd(y, m, d, SEMESTER_WEEKS * 7)
    const seasonalEnd = addDaysYmd(y, m, d, (SEMESTER_WEEKS + SEASONAL_WEEKS) * 7 - 1)
    if (dateKey >= seasonalStart && dateKey <= seasonalEnd) return 'SEASONAL'
  }
  const prev = y - 1
  const fallSeasonalStart = addDaysYmd(prev, 9, 1, SEMESTER_WEEKS * 7)
  const fallSeasonalEnd = addDaysYmd(prev, 9, 1, (SEMESTER_WEEKS + SEASONAL_WEEKS) * 7 - 1)
  if (dateKey >= fallSeasonalStart && dateKey <= fallSeasonalEnd) return 'SEASONAL'
  return 'VACATION'
}

function scheduleSemesterForTerm(term) {
  return term === 'VACATION' ? 'VACATION' : 'SEMESTER'
}

const FIXED_HOLIDAYS = new Set(['01-01', '03-01', '05-05', '06-06', '08-15', '10-03', '10-09', '12-25'])
const MOVABLE_HOLIDAYS = new Set([
  '2026-02-16',
  '2026-02-17',
  '2026-02-18',
  '2026-05-24',
  '2026-09-24',
  '2026-09-25',
  '2026-09-26',
])

function isKoreanPublicHoliday(dateKey) {
  if (MOVABLE_HOLIDAYS.has(dateKey)) return true
  return FIXED_HOLIDAYS.has(dateKey.slice(5))
}

function routeAllowed(routeName, term, weekendOrHoliday) {
  if (weekendOrHoliday || term === 'VACATION') return routeName === CITY_VAC
  if (term === 'SEASONAL') return routeName !== GIHEUNG
  return true
}

function addMinutesToTime(hhmmss, minutes) {
  const [h, m, s] = hhmmss.split(':').map(Number)
  const total = h * 60 + m + minutes
  const hh = String(Math.floor(total / 60) % 24).padStart(2, '0')
  const mm = String(total % 60).padStart(2, '0')
  const ss = String(s || 0).padStart(2, '0')
  return `${hh}:${mm}:${ss}`
}

function datesInRange(start, end) {
  const out = []
  const cur = new Date(`${start}T12:00:00`)
  const last = new Date(`${end}T12:00:00`)
  while (cur <= last) {
    const y = cur.getFullYear()
    const mo = String(cur.getMonth() + 1).padStart(2, '0')
    const d = String(cur.getDate()).padStart(2, '0')
    const date = `${y}-${mo}-${d}`
    const weekday = JS_TO_WEEKDAY[cur.getDay()]
    const term = termForDate(date)
    const holiday = isKoreanPublicHoliday(date)
    const weekend = weekday === 'SAT' || weekday === 'SUN'
    out.push({
      date,
      weekday,
      term,
      scheduleSemester: scheduleSemesterForTerm(term),
      holiday,
      weekend,
      cityOnly: weekend || holiday || term === 'VACATION',
    })
    cur.setDate(cur.getDate() + 1)
  }
  return out
}

function chunk(arr, size) {
  const out = []
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size))
  return out
}

function durationForRoute(name) {
  if (name === GIHEUNG) return 30
  if (name === '시내 셔틀' || name === CITY_VAC) return 40
  return 25
}

async function main() {
  let client = createClient(url, anon)
  const login = await client.auth.signInWithPassword({
    email: 'admin@mju.ac.kr',
    password: 'Admin1234!',
  })
  if (login.error || !login.data.session) {
    throw new Error(`ADMIN 로그인 실패: ${login.error?.message}`)
  }
  client = createClient(url, anon, {
    global: { headers: { Authorization: `Bearer ${login.data.session.access_token}` } },
  })

  const { data: routes, error: routeErr } = await client
    .from('routes')
    .select('id, route_name')
    .in('route_name', ROUTE_NAMES)
  if (routeErr) throw new Error(routeErr.message)
  const routeById = new Map((routes ?? []).map((r) => [r.id, r]))
  const routeIds = [...routeById.keys()]
  if (!routeIds.length) throw new Error('대상 노선 없음')

  const { data: routeStops, error: rsErr } = await client
    .from('route_stops')
    .select('route_id, stop_id, stop_order')
    .in('route_id', routeIds)
    .order('stop_order')
  if (rsErr) throw new Error(rsErr.message)
  const endsByRoute = new Map()
  for (const rs of routeStops ?? []) {
    const cur = endsByRoute.get(rs.route_id) ?? { first: null, last: null }
    if (!cur.first) cur.first = rs.stop_id
    cur.last = rs.stop_id
    endsByRoute.set(rs.route_id, cur)
  }

  const { data: drivers, error: driverErr } = await client
    .from('users')
    .select('id, login_id')
    .eq('role', 'DRIVER')
    .order('login_id')
  if (driverErr) throw new Error(driverErr.message)
  if (!drivers?.length) throw new Error('DRIVER 없음')

  const { data: buses, error: busErr } = await client
    .from('buses')
    .select('id, bus_name')
    .eq('status', 'ACTIVE')
    .order('bus_name')
  if (busErr) throw new Error(busErr.message)
  if (!buses?.length) throw new Error('ACTIVE bus 없음')

  const { data: schedules, error: schErr } = await client
    .from('schedules')
    .select('id, route_id, departure_time, weekday, semester')
    .in('route_id', routeIds)
    .order('departure_time')
  if (schErr) throw new Error(schErr.message)

  const bySemWeekday = new Map()
  for (const s of schedules ?? []) {
    const key = `${s.semester}|${s.weekday}`
    const list = bySemWeekday.get(key) ?? []
    list.push(s)
    bySemWeekday.set(key, list)
  }

  console.log(`range ${WEEK_START} ~ ${WEEK_END} (clearAll=${clearAll})`)
  console.log('--- clear existing ops ---')
  // PostgREST는 WHERE 없는 DELETE 거부 → 넓은 날짜 또는 지정 구간
  const delStart = clearAll ? '2000-01-01' : WEEK_START
  const delEnd = clearAll ? '2100-12-31' : WEEK_END
  const { error: delErr, count: delCount } = await client
    .from('operations')
    .delete({ count: 'exact' })
    .gte('operation_date', delStart)
    .lte('operation_date', delEnd)
  if (delErr) throw new Error(`delete failed: ${delErr.message}`)
  console.log(`deleted ${delCount ?? '?'} rows (${delStart}~${delEnd})`)

  const days = datesInRange(WEEK_START, WEEK_END)
  const rows = []
  let seq = 0

  for (const day of days) {
    const poolSem = day.cityOnly ? 'VACATION' : day.scheduleSemester
    let daySchedules = bySemWeekday.get(`${poolSem}|${day.weekday}`) ?? []

    daySchedules = daySchedules.filter((s) => {
      const name = routeById.get(s.route_id)?.route_name
      if (!name) return false
      return routeAllowed(name, day.term, day.cityOnly)
    })

    // 방학·주말·공휴일인데 VACATION 스케줄에 시내방학형이 없으면 SEMESTER 쪽 CITY_VAC 도 허용
    if (day.cityOnly && daySchedules.length === 0) {
      const alt = bySemWeekday.get(`SEMESTER|${day.weekday}`) ?? []
      daySchedules = alt.filter((s) => routeById.get(s.route_id)?.route_name === CITY_VAC)
    }

    console.log(
      `${day.date} ${day.weekday} term=${day.term} cityOnly=${day.cityOnly}: ${daySchedules.length} schedules`,
    )

    for (const sch of daySchedules) {
      const route = routeById.get(sch.route_id)
      const driver = drivers[seq % drivers.length]
      const bus = buses[seq % buses.length]
      const depart = String(sch.departure_time).slice(0, 8)
      const duration = durationForRoute(route?.route_name)
      const ends = endsByRoute.get(sch.route_id)
      rows.push({
        schedule_id: sch.id,
        driver_id: driver.id,
        bus_id: bus.id,
        operation_date: day.date,
        status: 'SCHEDULED',
        external_id: `week-${day.date}-${sch.id}`,
        round: 1,
        origin_stop_id: ends?.first ?? null,
        destination_stop_id: ends?.last ?? null,
        expected_end_time: addMinutesToTime(depart.length === 5 ? `${depart}:00` : depart, duration),
      })
      seq += 1
    }
  }

  console.log(`--- insert ${rows.length} operations ---`)
  let ok = 0
  for (const batch of chunk(rows, 80)) {
    const { error } = await client.from('operations').insert(batch)
    if (error) {
      console.error('batch failed:', error.message)
      for (const row of batch) {
        const { error: oneErr } = await client.from('operations').insert(row)
        if (oneErr) console.warn(row.external_id, oneErr.message)
        else ok += 1
      }
    } else {
      ok += batch.length
    }
  }

  const { count } = await client
    .from('operations')
    .select('*', { count: 'exact', head: true })
    .gte('operation_date', WEEK_START)
    .lte('operation_date', WEEK_END)

  console.log(`\ndone. inserted≈${ok}, range count=${count}`)
  for (const day of days) {
    const { count: c } = await client
      .from('operations')
      .select('*', { count: 'exact', head: true })
      .eq('operation_date', day.date)
    const { data: sample } = await client
      .from('operations')
      .select('schedules:schedule_id(routes:route_id(route_name))')
      .eq('operation_date', day.date)
      .limit(300)
    const names = {}
    for (const o of sample || []) {
      const n = o.schedules?.routes?.route_name || '?'
      names[n] = (names[n] || 0) + 1
    }
    console.log(
      `  ${day.date} ${day.weekday} ${day.term}${day.holiday ? ' HOLIDAY' : ''}: ${c} :: ${JSON.stringify(names)}`,
    )
  }
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
