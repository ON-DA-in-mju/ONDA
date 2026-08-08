import type { Plugin } from 'vite'
import { withResolvedStatus } from './src/lib/assignmentStatus'
import type { TodayAssignment } from './src/types/assignment'
import { assignmentStore, todayKey } from './vite-dev-store'

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

/** 기사 앱 ↔ 관리자 웹 로컬 연동용 인메모리 오늘 배정 API */
export function assignmentsPlugin(): Plugin {
  return {
    name: 'assignments-api',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        const rawUrl = req.url ?? ''
        const parsed = new URL(rawUrl, 'http://localhost')
        const pathname = parsed.pathname

        if (!pathname.startsWith('/api/assignments')) {
          next()
          return
        }

        res.setHeader('Access-Control-Allow-Origin', '*')
        res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS')
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type')

        if (req.method === 'OPTIONS') {
          res.statusCode = 204
          res.end()
          return
        }

        try {
          if (req.method === 'GET' && pathname === '/api/assignments') {
            const date = parsed.searchParams.get('date') ?? todayKey()
            const driverId = parsed.searchParams.get('driverId')
            let rows = assignmentStore.filter((a) => a.date === date)
            if (driverId) rows = rows.filter((a) => a.driverId === driverId)
            rows = [...rows]
              .sort((a, b) => a.departTime.localeCompare(b.departTime))
              .map((row) => withResolvedStatus(row))
            sendJson(res, 200, rows)
            return
          }

          if (req.method === 'POST' && pathname === '/api/assignments') {
            const body = JSON.parse((await readBody(req)) || '{}') as Partial<TodayAssignment>
            if (!body.driverId || !body.routeName || !body.departTime) {
              sendJson(res, 400, { ok: false, message: 'driverId, routeName, departTime required' })
              return
            }
            const entry: TodayAssignment = {
              id: body.id?.trim() || `op-${Date.now()}`,
              date: body.date || todayKey(),
              driverId: String(body.driverId),
              driverName: String(body.driverName || body.driverId),
              routeName: String(body.routeName),
              vehicleName: String(body.vehicleName || '미정'),
              departTime: String(body.departTime),
              expectedEndTime: String(body.expectedEndTime || body.departTime),
              origin: String(body.origin || ''),
              destination: String(body.destination || ''),
              round: Number(body.round || 1),
              status: (body.status as TodayAssignment['status']) || 'scheduled',
            }
            assignmentStore.unshift(entry)
            sendJson(res, 201, { ok: true, entry })
            return
          }

          const idMatch = pathname.match(/^\/api\/assignments\/([^/]+)$/)
          if (idMatch && (req.method === 'PUT' || req.method === 'PATCH')) {
            const id = decodeURIComponent(idMatch[1])
            const idx = assignmentStore.findIndex((a) => a.id === id)
            if (idx < 0) {
              sendJson(res, 404, { ok: false, message: 'not found' })
              return
            }
            const body = JSON.parse((await readBody(req)) || '{}') as Partial<TodayAssignment>
            assignmentStore[idx] = { ...assignmentStore[idx], ...body, id }
            sendJson(res, 200, { ok: true, entry: assignmentStore[idx] })
            return
          }

          if (idMatch && req.method === 'DELETE') {
            const id = decodeURIComponent(idMatch[1])
            const idx = assignmentStore.findIndex((a) => a.id === id)
            if (idx < 0) {
              sendJson(res, 404, { ok: false, message: 'not found' })
              return
            }
            const [removed] = assignmentStore.splice(idx, 1)
            sendJson(res, 200, { ok: true, entry: removed })
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
