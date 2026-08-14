import type { Plugin } from 'vite'
import { loadEnv } from 'vite'

type Point = { lat: number; lng: number }

type DirectionsBody = {
  points?: Point[]
}

type DrivingResult = {
  path: Array<{ lat: number; lng: number }>
  distanceMeters: number
  durationMs: number
}

/** lng,lat 문자열 */
function toLngLat(p: Point): string {
  return `${p.lng},${p.lat}`
}

function readJson(req: import('http').IncomingMessage): Promise<string> {
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

/**
 * 네이버 Directions 5 (자동차) 프록시.
 * Client Secret은 브라우저에 노출하지 않고 Vite 서버에서만 사용.
 */
export function naverDirectionsPlugin(): Plugin {
  const cache = new Map<string, DrivingResult & { at: number }>()
  /** 캠퍼스 고정 노선 — 하루 동안 재사용 */
  const CACHE_TTL_MS = 24 * 60 * 60_000

  return {
    name: 'onda-naver-directions',
    configureServer(server) {
      const env = loadEnv(server.config.mode, process.cwd(), '')
      const clientId = (env.VITE_NAVER_MAP_CLIENT_ID || env.NAVER_MAP_CLIENT_ID || '').trim()
      const clientSecret = (env.NAVER_MAP_CLIENT_SECRET || '').trim()

      server.middlewares.use(async (req, res, next) => {
        if (!req.url?.startsWith('/api/naver/driving') || req.method !== 'POST') {
          next()
          return
        }

        if (!clientId || !clientSecret) {
          sendJson(res, 503, {
            ok: false,
            message:
              '네이버 Directions 미설정. .env.local에 NAVER_MAP_CLIENT_SECRET(Client Secret)을 넣고 서버를 재시작하세요. NCP 콘솔에서 Directions 애플리케이션도 활성화해야 합니다.',
          })
          return
        }

        try {
          const raw = await readJson(req)
          const body = JSON.parse(raw || '{}') as DirectionsBody
          const points = (body.points ?? []).filter(
            (p) => Number.isFinite(p.lat) && Number.isFinite(p.lng),
          )
          if (points.length < 2) {
            sendJson(res, 400, { ok: false, message: 'points가 2개 이상 필요합니다.' })
            return
          }

          const cacheKey = points.map((p) => `${p.lat.toFixed(5)},${p.lng.toFixed(5)}`).join('|')
          const cached = cache.get(cacheKey)
          if (cached && Date.now() - cached.at < CACHE_TTL_MS) {
            sendJson(res, 200, {
              ok: true,
              path: cached.path,
              distanceMeters: cached.distanceMeters,
              durationMs: cached.durationMs,
              cached: true,
            })
            return
          }

          const result = await fetchDrivingPath(clientId, clientSecret, points)
          cache.set(cacheKey, { ...result, at: Date.now() })
          sendJson(res, 200, { ok: true, ...result, cached: false })
        } catch (e) {
          const message = e instanceof Error ? e.message : 'Directions 요청 실패'
          console.warn('[naver-directions]', message)
          sendJson(res, 502, { ok: false, message })
        }
      })
    },
  }
}

async function fetchDrivingPath(
  clientId: string,
  clientSecret: string,
  points: Point[],
): Promise<DrivingResult> {
  // Directions 5: 경유지 최대 5개 → 출발 + 중간5 + 도착 = 최대 7점
  if (points.length <= 7) {
    return requestOneRoute(clientId, clientSecret, points)
  }
  // 그 이상이면 구간을 나눠 이어 붙임
  const out: DrivingResult = { path: [], distanceMeters: 0, durationMs: 0 }
  for (let i = 0; i < points.length - 1; i += 6) {
    const chunk = points.slice(i, Math.min(i + 7, points.length))
    if (chunk.length < 2) break
    const part = await requestOneRoute(clientId, clientSecret, chunk)
    if (out.path.length && part.path.length) part.path.shift()
    out.path.push(...part.path)
    out.distanceMeters += part.distanceMeters
    out.durationMs += part.durationMs
  }
  return out
}

async function requestOneRoute(
  clientId: string,
  clientSecret: string,
  points: Point[],
): Promise<DrivingResult> {
  const start = toLngLat(points[0])
  const goal = toLngLat(points[points.length - 1])
  const middle = points.slice(1, -1)
  const params = new URLSearchParams({
    start,
    goal,
    option: 'traoptimal',
  })
  if (middle.length) {
    params.set('waypoints', middle.map(toLngLat).join('|'))
  }

  const url = `https://maps.apigw.ntruss.com/map-direction/v1/driving?${params}`
  const res = await fetch(url, {
    headers: {
      'X-NCP-APIGW-API-KEY-ID': clientId,
      'X-NCP-APIGW-API-KEY': clientSecret,
    },
  })
  const text = await res.text()
  if (!res.ok) {
    throw new Error(`Directions HTTP ${res.status}: ${text.slice(0, 200)}`)
  }
  const data = JSON.parse(text) as {
    code?: number
    message?: string
    route?: {
      traoptimal?: Array<{
        path?: number[][]
        summary?: { distance?: number; duration?: number }
      }>
    }
  }
  if (data.code !== 0) {
    throw new Error(data.message || `Directions code=${data.code}`)
  }
  const trip = data.route?.traoptimal?.[0]
  const rawPath = trip?.path
  if (!rawPath?.length) {
    throw new Error('경로 path가 비어 있습니다')
  }
  // API: [lng, lat]
  return {
    path: rawPath.map(([lng, lat]) => ({ lat, lng })),
    distanceMeters: trip?.summary?.distance ?? 0,
    durationMs: trip?.summary?.duration ?? 0,
  }
}
