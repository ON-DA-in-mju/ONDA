/**
 * 1) 정류장 실좌표 교정
 * 2) 용인 운행 공지 시간표 → schedules 재시드
 * Usage: node scripts/reseed-official-schedules.mjs
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

const WEEKDAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI']
const WEEKEND = ['SAT', 'SUN']
const ALL_DAYS = [...WEEKDAYS, ...WEEKEND]

/** 기흥: 학교→기흥 + 기흥→학교 */
const GIHEUNG = [
  '08:00', '09:05', '09:10', '10:00', '10:05', '12:00', '13:00', '14:00', '15:15', '16:15', '17:15', '18:15', '19:15',
  '08:15', '08:20', '09:15', '09:20', '10:15', '10:20', '12:15', '13:15', '14:15', '15:30', '16:30', '17:30', '18:30', '19:30',
]

/** 명지대역(진입로) 50회 — 공지 시간표 */
const MJU = [
  '08:00', '08:15', '08:20', '08:25', '08:35', '08:45', '08:50', '09:00', '09:15', '09:25', '09:30', '09:35', '09:40', '09:55',
  '10:00', '10:20', '10:30', '10:40', '10:45', '11:00', '11:25', '11:30', '11:45', '11:55', '12:05', '12:20', '12:30', '12:45',
  '13:00', '13:25', '13:40', '14:00', '14:10', '14:15', '14:30', '14:50', '15:00', '15:10', '15:25', '15:30', '15:55',
  '16:10', '16:25', '16:30', '16:50', '17:00', '17:10', '17:20', '17:30', '17:45', '18:00',
]

/** 시내 학기중 평일 (공지 표 기준, 18:10까지) */
const CITY = ['08:05', '08:55', '10:10', '11:20', '13:10', '14:20', '15:40', '16:35', '18:10']

/** 주말·공휴일·방학 시내 */
const CITY_WE = ['08:20', '09:20', '10:20', '11:20', '12:20', '13:20', '15:20', '16:20', '17:20', '18:00']

const STOP_COORDS = [
  { id: '22222222-2222-2222-2222-222222222201', stop_name: '기흥역 5번 출구', latitude: 37.27597, longitude: 127.11669 },
  { id: '22222222-2222-2222-2222-222222222205', stop_name: '경전철 명지대역', latitude: 37.23811, longitude: 127.19057 },
  { id: '22222222-2222-2222-2222-222222222219', stop_name: '명지대역 사거리 정류장', latitude: 37.23755, longitude: 127.19185 },
  { id: '22222222-2222-2222-2222-222222222217', stop_name: '진입로(럭스나인 앞)', latitude: 37.2362, longitude: 127.1915 },
  { id: '22222222-2222-2222-2222-222222222204', stop_name: '진입로(역북동 주민센터)', latitude: 37.23446, longitude: 127.1883 },
  { id: '22222222-2222-2222-2222-222222222215', stop_name: '상공회의소', latitude: 37.2318, longitude: 127.1894 },
  { id: '22222222-2222-2222-2222-222222222216', stop_name: '이마트', latitude: 37.23143, longitude: 127.18916 },
  { id: '22222222-2222-2222-2222-222222222218', stop_name: '동부경찰서 중앙지구대', latitude: 37.2349, longitude: 127.1988 },
  { id: '22222222-2222-2222-2222-222222222206', stop_name: '용인CGV', latitude: 37.23509, longitude: 127.20561 },
  { id: '22222222-2222-2222-2222-222222222207', stop_name: '중앙공영주차장', latitude: 37.23455, longitude: 127.2072 },
  { id: '22222222-2222-2222-2222-222222222211', stop_name: '채플관 앞', latitude: 37.22415, longitude: 127.18705 },
  { id: '22222222-2222-2222-2222-222222222202', stop_name: '버스관리사무소', latitude: 37.22405, longitude: 127.18735 },
  { id: '22222222-2222-2222-2222-222222222223', stop_name: '정문', latitude: 37.22455, longitude: 127.18875 },
  { id: '22222222-2222-2222-2222-222222222208', stop_name: '명진당', latitude: 37.22255, longitude: 127.18695 },
  { id: '22222222-2222-2222-2222-222222222220', stop_name: '제1공학관', latitude: 37.22185, longitude: 127.18615 },
  { id: '22222222-2222-2222-2222-222222222209', stop_name: '제3공학관', latitude: 37.22125, longitude: 127.18675 },
  { id: '22222222-2222-2222-2222-222222222222', stop_name: '창조관', latitude: 37.22305, longitude: 127.18665 },
  { id: '22222222-2222-2222-2222-222222222221', stop_name: '함박관', latitude: 37.22135, longitude: 127.18555 },
  { id: '22222222-2222-2222-2222-222222222210', stop_name: '생활관(명현관)', latitude: 37.22015, longitude: 127.18515 },
]

