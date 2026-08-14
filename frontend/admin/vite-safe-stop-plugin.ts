import type { Plugin } from 'vite'
import {
  assignmentStore,
  liveByOp,
  pushSafeStopNotification,
  safeStopStore,
  todayKey,
  type SafeStopRequest,
} from './vite-dev-store'

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

/** 기사 앱 안전 정차 요청 ↔ 관리자 승인 API */
export function safeStopPlugin(): Plugin {
  return {
    name: 'safe-stop-api',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        const rawUrl = req.url ?? ''
        const parsed = new URL(rawUrl, 'http://localhost')
        const pathname = parsed.pathname

        if (!pathname.startsWith('/api/safe-stop')) {
          next()
          return
        }

        res.setHeader('Access-Control-Allow-Origin', '*')
        res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, OPTIONS')
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type')

        if (req.method === 'OPTIONS') {
          res.statusCode = 204
          res.end()
          return
        }

        try {
          if (req.method === 'GET' && pathname === '/api/safe-stop') {
            const driverId = parsed.searchParams.get('driverId')
            const pendingOnly = parsed.searchParams.get('pending') === '1'
            let rows = [...safeStopStore]
            if (driverId) rows = rows.filter((r) => r.driverId === driverId)
            if (pendingOnly) rows = rows.filter((r) => r.decision === 'pending')
            rows.sort((a, b) => b.createdAt - a.createdAt)
            sendJson(res, 200, rows)
            return
          }

          if (req.method === 'POST' && pathname === '/api/safe-stop') {
            const body = JSON.parse((await readBody(req)) || '{}') as Partial<SafeStopRequest>
            const driverId = String(body.driverId ?? '').trim()
            const operationId = String(body.operationId ?? '').trim()
            const reason = String(body.reason ?? '').trim()
            if (!driverId || !operationId || !reason) {
              sendJson(res, 400, { ok: false, message: 'driverId, operationId, reason required' })
              return
            }
            const entry: SafeStopRequest = {
              id: body.id?.trim() || `stop-${Date.now()}`,
              driverId,
              driverName: String(body.driverName ?? driverId),
              vehicleName: String(body.vehicleName ?? '미정'),
              routeName: String(body.routeName ?? '-'),
              operationId,
              reason,
              detailReason: String(body.detailReason ?? body.message ?? '').trim(),
              requestedAt: String(body.requestedAt ?? ''),
              date: body.date || todayKey(),
              decision: 'pending',
              createdAt: Date.now(),
            }
            safeStopStore.unshift(entry)
            pushSafeStopNotification(entry)
            sendJson(res, 201, { ok: true, entry })
            return
          }

          const idMatch = pathname.match(/^\/api\/safe-stop\/([^/]+)$/)
          if (idMatch && req.method === 'PATCH') {
            const id = decodeURIComponent(idMatch[1])
            const idx = safeStopStore.findIndex((r) => r.id === id)
            if (idx < 0) {
              sendJson(res, 404, { ok: false, message: 'not found' })
              return
            }
            const body = JSON.parse((await readBody(req)) || '{}') as { decision?: string }
            const decision = body.decision
            if (decision !== 'continue' && decision !== 'stop' && decision !== 'cancelled') {
              sendJson(res, 400, { ok: false, message: 'decision must be continue|stop|cancelled' })
              return
            }
            const current = safeStopStore[idx]
            if (current.decision !== 'pending') {
              sendJson(res, 400, { ok: false, message: 'already decided' })
              return
            }
            const now = Date.now()
            safeStopStore[idx] = {
              ...current,
              decision,
              decidedAt: now,
            }

            const opId = current.operationId
            const live = liveByOp.get(opId)
            const assigned = assignmentStore.find((a) => a.id === opId)

            if (decision === 'cancelled') {
              sendJson(res, 200, { ok: true, entry: safeStopStore[idx] })
              return
            }

            if (decision === 'stop') {
              if (live) {
                liveByOp.set(opId, {
                  ...live,
                  status: 'stopped',
                  lat: null,
                  lng: null,
                  accuracy: null,
                  gpsError: false,
                  updatedAt: now,
                })
              } else {
                liveByOp.set(opId, {
                  id: opId,
                  driverId: current.driverId,
                  driverName: current.driverName,
                  vehicleName: current.vehicleName,
                  routeName: current.routeName,
                  operationId: opId,
                  status: 'stopped',
                  lat: null,
                  lng: null,
                  accuracy: null,
                  gpsError: false,
                  updatedAt: now,
                })
              }
              if (assigned) assigned.status = 'ended'
            } else if (decision === 'continue') {
              if (live && live.status === 'stopped') {
                liveByOp.set(opId, { ...live, status: 'in_progress', updatedAt: now })
              }
              if (assigned && assigned.status === 'ended') {
                assigned.status = 'in_progress'
              }
            }

            sendJson(res, 200, { ok: true, entry: safeStopStore[idx] })
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
