import type { Plugin } from 'vite'

export type LoginHistoryEntry = {
  userId: string
  name: string
  time: string
  ip: string
  source: string
}

function formatNow(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const seed: LoginHistoryEntry[] = [
  { userId: 'admin', name: '관리자', time: '2026.07.20 09:32:15', ip: '192.168.10.25', source: 'admin-web' },
  { userId: 'operator1', name: '김운영', time: '2026.07.20 08:15:44', ip: '192.168.10.18', source: 'admin-web' },
  { userId: 'user01', name: '박사용', time: '2026.07.20 07:50:21', ip: '192.168.10.32', source: 'driver-app' },
  { userId: 'operator2', name: '이운영', time: '2026.07.19 17:45:09', ip: '192.168.10.18', source: 'admin-web' },
]

/** 기사 앱 ↔ 관리자 웹 로컬 연동용 인메모리 로그인 기록 API */
export function loginHistoryPlugin(): Plugin {
  const history: LoginHistoryEntry[] = [...seed]

  return {
    name: 'login-history-api',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = req.url?.split('?')[0]
        if (url !== '/api/login-history') {
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

        if (req.method === 'GET') {
          res.setHeader('Content-Type', 'application/json; charset=utf-8')
          res.end(JSON.stringify(history.slice(0, 50)))
          return
        }

        if (req.method === 'POST') {
          let body = ''
          req.on('data', (chunk) => {
            body += chunk
          })
          req.on('end', () => {
            try {
              const parsed = JSON.parse(body || '{}') as {
                userId?: string
                name?: string
                ip?: string
                source?: string
              }
              const userId = String(parsed.userId ?? '').trim()
              const name = String(parsed.name ?? '').trim()
              if (!userId || !name) {
                res.statusCode = 400
                res.end(JSON.stringify({ ok: false, message: 'userId and name required' }))
                return
              }
              const entry: LoginHistoryEntry = {
                userId,
                name,
                time: formatNow(),
                ip: String(parsed.ip ?? 'unknown'),
                source: String(parsed.source ?? 'driver-app'),
              }
              history.unshift(entry)
              if (history.length > 200) history.length = 200
              res.setHeader('Content-Type', 'application/json; charset=utf-8')
              res.end(JSON.stringify({ ok: true, entry }))
            } catch {
              res.statusCode = 400
              res.end(JSON.stringify({ ok: false, message: 'invalid json' }))
            }
          })
          return
        }

        res.statusCode = 405
        res.end('Method Not Allowed')
      })
    },
  }
}
