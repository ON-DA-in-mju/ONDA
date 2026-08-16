import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchBuses } from '../lib/api'
import { fetchRouteCatalog } from '../lib/routesApi'
import {
  addMinutesToHm,
  copyOperationsFromPreviousWeek,
  createUnassignedOperation,
  deleteAssignment,
  fetchAssignments,
  fetchRouteStopChoices,
  type RouteStopOption,
} from '../lib/assignmentsApi'
import { resolveAssignmentStatus } from '../lib/assignmentStatus'
import { semesterForDate } from '../lib/academicCalendar'
import { fetchSchedulesWithRoutes, type ScheduleWithRoute } from '../lib/seedMju'
import { resolveOperationalRouteName } from '../lib/routeVariants'
import { StatusBadge } from './ui/Form'
import { todayDateKey, type TodayAssignment } from '../types/assignment'
import type { Weekday } from '../types/database'

const JS_TO_WEEKDAY: Weekday[] = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

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

type BusOption = { name: string; plate: string }

/**
 * 운행 관리 — 선택한 날짜의 operations 를 기사 없이 생성한다.
 */
export function TodayOperationsPanel() {
  const [params] = useSearchParams()
  const [date, setDate] = useState(() => params.get('date') || todayDateKey())
  const [rows, setRows] = useState<TodayAssignment[]>([])
  const [buses, setBuses] = useState<BusOption[]>([])
  const [schedules, setSchedules] = useState<ScheduleWithRoute[]>([])
  const [stops, setStops] = useState<RouteStopOption[]>([])
  const [originOptions, setOriginOptions] = useState<string[]>([])
  const [destinationOptions, setDestinationOptions] = useState<string[]>([])
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [routeName, setRouteName] = useState('')
  const [routeOptions, setRouteOptions] = useState<string[]>([])
  const [scheduleKey, setScheduleKey] = useState('')
  const [departTime, setDepartTime] = useState('08:00')
  const [vehicleName, setVehicleName] = useState('')
  const [expectedEndTime, setExpectedEndTime] = useState('')
  const [round, setRound] = useState(1)
  const [busy, setBusy] = useState(false)
  const [copyBusy, setCopyBusy] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    const q = params.get('date')
    if (q) setDate(q)
  }, [params])

  const weekday = JS_TO_WEEKDAY[new Date(`${date}T12:00:00`).getDay()] ?? 'MON'
  const semester = semesterForDate(date)

  const loadRows = useCallback(async () => {
    setRows(await fetchAssignments({ date }))
  }, [date])

  useEffect(() => {
    void loadRows()
  }, [loadRows])

  useEffect(() => {
    let alive = true
    void fetchRouteCatalog().then((rows) => {
      if (!alive) return
      const names = rows.map((r) => r.name)
      setRouteOptions(names)
      setRouteName((prev) => (prev && names.includes(prev) ? prev : names[0] ?? ''))
    })
    return () => {
      alive = false
    }
  }, [])

  useEffect(() => {
    let alive = true
    void fetchBuses().then((data) => {
      if (!alive) return
      const options = (data ?? [])
        .filter((b) => b.status !== 'INACTIVE')
        .map((b) => ({ name: b.bus_name, plate: b.vehicle_number }))
      setBuses(options)
      setVehicleName((prev) => prev || options[0]?.name || '')
    })
    return () => {
      alive = false
    }
  }, [])

  useEffect(() => {
    let alive = true
    const loadSchedules = async () => {
      if (!routeName) {
        if (alive) setSchedules([])
        return
      }
      let data = await fetchSchedulesWithRoutes({ weekday, semester, routeName })
      if (!data?.length) {
        data = await fetchSchedulesWithRoutes({ weekday, routeName })
      }
      if (!alive) return
      setSchedules(data ?? [])
    }
    void loadSchedules()
    return () => {
      alive = false
    }
  }, [weekday, semester, routeName])

  const departureOptions = useMemo(() => {
    const seen = new Set<string>()
    const list: { key: string; time: string; scheduleId: string; actualRouteName: string }[] = []
    for (const row of schedules) {
      const time = (row.departure_time || '').slice(0, 5)
      if (!time) continue
      const actualRouteName = row.routes?.route_name || routeName
      const key = `${row.id}`
      if (seen.has(key)) continue
      seen.add(key)
      list.push({ key, time, scheduleId: row.id, actualRouteName })
    }
    return list.sort((a, b) => a.time.localeCompare(b.time))
  }, [schedules, routeName])

  const selectedDepart = departureOptions.find((o) => o.key === scheduleKey) ?? null

  useEffect(() => {
    if (!departureOptions.length) {
      setScheduleKey('')
      return
    }
    if (!departureOptions.some((o) => o.key === scheduleKey)) {
      setScheduleKey(departureOptions[0].key)
      setDepartTime(departureOptions[0].time)
    }
  }, [departureOptions, scheduleKey])

  const actualRouteForStops = resolveOperationalRouteName({
    baseRouteName: selectedDepart?.actualRouteName || routeName,
    departureTime: departTime || selectedDepart?.time || '08:00',
    date,
  })

  useEffect(() => {
    let alive = true
    void fetchRouteStopChoices(actualRouteForStops).then((data) => {
      if (!alive) return
      setStops(data.stops)
      setOriginOptions(data.originOptions)
      setDestinationOptions(data.destinationOptions)
      setOrigin((prev) => (prev && data.originOptions.includes(prev) ? prev : data.originOptions[0] ?? ''))
      setDestination((prev) =>
        prev && data.destinationOptions.includes(prev) ? prev : data.destinationOptions[0] ?? '',
      )
    })
    return () => {
      alive = false
    }
  }, [actualRouteForStops])

  const lastExpectedMinutes =
    stops.find((s) => s.name === destination)?.expectedMinutes ??
    stops[stops.length - 1]?.expectedMinutes ??
    null

  useEffect(() => {
    if (!departTime) {
      setExpectedEndTime('')
      return
    }
    const add = lastExpectedMinutes && lastExpectedMinutes > 0 ? lastExpectedMinutes : 60
    setExpectedEndTime(addMinutesToHm(departTime, add))
  }, [departTime, lastExpectedMinutes])

  const onCreate = async () => {
    if (!routeName) {
      setMessage('노선을 선택해 주세요.')
      return
    }
    if (!departTime) {
      setMessage('출발 시각을 입력해 주세요.')
      return
    }
    if (!vehicleName) {
      setMessage('차량을 선택해 주세요.')
      return
    }
    setBusy(true)
    setMessage(null)
    const result = await createUnassignedOperation({
      date,
      routeName: selectedDepart?.actualRouteName || routeName,
      departTime,
      expectedEndTime,
      vehicleName,
      origin: origin || undefined,
      destination: destination || undefined,
      round,
    })
    setBusy(false)
    if (!result.ok) {
      setMessage(result.message || '운행 생성 실패')
      return
    }
    setMessage(`${departTime} ${selectedDepart?.actualRouteName || routeName} 운행이 생성되었습니다. 기사는 기사 배정에서 지정하세요.`)
    await loadRows()
  }

  const onCopyPreviousWeek = async () => {
    const source = new Date(`${date}T12:00:00`)
    source.setDate(source.getDate() - 7)
    const sourceKey = `${source.getFullYear()}-${String(source.getMonth() + 1).padStart(2, '0')}-${String(source.getDate()).padStart(2, '0')}`
    if (
      !window.confirm(
        `${sourceKey} 운행(노선·시간·차량)을 ${date}에 만들까요?\n기사는 복사하지 않으며, 이미 같은 운행이 있으면 건너뜁니다.`,
      )
    ) {
      return
    }
    setCopyBusy(true)
    setMessage(null)
    const result = await copyOperationsFromPreviousWeek(date)
    setCopyBusy(false)
    setMessage(result.message)
    await loadRows()
  }

  const onDelete = async (row: TodayAssignment) => {
    const label = [row.departTime, row.routeName, row.vehicleName].filter(Boolean).join(' ')
    if (!window.confirm(`${label} 운행을 삭제할까요?`)) return
    setDeletingId(row.id)
    setMessage(null)
    const result = await deleteAssignment(row.id)
    setDeletingId(null)
    if (!result.ok) {
      setMessage(result.message || '삭제 실패')
      return
    }
    setMessage(`${label} 운행을 삭제했습니다.`)
    await loadRows()
  }

  return (
    <section className="card card-pad" style={{ marginTop: 0 }}>
      <div className="card-head">
        <h3>운행 관리</h3>
        <div className="toolbar" style={{ gap: 8 }}>
          <button
            className="btn btn-outline btn-xs"
            type="button"
            onClick={() => void onCopyPreviousWeek()}
            disabled={busy || copyBusy}
          >
            {copyBusy ? '불러오는 중…' : '7일 전 운행 불러오기'}
          </button>
          <button className="btn btn-outline btn-xs" type="button" onClick={() => void loadRows()} disabled={busy || copyBusy}>
            새로고침
          </button>
        </div>
      </div>

      <p className="muted" style={{ fontSize: 13, marginTop: 0, marginBottom: 12 }}>
        선택한 날짜의 운행을 생성합니다. 하나하나 만들기 어려우면 「7일 전 운행 불러오기」로 저번 주와 같은 노선·시간·차량을
        한 번에 만들 수 있습니다. 기사 배정은 「기사 배정」 화면에서 합니다.
      </p>

      <div
        className="toolbar"
        style={{ marginBottom: 14, flexWrap: 'wrap', gap: 8, alignItems: 'flex-end' }}
      >
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="op-date">날짜</label>
          <input
            id="op-date"
            className="input"
            type="date"
            style={{ width: 170, height: 32 }}
            value={date}
            onChange={(e) => setDate(e.target.value || todayDateKey())}
          />
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="op-route">노선</label>
          <select
            id="op-route"
            className="select"
            style={{ width: 220, height: 32 }}
            value={routeName}
            onChange={(e) => setRouteName(e.target.value)}
          >
            {routeOptions.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="op-depart">출발</label>
          <input
            id="op-depart"
            className="input"
            type="time"
            style={{ width: 130, height: 32 }}
            value={departTime}
            onChange={(e) => {
              setDepartTime(e.target.value)
              const matched = departureOptions.find((o) => o.time === e.target.value)
              setScheduleKey(matched?.key ?? '')
            }}
          />
        </div>
        {departureOptions.length > 0 ? (
          <div className="field" style={{ margin: 0 }}>
            <label htmlFor="op-timetable">시간표</label>
            <select
              id="op-timetable"
              className="select"
              style={{ width: 160, height: 32 }}
              value={scheduleKey}
              onChange={(e) => {
                setScheduleKey(e.target.value)
                const opt = departureOptions.find((o) => o.key === e.target.value)
                if (opt) setDepartTime(opt.time)
              }}
            >
              <option value="">직접 입력</option>
              {departureOptions.map((opt) => (
                <option key={opt.key} value={opt.key}>
                  {opt.time}
                  {opt.actualRouteName !== routeName ? ` · ${opt.actualRouteName}` : ''}
                </option>
              ))}
            </select>
          </div>
        ) : null}
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="op-end">종료</label>
          <input
            id="op-end"
            className="input"
            type="time"
            style={{ width: 130, height: 32 }}
            value={expectedEndTime}
            onChange={(e) => setExpectedEndTime(e.target.value)}
          />
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="op-bus">차량</label>
          <select
            id="op-bus"
            className="select"
            style={{ width: 180, height: 32 }}
            value={vehicleName}
            onChange={(e) => setVehicleName(e.target.value)}
          >
            {buses.length === 0 ? (
              <option value="">차량 없음</option>
            ) : (
              buses.map((b) => (
                <option key={b.name} value={b.name}>
                  {b.name}
                  {b.plate ? ` (${b.plate})` : ''}
                </option>
              ))
            )}
          </select>
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="op-origin">출발 정류장</label>
          <select
            id="op-origin"
            className="select"
            style={{ width: 200, height: 32 }}
            value={origin}
            onChange={(e) => setOrigin(e.target.value)}
          >
            {originOptions.length === 0 ? (
              <option value="">정류장 없음</option>
            ) : (
              originOptions.map((name) => (
                <option key={`origin-${name}`} value={name}>
                  {name}
                </option>
              ))
            )}
          </select>
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="op-dest">도착 정류장</label>
          <select
            id="op-dest"
            className="select"
            style={{ width: 200, height: 32 }}
            value={destination}
            onChange={(e) => setDestination(e.target.value)}
          >
            {destinationOptions.length === 0 ? (
              <option value="">정류장 없음</option>
            ) : (
              destinationOptions.map((name) => (
                <option key={`dest-${name}`} value={name}>
                  {name}
                </option>
              ))
            )}
          </select>
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="op-round">회차</label>
          <input
            id="op-round"
            className="input"
            type="number"
            min={1}
            style={{ width: 80, height: 32 }}
            value={round}
            onChange={(e) => setRound(Math.max(1, Number(e.target.value) || 1))}
          />
        </div>
        <button className="btn btn-primary" type="button" style={{ height: 32 }} onClick={() => void onCreate()} disabled={busy || copyBusy}>
          {busy ? '저장 중…' : '운행 생성'}
        </button>
      </div>

      {message ? (
        <p className="muted" style={{ fontSize: 13, marginTop: 0, marginBottom: 12 }}>
          {message}
        </p>
      ) : null}

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
              <th>회차</th>
              <th>상태</th>
              <th style={{ width: 88 }}>관리</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={9} className="muted">
                  이 날짜에 생성된 운행이 없습니다. 위에서 시간·노선·차량을 고른 뒤 「운행 생성」을 눌러 주세요.
                </td>
              </tr>
            ) : (
              rows.map((row, idx) => {
                const status = resolveAssignmentStatus(row)
                const deleting = deletingId === row.id
                return (
                  <tr key={row.id}>
                    <td>{idx + 1}</td>
                    <td>{row.routeName}</td>
                    <td style={{ fontWeight: 600 }}>{row.vehicleName}</td>
                    <td style={{ fontWeight: 700 }}>{row.departTime}</td>
                    <td>{row.expectedEndTime || '—'}</td>
                    <td>
                      {row.origin && row.destination ? `${row.origin} → ${row.destination}` : '—'}
                    </td>
                    <td>{row.round}</td>
                    <td>
                      <StatusBadge tone={statusTone[status]}>{statusLabel[status]}</StatusBadge>
                    </td>
                    <td>
                      <button
                        className="btn btn-outline btn-xs"
                        type="button"
                        onClick={() => void onDelete(row)}
                        disabled={busy || copyBusy || deletingId != null}
                      >
                        {deleting ? '삭제 중…' : '삭제'}
                      </button>
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
