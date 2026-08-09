import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BarChart3, ShieldCheck, Users } from 'lucide-react'
import { Logo } from '../brand/Logo'
import heroImg from '../../assets/auth-hero.png'
import '../../styles/auth.css'

const features = [
  {
    icon: ShieldCheck,
    title: '안전한 시스템',
    desc: '관리자 권한과 민감 데이터를 안전하게 보호합니다.',
  },
  {
    icon: BarChart3,
    title: '실시간 모니터링',
    desc: '운행 상태와 이상 상황을 빠르게 확인하고 대응합니다.',
  },
  {
    icon: Users,
    title: '효율적인 관리',
    desc: '직관적인 인터페이스로 운영 업무를 효율적으로 처리합니다.',
  },
]

type AuthLayoutProps = {
  title: string
  lead: string
  children: ReactNode
  footer: ReactNode
}

/** 회원가입 등 스플릿 레이아웃용 */
export function AuthLayout({ title, lead, children, footer }: AuthLayoutProps) {
  return (
    <div className="auth-page">
      <div className="auth-shell">
        <aside className="auth-brand">
          <div className="auth-brand-logo">
            <Logo height={52} />
            <div>
              <span className="auth-brand-sub">관리자 시스템</span>
            </div>
          </div>

          <div>
            <h1>{title}</h1>
            <p className="lead">{lead}</p>
          </div>

          <div className="auth-features">
            {features.map((item) => (
              <div className="auth-feature" key={item.title}>
                <div className="auth-feature-icon">
                  <item.icon size={18} />
                </div>
                <div>
                  <strong>{item.title}</strong>
                  <span>{item.desc}</span>
                </div>
              </div>
            ))}
          </div>

          <div className="auth-illustration" aria-hidden>
            <img src={heroImg} alt="" />
          </div>

          <p className="auth-copy">© 2026 ON-DA 관리자 시스템. 모든 권리 보유.</p>
        </aside>

        <section className="auth-panel">
          {children}
          <div className="auth-footer">{footer}</div>
        </section>
      </div>
    </div>
  )
}

export function GoogleButton({ label }: { label: string }) {
  return (
    <button type="button" className="google-btn btn-block">
      <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden>
        <path
          fill="#EA4335"
          d="M9 7.2v3.6h5.1c-.2 1.2-1.5 3.5-5.1 3.5-3.1 0-5.6-2.5-5.6-5.6S5.9 3.1 9 3.1c1.8 0 3 .7 3.7 1.4l2.5-2.4C13.8.8 11.6 0 9 0 4 0 0 4 0 9s4 9 9 9c5.2 0 8.6-3.6 8.6-8.7 0-.6-.1-1-.2-1.5H9z"
        />
      </svg>
      {label}
    </button>
  )
}

export function AuthSwitchLink({
  question,
  to,
  label,
}: {
  question: string
  to: string
  label: string
}) {
  return (
    <>
      {question}
      <Link to={to}>{label}</Link>
    </>
  )
}
