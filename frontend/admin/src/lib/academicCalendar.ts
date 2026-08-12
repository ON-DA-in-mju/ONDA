import type { SemesterType } from '../types/database'

/**
 * 학사 일정 (합의)
 * - 1학기: 3/1 개강 → 15주 정규 학기 → 이어서 4주 계절학기
 * - 2학기: 9/1 개강 → 15주 정규 학기 → 이어서 4주 계절학기
 * - 그 외: 방학
 *
 * 노선 규칙(설명 기준):
 * - 기흥역 통학버스: 정규 학기 평일만 (계절학기·방학 제외)
 * - 명지대역·시내(일반): 정규 학기 + 계절학기 평일
 * - 시내 (주말·공휴일·방학): 주말·공휴일·방학
 */
export type AcademicTerm = 'SEMESTER' | 'SEASONAL' | 'VACATION'

export const SEMESTER_WEEKS = 15
export const SEASONAL_WEEKS = 4
export const SPRING_SEMESTER_START_MD = { month: 3, day: 1 } as const
export const FALL_SEMESTER_START_MD = { month: 9, day: 1 } as const

/** @deprecated 구간 표시용 — termForDate / semesterForDate 사용 */
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

function addDaysKey(year: number, month: number, day: number, addDays: number): string {
  const d = new Date(year, month - 1, day)
  d.setDate(d.getDate() + addDays)
  return toDateKey(d)
}

/** start(포함) + weeks주 − 1일 = 구간 종료일 */
export function rangeEndDateKey(
  year: number,
  startMd: { month: number; day: number },
  weeks: number,
): string {
  return addDaysKey(year, startMd.month, startMd.day, weeks * 7 - 1)
}

/** @deprecated use rangeEndDateKey(..., SEMESTER_WEEKS) */
export function semesterEndDateKey(year: number, startMd: { month: number; day: number }): string {
  return rangeEndDateKey(year, startMd, SEMESTER_WEEKS)
}

function inInclusiveRange(key: string, startKey: string, endKey: string): boolean {
  return key >= startKey && key <= endKey
}

/** YYYY-MM-DD → 정규학기 | 계절학기 | 방학 */
export function termForDate(input: string | Date): AcademicTerm {
  const key = typeof input === 'string' ? input.slice(0, 10) : toDateKey(input)
  const { y } = parseDateKey(key)

  for (const startMd of [SPRING_SEMESTER_START_MD, FALL_SEMESTER_START_MD]) {
    const semStart = dateKeyFromYmd(y, startMd.month, startMd.day)
    const semEnd = rangeEndDateKey(y, startMd, SEMESTER_WEEKS)
    if (inInclusiveRange(key, semStart, semEnd)) return 'SEMESTER'

    const seasonalStart = addDaysKey(y, startMd.month, startMd.day, SEMESTER_WEEKS * 7)
    const seasonalEnd = addDaysKey(y, startMd.month, startMd.day, (SEMESTER_WEEKS + SEASONAL_WEEKS) * 7 - 1)
    // 가을 계절학기는 연말 넘어갈 수 있음 → 다음 해 키 비교
    if (seasonalStart <= seasonalEnd) {
      if (inInclusiveRange(key, seasonalStart, seasonalEnd)) return 'SEASONAL'
    } else {
      // should not happen with addDaysKey producing absolute keys
      if (key >= seasonalStart || key <= seasonalEnd) return 'SEASONAL'
    }
  }

  // 전년도 가을 계절학기 (12월~1월) — y년 1월 초
  const prev = y - 1
  const fallSeasonalStart = addDaysKey(prev, FALL_SEMESTER_START_MD.month, FALL_SEMESTER_START_MD.day, SEMESTER_WEEKS * 7)
  const fallSeasonalEnd = addDaysKey(
    prev,
    FALL_SEMESTER_START_MD.month,
    FALL_SEMESTER_START_MD.day,
    (SEMESTER_WEEKS + SEASONAL_WEEKS) * 7 - 1,
  )
  if (inInclusiveRange(key, fallSeasonalStart, fallSeasonalEnd)) return 'SEASONAL'

  return 'VACATION'
}

/**
 * schedules.semester 매핑용.
 * 정규·계절학기 → SEMESTER 시간표, 방학 → VACATION 시간표.
 */
export function semesterForDate(input: string | Date): SemesterType {
  return termForDate(input) === 'VACATION' ? 'VACATION' : 'SEMESTER'
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
  '2026-02-16',
  '2026-02-17',
  '2026-02-18',
  '2026-05-24',
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
  return termForDate(input) === 'VACATION' || isWeekendDate(input) || isKoreanPublicHoliday(input)
}
