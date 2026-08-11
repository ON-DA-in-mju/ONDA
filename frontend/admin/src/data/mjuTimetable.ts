import type { Weekday, SemesterType } from '../types/database'
import { resolveOperationalRouteName } from '../lib/routeVariants'

export const MJU_ROUTE_NAMES = ['기흥역 통학버스', '명지대역 셔틀', '시내 셔틀'] as const
export type MjuRouteName = (typeof MJU_ROUTE_NAMES)[number]

/** schedules에 저장되는 실제 노선명(변형 포함) */
export type ScheduleRouteName = string

export type MjuTrip = {
  no: number
  route: MjuRouteName
  departure: string
  via?: string
  arrival?: string
  buses?: number
}

export type MjuTimetablePack = {
  id: string
  title: string
  period: 'SEMESTER' | 'VACATION'
  daysLabel: string
  weekdays: Weekday[]
  semester: SemesterType
  trips: MjuTrip[]
  note?: string
}

const WEEKDAYS: Weekday[] = ['MON', 'TUE', 'WED', 'THU', 'FRI']
const WEEKEND: Weekday[] = ['SAT', 'SUN']

/** 학기 중 평일 — 명지대역·시내 (이미지 am+pm) */
const SEMESTER_SHUTTLE: MjuTrip[] = [
  { no: 1, route: '명지대역 셔틀', departure: '08:00', via: '08:15' },
  { no: 2, route: '시내 셔틀', departure: '08:05', via: '08:20' },
  { no: 3, route: '명지대역 셔틀', departure: '08:15', via: '08:30' },
  { no: 4, route: '명지대역 셔틀', departure: '08:20', via: '08:35' },
  { no: 5, route: '명지대역 셔틀', departure: '08:25', via: '08:40' },
  { no: 6, route: '명지대역 셔틀', departure: '08:35', via: '08:50' },
  { no: 7, route: '명지대역 셔틀', departure: '08:45', via: '09:00' },
  { no: 8, route: '명지대역 셔틀', departure: '08:50', via: '09:05' },
  { no: 9, route: '시내 셔틀', departure: '08:55', via: '09:10' },
  { no: 10, route: '명지대역 셔틀', departure: '09:00', via: '09:15' },
  { no: 11, route: '명지대역 셔틀', departure: '09:15', via: '09:30' },
  { no: 12, route: '명지대역 셔틀', departure: '09:25', via: '09:40' },
  { no: 13, route: '명지대역 셔틀', departure: '09:30', via: '09:45' },
  { no: 14, route: '명지대역 셔틀', departure: '09:35', via: '09:50' },
  { no: 15, route: '명지대역 셔틀', departure: '09:40', via: '09:55' },
  { no: 16, route: '명지대역 셔틀', departure: '09:55', via: '10:10' },
  { no: 17, route: '명지대역 셔틀', departure: '10:00', via: '10:15' },
  { no: 18, route: '시내 셔틀', departure: '10:10', via: '10:25' },
  { no: 19, route: '명지대역 셔틀', departure: '10:20', via: '10:35' },
  { no: 20, route: '명지대역 셔틀', departure: '10:30', via: '10:45' },
  { no: 21, route: '명지대역 셔틀', departure: '10:40', via: '10:55' },
  { no: 22, route: '명지대역 셔틀', departure: '10:45', via: '11:00' },
  { no: 23, route: '명지대역 셔틀', departure: '11:00', via: '11:15' },
  { no: 24, route: '시내 셔틀', departure: '11:20', via: '11:35' },
  { no: 25, route: '명지대역 셔틀', departure: '11:25', via: '11:40' },
  { no: 26, route: '명지대역 셔틀', departure: '11:30', via: '11:45' },
  { no: 27, route: '명지대역 셔틀', departure: '11:45', via: '12:00' },
  { no: 28, route: '명지대역 셔틀', departure: '11:55', via: '12:10' },
  { no: 29, route: '명지대역 셔틀', departure: '12:05', via: '12:20' },
  { no: 30, route: '명지대역 셔틀', departure: '12:20', via: '12:35' },
  { no: 31, route: '명지대역 셔틀', departure: '12:30', via: '12:45' },
  { no: 32, route: '명지대역 셔틀', departure: '12:45', via: '13:00' },
  { no: 33, route: '명지대역 셔틀', departure: '13:00', via: '13:15' },
  { no: 34, route: '시내 셔틀', departure: '13:10', via: '13:25' },
  { no: 35, route: '명지대역 셔틀', departure: '13:25', via: '13:40' },
  { no: 36, route: '명지대역 셔틀', departure: '13:40', via: '13:55' },
  { no: 37, route: '명지대역 셔틀', departure: '14:00', via: '14:15' },
  { no: 38, route: '명지대역 셔틀', departure: '14:10', via: '14:25' },
  { no: 39, route: '명지대역 셔틀', departure: '14:15', via: '14:30' },
  { no: 40, route: '시내 셔틀', departure: '14:20', via: '14:35' },
  { no: 41, route: '명지대역 셔틀', departure: '14:30', via: '14:45' },
  { no: 42, route: '명지대역 셔틀', departure: '14:50', via: '15:05' },
  { no: 43, route: '명지대역 셔틀', departure: '15:00', via: '15:15' },
  { no: 44, route: '명지대역 셔틀', departure: '15:10', via: '15:25' },
  { no: 45, route: '명지대역 셔틀', departure: '15:25', via: '15:40' },
  { no: 46, route: '명지대역 셔틀', departure: '15:30', via: '15:45' },
  { no: 47, route: '시내 셔틀', departure: '15:40', via: '15:55' },
  { no: 48, route: '명지대역 셔틀', departure: '15:55', via: '16:10' },
  { no: 49, route: '명지대역 셔틀', departure: '16:10', via: '16:25' },
  { no: 50, route: '명지대역 셔틀', departure: '16:25', via: '16:40' },
  { no: 51, route: '명지대역 셔틀', departure: '16:30', via: '16:45' },
  { no: 52, route: '시내 셔틀', departure: '16:35', via: '16:50' },
  { no: 53, route: '명지대역 셔틀', departure: '16:50', via: '17:05' },
  { no: 54, route: '명지대역 셔틀', departure: '17:00', via: '17:15' },
  { no: 55, route: '명지대역 셔틀', departure: '17:10', via: '17:25' },
  { no: 56, route: '명지대역 셔틀', departure: '17:20', via: '17:35' },
  { no: 57, route: '명지대역 셔틀', departure: '17:30', via: '17:45' },
  { no: 58, route: '명지대역 셔틀', departure: '17:45', via: '18:00' },
  { no: 59, route: '명지대역 셔틀', departure: '18:00', via: '18:15' },
  { no: 60, route: '시내 셔틀', departure: '18:10', via: '18:25' },
  { no: 61, route: '명지대역 셔틀', departure: '19:00', via: '19:15' },
  { no: 62, route: '명지대역 셔틀', departure: '19:20', via: '19:35' },
  { no: 63, route: '명지대역 셔틀', departure: '19:30', via: '19:45' },
  { no: 64, route: '시내 셔틀', departure: '20:00', via: '20:15' },
]

