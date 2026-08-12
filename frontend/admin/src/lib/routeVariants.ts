import type { SemesterType, Weekday } from '../types/database'
import {
  CITY_SHUTTLE_ROUTE_NAME,
  CITY_SHUTTLE_VACATION_ROUTE_NAME,
  GIHEUNG_ROUTE_NAME,
  MYONGJI_STATION_AFTER18_ROUTE_NAME,
  MYONGJI_STATION_ROUTE_NAME,
} from '../data/cityShuttleStops'
import { isCityVacationServiceDay, isKoreanPublicHoliday, semesterForDate, termForDate } from './academicCalendar'


export const OPERATIONAL_ROUTE_NAMES = [
  GIHEUNG_ROUTE_NAME,
  MYONGJI_STATION_ROUTE_NAME,
  MYONGJI_STATION_AFTER18_ROUTE_NAME,
  CITY_SHUTTLE_ROUTE_NAME,
  CITY_SHUTTLE_VACATION_ROUTE_NAME,
] as const

export type OperationalRouteName = (typeof OPERATIONAL_ROUTE_NAMES)[number]

const AFTER18_MINUTES = 18 * 60

export function departureToMinutes(departureTime: string): number {
  const [hh, mm] = departureTime.slice(0, 5).split(':').map(Number)
  return (hh || 0) * 60 + (mm || 0)
}

export function isAfter18Departure(departureTime: string): boolean {
  return departureToMinutes(departureTime) >= AFTER18_MINUTES
}

/**
 * 시간표/배차용 실제 노선명.
 * - 명지대역 셔틀 + 18:00 이후 → 명지대역 셔틀 (18시 이후)
 * - 시내 셔틀 + (주말·공휴일·방학) → 시내 셔틀 (주말·공휴일·방학)
 */
export function resolveOperationalRouteName(input: {
  baseRouteName: string
  departureTime: string
  /** schedules 시드: weekday + semester 로 판별 */
  weekday?: Weekday
  semester?: SemesterType
  /** operations 시드: 구체 날짜로 공휴일·학기 판별 */
  date?: string
}): string {
  const base = input.baseRouteName.trim()

  if (base === MYONGJI_STATION_ROUTE_NAME || base === MYONGJI_STATION_AFTER18_ROUTE_NAME) {
    return isAfter18Departure(input.departureTime)
      ? MYONGJI_STATION_AFTER18_ROUTE_NAME
      : MYONGJI_STATION_ROUTE_NAME
  }

  if (base === CITY_SHUTTLE_ROUTE_NAME || base === CITY_SHUTTLE_VACATION_ROUTE_NAME) {
    if (input.date) {
      return isCityVacationServiceDay(input.date) ? CITY_SHUTTLE_VACATION_ROUTE_NAME : CITY_SHUTTLE_ROUTE_NAME
    }
    const weekend = input.weekday === 'SAT' || input.weekday === 'SUN'
    const vacation = input.semester === 'VACATION'
    return weekend || vacation ? CITY_SHUTTLE_VACATION_ROUTE_NAME : CITY_SHUTTLE_ROUTE_NAME
  }

  return base
}

/** 공휴일(평일)에는 기흥·명지대 학기 평일 운행 제외, 시내 방학형만 */
export function shouldSkipBaseScheduleOnHoliday(opts: {
  date: string
  routeName: string
}): boolean {
  if (!isKoreanPublicHoliday(opts.date)) return false
  if (termForDate(opts.date) === 'VACATION') return false
  const name = opts.routeName
  if (name === CITY_SHUTTLE_VACATION_ROUTE_NAME) return false
  if (name === CITY_SHUTTLE_ROUTE_NAME) return true
  if (name === GIHEUNG_ROUTE_NAME) return true
  if (name === MYONGJI_STATION_ROUTE_NAME || name === MYONGJI_STATION_AFTER18_ROUTE_NAME) return true
  return false
}

/**
 * 학사 기간별 노선 운행 여부
 * - SEMESTER: 전 노선(해당 요일 스케줄)
 * - SEASONAL: 기흥역 제외 (계절학기·방학 제외 노선)
 * - VACATION: 시내 (주말·공휴일·방학)만
 */
export function routeRunsOnTerm(routeName: string, term: ReturnType<typeof termForDate>): boolean {
  if (term === 'VACATION') return routeName === CITY_SHUTTLE_VACATION_ROUTE_NAME
  if (term === 'SEASONAL') return routeName !== GIHEUNG_ROUTE_NAME
  return true
}

export const VARIANT_ROUTE_DEFS = [
  {
    name: MYONGJI_STATION_AFTER18_ROUTE_NAME,
    direction: '명지대역 (18시 이후)',
    description:
      '18시 이후 명지대역 셔틀. 명진당까지 운행. 기본 명지대역 셔틀과 정류장·경로는 동일 계열.',
    start_location: '버스관리사무소',
    end_location: '명진당',
  },
  {
    name: CITY_SHUTTLE_VACATION_ROUTE_NAME,
    direction: '시내 (주말·공휴일·방학)',
    description: '주말·공휴일·방학 시내 셔틀. 생활관(명현관) 기점 순환.',
    start_location: '생활관(명현관)',
    end_location: '생활관(명현관)',
  },
] as const
