/**
 * Apply Yongin official route text → routes/stops/route_stops
 * Usage: node scripts/apply-routes-variants.mjs
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

const STOPS = [
  { id: '22222222-2222-2222-2222-222222222211', stop_name: '채플관 앞', latitude: 37.224, longitude: 127.1872 },
  { id: '22222222-2222-2222-2222-222222222201', stop_name: '기흥역 5번 출구', latitude: 37.2754, longitude: 127.1159 },
  { id: '22222222-2222-2222-2222-222222222202', stop_name: '버스관리사무소', latitude: 37.2245, longitude: 127.1878 },
  { id: '22222222-2222-2222-2222-222222222215', stop_name: '상공회의소', latitude: 37.2301, longitude: 127.1889 },
  { id: '22222222-2222-2222-2222-222222222217', stop_name: '진입로(럭스나인 앞)', latitude: 37.235, longitude: 127.191 },
  { id: '22222222-2222-2222-2222-222222222218', stop_name: '동부경찰서 중앙지구대', latitude: 37.2345, longitude: 127.2005 },
  { id: '22222222-2222-2222-2222-222222222206', stop_name: '용인CGV', latitude: 37.2348, longitude: 127.2092 },
  { id: '22222222-2222-2222-2222-222222222207', stop_name: '중앙공영주차장', latitude: 37.234, longitude: 127.206 },
  { id: '22222222-2222-2222-2222-222222222204', stop_name: '진입로(역북동 주민센터)', latitude: 37.2335, longitude: 127.1895 },
  { id: '22222222-2222-2222-2222-222222222216', stop_name: '이마트', latitude: 37.231, longitude: 127.188 },
  { id: '22222222-2222-2222-2222-222222222220', stop_name: '제1공학관', latitude: 37.222, longitude: 127.186 },
  { id: '22222222-2222-2222-2222-222222222209', stop_name: '제3공학관', latitude: 37.2215, longitude: 127.1868 },
  { id: '22222222-2222-2222-2222-222222222221', stop_name: '함박관', latitude: 37.221, longitude: 127.1858 },
  { id: '22222222-2222-2222-2222-222222222222', stop_name: '창조관', latitude: 37.2235, longitude: 127.1865 },
  { id: '22222222-2222-2222-2222-222222222205', stop_name: '경전철 명지대역', latitude: 37.2381, longitude: 127.1905 },
  { id: '22222222-2222-2222-2222-222222222219', stop_name: '명지대역 사거리 정류장', latitude: 37.2375, longitude: 127.1918 },
  { id: '22222222-2222-2222-2222-222222222208', stop_name: '명진당', latitude: 37.2228, longitude: 127.1875 },
  { id: '22222222-2222-2222-2222-222222222223', stop_name: '정문', latitude: 37.2238, longitude: 127.1885 },
  { id: '22222222-2222-2222-2222-222222222210', stop_name: '생활관(명현관)', latitude: 37.2205, longitude: 127.1855 },
]

const ROUTES = [
  {
    route_name: '기흥역 통학버스',
    direction: '왕복',
    description:
      '학기 중 평일만 운행(계절학기·방학 중 제외). 명지대 버스 관리사무소 정류장(채플관 앞) → 기흥역 5번 출구 → 명지대 버스 관리사무소 정류장(채플관 앞). 편도 약 15분(교통상황에 따라 변동).',
    start_location: '채플관 앞',
    end_location: '채플관 앞',
    is_active: true,
  },
  {
    route_name: '명지대역 셔틀',
    direction: '진입로(명지대역)',
    description:
      '학기(계절학기 포함) 중 평일 운행. 4대·50회. 18:10까지만 운행. 버스관리사무소 → 상공회의소 → 진입로(럭스나인 앞) → 경전철 명지대역 → 명지대역 사거리 정류장 → 진입로(역북동 주민센터) → 이마트 → 명진당 → 제3공학관 → 함박관 → 창조관 → 버스관리사무소.',
    start_location: '버스관리사무소',
    end_location: '버스관리사무소',
    is_active: true,
  },
  {
    route_name: '시내 셔틀',
    direction: '시내',
    description:
      '학기(계절학기 포함) 중 평일 운행. 1대·10회. 18:10까지만 운행. 버스관리사무소 → 상공회의소 → 진입로(럭스나인 앞) → 동부경찰서 중앙지구대 → 용인CGV → 중앙공영주차장 → 진입로(역북동 주민센터) → 이마트 → 제1공학관 → 제3공학관 → 함박관 → 창조관 → 버스관리사무소. 공휴일·주말·방학은 「시내 셔틀 (주말·공휴일·방학)」 참고.',
    start_location: '버스관리사무소',
    end_location: '버스관리사무소',
    is_active: true,
  },
  {
    id: '66666666-6666-6666-6666-666666666002',
    route_name: '시내 셔틀 (주말·공휴일·방학)',
    direction: '시내',
    description:
      '공휴일(주말) 및 방학 중 운행. 1대·10회. 생활관(명현관) → 함박관 → 정문 → 상공회의소 → 진입로(럭스나인 앞) → 동부경찰서 중앙지구대 → 용인 CGV → 중앙공영주차장 → 경전철 명지대역 → 진입로(역북동 주민센터) → 이마트 → 제1공학관 → 생활관(명현관).',
    start_location: '생활관(명현관)',
    end_location: '생활관(명현관)',
    is_active: true,
  },
  {
    route_name: '명지대역 셔틀 (18시 이후)',
    direction: '진입로(명지대역)',
    description:
      '사용 안 함. 용인 운행 공지 기준 진입로·시내 셔틀은 18:10까지 동일 구간으로 운행하며 별도 야간 노선이 아님.',
    start_location: '버스관리사무소',
    end_location: '버스관리사무소',
    is_active: false,
  },
]

const ROUTE_STOPS = {
  '기흥역 통학버스': [
    ['22222222-2222-2222-2222-222222222211', 1, 0],
    ['22222222-2222-2222-2222-222222222201', 2, 15],
    ['22222222-2222-2222-2222-222222222211', 3, 30],
  ],
  '명지대역 셔틀': [
    ['22222222-2222-2222-2222-222222222202', 1, 0],
    ['22222222-2222-2222-2222-222222222215', 2, 5],
    ['22222222-2222-2222-2222-222222222217', 3, 8],
    ['22222222-2222-2222-2222-222222222205', 4, 12],
    ['22222222-2222-2222-2222-222222222219', 5, 14],
    ['22222222-2222-2222-2222-222222222204', 6, 17],
    ['22222222-2222-2222-2222-222222222216', 7, 20],
    ['22222222-2222-2222-2222-222222222208', 8, 24],
    ['22222222-2222-2222-2222-222222222209', 9, 26],
    ['22222222-2222-2222-2222-222222222221', 10, 28],
    ['22222222-2222-2222-2222-222222222222', 11, 30],
    ['22222222-2222-2222-2222-222222222202', 12, 33],
  ],
  '시내 셔틀': [
    ['22222222-2222-2222-2222-222222222202', 1, 0],
    ['22222222-2222-2222-2222-222222222215', 2, 5],
    ['22222222-2222-2222-2222-222222222217', 3, 8],
    ['22222222-2222-2222-2222-222222222218', 4, 12],
    ['22222222-2222-2222-2222-222222222206', 5, 16],
    ['22222222-2222-2222-2222-222222222207', 6, 18],
    ['22222222-2222-2222-2222-222222222204', 7, 22],
    ['22222222-2222-2222-2222-222222222216', 8, 25],
    ['22222222-2222-2222-2222-222222222220', 9, 29],
    ['22222222-2222-2222-2222-222222222209', 10, 31],
    ['22222222-2222-2222-2222-222222222221', 11, 33],
    ['22222222-2222-2222-2222-222222222222', 12, 35],
    ['22222222-2222-2222-2222-222222222202', 13, 38],
  ],
  '시내 셔틀 (주말·공휴일·방학)': [
    ['22222222-2222-2222-2222-222222222210', 1, 0],
    ['22222222-2222-2222-2222-222222222221', 2, 2],
    ['22222222-2222-2222-2222-222222222223', 3, 4],
    ['22222222-2222-2222-2222-222222222215', 4, 8],
    ['22222222-2222-2222-2222-222222222217', 5, 11],
    ['22222222-2222-2222-2222-222222222218', 6, 15],
    ['22222222-2222-2222-2222-222222222206', 7, 18],
    ['22222222-2222-2222-2222-222222222207', 8, 20],
    ['22222222-2222-2222-2222-222222222205', 9, 23],
    ['22222222-2222-2222-2222-222222222204', 10, 26],
    ['22222222-2222-2222-2222-222222222216', 11, 28],
    ['22222222-2222-2222-2222-222222222220', 12, 32],
    ['22222222-2222-2222-2222-222222222210', 13, 36],
  ],
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
  if (login.error || !login.data.session) throw new Error(login.error?.message || 'admin login failed')
  client = createClient(url, anon, {
    global: { headers: { Authorization: `Bearer ${login.data.session.access_token}` } },
  })

  const { error: stopErr } = await client.from('stops').upsert(STOPS)
  if (stopErr) throw new Error(`stops: ${stopErr.message}`)
  console.log(`stops ${STOPS.length}`)

  for (const r of ROUTES) {
    const { data: existing } = await client.from('routes').select('id').eq('route_name', r.route_name).maybeSingle()
    const patch = {
      direction: r.direction,
      description: r.description,
      start_location: r.start_location,
      end_location: r.end_location,
      is_active: r.is_active,
    }
    if (existing?.id) {
      const { error } = await client.from('routes').update(patch).eq('id', existing.id)
      if (error) throw new Error(`update ${r.route_name}: ${error.message}`)
      console.log(`updated ${r.route_name} active=${r.is_active}`)
    } else if (r.is_active !== false || r.id) {
      const { error } = await client.from('routes').insert({
        ...(r.id ? { id: r.id } : {}),
        route_name: r.route_name,
        ...patch,
      })
      if (error) throw new Error(`insert ${r.route_name}: ${error.message}`)
      console.log(`inserted ${r.route_name}`)
    }
  }

  const activeNames = Object.keys(ROUTE_STOPS)
  const { data: routeRows, error: routeErr } = await client
    .from('routes')
    .select('id, route_name')
    .in('route_name', [...activeNames, '명지대역 셔틀 (18시 이후)'])
  if (routeErr) throw new Error(routeErr.message)
  const idByName = new Map((routeRows ?? []).map((r) => [r.route_name, r.id]))
  const allIds = [...idByName.values()]

  const { error: delErr } = await client.from('route_stops').delete().in('route_id', allIds)
  if (delErr) throw new Error(`route_stops delete: ${delErr.message}`)

  const rows = []
  for (const [name, stops] of Object.entries(ROUTE_STOPS)) {
    const route_id = idByName.get(name)
    if (!route_id) throw new Error(`missing route ${name}`)
    for (const [stop_id, stop_order, expected_minutes] of stops) {
      rows.push({ route_id, stop_id, stop_order, expected_minutes })
    }
  }
  const { error: insErr } = await client.from('route_stops').insert(rows)
  if (insErr) throw new Error(`route_stops insert: ${insErr.message}`)
  console.log(`route_stops ${rows.length}`)

  const { data: check } = await client
    .from('routes')
    .select('route_name, is_active, description')
    .in('route_name', [...activeNames, '명지대역 셔틀 (18시 이후)'])
    .order('route_name')
  for (const r of check ?? []) {
    console.log(`\n[${r.is_active ? 'ON' : 'OFF'}] ${r.route_name}`)
    console.log(r.description)
  }
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
