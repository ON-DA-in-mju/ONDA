import { useMemo } from 'react'
import {
  AlertTriangle,
  Bus,
  CalendarDays,
  Megaphone,
  MessageSquareWarning,
  RadioTower,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { NaverMap, type MapVehicle } from '../components/map/NaverMap'
import { gpsAlerts, kpiCards, liveVehicles, recentOps } from '../data/mock'
import { StatusBadge } from '../components/ui/Form'
import '../styles/dashboard.css'

const iconNodes = [CalendarDays, Bus, Bus, RadioTower, MessageSquareWarning, Megaphone]

const reportLegend = [
  { label: '승하차 불편', count: 5, pct: '41.7%', color: '#9870d7' },
  { label: '안전/사고', count: 2, pct: '16.7%', color: '#fea907' },
  { label: '기사/서비스', count: 2, pct: '16.7%', color: '#3fb46a' },
  { label: '분실물', count: 2, pct: '16.7%', color: '#266ef4' },
  { label: '기타', count: 1, pct: '8.3%', color: '#c4c9d7' },
]

/** ADM-01 대시보드 — Figma 7457c195 */
export function DashboardPage() {
  const mapVehicles: MapVehicle[] = useMemo(
    () =>
      liveVehicles.slice(0, 5).map((v) => ({
        id: v.bus,
        label: v.bus.replace('온다 ', ''),
        subLabel: v.route,
        lat: v.lat,
        lng: v.lng,
        tone: v.tone,
        gpsStatus: v.gps,
      })),
    [],
  )

  return (
    <div className="page dash-page">
      <div className="dash-kpis">
        {kpiCards.map((card, idx) => {
          const Icon = iconNodes[idx]
          return (
            <div key={card.title} className="dash-kpi">
              <div className="dash-kpi-icon" style={{ background: card.color }}>
                <Icon size={18} />
              </div>
              <div className="dash-kpi-body">
                <div className="label">{card.title}</div>
                <div className="value">
                  {card.value}
                  <em>{card.unit}</em>
                </div>
                <div className={`delta ${card.deltaTone}`}>{card.delta}</div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="dash-mid">
        <section className="card card-pad dash-panel">
          <div className="card-head">
            <h3>실시간 운행 지도</h3>
            <Link className="btn btn-outline btn-xs" to="/live">
              실시간 운행 보기
            </Link>
          </div>
          <div className="dash-map">
            <NaverMap vehicles={mapVehicles} zoom={14} />
          </div>
          <div className="legend-row">
            <span>
              <i style={{ background: '#236cee' }} /> 운행 중
            </span>
            <span>
              <i style={{ background: '#3fb46a' }} /> 예정
            </span>
            <span>
              <i style={{ background: '#fea907' }} /> 대기
            </span>
            <span>
              <i style={{ background: '#e64c51' }} /> GPS 이상
            </span>
            <span>
              <i style={{ background: '#9870d7' }} /> 정비
            </span>
          </div>
        </section>

        <section className="card card-pad dash-panel">
          <div className="card-head">
            <h3>최근 운행 현황</h3>
            <Link className="btn btn-outline btn-xs" to="/schedules">
              오늘의 운행 보기
            </Link>
          </div>
          <div className="ops-list">
            {recentOps.map((item) => (
              <div className="ops-item" key={`${item.bus}-${item.time}`}>
                <StatusBadge tone={item.tone}>{item.status}</StatusBadge>
                <div className="ops-body">
                  <strong>{item.route}</strong>
                  <span>
                    {item.bus} · {item.driver}
                  </span>
                </div>
                <span className="ops-time">{item.time}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="card card-pad dash-panel">
          <div className="card-head">
            <h3>학생 제보 요약</h3>
            <Link className="btn btn-outline btn-xs" to="/reports">
              학생 제보 확인
            </Link>
          </div>
          <div className="dash-report">
            <div className="donut">
              <div className="donut-hole">
                총 12건
                <br />
                처리 대기
              </div>
            </div>
            <div className="dash-report-legend">
              {reportLegend.map((item) => (
                <div key={item.label} className="dash-report-row">
                  <span>
                    <i style={{ background: item.color }} />
                    {item.label}
                  </span>
                  <strong>
                    {item.count}건 · {item.pct}
                  </strong>
                </div>
              ))}
            </div>
          </div>
        </section>
      </div>

      <div className="dash-bottom">
        <section className="card card-pad dash-panel">
          <div className="card-head">
            <h3>GPS·통신 이상 경고</h3>
            <Link className="btn btn-outline btn-xs" to="/live">
              실시간 운행 보기
            </Link>
          </div>
          <table className="data-table dense">
            <thead>
              <tr>
                <th>차량 번호</th>
                <th>노선</th>
                <th>현재 위치</th>
                <th>이상 유형</th>
                <th>발생 시간</th>
                <th>조치 상태</th>
              </tr>
            </thead>
            <tbody>
              {gpsAlerts.map((row) => (
                <tr key={row.bus}>
                  <td>{row.bus}</td>
                  <td>{row.route}</td>
                  <td>{row.location}</td>
                  <td>{row.issue}</td>
                  <td>{row.time}</td>
                  <td>
                    <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="card card-pad dash-panel">
          <div className="card-head">
            <h3>최근 긴급 공지</h3>
            <Link className="btn btn-outline btn-xs" to="/notices">
              긴급 공지 등록
            </Link>
          </div>
          <div className="dash-notice">
            <div className="dash-notice-icon">
              <AlertTriangle size={18} />
            </div>
            <div className="dash-notice-body">
              <div className="dash-notice-top">
                <StatusBadge tone="red">진행 중</StatusBadge>
                <span className="muted">2026.08.20 ~ 2026.08.23</span>
              </div>
              <strong>폭염 특보에 따른 운행 조정 안내</strong>
              <p>폭염 특보 발령으로 일부 노선의 배차 간격과 운행 시간이 임시 조정됩니다.</p>
            </div>
          </div>
          <div className="dash-notice-link">
            <Link to="/notices">긴급 공지 전체 보기 &gt;</Link>
          </div>
        </section>
      </div>
    </div>
  )
}