/** 계절학기 — 명지대역·시내 */
const SEASONAL_SHUTTLE: MjuTrip[] = [
  { no: 1, route: '명지대역 셔틀', departure: '08:00', via: '08:15' },
  { no: 2, route: '시내 셔틀', departure: '08:05', via: '08:20' },
  { no: 3, route: '명지대역 셔틀', departure: '08:15', via: '08:30' },
  { no: 4, route: '명지대역 셔틀', departure: '08:20', via: '08:35' },
  { no: 5, route: '명지대역 셔틀', departure: '08:25', via: '08:40' },
  { no: 6, route: '명지대역 셔틀', departure: '08:35', via: '08:50' },
  { no: 7, route: '명지대역 셔틀', departure: '08:45', via: '09:00' },
  { no: 8, route: '명지대역 셔틀', departure: '08:50', via: '09:05' },
  { no: 9, route: '시내 셔틀', departure: '08:55', via: '09:10' },
  { no: 10, route: '명지대역 셔틀', departure: '09:00', via: '09:15' },
  { no: 11, route: '명지대역 셔틀', departure: '09:15', via: '09:30' },
  { no: 12, route: '명지대역 셔틀', departure: '09:25', via: '09:40' },
  { no: 13, route: '명지대역 셔틀', departure: '09:35', via: '09:50' },
  { no: 14, route: '명지대역 셔틀', departure: '09:40', via: '09:55' },
  { no: 15, route: '명지대역 셔틀', departure: '09:55', via: '10:10' },
  { no: 16, route: '명지대역 셔틀', departure: '10:00', via: '10:15' },
  { no: 17, route: '시내 셔틀', departure: '10:10', via: '10:25' },
  { no: 18, route: '명지대역 셔틀', departure: '10:20', via: '10:35' },
  { no: 19, route: '명지대역 셔틀', departure: '10:40', via: '10:55' },
  { no: 20, route: '명지대역 셔틀', departure: '10:45', via: '11:00' },
  { no: 21, route: '명지대역 셔틀', departure: '11:00', via: '11:15' },
  { no: 22, route: '시내 셔틀', departure: '11:20', via: '11:35' },
  { no: 23, route: '명지대역 셔틀', departure: '11:25', via: '11:40' },
  { no: 24, route: '명지대역 셔틀', departure: '11:45', via: '12:00' },
  { no: 25, route: '명지대역 셔틀', departure: '11:55', via: '12:10' },
  { no: 26, route: '명지대역 셔틀', departure: '12:05', via: '12:20' },
  { no: 27, route: '명지대역 셔틀', departure: '12:20', via: '12:35' },
  { no: 28, route: '명지대역 셔틀', departure: '12:45', via: '13:00' },
  { no: 29, route: '명지대역 셔틀', departure: '13:00', via: '13:15' },
  { no: 30, route: '시내 셔틀', departure: '13:10', via: '13:25' },
  { no: 31, route: '명지대역 셔틀', departure: '13:40', via: '13:55' },
  { no: 32, route: '명지대역 셔틀', departure: '14:00', via: '14:15' },
  { no: 33, route: '명지대역 셔틀', departure: '14:10', via: '14:25' },
  { no: 34, route: '명지대역 셔틀', departure: '14:15', via: '14:30' },
  { no: 35, route: '시내 셔틀', departure: '14:20', via: '14:35' },
  { no: 36, route: '명지대역 셔틀', departure: '14:50', via: '15:05' },
  { no: 37, route: '명지대역 셔틀', departure: '15:00', via: '15:15' },
  { no: 38, route: '명지대역 셔틀', departure: '15:10', via: '15:25' },
  { no: 39, route: '명지대역 셔틀', departure: '15:25', via: '15:40' },
  { no: 40, route: '시내 셔틀', departure: '15:40', via: '15:55' },
  { no: 41, route: '명지대역 셔틀', departure: '15:55', via: '16:10' },
  { no: 42, route: '명지대역 셔틀', departure: '16:10', via: '16:25' },
  { no: 43, route: '명지대역 셔틀', departure: '16:25', via: '16:40' },
  { no: 44, route: '시내 셔틀', departure: '16:35', via: '16:50' },
  { no: 45, route: '명지대역 셔틀', departure: '16:50', via: '17:05' },
  { no: 46, route: '명지대역 셔틀', departure: '17:00', via: '17:15' },
  { no: 47, route: '명지대역 셔틀', departure: '17:10', via: '17:25' },
  { no: 48, route: '명지대역 셔틀', departure: '17:20', via: '17:35' },
  { no: 49, route: '명지대역 셔틀', departure: '17:30', via: '17:45' },
  { no: 50, route: '명지대역 셔틀', departure: '17:45', via: '18:00' },
  { no: 51, route: '명지대역 셔틀', departure: '18:00', via: '18:15' },
  { no: 52, route: '시내 셔틀', departure: '18:10', via: '18:25' },
  { no: 53, route: '시내 셔틀', departure: '19:00', via: '19:15' },
  { no: 54, route: '시내 셔틀', departure: '20:00', via: '20:15' },
]

