/** 일요일 시작 주간 (일~토) — 요일 칩과 맞춤 */

const pad = (n: number) => String(n).padStart(2, '0')

export function toDateKey(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function formatDotDate(d: Date): string {
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

export function parseDateKey(key: string): Date {
  const [y, m, d] = key.split('-').map(Number)
  return new Date(y, (m || 1) - 1, d || 1)
}

export function startOfWeek(d: Date): Date {
  const start = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  start.setDate(start.getDate() - start.getDay()) // Sunday
  return start
}

export function addDays(d: Date, days: number): Date {
  const next = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  next.setDate(next.getDate() + days)
  return next
}

export function weekRangeFromDate(d: Date): { start: Date; end: Date } {
  const start = startOfWeek(d)
  return { start, end: addDays(start, 6) }
}

export function formatWeekRange(start: Date, end: Date): string {
  return `${formatDotDate(start)} ~ ${formatDotDate(end)}`
}

export const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'] as const
