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

export async function fetchAdminNotifications(): Promise<AdminNotificationsSnapshot> {
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

export async function markNotificationRead(id: string): Promise<boolean> {
  try {
    const res = await fetch(`/api/admin-notifications/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ read: true }),
    })
    return res.ok
  } catch {
    return false
  }
}
