import { Navigate, Route, Routes } from 'react-router-dom'
import { AdminLayout } from './components/layout/AdminLayout'
import { ProtectedRoute, PublicOnlyRoute } from './components/routing/RouteGuards'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { DashboardPage } from './pages/DashboardPage'
import {
  ScheduleBulkPage,
  ScheduleDetailPage,
  SchedulesPage,
  ScheduleSuspendPage,
} from './pages/SchedulesPages'
import { LiveDetailPage, LivePage, LiveSuspendPage } from './pages/LivePages'
import {
  DriversPage,
  NoticesPage,
  ReportsPage,
  RouteDetailPage,
  RoutesPage,
  SettingsPage,
  StopsPage,
  SystemPage,
  UsersPage,
  VehiclesPage,
} from './pages/ManagePages'

/**
 * SPA 루트
 * - React Router로 클라이언트 라우팅 (전체 새로고침 없음)
 * - Virtual DOM: 상태 변경 시 필요한 컴포넌트만 재렌더
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />

      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AdminLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/schedules" element={<SchedulesPage />} />
          <Route path="/schedules/detail" element={<ScheduleDetailPage />} />
          <Route path="/schedules/bulk" element={<ScheduleBulkPage />} />
          <Route path="/schedules/suspend" element={<ScheduleSuspendPage />} />
          <Route path="/live" element={<LivePage />} />
          <Route path="/live/detail" element={<LiveDetailPage />} />
          <Route path="/live/suspend" element={<LiveSuspendPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/notices" element={<NoticesPage />} />
          <Route path="/routes" element={<RoutesPage />} />
          <Route path="/routes/detail" element={<RouteDetailPage />} />
          <Route path="/stops" element={<StopsPage />} />
          <Route path="/vehicles" element={<VehiclesPage />} />
          <Route path="/drivers" element={<DriversPage />} />
          <Route path="/users" element={<UsersPage />} />
          <Route path="/system" element={<SystemPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
