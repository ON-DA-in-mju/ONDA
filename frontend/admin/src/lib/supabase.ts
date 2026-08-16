import { createClient } from '@supabase/supabase-js'
import type { Database } from '../types/database'

const AUTH_STORAGE_KEY = 'onda-admin-auth'

/** 채팅/Vercel 붙여넣기로 섞인 따옴표·CRLF·BOM·이름=값 접두어를 제거 */
function cleanEnv(value: string | undefined): string {
  if (!value) return ''
  let v = value
    .replace(/^\uFEFF/, '')
    .replace(/[\u201C\u201D\u2018\u2019]/g, '"')
    .replace(/[^\x20-\x7E]/g, '')
    .trim()
    .replace(/^["'`]+|["'`]+$/g, '')
    .trim()

  const eq = v.indexOf('=')
  if (eq > 0) {
    const name = v.slice(0, eq)
    if (/^[A-Z][A-Z0-9_]*$/.test(name) && /SUPABASE|NAVER|VITE/.test(name)) {
      v = v.slice(eq + 1).trim().replace(/^["'`]+|["'`]+$/g, '')
    }
  }
  return v.trim()
}

function extractSupabaseUrl(raw: string | undefined): string {
  const cleaned = cleanEnv(raw)
  const match = cleaned.match(/https:\/\/[a-z0-9-]+\.supabase\.co/i)
  return match ? match[0] : cleaned.replace(/\/+$/, '')
}

function extractAnonKey(raw: string | undefined): string {
  const cleaned = cleanEnv(raw)
  const jwt = cleaned.match(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/)
  if (jwt) return jwt[0]
  const publishable = cleaned.match(/sb_publishable_[A-Za-z0-9_-]+/)
  if (publishable) return publishable[0]
  return cleaned
}

function asciiOnly(value: string): string {
  return value.replace(/[^\x20-\x7E]/g, '').trim()
}

function toSafeHeaders(init?: HeadersInit): Headers {
  const out = new Headers()
  const append = (key: string, value: string) => {
    const k = asciiOnly(String(key))
    const v = asciiOnly(String(value))
    if (k && v) out.set(k, v)
  }
  if (!init) return out
  if (init instanceof Headers) {
    init.forEach((value, key) => append(key, value))
  } else if (Array.isArray(init)) {
    for (const [key, value] of init) append(key, value)
  } else {
    for (const [key, value] of Object.entries(init)) append(key, String(value ?? ''))
  }
  return out
}

function supabaseFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const headers = toSafeHeaders(init?.headers)
  let href = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url
  href = asciiOnly(href).replace(/^["'`]+|["'`]+$/g, '')

  // Chrome throws "Invalid value" if RequestInit enums are `undefined` (common after spread).
  const next: RequestInit = { method: init?.method ?? 'GET', headers }
  if (init?.body != null) next.body = init.body
  if (init?.signal) next.signal = init.signal
  return fetch(href, next)
}

const url = extractSupabaseUrl(import.meta.env.VITE_SUPABASE_URL)
const anonKey = extractAnonKey(import.meta.env.VITE_SUPABASE_ANON_KEY)

export const isSupabaseConfigured = Boolean(
  url &&
    anonKey &&
    !url.includes('YOUR_PROJECT') &&
    /^https:\/\/[a-z0-9-]+\.supabase\.co$/i.test(url) &&
    anonKey.length > 20,
)

export const supabaseUrl = url
export const supabaseAnonKey = anonKey

export const supabasePublicHost = isSupabaseConfigured
  ? url.replace(/^https:\/\//, '')
  : ''

export const supabaseKeyKind = anonKey.startsWith('sb_publishable_')
  ? 'publishable'
  : anonKey.startsWith('eyJ')
    ? 'jwt'
    : 'unknown'

if (!isSupabaseConfigured) {
  console.warn(
    '[ONDA] Supabase 환경변수가 없습니다. Vercel Environment Variables 또는 .env.local 에 VITE_SUPABASE_URL, VITE_SUPABASE_ANON_KEY 를 넣으세요.',
  )
}

export function clearSupabaseAuthStorage() {
  try {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    sessionStorage.removeItem('onda-admin-user')
    for (const key of Object.keys(localStorage)) {
      if (key.startsWith('sb-') || key.includes('onda-admin')) {
        localStorage.removeItem(key)
      }
    }
  } catch {
    // private mode
  }
}

export function isFetchInvalidValueError(e: unknown): boolean {
  const msg = e instanceof Error ? e.message : String(e)
  return msg.includes('Invalid value') || msg.includes('Failed to execute')
}

/** 브라우저에서만 쓰는 anon 클라이언트 (service_role 키 절대 넣지 마세요) */
export const supabase = createClient<Database>(
  url || 'https://placeholder.supabase.co',
  anonKey || 'placeholder',
  {
    global: { fetch: supabaseFetch },
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true,
      storageKey: AUTH_STORAGE_KEY,
    },
  },
)

type PasswordGrantResult = {
  data: {
    user: { id: string; email?: string | null } | null
    session: { access_token: string } | null
  }
  error: { message: string } | null
}

/** supabase-js fetch 옵션을 우회하고 헤더를 직접 넣어 로그인 */
export async function signInWithPasswordRaw(
  email: string,
  password: string,
): Promise<PasswordGrantResult> {
  const res = await fetch(`${url}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: {
      apikey: anonKey,
      Authorization: `Bearer ${anonKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  })
  const json = (await res.json().catch(() => ({}))) as {
    access_token?: string
    refresh_token?: string
    error_description?: string
    msg?: string
    error?: string
  }
  if (!res.ok || !json.access_token || !json.refresh_token) {
    return {
      data: { user: null, session: null },
      error: {
        message: json.error_description || json.msg || json.error || `로그인 실패 (${res.status})`,
      },
    }
  }

  const { data, error } = await supabase.auth.setSession({
    access_token: json.access_token,
    refresh_token: json.refresh_token,
  })
  return {
    data: { user: data.user, session: data.session },
    error: error ? { message: error.message } : null,
  }
}
