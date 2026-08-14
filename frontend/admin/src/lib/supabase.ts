import { createClient } from '@supabase/supabase-js'
import type { Database } from '../types/database'

const url = import.meta.env.VITE_SUPABASE_URL as string | undefined
const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined

export const isSupabaseConfigured = Boolean(url && anonKey && !url.includes('YOUR_PROJECT'))

if (!isSupabaseConfigured) {
  console.warn(
    '[ONDA] Supabase 환경변수가 없습니다. frontend/admin/.env.local 에 VITE_SUPABASE_URL, VITE_SUPABASE_ANON_KEY 를 넣으세요.',
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
