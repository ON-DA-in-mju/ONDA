import type { Plugin } from 'vite'
import { assignmentStore, liveByOp, todayKey, type LiveOpRow } from './vite-dev-store'

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

type HeartbeatBody = {
  driverId?: string
  driverName?: string
  vehicleName?: string
  routeName?: string
  operationId?: string
  status?: 'in_progress' | 'ended' | 'idle' | 'stopped'
  lat?: number | null
  lng?: number | null
  accuracy?: number | null
  gpsError?: boolean
}

type LiveRow = LiveOpRow

const STALE_MS = 45_000
const ACCURACY_ERROR_M = 80

function readBody(req: NodeJS.ReadableStream): Promise<string> {
  return new Promise((resolve, reject) => {
    let body = ''
    req.on('data', (chunk) => {
      body += chunk
    })
    req.on('end', () => resolve(body))
    req.on('error', reject)
  })
}

function sendJson(res: import('http').ServerResponse, status: number, data: unknown) {
  res.statusCode = status
  res.setHeader('Content-Type', 'application/json; charset=utf-8')
  res.end(JSON.stringify(data))
}

function formatLast(updatedAt: number, now = Date.now()): string {
  const sec = Math.max(0, Math.floor((now - updatedAt) / 1000))
  if (sec < 5) return '방금 전'
  if (sec < 60) return `${sec}초 전`
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min}분 전`
  return `${Math.floor(min / 60)}시간 전`
}

function locationLabel(lat: number | null, lng: number | null): string {
  if (lat == null || lng == null) return '위치 없음'
  return `${lat.toFixed(5)}, ${lng.toFixed(5)}`
}

function classifyGps(
  row: {
    status: LiveVehicle['status']
    lat: number | null
    lng: number | null
    accuracy: number | null
    gpsError: boolean
    updatedAt: number
  },
  now: number,
): { gpsKind: LiveGpsKind; gps: string } {
  // 운행 중이 아니면 GPS 미수신 (= 대기/종료와 동일 집계 대상)
  if (row.status !== 'in_progress') {
    return { gpsKind: 'none', gps: '미수신' }
  }
  const age = now - row.updatedAt
  if (row.gpsError || row.lat == null || row.lng == null || age > STALE_MS) {
    return { gpsKind: 'error', gps: '오류' }
  }
  if (row.accuracy != null && row.accuracy > ACCURACY_ERROR_M) {
    return { gpsKind: 'error', gps: '오류' }
  }
  return { gpsKind: 'ok', gps: '정상' }
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
      return { statusLabel: '대기', tone: 'blue' }
  }
}

function enrich(row: LiveRow, now: number): LiveVehicle {
  const { statusLabel, tone } = statusMeta(row.status)
  const { gpsKind, gps } = classifyGps(row, now)
  return {
    id: row.id,
    driverId: row.driverId,
    driverName: row.driverName,
    vehicleName: row.vehicleName,
    routeName: row.routeName,
    operationId: row.operationId,
    status: row.status,
    statusLabel,
    tone: gpsKind === 'error' && row.status === 'in_progress' ? 'red' : tone,
    lat: row.lat,
    lng: row.lng,
    accuracy: row.accuracy,
    stop: locationLabel(row.lat, row.lng),
    gps,
    gpsKind,
    updatedAt: row.updatedAt,
    last: formatLast(row.updatedAt, now),
  }
}

/** 오늘 배정 건을 기본 대기(미수신)로 두고, heartbeat로 덮어쓴다 */
function buildVehicles(liveByOp: Map<string, LiveRow>, now: number): LiveVehicle[] {
  const date = todayKey()
  const todays = assignmentStore.filter((a) => a.date === date)
  const merged = new Map<string, LiveRow>()

  for (const a of todays) {
    merged.set(a.id, {
      id: a.id,
      driverId: a.driverId,
      driverName: a.driverName,
      vehicleName: a.vehicleName,
      routeName: a.routeName,
      operationId: a.id,
      status: 'idle',
      lat: null,
      lng: null,
      accuracy: null,
      gpsError: false,
      updatedAt: now,
    })
  }

  for (const [opId, row] of liveByOp) {
    const base = merged.get(opId)
    merged.set(opId, {
      ...row,
      id: opId,
      operationId: opId,
      driverName: row.driverName || base?.driverName || row.driverId,
      vehicleName: row.vehicleName || base?.vehicleName || '미정',
      routeName: row.routeName || base?.routeName || '-',
    })
  }

  return [...merged.values()].map((row) => enrich(row, now))
}

/** 기사 앱 ↔ 관리자 웹 로컬 연동용 실시간 운행 heartbeat API */
export function livePlugin(): Plugin {
  return {
    name: 'live-api',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        const pathname = (req.url ?? '').split('?')[0]
        if (!pathname.startsWith('/api/live')) {
          next()
          return
        }

        res.setHeader('Access-Control-Allow-Origin', '*')
        res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type')

        if (req.method === 'OPTIONS') {
          res.statusCode = 204
          res.end()
          return
        }

        try {
          if (req.method === 'GET' && pathname === '/api/live/vehicles') {
            const now = Date.now()
            const vehicles = buildVehicles(liveByOp, now)
            const departOf = (id: string) =>
              assignmentStore.find((a) => a.id === id)?.departTime ?? '99:99'
            vehicles.sort((a, b) => departOf(a.operationId).localeCompare(departOf(b.operationId)))

            const ok = vehicles.filter((v) => v.gpsKind === 'ok').length
            const none = vehicles.filter((v) => v.gpsKind === 'none').length
            const error = vehicles.filter((v) => v.gpsKind === 'error').length
            const total = vehicles.length
            const rate = total === 0 ? 0 : Math.round((ok / total) * 1000) / 10

            sendJson(res, 200, {
              vehicles,
              stats: {
                ok,
                none,
                error,
                total,
                rate,
                inProgress: vehicles.filter((v) => v.status === 'in_progress').length,
                ended: vehicles.filter((v) => v.status === 'ended' || v.status === 'stopped').length,
                idle: vehicles.filter((v) => v.status === 'idle').length,
                stopped: vehicles.filter((v) => v.status === 'stopped').length,
              },
            })
            return
          }

          if (req.method === 'POST' && pathname === '/api/live/heartbeat') {
            const body = JSON.parse((await readBody(req)) || '{}') as HeartbeatBody
            const driverId = String(body.driverId ?? '').trim()
            const operationId = String(body.operationId ?? '').trim()
            if (!driverId || !operationId) {
              sendJson(res, 400, { ok: false, message: 'driverId and operationId required' })
              return
            }
            const now = Date.now()
            const prev = liveByOp.get(operationId)
            const assigned = assignmentStore.find((a) => a.id === operationId)
            let status = body.status ?? 'in_progress'
            // 관리자가 안전 정차(중단) 처리한 건은 heartbeat로 운행 중으로 되돌리지 않음
            if (prev?.status === 'stopped' && status === 'in_progress') {
              status = 'stopped'
            }

            if (status === 'in_progress') {
              for (const [opId, row] of liveByOp) {
                if (row.driverId === driverId && opId !== operationId && row.status === 'in_progress') {
                  liveByOp.set(opId, { ...row, status: 'idle', lat: null, lng: null, accuracy: null, updatedAt: now })
                  const other = assignmentStore.find((a) => a.id === opId)
                  if (other && other.status === 'in_progress') {
                    other.status = 'scheduled'
                  }
                }
              }
            }

            const entry: LiveRow = {
              id: operationId,
              driverId,
              driverName: String(body.driverName ?? prev?.driverName ?? assigned?.driverName ?? driverId),
              vehicleName: String(body.vehicleName ?? prev?.vehicleName ?? assigned?.vehicleName ?? '미정'),
              routeName: String(body.routeName ?? prev?.routeName ?? assigned?.routeName ?? '-'),
              operationId,
              status,
              lat: typeof body.lat === 'number' ? body.lat : status === 'in_progress' ? (prev?.lat ?? null) : null,
              lng: typeof body.lng === 'number' ? body.lng : status === 'in_progress' ? (prev?.lng ?? null) : null,
              accuracy: typeof body.accuracy === 'number' ? body.accuracy : null,
              gpsError: Boolean(body.gpsError),
              updatedAt: now,
            }
            liveByOp.set(operationId, entry)

            if (assigned) {
              if (status === 'in_progress') assigned.status = 'in_progress'
              else if (status === 'ended' || status === 'stopped') assigned.status = 'ended'
              else if (status === 'idle' && assigned.status === 'in_progress') assigned.status = 'scheduled'
            }

            sendJson(res, 200, { ok: true, vehicle: enrich(entry, now) })
            return
          }

          sendJson(res, 405, { ok: false, message: 'Method Not Allowed' })
        } catch (e) {
          sendJson(res, 500, { ok: false, message: e instanceof Error ? e.message : 'error' })
        }
      })
    },
  }
}
