import type { SemesterType } from '../types/database'

/**
 * 학기 기준 (프로젝트 합의)
 * - 1학기: 3월 1일부터 15주
 * - 2학기: 9월 1일부터 15주
 * - 그 외: 방학
 */
export const SEMESTER_WEEKS = 15
export const SPRING_SEMESTER_START_MD = { month: 3, day: 1 } as const
export const FALL_SEMESTER_START_MD = { month: 9, day: 1 } as const

/** @deprecated 구간 표시용 — semesterForDate는 15주 규칙 사용 */
export const VACATION_START_MD = { month: 6, day: 14 } as const
/** @deprecated */
export const VACATION_END_MD = { month: 8, day: 31 } as const

function pad2(n: number) {
  return String(n).padStart(2, '0')
}

function toDateKey(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function parseDateKey(key: string): { y: number; m: number; d: number } {
  const [y, m, d] = key.slice(0, 10).split('-').map(Number)
  return { y, m, d }
}

function dateKeyFromYmd(y: number, m: number, d: number): string {
  return `${y}-${pad2(m)}-${pad2(d)}`
}

/** start(포함) + weeks주 − 1일 = 학기 종료일 */
export function semesterEndDateKey(year: number, startMd: { month: number; day: number }): string {
  const start = new Date(year, startMd.month - 1, startMd.day)
  const end = new Date(start)
  end.setDate(end.getDate() + SEMESTER_WEEKS * 7 - 1)
  return toDateKey(end)
}

function inInclusiveRange(key: string, startKey: string, endKey: string): boolean {
  return key >= startKey && key <= endKey
}

/** YYYY-MM-DD 또는 Date → SEMESTER | VACATION */
export function semesterForDate(input: string | Date): SemesterType {
  const key = typeof input === 'string' ? input.slice(0, 10) : toDateKey(input)
  const { y } = parseDateKey(key)

  const springStart = dateKeyFromYmd(y, SPRING_SEMESTER_START_MD.month, SPRING_SEMESTER_START_MD.day)
  const springEnd = semesterEndDateKey(y, SPRING_SEMESTER_START_MD)
  const fallStart = dateKeyFromYmd(y, FALL_SEMESTER_START_MD.month, FALL_SEMESTER_START_MD.day)
  const fallEnd = semesterEndDateKey(y, FALL_SEMESTER_START_MD)

  if (inInclusiveRange(key, springStart, springEnd)) return 'SEMESTER'
  if (inInclusiveRange(key, fallStart, fallEnd)) return 'SEMESTER'
  return 'VACATION'
}

/**
 * 고정 공휴일(양력) + 연도별 주요 이동 공휴일.
 * 시내 셔틀 (주말·공휴일·방학) 판별에 사용.
 */
const FIXED_HOLIDAYS_MD = [
  [1, 1],
  [3, 1],
  [5, 5],
  [6, 6],
  [8, 15],
  [10, 3],
  [10, 9],
  [12, 25],
] as const

/** 설·추석 연휴 등 (YYYY-MM-DD). 필요 시 연도 추가. */
const MOVABLE_HOLIDAYS: ReadonlySet<string> = new Set([
  // 2026 설날 연휴
  '2026-02-16',
  '2026-02-17',
  '2026-02-18',
  // 2026 부처님오신날
  '2026-05-24',
  // 2026 추석 연휴
  '2026-09-24',
  '2026-09-25',
  '2026-09-26',
])

export function isKoreanPublicHoliday(input: string | Date): boolean {
  const key = typeof input === 'string' ? input.slice(0, 10) : toDateKey(input)
  if (MOVABLE_HOLIDAYS.has(key)) return true
  const { m, d } = parseDateKey(key)
  return FIXED_HOLIDAYS_MD.some(([mm, dd]) => mm === m && dd === d)
}

export function isWeekendDate(input: string | Date): boolean {
  const key = typeof input === 'string' ? input.slice(0, 10) : toDateKey(input)
  const { y, m, d } = parseDateKey(key)
  const dow = new Date(y, m - 1, d).getDay()
  return dow === 0 || dow === 6
}

/** 시내 변형 노선(주말·공휴일·방학) 적용 여부 */
export function isCityVacationServiceDay(input: string | Date): boolean {
  return semesterForDate(input) === 'VACATION' || isWeekendDate(input) || isKoreanPublicHoliday(input)
}
