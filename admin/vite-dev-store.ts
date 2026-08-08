import type { TodayAssignment } from './src/types/assignment'

function todayDateKey(d = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function seedAssignments(): TodayAssignment[] {
  const date = todayDateKey()
  return [
    {
      id: 'op-0905',
      date,
      driverId: 'user01',
      driverName: '박사용',
      routeName: '기흥역 통학버스',
      vehicleName: '2호차',
      departTime: '09:05',
      expectedEndTime: '09:25',
      origin: '채플관 앞',
      destination: '기흥역 5번 출구',
      round: 1,
      status: 'scheduled',
    },
    {
      id: 'op-1000',
      date,
      driverId: 'user01',
      driverName: '박사용',
      routeName: '명지대역 셔틀',
      vehicleName: '1호차',
      departTime: '10:00',
      expectedEndTime: '10:25',
      origin: '자연캠퍼스',
      destination: '명지대역',
      round: 1,
      status: 'scheduled',
    },
    {
      id: 'op-1200',
      date,
      driverId: 'user01',
      driverName: '박사용',
      routeName: '시내 셔틀',
      vehicleName: '3호차',
      departTime: '12:00',
      expectedEndTime: '12:40',
      origin: '채플관 앞',
      destination: '용인시청',
      round: 1,
      status: 'scheduled',
    },
    {
      id: 'd02-op-0840',
      date,
      driverId: 'user02',
      driverName: '최사용',
      routeName: '기흥역 통학버스',
      vehicleName: '1호차',
      departTime: '08:40',
      expectedEndTime: '09:10',
      origin: '채플관 앞',
      destination: '기흥역 5번 출구',
      round: 1,
      status: 'scheduled',
    },
    {
      id: 'd02-op-1110',
      date,
      driverId: 'user02',
      driverName: '최사용',
      routeName: '명지대역 셔틀',
      vehicleName: '1호차',
      departTime: '11:10',
      expectedEndTime: '11:40',
      origin: '자연캠퍼스',
      destination: '명지대역',
      round: 1,
      status: 'scheduled',
    },
    {
      id: 'd02-op-1420',
      date,
      driverId: 'user02',
      driverName: '최사용',
      routeName: '시내 셔틀',
      vehicleName: '4호차',
      departTime: '14:20',
      expectedEndTime: '15:00',
      origin: '채플관 앞',
      destination: '용인시청',
      round: 1,
      status: 'scheduled',
    },
  ]
}

/** Vite 플러그인 간 공유 인메모리 배정 저장소 */
export const assignmentStore: TodayAssignment[] = seedAssignments()

export type LiveOpStatus = 'in_progress' | 'ended' | 'idle' | 'stopped'

export type LiveOpRow = {
  id: string
  driverId: string
  driverName: string
  vehicleName: string
  routeName: string
  operationId: string
  status: LiveOpStatus
  lat: number | null
  lng: number | null
  accuracy: number | null
  gpsError: boolean
  updatedAt: number
}

/** 실시간 heartbeat (operationId 키) */
export const liveByOp = new Map<string, LiveOpRow>()

export type SafeStopDecision = 'pending' | 'continue' | 'stop' | 'cancelled'

export type SafeStopRequest = {
  id: string
  driverId: string
  driverName: string
  vehicleName: string
  routeName: string
  operationId: string
  reason: string
  /** 기사가 입력한 상세 사유 */
  detailReason: string
  requestedAt: string
  date: string
  decision: SafeStopDecision
  createdAt: number
  decidedAt?: number
}

/** 안전 정차 요청 */
export const safeStopStore: SafeStopRequest[] = []

export type AdminNotificationType = 'safe_stop'

export type AdminNotification = {
  id: string
  type: AdminNotificationType
  title: string
  body: string
  href: string
  relatedId: string
  createdAt: number
  read: boolean
}

/** 관리자 알림 (종 아이콘 / 토스트) */
export const adminNotificationStore: AdminNotification[] = []

export function pushSafeStopNotification(entry: SafeStopRequest) {
  adminNotificationStore.unshift({
    id: `notif-${entry.id}`,
    type: 'safe_stop',
    title: '안전 정차 요청',
    body: `${entry.driverName} 기사님의 중단 요청이 들어왔습니다. (${entry.vehicleName} · ${entry.reason})`,
    href: '/live/suspend',
    relatedId: entry.id,
    createdAt: Date.now(),
    read: false,
  })
}

export function todayKey(): string {
  return todayDateKey()
}
