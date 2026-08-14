import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { Session } from '@supabase/supabase-js'
import { isSupabaseConfigured, supabase, clearSupabaseAuthStorage, isFetchInvalidValueError } from '../lib/supabase'
import type { UserRole } from '../types/database'

/** 관리자 웹에서 쓰는 역할 — DB `user_role` 과 동일 */
export type AdminRole = UserRole

export type AuthUser = {
  id: string
  name: string
  email: string
  role: UserRole
}

export type SignupPayload = {
  name: string
  email: string
  password: string
  role: UserRole
  phone?: string
}

type AuthContextValue = {
  user: AuthUser | null
  isAuthenticated: boolean
  loading: boolean
  usingSupabase: boolean
  login: (email: string, password: string) => Promise<{ ok: boolean; message?: string }>
  signup: (payload: SignupPayload) => Promise<{ ok: boolean; message?: string }>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

const DEMO_USER: AuthUser = {
  id: 'admin-1',
  name: '관리자',
  email: 'admin@mju.ac.kr',
  role: 'ADMIN',
}

function normalizeRole(role: unknown): UserRole {
  const r = String(role ?? '').toUpperCase()
  if (r === 'DRIVER' || r === 'STUDENT' || r === 'ADMIN') return r
  return 'ADMIN'
}

async function fetchUserProfile(userId: string, fallbackEmail: string): Promise<AuthUser> {
  const { data, error } = await supabase
    .from('users')
    .select('id, email, name, role')
    .eq('id', userId)
    .maybeSingle()

  if (error || !data) {
    return {
      id: userId,
      email: fallbackEmail,
      name: fallbackEmail.split('@')[0] || '관리자',
      role: 'ADMIN',
    }
  }

  return {
    id: data.id,
    email: data.email ?? fallbackEmail,
    name: data.name,
    role: normalizeRole(data.role),
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true

    const boot = async () => {
      if (!isSupabaseConfigured) {
        const raw = sessionStorage.getItem('onda-admin-user')
        if (mounted) {
          setUser(raw ? (JSON.parse(raw) as AuthUser) : null)
          setLoading(false)
        }
        return
      }

      try {
        const { data } = await supabase.auth.getSession()
        if (!mounted) return

        if (data.session?.user) {
          const profile = await fetchUserProfile(data.session.user.id, data.session.user.email ?? '')
          if (mounted) setUser(profile)
        } else {
          setUser(null)
        }
      } catch (e) {
        if (isFetchInvalidValueError(e)) {
          clearSupabaseAuthStorage()
        }
        if (mounted) setUser(null)
      }
      if (mounted) setLoading(false)
    }

    void boot()

    if (!isSupabaseConfigured) {
      return () => {
        mounted = false
      }
    }

    const { data: sub } = supabase.auth.onAuthStateChange(async (_event, session: Session | null) => {
      if (!mounted) return
      if (session?.user) {
        const profile = await fetchUserProfile(session.user.id, session.user.email ?? '')
        if (mounted) setUser(profile)
      } else {
        setUser(null)
      }
    })

    return () => {
      mounted = false
      sub.subscription.unsubscribe()
    }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    if (!email.trim() || !password) {
      return { ok: false, message: '이메일과 비밀번호를 입력하세요.' }
    }

    if (!isSupabaseConfigured) {
      const next = { ...DEMO_USER, email: email.trim() }
      setUser(next)
      sessionStorage.setItem('onda-admin-user', JSON.stringify(next))
      return { ok: true, message: '로컬 데모 로그인 (Supabase 미설정)' }
    }

    let data: Awaited<ReturnType<typeof supabase.auth.signInWithPassword>>['data']
    let error: Awaited<ReturnType<typeof supabase.auth.signInWithPassword>>['error']
    try {
      ;({ data, error } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password,
      }))
    } catch (e) {
      if (isFetchInvalidValueError(e)) {
        clearSupabaseAuthStorage()
        try {
          ;({ data, error } = await supabase.auth.signInWithPassword({
            email: email.trim(),
            password,
          }))
        } catch (e2) {
          return {
            ok: false,
            message:
              'Supabase 연결 설정이 깨져 있습니다. Vercel 환경변수(따옴표 없이) 저장 후 Redeploy, 브라우저에서 이 사이트 데이터 삭제를 해주세요.',
          }
        }
      } else {
        const msg = e instanceof Error ? e.message : String(e)
        return { ok: false, message: msg }
      }
    }

    if (error) {
      return { ok: false, message: error.message }
    }

    if (data.user) {
      const profile = await fetchUserProfile(data.user.id, data.user.email ?? email.trim())
      if (profile.role !== 'ADMIN' && profile.role !== 'DRIVER') {
        await supabase.auth.signOut()
        setUser(null)
        return { ok: false, message: '관리자/기사 계정만 로그인할 수 있습니다.' }
      }
      setUser(profile)
    }
    return { ok: true }
  }, [])

  const signup = useCallback(async (payload: SignupPayload) => {
    if (payload.password.length < 8) {
      return { ok: false, message: '비밀번호는 8자 이상이어야 합니다.' }
    }

    const role = normalizeRole(payload.role)
    if (role === 'STUDENT') {
      return { ok: false, message: '관리자 웹에서는 STUDENT 역할로 가입할 수 없습니다.' }
    }

    if (!isSupabaseConfigured) {
      const next: AuthUser = {
        id: `admin-${Date.now()}`,
        name: payload.name,
        email: payload.email,
        role,
      }
      setUser(next)
      sessionStorage.setItem('onda-admin-user', JSON.stringify(next))
      return { ok: true, message: '로컬 데모 가입 (Supabase 미설정)' }
    }

    const { data, error } = await supabase.auth.signUp({
      email: payload.email.trim(),
      password: payload.password,
      options: {
        data: {
          name: payload.name,
          role,
          phone: payload.phone ?? null,
        },
      },
    })

    if (error) {
      return { ok: false, message: error.message }
    }

    if (data.user) {
      if (data.session) {
        const profile = await fetchUserProfile(data.user.id, payload.email.trim())
        setUser(profile)
      } else {
        return {
          ok: true,
          message: '가입 완료. 이메일 확인 후 로그인해 주세요. (Supabase Auth 설정)',
        }
      }
    }
    return { ok: true }
  }, [])

  const logout = useCallback(async () => {
    if (isSupabaseConfigured) {
      await supabase.auth.signOut()
    }
    setUser(null)
    sessionStorage.removeItem('onda-admin-user')
  }, [])

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      loading,
      usingSupabase: isSupabaseConfigured,
      login,
      signup,
      logout,
    }),
    [user, loading, login, signup, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
