import { Link } from 'react-router-dom'
import mapImg from '../assets/map.png'
import { liveVehicles } from '../data/mock'
import { StatusBadge } from '../components/ui/Form'

export function LivePage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="toolbar">
          <select className="select" style={{ width: 140 }}>
            <option>노선 전체</option>
          </select>
          <select className="select" style={{ width: 140 }}>
            <option>상태 전체</option>
          </select>
          <input className="input" style={{ width: 240 }} placeholder="차량 번호, 기사명 검색" />
          <div style={{ flex: 1 }} />
          <Link className="btn btn-outline" to="/live/detail">
            운행 상세 보기
          </Link>
          <button className="btn btn-outline" type="button">
            기사에게 연락
          </button>
          <Link className="btn btn-primary" to="/live/suspend">
            운행 상태 변경
          </Link>
        </div>
      </section>

      <div className="grid" style={{ gridTemplateColumns: '1.2fr 1fr' }}>
        <section className="card card-pad">
          <div className="card-head">
            <h3>실시간 차량 위치</h3>
            <div className="toolbar" style={{ fontSize: 11 }}>
              <StatusBadge tone="green">운행</StatusBadge>
              <StatusBadge tone="orange">정지</StatusBadge>
              <StatusBadge tone="blue">대기</StatusBadge>
              <StatusBadge tone="gray">종료</StatusBadge>
              <StatusBadge tone="red">신호 소실</StatusBadge>
            </div>
          </div>
          <div style={{ borderRadius: 10, overflow: 'hidden', border: '1px solid #eef1f6' }}>
            <img src={mapImg} alt="실시간 차량 위치" style={{ width: '100%', height: 320, objectFit: 'cover' }} />
          </div>
        </section>

        <section className="card card-pad">
          <div className="card-head">
            <h3>실시간 차량 목록</h3>
          </div>
          <div className="toolbar" style={{ marginBottom: 10, fontSize: 12 }}>
            <StatusBadge tone="blue">전체 52</StatusBadge>
            <StatusBadge tone="green">운행 중 32</StatusBadge>
            <StatusBadge tone="orange">정지 12</StatusBadge>
            <StatusBadge tone="gray">대기 2</StatusBadge>
            <StatusBadge tone="red">종료 2</StatusBadge>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>차량</th>
                <th>기사</th>
                <th>노선</th>
                <th>현재 위치</th>
                <th>상태</th>
                <th>GPS</th>
              </tr>
            </thead>
            <tbody>
              {liveVehicles.map((row) => (
                <tr key={row.bus} style={row.tone === 'red' ? { background: '#fff5f5' } : undefined}>
                  <td>{row.bus}</td>
                  <td>{row.driver}</td>
                  <td>{row.route}</td>
                  <td>{row.stop}</td>
                  <td>
                    <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                  </td>
                  <td>{row.gps}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="alert alert-danger" style={{ marginTop: 12 }}>
            실시간 미수신 차량 알림: 온다 6호기 외 2대 ·{' '}
            <button className="btn btn-outline" type="button" style={{ height: 28, marginLeft: 8 }}>
              조치 방법 보기
            </button>
          </div>
        </section>
      </div>

      <div className="grid grid-3">
        <section className="card card-pad">
          <h3>GPS 정상율</h3>
          <div style={{ fontSize: 32, fontWeight: 800, color: '#266ef4', margin: '12px 0' }}>94.4%</div>
          <div className="muted" style={{ fontSize: 12 }}>
            정상 / 미수신 / 오류
          </div>
        </section>
        <section className="card card-pad">
          <h3>운행 상태 비율</h3>
          <div style={{ fontSize: 32, fontWeight: 800, margin: '12px 0' }}>18대</div>
          <div className="toolbar" style={{ fontSize: 12 }}>
            <StatusBadge tone="green">운행</StatusBadge>
            <StatusBadge tone="orange">정지</StatusBadge>
            <StatusBadge tone="blue">대기</StatusBadge>
            <StatusBadge tone="gray">종료</StatusBadge>
          </div>
        </section>
        <section className="card card-pad">
          <div className="card-head">
            <h3>주요 알림</h3>
            <span className="muted" style={{ fontSize: 12 }}>
              전체 보기
            </span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div className="alert alert-danger">온다 6호기 GPS 미수신 · 8분 전</div>
            <div className="alert alert-warning">수원역 노선 지연 발생 · 12분 전</div>
            <div className="alert alert-info">기흥역 배차 정상 복귀 · 21분 전</div>
          </div>
        </section>
      </div>
    </div>
  )
}

export function LiveDetailPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>운행 상태 상세 · 온다 3호기</h3>
          <StatusBadge tone="green">운행 중</StatusBadge>
        </div>
        <div className="grid grid-4">
          {[
            ['기사', '김기사'],
            ['노선', '기흥역 ↔ 캠퍼스'],
            ['현재 정류장', '도서관 앞'],
            ['마지막 GPS', '12초 전'],
            ['탑승 인원', '28명'],
            ['금일 운행 거리', '46.2km'],
            ['지연', '없음'],
            ['통신 상태', '정상'],
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
    </div>
  )
}

export function LiveSuspendPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>운행 중단 요청 처리</h3>
        </div>
        <div className="alert alert-warning" style={{ marginBottom: 12 }}>
          기사가 요청을 제출했습니다. 관리자가 요청을 승인합니다.
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label>차량</label>
            <input className="input" defaultValue="온다 5호기" readOnly />
          </div>
          <div className="field">
            <label>요청 사유</label>
            <input className="input" defaultValue="차량 점검 필요" readOnly />
          </div>
          <div className="field">
            <label>처리 결과</label>
            <select className="select" defaultValue="approve">
              <option value="approve">승인</option>
              <option value="reject">반려</option>
            </select>
          </div>
          <div className="field">
            <label>관리자 메모</label>
            <input className="input" placeholder="내부 메모를 입력하세요" />
          </div>
        </div>
        <div className="toolbar" style={{ marginTop: 16, justifyContent: 'flex-end' }}>
          <button className="btn btn-outline" type="button">
            취소
          </button>
          <button className="btn btn-primary" type="button">
            처리 완료
          </button>
        </div>
      </section>
    </div>
  )
}
