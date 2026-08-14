import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../state/AuthContext'

/** SPA 보호 라우트: 비로그인 시 로그인으로 이동 (전체 새로고침 없음) */
export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <div style={{ padding: 24, color: '#6b7280' }}>세션 확인 중...</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}

export function PublicOnlyRoute() {
  const { isAuthenticated, loading } = useAuth()
  if (loading) {
    return <div style={{ padding: 24, color: '#6b7280' }}>세션 확인 중...</div>
  }
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }
  return <Outlet />
}