/** 학기중 주말·공휴일·방학 — 시내만 */
const WEEKEND_VACATION_CITY: MjuTrip[] = [
  { no: 1, route: '시내 셔틀', departure: '08:20', arrival: '08:45' },
  { no: 2, route: '시내 셔틀', departure: '09:20', arrival: '09:45' },
  { no: 3, route: '시내 셔틀', departure: '10:20', arrival: '10:45' },
  { no: 4, route: '시내 셔틀', departure: '11:20', arrival: '11:45' },
  { no: 5, route: '시내 셔틀', departure: '12:20', arrival: '12:45' },
  { no: 6, route: '시내 셔틀', departure: '13:20', arrival: '13:45' },
  { no: 7, route: '시내 셔틀', departure: '15:20', arrival: '15:45' },
  { no: 8, route: '시내 셔틀', departure: '16:20', arrival: '16:45' },
  { no: 9, route: '시내 셔틀', departure: '17:20', arrival: '17:45' },
  { no: 10, route: '시내 셔틀', departure: '18:00', arrival: '18:25' },
]

/** 기흥역 통학버스 — 학기중 평일만 (출발=학교 출발, 없으면 기흥역 시각) */
const GIHEUNG_SEMESTER: MjuTrip[] = [
  { no: 1, route: '기흥역 통학버스', departure: '08:15', via: '08:15', arrival: '08:30', buses: 2 },
  { no: 2, route: '기흥역 통학버스', departure: '08:25', via: '08:25', arrival: '08:35', buses: 3 },
  { no: 3, route: '기흥역 통학버스', departure: '09:05', via: '09:15', arrival: '09:30', buses: 3 },
  { no: 4, route: '기흥역 통학버스', departure: '09:10', via: '09:20', arrival: '09:35', buses: 2 },
  { no: 5, route: '기흥역 통학버스', departure: '10:00', via: '10:15', arrival: '10:30', buses: 2 },
  { no: 6, route: '기흥역 통학버스', departure: '10:05', via: '10:20', arrival: '10:35', buses: 3 },
  { no: 7, route: '기흥역 통학버스', departure: '12:00', via: '12:15', arrival: '12:30', buses: 1 },
  { no: 8, route: '기흥역 통학버스', departure: '13:00', via: '13:15', arrival: '13:30', buses: 1 },
  { no: 9, route: '기흥역 통학버스', departure: '14:00', via: '14:15', arrival: '14:30', buses: 1 },
  { no: 10, route: '기흥역 통학버스', departure: '15:15', via: '15:30', arrival: '15:45', buses: 2 },
  { no: 11, route: '기흥역 통학버스', departure: '16:15', via: '16:30', arrival: '16:45', buses: 3 },
  { no: 12, route: '기흥역 통학버스', departure: '17:15', via: '17:30', arrival: '17:45', buses: 4 },
  { no: 13, route: '기흥역 통학버스', departure: '18:15', via: '18:30', arrival: '18:45', buses: 2 },
  { no: 14, route: '기흥역 통학버스', departure: '19:15', via: '19:30', buses: 1 },
]

