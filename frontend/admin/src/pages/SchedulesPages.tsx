import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bus,
  CalendarDays,
  Car,
  Clock3,
  Download,
  Plus,
  Route as RouteIcon,
  Search,
  Users,
} from 'lucide-react'
import { SCHEDULE_ROUTE_OPTIONS, schedules } from '../data/mock'
import { MJU_TIMETABLE_PACKS } from '../data/mjuTimetable'
import { StatusBadge } from '../components/ui/Form'
import { semesterForDate } from '../lib/academicCalendar'
import { fetchSchedulesWithRoutes, routeMatchesFilter, type ScheduleWithRoute } from '../lib/seedMju'
import { isSupabaseConfigured } from '../lib/supabase'
import type { SemesterType, Weekday } from '../types/database'
import '../styles/schedules.css'

const days = ['전체', '월', '화', '수', '목', '금', '토', '일'] as const
type DayChip = (typeof days)[number]

const DAY_TO_WEEKDAY: Record<Exclude<DayChip, '전체'>, Weekday> = {
  월: 'MON',
  화: 'TUE',
  수: 'WED',
  목: 'THU',
  금: 'FRI',
  토: 'SAT',
  일: 'SUN',
}

const WEEKDAY_KO: Record<Weekday, string> = {
  MON: '월',
  TUE: '화',
  WED: '수',
  THU: '목',
  FRI: '금',
  SAT: '토',
  SUN: '일',
}

const summaryCards = [
  { label: '총 운행 횟수', value: '210', unit: '회', hint: '일평균 30회', icon: CalendarDays, tone: 'blue' },
  { label: '총 운행 시간', value: '42시간 15분', unit: '', hint: '일평균 6시간 03분', icon: Clock3, tone: 'green' },
  { label: '총 운행 차량', value: '8', unit: '대', hint: '노선 평균 2대', icon: Car, tone: 'orange' },
  { label: '예상 탑승 인원', value: '8,450', unit: '명', hint: '일평균 1,210명', icon: Users, tone: 'purple' },
] as const

const exceptions = [
  { date: '2026.07.25', day: '금', reason: '정기 점검', route: '기흥역 통학버스', action: '운행 중단', status: '예정' },
  { date: '2026.07.27', day: '일', reason: '축제 일정으로 배차 증편', route: '전체 노선', action: '증편', status: '예정' },
  { date: '2026.07.30', day: '수', reason: '도로 공사', route: '시내 셔틀', action: '우회 운행', status: '예정' },
]

function formatTime(t: string) {
  return t.slice(0, 5)
}

function jsDayToChip(d: number): DayChip {
  const map: DayChip[] = ['일', '월', '화', '수', '목', '금', '토']
  return map[d] ?? '월'
}

