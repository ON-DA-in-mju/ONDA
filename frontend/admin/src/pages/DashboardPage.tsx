import {
  AlertTriangle,
  Bus,
  CalendarDays,
  Megaphone,
  MessageSquareWarning,
  RadioTower,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import mapImg from '../assets/map.png'
import { gpsAlerts, kpiCards, recentOps } from '../data/mock'
import { StatCard, StatusBadge } from '../components/ui/Form'

const iconNodes = [CalendarDays, Bus, Bus, RadioTower, MessageSquareWarning, Megaphone]

const reportLegend = [
  { label: '승하차 불편', color: '#9870d7' },
  { label: '운행 문의', color: '#fea907' },
  { label: '기사 서비스', color: '#3fb46a' },
  { label: '분실물', color: '#266ef4' },
  { label: '기타', color: '#c4c9d7' },
]

/** ADM-02 대시보드 — Figma 위젯 구조 반영 */
export function DashboardPage() {
  return (
    <div className="page">
      <div className="grid grid-6">
        {kpiCards.map((card, idx) => {
          const Icon = iconNodes[idx]
          return (
            <StatCard
              key={card.title}
              title={card.title}
              value={card.value}
              unit={card.unit}
              delta={card.delta}
              color={card.color}
              icon={<Icon size={18} />}
            />
          )
        })}
      </div>

      <div className="dash-mid">
        <section className="card card-pad">
          <div className="card-head">
            <h3>실시간 운행 지도</h3>
            <Link className="btn btn-ghost" to="/live" style={{ height: 30, fontSize: 12 }}>
              실시간 운행 보기
            </Link>
          </div>
          <div className="map-frame">
            <img src={mapImg} alt="실시간 운행 지도" />
          </div>
          <div className="legend-row">
            <span>
              <i style={{ background: '#236cee' }} /> 운행 중
            </span>
            <span>
              <i style={{ background: '#3fb46a' }} /> 대기
            </span>
            <span>
              <i style={{ background: '#fea907' }} /> 지연
            </span>
            <span>
              <i style={{ background: '#e64c51' }} /> 통신이상
            </span>
            <span>
              <i style={{ background: '#9870d7' }} /> 제보
            </span>
          </div>
        </section>

        <section className="card card-pad">
          <div className="card-head">
            <h3>최근 운행 현황</h3>
            <Link to="/schedules" className="muted" style={{ fontSize: 12 }}>
              모든 운행 보기
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

        <section className="card card-pad">
          <div className="card-head">
            <h3>학생 제보 요약</h3>
            <Link className="btn btn-ghost" to="/reports" style={{ height: 30, fontSize: 12 }}>
              학생 제보 확인
            </Link>
          </div>
          <div className="donut-wrap">
            <div className="donut">
              <div className="donut-hole">
                총 12건
                <br />
                처리 대기
              </div>
            </div>
          </div>
          <div className="legend-col">
            {reportLegend.map((item) => (
              <span key={item.label}>
                <i style={{ background: item.color }} /> {item.label}
              </span>
            ))}
          </div>
          <p className="muted" style={{ fontSize: 12, textAlign: 'center', marginTop: 8 }}>
            가장 많은 제보 유형은 승하차 관련입니다.
          </p>
        </section>
      </div>

      <div className="dash-bottom">
        <section className="card card-pad">
          <div className="card-head">
            <h3>GPS·통신 이상 경고</h3>
            <Link className="btn btn-ghost" to="/live" style={{ height: 30, fontSize: 12 }}>
              실시간 운행 보기
            </Link>
          </div>
          <table className="data-table">
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

        <section className="card card-pad">
          <div className="card-head">
            <h3>최근 긴급 공지</h3>
            <Link className="btn btn-ghost" to="/notices" style={{ height: 30, fontSize: 12 }}>
              긴급 공지 등록
            </Link>
          </div>
          <div className="alert alert-danger" style={{ display: 'flex', gap: 10 }}>
            <AlertTriangle size={18} />
            <div>
              <strong>폭염 특보에 따른 운행 조정 안내</strong>
              <div style={{ marginTop: 4 }}>2023.08.20 ~ 2023.08.23</div>
              <div style={{ marginTop: 6 }}>
                폭염 특보 발령으로 일부 노선의 배차 간격과 운행 시간이 임시 조정됩니다.
              </div>
            </div>
          </div>
          <div style={{ marginTop: 12, textAlign: 'right' }}>
            <Link to="/notices" className="muted" style={{ fontSize: 12, color: 'var(--color-primary)' }}>
              긴급 공지 전체 보기 &gt;
            </Link>
          </div>
        </section>
      </div>
    </div>
  )
}
