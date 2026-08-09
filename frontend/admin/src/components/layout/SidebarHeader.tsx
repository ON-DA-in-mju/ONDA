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
  onNotificationClick?: () => void
  onProfileClick?: () => void
}

export function Header({
  title,
  userName,
  userEmail,
  notificationCount,
  onNotificationClick,
  onProfileClick,
}: HeaderProps) {
  return (
    <header className="admin-header">
      <h1>{title}</h1>
      <div className="header-right">
        <button className="bell-wrap" type="button" aria-label="알림" onClick={onNotificationClick}>
          <Bell size={22} />
          {notificationCount > 0 ? <span className="bell-badge">{notificationCount}</span> : null}
        </button>
        <button className="profile" type="button" onClick={onProfileClick} aria-label="내 계정 설정">
          <div className="avatar">{userName.slice(0, 1)}</div>
          <div>
            <strong>{userName}</strong>
            <span>{userEmail}</span>
          </div>
        </button>
      </div>
    </header>
  )
}
