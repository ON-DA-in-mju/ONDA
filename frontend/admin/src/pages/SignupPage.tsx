import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Lock, Mail, Phone, User } from 'lucide-react'
import { AuthLayout, AuthSwitchLink, GoogleButton } from '../components/auth/AuthLayout'
import { Field, IconInput, PasswordInput } from '../components/ui/Form'
import { useAuth, type AdminRole } from '../state/AuthContext'

/** 회원가입 — 로그인과 동일 스플릿 레이아웃 */
export function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({
    name: '',
    email: '',
    code: '',
    password: '',
    confirm: '',
    role: '' as '' | AdminRole,
    phone: '',
    agree: false,
  })

  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!form.agree) {
      setError('이용약관에 동의해 주세요.')
      return
    }
    if (form.password !== form.confirm) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }
    if (!form.role) {
      setError('역할을 선택해 주세요.')
      return
    }
    setLoading(true)
    const result = await signup({
      name: form.name,
      email: form.email,
      password: form.password,
      role: form.role,
      phone: form.phone || undefined,
    })
    setLoading(false)
    if (!result.ok) {
      setError(result.message ?? '회원가입에 실패했습니다.')
      return
    }
    navigate('/dashboard', { replace: true })
  }

  return (
    <AuthLayout
      title="ON-DA 관리자 시스템 회원가입"
      lead="ON-DA 관리자 시스템에 오신 것을 환영합니다. 계정을 생성하고 운행 관제를 시작하세요."
      footer={<AuthSwitchLink question="이미 계정이 있으신가요?" to="/login" label="로그인" />}
    >
      <h2>회원가입</h2>
      <p className="sub">관리자 계정을 생성합니다.</p>

      <form className="auth-form" onSubmit={onSubmit}>
        <Field label="이름" required>
          <IconInput
            leftIcon={<User size={16} />}
            placeholder="이름을 입력하세요"
            value={form.name}
            onChange={(e) => set('name', e.target.value)}
            required
          />
        </Field>

        <Field label="이메일" required>
          <IconInput
            leftIcon={<Mail size={16} />}
            type="email"
            placeholder="이메일 주소를 입력하세요"
            value={form.email}
            onChange={(e) => set('email', e.target.value)}
            required
          />
        </Field>

        <Field label="이메일 인증" required>
          <div className="auth-row">
            <input
              className="input"
              placeholder="인증번호를 입력하세요"
              value={form.code}
              onChange={(e) => set('code', e.target.value)}
              required
            />
            <button className="btn btn-ghost" type="button" style={{ height: 42 }}>
              인증번호 발송
            </button>
          </div>
        </Field>

        <Field label="비밀번호" required hint="영문, 숫자, 특수문자 포함 8자 이상">
          <PasswordInput
            leftIcon={<Lock size={16} />}
            placeholder="비밀번호를 입력하세요"
            value={form.password}
            onChange={(e) => set('password', e.target.value)}
            required
          />
        </Field>

        <Field label="비밀번호 확인" required>
          <PasswordInput
            leftIcon={<Lock size={16} />}
            placeholder="비밀번호를 다시 입력하세요"
            value={form.confirm}
            onChange={(e) => set('confirm', e.target.value)}
            required
          />
        </Field>

        <Field label="역할" required>
          <select
            className="select"
            value={form.role}
            onChange={(e) => set('role', e.target.value as AdminRole | '')}
            required
          >
            <option value="">선택하세요</option>
            <option value="ADMIN">관리자 (ADMIN)</option>
            <option value="DRIVER">기사 (DRIVER)</option>
          </select>
        </Field>

        <Field label="전화번호">
          <IconInput
            leftIcon={<Phone size={16} />}
            placeholder="- 없이 숫자만 입력하세요 (선택사항)"
            value={form.phone}
            onChange={(e) => set('phone', e.target.value)}
          />
        </Field>

        <label className="auth-check">
          <input
            type="checkbox"
            checked={form.agree}
            onChange={(e) => set('agree', e.target.checked)}
          />
          이용약관 및 개인정보처리방침에 동의합니다.
          <span className="req">*</span>
        </label>

        {error ? <div className="alert alert-danger">{error}</div> : null}

        <button className="btn btn-primary btn-block" type="submit" style={{ height: 44 }} disabled={loading}>
          {loading ? '가입 중...' : '회원가입'}
        </button>

        <div className="auth-divider">또는</div>
        <GoogleButton label="Google 계정으로 가입" />
      </form>
    </AuthLayout>
  )
}
