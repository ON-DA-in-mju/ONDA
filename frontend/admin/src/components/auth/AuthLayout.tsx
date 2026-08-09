import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BarChart3, ShieldCheck, Users } from 'lucide-react'
import { Logo } from '../brand/Logo'
import heroImg from '../../assets/signup-hero.png'
import googleG from '../../assets/google-g.png'
import '../../styles/auth.css'

const features = [
  {
    icon: ShieldCheck,
    title: '안전한 시스템',
    desc: '최고 수준의 보안으로 데이터를 안전하게 보호합니다.',
  },
  {
    icon: BarChart3,
    title: '실시간 모니터링',
    desc: '시스템 현황을 실시간으로 확인하고 빠르게 대응할 수 있습니다.',
  },
  {
    icon: Users,
    title: '효율적인 관리',
    desc: '직관적인 인터페이스로 시스템을 효율적으로 관리하세요.',
  },
]

type AuthLayoutProps = {
  titleLine1: string
  titleLine2: string
  lead: string
  children: ReactNode
  footer: ReactNode
}

/** 회원가입 스플릿 레이아웃 — Figma 기준 */
export function AuthLayout({ titleLine1, titleLine2, lead, children, footer }: AuthLayoutProps) {
  return (
    <div className="auth-page">
      <div className="auth-shell">
        <aside className="auth-brand">
          <div className="auth-brand-logo">
            <Logo height={44} />
            <span className="auth-brand-sub">관리자 시스템</span>
          </div>

          <div className="auth-brand-copy">
            <h1>
              {titleLine1}
              <br />
              <span className="auth-title-accent">{titleLine2}</span>
            </h1>
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
          <div className="auth-card">
            {children}
            <div className="auth-footer">{footer}</div>
          </div>
        </section>
      </div>
    </div>
  )
}

export function GoogleButton({ label }: { label: string }) {
  return (
    <button type="button" className="google-btn btn-block">
      <img src={googleG} alt="" className="google-btn-icon" width={28} height={28} />
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
      {question} <Link to={to}>{label}</Link>
    </>
  )
}
