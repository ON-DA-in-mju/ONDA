import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  assignDriverToOperation,
  fetchAssignments,
  fetchDriverOptions,
  type DriverOption,
} from '../lib/assignmentsApi'
import { resolveAssignmentStatus } from '../lib/assignmentStatus'
import { StatusBadge } from './ui/Form'
import { SCHEDULE_ROUTE_OPTIONS } from '../data/mock'
import { todayDateKey, type TodayAssignment } from '../types/assignment'

const statusLabel: Record<TodayAssignment['status'], string> = {
  waiting: '운행 대기',
  departing_soon: '곧 출발',
  scheduled: '운행 예정',
  in_progress: '운행 중',
  ended: '운행 종료',
}

const statusTone: Record<TodayAssignment['status'], 'gray' | 'orange' | 'blue' | 'green'> = {
  waiting: 'gray',
  departing_soon: 'orange',
  scheduled: 'blue',
  in_progress: 'green',
  ended: 'gray',
}

/**
 * 기사 배정 — DB에 이미 있는 배차(operations) 목록을 나열하고
 * 오른쪽에서 기사만 선택·변경한다.
 */
export function TodayAssignmentsPanel() {
  const [params] = useSearchParams()
  const [date, setDate] = useState(() => params.get('date') || todayDateKey())
  const [rows, setRows] = useState<TodayAssignment[]>([])
  const [drivers, setDrivers] = useState<DriverOption[]>([])
  const [filterDriver, setFilterDriver] = useState('')
  const [filterRoute, setFilterRoute] = useState('')
  const [busyId, setBusyId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [messageError, setMessageError] = useState(false)

  useEffect(() => {
    const q = params.get('date')
    if (q) setDate(q)
  }, [params])

  const loadDrivers = useCallback(async () => {
    setDrivers(await fetchDriverOptions())
  }, [])

  const load = useCallback(async () => {
    const data = await fetchAssignments({
      date,
      driverId: filterDriver || undefined,
    })
    setRows(data)
  }, [date, filterDriver])

  const inFlightRef = useRef(false)

  useEffect(() => {
    void loadDrivers()
  }, [loadDrivers])

  useEffect(() => {
    let alive = true
    const tick = async () => {
      if (inFlightRef.current) return
      inFlightRef.current = true
      try {
        if (!alive) return
        await load()
      } finally {
        inFlightRef.current = false
      }
    }
    void tick()
    const timer = window.setInterval(() => void tick(), 15_000)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [load])

  /** 「곧 출발」 배지 갱신 */
  const [, setTick] = useState(0)
  useEffect(() => {
    const timer = window.setInterval(() => setTick((n) => n + 1), 30_000)
    return () => window.clearInterval(timer)
  }, [])

  const visibleRows = useMemo(() => {
    if (!filterRoute) return rows
    return rows.filter((r) => r.routeName === filterRoute)
  }, [rows, filterRoute])

  const onAssignDriver = async (row: TodayAssignment, nextDriverId: string) => {
    if (!nextDriverId || nextDriverId === row.driverId) return
    setBusyId(row.id)
    setMessage(null)
    setMessageError(false)
    const result = await assignDriverToOperation(row.id, nextDriverId)
    setBusyId(null)
    if (!result.ok) {
      setMessageError(true)
      setMessage(result.message || '기사 배정 실패')
      return
    }
    const name = drivers.find((d) => d.id === nextDriverId)?.name ?? nextDriverId
    setMessageError(false)
    setMessage(`${row.departTime} ${row.routeName} · ${name} 배정됨`)
    await load()
  }

  return (
    <section className="card card-pad" style={{ marginTop: 0 }}>
      <div className="card-head">
        <h3>기사 배정</h3>
        <div className="toolbar" style={{ gap: 8 }}>
          <button
            className="btn btn-outline btn-xs"
            type="button"
            onClick={() => void load()}
            disabled={busyId != null}
          >
            새로고침
          </button>
        </div>
      </div>

      <p className="muted" style={{ fontSize: 13, marginTop: 0, marginBottom: 12 }}>
        선택한 날짜의 운행 목록입니다. 기사가 없는 운행은 --- 로 표시되며, 드롭다운에서 기사를 지정합니다. 같은
        기사의 출발~종료가 겹치면 저장되지 않습니다.
      </p>

      <div className="toolbar" style={{ marginBottom: 10, flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
        <input
          className="input"
          type="date"
          style={{ width: 170, height: 32 }}
          value={date}
          onChange={(e) => setDate(e.target.value || todayDateKey())}
          aria-label="배정 날짜"
        />
        <select
          className="select"
          style={{ width: 180, height: 32 }}
          value={filterRoute}
          onChange={(e) => setFilterRoute(e.target.value)}
          aria-label="노선 필터"
        >
          <option value="">노선 전체</option>
          {SCHEDULE_ROUTE_OPTIONS.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>
        <select
          className="select"
          style={{ width: 180, height: 32 }}
          value={filterDriver}
          onChange={(e) => setFilterDriver(e.target.value)}
          aria-label="기사 필터"
        >
          <option value="">전체 기사</option>
          {drivers.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name} ({d.id})
            </option>
          ))}
        </select>
        <span className="muted" style={{ fontSize: 12 }}>
          {visibleRows.length}건
          {filterRoute || filterDriver ? ` / 전체 ${rows.length}건` : ''}
        </span>
        {message ? (
          <span style={{ fontSize: 12, color: messageError ? '#b91c1c' : undefined }} className={messageError ? undefined : 'muted'}>
            {message}
          </span>
        ) : null}
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table className="data-table dense">
          <thead>
            <tr>
              <th style={{ width: 48 }}>#</th>
              <th>노선</th>
              <th>호차</th>
              <th>출발</th>
              <th>종료</th>
              <th>구간</th>
              <th>상태</th>
              <th style={{ minWidth: 180 }}>기사</th>
            </tr>
          </thead>
          <tbody>
            {visibleRows.length === 0 ? (
              <tr>
                <td colSpan={8} className="muted">
                  선택한 날짜에 운행이 없습니다. 「운행 관리」에서 먼저 운행을 생성해 주세요.
                </td>
              </tr>
            ) : (
              visibleRows.map((row, idx) => {
                const status = resolveAssignmentStatus(row)
                const assigning = busyId === row.id
                const driverInList = drivers.some((d) => d.id === row.driverId)
                return (
                  <tr key={row.id}>
                    <td>{idx + 1}</td>
                    <td>{row.routeName}</td>
                    <td style={{ fontWeight: 600 }}>{row.vehicleName}</td>
                    <td style={{ fontWeight: 700 }}>{row.departTime}</td>
                    <td>{row.expectedEndTime || '—'}</td>
                    <td>
                      {row.origin && row.destination
                        ? `${row.origin} → ${row.destination}`
                        : '—'}
                    </td>
                    <td>
                      <StatusBadge tone={statusTone[status]}>{statusLabel[status]}</StatusBadge>
                    </td>
                    <td>
                      <select
                        className="select"
                        style={{ width: '100%', minWidth: 160, height: 32 }}
                        value={row.driverId || ''}
                        disabled={assigning || status === 'in_progress' || status === 'ended'}
                        onChange={(e) => void onAssignDriver(row, e.target.value)}
                        aria-label={`${row.routeName} ${row.departTime} 기사 선택`}
                      >
                        <option value="">---</option>
                        {!driverInList && row.driverId ? (
                          <option value={row.driverId}>
                            {row.driverName || row.driverId} ({row.driverId})
                          </option>
                        ) : null}
                        {drivers.map((d) => (
                          <option key={d.id} value={d.id}>
                            {d.name} ({d.id})
                          </option>
                        ))}
                      </select>
                      {assigning ? (
                        <div className="muted" style={{ fontSize: 10, marginTop: 2 }}>
                          저장 중…
                        </div>
                      ) : null}
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}
