import { useState } from 'react'
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
import { schedules } from '../data/mock'
import { StatusBadge } from '../components/ui/Form'
import '../styles/schedules.css'

const days = ['전체', '월', '화', '수', '목', '금', '토', '일'] as const

const summaryCards = [
  { label: '총 운행 횟수', value: '210', unit: '회', hint: '일평균 30회', icon: CalendarDays, tone: 'blue' },
  { label: '총 운행 시간', value: '42시간 15분', unit: '', hint: '일평균 6시간 03분', icon: Clock3, tone: 'green' },
  { label: '총 운행 차량', value: '8', unit: '대', hint: '노선 평균 2대', icon: Car, tone: 'orange' },
  { label: '예상 탑승 인원', value: '8,450', unit: '명', hint: '일평균 1,210명', icon: Users, tone: 'purple' },
] as const

const timetableA = [
  ['1', '07:00', '72버 1234'],
  ['2', '07:20', '73버 1122'],
  ['3', '07:40', '72버 5678'],
  ['4', '08:00', '74버 7788'],
  ['5', '08:20', '72버 1234'],
]

const timetableB = [
  ['1', '07:10', '73버 1122'],
  ['2', '07:30', '72버 5678'],
  ['3', '07:50', '74버 7788'],
  ['4', '08:10', '72버 1234'],
  ['5', '08:30', '73버 1122'],
]

const exceptions = [
  { date: '2026.07.25', day: '금', reason: '정기 점검', route: '기흥역 ⇄ 캠퍼스', action: '운행 중단', status: '예정' },
  { date: '2026.07.27', day: '일', reason: '축제 일정으로 배차 증편', route: '전체 노선', action: '증편', status: '예정' },
  { date: '2026.07.30', day: '수', reason: '도로 공사', route: '시내 셔틀', action: '우회 운행', status: '예정' },
]

const patternHours = ['06', '08', '10', '12', '14', '16', '18', '20', '22', '24']
const patternWeekday = [35, 78, 92, 70, 55, 68, 95, 82, 48, 22]
const patternWeekend = [18, 42, 55, 48, 38, 45, 60, 52, 30, 12]