export const MJU_ROUTES = [
  {
    name: '기흥역 통학버스' as const,
    direction: '기흥역 ↔ 캠퍼스',
    description:
      '학기 중 평일만 운행. 주말·공휴일·계절학기·방학 미운행. 기점·종점: 캠퍼스 ↔ 기흥역 5번 출구.',
    start_location: '캠퍼스',
    end_location: '기흥역 5번 출구',
    buses: '5대',
    days: '학기중 평일',
    hours: '08:15 ~ 19:30',
    stopCount: 2,
  },
  {
    name: '명지대역 셔틀' as const,
    direction: '명지대역',
    description:
      '버스관리사무소 → 이마트·상공회의소 → 역북동행정복지센터 건너편 → 명지대역 → 역북동행정복지센터 앞 → 이마트 건너편 → 명진당 → 제3공학관. 18시 이후 명진당까지.',
    start_location: '버스관리사무소',
    end_location: '제3공학관',
    buses: '4대(+추가 1대)',
    days: '학기중 평일 · 계절학기',
    hours: '08:00 ~ 20:00',
    stopCount: 8,
  },
  {
    name: '시내 셔틀' as const,
    direction: '시내',
    description:
      '버스관리사무소 → 이마트·상공회의소 → 역북동 → 동부경찰서 → 용인 CGV → 중앙공영주차장 → … → 제3공학관. 주말·공휴일·방학은 생활관(명현관) 기점 순환.',
    start_location: '버스관리사무소',
    end_location: '제3공학관',
    buses: '1대',
    days: '학기중 매일 · 방학',
    hours: '08:00 ~ 20:15',
    stopCount: 11,
  },
]