/** ADM-02 오늘의 운행·배차 목록 — 공식 시간표는 DB schedules */
export function SchedulesPage() {
  const [activeDay, setActiveDay] = useState<DayChip>(() => jsDayToChip(new Date().getDay()))
  const [draftRoute, setDraftRoute] = useState<string>(SCHEDULE_ROUTE_OPTIONS[0])
  const [routeFilter, setRouteFilter] = useState<string>(SCHEDULE_ROUTE_OPTIONS[0])
  const [exceptionTab, setExceptionTab] = useState<'upcoming' | 'past'>('upcoming')
  const [timetableRoute, setTimetableRoute] = useState<string>(SCHEDULE_ROUTE_OPTIONS[1])
  const [timetablePeriod, setTimetablePeriod] = useState<SemesterType>(() => semesterForDate(new Date()))
  const [dbSchedules, setDbSchedules] = useState<ScheduleWithRoute[] | null>(null)

  const selectedWeekday: Weekday | null = activeDay === '전체' ? null : DAY_TO_WEEKDAY[activeDay]

  const loadDbSchedules = useCallback(async () => {
    const rows = await fetchSchedulesWithRoutes()
    setDbSchedules(rows)
  }, [])

  useEffect(() => {
    void loadDbSchedules()
  }, [loadDbSchedules])

  useEffect(() => {
    setTimetablePeriod(semesterForDate(new Date()))
  }, [])

  useEffect(() => {
    if (routeFilter) setTimetableRoute(routeFilter)
  }, [routeFilter])

  const localFallbackTrips = useMemo(() => {
    const pack =
      timetablePeriod === 'VACATION'
        ? MJU_TIMETABLE_PACKS.find((p) => p.id === 'weekend-vacation-city' || p.id === 'seasonal-shuttle')
        : timetableRoute === '기흥역 통학버스'
          ? MJU_TIMETABLE_PACKS.find((p) => p.id === 'semester-giheung')
          : MJU_TIMETABLE_PACKS.find((p) => p.id === 'semester-shuttle')
    const trips = (pack ?? MJU_TIMETABLE_PACKS[0])?.trips ?? []
    return trips.filter((t) => t.route === timetableRoute || routeMatchesFilter(t.route, timetableRoute))
  }, [timetableRoute, timetablePeriod])

  const dbTrips = useMemo(() => {
    if (!dbSchedules) return []
    return dbSchedules
      .filter(
        (s) =>
          routeMatchesFilter(s.routes?.route_name, timetableRoute) &&
          s.semester === timetablePeriod &&
          (selectedWeekday == null || s.weekday === selectedWeekday),
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
    if (!dbTrips.length) return null
    const times = dbTrips.map((s) => formatTime(s.departure_time)).sort()
    return { rounds: dbTrips.length, start: times[0], end: times[times.length - 1] }
  }, [dbTrips])

  const displayRows = useMemo(() => {
    if (isSupabaseConfigured) return dbTrips
    if (dbTrips.length) return dbTrips
    return localFallbackTrips.map((t, i) => ({
      id: `local-${i}`,
      departure_time: `${t.departure}:00`,
      weekday: (selectedWeekday ?? 'MON') as Weekday,
      semester: timetablePeriod,
      routes: { route_name: t.route },
    }))
  }, [dbTrips, localFallbackTrips, selectedWeekday, timetablePeriod])

  const patternPreview = useMemo(() => {
    const START_HOUR = 8
    const SLOT_COUNT = 10
    const rows =
      dbSchedules?.filter(
        (s) => routeMatchesFilter(s.routes?.route_name, timetableRoute) && s.semester === timetablePeriod,
      ) ?? []
    const source: { hour: number; weekday: Weekday }[] = rows.length
      ? rows.map((s) => ({
          hour: Number.parseInt(String(s.departure_time).slice(0, 2), 10) || 0,
          weekday: s.weekday as Weekday,
        }))
      : localFallbackTrips.flatMap((t) => {
          const hour = Number.parseInt(t.departure.slice(0, 2), 10) || 0
          const wds: Weekday[] = ['MON', 'TUE', 'WED', 'THU', 'FRI']
          return wds.map((weekday) => ({ hour, weekday }))
        })
    const WEEKDAY_SET = new Set<Weekday>(['MON', 'TUE', 'WED', 'THU', 'FRI'])
    const slots = Array.from({ length: SLOT_COUNT }, (_, i) => {
      const hour = START_HOUR + i
      const atHour = source.filter((s) => s.hour === hour)
      const weekdayPct = Math.min(100, atHour.filter((s) => WEEKDAY_SET.has(s.weekday)).length * 8)
      const weekendPct = Math.min(100, atHour.filter((s) => !WEEKDAY_SET.has(s.weekday)).length * 12)
      return { hour, weekdayPct, weekendPct }
    })
    return { slots, fromDb: rows.length > 0, empty: source.length === 0 }
  }, [dbSchedules, timetableRoute, timetablePeriod, localFallbackTrips])

  const onSearch = () => {
    setRouteFilter(draftRoute)
    setTimetableRoute(draftRoute)
  }

  const onReset = () => {
    const today = jsDayToChip(new Date().getDay())
    setActiveDay(today)
    setDraftRoute(SCHEDULE_ROUTE_OPTIONS[0])
    setRouteFilter(SCHEDULE_ROUTE_OPTIONS[0])
    setTimetableRoute(SCHEDULE_ROUTE_OPTIONS[0])
    setTimetablePeriod(semesterForDate(new Date()))
  }

  return (
    <div className="page sched-page">
      <div className="sched-grid">
        <div className="sched-col">
          <section className="card card-pad sched-panel">
            <div className="card-head">
              <h3>
                <Search size={15} />
                운행 일정 조회
              </h3>
            </div>

            <div className="sched-filter">
              <div className="sched-filter-field">
                <label>
                  <RouteIcon size={13} />
                  노선
                </label>
                <select
                  className="select"
                  style={{ width: 220 }}
                  value={draftRoute}
                  onChange={(e) => setDraftRoute(e.target.value)}
                >
                  {SCHEDULE_ROUTE_OPTIONS.map((name) => (
                    <option key={name} value={name}>
                      {name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="sched-filter-field sched-filter-days">
                <label>
                  <CalendarDays size={13} />
                  요일
                </label>
                <div className="sched-day-group">
                  {days.map((d) => (
                    <button
                      key={d}
                      type="button"
                      className={`sched-day${activeDay === d ? ' active' : ''}`}
                      onClick={() => setActiveDay(d)}
                    >
                      {d}
                    </button>
                  ))}
                </div>
              </div>

              <div className="sched-filter-actions">
                <button className="btn btn-primary" type="button" onClick={onSearch}>
                  <Search size={14} />
                  조회하기
                </button>
                <button className="btn btn-outline" type="button" onClick={onReset}>
                  초기화
                </button>
              </div>
            </div>
          </section>

          <section className="card card-pad sched-panel">
            <div className="card-head">
              <h3>
                <Bus size={15} />
                운행 일정 목록
              </h3>
              <Link className="btn btn-outline btn-xs" to="/schedules/suspend">
                운행 중단 처리
              </Link>
            </div>
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>요일</th>
                  <th>노선</th>
                  <th>첫차 시간</th>
                  <th>막차 시간</th>
                  <th>운행 간격</th>
                  <th>운행 횟수</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {schedules.map((row) => (
                  <tr key={`${row.day}-${row.route}`}>
                    <td>{row.day}</td>
                    <td>{row.route}</td>
                    <td>{row.start}</td>
                    <td>{row.end}</td>
                    <td>{row.interval}</td>
                    <td>{row.rounds}회</td>
                    <td>
                      <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                    </td>
                    <td>
                      <Link className="btn btn-outline btn-xs" to="/schedules/detail">
                        상세
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

          <section className="card card-pad sched-panel">
            <div className="card-head">
              <h3>
                <CalendarDays size={15} />
                예외 일정 관리
              </h3>
              <div className="toolbar">
                <button
                  type="button"
                  className={`btn btn-xs ${exceptionTab === 'upcoming' ? 'btn-primary' : 'btn-outline'}`}
                  onClick={() => setExceptionTab('upcoming')}
                >
                  예정
                </button>
                <button
                  type="button"
                  className={`btn btn-xs ${exceptionTab === 'past' ? 'btn-primary' : 'btn-outline'}`}
                  onClick={() => setExceptionTab('past')}
                >
                  지난
                </button>
                <Link className="btn btn-primary btn-xs" to="/schedules/exception?mode=new">
                  <Plus size={13} />
                  예외 일정 등록
                </Link>
              </div>
            </div>
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>날짜</th>
                  <th>요일</th>
                  <th>사유</th>
                  <th>노선</th>
                  <th>조치</th>
                  <th>상태</th>
                </tr>
              </thead>
              <tbody>
                {(exceptionTab === 'upcoming' ? exceptions : []).map((row) => (
                  <tr key={`${row.date}-${row.route}`}>
                    <td>{row.date}</td>
                    <td>{row.day}</td>
                    <td>{row.reason}</td>
                    <td>{row.route}</td>
                    <td>{row.action}</td>
                    <td>
                      <StatusBadge tone="orange">{row.status}</StatusBadge>
                    </td>
                  </tr>
                ))}
                {exceptionTab === 'past' ? (
                  <tr>
                    <td colSpan={6} className="muted">
                      지난 예외 일정이 없습니다.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </section>
        </div>

        <div className="sched-col">
          <section className="card card-pad sched-panel">
            <div className="card-head">
              <h3>
                <CalendarDays size={15} />
                운행 일정 요약
              </h3>
              <div className="toolbar">
                <button className="btn btn-outline btn-xs" type="button">
                  <Download size={13} />
                  엑셀 다운로드
                </button>
                <Link className="btn btn-primary btn-xs" to="/schedules/bulk">
                  <Plus size={13} />
                  운행 일정 생성
                </Link>
              </div>
            </div>
            <div className="sched-summary">
              {summaryCards.map((card) => (
                <div key={card.label} className="sched-summary-card">
                  <div className={`sched-summary-icon ${card.tone}`}>
                    <card.icon size={16} />
                  </div>
                  <div>
                    <div className="label">{card.label}</div>
                    <div className="value">
                      {card.value}
                      {card.unit ? <em>{card.unit}</em> : null}
                    </div>
                    <div className="hint">{card.hint}</div>
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="card card-pad sched-panel">
            <div className="card-head">
              <h3>
                <Clock3 size={15} />
                공식 시간표
              </h3>
              <div className="toolbar sched-tt-filters">
                <select
                  className="select"
                  style={{ width: 200, height: 32 }}
                  value={timetableRoute}
                  onChange={(e) => setTimetableRoute(e.target.value)}
                >
                  {SCHEDULE_ROUTE_OPTIONS.map((name) => (
                    <option key={name} value={name}>
                      {name}
                    </option>
                  ))}
                </select>
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
            <p className="muted sched-tt-meta">
              {timetablePeriod === 'SEMESTER' ? '학기 중' : '방학·계절학기'} ·{' '}
              {activeDay === '전체' ? '전체 요일' : `${activeDay}요일`} · {timetableRoute}
              {dbRouteSummary
                ? ` · ${dbRouteSummary.rounds}회 · ${dbRouteSummary.start}~${dbRouteSummary.end}`
                : timetableLoading
                  ? ' · 불러오는 중…'
                  : timetableRoute === '기흥역 통학버스' && timetablePeriod === 'VACATION'
                    ? ' · 기흥역은 방학·계절학기 미운행'
                    : ' · 해당 조건 일정 없음'}
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
                    displayRows.map((row, idx) => (
                      <tr key={'id' in row && row.id ? String(row.id) : `row-${idx}`}>
                        <td>{idx + 1}</td>
                        <td>{('routes' in row && row.routes?.route_name) || timetableRoute}</td>
                        <td style={{ fontWeight: 700 }}>
                          {'departure_time' in row ? formatTime(String(row.departure_time)) : '-'}
                        </td>
                        <td>
                          {'weekday' in row
                            ? WEEKDAY_KO[row.weekday as Weekday] ?? String(row.weekday)
                            : activeDay}
                        </td>
                        <td>
                          {('semester' in row ? String(row.semester) : timetablePeriod) === 'VACATION'
                            ? '방학·계절학기'
                            : '학기 중'}
                        </td>
                      </tr>
                    ))}
                  {!timetableLoading && displayRows.length === 0 ? (
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

          <section className="card card-pad sched-panel">
            <div className="card-head">
              <h3>
                <Bus size={15} />
                운행 패턴 미리보기
              </h3>
              <span className="muted" style={{ fontSize: 12 }}>
                {timetableRoute} · {timetablePeriod === 'VACATION' ? '방학' : '학기중'} · 08~17시
                {patternPreview.fromDb ? '' : ' (로컬)'}
              </span>
            </div>
            {patternPreview.empty ? (
              <p className="muted" style={{ margin: '12px 0', fontSize: 13 }}>
                선택 노선·학기에 해당하는 schedules가 없습니다.
              </p>
            ) : (
              <>
                <div className="sched-pattern" aria-hidden>
                  {patternPreview.slots.map((slot) => (
                    <div key={slot.hour} className="sched-pattern-col">
                      <div className="sched-pattern-bars">
                        <span className="bar bar-a" style={{ height: `${slot.weekdayPct || 4}%` }} />
                        <span className="bar bar-b" style={{ height: `${slot.weekendPct || 4}%` }} />
                      </div>
                      <em>{String(slot.hour).padStart(2, '0')}시</em>
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
              </>
            )}
          </section>
        </div>
      </div>
    </div>
  )
}

export function ScheduleDetailPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>운행 일정 상세 · 기흥역 통학버스 (월요일)</h3>
          <div className="toolbar">
            <button className="btn btn-outline" type="button">
              수정
            </button>
            <button className="btn btn-danger" type="button">
              삭제
            </button>
          </div>
        </div>
        <div className="grid grid-4">
          {[
            ['노선', '기흥역 통학버스'],
            ['운행 요일', '월요일'],
            ['운행 시간', '08:00 ~ 19:30'],
            ['배차 간격', '약 60분'],
            ['총 회차', '27회'],
            ['배차 차량', '온다 1~4호차'],
            ['담당 기사', '김기사 외 3명'],
            ['상태', '운행 중'],
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
            <tr>
              <td>현장 담당</td>
              <td>김기사</td>
              <td>010-2222-3333</td>
              <td>기흥역 통학버스</td>
            </tr>
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
                <td>기흥역 통학버스</td>
                <td>월</td>
                <td>{`0${6 + n}:00`}</td>
                <td>19:30</td>
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
            <select className="select" defaultValue={SCHEDULE_ROUTE_OPTIONS[0]}>
              <option value="all">전체 노선</option>
              {SCHEDULE_ROUTE_OPTIONS.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
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
