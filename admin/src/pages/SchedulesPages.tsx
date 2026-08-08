import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { SCHEDULE_ROUTE_OPTIONS, drivers, schedules } from '../data/mock'
import { fetchAssignments } from '../lib/assignmentsApi'
import { resolveAssignmentStatus } from '../lib/assignmentStatus'
import { todayDateKey } from '../types/assignment'
import {
  WEEKDAY_LABELS,
  addDays,
  formatDotDate,
  formatWeekRange,
  parseDateKey,
  toDateKey,
  weekRangeFromDate,
} from '../lib/weekDate'
import { TodayAssignmentsPanel } from '../components/TodayAssignmentsPanel'
import { StatusBadge } from '../components/ui/Form'
import type { TodayAssignment } from '../types/assignment'

type ScheduleRowStatus = '운행 예정' | '곧 출발' | '운행 중' | '운행 종료'
type ScheduleTone = 'blue' | 'orange' | 'green' | 'gray'

function statusFromAssignments(items: TodayAssignment[]): {
  status: ScheduleRowStatus
  tone: ScheduleTone
  rounds: number
} {
  const resolved = items.map((a) => resolveAssignmentStatus(a))
  const rounds = items.length
  if (resolved.some((s) => s === 'in_progress')) {
    return { status: '운행 중', tone: 'green', rounds }
  }
  if (resolved.some((s) => s === 'departing_soon')) {
    return { status: '곧 출발', tone: 'orange', rounds }
  }
  if (resolved.some((s) => s === 'ended') && resolved.every((s) => s === 'ended')) {
    return { status: '운행 종료', tone: 'gray', rounds }
  }
  if (rounds > 0) {
    return { status: '운행 예정', tone: 'blue', rounds }
  }
  return { status: '운행 예정', tone: 'blue', rounds: 0 }
}

