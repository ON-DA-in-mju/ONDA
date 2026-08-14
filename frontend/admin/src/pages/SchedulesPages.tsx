import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Bus, CalendarDays, Clock, Search, Users } from 'lucide-react'
import { WeekRangePicker } from '../components/WeekRangePicker'
import { SCHEDULE_ROUTE_OPTIONS, schedules } from '../data/mock'
import { fetchRouteCatalog } from '../lib/routesApi'
import { MJU_TIMETABLE_PACKS, type MjuRouteName } from '../data/mjuTimetable'
import { fetchAssignments, fetchAssignmentsInRange } from '../lib/assignmentsApi'
import { fetchUsers } from '../lib/api'
import { fetchSchedulesWithRoutes, routeMatchesFilter, type ScheduleWithRoute } from '../lib/seedMju'
import { routeRunsOnTerm } from '../lib/routeVariants'
import { isSupabaseConfigured } from '../lib/supabase'
import { semesterForDate } from '../lib/academicCalendar'
import { resolveAssignmentStatus } from '../lib/assignmentStatus'
import { todayDateKey } from '../types/assignment'
import type { Weekday } from '../types/database'
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
import { TodayOperationsPanel } from '../components/TodayOperationsPanel'
import { StatusBadge } from '../components/ui/Form'
import { ListPagination } from '../components/ui/ListPagination'
import type { TodayAssignment } from '../types/assignment'
import '../styles/figma-pages.css'

const JS_TO_WEEKDAY: Weekday[] = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

const LIST_PAGE_SIZE = 5
const LIST_PAGE_WINDOW = 10

function formatTime(t: string) {
  return t.slice(0, 5)
}

function timeToMinutes(value: string): number | null {
  const t = value.slice(0, 5)
  if (!/^\d{2}:\d{2}$/.test(t)) return null
  const [h, m] = t.split(':').map(Number)
  if (!Number.isFinite(h) || !Number.isFinite(m)) return null
  return h * 60 + m
}

/** 연속 출발 시각 평균 간격. 2회 미만이면 "-" */
function averageIntervalLabel(times: string[]): string {
  const mins = [...new Set(times.map((t) => timeToMinutes(t)).filter((n): n is number => n != null))].sort(
    (a, b) => a - b,
  )
  if (mins.length < 2) return '-'
  const gaps: number[] = []
  for (let i = 1; i < mins.length; i++) {
    const gap = mins[i] - mins[i - 1]
    if (gap > 0) gaps.push(gap)
  }
  if (!gaps.length) return '-'
  const avg = Math.round(gaps.reduce((a, b) => a + b, 0) / gaps.length)
  return avg > 0 ? `약 ${avg}분` : '-'
}

