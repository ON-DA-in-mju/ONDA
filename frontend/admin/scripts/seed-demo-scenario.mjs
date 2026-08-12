/**
 * 시연 mock → Supabase (API로 가능한 부분)
 * - Auth 계정 signup/login
 * - buses / stops / vehicles / 보강 schedules
 * - SQL Editor에서 seed_demo_scenario.sql 실행 후 operations 롤오버
 *
 * Usage: node scripts/seed-demo-scenario.mjs
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

/** Auth 최소 6자 — 기사 앱 mock(1234) 대체 */
const DRIVER_PASSWORD = '123456'

const ACCOUNTS = [
  { email: 'admin@mju.ac.kr', password: 'Admin1234!', name: '관리자', role: 'ADMIN', login_id: 'admin', phone: null },
  { email: 'operator1@mju.ac.kr', password: DRIVER_PASSWORD, name: '김운영', role: 'ADMIN', login_id: 'operator1', phone: '010-2000-0001' },
  { email: 'operator2@mju.ac.kr', password: DRIVER_PASSWORD, name: '이운영', role: 'ADMIN', login_id: 'operator2', phone: '010-2000-0002' },
  { email: 'user01@mju.ac.kr', password: DRIVER_PASSWORD, name: '박사용', role: 'DRIVER', login_id: 'user01', phone: '010-1111-2222' },
  { email: 'user02@mju.ac.kr', password: DRIVER_PASSWORD, name: '최사용', role: 'DRIVER', login_id: 'user02', phone: '010-3333-4444' },
  { email: 'user03@mju.ac.kr', password: DRIVER_PASSWORD, name: '정사용', role: 'DRIVER', login_id: 'user03', phone: '010-5555-6666' },
  { email: 'user04@mju.ac.kr', password: DRIVER_PASSWORD, name: '한사용', role: 'DRIVER', login_id: 'user04', phone: '010-7777-8888' },
  { email: 'user05@mju.ac.kr', password: DRIVER_PASSWORD, name: '임사용', role: 'DRIVER', login_id: 'user05', phone: '010-9999-0000' },
]

const BUSES = [
  { id: '33333333-3333-3333-3333-333333333301', bus_name: '1호차', vehicle_number: '72버 1234', capacity: 45, status: 'ACTIVE' },
  { id: '33333333-3333-3333-3333-333333333302', bus_name: '2호차', vehicle_number: '73버 1122', capacity: 45, status: 'MAINTENANCE' },
  { id: '33333333-3333-3333-3333-333333333303', bus_name: '3호차', vehicle_number: '72버 5678', capacity: 45, status: 'ACTIVE' },
  { id: '33333333-3333-3333-3333-333333333304', bus_name: '4호차', vehicle_number: '75버 9900', capacity: 45, status: 'INACTIVE' },
]

const STOPS = [
  { id: '22222222-2222-2222-2222-222222222201', stop_name: '기흥역 5번 출구', latitude: 37.2754, longitude: 127.1159 },
  { id: '22222222-2222-2222-2222-222222222205', stop_name: '명지대역', latitude: 37.2381, longitude: 127.1905 },
  { id: '22222222-2222-2222-2222-222222222211', stop_name: '채플관 앞', latitude: 37.224, longitude: 127.1872 },
  { id: '22222222-2222-2222-2222-222222222212', stop_name: '학생회관', latitude: 37.2225, longitude: 127.1888 },
  { id: '22222222-2222-2222-2222-222222222213', stop_name: '용인시청', latitude: 37.2342, longitude: 127.2095 },
  { id: '22222222-2222-2222-2222-222222222214', stop_name: '자연캠퍼스', latitude: 37.2248, longitude: 127.187 },
]

const VEHICLES = [
  { id: '55555555-5555-5555-5555-555555555501', name: '온다 1호기', plate: '72버 1234', status: '운행 중', mileage: '84,220km', next_maintenance: '2026.08.20' },
  { id: '55555555-5555-5555-5555-555555555502', name: '온다 2호기', plate: '73버 1122', status: '정비 예정', mileage: '91,040km', next_maintenance: '2026.08.08' },
  { id: '55555555-5555-5555-5555-555555555503', name: '온다 3호기', plate: '72버 5678', status: '운행 중', mileage: '67,510km', next_maintenance: '2026.09.01' },
  { id: '55555555-5555-5555-5555-555555555504', name: '온다 6호기', plate: '75버 9900', status: '통신 이상', mileage: '102,300km', next_maintenance: '2026.08.07' },
]

const EXTRA_SCHEDULES = [
  { route_name: '기흥역 통학버스', departure_time: '08:40:00' },
  { route_name: '명지대역 셔틀', departure_time: '11:10:00' },
  { route_name: '시내 셔틀', departure_time: '12:00:00' },
]

const WEEKDAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI']

