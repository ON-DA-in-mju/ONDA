import type { Plugin } from 'vite'
import { adminNotificationStore } from './vite-dev-store'

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

/** 관리자 알림 API */
export function adminNotificationsPlugin(): Plugin {
  return {
    name: 'admin-notifications-api',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        const rawUrl = req.url ?? ''
        const parsed = new URL(rawUrl, 'http://localhost')
        const pathname = parsed.pathname

        if (!pathname.startsWith('/api/admin-notifications')) {
          next()
          return
        }

        res.setHeader('Access-Control-Allow-Origin', '*')
        res.setHeader('Access-Control-Allow-Methods', 'GET, PATCH, OPTIONS')
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type')

        if (req.method === 'OPTIONS') {
          res.statusCode = 204
          res.end()
          return
        }

        try {
          if (req.method === 'GET' && pathname === '/api/admin-notifications') {
            const unreadOnly = parsed.searchParams.get('unread') === '1'
            let rows = [...adminNotificationStore]
            if (unreadOnly) rows = rows.filter((n) => !n.read)
            rows.sort((a, b) => b.createdAt - a.createdAt)
            sendJson(res, 200, {
              items: rows,
              unreadCount: adminNotificationStore.filter((n) => !n.read).length,
            })
            return
          }

          if (req.method === 'PATCH' && pathname === '/api/admin-notifications/read-all') {
            for (let i = 0; i < adminNotificationStore.length; i++) {
              adminNotificationStore[i] = { ...adminNotificationStore[i], read: true }
            }
            sendJson(res, 200, { ok: true })
            return
          }

          const idMatch = pathname.match(/^\/api\/admin-notifications\/([^/]+)$/)
          if (idMatch && req.method === 'PATCH') {
            const id = decodeURIComponent(idMatch[1])
            const idx = adminNotificationStore.findIndex((n) => n.id === id)
            if (idx < 0) {
              sendJson(res, 404, { ok: false, message: 'not found' })
              return
            }
            const body = JSON.parse((await readBody(req)) || '{}') as { read?: boolean }
            adminNotificationStore[idx] = {
              ...adminNotificationStore[idx],
              read: body.read !== false,
            }
            sendJson(res, 200, { ok: true, entry: adminNotificationStore[idx] })
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
