import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Lock, User } from 'lucide-react'
import { Logo } from '../components/brand/Logo'
import { Field, IconInput, PasswordInput } from '../components/ui/Form'
import { useAuth } from '../state/AuthContext'
import { supabaseKeyKind, supabasePublicHost } from '../lib/supabase'
import heroImg from '../assets/auth-hero.png'
import '../styles/login.css'

/** ADM-00 관리자 로그인 — Figma 중앙 폼 + 하단 일러스트 */
export function LoginPage() {
  const { login, usingSupabase } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/dashboard'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    const result = await login(email, password)
    setLoading(false)
    if (!result.ok) {
      setError(result.message ?? '로그인에 실패했습니다.')
      return
    }
    navigate(from, { replace: true })
  }

  return (
    <div className="login-page">
      <div className="login-shell">
        <div className="login-top">
          <Logo height={36} />
          <span className="login-contact">
            계정 문의 : <a href="mailto:admin@mju.ac.kr">admin@mju.ac.kr</a>
          </span>
        </div>

        <div className="login-main">
          <h1>셔틀버스 관리자 시스템</h1>
          <p className="lead">
            명지대학교 셔틀버스 서비스 관리를 위한
            <br />
            관리자 전용 시스템입니다.
          </p>
          <p className="muted" style={{ fontSize: 12, marginTop: -8, marginBottom: 12, textAlign: 'center' }}>
            {usingSupabase
              ? `Supabase 연결됨 · ${supabasePublicHost} · ${supabaseKeyKind}`
              : 'Supabase 미설정 · Vercel Environment Variables 확인'}
          </p>

          <form className="login-form" onSubmit={onSubmit}>
            <Field label="관리자 ID" required>
              <IconInput
                leftIcon={<User size={16} />}
                type="email"
                placeholder="아이디 (예: admin@mju.ac.kr)"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="username"
              />
            </Field>

            <Field label="비밀번호" required>
              <PasswordInput
                leftIcon={<Lock size={16} />}
                placeholder="비밀번호 (예: Admin1234!)"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </Field>

            {error ? <div className="alert alert-danger">{error}</div> : null}

            <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <p className="login-links">
            아직 계정이 없으신가요?
            <Link to="/signup">회원가입</Link>
          </p>
        </div>

        <div className="login-hero" aria-hidden>
          <img src={heroImg} alt="" />
        </div>
      </div>
    </div>
  )
}
