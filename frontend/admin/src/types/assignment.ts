/** 오늘 배정 — 관리자 웹 / 기사 앱 / (나중) Supabase 공통 형태 */
export type AssignmentStatus =
  | 'waiting'
  | 'departing_soon'
  | 'scheduled'
  | 'in_progress'
  | 'ended'

export type TodayAssignment = {
  id: string
  /** YYYY-MM-DD */
  date: string
  /** 미배정이면 빈 문자열 */
  driverId: string
  driverName: string
  routeName: string
  vehicleName: string
  /** HH:mm */
  departTime: string
  /** HH:mm */
  expectedEndTime: string
  origin: string
  destination: string
  round: number
  status: AssignmentStatus
}

export const DRIVER_OPTIONS = [
  { id: 'user01', name: '박사용' },
  { id: 'user02', name: '최사용' },
  { id: 'user03', name: '정사용' },
  { id: 'user04', name: '한사용' },
  { id: 'user05', name: '임사용' },
] as const

export function todayDateKey(d = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
