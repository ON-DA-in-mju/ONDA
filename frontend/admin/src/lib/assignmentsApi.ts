import type { TodayAssignment } from '../types/assignment'
import { todayDateKey } from '../types/assignment'

export async function fetchAssignments(params?: {
  date?: string
  driverId?: string
}): Promise<TodayAssignment[]> {
  const date = params?.date ?? todayDateKey()
  const qs = new URLSearchParams({ date })
  if (params?.driverId) qs.set('driverId', params.driverId)
  try {
    const res = await fetch(`/api/assignments?${qs}`, { cache: 'no-store' })
    if (!res.ok) return []
    const data = (await res.json()) as TodayAssignment[]
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

export async function createAssignment(
  payload: Omit<TodayAssignment, 'id'> & { id?: string },
): Promise<{ ok: boolean; entry?: TodayAssignment; message?: string }> {
  try {
    const res = await fetch('/api/assignments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    const data = (await res.json()) as { ok?: boolean; entry?: TodayAssignment; message?: string }
    if (!res.ok) return { ok: false, message: data.message || 'create failed' }
    return { ok: true, entry: data.entry }
  } catch {
    return { ok: false, message: 'network error' }
  }
}

export async function updateAssignment(
  id: string,
  patch: Partial<TodayAssignment>,
): Promise<{ ok: boolean; entry?: TodayAssignment; message?: string }> {
  try {
    const res = await fetch(`/api/assignments/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(patch),
    })
    const data = (await res.json()) as { ok?: boolean; entry?: TodayAssignment; message?: string }
    if (!res.ok) return { ok: false, message: data.message || 'update failed' }
    return { ok: true, entry: data.entry }
  } catch {
    return { ok: false, message: 'network error' }
  }
}

export async function deleteAssignment(id: string): Promise<{ ok: boolean; message?: string }> {
  try {
    const res = await fetch(`/api/assignments/${encodeURIComponent(id)}`, { method: 'DELETE' })
    const data = (await res.json()) as { ok?: boolean; message?: string }
    if (!res.ok) return { ok: false, message: data.message || 'delete failed' }
    return { ok: true }
  } catch {
    return { ok: false, message: 'network error' }
  }
}
