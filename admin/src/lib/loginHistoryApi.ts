export type LoginHistoryEntry = {
  userId: string
  name: string
  time: string
  ip: string
  source: string
}

export async function fetchLoginHistory(): Promise<LoginHistoryEntry[]> {
  try {
    const res = await fetch('/api/login-history', { cache: 'no-store' })
    if (!res.ok) return []
    const data = (await res.json()) as LoginHistoryEntry[]
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

/** `2026.07.20 09:32:15` → 테이블용 `2026.07.20 09:32` */
export function toLastLoginDisplay(time: string): string {
  return time.length >= 16 ? time.slice(0, 16) : time
}
