import { Navigate, Route, Routes } from 'react-router-dom'
import { AdminLayout } from './components/layout/AdminLayout'
import { ProtectedRoute, PublicOnlyRoute } from './components/routing/RouteGuards'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { DashboardPage } from './pages/DashboardPage'
import {
  ScheduleAssignmentsPage,
  ScheduleDetailPage,
  ScheduleOperationsPage,
  SchedulesPage,
  ScheduleSuspendPage,
} from './pages/SchedulesPages'
import { LiveDetailPage, LivePage, LiveSuspendPage, LiveVehicleDetailPage } from './pages/LivePages'
import { NotificationsPage } from './pages/NotificationsPage'
import {
  NoticesPage,
  ReportsPage,
  RouteDetailPage,
  RoutesPage,
  SettingsPage,
  SystemPage,
  UsersPage,
  VehiclesPage,
} from './pages/ManagePages'
import {
  DriverContactPage,
  DriverFormPage,
  DriversPage,
  MaintenanceCreatePage,
  MaintenanceDetailPage,
  RouteCreatePage,
  StopFormPage,
  StopsPage,
  UserCreatePage,
} from './pages/ExtraPages'

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
          <Route path="/schedules/operations" element={<ScheduleOperationsPage />} />
          <Route path="/schedules/assignments" element={<ScheduleAssignmentsPage />} />
          <Route path="/schedules/detail" element={<ScheduleDetailPage />} />
          <Route path="/schedules/suspend" element={<ScheduleSuspendPage />} />
          <Route path="/live" element={<LivePage />} />
          <Route path="/live/detail" element={<LiveDetailPage />} />
          <Route path="/live/detail/:operationId" element={<LiveVehicleDetailPage />} />
          <Route path="/live/suspend" element={<LiveSuspendPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/notices" element={<NoticesPage />} />
          <Route path="/routes" element={<RoutesPage />} />
          <Route path="/routes/new" element={<RouteCreatePage />} />
          <Route path="/routes/detail" element={<RouteDetailPage />} />
          <Route path="/stops" element={<StopsPage />} />
          <Route path="/stops/new" element={<StopFormPage />} />
          <Route path="/vehicles" element={<VehiclesPage />} />
          <Route path="/vehicles/maintenance/new" element={<MaintenanceCreatePage />} />
          <Route path="/vehicles/maintenance/detail" element={<MaintenanceDetailPage />} />
          <Route path="/drivers" element={<DriversPage />} />
          <Route path="/drivers/new" element={<DriverFormPage />} />
          <Route path="/drivers/contact" element={<DriverContactPage />} />
          <Route path="/users" element={<UsersPage />} />
          <Route path="/users/new" element={<UserCreatePage />} />
          <Route path="/system" element={<SystemPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
