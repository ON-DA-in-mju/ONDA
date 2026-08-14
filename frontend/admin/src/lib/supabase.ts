import { createClient } from '@supabase/supabase-js'
import type { Database } from '../types/database'

/** Vercel/Windows env에 붙는 따옴표·CRLF를 제거 (없으면 fetch 'Invalid value' 발생) */
function cleanEnv(value: string | undefined): string {
  if (!value) return ''
  return value
    .trim()
    .replace(/^["']|["']$/g, '')
    .replace(/[\r\n\u0000]/g, '')
}

const url = cleanEnv(import.meta.env.VITE_SUPABASE_URL)
const anonKey = cleanEnv(import.meta.env.VITE_SUPABASE_ANON_KEY)

export const isSupabaseConfigured = Boolean(
  url &&
    anonKey &&
    !url.includes('YOUR_PROJECT') &&
    url.startsWith('https://') &&
    anonKey.length > 20,
)

if (!isSupabaseConfigured) {
  console.warn(
    '[ONDA] Supabase 환경변수가 없습니다. Vercel Environment Variables 또는 .env.local 에 VITE_SUPABASE_URL, VITE_SUPABASE_ANON_KEY 를 넣으세요.',
  )
}

/** 브라우저에서만 쓰는 anon 클라이언트 (service_role 키 절대 넣지 마세요) */
export const supabase = createClient<Database>(
  url || 'https://placeholder.supabase.co',
  anonKey || 'placeholder',
  {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true,
      storageKey: 'onda-admin-auth',
    },
  },
)
