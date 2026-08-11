import { useCallback, useEffect, useMemo, useState } from 'react'
import { createAssignment, deleteAssignment, fetchAssignments } from '../lib/assignmentsApi'
import { resolveAssignmentStatus } from '../lib/assignmentStatus'
import { StatusBadge } from './ui/Form'
import { SCHEDULE_ROUTE_OPTIONS } from '../data/mock'
import { DRIVER_OPTIONS, todayDateKey, type TodayAssignment } from '../types/assignment'

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

/** 관리자 → Supabase `operations` 오늘 배차 관리 */
export function TodayAssignmentsPanel() {
  const [date, setDate] = useState(todayDateKey())
  const [rows, setRows] = useState<TodayAssignment[]>([])
  const [filterDriver, setFilterDriver] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const [driverId, setDriverId] = useState('user01')
  const [routeName, setRouteName] = useState('기흥역 통학버스')
  const [vehicleName, setVehicleName] = useState('2호차')
  const [departTime, setDepartTime] = useState('15:00')
  const [expectedEndTime, setExpectedEndTime] = useState('15:30')
  const [origin, setOrigin] = useState('채플관 앞')
  const [destination, setDestination] = useState('기흥역 5번 출구')

  const load = useCallback(async () => {
    const data = await fetchAssignments({
      date,
      driverId: filterDriver || undefined,
    })
    setRows(data)
  }, [date, filterDriver])

  useEffect(() => {
    void load()
    const timer = window.setInterval(() => void load(), 5_000)
    return () => window.clearInterval(timer)
  }, [load])

  /** 시계 진행에 따라 「곧 출발」 배지 갱신 */
  const [, setTick] = useState(0)
  useEffect(() => {
    const timer = window.setInterval(() => setTick((n) => n + 1), 30_000)
    return () => window.clearInterval(timer)
  }, [])

  const driverName = useMemo(
    () => DRIVER_OPTIONS.find((d) => d.id === driverId)?.name ?? driverId,
    [driverId],
  )

  const onAdd = async () => {
    setBusy(true)
    setMessage(null)
    const result = await createAssignment({
      date,
      driverId,
      driverName,
      routeName,
      vehicleName,
      departTime,
      expectedEndTime,
      origin,
      destination,
      round: 1,
      status: 'scheduled',
    })
    setBusy(false)
    if (!result.ok) {
      setMessage(result.message || '추가 실패')
      return
    }
    setMessage(`배정 추가됨 · ${driverName} (${driverId}) · ${date}`)
    await load()
  }

  const onDelete = async (id: string) => {
    setBusy(true)
    await deleteAssignment(id)
    setBusy(false)
    await load()
  }

  const onSave = async () => {
    setBusy(true)
    setMessage(null)
    const data = await fetchAssignments({
      date,
      driverId: filterDriver || undefined,
    })
    setRows(data)
    setBusy(false)
    setMessage(`${date} 기사 배정이 저장되었습니다. (${data.length}건)`)
  }

  return (
    <section className="card card-pad" style={{ marginTop: 0 }}>
      <div className="card-head">
        <h3>기사 배정</h3>
        <div className="toolbar" style={{ gap: 8 }}>
          <button className="btn btn-outline btn-xs" type="button" onClick={() => void load()} disabled={busy}>
            새로고침
          </button>
          <button className="btn btn-primary btn-xs" type="button" onClick={() => void onSave()} disabled={busy}>
            저장
          </button>
        </div>
      </div>
      <div className="toolbar" style={{ marginBottom: 10, flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
        <select
          className="select"
          style={{ width: 180, height: 32 }}
          value={filterDriver}
          onChange={(e) => setFilterDriver(e.target.value)}
        >
          <option value="">전체 기사</option>
          {DRIVER_OPTIONS.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name} ({d.id})
            </option>
          ))}
        </select>
        <input
          className="input"
          type="date"
          style={{ width: 170, height: 32 }}
          value={date}
          onChange={(e) => setDate(e.target.value || todayDateKey())}
          aria-label="배정 날짜"
        />
        <span className="muted" style={{ fontSize: 12 }}>
          {rows.length}건
        </span>
        {message ? (
          <span className="muted" style={{ fontSize: 12 }}>
            {message}
          </span>
        ) : null}
      </div>

      <table className="data-table dense">
        <thead>
          <tr>
            <th>기사</th>
            <th>노선</th>
            <th>차량</th>
            <th>출발</th>
            <th>종료</th>
            <th>구간</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td colSpan={8} className="muted">
                선택한 날짜에 배정이 없습니다. 아래에서 추가해 주세요.
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={row.id}>
                <td>
                  <div style={{ fontWeight: 600 }}>{row.driverName}</div>
                  <div className="muted" style={{ fontSize: 10 }}>
                    {row.driverId}
                  </div>
                </td>
                <td>{row.routeName}</td>
                <td>{row.vehicleName}</td>
                <td>{row.departTime}</td>
                <td>{row.expectedEndTime}</td>
                <td>
                  {row.origin} → {row.destination}
                </td>
                <td>
                  {(() => {
                    const status = resolveAssignmentStatus(row)
                    return <StatusBadge tone={statusTone[status]}>{statusLabel[status]}</StatusBadge>
                  })()}
                </td>
                <td>
                  <button
                    className="btn btn-outline btn-xs"
                    type="button"
                    disabled={busy}
                    onClick={() => void onDelete(row.id)}
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      <div style={{ marginTop: 14, borderTop: '1px solid #e8eef7', paddingTop: 12 }}>
        <div className="muted" style={{ fontSize: 12, marginBottom: 8, fontWeight: 600 }}>
          배정 추가 · {date}
        </div>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(180px, 1.1fr) minmax(200px, 1.4fr) minmax(120px, 0.8fr) 110px 110px minmax(140px, 1fr) minmax(140px, 1fr) auto',
            gap: 8,
            alignItems: 'center',
          }}
        >
          <select className="select" style={{ width: '100%', height: 36 }} value={driverId} onChange={(e) => setDriverId(e.target.value)}>
            {DRIVER_OPTIONS.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name} ({d.id})
              </option>
            ))}
          </select>
          <select
            className="select"
            style={{ width: '100%', height: 36 }}
            value={routeName}
            onChange={(e) => setRouteName(e.target.value)}
          >
            {SCHEDULE_ROUTE_OPTIONS.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <input
            className="input"
            style={{ width: '100%', height: 36 }}
            value={vehicleName}
            onChange={(e) => setVehicleName(e.target.value)}
            placeholder="차량"
          />
          <input
            className="input"
            style={{ width: '100%', height: 36 }}
            value={departTime}
            onChange={(e) => setDepartTime(e.target.value)}
            placeholder="출발 HH:mm"
          />
          <input
            className="input"
            style={{ width: '100%', height: 36 }}
            value={expectedEndTime}
            onChange={(e) => setExpectedEndTime(e.target.value)}
            placeholder="종료 HH:mm"
          />
          <input
            className="input"
            style={{ width: '100%', height: 36 }}
            value={origin}
            onChange={(e) => setOrigin(e.target.value)}
            placeholder="출발지"
          />
          <input
            className="input"
            style={{ width: '100%', height: 36 }}
            value={destination}
            onChange={(e) => setDestination(e.target.value)}
            placeholder="도착지"
          />
          <button
            className="btn btn-outline"
            type="button"
            style={{ height: 36, whiteSpace: 'nowrap' }}
            disabled={busy}
            onClick={() => void onAdd()}
          >
            배정 추가
          </button>
        </div>
        <div className="toolbar" style={{ marginTop: 12, justifyContent: 'flex-end' }}>
          <button
            className="btn btn-primary"
            type="button"
            style={{ height: 36, minWidth: 96 }}
            disabled={busy}
            onClick={() => void onSave()}
          >
            저장
          </button>
        </div>
      </div>
    </section>
  )
}