/** ADM-02 오늘의 운행·배차 목록 — Figma 56235228 */
export function SchedulesPage() {
  const [activeDay, setActiveDay] = useState<(typeof days)[number]>('월')
  const [exceptionTab, setExceptionTab] = useState<'upcoming' | 'past'>('upcoming')

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
                <select className="select" defaultValue="giheung">
                  <option value="all">노선 전체</option>
                  <option value="giheung">기흥역 ⇄ 캠퍼스</option>
                  <option value="yongin">용인시청 ⇄ 캠퍼스</option>
                  <option value="suwon">수원역 ⇄ 캠퍼스</option>
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

              <div className="sched-filter-field sched-filter-period">
                <label>
                  <CalendarDays size={13} />
                  기간
                </label>
                <div className="sched-period">
                  <div className="sched-date-wrap">
                    <input className="input" defaultValue="2026.07.20" />
                    <CalendarDays size={14} />
                  </div>
                  <span>~</span>
                  <div className="sched-date-wrap">
                    <input className="input" defaultValue="2026.07.26" />
                    <CalendarDays size={14} />
                  </div>
                </div>
              </div>

              <div className="sched-filter-actions">
                <button className="btn btn-primary" type="button">
                  <Search size={14} />
                  조회하기
                </button>
                <button className="btn btn-outline" type="button">
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
                    <td>{row.route.replace('↔', '⇄')}</td>
                    <td>{row.start}</td>
                    <td>{row.end}</td>
                    <td>{row.interval}</td>
                    <td>{row.rounds}회</td>
                    <td>
                      <StatusBadge tone={row.tone}>{row.status === '수정 중' ? '주말 운행' : row.status}</StatusBadge>
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
            <div className="pagination">
              <div className="pagination-pages">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button key={n} className={`page-chip${n === 1 ? ' active' : ''}`} type="button">
                    {n}
                  </button>
                ))}
              </div>
              <select className="select" style={{ width: 110, height: 28, fontSize: 11 }}>
                <option>10개씩 보기</option>
              </select>
            </div>
          </section>

          <section className="card card-pad sched-panel">
            <div className="card-head">
              <h3>
                <CalendarDays size={15} />
                예외 일정 관리
              </h3>
              <Link className="btn btn-primary btn-xs" to="/schedules/exception?mode=new">
                <Plus size={13} />
                예외 일정 등록
              </Link>
            </div>
            <div className="sched-tabs">
              <button
                type="button"
                className={exceptionTab === 'upcoming' ? 'active' : undefined}
                onClick={() => setExceptionTab('upcoming')}
              >
                예정된 예외
              </button>
              <button
                type="button"
                className={exceptionTab === 'past' ? 'active' : undefined}
                onClick={() => setExceptionTab('past')}
              >
                지난 예외
              </button>
            </div>
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>일자</th>
                  <th>요일</th>
                  <th>사유</th>
                  <th>적용 노선</th>
                  <th>조치</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {(exceptionTab === 'upcoming' ? exceptions : exceptions.slice(0, 1)).map((row) => (
                  <tr key={row.date + row.reason}>
                    <td>{row.date}</td>
                    <td>{row.day}</td>
                    <td>{row.reason}</td>
                    <td>{row.route}</td>
                    <td>{row.action}</td>
                    <td>
                      <StatusBadge tone="orange">{exceptionTab === 'upcoming' ? '예정' : '종료'}</StatusBadge>
                    </td>
                    <td>
                      <Link className="btn btn-outline btn-xs" to={`/schedules/exception?date=${encodeURIComponent(row.date)}`}>
                        상세
                      </Link>
                    </td>
                  </tr>
                ))}
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
                <span className="muted"> · 2026.07.20 ~ 2026.07.26</span>
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
                상세 시간표
              </h3>
              <span className="sched-subtitle">월요일 · 기흥역 ⇄ 캠퍼스</span>
            </div>
            <div className="sched-timetable">
              <div>
                <div className="sched-tt-title">기흥역 → 캠퍼스</div>
                <table className="data-table dense">
                  <thead>
                    <tr>
                      <th>순번</th>
                      <th>출발 시간</th>
                      <th>차량</th>
                    </tr>
                  </thead>
                  <tbody>
                    {timetableA.map((row) => (
                      <tr key={row[0]}>
                        <td>{row[0]}</td>
                        <td>{row[1]}</td>
                        <td>{row[2]}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="sched-tt-rail" aria-hidden>
                <span />
                <div className="sched-tt-bus">
                  <Bus size={16} />
                </div>
                <span />
              </div>

              <div>
                <div className="sched-tt-title">캠퍼스 → 기흥역</div>
                <table className="data-table dense">
                  <thead>
                    <tr>
                      <th>순번</th>
                      <th>출발 시간</th>
                      <th>차량</th>
                    </tr>
                  </thead>
                  <tbody>
                    {timetableB.map((row) => (
                      <tr key={row[0]}>
                        <td>{row[0]}</td>
                        <td>{row[1]}</td>
                        <td>{row[2]}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>

          <section className="card card-pad sched-panel">
            <div className="card-head">
              <h3>
                <Bus size={15} />
                운행 패턴 미리보기
              </h3>
              <button className="btn btn-ghost btn-xs" type="button">
                전체 보기
              </button>
            </div>
            <div className="sched-pattern" aria-hidden>
              {patternHours.map((hour, i) => (
                <div key={hour} className="sched-pattern-col">
                  <div className="sched-pattern-bars">
                    <span className="bar bar-a" style={{ height: `${patternWeekday[i]}%` }} />
                    <span className="bar bar-b" style={{ height: `${patternWeekend[i]}%` }} />
                  </div>
                  <em>{hour}시</em>
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

export function ScheduleDetailPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>운행 일정 상세 · 기흥역 ⇄ 캠퍼스 (월요일)</h3>
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
            ['노선', '기흥역 ⇄ 캠퍼스'],
            ['운행 요일', '월요일'],
            ['운행 시간', '07:00 ~ 22:30'],
            ['배차 간격', '15분'],
            ['총 회차', '42회'],
            ['배차 차량', '온다 1~4호기'],
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
              <td>기흥역 노선</td>
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
                <td>기흥역 ⇄ 캠퍼스</td>
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
              <option>기흥역 ⇄ 캠퍼스</option>
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
