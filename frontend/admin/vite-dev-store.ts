import type { TodayAssignment } from './src/types/assignment'

function todayDateKey(d = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 요일과 무관하게 매일 동일하게 쓰는 배차 템플릿 (user01 3건 + user02 3건) */
const ASSIGNMENT_TEMPLATES: Omit<TodayAssignment, 'date' | 'status'>[] = [
  {
    id: 'op-0905',
    driverId: 'user01',
    driverName: '박사용',
    routeName: '기흥역 통학버스',
    vehicleName: '2호차',
    departTime: '09:05',
    expectedEndTime: '09:25',
    origin: '채플관 앞',
    destination: '기흥역 5번 출구',
    round: 1,
  },
  {
    id: 'op-1000',
    driverId: 'user01',
    driverName: '박사용',
    routeName: '명지대역 셔틀',
    vehicleName: '1호차',
    departTime: '10:00',
    expectedEndTime: '10:25',
    origin: '자연캠퍼스',
    destination: '명지대역',
    round: 1,
  },
  {
    id: 'op-1200',
    driverId: 'user01',
    driverName: '박사용',
    routeName: '시내 셔틀',
    vehicleName: '3호차',
    departTime: '12:00',
    expectedEndTime: '12:40',
    origin: '채플관 앞',
    destination: '용인시청',
    round: 1,
  },
  {
    id: 'd02-op-0840',
    driverId: 'user02',
    driverName: '최사용',
    routeName: '기흥역 통학버스',
    vehicleName: '1호차',
    departTime: '08:40',
    expectedEndTime: '09:10',
    origin: '채플관 앞',
    destination: '기흥역 5번 출구',
    round: 1,
  },
  {
    id: 'd02-op-1110',
    driverId: 'user02',
    driverName: '최사용',
    routeName: '명지대역 셔틀',
    vehicleName: '1호차',
    departTime: '11:10',
    expectedEndTime: '11:40',
    origin: '자연캠퍼스',
    destination: '명지대역',
    round: 1,
  },
  {
    id: 'd02-op-1420',
    driverId: 'user02',
    driverName: '최사용',
    routeName: '시내 셔틀',
    vehicleName: '4호차',
    departTime: '14:20',
    expectedEndTime: '15:00',
    origin: '채플관 앞',
    destination: '용인시청',
    round: 1,
  },
]

function cloneTemplates(date: string, status: TodayAssignment['status'] = 'scheduled'): TodayAssignment[] {
  return ASSIGNMENT_TEMPLATES.map((t) => ({ ...t, date, status }))
}

/** Vite 플러그인 간 공유 인메모리 배정 저장소 (당일 운행 상태 포함) */
export const assignmentStore: TodayAssignment[] = cloneTemplates(todayDateKey())

/**
 * 자정(날짜)이 바뀌면 당일 배차를 템플릿으로 롤오버하고 상태를 운행 예정으로 리셋한다.
 * 다른 요일 조회는 동일 템플릿을 scheduled로 반환한다.
 */
export function ensureAssignmentsForDate(date: string): TodayAssignment[] {
  const today = todayDateKey()
  const storeDate = assignmentStore[0]?.date

  // 서버가 켜진 채로 날짜가 넘어가면 당일 상태·라이브 초기화
  if (storeDate && storeDate !== today) {
    assignmentStore.splice(0, assignmentStore.length, ...cloneTemplates(today))
    liveByOp.clear()
  }

  // 템플릿 누락 시 복구
  if (assignmentStore.length === 0) {
    assignmentStore.push(...cloneTemplates(today))
  }

  if (date === today) {
    // 당일: 스토어에 없는 템플릿 id가 있으면 보충
    for (const t of ASSIGNMENT_TEMPLATES) {
      if (!assignmentStore.some((a) => a.id === t.id && a.date === today)) {
        assignmentStore.push({ ...t, date: today, status: 'scheduled' })
      }
    }
    return assignmentStore.filter((a) => a.date === today)
  }

  // 과거/미래 요일: 동일 패턴을 예정 상태로만 보여 줌 (데모용)
  return cloneTemplates(date, 'scheduled')
}

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
