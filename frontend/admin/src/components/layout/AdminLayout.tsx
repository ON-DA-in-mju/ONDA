import { useEffect, useRef, useState } from 'react'
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
import {
  ADMIN_NOTIFICATIONS_CHANGED,
  fetchAdminNotifications,
  markNotificationRead,
  type AdminNotification,
} from '../../lib/adminNotificationsApi'
import { createExclusivePoll } from '../../lib/exclusivePoll'
import { Header, Sidebar, type NavItem } from './SidebarHeader'
import '../../styles/layout.css'

const TOAST_DURATION_MS = 8_000

const titles: Record<string, string> = {
  '/dashboard': '대시보드',
  '/schedules': '오늘의 운행·배차 목록',
  '/schedules/operations': '운행 관리',
  '/schedules/assignments': '기사 배정',
  '/schedules/detail': '운행 일정 상세',
  '/schedules/suspend': '운행 중단·기상악화 처리',
  '/live': '실시간 운행 관제',
  '/live/detail': '오늘의 운행 목록',
  '/live/suspend': '안전 정차 요청',
  '/notifications': '알림',
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
  const { user, logout, loading } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [unreadCount, setUnreadCount] = useState(0)
  const [toast, setToast] = useState<AdminNotification | null>(null)
  const knownIdsRef = useRef<Set<string> | null>(null)
  const toastTimerRef = useRef<number | null>(null)

  const dismissToast = () => {
    if (toastTimerRef.current != null) {
      window.clearTimeout(toastTimerRef.current)
      toastTimerRef.current = null
    }
    setToast(null)
  }

  const showToast = (item: AdminNotification) => {
    if (toastTimerRef.current != null) window.clearTimeout(toastTimerRef.current)
    setToast(item)
    toastTimerRef.current = window.setTimeout(() => {
      setToast(null)
      toastTimerRef.current = null
    }, TOAST_DURATION_MS)
  }

  useEffect(() => {
    // 로그인 세션 준비 전 폴링하면 pending을 놓칠 수 있음
    if (loading || !user) return

    let alive = true
    const poll = createExclusivePoll(async () => {
      void import('../../lib/forceSuspendApi').then((api) => api.expireSuspendedRoutes()).catch(() => undefined)
      // 레이아웃은 pending만 — 전체 조인 조회가 쌓이면 클릭이 먹통처럼 보임
      const data = await fetchAdminNotifications({ pendingOnly: true })
      if (!alive) return
      setUnreadCount((prev) => (prev === data.unreadCount ? prev : data.unreadCount))

      const ids = new Set(data.items.map((n) => n.id))
      const unread = data.items.filter((n) => !n.read)

      if (knownIdsRef.current == null) {
        knownIdsRef.current = ids
        if (unread.length > 0) showToast(unread[0])
        return
      }

      const fresh = unread.filter((n) => !knownIdsRef.current!.has(n.id))
      knownIdsRef.current = new Set([...knownIdsRef.current, ...ids])
      if (fresh.length > 0) {
        showToast(fresh[0])
      }
    })
    void poll()
    const timer = window.setInterval(() => void poll(), 15_000)
    const onChanged = () => {
      void poll()
    }
    window.addEventListener(ADMIN_NOTIFICATIONS_CHANGED, onChanged)
    return () => {
      alive = false
      window.clearInterval(timer)
      window.removeEventListener(ADMIN_NOTIFICATIONS_CHANGED, onChanged)
      if (toastTimerRef.current != null) window.clearTimeout(toastTimerRef.current)
    }
  }, [loading, user])

  const onToastClick = async () => {
    if (!toast) return
    const target = toast
    dismissToast()
    if (!target.read) await markNotificationRead(target.id)
    navigate(target.href || '/live/suspend')
  }

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

  const title =
    titles[location.pathname] ??
    (location.pathname.startsWith('/live/detail/')
      ? '운행 상태 상세'
      : location.pathname.startsWith('/schedules/detail')
        ? '운행 일정 상세'
        : location.pathname.startsWith('/reports/detail')
          ? '제보 상세'
          : 'ON-DA 관리자')

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
          notificationCount={unreadCount}
          onNotificationClick={() => navigate('/notifications')}
        />
        <main className="admin-content">
          <Outlet />
        </main>
        {toast ? (
          <button type="button" className="admin-toast" onClick={() => void onToastClick()}>
            <strong>{toast.title}</strong>
            <span>{toast.body}</span>
            <em>클릭하면 안전 정차 요청 화면으로 이동합니다 · 8초 후 자동으로 닫힙니다</em>
          </button>
        ) : null}
      </div>
    </div>
  )
}