export const MJU_TIMETABLE_PACKS: MjuTimetablePack[] = [
  {
    id: 'semester-giheung',
    title: '기흥역 통학버스 (학기 중 평일)',
    period: 'SEMESTER',
    daysLabel: '평일',
    weekdays: WEEKDAYS,
    semester: 'SEMESTER',
    trips: GIHEUNG_SEMESTER,
    note: '주말·공휴일·계절학기·방학 미운행 · 총 30회(5대×6회)',
  },
  {
    id: 'semester-shuttle',
    title: '명지대역·시내 셔틀 (학기 중 평일)',
    period: 'SEMESTER',
    daysLabel: '평일',
    weekdays: WEEKDAYS,
    semester: 'SEMESTER',
    trips: SEMESTER_SHUTTLE,
    note: '역북동 행정복지센터 경유 시각 포함',
  },
  {
    id: 'semester-shuttle-pm-ref',
    title: '명지대역·시내 셔틀 오후표 (학기 중)',
    period: 'SEMESTER',
    daysLabel: '평일',
    weekdays: WEEKDAYS,
    semester: 'SEMESTER',
    trips: SEMESTER_SHUTTLE.filter((t) => t.no >= 49),
  },
  {
    id: 'semester-route-info',
    title: '학기 중 노선 안내',
    period: 'SEMESTER',
    daysLabel: '평일',
    weekdays: WEEKDAYS,
    semester: 'SEMESTER',
    trips: [],
  },
  {
    id: 'seasonal-shuttle',
    title: '명지대역·시내 셔틀 (계절학기)',
    period: 'VACATION',
    daysLabel: '계절학기',
    weekdays: WEEKDAYS,
    semester: 'VACATION',
    trips: SEASONAL_SHUTTLE,
  },
  {
    id: 'seasonal-shuttle-pm',
    title: '명지대역·시내 셔틀 오후표 (계절학기)',
    period: 'VACATION',
    daysLabel: '계절학기',
    weekdays: WEEKDAYS,
    semester: 'VACATION',
    trips: SEASONAL_SHUTTLE.filter((t) => t.no >= 23),
  },
  {
    id: 'weekend-vacation-city',
    title: '시내 셔틀 (학기중 주말·공휴일·방학)',
    period: 'VACATION',
    daysLabel: '주말·공휴일·방학',
    weekdays: WEEKEND,
    semester: 'VACATION',
    trips: WEEKEND_VACATION_CITY,
    note: '생활관(명현관) 기점 순환 10회',
  },
]

/** schedules 테이블 insert용 — 노선별 출발시각 × 요일 × semester */
/** schedules insert용 — 18시 이후·주말/방학 시내를 변형 노선으로 분리 */
export function expandScheduleRows(): {
  routeName: ScheduleRouteName
  departure_time: string
  weekday: Weekday
  semester: SemesterType
}[] {
  const out: {
    routeName: ScheduleRouteName
    departure_time: string
    weekday: Weekday
    semester: SemesterType
  }[] = []

  const push = (trips: MjuTrip[], weekdays: Weekday[], semester: SemesterType, routeFilter?: MjuRouteName) => {
    for (const t of trips) {
      if (routeFilter && t.route !== routeFilter) continue
      for (const wd of weekdays) {
        const departure_time = `${t.departure}:00`
        out.push({
          routeName: resolveOperationalRouteName({
            baseRouteName: t.route,
            departureTime: departure_time,
            weekday: wd,
            semester,
          }),
          departure_time,
          weekday: wd,
          semester,
        })
      }
    }
  }

  push(GIHEUNG_SEMESTER, WEEKDAYS, 'SEMESTER', '기흥역 통학버스')
  push(SEMESTER_SHUTTLE, WEEKDAYS, 'SEMESTER')
  push(SEASONAL_SHUTTLE, WEEKDAYS, 'VACATION')
  // 학기중 주말도 시내 운행 → 시내 셔틀 (주말·공휴일·방학)
  push(WEEKEND_VACATION_CITY, WEEKEND, 'SEMESTER', '시내 셔틀')
  push(WEEKEND_VACATION_CITY, WEEKEND, 'VACATION', '시내 셔틀')
  // 방학 평일 시내 → 시내 셔틀 (주말·공휴일·방학)
  push(WEEKEND_VACATION_CITY, WEEKDAYS, 'VACATION', '시내 셔틀')

  return out
}

export function summarizeRouteSchedule(route: MjuRouteName, semester: SemesterType = 'SEMESTER') {
  const trips =
    route === '기흥역 통학버스'
      ? GIHEUNG_SEMESTER
      : semester === 'VACATION'
        ? SEASONAL_SHUTTLE.filter((t) => t.route === route).concat(
            route === '시내 셔틀' ? WEEKEND_VACATION_CITY : [],
          )
        : SEMESTER_SHUTTLE.filter((t) => t.route === route)

  const times = trips.map((t) => t.departure).sort()
  return {
    rounds: trips.length,
    start: times[0] ?? '-',
    end: times[times.length - 1] ?? '-',
  }
}
