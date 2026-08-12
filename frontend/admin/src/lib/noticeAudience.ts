import type { NoticeAudience } from '../types/database'

/** PostgREST text[] / 문자열 / 한글 라벨을 STUDENT|DRIVER 배열로 정규화 */
export function normalizeNoticeAudience(raw: unknown): NoticeAudience[] {
  const tokens: string[] = []
  if (Array.isArray(raw)) {
    for (const item of raw) tokens.push(String(item))
  } else if (typeof raw === 'string') {
    tokens.push(...raw.replace(/[{}]/g, '').split(/[,|/]+/))
  }

  const out = new Set<NoticeAudience>()
  for (const token of tokens) {
    const value = token.trim().toUpperCase()
    if (value === 'STUDENT' || token.trim() === '학생') out.add('STUDENT')
    if (value === 'DRIVER' || token.trim() === '기사') out.add('DRIVER')
  }
  return [...out]
}

export function noticeAudienceLabel(raw: unknown): string {
  const set = new Set(normalizeNoticeAudience(raw))
  const student = set.has('STUDENT')
  const driver = set.has('DRIVER')
  if (student && driver) return '학생/기사'
  if (student) return '학생'
  if (driver) return '기사'
  return '-'
}

export function isMissingAudienceColumnError(message: string | undefined): boolean {
  if (!message) return false
  return /audience/i.test(message) && /does not exist|schema cache|42703/i.test(message)
}

export const MISSING_AUDIENCE_COLUMN_MESSAGE =
  'DB에 공지 대상(audience) 컬럼이 없습니다. Supabase SQL Editor에서 migrate_notices_audience.sql 을 실행한 뒤 다시 저장하세요.'
