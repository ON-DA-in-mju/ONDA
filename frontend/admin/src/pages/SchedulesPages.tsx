import { Link } from 'react-router-dom'
import { schedules } from '../data/mock'
import { StatusBadge } from '../components/ui/Form'

/** ADM-03 운행 일정 목록 — Figma 좌/우 위젯 구조 */
export function SchedulesPage() {
  return (
    <div className="page">
      <div className="sched-grid">
        <div className="sched-col">
          <section className="card card-pad">
            <div className="card-head">
              <h3>운행 일정 조회</h3>
            </div>
            <div className="toolbar">
              <select className="select" style={{ width: 150 }}>
                <option>노선 전체</option>
                <option>기흥역 ↔ 캠퍼스</option>
                <option>용인시청 ↔ 캠퍼스</option>
              </select>
              <div className="toolbar">
                {['일', '월', '화', '수', '목', '금', '토'].map((d, i) => (
                  <button key={d} className={`page-chip${i === 1 ? ' active' : ''}`} type="button">
                    {d}
                  </button>
                ))}
              </div>
              <input className="input" style={{ width: 200 }} defaultValue="2024.07.22 ~ 2024.07.28" />
              <button className="btn btn-primary" type="button">
                조회하기
              </button>
              <button className="btn btn-outline" type="button">
                초기화
              </button>
            </div>
          </section>

          <section className="card card-pad">
            <div className="card-head">
              <h3>운행 일정 목록</h3>
              <Link className="btn btn-ghost" to="/schedules/suspend" style={{ height: 30 }}>
                운행 중단 처리
              </Link>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>요일</th>
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
                {schedules.map((row) => (
                  <tr key={`${row.day}-${row.route}`}>
                    <td>{row.day}</td>
                    <td>{row.route}</td>
                    <td>{row.start}</td>
                    <td>{row.end}</td>
                    <td>{row.interval}</td>
                    <td>{row.rounds}</td>
                    <td>
                      <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                    </td>
                    <td>
                      <Link className="btn btn-outline" to="/schedules/detail" style={{ height: 28, fontSize: 12 }}>
                        상세
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="pagination">
              <span>총 4건</span>
              <div className="pagination-pages">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button key={n} className={`page-chip${n === 1 ? ' active' : ''}`} type="button">
                    {n}
                  </button>
                ))}
              </div>
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
                  <td>2024.07.25</td>
                  <td>정기 점검</td>
                  <td>
                    <StatusBadge tone="orange">확정</StatusBadge>
                  </td>
                </tr>
                <tr>
                  <td>2024.07.27</td>
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
              <h3>운행 일정 요약 · 2024.07.22 ~ 2024.07.28</h3>
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
                ['총 운행 횟수', '210회', '일평균 30회'],
                ['총 운행 시간', '42시간 15분', '일평균 6시간 03분'],
                ['총 운행 차량', '8대', '노선 평균 2대'],
                ['예상 탑승 인원', '8,450명', '일평균 1,210명'],
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
              <h3>상세 시간표 (월요일 - 기흥역 ↔ 캠퍼스)</h3>
            </div>
            <div className="grid grid-2">
              {['기흥역 → 캠퍼스', '캠퍼스 → 기흥역'].map((title) => (
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
                      {[1, 2, 3, 4].map((n) => (
                        <tr key={n}>
                          <td>{n}</td>
                          <td>{`0${6 + n}:00`}</td>
                          <td>{`온다 ${n}호기`}</td>
                        </tr>
                      ))}
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

export function ScheduleDetailPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>운행 일정 상세 · 기흥역 ↔ 캠퍼스 (월요일)</h3>
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
            ['노선', '기흥역 ↔ 캠퍼스'],
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
