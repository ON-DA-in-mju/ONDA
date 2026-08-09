import type { AssignmentStatus, TodayAssignment } from '../types/assignment'

/** 출발 N분 전부터 「곧 출발」 */
export const DEPARTING_SOON_MINUTES = 10

function parseHmToMinutes(value: string): number | null {
  const parts = value.split(':')
  if (parts.length < 2) return null
  const h = Number(parts[0])
  const m = Number(parts[1])
  if (!Number.isFinite(h) || !Number.isFinite(m)) return null
  return h * 60 + m
}

/**
 * 저장 상태와 무관하게, 운행 중/종료가 아니면
 * 출발 시각 기준 10분 전~시작 전까지 「곧 출발」, 그 외 「운행 예정」.
 */
export function resolveAssignmentStatus(
  assignment: Pick<TodayAssignment, 'departTime' | 'status'>,
  now = new Date(),
): AssignmentStatus {
  if (assignment.status === 'in_progress' || assignment.status === 'ended') {
    return assignment.status
  }
  if (assignment.status === 'waiting') {
    return 'waiting'
  }

  const depart = parseHmToMinutes(assignment.departTime)
  if (depart == null) return 'scheduled'

  const nowMinutes = now.getHours() * 60 + now.getMinutes()
  const minutesUntil = depart - nowMinutes
  // 출발 전 0~10분만 「곧 출발」 (이미 지난 시각은 운행 예정 유지)
  return minutesUntil >= 0 && minutesUntil <= DEPARTING_SOON_MINUTES
    ? 'departing_soon'
    : 'scheduled'
}

export function withResolvedStatus<T extends TodayAssignment>(row: T, now = new Date()): T {
  return { ...row, status: resolveAssignmentStatus(row, now) }
}
