/**
 * schedules → operations (학기/방학 자동 선택)
 * 기본: 2026-08-10 ~ 2026-08-16
 *
 * 학기: 3/1·9/1 부터 각 15주
 * 방학: 그 외
 * 공휴일(학기 평일): 시내 (주말·공휴일·방학)만 배차
 *
 * Usage: node scripts/seed-week-operations-from-schedules.mjs
 *        node scripts/seed-week-operations-from-schedules.mjs 2026-08-10 2026-08-16
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

const WEEK_START = process.argv[2] || '2026-08-10'
const WEEK_END = process.argv[3] || '2026-08-16'
const ROUTE_NAMES = [
  '기흥역 통학버스',
  '명지대역 셔틀',
  '명지대역 셔틀 (18시 이후)',
  '시내 셔틀',
  '시내 셔틀 (주말·공휴일·방학)',
]
const CITY_VAC = '시내 셔틀 (주말·공휴일·방학)'
const JS_TO_WEEKDAY = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

const SEMESTER_WEEKS = 15

function pad2(n) {
  return String(n).padStart(2, '0')
}

function semesterEndKey(year, month, day) {
  const start = new Date(year, month - 1, day)
  const end = new Date(start)
  end.setDate(end.getDate() + SEMESTER_WEEKS * 7 - 1)
  return `${end.getFullYear()}-${pad2(end.getMonth() + 1)}-${pad2(end.getDate())}`
}

/** 3/1·9/1 부터 15주 = 학기, 나머지 방학 */
function semesterForDate(dateKey) {
  const [y] = dateKey.split('-').map(Number)
  const springStart = `${y}-03-01`
  const springEnd = semesterEndKey(y, 3, 1)
  const fallStart = `${y}-09-01`
  const fallEnd = semesterEndKey(y, 9, 1)
  if (dateKey >= springStart && dateKey <= springEnd) return 'SEMESTER'
  if (dateKey >= fallStart && dateKey <= fallEnd) return 'SEMESTER'
  return 'VACATION'
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
    out.push({
      date,
      weekday: JS_TO_WEEKDAY[cur.getDay()],
      semester: semesterForDate(date),
      holiday: isKoreanPublicHoliday(date),
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
  if (name === '기흥역 통학버스') return 30
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
    .select('id, route_name, start_location, end_location')
    .in('route_name', ROUTE_NAMES)
  if (routeErr) throw new Error(routeErr.message)
  const routeById = new Map((routes ?? []).map((r) => [r.id, r]))
  const routeIds = [...routeById.keys()]
  if (!routeIds.length) throw new Error('대상 노선 없음')

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

  /** semester|weekday → schedules[] */
  const bySemWeekday = new Map()
  for (const s of schedules ?? []) {
    const key = `${s.semester}|${s.weekday}`
    const list = bySemWeekday.get(key) ?? []
    list.push(s)
    bySemWeekday.set(key, list)
  }

  console.log(`range ${WEEK_START} ~ ${WEEK_END}`)
  console.log('--- clear existing ops in range ---')
  const { error: delErr, count: delCount } = await client
    .from('operations')
    .delete({ count: 'exact' })
    .gte('operation_date', WEEK_START)
    .lte('operation_date', WEEK_END)
  if (delErr) throw new Error(`delete failed: ${delErr.message}`)
  console.log(`deleted ${delCount ?? '?'} rows`)

  const days = datesInRange(WEEK_START, WEEK_END)
  const rows = []
  let seq = 0

  for (const day of days) {
    let daySchedules = bySemWeekday.get(`${day.semester}|${day.weekday}`) ?? []

    // 학기 중 공휴일(평일): 시내 방학형만 — VACATION 같은 요일의 시내(주말·공휴일·방학) 스케줄 사용
    if (day.holiday && day.semester === 'SEMESTER' && day.weekday !== 'SAT' && day.weekday !== 'SUN') {
      const vacDay = bySemWeekday.get(`VACATION|${day.weekday}`) ?? []
      daySchedules = vacDay.filter((s) => routeById.get(s.route_id)?.route_name === CITY_VAC)
      console.log(
        `${day.date} ${day.weekday} HOLIDAY→city-vacation only: ${daySchedules.length} schedules`,
      )
    } else {
      console.log(`${day.date} ${day.weekday} ${day.semester}: ${daySchedules.length} schedules`)
    }

    for (const sch of daySchedules) {
      const route = routeById.get(sch.route_id)
      const driver = drivers[seq % drivers.length]
      const bus = buses[seq % buses.length]
      const depart = String(sch.departure_time).slice(0, 8)
      const duration = durationForRoute(route?.route_name)
      rows.push({
        schedule_id: sch.id,
        driver_id: driver.id,
        bus_id: bus.id,
        operation_date: day.date,
        status: 'SCHEDULED',
        external_id: `week-${day.date}-${sch.id}`,
        round: 1,
        origin: route?.start_location || route?.route_name || '',
        destination: route?.end_location || '',
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
    console.log(`  ${day.date} ${day.weekday} ${day.semester}${day.holiday ? ' HOLIDAY' : ''}: ${c}`)
  }

  // 오늘 노선명 분포 샘플
  const { data: sample } = await client
    .from('operations')
    .select('id, schedules:schedule_id(departure_time, routes:route_id(route_name))')
    .eq('operation_date', '2026-08-12')
    .limit(200)
  const names = {}
  for (const o of sample || []) {
    const n = o.schedules?.routes?.route_name || '?'
    names[n] = (names[n] || 0) + 1
  }
  console.log('2026-08-12 route distribution:', names)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
