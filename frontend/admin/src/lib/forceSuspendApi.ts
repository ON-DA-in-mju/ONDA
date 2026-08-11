import { isSupabaseConfigured, supabase } from './supabase'

export type BusOption = {
  busId: string
  busName: string
  vehicleNumber: string
  label: string
}

export const ALL_BUSES = '__all__'

/** 등록된 전체 차량 목록 (드롭다운용) */
export async function fetchBusOptions(): Promise<BusOption[]> {
  if (!isSupabaseConfigured) return []

  const { data, error } = await supabase
    .from('buses')
    .select('id, bus_name, vehicle_number')
    .order('bus_name', { ascending: true })

  if (error) {
    console.warn('[suspend] buses', error.message)
    return []
  }

  return (data ?? []).map((b) => {
    const busName = (b.bus_name || '').trim() || '미정'
    const vehicleNumber = (b.vehicle_number || '').trim()
    const label = vehicleNumber ? `${busName} (${vehicleNumber})` : busName
    return {
      busId: b.id,
      busName,
      vehicleNumber,
      label,
    }
  })
}

type OpSuspendRow = {
  id: string
  operation_date: string
  bus_id: string
  status: string
  schedules: { departure_time: string | null } | null
}

function parseDepartLocal(operationDate: string, departureTime: string | null): Date | null {
  if (!operationDate || !departureTime) return null
  const hm = departureTime.slice(0, 5)
  // 로컬 시각으로 해석 (datetime-local 입력과 동일)
  const d = new Date(`${operationDate}T${hm}:00`)
  return Number.isNaN(d.getTime()) ? null : d
}

/**
 * 선택 차량(또는 전체) + 시작~종료 시각 구간의 SCHEDULED 배차만 CANCELLED(운행 불가)로 변경.
 * IN_PROGRESS 는 건드리지 않음. ended_at 은 설정하지 않음(시작하지 않은 취소).
 */
export async function suspendOperationsInRange(params: {
  busId: string // bus uuid or ALL_BUSES
  startIso: string // from datetime-local, e.g. 2026-08-11T09:00
  endIso: string
  reason: string
}): Promise<{ ok: boolean; message?: string; count?: number }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }

  const start = new Date(params.startIso)
  const end = new Date(params.endIso)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return { ok: false, message: '시작 시각과 종료 시각을 확인해 주세요.' }
  }
  if (end < start) {
    return { ok: false, message: '종료 시각은 시작 시각 이후여야 합니다.' }
  }

  let query = supabase
    .from('operations')
    .select('id, operation_date, bus_id, status, schedules:schedule_id(departure_time)')
    .eq('status', 'SCHEDULED')

  if (params.busId !== ALL_BUSES) {
    if (!params.busId || params.busId === '-') {
      return { ok: false, message: '차량을 선택해 주세요.' }
    }
    query = query.eq('bus_id', params.busId)
  }

  const { data, error } = await query
  if (error) return { ok: false, message: error.message }

  const rows = (data ?? []) as unknown as OpSuspendRow[]
  const targetIds = rows
    .filter((row) => {
      const depart = parseDepartLocal(row.operation_date, row.schedules?.departure_time ?? null)
      if (!depart) return false
      return depart >= start && depart <= end
    })
    .map((r) => r.id)

  if (targetIds.length === 0) {
    return { ok: false, message: '해당 시간대에 운행 불가 처리할 예정 배차가 없습니다.' }
  }

  const { data: updated, error: updErr } = await supabase
    .from('operations')
    .update({ status: 'CANCELLED' })
    .in('id', targetIds)
    .eq('status', 'SCHEDULED')
    .select('id')

  if (updErr) return { ok: false, message: updErr.message }

  const count = updated?.length ?? 0
  if (count > 0) {
    await supabase.from('operation_logs').insert(
      (updated ?? []).map((u) => ({
        operation_id: u.id,
        event_type: 'STATUS_CHANGED' as const,
        log_message: `ADMIN_SUSPEND reason=${params.reason}`,
      })),
    )
  }

  return {
    ok: true,
    count,
    message: `${count}건의 배차를 운행 불가로 처리했습니다.`,
  }
}