function isVisibleInTimetablePeriod(
  routeName: string | null | undefined,
  period: 'SEMESTER' | 'VACATION',
) {
  if (!routeName) return false
  return routeRunsOnTerm(routeName, period === 'VACATION' ? 'VACATION' : 'SEMESTER')
}

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
  const todayDefaults = () => {
    const now = new Date()
    return { weekStart: weekRangeFromDate(now).start, weekday: now.getDay() }
  }
  const initial = todayDefaults()

  /** 조회 폼에서만 바뀌는 선택값 (조회하기 전까지 목록/시간표에 미반영) */
  const [draftWeekStart, setDraftWeekStart] = useState(initial.weekStart)
  const [draftWeekday, setDraftWeekday] = useState(initial.weekday)
  /** 조회하기/초기화로 확정된 값 — 목록·공식 시간표·요약 기준 */
  const [weekStart, setWeekStart] = useState(initial.weekStart)
  const [weekday, setWeekday] = useState(initial.weekday)

  const draftWeekEnd = useMemo(() => addDays(draftWeekStart, 6), [draftWeekStart])
  const weekEnd = useMemo(() => addDays(weekStart, 6), [weekStart])
  const [routeFilter, setRouteFilter] = useState('')
  // 명지대역을 기본으로 — 기흥역은 방학 미운행이라 학기중만 보이기 쉬움
  const [timetableRoute, setTimetableRoute] = useState<string>('명지대역 셔틀')
  const [assignments, setAssignments] = useState<TodayAssignment[]>([])
  const [weekAssignments, setWeekAssignments] = useState<TodayAssignment[]>([])
  const [dbSchedules, setDbSchedules] = useState<ScheduleWithRoute[] | null>(null)
  const [dbRouteNames, setDbRouteNames] = useState<string[]>([])
  const [listPage, setListPage] = useState(1)
  const [weekPickerOpen, setWeekPickerOpen] = useState(false)

  const resolveSelectedDate = (start: Date, end: Date, day: number) => {
    if (day < 0) {
      const today = new Date()
      const todayKey = toDateKey(today)
      const startKey = toDateKey(start)
      const endKey = toDateKey(end)
      if (todayKey >= startKey && todayKey <= endKey) return today
      return start
    }
    return addDays(start, day)
  }

  const selectedDate = useMemo(
    () => resolveSelectedDate(weekStart, weekEnd, weekday),
    [weekStart, weekEnd, weekday],
  )
  const selectedDateKey = toDateKey(selectedDate)
  const weekLabel = formatWeekRange(weekStart, weekEnd)
  const selectedWeekday = JS_TO_WEEKDAY[weekday < 0 ? selectedDate.getDay() : weekday]
  const timetablePeriod = semesterForDate(selectedDateKey)

  const mjuPack = useMemo(() => {
    const route = timetableRoute as MjuRouteName
    if (route === '기흥역 통학버스') {
      return MJU_TIMETABLE_PACKS.find((p) => p.id === 'semester-giheung') ?? MJU_TIMETABLE_PACKS[0]
    }
    if (timetablePeriod === 'VACATION') {
      return MJU_TIMETABLE_PACKS.find((p) => p.id === 'weekend-vacation-city') ?? MJU_TIMETABLE_PACKS[0]
    }
    return MJU_TIMETABLE_PACKS.find((p) => p.id === 'semester-shuttle') ?? MJU_TIMETABLE_PACKS[0]
  }, [timetableRoute, timetablePeriod, selectedWeekday])

  const mjuTrips = useMemo(() => {
    if (timetablePeriod === 'VACATION' && timetableRoute !== '시내 셔틀') return []
    return mjuPack.trips.filter((t) => t.route === timetableRoute)
  }, [mjuPack, timetableRoute, timetablePeriod])

  const loadDbSchedules = useCallback(async () => {
    try {
      const rows = await fetchSchedulesWithRoutes()
      // null(조회 실패)도 빈 배열로 두어 ‘불러오는 중’에 멈추지 않게 함
      setDbSchedules(rows ?? [])
    } catch (e) {
      console.error('[schedules] load failed', e)
      setDbSchedules([])
    }
  }, [])

  useEffect(() => {
    void loadDbSchedules()
  }, [loadDbSchedules])

  useEffect(() => {
    let alive = true
    void fetchRouteCatalog().then((rows) => {
      if (!alive) return
      setDbRouteNames(rows.map((r) => r.name))
    })
    return () => {
      alive = false
    }
  }, [])

  const routeOptions = dbRouteNames.length ? dbRouteNames : [...SCHEDULE_ROUTE_OPTIONS]

  // 공식 시간표는 DB schedules만 사용. 자동 seed/동기화는 기존 데이터를 지울 수 있어 하지 않음.

  const dbTrips = useMemo(() => {
    if (!dbSchedules) return []
    return dbSchedules
      .filter(
        (s) =>
          routeMatchesFilter(s.routes?.route_name, timetableRoute) &&
          s.weekday === selectedWeekday &&
          s.semester === timetablePeriod &&
          isVisibleInTimetablePeriod(s.routes?.route_name, timetablePeriod),
      )
      .sort((a, b) => a.departure_time.localeCompare(b.departure_time))
  }, [dbSchedules, timetableRoute, selectedWeekday, timetablePeriod])

  const timetableLoading = isSupabaseConfigured && dbSchedules === null
  const timetableSourceLabel = isSupabaseConfigured
    ? dbSchedules
      ? `DB schedules · ${dbTrips.length}건`
      : 'DB 조회 중…'
    : '로컬 시간표'

  const dbRouteSummary = useMemo(() => {
    if (!dbSchedules) return null
    const forRoute = dbSchedules.filter(
      (s) =>
        routeMatchesFilter(s.routes?.route_name, timetableRoute) &&
        s.semester === timetablePeriod &&
        s.weekday === selectedWeekday &&
        isVisibleInTimetablePeriod(s.routes?.route_name, timetablePeriod),
    )
    if (!forRoute.length) return null
    const times = forRoute.map((s) => formatTime(s.departure_time)).sort()
    return { rounds: forRoute.length, start: times[0], end: times[times.length - 1] }
  }, [dbSchedules, timetableRoute, timetablePeriod, selectedWeekday])

  /** 운행 패턴 미리보기 — 조회 날짜에 실제 배차된 operations 시간대별 회차 (08–17시) */
  const patternPreview = useMemo(() => {
    const START_HOUR = 8
    const SLOT_COUNT = 10

    const items = routeFilter
      ? assignments.filter((a) => routeMatchesFilter(a.routeName, routeFilter))
      : assignments

    const sourceHours = items
      .map((a) => Number.parseInt((a.departTime || '').slice(0, 2), 10))
      .filter((h) => Number.isFinite(h))

    const slots = Array.from({ length: SLOT_COUNT }, (_, i) => {
      const hour = START_HOUR + i
      const count = sourceHours.filter((h) => h === hour).length
      return {
        hour,
        label: `${String(hour).padStart(2, '0')}`,
        count,
      }
    })

    const max = Math.max(0.001, ...slots.map((s) => s.count))

    return {
      slots: slots.map((s) => ({
        ...s,
        pct: Math.round((s.count / max) * 100),
      })),
      total: items.length,
      empty: items.length === 0,
      routeLabel: routeFilter || '전체 노선',
    }
  }, [assignments, routeFilter])

  const load = useCallback(async () => {
    const rows = await fetchAssignments({ date: selectedDateKey })
    setAssignments(rows)
  }, [selectedDateKey])

  const assignInFlightRef = useRef(false)

  useEffect(() => {
    let alive = true
    setAssignments([])
    const tick = async () => {
      if (assignInFlightRef.current) return
      assignInFlightRef.current = true
      try {
        if (!alive) return
        await load()
      } finally {
        assignInFlightRef.current = false
      }
    }
    void tick()
    const timer = window.setInterval(() => void tick(), 20_000)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [load])

  const listRows = useMemo(() => {
    const isToday = selectedDateKey === todayDateKey()
    return routeOptions
      .filter((route) => !routeFilter || route === routeFilter)
      .map((route, idx) => {
        const routeItems = assignments.filter((a) => routeMatchesFilter(a.routeName, route))
        const derived = statusFromAssignments(routeItems)
        const times = (dbSchedules ?? [])
          .filter(
            (s) =>
              s.routes?.route_name === route &&
              s.weekday === selectedWeekday &&
              s.semester === timetablePeriod,
          )
          .map((s) => formatTime(s.departure_time))
          .filter(Boolean)
          .sort()
        return {
          no: idx + 1,
          route,
          start: times[0] ?? '-',
          end: times.at(-1) ?? '-',
          interval: averageIntervalLabel(routeItems.map((a) => a.departTime)),
          rounds: derived.rounds,
          status: isToday ? derived.status : '-',
          tone: derived.tone,
        }
      })
  }, [assignments, routeFilter, routeOptions, dbSchedules, selectedWeekday, timetablePeriod, selectedDateKey])

  const listPageCount = Math.max(1, Math.ceil(listRows.length / LIST_PAGE_SIZE))
  const safeListPage = Math.min(listPage, listPageCount)
  const pagedListRows = useMemo(() => {
    const start = (safeListPage - 1) * LIST_PAGE_SIZE
    return listRows.slice(start, start + LIST_PAGE_SIZE)
  }, [listRows, safeListPage])

  useEffect(() => {
    setListPage(1)
  }, [routeFilter, selectedDateKey, weekday, weekStart])

  useEffect(() => {
    if (listPage > listPageCount) setListPage(listPageCount)
  }, [listPage, listPageCount])

  useEffect(() => {
    let alive = true
    const startKey = toDateKey(weekStart)
    const endKey = toDateKey(weekEnd)
    setWeekAssignments([])
    void fetchAssignmentsInRange(startKey, endKey).then((rows) => {
      if (!alive) return
      setWeekAssignments(rows.filter((row) => row.date >= startKey && row.date <= endKey))
    })
    return () => {
      alive = false
    }
  }, [weekStart, weekEnd])

  /** 주간 요약 KPI — 조회에서 고른 주간 실제 운행 기준 */
  const weekSummary = useMemo(() => {
    const rows = routeFilter
      ? weekAssignments.filter((a) => routeMatchesFilter(a.routeName, routeFilter))
      : weekAssignments
    const tripCount = rows.length
    const dayCount = 7
    const avgTrips = Math.round(tripCount / dayCount)
    const tripMinutes = (row: TodayAssignment) => {
      const [sh, sm] = (row.departTime || '00:00').split(':').map(Number)
      const [eh, em] = (row.expectedEndTime || '').split(':').map(Number)
      const start = (sh || 0) * 60 + (sm || 0)
      if (!row.expectedEndTime || !Number.isFinite(eh) || !Number.isFinite(em)) return 25
      const end = eh * 60 + em
      const diff = end - start
      return diff > 0 ? diff : 25
    }
    const totalMinutes = rows.reduce((sum, row) => sum + tripMinutes(row), 0)
    const avgMinutes = Math.round(totalMinutes / dayCount)
    const fmtDur = (mins: number) => {
      const h = Math.floor(mins / 60)
      const m = mins % 60
      return { h, m }
    }
    const vehicles = new Set(rows.map((r) => r.vehicleName).filter(Boolean))
    const passengers = tripCount * 40
    return {
      tripCount,
      avgTrips,
      totalDur: fmtDur(totalMinutes),
      avgDur: fmtDur(avgMinutes),
      vehicleCount: vehicles.size,
      passengers,
      avgPassengers: Math.round(passengers / dayCount),
    }
  }, [weekAssignments, routeFilter])

  useEffect(() => {
    if (!routeFilter) return
    if (routeOptions.includes(routeFilter)) {
      setTimetableRoute(routeFilter)
    }
  }, [routeFilter, routeOptions])

  const onPickWeekStart = (start: Date) => {
    setDraftWeekStart(start)
  }

  const onSearch = () => {
    setWeekStart(draftWeekStart)
    setWeekday(draftWeekday)
    setWeekPickerOpen(false)
    setListPage(1)
    const date = resolveSelectedDate(draftWeekStart, addDays(draftWeekStart, 6), draftWeekday)
    void fetchAssignments({ date: toDateKey(date) }).then(setAssignments)
  }

  const onReset = () => {
    const { weekStart: ws, weekday: wd } = todayDefaults()
    setDraftWeekStart(ws)
    setDraftWeekday(wd)
    setWeekStart(ws)
    setWeekday(wd)
    setRouteFilter('')
    setWeekPickerOpen(false)
    setListPage(1)
    const date = resolveSelectedDate(ws, addDays(ws, 6), wd)
    void fetchAssignments({ date: toDateKey(date) }).then(setAssignments)
  }

  return (
    <div className="page">
      <div className="sched-grid">
        {/* 1행: 조회 ↔ 요약 (Y 높이 일치) */}
        <section className="card card-pad sched-card sched-filter-card">
          <div className="card-head">
            <h3>운행 일정 조회</h3>
          </div>
          <div className="sched-filter-form">
            <div className="sched-filter-row sched-filter-row-top">
              <div className="sched-filter-field sched-filter-route">
                <span className="sched-filter-label">노선</span>
                <select
                  className="select"
                  value={routeFilter}
                  onChange={(e) => setRouteFilter(e.target.value)}
                >
                  <option value="">노선 전체</option>
                  {routeOptions.map((name) => (
                    <option key={name} value={name}>
                      {name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="sched-filter-field sched-filter-days">
                <span className="sched-filter-label">요일</span>
                <div className="sched-day-group">
                  {/* 월~토 → 일 */}
                  {[1, 2, 3, 4, 5, 6, 0].map((i) => (
                    <button
                      key={WEEKDAY_LABELS[i]}
                      type="button"
                      className={`sched-day-chip${draftWeekday === i ? ' active' : ''}`}
                      onClick={() => setDraftWeekday(i)}
                    >
                      {WEEKDAY_LABELS[i]}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="sched-filter-row sched-filter-row-bottom">
              <div className="sched-filter-field sched-filter-period">
                <span className="sched-filter-label">기간</span>
                <div className="sched-period-wrap">
                  <div className="sched-period-box">
                    <div className="sched-period-part">
                      <button
                        type="button"
                        onClick={() => setWeekPickerOpen((v) => !v)}
                        aria-expanded={weekPickerOpen}
                        aria-label="기간 시작일"
                      >
                        {formatDotDate(draftWeekStart)}
                      </button>
                      <CalendarDays
                        size={16}
                        className="cal-icon"
                        onClick={() => setWeekPickerOpen((v) => !v)}
                      />
                    </div>
                    <span className="sched-period-sep">~</span>
                    <div className="sched-period-part">
                      <button
                        type="button"
                        onClick={() => setWeekPickerOpen((v) => !v)}
                        aria-label="기간 종료일"
                      >
                        {formatDotDate(draftWeekEnd)}
                      </button>
                      <CalendarDays
                        size={16}
                        className="cal-icon"
                        onClick={() => setWeekPickerOpen((v) => !v)}
                      />
                    </div>
                  </div>
                  <WeekRangePicker
                    weekStart={draftWeekStart}
                    open={weekPickerOpen}
                    onClose={() => setWeekPickerOpen(false)}
                    onPick={onPickWeekStart}
                  />
                </div>
              </div>

              <div className="sched-filter-actions">
                <button className="btn btn-primary" type="button" onClick={onSearch}>
                  <Search size={15} style={{ marginRight: 6 }} />
                  조회하기
                </button>
                <button className="btn btn-outline" type="button" onClick={onReset}>
                  초기화
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className="card card-pad sched-card sched-summary-card">
          <div className="card-head">
            <h3>
              운행 일정 요약{' '}
              <span className="muted sched-summary-range">({weekLabel})</span>
            </h3>
          </div>
          <div className="sched-summary-kpis">
            <div className="sched-summary-kpi">
              <div className="sched-summary-kpi-head">
                <div className="icon blue">
                  <CalendarDays size={23} strokeWidth={3} />
                </div>
                <div className="label">총 운행 횟수</div>
              </div>
              <div className="value">
                {weekSummary.tripCount.toLocaleString()}
                <em>회</em>
              </div>
              <div className="hint">일 평균 {weekSummary.avgTrips.toLocaleString()}회</div>
            </div>
            <div className="sched-summary-kpi">
              <div className="sched-summary-kpi-head">
                <div className="icon green">
                  <Clock size={23} strokeWidth={3} />
                </div>
                <div className="label">총 운행 시간</div>
              </div>
              <div className="value">
                {weekSummary.totalDur.h}
                <em>시간</em> {String(weekSummary.totalDur.m).padStart(2, '0')}
                <em>분</em>
              </div>
              <div className="hint">
                일 평균 {weekSummary.avgDur.h}시간 {String(weekSummary.avgDur.m).padStart(2, '0')}분
              </div>
            </div>
            <div className="sched-summary-kpi">
              <div className="sched-summary-kpi-head">
                <div className="icon orange">
                  <Bus size={23} strokeWidth={3} />
                </div>
                <div className="label">총 운행 차량</div>
              </div>
              <div className="value">
                {weekSummary.vehicleCount}
                <em>대</em>
              </div>
              <div className="hint">투입 차량 기준</div>
            </div>
            <div className="sched-summary-kpi">
              <div className="sched-summary-kpi-head">
                <div className="icon purple">
                  <Users size={23} strokeWidth={3} />
                </div>
                <div className="label">예상 탑승 인원</div>
              </div>
              <div className="value">
                {weekSummary.passengers.toLocaleString()}
                <em>명</em>
              </div>
              <div className="hint">일 평균 {weekSummary.avgPassengers.toLocaleString()}명</div>
            </div>
          </div>
        </section>

        {/* 2행: 목록 ↔ 공식 시간표 (Y 높이 일치) */}
        <section className="card card-pad sched-card sched-list-card">
          <div className="card-head">
            <h3>운행 일정 목록</h3>
            <div className="toolbar" style={{ gap: 8 }}>
              <Link
                className="btn btn-outline"
                to={`/schedules/operations?date=${selectedDateKey}`}
                style={{ height: 30 }}
              >
                운행 관리
              </Link>
              <Link
                className="btn btn-primary"
                to={`/schedules/assignments?date=${selectedDateKey}`}
                style={{ height: 30 }}
              >
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
              {pagedListRows.map((row) => (
                <tr key={row.route}>
                  <td>{row.no}</td>
                  <td>{row.route}</td>
                  <td>{row.start}</td>
                  <td>{row.end}</td>
                  <td>{row.interval}</td>
                  <td>{row.rounds}</td>
                  <td>
                    {row.status === '-' ? '-' : <StatusBadge tone={row.tone}>{row.status}</StatusBadge>}
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
          <ListPagination
            total={listRows.length}
            page={safeListPage}
            pageSize={LIST_PAGE_SIZE}
            onPageChange={setListPage}
            ariaLabel="시간표 목록 페이지"
            windowSize={LIST_PAGE_WINDOW}
          />
        </section>

        <section className="card card-pad sched-card sched-timetable-card">
          <div className="card-head sched-timetable-head">
            <div className="sched-timetable-title-row">
              <h3>공식 시간표</h3>
              <select
                className="select sched-timetable-route-select"
                value={routeOptions.includes(timetableRoute) ? timetableRoute : routeOptions[0] ?? ''}
                onChange={(e) => {
                  setTimetableRoute(e.target.value)
                }}
                aria-label="상세 시간표 노선"
              >
                {routeOptions.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <p className="muted" style={{ fontSize: 12, marginTop: 0 }}>
            {formatDotDate(selectedDate)} · {WEEKDAY_LABELS[selectedDate.getDay()]}요일 ·{' '}
            {timetablePeriod === 'VACATION' ? '방학' : '학기 중'} · {timetableRoute}
            {dbRouteSummary
              ? ` · ${dbRouteSummary.rounds}회 · ${dbRouteSummary.start}~${dbRouteSummary.end}`
              : timetableLoading
                ? ' · 불러오는 중…'
                : timetableRoute === '기흥역 통학버스' && timetablePeriod === 'VACATION'
                  ? ' · 기흥역은 방학·계절학기 미운행'
                  : timetableRoute === '명지대역 셔틀' && timetablePeriod === 'VACATION'
                    ? ' · 명지대역 셔틀은 학기 중 평일만 운행'
                    : ' · 해당 요일 일정 없음'}
            {' · '}
            {timetableSourceLabel}
          </p>
          <div className="sched-timetable-scroll">
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>#</th>
                  <th>노선</th>
                  <th>출발</th>
                  <th>요일</th>
                  <th>구분</th>
                </tr>
              </thead>
              <tbody>
                {timetableLoading ? (
                  <tr>
                    <td colSpan={5} className="muted">
                      schedules 불러오는 중…
                    </td>
                  </tr>
                ) : null}
                {!timetableLoading &&
                  (isSupabaseConfigured
                    ? dbTrips
                    : dbTrips.length
                      ? dbTrips
                      : mjuTrips.map((t, i) => ({
                          id: `local-${i}`,
                          departure_time: `${t.departure}:00`,
                          weekday: selectedWeekday,
                          semester: timetablePeriod,
                          routes: { route_name: t.route },
                        }))
                  ).map((row, idx) => (
                    <tr key={'id' in row && row.id ? String(row.id) : `row-${idx}`}>
                      <td>{idx + 1}</td>
                      <td>{('routes' in row && row.routes?.route_name) || timetableRoute}</td>
                      <td style={{ fontWeight: 700 }}>
                        {'departure_time' in row ? formatTime(String(row.departure_time)) : '-'}
                      </td>
                      <td>{'weekday' in row ? String(row.weekday) : selectedWeekday}</td>
                      <td>
                        {('semester' in row ? String(row.semester) : timetablePeriod) === 'VACATION'
                          ? '방학·계절학기'
                          : '학기 중'}
                      </td>
                    </tr>
                  ))}
                {!timetableLoading &&
                (isSupabaseConfigured ? dbTrips.length === 0 : !dbTrips.length && !mjuTrips.length) ? (
                  <tr>
                    <td colSpan={5} className="muted">
                      {timetableRoute === '기흥역 통학버스' && timetablePeriod === 'VACATION'
                        ? '기흥역 통학버스는 방학·계절학기·주말에 운행하지 않습니다.'
                        : timetableRoute === '명지대역 셔틀' && timetablePeriod === 'VACATION'
                          ? '명지대역 셔틀은 학기 중 평일에만 운행합니다.'
                          : '해당 조건의 schedules가 없습니다. 조회 날짜·노선을 바꿔 보세요.'}
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <section className="card card-pad sched-card sched-pattern-card">
          <div className="card-head">
            <h3>운행 패턴 미리보기</h3>
            <span className="muted" style={{ fontSize: 12 }}>
              {patternPreview.routeLabel} · {formatDotDate(selectedDate)} · {WEEKDAY_LABELS[selectedDate.getDay()]}요일
              · 실제 배차 · 08–17시
            </span>
          </div>
          {patternPreview.empty ? (
            <p className="muted" style={{ margin: 'auto 0', fontSize: 13 }}>
              선택한 날짜에 배차된 운행이 없습니다.
            </p>
          ) : (
            <>
              <div
                className="pattern-bars"
                role="img"
                aria-label={`${patternPreview.routeLabel} ${formatDotDate(selectedDate)} 시간대별 배차 회차`}
              >
                {patternPreview.slots.map((slot) => (
                  <div key={slot.hour} className="pattern-col" title={`${slot.label}시 · ${slot.count}회`}>
                    <span
                      className={`bar bar-a${slot.pct === 0 ? ' is-empty' : ''}`}
                      style={{ height: `${Math.max(slot.pct, slot.pct === 0 ? 3 : 8)}%` }}
                    />
                  </div>
                ))}
              </div>
              <div className="pattern-hour-labels" aria-hidden>
                {patternPreview.slots.map((slot) => (
                  <span key={slot.hour}>{slot.label}</span>
                ))}
              </div>
            </>
          )}
          <div className="legend-row">
            <span>
              <i style={{ background: '#266ef4' }} /> 선택일
              {!patternPreview.empty ? ` ${patternPreview.total}회` : ''}
            </span>
          </div>
        </section>
      </div>
    </div>
  )
}

/** 운행 관리 — 선택한 날짜의 operations 생성 (기사 미배정) */
export function ScheduleOperationsPage() {
  return (
    <div className="page">
      <div className="toolbar" style={{ marginBottom: 4 }}>
        <Link className="btn btn-ghost" to="/schedules" style={{ height: 30 }}>
          ← 운행 일정 목록
        </Link>
      </div>
      <TodayOperationsPanel />
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
  const weekdayParam = Number(params.get('weekday') ?? new Date().getDay())
  const weekdayFromDate = parseDateKey(date).getDay()
  const weekdayNum = weekdayParam >= 0 && weekdayParam <= 6 ? weekdayParam : weekdayFromDate
  const weekdayLabel = WEEKDAY_LABELS[weekdayNum] ?? '일'
  const selectedWeekday = JS_TO_WEEKDAY[weekdayNum]
  const semester = semesterForDate(date)

  const template = schedules.find((s) => s.route === routeName) ?? schedules[0]
  const [routeAssignments, setRouteAssignments] = useState<TodayAssignment[]>([])
  const [dbTimes, setDbTimes] = useState<string[]>([])
  const [selectedOpId, setSelectedOpId] = useState<string | null>(null)
  const [driversByKey, setDriversByKey] = useState<Map<string, { name: string; phone: string; email: string }>>(
    () => new Map(),
  )

  useEffect(() => {
    let alive = true
    const loadDetail = async () => {
      const rows = await fetchAssignments({ date })
      if (!alive) return
      setRouteAssignments(rows.filter((a) => routeMatchesFilter(a.routeName, routeName)))
    }
    void loadDetail()
    const timer = window.setInterval(() => void loadDetail(), 5_000)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [date, routeName])

  useEffect(() => {
    setSelectedOpId(null)
  }, [date, routeName])

  useEffect(() => {
    let alive = true
    void fetchSchedulesWithRoutes({ weekday: selectedWeekday, semester, routeName }).then((rows) => {
      if (!alive) return
      const times = [...new Set((rows ?? []).map((s) => formatTime(s.departure_time)).filter(Boolean))].sort()
      setDbTimes(times)
    })
    return () => {
      alive = false
    }
  }, [selectedWeekday, semester, routeName])

  useEffect(() => {
    let alive = true
    void fetchUsers().then((rows) => {
      if (!alive) return
      const map = new Map<string, { name: string; phone: string; email: string }>()
      for (const u of rows ?? []) {
        if (u.role !== 'DRIVER') continue
        const info = {
          name: u.name?.trim() || '-',
          phone: u.phone?.trim() || '-',
          email: u.email?.trim() || '-',
        }
        map.set(u.id, info)
        const login = (u.login_id || u.email?.split('@')[0] || '').trim()
        if (login) map.set(login, info)
      }
      setDriversByKey(map)
    })
    return () => {
      alive = false
    }
  }, [])

  const start = dbTimes[0] ?? template.start
  const end = dbTimes.at(-1) ?? template.end
  const interval = averageIntervalLabel(routeAssignments.map((a) => a.departTime))
  const rounds = routeAssignments.length
  const selectedOp = routeAssignments.find((row) => row.id === selectedOpId) ?? null
  const selectedDriver = selectedOp?.driverId ? driversByKey.get(selectedOp.driverId) : null

  useEffect(() => {
    if (selectedOpId && !routeAssignments.some((row) => row.id === selectedOpId)) {
      setSelectedOpId(null)
    }
  }, [routeAssignments, selectedOpId])

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
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(5, minmax(0, 1fr))',
            gap: 12,
          }}
        >
          {[
            ['노선', routeName],
            ['운행 요일', `${weekdayLabel}요일`],
            ['운행 시간', `${start} ~ ${end}`],
            ['배차 간격', interval],
            ['총 회차', `${rounds}회`],
          ].map(([k, v]) => (
            <div key={k} className="card card-pad" style={{ boxShadow: 'none' }}>
              <div className="muted" style={{ fontSize: 12 }}>
                {k}
              </div>
              <div style={{ fontWeight: 700 }}>{v}</div>
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
                  해당 일자·노선에 운행이 없습니다. 운행 관리에서 생성해 주세요.
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
                  <tr
                    key={row.id}
                    className={selectedOpId === row.id ? 'is-selected' : undefined}
                    onClick={() => setSelectedOpId((prev) => (prev === row.id ? null : row.id))}
                    style={{
                      cursor: 'pointer',
                      background: selectedOpId === row.id ? '#f0f6ff' : undefined,
                    }}
                  >
                    <td>
                      {row.driverName || '---'}
                      {row.driverId ? (
                        <div className="muted" style={{ fontSize: 10 }}>
                          {row.driverId}
                        </div>
                      ) : null}
                    </td>
                    <td>{row.vehicleName || '—'}</td>
                    <td>{row.departTime}</td>
                    <td>{row.expectedEndTime || '—'}</td>
                    <td>
                      {row.origin && row.destination ? `${row.origin} → ${row.destination}` : '—'}
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
            {!selectedOp ? (
              <tr>
                <td>담당 기사</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
              </tr>
            ) : selectedDriver ? (
              <tr>
                <td>담당 기사</td>
                <td>{selectedDriver.name}</td>
                <td>{selectedDriver.phone}</td>
                <td>
                  {[selectedOp.vehicleName, selectedOp.departTime].filter(Boolean).join(' · ') || '-'}
                </td>
              </tr>
            ) : (
              <tr>
                <td>담당 기사</td>
                <td>{selectedOp.driverName || '-'}</td>
                <td>-</td>
                <td>기사 미배정</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  )
}

export function ScheduleSuspendPage() {
  const [routes, setRoutes] = useState<{ id: string; name: string }[]>([])
  const [selectedRouteId, setSelectedRouteId] = useState('-')
  const [reason, setReason] = useState('weather')
  const [message, setMessage] = useState('기상악화로 인해 해당 운행이 일시 중단됩니다.')
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState<string | null>(null)
  const [loadingOptions, setLoadingOptions] = useState(true)

  const reasonLabel =
    reason === 'accident' ? '사고' : reason === 'event' ? '행사' : '기상악화'

  const loadOptions = useCallback(async () => {
    const { fetchRouteOptions, ALL_ROUTES } = await import('../lib/forceSuspendApi')
    const routeRows = await fetchRouteOptions()
    setRoutes(routeRows)
    setLoadingOptions(false)
    setSelectedRouteId((prev) => {
      if (routeRows.length === 0) return '-'
      if (prev === ALL_ROUTES) return ALL_ROUTES
      if (prev !== '-' && routeRows.some((r) => r.id === prev)) return prev
      return routeRows[0]?.id ?? '-'
    })
  }, [])

  useEffect(() => {
    void loadOptions()
  }, [loadOptions])

  const onSubmit = async () => {
    if (selectedRouteId === '-') {
      setFeedback('노선을 선택해 주세요.')
      return
    }
    if (!startAt || !endAt) {
      setFeedback('시작 시각과 종료 시각을 입력해 주세요.')
      return
    }
    setSubmitting(true)
    setFeedback(null)
    const reasonText = `${reasonLabel}: ${message}`
    const api = await import('../lib/forceSuspendApi')
    const result = await api.suspendOperationsInRange({
      routeId: selectedRouteId,
      startIso: startAt,
      endIso: endAt,
      reason: reasonText,
    })
    setSubmitting(false)
    if (!result.ok) {
      setFeedback(result.message || '중단 처리에 실패했습니다.')
      return
    }
    setFeedback(result.message || '운행 중단 처리를 반영했습니다.')
  }

  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>운행 중단 · 기상악화 처리</h3>
        </div>
        <p className="muted" style={{ marginTop: 0, marginBottom: 14, fontSize: 13 }}>
          선택한 노선의 예정·이미 배정된 배차를 운행 불가로 바꾸고, 노선 관리에도 운행 불가로 표시합니다. 현재 운행
          중인 버스는 중단하지 않습니다.
        </p>
        <div className="grid grid-2">
          <div className="field">
            <label>
              노선<span className="req">*</span>
            </label>
            <select
              className="select suspend-select"
              value={selectedRouteId}
              onChange={(e) => setSelectedRouteId(e.target.value)}
              disabled={loadingOptions || routes.length === 0}
            >
              {routes.length === 0 ? (
                <option value="-">-</option>
              ) : (
                <>
                  <option value="__all_routes__">전체 노선</option>
                  {routes.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name}
                    </option>
                  ))}
                </>
              )}
            </select>
          </div>
          <div className="field">
            <label>
              중단 사유<span className="req">*</span>
            </label>
            <select className="select suspend-select" value={reason} onChange={(e) => setReason(e.target.value)}>
              <option value="weather">기상악화</option>
              <option value="accident">사고</option>
              <option value="event">행사</option>
            </select>
          </div>
          <div className="field">
            <label>
              시작 시각<span className="req">*</span>
            </label>
            <input
              className="input"
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label>
              종료 시각<span className="req">*</span>
            </label>
            <input
              className="input"
              type="datetime-local"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
              required
            />
          </div>
        </div>
        <div className="field" style={{ marginTop: 12 }}>
          <label>안내 문구</label>
          <textarea className="textarea" value={message} onChange={(e) => setMessage(e.target.value)} />
        </div>
        {feedback ? (
          <p className="muted" style={{ marginTop: 12, fontSize: 13 }}>
            {feedback}
          </p>
        ) : null}
        <div className="toolbar" style={{ marginTop: 16, justifyContent: 'flex-end' }}>
          <button className="btn btn-outline" type="button" onClick={() => setFeedback(null)}>
            취소
          </button>
          <button
            className="btn btn-danger"
            type="button"
            disabled={submitting || selectedRouteId === '-' || !startAt || !endAt}
            onClick={() => void onSubmit()}
          >
            {submitting ? '처리 중…' : '운행 중단 처리'}
          </button>
        </div>
      </section>
    </div>
  )
}
