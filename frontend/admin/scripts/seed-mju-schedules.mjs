/**
 * One-off seed runner (Node). Reads .env.local and inserts MJU schedules.
 * - 명지대역 18:00~ → 명지대역 셔틀 (18시 이후)
 * - 시내 주말·방학 → 시내 셔틀 (주말·공휴일·방학)
 *
 * Usage: node scripts/seed-mju-schedules.mjs
 */
import fs from 'node:fs'
import path from 'node:path'
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
const key = env.VITE_SUPABASE_ANON_KEY
const email = 'onda.seed@mju.ac.kr'
const password = 'OndaSeed1234!'

const WEEKDAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI']
const WEEKEND = ['SAT', 'SUN']

const GIHEUNG = [
  '08:15', '08:25', '09:05', '09:10', '10:00', '10:05', '12:00', '13:00', '14:00', '15:15', '16:15', '17:15',
  '18:15', '19:15',
]
const MJU_SEM = [
  '08:00', '08:15', '08:20', '08:25', '08:35', '08:45', '08:50', '09:00', '09:15', '09:25', '09:30', '09:35',
  '09:40', '09:55', '10:00', '10:20', '10:30', '10:40', '10:45', '11:00', '11:25', '11:30', '11:45', '11:55',
  '12:05', '12:20', '12:30', '12:45', '13:00', '13:25', '13:40', '14:00', '14:10', '14:15', '14:30', '14:50',
  '15:00', '15:10', '15:25', '15:30', '15:55', '16:10', '16:25', '16:30', '16:50', '17:00', '17:10', '17:20',
  '17:30', '17:45', '18:00', '19:00', '19:20', '19:30',
]
const CITY_SEM = ['08:05', '08:55', '10:10', '11:20', '13:10', '14:20', '15:40', '16:35', '18:10', '20:00']
const CITY_WE = ['08:20', '09:20', '10:20', '11:20', '12:20', '13:20', '15:20', '16:20', '17:20', '18:00']
const MJU_VAC = [
  '08:00', '08:15', '08:20', '08:25', '08:35', '08:45', '08:50', '09:00', '09:15', '09:25', '09:35', '09:40',
  '09:55', '10:00', '10:20', '10:40', '10:45', '11:00', '11:25', '11:45', '11:55', '12:05', '12:20', '12:45',
  '13:00', '13:40', '14:00', '14:10', '14:15', '14:50', '15:00', '15:10', '15:25', '15:55', '16:10', '16:25',
  '16:50', '17:00', '17:10', '17:20', '17:30', '17:45', '18:00',
]
const CITY_VAC_WD = ['08:05', '08:55', '10:10', '11:20', '13:10', '14:20', '15:40', '16:35', '18:10', '19:00', '20:00']

const ROUTE_GIHEUNG = '기흥역 통학버스'
const ROUTE_MYONGJI = '명지대역 셔틀'
const ROUTE_MYONGJI_AFTER18 = '명지대역 셔틀 (18시 이후)'
const ROUTE_CITY = '시내 셔틀'
const ROUTE_CITY_VAC = '시내 셔틀 (주말·공휴일·방학)'
const ALL_ROUTE_NAMES = [ROUTE_GIHEUNG, ROUTE_MYONGJI, ROUTE_MYONGJI_AFTER18, ROUTE_CITY, ROUTE_CITY_VAC]

function isAfter18(t) {
  const [h, m] = t.split(':').map(Number)
  return h * 60 + m >= 18 * 60
}

function resolveRoute(base, time, weekday, semester) {
  if (base === ROUTE_MYONGJI) return isAfter18(time) ? ROUTE_MYONGJI_AFTER18 : ROUTE_MYONGJI
  if (base === ROUTE_CITY) {
    const weekend = weekday === 'SAT' || weekday === 'SUN'
    return weekend || semester === 'VACATION' ? ROUTE_CITY_VAC : ROUTE_CITY
  }
  return base
}

function expand(baseRoute, times, days, semester) {
  const out = []
  for (const t of times) {
    for (const d of days) {
      out.push({
        routeName: resolveRoute(baseRoute, t, d, semester),
        departure_time: `${t}:00`,
        weekday: d,
        semester,
      })
    }
  }
  return out
}

async function main() {
  const login = await fetch(`${url}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: { apikey: key, 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const lb = await login.json()
  if (!lb.access_token) throw new Error(JSON.stringify(lb))

  const headers = {
    apikey: key,
    Authorization: `Bearer ${lb.access_token}`,
    'Content-Type': 'application/json',
    Prefer: 'return=minimal',
  }

  const idByName = {}
  for (const name of ALL_ROUTE_NAMES) {
    const res = await fetch(`${url}/rest/v1/routes?route_name=eq.${encodeURIComponent(name)}&select=id`, {
      headers: { apikey: key, Authorization: `Bearer ${lb.access_token}` },
    })
    const arr = await res.json()
    if (!arr[0]?.id) throw new Error(`missing route ${name}`)
    idByName[name] = arr[0].id
  }

  const ids = Object.values(idByName)
  await fetch(`${url}/rest/v1/schedules?route_id=in.(${ids.join(',')})`, {
    method: 'DELETE',
    headers,
  })

  let drafted = []
  drafted = drafted.concat(expand(ROUTE_GIHEUNG, GIHEUNG, WEEKDAYS, 'SEMESTER'))
  drafted = drafted.concat(expand(ROUTE_MYONGJI, MJU_SEM, WEEKDAYS, 'SEMESTER'))
  drafted = drafted.concat(expand(ROUTE_CITY, CITY_SEM, WEEKDAYS, 'SEMESTER'))
  drafted = drafted.concat(expand(ROUTE_CITY, CITY_WE, WEEKEND, 'SEMESTER'))
  drafted = drafted.concat(expand(ROUTE_MYONGJI, MJU_VAC, WEEKDAYS, 'VACATION'))
  drafted = drafted.concat(expand(ROUTE_CITY, CITY_VAC_WD, WEEKDAYS, 'VACATION'))
  drafted = drafted.concat(expand(ROUTE_CITY, CITY_WE, WEEKEND, 'VACATION'))

  const rows = drafted.map((r) => ({
    route_id: idByName[r.routeName],
    departure_time: r.departure_time,
    weekday: r.weekday,
    semester: r.semester,
  }))

  const counts = {}
  for (const r of drafted) counts[r.routeName] = (counts[r.routeName] || 0) + 1
  console.log('schedule counts by route:', counts)

  const chunk = 150
  for (let i = 0; i < rows.length; i += chunk) {
    const slice = rows.slice(i, i + chunk)
    const res = await fetch(`${url}/rest/v1/schedules`, {
      method: 'POST',
      headers,
      body: JSON.stringify(slice),
    })
    if (!res.ok) {
      throw new Error(`insert ${i}: ${res.status} ${await res.text()}`)
    }
    console.log(`inserted ${i + slice.length}/${rows.length}`)
  }

  const check = await fetch(`${url}/rest/v1/schedules?select=id&route_id=in.(${ids.join(',')})`, {
    headers: { apikey: key, Authorization: `Bearer ${lb.access_token}`, Prefer: 'count=exact' },
    method: 'HEAD',
  })
  console.log('done. content-range', check.headers.get('content-range'))
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
