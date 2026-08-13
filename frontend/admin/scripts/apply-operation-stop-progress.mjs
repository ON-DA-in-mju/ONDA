/**
 * Apply operation_stop_progress DDL if the DB already exposes a SQL runner.
 * Otherwise verifies the table after SQL Editor execution.
 *
 * Usage: node scripts/apply-operation-stop-progress.mjs
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
const sql = fs.readFileSync(
  path.join(root, 'supabase', 'migrate_operation_stop_progress.sql'),
  'utf8',
)

const client = createClient(url, anon)
const login = await client.auth.signInWithPassword({
  email: 'admin@mju.ac.kr',
  password: 'Admin1234!',
})
if (login.error) {
  console.error('login failed:', login.error.message)
  process.exit(1)
}

const { error } = await client.from('operation_stop_progress').select('operation_id').limit(1)
if (!error) {
  console.log('operation_stop_progress already exists')
  process.exit(0)
}

console.error('table missing:', error.message)
console.error('Run supabase/migrate_operation_stop_progress.sql in SQL Editor (postgres).')
console.error('SQL file length:', sql.length)
process.exit(2)