/** ADM-03 운행 일정 목록 — Figma 좌/우 위젯 구조 */
export function SchedulesPage() {
  const initialWeek = weekRangeFromDate(new Date())
  const [weekStart, setWeekStart] = useState(initialWeek.start)
  const weekEnd = useMemo(() => addDays(weekStart, 6), [weekStart])
  const [weekday, setWeekday] = useState(() => new Date().getDay())
  const [routeFilter, setRouteFilter] = useState('')
  const [assignments, setAssignments] = useState<TodayAssignment[]>([])
  const weekInputRef = useRef<HTMLInputElement>(null)

  const selectedDate = useMemo(() => addDays(weekStart, weekday), [weekStart, weekday])
  const selectedDateKey = toDateKey(selectedDate)
  const weekLabel = formatWeekRange(weekStart, weekEnd)

  const load = useCallback(async () => {
    const rows = await fetchAssignments({ date: selectedDateKey })
    setAssignments(rows)
  }, [selectedDateKey])

  useEffect(() => {
    void load()
    const timer = window.setInterval(() => void load(), 5_000)
    return () => window.clearInterval(timer)
  }, [load])

  const listRows = useMemo(() => {
    return schedules
      .filter((row) => !routeFilter || row.route === routeFilter)
      .map((row) => {
        const routeItems = assignments.filter((a) => a.routeName === row.route)
        const derived = statusFromAssignments(routeItems)
        return { ...row, ...derived }
      })
  }, [assignments, routeFilter])

  const totalRounds = listRows.reduce((sum, row) => sum + row.rounds, 0)
  const focusRoute = listRows[0]?.route ?? SCHEDULE_ROUTE_OPTIONS[0]

  const onPickWeekDate = (value: string) => {
    if (!value) return
    const { start } = weekRangeFromDate(parseDateKey(value))
    setWeekStart(start)
  }

  const onReset = () => {
    const { start } = weekRangeFromDate(new Date())
    setWeekStart(start)
    setWeekday(new Date().getDay())
    setRouteFilter('')
  }

  return (
    <div className="page">
      <div className="sched-grid">
        <div className="sched-col">
          <section className="card card-pad">
            <div className="card-head">
              <h3>운행 일정 조회</h3>
            </div>
            <div className="toolbar" style={{ flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
              <select
                className="select"
                style={{ width: 170 }}
                value={routeFilter}
                onChange={(e) => setRouteFilter(e.target.value)}
              >
                <option value="">노선 전체</option>
                {SCHEDULE_ROUTE_OPTIONS.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
              <div className="toolbar" style={{ gap: 4 }}>
                {WEEKDAY_LABELS.map((d, i) => (
                  <button
                    key={d}
                    className={`page-chip${weekday === i ? ' active' : ''}`}
                    type="button"
                    onClick={() => setWeekday(i)}
                  >
                    {d}
                  </button>
                ))}
              </div>
              <div style={{ position: 'relative' }}>
                <button
                  className="input"
                  type="button"
                  style={{
                    width: 220,
                    height: 36,
                    textAlign: 'left',
                    cursor: 'pointer',
                    background: '#fff',
                  }}
                  onClick={() => {
                    const el = weekInputRef.current
                    if (!el) return
                    el.showPicker?.()
                    el.click()
                  }}
                >
                  {weekLabel}
                </button>
                <input
                  ref={weekInputRef}
                  type="date"
                  value={toDateKey(selectedDate)}
                  onChange={(e) => onPickWeekDate(e.target.value)}
                  style={{
                    position: 'absolute',
                    opacity: 0,
                    pointerEvents: 'none',
                    width: 0,
                    height: 0,
                  }}
                  aria-label="주간 기간 선택"
                />
              </div>
              <button className="btn btn-primary" type="button" onClick={() => void load()}>
                조회하기
              </button>
              <button className="btn btn-outline" type="button" onClick={onReset}>
                초기화
              </button>
            </div>
          </section>

          <section className="card card-pad">
            <div className="card-head">
              <h3>운행 일정 목록</h3>
              <div className="toolbar" style={{ gap: 8 }}>
                <Link className="btn btn-primary" to="/schedules/assignments" style={{ height: 30 }}>
                  기사 배정
                </Link>
                <Link className="btn btn-ghost" to="/schedules/suspend" style={{ height: 30 }}>
                  운행 중단 처리
                </Link>
              </div>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>순번</th>
                  <th>노선</th>
                  <th>시작</th>
                  <th>종료</th>
                  <th>배차 간격</th>
                  <th>운행 횟수</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {listRows.map((row) => (
                  <tr key={row.route}>
                    <td>{row.no}</td>
                    <td>{row.route}</td>
                    <td>{row.start}</td>
                    <td>{row.end}</td>
                    <td>{row.interval}</td>
                    <td>{row.rounds}</td>
                    <td>
                      <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                    </td>
                    <td>
                      <Link
                        className="btn btn-outline"
                        to={`/schedules/detail?route=${encodeURIComponent(row.route)}&date=${selectedDateKey}&weekday=${weekday}`}
                        style={{ height: 28, fontSize: 12 }}
                      >
                        상세
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="pagination">
              <span>총 {listRows.length}건</span>
            </div>
          </section>

          <section className="card card-pad">
            <div className="card-head">
              <h3>예외 일정 관리</h3>
              <button className="btn btn-primary" type="button" style={{ height: 30, fontSize: 12 }}>
                + 예외 일정 등록
              </button>
            </div>
            <div className="toolbar" style={{ marginBottom: 10 }}>
              <button className="btn btn-ghost" type="button" style={{ height: 30 }}>
                예정된 예외
              </button>
              <button className="btn btn-outline" type="button" style={{ height: 30 }}>
                지난 예외
              </button>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>일자</th>
                  <th>사유</th>
                  <th>상태</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{formatDotDate(addDays(weekStart, 3))}</td>
                  <td>정기 점검</td>
                  <td>
                    <StatusBadge tone="orange">확정</StatusBadge>
                  </td>
                </tr>
                <tr>
                  <td>{formatDotDate(addDays(weekStart, 5))}</td>
                  <td>축제 일정으로 배차 증편</td>
                  <td>
                    <StatusBadge tone="orange">확정</StatusBadge>
                  </td>
                </tr>
              </tbody>
            </table>
          </section>
        </div>

        <div className="sched-col">
          <section className="card card-pad">
            <div className="card-head">
              <h3>운행 일정 요약 · {weekLabel}</h3>
              <div className="toolbar">
                <button className="btn btn-outline" type="button">
                  엑셀 다운로드
                </button>
                <Link className="btn btn-primary" to="/schedules/bulk">
                  + 운행 일정 생성
                </Link>
              </div>
            </div>
            <div className="grid grid-2">
              {[
                ['선택일 배차 수', `${totalRounds}회`, formatDotDate(selectedDate)],
                ['등록 노선', `${listRows.length}개`, '기흥·명지대·시내'],
                ['총 운행 차량', '8대', '노선 평균 2~3대'],
                ['예상 탑승 인원', '—', '추후 연동'],
              ].map(([t, v, s]) => (
                <div key={t} className="card card-pad" style={{ boxShadow: 'none' }}>
                  <div className="muted" style={{ fontSize: 12 }}>
                    {t}
                  </div>
                  <div style={{ fontSize: 20, fontWeight: 800 }}>{v}</div>
                  <div className="muted" style={{ fontSize: 11 }}>
                    {s}
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="card card-pad">
            <div className="card-head">
              <h3>
                상세 시간표 ({WEEKDAY_LABELS[weekday]}요일 · {focusRoute})
              </h3>
            </div>
            <div className="grid grid-2">
              {[`${focusRoute} 왕편`, `${focusRoute} 복편`].map((title) => (
                <div key={title}>
                  <strong style={{ fontSize: 13 }}>{title}</strong>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>회차</th>
                        <th>출발</th>
                        <th>차량</th>
                      </tr>
                    </thead>
                    <tbody>
                      {assignments
                        .filter((a) => a.routeName === focusRoute)
                        .map((a, idx) => (
                          <tr key={a.id}>
                            <td>{a.round || idx + 1}</td>
                            <td>{a.departTime}</td>
                            <td>{a.vehicleName}</td>
                          </tr>
                        ))}
                      {assignments.filter((a) => a.routeName === focusRoute).length === 0 ? (
                        <tr>
                          <td colSpan={3} className="muted">
                            선택일 배차 없음
                          </td>
                        </tr>
                      ) : null}
                    </tbody>
                  </table>
                </div>
              ))}
            </div>
          </section>

          <section className="card card-pad">
            <div className="card-head">
              <h3>운행 패턴 미리보기</h3>
              <span className="muted" style={{ fontSize: 12 }}>
                전체 보기
              </span>
            </div>
            <div className="pattern-bars" aria-hidden>
              {[40, 70, 95, 80, 55, 45, 60, 75, 50, 30].map((h, i) => (
                <div key={i} className="pattern-col">
                  <span style={{ height: `${h}%` }} className="bar bar-a" />
                  <span style={{ height: `${Math.max(18, h - 25)}%` }} className="bar bar-b" />
                </div>
              ))}
            </div>
            <div className="legend-row">
              <span>
                <i style={{ background: '#266ef4' }} /> 평일
              </span>
              <span>
                <i style={{ background: '#3fb46a' }} /> 주말
              </span>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}

/** 기사 배정 전용 화면 — 운행 일정 목록의 「기사 배정」에서 진입 */
export function ScheduleAssignmentsPage() {
  return (
    <div className="page">
      <div className="toolbar" style={{ marginBottom: 4 }}>
        <Link className="btn btn-ghost" to="/schedules" style={{ height: 30 }}>
          ← 운행 일정 목록
        </Link>
      </div>
      <TodayAssignmentsPanel />
    </div>
  )
}

export function ScheduleDetailPage() {
  const [params] = useSearchParams()
  const routeName = params.get('route') || SCHEDULE_ROUTE_OPTIONS[0]
  const date = params.get('date') || todayDateKey()
  const weekday = Number(params.get('weekday') ?? new Date().getDay())
  const weekdayLabel = WEEKDAY_LABELS[Number.isFinite(weekday) ? weekday : 0] ?? '일'

  const template = schedules.find((s) => s.route === routeName) ?? schedules[0]
  const [routeAssignments, setRouteAssignments] = useState<TodayAssignment[]>([])

  useEffect(() => {
    let alive = true
    const loadDetail = async () => {
      const rows = await fetchAssignments({ date })
      if (!alive) return
      setRouteAssignments(rows.filter((a) => a.routeName === routeName))
    }
    void loadDetail()
    const timer = window.setInterval(() => void loadDetail(), 5_000)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [date, routeName])

  const derived = statusFromAssignments(routeAssignments)
  const driverNames = [...new Set(routeAssignments.map((a) => a.driverName))]
  const vehicles = [...new Set(routeAssignments.map((a) => a.vehicleName))]
  const driverSummary =
    driverNames.length === 0
      ? '배정 없음'
      : driverNames.length === 1
        ? driverNames[0]
        : `${driverNames[0]} 외 ${driverNames.length - 1}명`
  const vehicleSummary = vehicles.length === 0 ? '배정 없음' : vehicles.join(', ')

  const statusTone = derived.tone

  return (
    <div className="page">
      <div className="toolbar" style={{ marginBottom: 4 }}>
        <Link className="btn btn-ghost" to="/schedules" style={{ height: 30 }}>
          ← 운행 일정 목록
        </Link>
      </div>
      <section className="card card-pad">
        <div className="card-head">
          <h3>
            운행 일정 상세 · {routeName} ({weekdayLabel}요일)
          </h3>
        </div>
        <div className="muted" style={{ fontSize: 12, marginBottom: 10 }}>
          {formatDotDate(parseDateKey(date))}
        </div>
        <div className="grid grid-4">
          {[
            ['노선', routeName],
            ['운행 요일', `${weekdayLabel}요일`],
            ['운행 시간', `${template.start} ~ ${template.end}`],
            ['배차 간격', template.interval],
            ['총 회차', `${derived.rounds}회`],
            ['배차 차량', vehicleSummary],
            ['담당 기사', driverSummary],
            ['상태', derived.status],
          ].map(([k, v]) => (
            <div key={k} className="card card-pad" style={{ boxShadow: 'none' }}>
              <div className="muted" style={{ fontSize: 12 }}>
                {k}
              </div>
              <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8 }}>
                {k === '상태' ? <StatusBadge tone={statusTone}>{v}</StatusBadge> : v}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="card card-pad">
        <div className="card-head">
          <h3>선택일 배차 목록</h3>
        </div>
        <table className="data-table dense">
          <thead>
            <tr>
              <th>기사</th>
              <th>차량</th>
              <th>출발</th>
              <th>종료</th>
              <th>구간</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {routeAssignments.length === 0 ? (
              <tr>
                <td colSpan={6} className="muted">
                  해당 일자·노선에 배정이 없습니다.
                </td>
              </tr>
            ) : (
              routeAssignments.map((row) => {
                const status = resolveAssignmentStatus(row)
                const label =
                  status === 'departing_soon'
                    ? '곧 출발'
                    : status === 'in_progress'
                      ? '운행 중'
                      : status === 'ended'
                        ? '운행 종료'
                        : status === 'waiting'
                          ? '운행 대기'
                          : '운행 예정'
                const tone =
                  status === 'departing_soon'
                    ? 'orange'
                    : status === 'in_progress'
                      ? 'green'
                      : status === 'ended'
                        ? 'gray'
                        : 'blue'
                return (
                  <tr key={row.id}>
                    <td>
                      {row.driverName}
                      <div className="muted" style={{ fontSize: 10 }}>
                        {row.driverId}
                      </div>
                    </td>
                    <td>{row.vehicleName}</td>
                    <td>{row.departTime}</td>
                    <td>{row.expectedEndTime}</td>
                    <td>
                      {row.origin} → {row.destination}
                    </td>
                    <td>
                      <StatusBadge tone={tone}>{label}</StatusBadge>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </section>

      <section className="card card-pad">
        <div className="card-head">
          <h3>담당 정보 (관리자 전용)</h3>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>역할</th>
              <th>이름</th>
              <th>연락처</th>
              <th>비고</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>운행 관리자</td>
              <td>이운영</td>
              <td>010-1234-5678</td>
              <td>평일 주간</td>
            </tr>
            {driverNames.length === 0 ? (
              <tr>
                <td>현장 담당</td>
                <td colSpan={3} className="muted">
                  배정된 기사 없음
                </td>
              </tr>
            ) : (
              driverNames.map((name) => {
                const driver = drivers.find((d) => d.name === name)
                return (
                  <tr key={name}>
                    <td>담당 기사</td>
                    <td>{name}</td>
                    <td>{driver?.phone ?? '-'}</td>
                    <td>{routeName}</td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </section>
    </div>
  )
}

export function ScheduleBulkPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>일괄 등록 미리보기</h3>
          <div className="toolbar">
            <button className="btn btn-outline" type="button">
              다시 업로드
            </button>
            <button className="btn btn-primary" type="button">
              등록 확정
            </button>
          </div>
        </div>
        <div className="alert alert-info">CSV 업로드 결과 48건이 정상, 2건이 검증 실패입니다.</div>
        <table className="data-table">
          <thead>
            <tr>
              <th>행</th>
              <th>노선</th>
              <th>요일</th>
              <th>시작</th>
              <th>종료</th>
              <th>검증</th>
            </tr>
          </thead>
          <tbody>
            {[1, 2, 3, 4, 5].map((n) => (
              <tr key={n}>
                <td>{n}</td>
                <td>기흥역 ↔ 캠퍼스</td>
                <td>월</td>
                <td>{`0${6 + n}:00`}</td>
                <td>22:30</td>
                <td>
                  <StatusBadge tone={n === 4 ? 'red' : 'green'}>{n === 4 ? '실패' : '정상'}</StatusBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}

export function ScheduleSuspendPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>운행 중단 · 기상악화 처리</h3>
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label>
              대상 노선<span className="req">*</span>
            </label>
            <select className="select" defaultValue="all">
              <option value="all">전체 노선</option>
              <option>기흥역 ↔ 캠퍼스</option>
            </select>
          </div>
          <div className="field">
            <label>
              중단 사유<span className="req">*</span>
            </label>
            <select className="select" defaultValue="weather">
              <option value="weather">기상악화</option>
              <option value="accident">사고</option>
              <option value="event">행사</option>
            </select>
          </div>
          <div className="field">
            <label>시작 시각</label>
            <input className="input" type="datetime-local" />
          </div>
          <div className="field">
            <label>종료 시각</label>
            <input className="input" type="datetime-local" />
          </div>
        </div>
        <div className="field" style={{ marginTop: 12 }}>
          <label>안내 문구</label>
          <textarea className="textarea" defaultValue="기상악화로 인해 해당 시간대 운행이 일시 중단됩니다." />
        </div>
        <div className="toolbar" style={{ marginTop: 16, justifyContent: 'flex-end' }}>
          <button className="btn btn-outline" type="button">
            취소
          </button>
          <button className="btn btn-danger" type="button">
            운행 중단 처리
          </button>
        </div>
      </section>
    </div>
  )
}
