import { createClient } from '@supabase/supabase-js'
import type { Database } from '../types/database'

const AUTH_STORAGE_KEY = 'onda-admin-auth'

/** Vercel/Windows env에 붙는 따옴표·CRLF·비가시 문자를 제거 (브라우저 fetch Invalid value 방지) */
function cleanEnv(value: string | undefined): string {
  if (!value) return ''
  return value
    .trim()
    .replace(/^["']|["']$/g, '')
    .replace(/[^\x20-\x7E]/g, '')
}

const url = cleanEnv(import.meta.env.VITE_SUPABASE_URL)
const anonKey = cleanEnv(import.meta.env.VITE_SUPABASE_ANON_KEY)

export const isSupabaseConfigured = Boolean(
  url &&
    anonKey &&
    !url.includes('YOUR_PROJECT') &&
    /^https:\/\/[a-z0-9-]+\.supabase\.co\/?$/i.test(url) &&
    anonKey.length > 20,
)

if (!isSupabaseConfigured) {
  console.warn(
    '[ONDA] Supabase 환경변수가 없습니다. Vercel Environment Variables 또는 .env.local 에 VITE_SUPABASE_URL, VITE_SUPABASE_ANON_KEY 를 넣으세요.',
  )
}

export function clearSupabaseAuthStorage() {
  try {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    sessionStorage.removeItem('onda-admin-user')
    // supabase-js가 붙이는 관련 키도 정리
    for (const key of Object.keys(localStorage)) {
      if (key.startsWith('sb-') && key.includes('auth')) {
        localStorage.removeItem(key)
      }
    }
  } catch {
    // private mode 등
  }
}

function isFetchInvalidValueError(e: unknown): boolean {
  const msg = e instanceof Error ? e.message : String(e)
  return msg.includes('Invalid value') || msg.includes('Failed to execute')
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
      storageKey: AUTH_STORAGE_KEY,
    },
  },
)

export { isFetchInvalidValueError, AUTH_STORAGE_KEY }
