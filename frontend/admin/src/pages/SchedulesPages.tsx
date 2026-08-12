import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Bus, CalendarDays, Clock, Download, Plus, Search, Users } from 'lucide-react'
import { WeekRangePicker } from '../components/WeekRangePicker'
import { SCHEDULE_ROUTE_OPTIONS, drivers, schedules } from '../data/mock'
import {
  MJU_TIMETABLE_PACKS,
  type MjuRouteName,
} from '../data/mjuTimetable'
import { fetchAssignments } from '../lib/assignmentsApi'
import { fetchSchedulesWithRoutes, routeMatchesFilter, type ScheduleWithRoute } from '../lib/seedMju'
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
  const [timetablePeriod, setTimetablePeriod] = useState<'SEMESTER' | 'VACATION'>(() =>
    semesterForDate(new Date()),
  )
  const [assignments, setAssignments] = useState<TodayAssignment[]>([])
  const [dbSchedules, setDbSchedules] = useState<ScheduleWithRoute[] | null>(null)
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

  const mjuPack = useMemo(() => {
    const route = timetableRoute as MjuRouteName
    if (route === '기흥역 통학버스') {
      return MJU_TIMETABLE_PACKS.find((p) => p.id === 'semester-giheung') ?? MJU_TIMETABLE_PACKS[0]
    }
    if (timetablePeriod === 'VACATION') {
      if (route === '시내 셔틀' && (selectedWeekday === 'SAT' || selectedWeekday === 'SUN')) {
        return MJU_TIMETABLE_PACKS.find((p) => p.id === 'weekend-vacation-city') ?? MJU_TIMETABLE_PACKS[0]
      }
      return MJU_TIMETABLE_PACKS.find((p) => p.id === 'seasonal-shuttle') ?? MJU_TIMETABLE_PACKS[0]
    }
    return MJU_TIMETABLE_PACKS.find((p) => p.id === 'semester-shuttle') ?? MJU_TIMETABLE_PACKS[0]
  }, [timetableRoute, timetablePeriod, selectedWeekday])

  const mjuTrips = useMemo(() => {
    return mjuPack.trips.filter((t) => t.route === timetableRoute)
  }, [mjuPack, timetableRoute])

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

  /** 선택일이 방학/학기면 공식 시간표 기본 탭도 맞춤 */
  useEffect(() => {
    setTimetablePeriod(semesterForDate(selectedDateKey))
  }, [selectedDateKey])

  // 공식 시간표는 DB schedules만 사용. 자동 seed/동기화는 기존 데이터를 지울 수 있어 하지 않음.

  const dbTrips = useMemo(() => {
    if (!dbSchedules) return []
    return dbSchedules
      .filter(
        (s) =>
          routeMatchesFilter(s.routes?.route_name, timetableRoute) &&
          s.weekday === selectedWeekday &&
          s.semester === timetablePeriod,
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
        s.weekday === selectedWeekday,
    )
    if (!forRoute.length) return null
    const times = forRoute.map((s) => formatTime(s.departure_time)).sort()
    return { rounds: forRoute.length, start: times[0], end: times[times.length - 1] }
  }, [dbSchedules, timetableRoute, timetablePeriod, selectedWeekday])

  /** 운행 패턴 미리보기 — schedules 시간대별 평일/주말 회차 (08~17시) */
  const patternPreview = useMemo(() => {
    const WEEKDAY_SET = new Set<Weekday>(['MON', 'TUE', 'WED', 'THU', 'FRI'])
    const WEEKEND_SET = new Set<Weekday>(['SAT', 'SUN'])
    const START_HOUR = 8
    const SLOT_COUNT = 10

    const rows =
      dbSchedules?.filter(
        (s) => routeMatchesFilter(s.routes?.route_name, timetableRoute) && s.semester === timetablePeriod,
      ) ?? []

    // DB 없으면 로컬 시간표 팩으로 폴백 (요일별 동일 시각 = 평일 패턴만)
    const source: { hour: number; weekday: Weekday }[] = rows.length
      ? rows.map((s) => ({
          hour: Number.parseInt(String(s.departure_time).slice(0, 2), 10) || 0,
          weekday: s.weekday,
        }))
      : mjuTrips.flatMap((t) => {
          const hour = Number.parseInt(t.departure.slice(0, 2), 10) || 0
          const days: Weekday[] =
            timetableRoute === '시내 셔틀' && timetablePeriod === 'VACATION'
              ? ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']
              : ['MON', 'TUE', 'WED', 'THU', 'FRI']
          return days.map((weekday) => ({ hour, weekday }))
        })

    const slots = Array.from({ length: SLOT_COUNT }, (_, i) => {
      const hour = START_HOUR + i
      const atHour = source.filter((s) => s.hour === hour)
      const weekdayCount = atHour.filter((s) => WEEKDAY_SET.has(s.weekday)).length
      const weekendCount = atHour.filter((s) => WEEKEND_SET.has(s.weekday)).length
      // 요일당 평균 회차 (막대 높이 비교용)
      const weekdayAvg = weekdayCount / 5
      const weekendAvg = weekendCount / 2
      return {
        hour,
        label: `${String(hour).padStart(2, '0')}`,
        weekdayAvg,
        weekendAvg,
        weekdayCount,
        weekendCount,
      }
    })

    const max = Math.max(0.001, ...slots.flatMap((s) => [s.weekdayAvg, s.weekendAvg]))
    const weekdayTrips = slots.reduce((n, s) => n + s.weekdayCount, 0)
    const weekendTrips = slots.reduce((n, s) => n + s.weekendCount, 0)
    const empty = weekdayTrips === 0 && weekendTrips === 0

    return {
      slots: slots.map((s) => ({
        ...s,
        weekdayPct: Math.round((s.weekdayAvg / max) * 100),
        weekendPct: Math.round((s.weekendAvg / max) * 100),
      })),
      weekdayTrips,
      weekendTrips,
      empty,
      fromDb: rows.length > 0,
    }
  }, [dbSchedules, timetableRoute, timetablePeriod, mjuTrips])

  const load = useCallback(async () => {
    const rows = await fetchAssignments({ date: selectedDateKey })
    setAssignments(rows)
  }, [selectedDateKey])

  const assignInFlightRef = useRef(false)

  useEffect(() => {
    let alive = true
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
    return schedules
      .filter((row) => !routeFilter || row.route === routeFilter)
      .map((row) => {
        const routeItems = assignments.filter((a) => a.routeName === row.route)
        const derived = statusFromAssignments(routeItems)
        const dbCount =
          dbSchedules?.filter(
            (s) =>
              s.routes?.route_name === row.route &&
              s.weekday === selectedWeekday &&
              s.semester === timetablePeriod,
          ).length ?? 0
        return {
          ...row,
          ...derived,
          rounds: derived.rounds || dbCount || row.rounds,
          start:
            dbSchedules
              ?.filter((s) => s.routes?.route_name === row.route && s.weekday === selectedWeekday && s.semester === timetablePeriod)
              .map((s) => formatTime(s.departure_time))
              .sort()[0] ?? row.start,
          end:
            [...(dbSchedules ?? [])]
              .filter((s) => s.routes?.route_name === row.route && s.weekday === selectedWeekday && s.semester === timetablePeriod)
              .map((s) => formatTime(s.departure_time))
              .sort()
              .at(-1) ?? row.end,
        }
      })
  }, [assignments, routeFilter, dbSchedules, selectedWeekday, timetablePeriod])

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

  /** 주간 요약 KPI (Figma: 기간 전체 기준) */
  const weekSummary = useMemo(() => {
    const routes = routeFilter ? [routeFilter] : [...SCHEDULE_ROUTE_OPTIONS]
    let tripCount = 0
    if (dbSchedules?.length) {
      for (let d = 0; d < 7; d++) {
        const wd = JS_TO_WEEKDAY[d]
        for (const route of routes) {
          tripCount += dbSchedules.filter(
            (s) =>
              s.routes?.route_name === route &&
              s.weekday === wd &&
              s.semester === timetablePeriod,
          ).length
        }
      }
    } else {
      tripCount = listRows.reduce((sum, row) => sum + row.rounds, 0) * 7
    }
    const dayCount = 7
    const avgTrips = Math.round(tripCount / dayCount)
    const totalMinutes = tripCount * 25
    const avgMinutes = Math.round(totalMinutes / dayCount)
    const fmtDur = (mins: number) => {
      const h = Math.floor(mins / 60)
      const m = mins % 60
      return { h, m }
    }
    const totalDur = fmtDur(totalMinutes)
    const avgDur = fmtDur(avgMinutes)
    const vehicleCount = 8
    const passengers = tripCount * 40
    const avgPassengers = Math.round(passengers / dayCount)
    return {
      tripCount,
      avgTrips,
      totalDur,
      avgDur,
      vehicleCount,
      passengers,
      avgPassengers,
    }
  }, [dbSchedules, routeFilter, timetablePeriod, listRows])

  useEffect(() => {
    if (!routeFilter) return
    setTimetableRoute(routeFilter)
  }, [routeFilter])

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
    setTimetablePeriod(semesterForDate(toDateKey(date)))
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
    setTimetablePeriod(semesterForDate(toDateKey(date)))
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
                  {SCHEDULE_ROUTE_OPTIONS.map((name) => (
                    <option key={name} value={name}>
                      {name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="sched-filter-field sched-filter-days">
                <span className="sched-filter-label">요일</span>
                <div className="sched-day-group">
                  <button
                    type="button"
                    className={`sched-day-chip wide${draftWeekday < 0 ? ' active' : ''}`}
                    onClick={() => setDraftWeekday(-1)}
                  >
                    전체
                  </button>
                  {/* Figma 순서: 월~토 → 일 */}
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
            <div className="toolbar">
              <button className="btn btn-outline" type="button">
                <Download size={15} style={{ marginRight: 6 }} />
                엑셀 다운로드
              </button>
              <Link className="btn btn-primary" to="/schedules/bulk">
                <Plus size={15} style={{ marginRight: 4 }} />
                운행 일정 생성
              </Link>
            </div>
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
              {pagedListRows.map((row) => (
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
                value={timetableRoute}
                onChange={(e) => {
                  setTimetableRoute(e.target.value)
                }}
                aria-label="상세 시간표 노선"
              >
                {SCHEDULE_ROUTE_OPTIONS.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
            </div>
            <div className="toolbar" style={{ gap: 6 }}>
              <button
                type="button"
                className={`btn btn-xs ${timetablePeriod === 'SEMESTER' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setTimetablePeriod('SEMESTER')}
              >
                학기 중
              </button>
              <button
                type="button"
                className={`btn btn-xs ${timetablePeriod === 'VACATION' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setTimetablePeriod('VACATION')}
              >
                방학·계절학기
              </button>
            </div>
          </div>
          <p className="muted" style={{ fontSize: 12, marginTop: 0 }}>
            {timetablePeriod === 'SEMESTER' ? '학기 중' : '방학·계절학기'} · {WEEKDAY_LABELS[weekday]}요일 ·{' '}
            {timetableRoute}
            {dbRouteSummary
              ? ` · ${dbRouteSummary.rounds}회 · ${dbRouteSummary.start}~${dbRouteSummary.end}`
              : timetableLoading
                ? ' · 불러오는 중…'
                : timetableRoute === '기흥역 통학버스' && timetablePeriod === 'VACATION'
                  ? ' · 기흥역은 방학·계절학기 미운행'
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
                        : '해당 조건의 schedules가 없습니다. 노선·학기중/방학·요일을 바꿔 보세요.'}
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        {/* 3행: 예외 ↔ 패턴 미리보기 (아래변 일치) */}
        <section className="card card-pad sched-card sched-exception-card">
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

        <section className="card card-pad sched-card sched-pattern-card">
          <div className="card-head">
            <h3>운행 패턴 미리보기</h3>
            <span className="muted" style={{ fontSize: 12 }}>
              {timetableRoute} · {timetablePeriod === 'VACATION' ? '방학' : '학기중'} · 08–17시
              {patternPreview.fromDb ? '' : ' (로컬)'}
            </span>
          </div>
          {patternPreview.empty ? (
            <p className="muted" style={{ margin: 'auto 0', fontSize: 13 }}>
              선택한 노선·학기에 해당하는 schedules가 없습니다.
            </p>
          ) : (
            <>
              <div
                className="pattern-bars"
                role="img"
                aria-label={`${timetableRoute} 시간대별 평일·주말 운행 회차`}
              >
                {patternPreview.slots.map((slot) => (
                  <div key={slot.hour} className="pattern-col" title={`${slot.label}시 · 평일 ${slot.weekdayAvg.toFixed(1)} · 주말 ${slot.weekendAvg.toFixed(1)}`}>
                    <span
                      className={`bar bar-a${slot.weekdayPct === 0 ? ' is-empty' : ''}`}
                      style={{ height: `${Math.max(slot.weekdayPct, slot.weekdayPct === 0 ? 3 : 8)}%` }}
                    />
                    <span
                      className={`bar bar-b${slot.weekendPct === 0 ? ' is-empty' : ''}`}
                      style={{ height: `${Math.max(slot.weekendPct, slot.weekendPct === 0 ? 3 : 8)}%` }}
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
              <i style={{ background: '#266ef4' }} /> 평일
              {!patternPreview.empty ? ` ${patternPreview.weekdayTrips}회` : ''}
            </span>
            <span>
              <i style={{ background: '#3fb46a' }} /> 주말
              {!patternPreview.empty ? ` ${patternPreview.weekendTrips}회` : ''}
            </span>
          </div>
        </section>
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
  const [buses, setBuses] = useState<{ busId: string; label: string }[]>([])
  const [selectedBusId, setSelectedBusId] = useState('-')
  const [reason, setReason] = useState('weather')
  const [message, setMessage] = useState('기상악화로 인해 해당 운행이 일시 중단됩니다.')
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState<string | null>(null)
  const [loadingBuses, setLoadingBuses] = useState(true)

  const reasonLabel =
    reason === 'accident' ? '사고' : reason === 'event' ? '행사' : '기상악화'

  const loadBuses = useCallback(async () => {
    const { fetchBusOptions, ALL_BUSES } = await import('../lib/forceSuspendApi')
    const rows = await fetchBusOptions()
    setBuses(rows.map((r) => ({ busId: r.busId, label: r.label })))
    setLoadingBuses(false)
    setSelectedBusId((prev) => {
      if (rows.length === 0) return '-'
      if (prev === ALL_BUSES) return ALL_BUSES
      if (prev !== '-' && rows.some((r) => r.busId === prev)) return prev
      return ALL_BUSES
    })
  }, [])

  useEffect(() => {
    void loadBuses()
  }, [loadBuses])

  const onSubmit = async () => {
    if (selectedBusId === '-' || buses.length === 0) {
      setFeedback('등록된 차량이 없습니다.')
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
      busId: selectedBusId,
      startIso: startAt,
      endIso: endAt,
      reason: reasonText,
    })
    setSubmitting(false)
    if (!result.ok) {
      setFeedback(result.message || '중단 처리에 실패했습니다.')
      return
    }
    setFeedback(
      result.message ||
        `해당 시간대 배차를 운행 불가로 처리했습니다${result.count != null ? ` (${result.count}건)` : ''}.`,
    )
  }

  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>운행 중단 · 기상악화 처리</h3>
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label>
              차량<span className="req">*</span>
            </label>
            <select
              className="select suspend-select"
              value={selectedBusId}
              onChange={(e) => setSelectedBusId(e.target.value)}
              disabled={loadingBuses || buses.length === 0}
            >
              {buses.length === 0 ? (
                <option value="-">-</option>
              ) : (
                <>
                  <option value="__all__">전체 차량</option>
                  {buses.map((b) => (
                    <option key={b.busId} value={b.busId}>
                      {b.label}
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
          <textarea
            className="textarea"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
          />
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
            disabled={
              submitting ||
              buses.length === 0 ||
              selectedBusId === '-' ||
              !startAt ||
              !endAt
            }
            onClick={() => void onSubmit()}
          >
            {submitting ? '처리 중…' : '운행 중단 처리'}
          </button>
        </div>
      </section>
    </div>
  )
}
