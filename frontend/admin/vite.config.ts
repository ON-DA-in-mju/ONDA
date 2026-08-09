import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { assignmentsPlugin } from './vite-assignments-plugin'
import { livePlugin } from './vite-live-plugin'
import { loginHistoryPlugin } from './vite-login-history-plugin'
import { adminNotificationsPlugin } from './vite-admin-notifications-plugin'
import { safeStopPlugin } from './vite-safe-stop-plugin'

export default defineConfig({
  plugins: [
    react(),
    loginHistoryPlugin(),
    assignmentsPlugin(),
    livePlugin(),
    safeStopPlugin(),
    adminNotificationsPlugin(),
  ],
  server: {
    port: 5173,
    host: true,
  },
})
