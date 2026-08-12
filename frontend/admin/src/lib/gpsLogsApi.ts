import { isSupabaseConfigured, supabase } from './supabase'
import type { Database } from '../types/database'

export const GPS_LOGS_MAX = 100

export type GpsReceiveLog = {
  id: string
  operationId: string
  vehicleName: string
  eventType: string
  message: string
  createdAt: string | null
  createdAtLabel: string
  lat: number | null
  lng: number | null
  accuracy: string | null
}

type OperationLogRow = Database['public']['Tables']['operation_logs']['Row']
type OperationLogWithOps = OperationLogRow & {
  operations?: {
    buses?: {
      bus_name: string | null
      vehicle_number: string | null
    } | null
  } | null
}

const GPS_EVENT = 'LOCATION_UPDATED'

function formatKoTime(iso: string | null): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

function parseGpsMessage(message: string): { lat: number | null; lng: number | null; accuracy: string | null } {
  const lat = message.match(/lat=(-?\d+(?:\.\d+)?)/i)?.[1]
  const lng = message.match(/lng=(-?\d+(?:\.\d+)?)/i)?.[1]
  const accuracy = message.match(/accuracy=([^\s]+)/i)?.[1] ?? null
  return {
    lat: lat != null ? Number(lat) : null,
    lng: lng != null ? Number(lng) : null,
    accuracy,
  }
}

function mapRow(row: OperationLogRow): GpsReceiveLog {
  const parsed = parseGpsMessage(row.log_message)
  const ops = (row as unknown as OperationLogWithOps).operations
  const bus = ops?.buses
  const busName = (bus?.bus_name ?? '').trim()
  const vehicleNumber = (bus?.vehicle_number ?? '').trim()
  const vehicleDisplay = vehicleNumber || busName || '미정'
  return {
    id: row.id,
    operationId: row.operation_id,
    vehicleName: vehicleDisplay,
    eventType: row.event_type,
    message: row.log_message,
    createdAt: row.created_at,
    createdAtLabel: formatKoTime(row.created_at),
    lat: parsed.lat,
    lng: parsed.lng,
    accuracy: parsed.accuracy,
  }
}

/** 최신 [GPS_LOGS_MAX]건만 남기고 오래된 operation_logs 삭제 */
export async function enforceOperationLogsCap(max = GPS_LOGS_MAX): Promise<number> {
  if (!isSupabaseConfigured) return 0
  const { data: excess, error } = await supabase
    .from('operation_logs')
    .select('id')
    .order('created_at', { ascending: false, nullsFirst: false })
    .order('id', { ascending: false })
    .range(max, max + 499)

  if (error) {
    console.warn('[gpsLogs] trim list', error.message)
    return 0
  }
  const ids = (excess ?? []).map((r) => r.id).filter(Boolean)
  if (!ids.length) return 0

  const { error: delErr } = await supabase.from('operation_logs').delete().in('id', ids)
  if (delErr) {
    console.warn('[gpsLogs] trim delete', delErr.message)
    return 0
  }
  return ids.length
}

/** GPS(LOCATION_UPDATED) 수신 로그 — 최신순, 최대 100건 */
export async function fetchGpsReceiveLogs(): Promise<GpsReceiveLog[] | null> {
  if (!isSupabaseConfigured) return null

  await enforceOperationLogsCap(GPS_LOGS_MAX)

  const { data, error } = await supabase
    .from('operation_logs')
    .select(
      'id, operation_id, event_type, log_message, created_at, operations:operation_id ( buses:bus_id ( bus_name, vehicle_number ) )',
    )
    .eq('event_type', GPS_EVENT)
    .order('created_at', { ascending: false, nullsFirst: false })
    .limit(GPS_LOGS_MAX)

  if (error) {
    console.warn('[gpsLogs] fetch', error.message)
    return null
  }
  return ((data ?? []) as OperationLogRow[]).map(mapRow)
}