const OPS = [
  { id: '44444444-4444-4444-4444-444444440001', external_id: 'op-0905', email: 'user01@mju.ac.kr', route_name: '기흥역 통학버스', depart: '09:05:00', bus_name: '2호차', end: '09:25:00', origin: '채플관 앞', destination: '기흥역 5번 출구', round: 1 },
  { id: '44444444-4444-4444-4444-444444440002', external_id: 'op-1000', email: 'user01@mju.ac.kr', route_name: '명지대역 셔틀', depart: '10:00:00', bus_name: '1호차', end: '10:25:00', origin: '자연캠퍼스', destination: '명지대역', round: 1 },
  { id: '44444444-4444-4444-4444-444444440003', external_id: 'op-1200', email: 'user01@mju.ac.kr', route_name: '시내 셔틀', depart: '12:00:00', bus_name: '3호차', end: '12:40:00', origin: '채플관 앞', destination: '용인시청', round: 1 },
  { id: '44444444-4444-4444-4444-444444440004', external_id: 'd02-op-0840', email: 'user02@mju.ac.kr', route_name: '기흥역 통학버스', depart: '08:40:00', bus_name: '1호차', end: '09:10:00', origin: '채플관 앞', destination: '기흥역 5번 출구', round: 1 },
  { id: '44444444-4444-4444-4444-444444440005', external_id: 'd02-op-1110', email: 'user02@mju.ac.kr', route_name: '명지대역 셔틀', depart: '11:10:00', bus_name: '1호차', end: '11:40:00', origin: '자연캠퍼스', destination: '명지대역', round: 1 },
  { id: '44444444-4444-4444-4444-444444440006', external_id: 'd02-op-1420', email: 'user02@mju.ac.kr', route_name: '시내 셔틀', depart: '14:20:00', bus_name: '4호차', end: '15:00:00', origin: '채플관 앞', destination: '용인시청', round: 1 },
]

function todayKey(d = new Date()) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function weekdayOf(d = new Date()) {
  return ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'][d.getDay()]
}

function scheduleWeekday(d = new Date()) {
  const w = weekdayOf(d)
  return w === 'SAT' || w === 'SUN' ? 'MON' : w
}

async function ensureAccount(base, account) {
  const anonClient = createClient(url, anon)
  let { data: signedIn, error: loginErr } = await anonClient.auth.signInWithPassword({
    email: account.email,
    password: account.password,
  })
  if (loginErr) {
    const { data: signedUp, error: signErr } = await anonClient.auth.signUp({
      email: account.email,
      password: account.password,
      options: {
        data: { name: account.name, role: account.role, login_id: account.login_id },
      },
    })
    if (signErr) {
      console.warn(`[auth] ${account.email}: ${signErr.message}`)
      return null
    }
    if (!signedUp.session) {
      ;({ data: signedIn, error: loginErr } = await anonClient.auth.signInWithPassword({
        email: account.email,
        password: account.password,
      }))
    } else {
      signedIn = signedUp
    }
  }
  const userId = signedIn?.user?.id
  if (!userId) {
    console.warn(`[auth] ${account.email}: no session (email confirm?)`)
    return null
  }

  const row = {
    id: userId,
    name: account.name,
    email: account.email,
    role: account.role,
    phone: account.phone,
    // SQL 마이그레이션 전에도 기사 mock id 조회 가능하도록
    student_no: account.login_id,
  }
  // login_id 컬럼이 아직 없으면 SQL 마이그레이션 필요
  const withLogin = { ...row, login_id: account.login_id }
  let { error } = await base.from('users').upsert(withLogin)
  if (error?.message?.includes('login_id')) {
    ;({ error } = await base.from('users').upsert(row))
    console.warn(`[users] login_id 컬럼 없음 → student_no에 ${account.login_id} 저장. SQL 실행 권장`)
  } else if (error) {
    console.warn(`[users] ${account.email}: ${error.message}`)
  } else {
    console.log(`[users] ${account.login_id} ${account.email} (${account.role})`)
  }
  return userId
}

