import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, Eye, Megaphone } from 'lucide-react'
import { maintenances, notices, reports, routes, systemLogs, users } from '../data/mock'
import { fetchLoginHistory, toLastLoginDisplay, type LoginHistoryEntry } from '../lib/loginHistoryApi'
import { StatusBadge } from '../components/ui/Form'
import '../styles/figma-pages.css'

/** ADM-05 커뮤니티 제보 관리 */
export function ReportsPage() {
  const [selected, setSelected] = useState(0)
  const item = reports[selected]

  return (
    <div className="page">
      <p className="page-subtitle">학생들의 제보를 검토하고 신뢰도를 관리하는 공간입니다.</p>
      <div className="grid grid-4">
        {[
          ['오늘 제보 수', '38건', '+8 전일 대비', 'blue'],
          ['처리 대기', '12건', '+3 전일 대비', 'orange'],
          ['자동 완료 예정', '4건', '24시간 이내', 'green'],
          ['비활성 처리', '26건', '누적', 'gray'],
        ].map(([t, v, s, tone]) => (
          <div key={t} className="card card-pad">
            <div className="muted" style={{ fontSize: 12 }}>
              {t}
            </div>
            <div style={{ fontSize: 22, fontWeight: 800 }}>{v}</div>
            <StatusBadge tone={tone as 'blue' | 'orange' | 'green' | 'gray'}>{s}</StatusBadge>
          </div>
        ))}
      </div>

      <div className="split-13">
        <section className="card card-pad">
          <div className="card-head">
            <h3>제보 목록</h3>
          </div>
          <div className="toolbar" style={{ marginBottom: 10 }}>
            <select className="select" style={{ width: 140 }}>
              <option>유형 전체</option>
            </select>
            <input className="input" style={{ width: 200 }} defaultValue="2026-07-01 ~ 2026-07-08" />
            <input className="input" style={{ flex: 1 }} placeholder="검색어 입력 (제보, 노선, 정류장)" />
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>유형</th>
                <th>대상</th>
                <th>시간</th>
                <th>좋아요</th>
                <th>상태</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {reports.map((row, idx) => (
                <tr key={row.type + row.time} style={idx === selected ? { background: '#f5f8ff' } : undefined}>
                  <td>{row.type}</td>
                  <td>{row.target}</td>
                  <td>{row.time}</td>
                  <td>{row.likes}</td>
                  <td>
                    <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                  </td>
                  <td>
                    <button className="btn btn-outline" type="button" style={{ height: 28 }} onClick={() => setSelected(idx)}>
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="card card-pad">
          <div className="card-head">
            <h3>제보 상세</h3>
            <StatusBadge tone={item.tone}>{item.status}</StatusBadge>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 13 }}>
            <div>
              <strong>{item.type}</strong>
              <div className="muted">{item.time} · student_1024</div>
            </div>
            <div className="card card-pad" style={{ boxShadow: 'none', background: '#fafbff' }}>
              버스가 학생회관 앞 정류장을 정차하지 않고 통과했습니다. 대기 학생이 다수 있었습니다.
            </div>
            <div className="muted">첨부/위치 · Android/Chrome · 반경 30m</div>
            <div className="field">
              <label>내부 메모</label>
              <textarea className="textarea" defaultValue="버스 운행 기록 확인 결과, 해당 시간대에 정상 운행." />
              <span className="field-hint">21/200</span>
            </div>
            <div className="toolbar">
              <button className="btn btn-outline" type="button">
                비활성화
              </button>
              <button className="btn btn-danger" type="button">
                삭제
              </button>
              <button className="btn btn-primary" type="button">
                만료 처리
              </button>
            </div>
            <div className="muted" style={{ fontSize: 11 }}>
              24시간 후 자동 만료 예정
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}

/** ADM-06 공지·긴급 알림 관리 — Figma 430:19126 */
export function NoticesPage() {
  const [noticeType, setNoticeType] = useState('긴급')
  const [push, setPush] = useState(true)
  const [title, setTitle] = useState('폭설로 인한 운행 지연 안내')
  const [body, setBody] = useState(
    '폭설로 인해 일부 노선의 운행이 지연되고 있습니다.\n자세한 내용은 노선별 운행 정보에서 확인해 주세요.\n이용에 불편을 드려 죄송합니다.',
  )

  const kpis = [
    { label: '전체 공지', value: '128건', hint: '최근 30일 기준', tone: 'blue', icon: <Megaphone size={18} /> },
    { label: '긴급 공지', value: '8건', hint: '최근 30일 기준', tone: 'red', icon: <Megaphone size={18} /> },
    { label: '예약 공지', value: '15건', hint: '개시 예정', tone: 'orange', icon: <CalendarDays size={18} /> },
    { label: '최근 조회수', value: '23,450회', hint: '최근 30일 기준', tone: 'gray', icon: <Eye size={18} /> },
  ] as const

  return (
    <div className="page">
      <div className="figma-kpis">
        {kpis.map((k) => (
          <div key={k.label} className="figma-kpi">
            <div className={`figma-kpi-icon ${k.tone}`}>{k.icon}</div>
            <div>
              <div className="label">{k.label}</div>
              <div className="value">{k.value}</div>
              <div className="hint">{k.hint}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="figma-split-notice">
        <section className="figma-panel">
          <div className="figma-panel-head">
            <h3>
              공지 목록 <span className="muted">(전체 128건)</span>
            </h3>
          </div>
          <div className="toolbar" style={{ marginBottom: 8 }}>
            <select className="select" style={{ width: 110, height: 32 }}>
              <option>전체 유형</option>
              <option>긴급</option>
              <option>중요</option>
              <option>운행 변경</option>
              <option>일반</option>
            </select>
            <input className="input" style={{ flex: 1, height: 32 }} placeholder="제목 또는 내용을 검색하세요." />
            <button className="btn btn-primary btn-xs" type="button">
              검색
            </button>
          </div>
          <table className="data-table dense">
            <thead>
              <tr>
                <th>번호</th>
                <th>유형</th>
                <th>제목</th>
                <th>대상</th>
                <th>게시 기간</th>
                <th>조회수</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {notices.map((row) => (
                <tr key={row.no}>
                  <td>{row.no}</td>
                  <td>
                    <StatusBadge tone={row.tone}>{row.type}</StatusBadge>
                  </td>
                  <td>{row.title}</td>
                  <td>{row.target}</td>
                  <td>{row.period}</td>
                  <td>{row.views.toLocaleString()}</td>
                  <td>
                    <StatusBadge tone={row.status === '게시중' ? 'red' : 'gray'}>{row.status}</StatusBadge>
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
            <select className="select" style={{ width: 110, height: 28 }}>
              <option>10개씩 보기</option>
            </select>
          </div>
          <p className="muted" style={{ fontSize: 11, marginTop: 8 }}>
            공지사항은 학생 앱 [알림] 탭과 푸시 알림으로 발송됩니다.
          </p>
        </section>

        <section className="figma-panel">
          <div className="figma-panel-head">
            <h3>공지 등록/수정</h3>
            <div className="figma-actions">
              <button className="btn btn-outline btn-xs" type="button">
                <Eye size={12} /> 미리보기
              </button>
              <button className="btn btn-outline btn-xs" type="button">
                삭제
              </button>
              <button className="btn btn-outline btn-xs" type="button">
                수정
              </button>
              <button className="btn btn-primary btn-xs" type="button">
                공지 등록
              </button>
              <button className="btn btn-danger btn-xs" type="button">
                긴급 공지 발송
              </button>
            </div>
          </div>

          <div className="notice-form-grid">
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <div className="field">
                <label>공지 유형</label>
                <div className="type-pills">
                  {(
                    [
                      ['긴급', true],
                      ['중요', false],
                      ['운행 변경', false],
                      ['일반', false],
                    ] as const
                  ).map(([t, danger]) => (
                    <button
                      key={t}
                      type="button"
                      className={`type-pill${noticeType === t ? ` active${danger ? ' danger' : ''}` : ''}`}
                      onClick={() => setNoticeType(t)}
                    >
                      {t === '긴급' || t === '일반' ? `${t} 공지` : t}
                    </button>
                  ))}
                </div>
              </div>
              <div className="field">
                <label>제목</label>
                <input className="input" style={{ height: 32 }} value={title} onChange={(e) => setTitle(e.target.value)} />
                <span className="field-hint">{title.length}/100</span>
              </div>
              <div className="field">
                <label>내용</label>
                <div className="toolbar" style={{ marginBottom: 4 }}>
                  {['B', 'I', 'U'].map((t) => (
                    <button key={t} type="button" className="btn btn-outline btn-xs" style={{ width: 28, padding: 0 }}>
                      {t}
                    </button>
                  ))}
                </div>
                <textarea className="textarea" style={{ minHeight: 90 }} value={body} onChange={(e) => setBody(e.target.value)} />
                <span className="field-hint">{body.length}/2000</span>
              </div>
              <div className="field">
                <label>대상</label>
                <div className="toolbar">
                  <button type="button" className="type-pill active">
                    전체 학생
                  </button>
                  <button type="button" className="type-pill">
                    특정 노선 선택
                  </button>
                </div>
                <input className="input" style={{ height: 32, marginTop: 6 }} placeholder="노선을 선택하세요." disabled />
                <span className="field-hint">여러 노선 선택 가능</span>
              </div>
              <div className="grid grid-2">
                <div className="field">
                  <label>시작일</label>
                  <input className="input" style={{ height: 32 }} defaultValue="2024.05.20 00:00" />
                </div>
                <div className="field">
                  <label>종료일</label>
                  <input className="input" style={{ height: 32 }} defaultValue="2024.05.20 23:59" />
                </div>
              </div>
              <label className="check-row">
                <input type="checkbox" defaultChecked />
                게시 기간 없음 (상시 게시)
              </label>
              <label className="check-row">
                <input type="checkbox" checked={push} onChange={(e) => setPush(e.target.checked)} />
                푸시 알림 동시 발송
              </label>
              {push ? (
                <div className="muted" style={{ fontSize: 11 }}>
                  ONDA 셔틀 앱 푸시 알림으로 즉시 발송됩니다.
                </div>
              ) : null}
            </div>

            <aside className="phone-preview">
              <div className="cap">실제 학생 앱에 표시되는 화면입니다.</div>
              <div className="screen">
                <div className="muted" style={{ fontSize: 10, marginBottom: 4 }}>
                  공지사항
                </div>
                <span className="tag">긴급 공지</span>
                <strong>{title || '제목'}</strong>
                <div className="time">2024.05.20 09:30</div>
                <p style={{ whiteSpace: 'pre-wrap' }}>{body}</p>
                <div className="muted" style={{ fontSize: 10, marginTop: 12, textAlign: 'center' }}>
                  오늘 하루 보지 않기
                </div>
              </div>
              <div className="push">
                <span className="app">ONDA 셔틀</span>
                <span className="when">지금</span>
                <div className="body">일부 노선의 운행이 지연되고 있습니다.</div>
              </div>
            </aside>
          </div>
        </section>
      </div>
    </div>
  )
}

/** ADM-04 노선·운행 관리 — Figma 430:18166 */
export function RoutesPage() {
  const [selected, setSelected] = useState(2)
  const detail = routes[selected]

  return (
    <div className="page">
      <div className="split-11">
        <section className="card card-pad">
          <div className="card-head">
            <h3>노선 목록</h3>
            <button className="btn btn-primary" type="button" style={{ height: 30 }}>
              노선 추가
            </button>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>노선명</th>
                <th>운행 상태</th>
                <th>배정 차량 수</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {routes.map((row, idx) => (
                <tr
                  key={row.name}
                  style={idx === selected ? { background: '#f5f8ff' } : undefined}
                  onClick={() => setSelected(idx)}
                >
                  <td>{row.name}</td>
                  <td>
                    <StatusBadge tone="green">{row.status}</StatusBadge>
                  </td>
                  <td>{row.buses}</td>
                  <td>
                    <button className="btn btn-outline" type="button" style={{ height: 28 }}>
                      수정
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="card card-pad">
          <div className="card-head">
            <h3>노선 상세 - {detail.name}</h3>
            <Link className="btn btn-ghost" to="/routes/detail" style={{ height: 30 }}>
              상세 보기
            </Link>
          </div>
          <div className="toolbar" style={{ marginBottom: 12 }}>
            {['기본 정보', '정류장', '시간표', '배정 차량'].map((tab, i) => (
              <button key={tab} className={`btn ${i === 0 ? 'btn-ghost' : 'btn-outline'}`} type="button" style={{ height: 30 }}>
                {tab}
              </button>
            ))}
          </div>
          <div className="grid grid-3">
            {[
              ['노선 유형', detail.type],
              ['운행 상태', detail.status],
              ['운행 요일', detail.days],
              ['운행 시간', detail.hours],
              ['배정 차량 수', detail.buses],
            ].map(([k, v]) => (
              <div key={k} className="card card-pad" style={{ boxShadow: 'none' }}>
                <div className="muted" style={{ fontSize: 12 }}>
                  {k}
                </div>
                <div style={{ fontWeight: 700 }}>{v}</div>
              </div>
            ))}
          </div>
          <div className="field" style={{ marginTop: 12 }}>
            <label>노선 설명</label>
            <p style={{ margin: 0, fontSize: 13 }}>{detail.desc}</p>
          </div>
          <div className="field" style={{ marginTop: 8 }}>
            <label>노선 경로 미리보기</label>
            <div className="map-frame" style={{ height: 120, background: '#eef5ff' }} />
          </div>
          <div className="grid grid-4" style={{ marginTop: 12 }}>
            {[
              ['총 정류장 수', '6개소'],
              ['총 운행 거리', '12.4 km'],
              ['예상 소요 시간', '28분'],
              ['운행 간격', '20분'],
            ].map(([k, v]) => (
              <div key={k} className="card card-pad" style={{ boxShadow: 'none' }}>
                <div className="muted" style={{ fontSize: 11 }}>
                  {k}
                </div>
                <div style={{ fontWeight: 700 }}>{v}</div>
              </div>
            ))}
          </div>
          <div className="toolbar" style={{ marginTop: 14, justifyContent: 'flex-end' }}>
            <button className="btn btn-outline" type="button">
              취소
            </button>
            <button className="btn btn-primary" type="button">
              저장
            </button>
          </div>
        </section>
      </div>
    </div>
  )
}

/** ADM-04-01 노선 상세 — Figma 430:18374 */
export function RouteDetailPage() {
  const [tab, setTab] = useState<'basic' | 'stops' | 'timetable' | 'buses'>('stops')
  const stopOrder = [
    ['1', '상공회의소'],
    ['2', '진입로(럭스나인 앞)'],
    ['3', '동부경찰서 중앙지구대'],
    ['4', '용인 CGV'],
    ['5', '버스관리사무소'],
    ['6', '중앙공영주차장'],
  ]

  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>
            시내 셔틀 <StatusBadge tone="green">운행 중</StatusBadge>
          </h3>
          <Link className="btn btn-outline" to="/routes" style={{ height: 30 }}>
            목록으로
          </Link>
        </div>

        <div className="toolbar" style={{ marginBottom: 14 }}>
          {[
            ['basic', '기본 정보'],
            ['stops', '정류장'],
            ['timetable', '시간표'],
            ['buses', '배정 차량'],
          ].map(([key, label]) => (
            <button
              key={key}
              className={`btn ${tab === key ? 'btn-ghost' : 'btn-outline'}`}
              type="button"
              style={{ height: 30 }}
              onClick={() => setTab(key as typeof tab)}
            >
              {label}
            </button>
          ))}
        </div>

        {tab === 'basic' ? (
          <div className="grid grid-3">
            {[
              ['출발지', '버스관리사무소'],
              ['도착지', '중앙공영주차장'],
              ['학생 앱 노출 여부', '노출 중'],
            ].map(([k, v]) => (
              <div key={k} className="card card-pad" style={{ boxShadow: 'none' }}>
                <div className="muted">{k}</div>
                <div style={{ fontWeight: 700 }}>{v}</div>
              </div>
            ))}
          </div>
        ) : null}

        {tab === 'stops' ? (
          <>
            <div className="card-head">
              <h3>정류장 순서</h3>
              <button className="btn btn-primary" type="button" style={{ height: 30 }}>
                + 정류장 추가
              </button>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>순번</th>
                  <th>정류장</th>
                </tr>
              </thead>
              <tbody>
                {stopOrder.map(([no, name]) => (
                  <tr key={no}>
                    <td>{no}</td>
                    <td>{name}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="muted" style={{ fontSize: 12, marginTop: 10 }}>
              학생 앱 미리보기 · 현재 3대 운행 중 · 다음 출발 17:18 / 17:21 / 17:24
            </p>
          </>
        ) : null}

        {tab === 'timetable' ? (
          <>
            <div className="card-head">
              <h3>시간표 편집</h3>
              <button className="btn btn-outline" type="button" style={{ height: 30 }}>
                편도
              </button>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>순번</th>
                  <th>출발시간</th>
                  <th>간격</th>
                  <th>운행 대수</th>
                  <th>비고</th>
                </tr>
              </thead>
              <tbody>
                {[
                  ['1', '07:15', '30분', '3대', ''],
                  ['2', '08:15', '30분', '3대', ''],
                  ['3', '09:15', '30분', '2대', ''],
                  ['18', '17:15', '30분', '2대', '다음 출발'],
                  ['19', '18:15', '30분', '2대', ''],
                  ['20', '19:15', '30분', '2대', ''],
                ].map((row) => (
                  <tr key={row[0] + row[1]}>
                    {row.map((cell) => (
                      <td key={cell}>{cell}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="muted" style={{ fontSize: 11, marginTop: 8 }}>
              교통 상황에 따라 ±5분 정도 오차가 발생할 수 있습니다.
            </p>
          </>
        ) : null}

        {tab === 'buses' ? (
          <>
            <div className="card-head">
              <h3>배정 차량</h3>
              <div className="toolbar">
                <button className="btn btn-outline" type="button" style={{ height: 30 }}>
                  차량 변경
                </button>
                <button className="btn btn-primary" type="button" style={{ height: 30 }}>
                  + 차량 추가
                </button>
              </div>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>호차</th>
                  <th>번호판</th>
                  <th>정원</th>
                  <th>차종</th>
                </tr>
              </thead>
              <tbody>
                {[
                  ['1호차', '70가 1234', '45인승', '현대 유니버스'],
                  ['2호차', '70가 5678', '45인승', '현대 유니버스'],
                  ['3호차', '70가 9012', '45인승', '현대 유니버스'],
                ].map((row) => (
                  <tr key={row[0]}>
                    {row.map((cell) => (
                      <td key={cell}>{cell}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        ) : null}

        <div className="toolbar" style={{ marginTop: 16, justifyContent: 'flex-end' }}>
          <button className="btn btn-outline" type="button">
            오늘 운행에 반영
          </button>
          <button className="btn btn-primary" type="button">
            저장
          </button>
        </div>
      </section>
    </div>
  )
}

export function StopsPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>정류장 관리</h3>
          <button className="btn btn-primary" type="button">
            + 정류장 등록
          </button>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>정류장명</th>
              <th>이용 노선</th>
              <th>좌표</th>
              <th>안내</th>
            </tr>
          </thead>
          <tbody>
            {[
              ['기흥역 5번 출구', '기흥역 통학버스', '37.2754, 127.1159', '5번 출구 앞 정류장'],
              ['채플관 앞', '기흥역 통학버스', '37.2240, 127.1872', '채플관 정문 버스정류장'],
              ['명지대역', '명지대역 셔틀', '37.2381, 127.1905', '명지대역 2번 출구'],
            ].map((row) => (
              <tr key={row[0]}>
                <td>{row[0]}</td>
                <td>{row[1]}</td>
                <td>{row[2]}</td>
                <td>{row[3]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}

/** ADM-07 차량·정비 관리 — Figma 430:19461 */
export function VehiclesPage() {
  const kpis = [
    { label: '전체 차량', value: '28', unit: '대', meta: '정상 20 · 정비중 4 · 점검 필요 4', tone: 'blue' },
    { label: '예정 정비', value: '6', unit: '건', meta: '이번 주 2 · 이번 달 6', tone: 'orange' },
    { label: '정비 완료', value: '12', unit: '건', meta: '지난달 대비 20%', tone: 'green' },
    { label: '정비 비용', value: '4,850', unit: '만원', meta: '(이번달) · 지난달 대비 15%', tone: 'purple' },
    { label: '가동률', value: '92.6', unit: '%', meta: '목표 90%', tone: 'blue' },
  ] as const

  return (
    <div className="page">
      <div className="toolbar" style={{ justifyContent: 'flex-end' }}>
        <select className="select" style={{ width: 120, height: 32 }}>
          <option>전체 차량</option>
        </select>
        <input className="input" style={{ width: 220, height: 32 }} defaultValue="2026.07.01 ~ 2026.07.31" />
        <button className="btn btn-primary btn-xs" type="button" style={{ height: 32 }}>
          정비 등록
        </button>
      </div>

      <div className="figma-kpis-5">
        {kpis.map((k) => (
          <div key={k.label} className="figma-kpi">
            <div>
              <div className="label">{k.label}</div>
              <div className="value">
                {k.value}
                <em>{k.unit}</em>
              </div>
              <div className="meta">{k.meta}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="figma-split-vehicle">
        <section className="figma-panel">
          <div className="figma-panel-head">
            <h3>정비 이력</h3>
            <div className="toolbar">
              <input className="input" style={{ width: 220, height: 32 }} placeholder="차량 번호 / 정비 항목 검색" />
              <select className="select" style={{ width: 110, height: 32 }}>
                <option>전체 상태</option>
                <option>완료</option>
                <option>예정</option>
                <option>점검중</option>
              </select>
            </div>
          </div>
          <table className="data-table dense">
            <thead>
              <tr>
                <th>정비일</th>
                <th>차량 번호</th>
                <th>정비 항목</th>
                <th>정비 유형</th>
                <th>정비사</th>
                <th>비용(원)</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {maintenances.map((row) => (
                <tr key={row.date + row.plate + row.item}>
                  <td>{row.date}</td>
                  <td>{row.plate}</td>
                  <td>{row.item}</td>
                  <td>{row.type}</td>
                  <td>{row.mechanic}</td>
                  <td>{row.cost}</td>
                  <td>
                    <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="card-head" style={{ marginTop: 16 }}>
            <h3>예정 정비</h3>
          </div>
          <table className="data-table dense">
            <thead>
              <tr>
                <th>일자</th>
                <th>차량</th>
                <th>항목</th>
                <th>잔여</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {[
                ['07.24', '73버 1122', '브레이크 패드 점검', '2일 후'],
                ['07.24', '74버 7788', '엔진오일 교환', '2일 후'],
                ['07.24', '72버 5678', '타이어 위치 교환', '3일 후'],
              ].map((row) => (
                <tr key={row.join('-')}>
                  <td>{row[0]}</td>
                  <td>{row[1]}</td>
                  <td>{row[2]}</td>
                  <td>{row[3]}</td>
                  <td>
                    <button className="btn btn-outline btn-xs" type="button">
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <div className="stack">
          <section className="figma-panel">
            <h3 style={{ margin: '0 0 8px', fontSize: 13 }}>정비 유형별 통계 (이번 달)</h3>
            <div className="donut-sm">
              <div className="donut-sm-hole">
                총
                <br />
                12건
              </div>
            </div>
            <div className="stat-list">
              <div className="stat-list-row">
                <span>정기 점검</span>
                <strong>6 (50%)</strong>
              </div>
              <div className="stat-list-row">
                <span>수리</span>
                <strong>3 (25%)</strong>
              </div>
              <div className="stat-list-row">
                <span>소모품 교체</span>
                <strong>2 (16.7%)</strong>
              </div>
              <div className="stat-list-row">
                <span>기타</span>
                <strong>1 (8.3%)</strong>
              </div>
            </div>
          </section>

          <section className="figma-panel">
            <h3 style={{ margin: '0 0 8px', fontSize: 13 }}>월별 정비 비용 추이 (만원)</h3>
            <div className="bar-chart" aria-hidden>
              {[
                ['02', 35],
                ['03', 55],
                ['04', 76],
                ['05', 64],
                ['06', 85],
                ['07', 80],
              ].map(([m, h]) => (
                <div key={m} className="bar-col">
                  <div className="bar" style={{ height: `${h}%` }} />
                  <span>2026-{m}</span>
                </div>
              ))}
            </div>
          </section>

          <section className="figma-panel">
            <h3 style={{ margin: '0 0 8px', fontSize: 13 }}>알림</h3>
            <div className="alert-stack">
              <div className="alert alert-warning">72버 1234 차량의 정기점검이 예정되어 있습니다. · 20분 전</div>
              <div className="alert alert-danger">75버 9900 차량의 타이어 교체가 필요합니다. · 1시간 전</div>
              <div className="alert alert-success">73버 1122 차량의 브레이크 패드 교체가 완료되었습니다. · 2시간 전</div>
            </div>
          </section>

          <section className="figma-panel">
            <h3 style={{ margin: '0 0 8px', fontSize: 13 }}>정비 통계 요약</h3>
            <div className="grid grid-2">
              {[
                ['완료율', '75%', '%', '(12/16)'],
                ['평균 정비 주기', '45', '일', ''],
                ['평균 정비 비용', '405', '천원', ''],
                ['가동률 (목표)', '92.6', '%', ''],
              ].map(([k, v, u, s]) => (
                <div key={k}>
                  <div className="muted" style={{ fontSize: 11 }}>
                    {k}
                  </div>
                  <div style={{ fontWeight: 800, fontSize: 18 }}>
                    {v}
                    <span style={{ fontSize: 11, fontWeight: 600, marginLeft: 2 }}>{u}</span>
                  </div>
                  {s ? (
                    <div className="muted" style={{ fontSize: 10 }}>
                      {s}
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}

export function DriversPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>기사 계정 관리</h3>
          <button className="btn btn-primary" type="button">
            + 기사 계정 생성
          </button>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>이름</th>
              <th>이메일</th>
              <th>상태</th>
              <th>최근 운행</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>김민수</td>
              <td>driver01@onda.local</td>
              <td>
                <StatusBadge tone="blue">운행 가능</StatusBadge>
              </td>
              <td>2026.08.06 09:05</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  )
}

/** ADM-08 사용자 관리 — Figma 430:19862 */
export function UsersPage() {
  const [selected, setSelected] = useState(0)
  const [loginHistory, setLoginHistory] = useState<LoginHistoryEntry[]>([])
  const user = users[selected]

  useEffect(() => {
    let alive = true
    const load = async () => {
      const rows = await fetchLoginHistory()
      if (alive) setLoginHistory(rows)
    }
    void load()
    const timer = window.setInterval(load, 5_000)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [])

  const lastLoginByUser = useMemo(() => {
    const map = new Map<string, string>()
    for (const row of loginHistory) {
      if (!map.has(row.userId)) map.set(row.userId, toLastLoginDisplay(row.time))
    }
    return map
  }, [loginHistory])

  return (
    <div className="page">
      <div className="figma-kpis">
        {[
          { label: '전체 사용자', value: '36명', hint: '활성 사용자 32명', tone: 'blue' },
          { label: '관리자', value: '5명', hint: '전체의 13.9%', tone: 'purple' },
          { label: '운영자', value: '12명', hint: '전체의 33.3%', tone: 'orange' },
          { label: '일반 사용자', value: '19명', hint: '전체의 52.8%', tone: 'gray' },
        ].map((k) => (
          <div key={k.label} className="figma-kpi">
            <div>
              <div className="label">{k.label}</div>
              <div className="value">{k.value}</div>
              <div className="hint">{k.hint}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="figma-split-notice">
        <section className="figma-panel">
          <div className="figma-panel-head">
            <h3>
              사용자 목록 <span className="muted">(36명)</span>
            </h3>
          </div>
          <div className="toolbar" style={{ marginBottom: 8 }}>
            <select className="select" style={{ width: 120, height: 32 }}>
              <option>전체 역할</option>
              <option>관리자</option>
              <option>운영자</option>
              <option>일반</option>
            </select>
            <input className="input" style={{ flex: 1, height: 32 }} placeholder="이름, 아이디, 이메일 검색" />
            <button className="btn btn-primary btn-xs" type="button">
              사용자 추가
            </button>
          </div>
          <table className="data-table dense">
            <thead>
              <tr>
                <th>아이디</th>
                <th>이름</th>
                <th>이메일</th>
                <th>역할</th>
                <th>상태</th>
                <th>마지막 로그인</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {users.map((row, idx) => (
                <tr key={row.id} style={idx === selected ? { background: '#f5f8ff' } : undefined}>
                  <td>{row.id}</td>
                  <td>{row.name}</td>
                  <td>{row.email}</td>
                  <td>
                    <StatusBadge tone={row.role === '관리자' ? 'blue' : row.role === '운영자' ? 'purple' : 'gray'}>
                      {row.role}
                    </StatusBadge>
                  </td>
                  <td>
                    <StatusBadge tone={row.status === '활성' ? 'green' : 'gray'}>{row.status}</StatusBadge>
                  </td>
                  <td>{lastLoginByUser.get(row.id) ?? row.lastLogin}</td>
                  <td>
                    <button className="btn btn-outline btn-xs" type="button" onClick={() => setSelected(idx)}>
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pagination">
            <div className="pagination-pages">
              {[1, 2, 3, 4].map((n) => (
                <button key={n} className={`page-chip${n === 1 ? ' active' : ''}`} type="button">
                  {n}
                </button>
              ))}
            </div>
            <select className="select" style={{ width: 110, height: 28 }}>
              <option>10개씩 보기</option>
            </select>
          </div>
        </section>

        <div className="stack">
          <section className="figma-panel">
            <h3 style={{ margin: '0 0 8px', fontSize: 13 }}>
              역할 권한 설정 · {user.name} ({user.id})
            </h3>
            <div className="muted" style={{ fontSize: 11, marginBottom: 8 }}>
              권한 가이드 · ○ 접근 가능 · × 접근 불가
            </div>
            <table className="data-table dense perm-table">
              <thead>
                <tr>
                  <th>메뉴</th>
                  <th>권한</th>
                </tr>
              </thead>
              <tbody>
                {[
                  ['대시보드', '○'],
                  ['운행 관리', '○'],
                  ['차량 관리', user.role === '일반' ? '×' : '○'],
                  ['시스템 설정', user.role === '관리자' ? '○' : '×'],
                ].map(([k, v]) => (
                  <tr key={k}>
                    <td>{k}</td>
                    <td>{v}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

          <section className="figma-panel">
            <div className="figma-panel-head">
              <h3>최근 로그인 기록</h3>
              <button className="btn btn-ghost btn-xs" type="button">
                전체 보기
              </button>
            </div>
            <div className="muted" style={{ fontSize: 12, lineHeight: 1.8 }}>
              {loginHistory.length === 0 ? (
                <span>로그인 기록이 없습니다. 관리자 웹(dev) 실행 후 기사 앱에서 로그인하면 여기에 반영됩니다.</span>
              ) : (
                loginHistory.slice(0, 8).map((row) => (
                  <div key={`${row.userId}-${row.time}-${row.ip}`}>
                    {row.name} ({row.userId}) · {row.time} · {row.ip}
                    {row.source === 'driver-app' ? ' · 기사앱' : ''}
                  </div>
                ))
              )}
            </div>
          </section>

          <section className="figma-panel">
            <h3 style={{ margin: '0 0 8px', fontSize: 13 }}>보안 정책</h3>
            <div className="field">
              <label>비밀번호 최소 길이</label>
              <input className="input" style={{ height: 32 }} defaultValue="10자 이상" />
            </div>
            <div className="field">
              <label>비밀번호 변경 주기</label>
              <input className="input" style={{ height: 32 }} defaultValue="90일" />
            </div>
            <div className="field">
              <label>세션 타임아웃</label>
              <input className="input" style={{ height: 32 }} defaultValue="30분" />
            </div>
            <div className="field">
              <label>연속 로그인 실패 허용 횟수</label>
              <input className="input" style={{ height: 32 }} defaultValue="5회" />
            </div>
            <button className="btn btn-primary btn-xs" type="button" style={{ marginTop: 8 }}>
              정책 설정
            </button>
          </section>
        </div>
      </div>
    </div>
  )
}

/** ADM-09 시스템 기록 조회 — Figma 430:20246 */
export function SystemPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>기록조회</h3>
        </div>
        <div className="toolbar" style={{ flexWrap: 'wrap' }}>
          <div className="field" style={{ minWidth: 120 }}>
            <label>기록 유형</label>
            <select className="select">
              <option>전체</option>
              <option>운영 기록</option>
              <option>사용자 활동</option>
              <option>시스템 변경</option>
              <option>오류 / 경고</option>
            </select>
          </div>
          <div className="field" style={{ minWidth: 120 }}>
            <label>사용자</label>
            <select className="select">
              <option>전체</option>
            </select>
          </div>
          <div className="field" style={{ minWidth: 220 }}>
            <label>기간</label>
            <input className="input" defaultValue="2026.07.13 ~ 2026.07.20" />
          </div>
          <div className="field" style={{ flex: 1, minWidth: 180 }}>
            <label>키워드 검색</label>
            <input className="input" placeholder="검색어를 입력하세요." />
          </div>
          <button className="btn btn-outline" type="button" style={{ alignSelf: 'end' }}>
            초기화
          </button>
          <button className="btn btn-primary" type="button" style={{ alignSelf: 'end' }}>
            조회하기
          </button>
        </div>
      </section>

      <div className="card-head">
        <h3>
          기록 요약 <span className="muted">(2026.07.13 ~ 2026.07.20)</span>
        </h3>
        <button className="btn btn-outline" type="button">
          엑셀 다운로드
        </button>
      </div>

      <div className="grid grid-5">
        {[
          ['전체 기록 수', '2,458건', '일 평균 351건'],
          ['운영 기록', '1,362건', '55.4%'],
          ['사용자 활동', '736건', '29.9%'],
          ['시스템 변경', '248건', '10.1%'],
          ['오류 / 경고', '112건', '4.6%'],
        ].map(([t, v, s]) => (
          <div key={t} className="card card-pad">
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

      <div className="split-14">
        <section className="card card-pad">
          <div className="card-head">
            <h3>시스템 기록 목록</h3>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>시간</th>
                <th>기록 유형</th>
                <th>상세 내용</th>
                <th>사용자</th>
                <th>IP 주소</th>
                <th>대상</th>
                <th>결과</th>
              </tr>
            </thead>
            <tbody>
              {systemLogs.map((row) => (
                <tr key={row.time + row.action}>
                  <td>{row.time}</td>
                  <td>{row.type}</td>
                  <td>{row.action}</td>
                  <td>{row.actor}</td>
                  <td>{row.ip}</td>
                  <td>{row.target}</td>
                  <td>
                    <StatusBadge
                      tone={row.result === '성공' ? 'green' : row.result === '경고' ? 'orange' : 'red'}
                    >
                      {row.result}
                    </StatusBadge>
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
            <select className="select" style={{ width: 110, height: 32 }}>
              <option>10개씩 보기</option>
            </select>
          </div>
          <p className="muted" style={{ fontSize: 11, marginTop: 8 }}>
            시스템 시간 기준으로 기록이 저장됩니다.
          </p>
        </section>

        <div className="stack">
          <section className="card card-pad">
            <h3>기록 유형 분포</h3>
            <div className="muted" style={{ fontSize: 12, lineHeight: 1.8 }}>
              운영 기록 1,362 (55.4%)
              <br />
              사용자 활동 736 (29.9%)
              <br />
              시스템 변경 248 (10.1%)
              <br />
              오류 / 경고 112 (4.6%)
              <br />
              <strong style={{ color: 'var(--color-text)' }}>총 2,458건</strong>
            </div>
          </section>
          <section className="card card-pad">
            <div className="card-head">
              <h3>보관 정책</h3>
              <button className="btn btn-outline" type="button" style={{ height: 28, fontSize: 12 }}>
                보관 정책 관리
              </button>
            </div>
            <p className="muted" style={{ fontSize: 12, margin: 0, lineHeight: 1.6 }}>
              시스템 기록은 1년간 보관됩니다. 보관 기간 이후 데이터는 자동 삭제됩니다.
            </p>
          </section>
        </div>
      </div>
    </div>
  )
}

export function SettingsPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>설정</h3>
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label>연속 로그인 실패 허용 횟수</label>
            <input className="input" defaultValue="5회" />
          </div>
          <div className="field">
            <label>세션 타임아웃</label>
            <input className="input" defaultValue="30분" />
          </div>
          <div className="field">
            <label>비밀번호 최소 길이</label>
            <input className="input" defaultValue="10자 이상" />
          </div>
          <div className="field">
            <label>비밀번호 변경 주기</label>
            <input className="input" defaultValue="90일" />
          </div>
          <div className="field">
            <label>알림 이메일</label>
            <input className="input" defaultValue="admin@mju.ac.kr" />
          </div>
          <div className="field">
            <label>기본 타임존</label>
            <select className="select" defaultValue="seoul">
              <option value="seoul">Asia/Seoul</option>
            </select>
          </div>
        </div>
        <div className="toolbar" style={{ marginTop: 16, justifyContent: 'flex-end' }}>
          <button className="btn btn-primary" type="button">
            저장
          </button>
        </div>
      </section>
    </div>
  )
}
