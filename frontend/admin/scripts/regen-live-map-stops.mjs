/**
 * Regenerate frontend/admin/src/data/cityShuttleStops.ts from DB export JSON.
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const adminRoot = path.resolve(__dirname, '..')
const repoRoot = path.resolve(adminRoot, '../..')
const exportPath = path.join(repoRoot, '.tmp-docs/db-map-export.json')
const outPath = path.join(adminRoot, 'src/data/cityShuttleStops.ts')

const raw = JSON.parse(fs.readFileSync(exportPath, 'utf8'))
const joined = raw.joined.value || raw.joined

const byRoute = new Map()
for (const row of joined) {
  const rn = row.routes?.route_name
  if (!rn || !row.stops) continue
  if (!byRoute.has(rn)) byRoute.set(rn, [])
  byRoute.get(rn).push({
    id: row.stops.id,
    name: row.stops.stop_name,
    lat: Number(row.stops.latitude),
    lng: Number(row.stops.longitude),
    order: Number(row.stop_order),
  })
}

function sorted(stops) {
  return [...stops].sort((a, b) => a.order - b.order)
}

const colors = {
  '시내 셔틀': '#f97316', // 주
  '명지대역 셔틀': '#eab308', // 노
  '기흥역 통학버스': '#22c55e', // 초
  '시내 셔틀 (주말·공휴일·방학)': '#3b82f6', // 파
  '명지대역 셔틀 (18시 이후)': '#8b5cf6', // 보
}

const ids = {
  '시내 셔틀': 'city',
  '명지대역 셔틀': 'myongji',
  '기흥역 통학버스': 'giheung',
  '시내 셔틀 (주말·공휴일·방학)': 'city-vacation',
  '명지대역 셔틀 (18시 이후)': 'myongji-after18',
}

const constNames = {
  '시내 셔틀': 'CITY_SHUTTLE_STOPS',
  '명지대역 셔틀': 'MYONGJI_STATION_SHUTTLE_STOPS',
  '기흥역 통학버스': 'GIHEUNG_SHUTTLE_STOPS',
  '시내 셔틀 (주말·공휴일·방학)': 'CITY_SHUTTLE_VACATION_STOPS',
  '명지대역 셔틀 (18시 이후)': 'MYONGJI_STATION_AFTER18_STOPS',
}

const order = [
  '시내 셔틀',
  '명지대역 셔틀',
  '기흥역 통학버스',
  '시내 셔틀 (주말·공휴일·방학)',
  '명지대역 셔틀 (18시 이후)',
]

let out = `/** 지도 정류장 핀 / 노선 레이어 (Supabase routes + route_stops + stops 기준) */\n\n`
out += `export type RouteStopPin = {\n`
out += `  id: string\n`
out += `  name: string\n`
out += `  lat: number\n`
out += `  lng: number\n`
out += `  order: number\n`
out += `  /** 참고용 주소 */\n`
out += `  address?: string\n`
out += `}\n\n`
out += `export type MapRouteLayer = {\n`
out += `  id: string\n`
out += `  name: string\n`
out += `  /** 핀·노선 색 */\n`
out += `  color: string\n`
out += `  stops: RouteStopPin[]\n`
out += `}\n\n`

for (const name of order) {
  const stops = sorted(byRoute.get(name) || [])
  const cname = constNames[name]
  out += `export const ${cname}: RouteStopPin[] = [\n`
  for (const s of stops) {
    out += `  { id: ${JSON.stringify(s.id)}, name: ${JSON.stringify(s.name)}, lat: ${s.lat}, lng: ${s.lng}, order: ${s.order} },\n`
  }
  out += `]\n\n`
}

out += `export const CITY_SHUTTLE_ROUTE_NAME = '시내 셔틀'\n`
out += `export const CITY_SHUTTLE_VACATION_ROUTE_NAME = '시내 셔틀 (주말·공휴일·방학)'\n`
out += `export const MYONGJI_STATION_ROUTE_NAME = '명지대역 셔틀'\n`
out += `export const GIHEUNG_ROUTE_NAME = '기흥역 통학버스'\n\n`
out += `export const LIVE_MAP_ROUTES: MapRouteLayer[] = [\n`
for (const name of order) {
  out += `  {\n`
  out += `    id: ${JSON.stringify(ids[name])},\n`
  out += `    name: ${JSON.stringify(name)},\n`
  out += `    color: ${JSON.stringify(colors[name])},\n`
  out += `    stops: ${constNames[name]},\n`
  out += `  },\n`
}
out += `]\n`

fs.writeFileSync(outPath, out, 'utf8')
console.log('wrote', outPath)
for (const name of order) {
  console.log(name, (byRoute.get(name) || []).length)
}