function expand(routeId, times, days, semester) {
  const out = []
  for (const t of times) {
    for (const d of days) {
      out.push({ route_id: routeId, departure_time: `${t}:00`, weekday: d, semester })
    }
  }
  return out
}

async function main() {
  let client = createClient(url, anon)
  let login = await client.auth.signInWithPassword({
    email: 'onda.seed@mju.ac.kr',
    password: 'OndaSeed1234!',
  })
  if (login.error) {
    login = await client.auth.signInWithPassword({
      email: 'admin@mju.ac.kr',
      password: 'Admin1234!',
    })
  }
  if (login.error || !login.data.session) throw new Error(login.error?.message || 'login failed')
  client = createClient(url, anon, {
    global: { headers: { Authorization: `Bearer ${login.data.session.access_token}` } },
  })

  console.log('--- stops coords ---')
  const { error: stopErr } = await client.from('stops').upsert(STOP_COORDS)
  if (stopErr) throw new Error(stopErr.message)
  console.log(`upserted ${STOP_COORDS.length}`)

  const names = ['기흥역 통학버스', '명지대역 셔틀', '시내 셔틀', '시내 셔틀 (주말·공휴일·방학)']
  const { data: routes, error: routeErr } = await client.from('routes').select('id, route_name').in('route_name', names)
  if (routeErr) throw new Error(routeErr.message)
  const id = Object.fromEntries((routes ?? []).map((r) => [r.route_name, r.id]))
  for (const n of names) if (!id[n]) throw new Error(`missing route ${n}`)

  console.log('--- delete schedules ---')
  const { error: delErr } = await client.from('schedules').delete().in('route_id', Object.values(id))
  if (delErr) throw new Error(delErr.message)

  let inserts = [
    ...expand(id['기흥역 통학버스'], GIHEUNG, WEEKDAYS, 'SEMESTER'),
    ...expand(id['명지대역 셔틀'], MJU, WEEKDAYS, 'SEMESTER'),
    ...expand(id['시내 셔틀'], CITY, WEEKDAYS, 'SEMESTER'),
    // 계절학기 평일 (VACATION + weekday) — 공지: 학기(계절학기 포함) 중 평일
    ...expand(id['명지대역 셔틀'], MJU, WEEKDAYS, 'VACATION'),
    ...expand(id['시내 셔틀'], CITY, WEEKDAYS, 'VACATION'),
    // 주말·공휴일·방학 시내 분리 노선
    ...expand(id['시내 셔틀 (주말·공휴일·방학)'], CITY_WE, WEEKEND, 'SEMESTER'),
    ...expand(id['시내 셔틀 (주말·공휴일·방학)'], CITY_WE, ALL_DAYS, 'VACATION'),
  ]

  // unique
  const seen = new Set()
  inserts = inserts.filter((r) => {
    const k = `${r.route_id}|${r.departure_time}|${r.weekday}|${r.semester}`
    if (seen.has(k)) return false
    seen.add(k)
    return true
  })

  console.log(`--- insert ${inserts.length} ---`)
  for (let i = 0; i < inserts.length; i += 200) {
    const { error } = await client.from('schedules').insert(inserts.slice(i, i + 200))
    if (error) throw new Error(`insert@${i}: ${error.message}`)
  }

  for (const n of names) {
    const { count } = await client.from('schedules').select('*', { count: 'exact', head: true }).eq('route_id', id[n])
    console.log(`${n}: ${count}`)
  }

  const { data: g } = await client
    .from('schedules')
    .select('departure_time')
    .eq('route_id', id['기흥역 통학버스'])
    .eq('weekday', 'MON')
    .eq('semester', 'SEMESTER')
    .order('departure_time')
  console.log(
    '기흥 MON:',
    [...new Set((g ?? []).map((x) => String(x.departure_time).slice(0, 5)))].join(', '),
  )

  const { data: stops } = await client
    .from('stops')
    .select('stop_name, latitude, longitude')
    .in(
      'id',
      STOP_COORDS.map((s) => s.id),
    )
    .order('stop_name')
  console.log('\ncoords:')
  for (const s of stops ?? []) console.log(`  ${s.stop_name}: ${Number(s.latitude).toFixed(5)}, ${Number(s.longitude).toFixed(5)}`)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
