import { isSupabaseConfigured } from './supabase'
import { fetchSafeStopRequests, type SafeStopRequest } from './safeStopApi'

export type AdminNotification = {
  id: string
  type: 'safe_stop'
  title: string
  body: string
  href: string
  relatedId: string
  createdAt: number
  read: boolean
}

export type AdminNotificationsSnapshot = {
  items: AdminNotification[]
  unreadCount: number
}

const SEEN_KEY = 'onda-admin-safe-stop-seen'
export const ADMIN_NOTIFICATIONS_CHANGED = 'onda-admin-notifications-changed'

export function emitAdminNotificationsChanged() {
  window.dispatchEvent(new Event(ADMIN_NOTIFICATIONS_CHANGED))
}

function loadSeenIds(): Set<string> {
  try {
    const raw = localStorage.getItem(SEEN_KEY)
    if (!raw) return new Set()
    const arr = JSON.parse(raw) as string[]
    return new Set(Array.isArray(arr) ? arr : [])
  } catch {
    return new Set()
  }
}

function saveSeenIds(ids: Set<string>) {
  try {
    localStorage.setItem(SEEN_KEY, JSON.stringify([...ids]))
  } catch {
    /* ignore */
  }
}

function toNotification(row: SafeStopRequest, seen: Set<string>): AdminNotification {
  const pending = row.decision === 'pending'
  return {
    id: `safe-stop-${row.id}`,
    type: 'safe_stop',
    title: pending ? '안전 정차 요청' : '안전 정차 처리 완료',
    body: `${row.driverName || row.driverId} · ${row.vehicleName} · ${row.reason}`,
    href: `/live/suspend?id=${encodeURIComponent(row.id)}`,
    relatedId: row.id,
    createdAt: row.createdAt || Date.now(),
    read: !pending || seen.has(row.id),
  }
}

async function fetchFromDevApi(): Promise<AdminNotificationsSnapshot> {
  try {
    const res = await fetch('/api/admin-notifications', { cache: 'no-store' })
    if (!res.ok) return { items: [], unreadCount: 0 }
    const data = (await res.json()) as AdminNotificationsSnapshot
    return {
      items: Array.isArray(data.items) ? data.items : [],
      unreadCount: typeof data.unreadCount === 'number' ? data.unreadCount : 0,
    }
  } catch {
    return { items: [], unreadCount: 0 }
  }
}

/**
 * 관리자 알림 = safe_stop_requests 기반.
 * - pending → 미확인 알림(배너/뱃지)
 * - 최근 처리 건도 알림 목록에 표시
 */
export async function fetchAdminNotifications(opts?: {
  /** 레이아웃 폴링용: pending만 (가볍고 빠름) */
  pendingOnly?: boolean
}): Promise<AdminNotificationsSnapshot> {
  if (!isSupabaseConfigured) return fetchFromDevApi()

  const rows = await fetchSafeStopRequests({
    pendingOnly: opts?.pendingOnly,
    limit: opts?.pendingOnly ? 20 : 50,
  })
  const seen = loadSeenIds()
  const items = rows
    .map((r) => toNotification(r, seen))
    .sort((a, b) => b.createdAt - a.createdAt)
    .slice(0, 50)

  return {
    items,
    unreadCount: items.filter((n) => !n.read).length,
  }
}

export async function markNotificationRead(id: string): Promise<boolean> {
  if (!isSupabaseConfigured) {
    try {
      const res = await fetch(`/api/admin-notifications/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ read: true }),
      })
      if (res.ok) emitAdminNotificationsChanged()
      return res.ok
    } catch {
      return false
    }
  }

  const relatedId = id.startsWith('safe-stop-') ? id.slice('safe-stop-'.length) : id
  const seen = loadSeenIds()
  seen.add(relatedId)
  saveSeenIds(seen)
  emitAdminNotificationsChanged()
  return true
}
