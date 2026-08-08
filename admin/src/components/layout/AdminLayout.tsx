import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  Bus,
  CalendarDays,
  House,
  Megaphone,
  PencilLine,
  Settings,
  TableOfContents,
  User,
  Waypoints,
  Wrench,
} from 'lucide-react'
import { useAuth } from '../../state/AuthContext'
import { Header, Sidebar, type NavItem } from './SidebarHeader'
import '../../styles/layout.css'

const titles: Record<string, string> = {
  '/dashboard': '대시보드',
  '/schedules': '오늘의 운행·배차 목록',
  '/schedules/detail': '운행 일정 상세',
  '/schedules/bulk': '일괄 등록 미리보기',
  '/schedules/suspend': '운행 중단·기상악화 처리',
  '/live': '실시간 운행 관제',
  '/live/detail': '운행 상태 상세',
  '/live/suspend': '운행 중단 요청 처리',
  '/reports': '커뮤니티 제보 관리',
  '/notices': '공지·긴급 알림 관리',
  '/routes': '노선·운행 관리',
  '/routes/detail': '노선 상세',
  '/stops': '정류장 관리',
  '/vehicles': '차량·정비 관리',
  '/drivers': '기사 계정 관리',
  '/users': '사용자 관리',
  '/system': '시스템 기록 조회',
  '/settings': '설정',
}

export function AdminLayout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const items: NavItem[] = [
    { to: '/dashboard', label: '대시보드', icon: <House size={16} /> },
    { to: '/schedules', label: '오늘의 운행', icon: <CalendarDays size={16} /> },
    { to: '/live', label: '실시간 운행', icon: <Bus size={16} /> },
    { to: '/reports', label: '제보 관리', icon: <PencilLine size={16} /> },
    { to: '/notices', label: '공지 관리', icon: <Megaphone size={16} /> },
    { to: '/routes', label: '노선 관리', icon: <Waypoints size={16} /> },
    { to: '/vehicles', label: '차량 관리', icon: <Wrench size={16} /> },
    { to: '/users', label: '사용자 관리', icon: <User size={16} /> },
    { to: '/system', label: '시스템 관리', icon: <TableOfContents size={16} /> },
    { to: '/settings', label: '설정', icon: <Settings size={16} /> },
  ]

  const title = titles[location.pathname] ?? 'ON-DA 관리자'

  return (
    <div className="admin-shell">
      <Sidebar
        items={items}
        activePath={location.pathname}
        onNavigate={(to) => navigate(to)}
        onLogout={() => {
          void logout().then(() => navigate('/login'))
        }}
      />
      <div className="admin-main">
        <Header
          title={title}
          userName={user?.name ?? '관리자'}
          userEmail={user?.email ?? 'admin@mju.ac.kr'}
          notificationCount={3}
        />
        <main className="admin-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
