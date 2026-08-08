export type SafeStopDecision = 'pending' | 'continue' | 'stop' | 'cancelled'

export type SafeStopRequest = {
  id: string
  driverId: string
  driverName: string
  vehicleName: string
  routeName: string
  operationId: string
  reason: string
  detailReason: string
  requestedAt: string
  date: string
  decision: SafeStopDecision
  createdAt: number
  decidedAt?: number
}

export async function fetchSafeStopRequests(params?: {
  driverId?: string
  pendingOnly?: boolean
}): Promise<SafeStopRequest[]> {
  try {
    const qs = new URLSearchParams()
    if (params?.driverId) qs.set('driverId', params.driverId)
    if (params?.pendingOnly) qs.set('pending', '1')
    const res = await fetch(`/api/safe-stop?${qs}`, { cache: 'no-store' })
    if (!res.ok) return []
    const data = (await res.json()) as SafeStopRequest[]
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

export async function decideSafeStop(
  id: string,
  decision: 'continue' | 'stop',
): Promise<{ ok: boolean; message?: string }> {
  try {
    const res = await fetch(`/api/safe-stop/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ decision }),
    })
    const data = (await res.json()) as { ok?: boolean; message?: string }
    if (!res.ok) return { ok: false, message: data.message || 'failed' }
    return { ok: true }
  } catch {
    return { ok: false, message: 'network error' }
  }
}
