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
          // `/live`가 `/live/detail`에만 매칭되도록 exact 우선, 하위는 prefix
          const active =
            activePath === item.to ||
            (item.to !== '/dashboard' && activePath.startsWith(`${item.to}/`))
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
}

export function Header({
  title,
  userName,
  userEmail,
  notificationCount,
  onNotificationClick,
}: HeaderProps) {
  return (
    <header className="admin-header">
      <h1>{title}</h1>
      <div className="header-right">
        <button
          type="button"
          className="bell-wrap"
          aria-label="알림"
          onClick={onNotificationClick}
          style={{
            border: 'none',
            background: 'transparent',
            cursor: 'pointer',
            padding: 0,
          }}
        >
          <Bell size={16} />
          {notificationCount > 0 ? <span className="bell-badge">{notificationCount}</span> : null}
        </button>
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
