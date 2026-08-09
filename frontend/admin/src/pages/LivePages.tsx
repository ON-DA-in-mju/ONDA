import { useMemo } from 'react'
import {
  AlertTriangle,
  Bus,
  Info,
  List,
  MapPin,
  Phone,
  PieChart,
  RadioTower,
  Search,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { NaverMap, type MapVehicle } from '../components/map/NaverMap'
import { liveVehicles } from '../data/mock'
import { StatusBadge } from '../components/ui/Form'
import '../styles/live.css'

const gpsSegments = [
  { label: '정상', count: 17, pct: 94.4, color: '#22c55e' },
  { label: '미수신', count: 1, pct: 5.6, color: '#ef4444' },
  { label: '오류', count: 0, pct: 0, color: '#f59e0b' },
]

const statusSegments = [
  { label: '운행 중', count: 14, pct: 77.8, color: '#22c55e' },
  { label: '정차 중', count: 1, pct: 5.6, color: '#3b82f6' },
  { label: '대기 중', count: 2, pct: 11.1, color: '#f59e0b' },
  { label: '운행 종료', count: 1, pct: 5.6, color: '#64748b' },
]

const alerts = [
  { tone: 'red' as const, title: '장시간 미수신', desc: '온다 6호차(9-2) GPS 신호 미수신 32분 경과', time: '09:05' },
  { tone: 'orange' as const, title: '지연 운행', desc: '온다 4호차(7-1) 수원역 구간 약 8분 지연', time: '09:02' },
  { tone: 'blue' as const, title: '운행 완료', desc: '온다 8호차(5-1) 오전 운행을 정상 종료했습니다', time: '08:55' },
]

function donutStyle(segments: { pct: number; color: string }[]) {
  let start = 0
  const parts = segments.map((s) => {
    const end = start + s.pct
    const part = `${s.color} ${start}% ${end}%`
    start = end
    return part
  })
  return { background: `conic-gradient(${parts.join(', ')})` }
}

/** ADM-03 실시간 운행 관제 */
export function LivePage() {
  const mapVehicles: MapVehicle[] = useMemo(
    () =>
      liveVehicles.map((v) => ({
        id: v.bus,
        label: v.bus,
        subLabel: v.route,
        lat: v.lat,
        lng: v.lng,
        tone: v.tone,
        gpsStatus: v.gps,
      })),
    [],
  )

  return (
    <div className="page live-page">
      <section className="card card-pad live-toolbar">
        <div className="live-filters">
          <label className="live-field">
            <span>노선</span>
            <select className="input" defaultValue="전체">
              <option>전체</option>
              <option>15-1</option>
              <option>5-2</option>
              <option>12-1</option>
              <option>7-1</option>
              <option>9-2</option>
            </select>
          </label>
          <label className="live-field">
            <span>상태</span>
            <select className="input" defaultValue="전체">
              <option>전체</option>
              <option>운행 중</option>
              <option>정차 중</option>
              <option>대기 중</option>
              <option>장시간 미수신</option>
            </select>
          </label>
          <label className="live-search">
            <span>검색</span>
            <div className="live-search-wrap">
              <Search size={15} />
              <input className="input" placeholder="차량 번호, 기사명 검색" />
            </div>
          </label>
        </div>
        <div className="live-actions">
          <Link className="btn btn-outline" to="/live/detail">
            <Info size={14} />
            운행 상세 보기
          </Link>
          <Link className="btn btn-outline" to="/drivers/contact">
            <Phone size={14} />
            기사에게 연락
          </Link>
          <Link className="btn btn-primary" to="/live/suspend">
            운행 상태 변경
          </Link>
        </div>
      </section>

      <div className="live-top-grid">
        <section className="card card-pad live-panel">
          <div className="live-panel-head">
            <h3>
              <MapPin size={17} />
              실시간 차량 위치
            </h3>
            <div className="live-status-legend">
              <span><i className="dot green" />운행 중</span>
              <span><i className="dot blue" />정차 중</span>
              <span><i className="dot orange" />대기 중</span>
              <span><i className="dot gray" />운행 종료</span>
              <span><i className="dot red" />장시간 미수신</span>
            </div>
          </div>
          <div className="live-map">
            <NaverMap vehicles={mapVehicles} />
          </div>
          <div className="live-route-legend">
            <span><i className="route-line r1" />15-1</span>
            <span><i className="route-line r2" />5-1</span>
            <span><i className="route-line r3" />12-1</span>
            <span><i className="route-line r4" />7-1</span>
            <span><i className="route-line r5" />9-2</span>
            <span><i className="dot gray" />정류장</span>
          </div>
        </section>

        <section className="card card-pad live-panel">
          <div className="live-panel-head">
            <h3>
              <List size={17} />
              실시간 차량 목록
            </h3>
            <p className="live-count">
              전체 <strong>18</strong>대 · 운행 중 <strong>14</strong>대 · 대기 <strong>2</strong>대 · 종료 <strong>2</strong>대
            </p>
          </div>
          <div className="live-table-wrap">
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>차량 번호</th>
                  <th>기사명</th>
                  <th>현재 노선</th>
                  <th>현재 정류장/구간</th>
                  <th>운행 상태</th>
                  <th>마지막 GPS 수신</th>
                  <th>GPS 상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {liveVehicles.map((row) => (
                  <tr key={row.bus} className={row.tone === 'red' ? 'live-row-alert' : undefined}>
                    <td>
                      <span className="live-bus-cell">
                        <span className={`live-bus-ico ${row.tone}`}>
                          <Bus size={13} />
                        </span>
                        {row.bus}
                      </span>
                    </td>
                    <td>{row.driver}</td>
                    <td>{row.route}</td>
                    <td>{row.stop}</td>
                    <td>
                      <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                    </td>
                    <td>{row.last}</td>
                    <td>
                      <StatusBadge tone={row.gps === '정상' ? 'green' : 'red'}>{row.gps}</StatusBadge>
                    </td>
                    <td>
                      <Link className="btn btn-outline btn-xs" to="/live/detail">
                        보기
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="live-alert-banner">
            <AlertTriangle size={16} />
            <div>
              <strong>장시간 미수신 차량 1대:</strong> 온다 6호차(9-2) 차량이 32분 이상 GPS 신호를 수신하지 못하고 있습니다.
            </div>
            <Link to="/live/detail">조치 방법 보기 →</Link>
          </div>
        </section>
      </div>

      <div className="live-bottom-grid">
        <section className="card card-pad live-panel">
          <div className="live-panel-head">
            <h3>
              <RadioTower size={17} />
              GPS 정상율
            </h3>
          </div>
          <div className="live-donut-row">
            <div className="live-donut" style={donutStyle(gpsSegments)}>
              <div className="live-donut-hole">
                <strong>94.4%</strong>
                <span>전체</span>
              </div>
            </div>
            <ul className="live-donut-legend">
              {gpsSegments.map((s) => (
                <li key={s.label}>
                  <span className="legend-dot" style={{ background: s.color }} />
                  <span className="legend-label">{s.label}</span>
                  <strong>{s.count}대</strong>
                  <em>{s.pct}%</em>
                </li>
              ))}
            </ul>
          </div>
          <p className="live-donut-foot">전체 18대 기준</p>
        </section>

        <section className="card card-pad live-panel">
          <div className="live-panel-head">
            <h3>
              <PieChart size={17} />
              운행 상태 비율
            </h3>
          </div>
          <div className="live-donut-row">
            <div className="live-donut" style={donutStyle(statusSegments)}>
              <div className="live-donut-hole">
                <strong>18대</strong>
                <span>전체</span>
              </div>
            </div>
            <ul className="live-donut-legend">
              {statusSegments.map((s) => (
                <li key={s.label}>
                  <span className="legend-dot" style={{ background: s.color }} />
                  <span className="legend-label">{s.label}</span>
                  <strong>{s.count}대</strong>
                  <em>{s.pct}%</em>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section className="card card-pad live-panel">
          <div className="live-panel-head">
            <h3>
              <AlertTriangle size={17} />
              주요 알림
            </h3>
            <Link className="live-more" to="/reports">
              전체 보기 →
            </Link>
          </div>
          <ul className="live-alerts">
            {alerts.map((a) => (
              <li key={a.title} className={`live-alert live-alert-${a.tone}`}>
                <span className="live-alert-ico">
                  {a.tone === 'blue' ? <Info size={15} /> : <AlertTriangle size={15} />}
                </span>
                <div>
                  <strong>{a.title}</strong>
                  <p>{a.desc}</p>
                </div>
                <time>{a.time}</time>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  )
}

export function LiveDetailPage() {
  const v = liveVehicles[0]
  const detailVehicles: MapVehicle[] = [
    {
      id: v.bus,
      label: v.bus,
      subLabel: v.route,
      lat: v.lat,
      lng: v.lng,
      tone: v.tone,
      gpsStatus: v.gps,
    },
  ]

  return (
    <div className="page live-page">
      <section className="card card-pad live-detail-card">
        <div className="live-detail-top">
          <div>
            <p className="live-kicker">
              <Bus size={14} />
              차량 상세
            </p>
            <h3>온다 1호차 · 15-1</h3>
            <p className="muted">기사 김기사 · 현재 명지대 정문 인근 운행 중</p>
          </div>
          <div className="live-actions">
            <Link className="btn btn-outline" to="/drivers/contact">
              <Phone size={14} />
              기사에게 연락
            </Link>
            <Link className="btn btn-primary" to="/live/suspend">
              운행 상태 변경
            </Link>
          </div>
        </div>
        <div className="live-detail-grid">
          <div className="live-detail-map">
            <NaverMap vehicles={detailVehicles} zoom={16} center={{ lat: v.lat, lng: v.lng }} />
          </div>
          <div className="live-detail-side">
            <div className="live-metric">
              <span>현재 속도</span>
              <strong>32 km/h</strong>
            </div>
            <div className="live-metric">
              <span>다음 정류장</span>
              <strong>도서관 앞</strong>
            </div>
            <div className="live-metric">
              <span>예상 도착</span>
              <strong>3분</strong>
            </div>
            <div className="live-metric">
              <span>GPS 상태</span>
              <strong className="ok">정상</strong>
            </div>
            <div className="live-metric">
              <span>마지막 수신</span>
              <strong>방금 전</strong>
            </div>
            <div className="live-metric">
              <span>금일 운행</span>
              <strong>4회 / 정상</strong>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}

export function LiveSuspendPage() {
  return (
    <div className="page live-page">
      <section className="card card-pad live-suspend-card">
        <h3>
          <AlertTriangle size={18} />
          운행 중단 / 상태 변경
        </h3>
        <p className="muted">선택된 차량의 실시간 운행 상태를 변경합니다. 변경 사유를 반드시 입력해 주세요.</p>
        <div className="live-suspend-form">
          <label>
            <span>대상 차량</span>
            <select className="input" defaultValue="온다 6호차">
              {liveVehicles.map((v) => (
                <option key={v.bus}>{v.bus}</option>
              ))}
            </select>
          </label>
          <label>
            <span>변경 상태</span>
            <select className="input" defaultValue="운행 중단">
              <option>운행 중단</option>
              <option>대기</option>
              <option>운행 재개</option>
              <option>운행 종료</option>
            </select>
          </label>
          <label className="full">
            <span>변경 사유</span>
            <textarea className="input" rows={4} placeholder="예: GPS 미수신으로 인한 임시 운행 중단" />
          </label>
        </div>
        <div className="live-actions end">
          <Link className="btn btn-outline" to="/live">
            취소
          </Link>
          <button type="button" className="btn btn-primary">
            상태 변경 적용
          </button>
        </div>
      </section>
    </div>
  )
}