async function main() {
  // 관리자 세션 (마스터 데이터 upsert용)
  let admin = createClient(url, anon)
  const seedLogin = await admin.auth.signInWithPassword({
    email: 'onda.seed@mju.ac.kr',
    password: 'OndaSeed1234!',
  })
  if (seedLogin.error || !seedLogin.data.session) {
    const adminLogin = await admin.auth.signInWithPassword({
      email: 'admin@mju.ac.kr',
      password: 'Admin1234!',
    })
    if (adminLogin.error || !adminLogin.data.session) {
      throw new Error(`ADMIN 로그인 실패: ${seedLogin.error?.message || adminLogin.error?.message}`)
    }
    admin = createClient(url, anon, {
      global: { headers: { Authorization: `Bearer ${adminLogin.data.session.access_token}` } },
    })
  } else {
    admin = createClient(url, anon, {
      global: { headers: { Authorization: `Bearer ${seedLogin.data.session.access_token}` } },
    })
  }

  console.log('--- accounts ---')
  /** email → auth user id (RLS로 users 전체 select가 막힐 수 있음) */
  const userByEmail = new Map()
  for (const account of ACCOUNTS) {
    const id = await ensureAccount(admin, account)
    if (id) userByEmail.set(account.email, id)
  }

  console.log('--- buses ---')
  {
    const { error } = await admin.from('buses').upsert(BUSES)
    console.log(error ? error.message : `upsert ${BUSES.length}`)
  }
  const busByName = new Map(BUSES.map((b) => [b.bus_name, b.id]))

  console.log('--- stops ---')
  {
    const { error } = await admin.from('stops').upsert(STOPS)
    console.log(error ? error.message : `upsert ${STOPS.length}`)
  }

  console.log('--- vehicles ---')
  {
    const { error } = await admin.from('vehicles').upsert(VEHICLES)
    console.log(error ? error.message : `upsert ${VEHICLES.length}`)
  }

  console.log('--- extra schedules ---')
  const { data: routes, error: routeErr } = await admin
    .from('routes')
    .select('id, route_name')
    .in(
      'route_name',
      EXTRA_SCHEDULES.map((s) => s.route_name),
    )
  if (routeErr) throw new Error(routeErr.message)
  const idByName = new Map((routes ?? []).map((r) => [r.route_name, r.id]))
  // 배차 노선도 포함 (명지대 10:00 등)
  for (const name of ['기흥역 통학버스', '명지대역 셔틀', '시내 셔틀']) {
    if (!idByName.has(name)) {
      const { data } = await admin.from('routes').select('id, route_name').eq('route_name', name).maybeSingle()
      if (data) idByName.set(data.route_name, data.id)
    }
  }
  const scheduleRows = []
  for (const s of EXTRA_SCHEDULES) {
    const route_id = idByName.get(s.route_name)
    if (!route_id) continue
    for (const weekday of WEEKDAYS) {
      scheduleRows.push({
        route_id,
        departure_time: s.departure_time,
        weekday,
        semester: 'SEMESTER',
      })
    }
  }
  // 중복 허용되면 unique 위반 가능 → 존재하는 건 skip
  let inserted = 0
  for (const row of scheduleRows) {
    const { data: existing } = await admin
      .from('schedules')
      .select('id')
      .eq('route_id', row.route_id)
      .eq('departure_time', row.departure_time)
      .eq('weekday', row.weekday)
      .eq('semester', row.semester)
      .maybeSingle()
    if (existing?.id) continue
    const { error } = await admin.from('schedules').insert(row)
    if (error) console.warn(error.message)
    else inserted += 1
  }
  console.log(`inserted ${inserted}`)

  console.log('--- today operations ---')
  const date = todayKey()
  const wd = scheduleWeekday()

  let opsOk = 0
  for (const op of OPS) {
    const driver_id = userByEmail.get(op.email)
    const bus_id = busByName.get(op.bus_name)
    const route_id = idByName.get(op.route_name)
    if (!driver_id || !bus_id || !route_id) {
      console.warn(
        `[ops] skip ${op.external_id}: missing`,
        JSON.stringify({ driver_id: !!driver_id, bus_id: !!bus_id, route_id: !!route_id }),
      )
      continue
    }
    const { data: sch } = await admin
      .from('schedules')
      .select('id')
      .eq('route_id', route_id)
      .eq('departure_time', op.depart)
      .eq('weekday', wd)
      .eq('semester', 'SEMESTER')
      .maybeSingle()
    if (!sch?.id) {
      console.warn(`[ops] skip ${op.external_id}: schedule ${op.depart} ${wd} missing`)
      continue
    }

    const basePayload = {
      id: op.id,
      schedule_id: sch.id,
      driver_id,
      bus_id,
      operation_date: date,
      status: 'SCHEDULED',
    }
    const fullPayload = {
      ...basePayload,
      external_id: op.external_id,
      round: op.round,
      origin: op.origin,
      destination: op.destination,
      expected_end_time: op.end,
    }

    let { error } = await admin.from('operations').upsert(fullPayload)
    if (error?.message?.includes('external_id') || error?.message?.includes('schema cache')) {
      ;({ error } = await admin.from('operations').upsert(basePayload))
      if (!error) {
        console.warn(`[ops] ${op.external_id}: 기본 컬럼만 저장 — seed_demo_scenario.sql 로 컬럼 추가 후 재실행`)
      }
    }
    if (error) {
      console.warn(`[ops] ${op.external_id}: ${error.message}`)
      if (error.message.includes('row-level security')) {
        console.warn('→ SQL Editor에서 supabase/seed_demo_scenario.sql 실행 필요 (RLS + 컬럼)')
      }
    } else {
      opsOk += 1
      console.log(`[ops] ${op.external_id} ok`)
    }
  }

  console.log(`\ndone. operations upserted: ${opsOk}/${OPS.length}`)
  console.log('비밀번호(기사): 123456  | admin: Admin1234!')
  console.log('전체 SQL: supabase/seed_demo_scenario.sql')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
