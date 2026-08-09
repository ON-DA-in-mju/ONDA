export type LiveGpsKind = 'ok' | 'none' | 'error'

export type LiveVehicle = {
  id: string
  driverId: string
  driverName: string
  vehicleName: string
  routeName: string
  operationId: string
  status: 'in_progress' | 'ended' | 'idle' | 'stopped'
  statusLabel: string
  tone: 'green' | 'orange' | 'blue' | 'gray' | 'red'
  lat: number | null
  lng: number | null
  accuracy: number | null
  stop: string
  gps: string
  gpsKind: LiveGpsKind
  updatedAt: number
  last: string
}

export type LiveStats = {
  ok: number
  none: number
  error: number
  total: number
  rate: number
  inProgress: number
  ended: number
  idle: number
  stopped?: number
}

export type LiveSnapshot = {
  vehicles: LiveVehicle[]
  stats: LiveStats
}

const emptyStats: LiveStats = {
  ok: 0,
  none: 0,
  error: 0,
  total: 0,
  rate: 0,
  inProgress: 0,
  ended: 0,
  idle: 0,
}

export async function fetchLiveVehicles(): Promise<LiveSnapshot> {
  try {
    const res = await fetch('/api/live/vehicles', { cache: 'no-store' })
    if (!res.ok) return { vehicles: [], stats: emptyStats }
    const data = (await res.json()) as LiveSnapshot
    return {
      vehicles: Array.isArray(data.vehicles) ? data.vehicles : [],
      stats: data.stats ?? emptyStats,
    }
  } catch {
    return { vehicles: [], stats: emptyStats }
  }
}
