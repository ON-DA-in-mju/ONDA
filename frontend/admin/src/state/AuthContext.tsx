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
import { isSupabaseConfigured, supabase } from '../lib/supabase'
import type { AdminRole } from '../types/database'

export type { AdminRole }

export type AuthUser = {
  id: string
  name: string
  email: string
  role: AdminRole
}

export type SignupPayload = {
  name: string
  email: string
  password: string
  role: AdminRole
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

async function fetchProfile(userId: string, fallbackEmail: string): Promise<AuthUser> {
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
    role: (data.role as AdminRole) || 'ADMIN',
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

      const { data } = await supabase.auth.getSession()
      if (!mounted) return

      if (data.session?.user) {
        const profile = await fetchProfile(data.session.user.id, data.session.user.email ?? '')
        if (mounted) setUser(profile)
      } else {
        setUser(null)
      }
      setLoading(false)
    }

    void boot()

    if (!isSupabaseConfigured) return () => {
      mounted = false
    }

    const { data: sub } = supabase.auth.onAuthStateChange(async (_event, session: Session | null) => {
      if (!mounted) return
      if (session?.user) {
        const profile = await fetchProfile(session.user.id, session.user.email ?? '')
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

    const { data, error } = await supabase.auth.signInWithPassword({
      email: email.trim(),
      password,
    })

    if (error) {
      return { ok: false, message: error.message }
    }

    if (data.user) {
      const profile = await fetchProfile(data.user.id, data.user.email ?? email.trim())
      setUser(profile)
    }
    return { ok: true }
  }, [])

  const signup = useCallback(async (payload: SignupPayload) => {
    if (payload.password.length < 8) {
      return { ok: false, message: '비밀번호는 8자 이상이어야 합니다.' }
    }

    if (!isSupabaseConfigured) {
      const next: AuthUser = {
        id: `admin-${Date.now()}`,
        name: payload.name,
        email: payload.email,
        role: payload.role,
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
          role: payload.role,
          phone: payload.phone ?? null,
        },
      },
    })

    if (error) {
      return { ok: false, message: error.message }
    }

    if (data.user) {
      // 이메일 확인이 켜져 있으면 session 이 없을 수 있음
      if (data.session) {
        const profile = await fetchProfile(data.user.id, payload.email.trim())
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
