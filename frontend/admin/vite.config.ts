import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { assignmentsPlugin } from './vite-assignments-plugin'
import { livePlugin } from './vite-live-plugin'
import { loginHistoryPlugin } from './vite-login-history-plugin'
import { adminNotificationsPlugin } from './vite-admin-notifications-plugin'
import { safeStopPlugin } from './vite-safe-stop-plugin'
import { naverDirectionsPlugin } from './vite-naver-directions-plugin'

function cleanViteEnv(key: string) {
  const raw = process.env[key]
  if (!raw) return
  process.env[key] = raw
    .replace(/[^\x20-\x7E]/g, '')
    .trim()
    .replace(/^["'`]+|["'`]+$/g, '')
}

cleanViteEnv('VITE_SUPABASE_URL')
cleanViteEnv('VITE_SUPABASE_ANON_KEY')
cleanViteEnv('VITE_NAVER_MAP_CLIENT_ID')

export default defineConfig({
  plugins: [
    react(),
    loginHistoryPlugin(),
    assignmentsPlugin(),
    livePlugin(),
    safeStopPlugin(),
    adminNotificationsPlugin(),
    naverDirectionsPlugin(),
  ],
  server: {
    port: 5173,
    host: true,
  },
})
