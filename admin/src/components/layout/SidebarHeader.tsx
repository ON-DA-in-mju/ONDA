import type { ReactNode } from 'react'
import { Bell } from 'lucide-react'
import { Logo } from '../brand/Logo'

export type NavItem = {
  to: string
  label: string
  icon: ReactNode
}

type SidebarProps = {
  items: NavItem[]
  activePath: string
  onNavigate: (to: string) => void
  onLogout: () => void
}

export function Sidebar({ items, activePath, onNavigate, onLogout }: SidebarProps) {
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <Logo height={56} />
      </div>

      <nav className="nav-list" aria-label="관리자 메뉴">
        {items.map((item) => {
          const active = activePath === item.to || activePath.startsWith(`${item.to}/`)
          return (
            <button
              key={item.to}
              type="button"
              className={`nav-item${active ? ' active' : ''}`}
              onClick={() => onNavigate(item.to)}
            >
              {item.icon}
              {item.label}
            </button>
          )
        })}
      </nav>

      <button className="logout-btn" type="button" onClick={onLogout}>
        로그아웃
      </button>
    </aside>
  )
}

type HeaderProps = {
  title: string
  userName: string
  userEmail: string
  notificationCount: number
}

export function Header({ title, userName, userEmail, notificationCount }: HeaderProps) {
  return (
    <header className="admin-header">
      <h1>{title}</h1>
      <div className="header-right">
        <div className="bell-wrap" aria-label="알림">
          <Bell size={16} />
          {notificationCount > 0 ? <span className="bell-badge">{notificationCount}</span> : null}
        </div>
        <div className="profile">
          <div className="avatar">{userName.slice(0, 1)}</div>
          <div>
            <strong>{userName}</strong>
            <span>{userEmail}</span>
          </div>
        </div>
      </div>
    </header>
  )
}
