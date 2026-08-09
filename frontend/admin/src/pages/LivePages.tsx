import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import mapImg from '../assets/map.png'
import { fetchLiveVehicles, type LiveSnapshot, type LiveVehicle } from '../lib/liveApi'
import {
  decideSafeStop,
  fetchSafeStopRequests,
  type SafeStopRequest,
} from '../lib/safeStopApi'
import { SCHEDULE_ROUTE_OPTIONS } from '../data/mock'
import { StatusBadge } from '../components/ui/Form'

const empty: LiveSnapshot = {
  vehicles: [],
  stats: { ok: 0, none: 0, error: 0, total: 0, rate: 0, inProgress: 0, ended: 0, idle: 0, stopped: 0 },
}

function useLiveSnapshot(pollMs = 5_000) {
  const [snapshot, setSnapshot] = useState<LiveSnapshot>(empty)

  useEffect(() => {
    let alive = true
    const load = async () => {
      const data = await fetchLiveVehicles()
      if (alive) setSnapshot(data)
    }
    void load()
    const timer = window.setInterval(load, pollMs)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [pollMs])

  return snapshot
}

export function LivePage() {
  const snapshot = useLiveSnapshot()
  const [routeFilter, setRouteFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [query, setQuery] = useState('')

  const vehicles = useMemo(() => {
    return snapshot.vehicles.filter((row) => {
      if (routeFilter && row.routeName !== routeFilter) return false
      if (statusFilter && row.status !== statusFilter) return false
      if (query.trim()) {
        const q = query.trim().toLowerCase()
        const hay = `${row.vehicleName} ${row.driverName} ${row.driverId} ${row.routeName}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      return true
    })
  }, [snapshot.vehicles, routeFilter, statusFilter, query])

  const { stats } = snapshot
  const staleAlert = snapshot.vehicles.filter((v) => v.gpsKind === 'error' || v.gpsKind === 'none')

  return (
    <div className="page">
      <section className="card card-pad">
        <div className="toolbar">
          <select
            className="select"
            style={{ width: 160 }}
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
          <select
            className="select"
            style={{ width: 140 }}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">상태 전체</option>
            <option value="in_progress">운행 중</option>
            <option value="idle">대기</option>
            <option value="stopped">안전 정차</option>
            <option value="ended">종료</option>
          </select>
          <input
            className="input"
            style={{ width: 240 }}
            placeholder="차량 번호, 기사명 검색"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div style={{ flex: 1 }} />
          <Link className="btn btn-outline" to="/live/detail">
            운행 상세 보기
          </Link>
          <button className="btn btn-outline" type="button">
            기사에게 연락
          </button>
          <Link className="btn btn-primary" to="/live/suspend">
            안전 정차 요청
          </Link>
        </div>
      </section>

      <div className="grid" style={{ gridTemplateColumns: '1.2fr 1fr' }}>
        <section className="card card-pad">
          <div className="card-head">
            <h3>실시간 차량 위치</h3>
            <div className="toolbar" style={{ fontSize: 11 }}>
              <StatusBadge tone="green">운행</StatusBadge>
              <StatusBadge tone="blue">대기</StatusBadge>
              <StatusBadge tone="orange">안전 정차</StatusBadge>
              <StatusBadge tone="gray">종료</StatusBadge>
              <StatusBadge tone="red">GPS 오류</StatusBadge>
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
          <div className="toolbar" style={{ marginBottom: 10, fontSize: 12, flexWrap: 'wrap', gap: 6 }}>
            <StatusBadge tone="blue">전체 {stats.total}</StatusBadge>
            <StatusBadge tone="green">운행 중 {stats.inProgress}</StatusBadge>
            <StatusBadge tone="blue">대기 {stats.idle}</StatusBadge>
            <StatusBadge tone="orange">안전 정차 {stats.stopped ?? 0}</StatusBadge>
            <StatusBadge tone="gray">종료 {stats.ended}</StatusBadge>
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
                <th>갱신</th>
              </tr>
            </thead>
            <tbody>
              {vehicles.length === 0 ? (
                <tr>
                  <td colSpan={7} className="muted">
                    수신된 실시간 운행이 없습니다. 기사 앱에서 운행을 시작하면 여기에 표시됩니다.
                  </td>
                </tr>
              ) : (
                vehicles.map((row) => <LiveVehicleRow key={row.id} row={row} />)
              )}
            </tbody>
          </table>
          {staleAlert.length > 0 ? (
            <div className="alert alert-danger" style={{ marginTop: 12 }}>
              GPS 미수신·오류 차량: {staleAlert[0].vehicleName} ({staleAlert[0].driverName})
              {staleAlert.length > 1 ? ` 외 ${staleAlert.length - 1}대` : ''}
            </div>
          ) : null}
        </section>
      </div>

      <div className="grid grid-3">
        <section className="card card-pad">
          <h3>GPS 정상율</h3>
          <div style={{ fontSize: 32, fontWeight: 800, color: '#266ef4', margin: '12px 0' }}>
            {stats.total === 0 ? '—' : `${stats.rate}%`}
          </div>
          <div className="toolbar" style={{ fontSize: 12, flexWrap: 'wrap', gap: 6 }}>
            <StatusBadge tone="green">정상 {stats.ok}</StatusBadge>
            <StatusBadge tone="gray">미수신 {stats.none}</StatusBadge>
            <StatusBadge tone="red">오류 {stats.error}</StatusBadge>
          </div>
        </section>
        <section className="card card-pad">
          <h3>운행 상태 비율</h3>
          <div style={{ fontSize: 32, fontWeight: 800, margin: '12px 0' }}>{stats.total}대</div>
          <div className="toolbar" style={{ fontSize: 12, flexWrap: 'wrap', gap: 6 }}>
            <StatusBadge tone="green">운행 {stats.inProgress}</StatusBadge>
            <StatusBadge tone="blue">대기 {stats.idle}</StatusBadge>
            <StatusBadge tone="orange">안전 정차 {stats.stopped ?? 0}</StatusBadge>
            <StatusBadge tone="gray">종료 {stats.ended}</StatusBadge>
          </div>
        </section>
        <section className="card card-pad">
          <div className="card-head">
            <h3>주요 알림</h3>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {staleAlert.slice(0, 3).map((v) => (
              <div key={v.id} className={v.gpsKind === 'error' ? 'alert alert-danger' : 'alert alert-warning'}>
                {v.vehicleName} · {v.driverName} · GPS {v.gps} · {v.last}
              </div>
            ))}
            {staleAlert.length === 0 ? (
              <div className="alert alert-info">현재 GPS 이상 알림이 없습니다.</div>
            ) : null}
          </div>
        </section>
      </div>
    </div>
  )
}

function LiveVehicleRow({ row }: { row: LiveVehicle }) {
  return (
    <tr style={row.gpsKind === 'error' ? { background: '#fff5f5' } : undefined}>
      <td>{row.vehicleName}</td>
      <td>
        {row.driverName}
        <div className="muted" style={{ fontSize: 10 }}>
          {row.driverId}
        </div>
      </td>
      <td>{row.routeName}</td>
      <td>{row.stop}</td>
      <td>
        <StatusBadge tone={row.tone}>{row.statusLabel}</StatusBadge>
      </td>
      <td>{row.gps}</td>
      <td className="muted" style={{ fontSize: 12 }}>
        {row.last}
      </td>
    </tr>
  )
}

/** 오늘의 실시간 운행 목록 → 행 클릭 시 차량 상세 */
export function LiveDetailPage() {
  const snapshot = useLiveSnapshot()
  const navigate = useNavigate()

  return (
    <div className="page">
      <div className="toolbar" style={{ marginBottom: 4 }}>
        <button className="btn btn-ghost" type="button" style={{ height: 30 }} onClick={() => navigate('/live')}>
          ← 이전
        </button>
      </div>
      <section className="card card-pad">
        <div className="card-head">
          <h3>오늘의 운행 목록</h3>
          <StatusBadge tone="blue">{snapshot.stats.total}대</StatusBadge>
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
              <th>갱신</th>
            </tr>
          </thead>
          <tbody>
            {snapshot.vehicles.length === 0 ? (
              <tr>
                <td colSpan={7} className="muted">
                  오늘 표시할 운행이 없습니다.
                </td>
              </tr>
            ) : (
              snapshot.vehicles.map((row) => (
                <tr
                  key={row.id}
                  style={{
                    cursor: 'pointer',
                    ...(row.gpsKind === 'error' ? { background: '#fff5f5' } : {}),
                  }}
                  onClick={() => navigate(`/live/detail/${encodeURIComponent(row.operationId || row.id)}`)}
                >
                  <td>{row.vehicleName}</td>
                  <td>
                    {row.driverName}
                    <div className="muted" style={{ fontSize: 10 }}>
                      {row.driverId}
                    </div>
                  </td>
                  <td>{row.routeName}</td>
                  <td>{row.stop}</td>
                  <td>
                    <StatusBadge tone={row.tone}>{row.statusLabel}</StatusBadge>
                  </td>
                  <td>{row.gps}</td>
                  <td className="muted" style={{ fontSize: 12 }}>
                    {row.last}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>
    </div>
  )
}

/** 선택한 차량의 실시간 상세 */
export function LiveVehicleDetailPage() {
  const { operationId = '' } = useParams()
  const decodedId = decodeURIComponent(operationId)
  const snapshot = useLiveSnapshot()
  const navigate = useNavigate()
  const vehicle = snapshot.vehicles.find((v) => v.operationId === decodedId || v.id === decodedId)

  return (
    <div className="page">
      <div className="toolbar" style={{ marginBottom: 4 }}>
        <button className="btn btn-ghost" type="button" style={{ height: 30 }} onClick={() => navigate('/live/detail')}>
          ← 이전
        </button>
      </div>
      <section className="card card-pad">
        <div className="card-head">
          <h3>
            운행 상태 상세 · {vehicle?.vehicleName ?? '차량'} ({vehicle?.routeName ?? '-'})
          </h3>
          {vehicle ? <StatusBadge tone={vehicle.tone}>{vehicle.statusLabel}</StatusBadge> : null}
        </div>
        {!vehicle ? (
          <div className="muted" style={{ fontSize: 13 }}>
            해당 운행 정보를 찾을 수 없습니다.
          </div>
        ) : (
          <div className="grid grid-4">
            {[
              ['기사', `${vehicle.driverName} (${vehicle.driverId})`],
              ['노선', vehicle.routeName],
              ['차량', vehicle.vehicleName],
              ['운행 상태', vehicle.statusLabel],
              ['현재 위치', vehicle.stop],
              ['GPS', vehicle.gps],
              ['위도', vehicle.lat != null ? String(vehicle.lat) : '-'],
              ['경도', vehicle.lng != null ? String(vehicle.lng) : '-'],
              ['정확도', vehicle.accuracy != null ? `${Math.round(vehicle.accuracy)}m` : '-'],
              ['마지막 갱신', vehicle.last],
              ['배정 ID', vehicle.operationId],
              ['통신', vehicle.gpsKind === 'ok' ? '정상' : vehicle.gpsKind === 'error' ? '오류' : '미수신'],
            ].map(([k, v]) => (
              <div key={k} className="card card-pad" style={{ boxShadow: 'none' }}>
                <div className="muted" style={{ fontSize: 12 }}>
                  {k}
                </div>
                <div style={{ fontWeight: 700 }}>{v}</div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

export function LiveSuspendPage() {
  const navigate = useNavigate()
  const [requests, setRequests] = useState<SafeStopRequest[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const load = async () => {
    const rows = await fetchSafeStopRequests()
    setRequests(rows)
    setSelectedId((prev) => {
      if (prev && rows.some((r) => r.id === prev)) return prev
      return rows[0]?.id ?? null
    })
  }

  useEffect(() => {
    void load()
    const timer = window.setInterval(() => void load(), 5_000)
    return () => window.clearInterval(timer)
  }, [])

  const selected = requests.find((r) => r.id === selectedId) ?? null

  const onDecide = async (decision: 'continue' | 'stop') => {
    if (!selected || selected.decision !== 'pending') return
    setBusy(true)
    setMessage(null)
    const result = await decideSafeStop(selected.id, decision)
    setBusy(false)
    if (!result.ok) {
      setMessage(result.message || '처리 실패')
      return
    }
    setMessage(decision === 'continue' ? '계속 운행으로 처리했습니다.' : '운행 중단(안전 정차)으로 처리했습니다.')
    await load()
  }

  return (
    <div className="page">
      <div className="toolbar" style={{ marginBottom: 4 }}>
        <button className="btn btn-ghost" type="button" style={{ height: 30 }} onClick={() => navigate('/live')}>
          ← 이전
        </button>
      </div>
      <section className="card card-pad">
        <div className="card-head">
          <h3>안전 정차 요청</h3>
          <button className="btn btn-outline btn-xs" type="button" onClick={() => void load()} disabled={busy}>
            새로고침
          </button>
        </div>
        {message ? (
          <div className="alert alert-info" style={{ marginBottom: 12 }}>
            {message}
          </div>
        ) : null}
        <div className="grid" style={{ gridTemplateColumns: '1.1fr 1fr', gap: 16 }}>
          <div>
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>요청 시각</th>
                  <th>기사</th>
                  <th>차량</th>
                  <th>사유</th>
                  <th>상태</th>
                </tr>
              </thead>
              <tbody>
                {requests.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="muted">
                      접수된 안전 정차 요청이 없습니다.
                    </td>
                  </tr>
                ) : (
                  requests.map((row) => (
                    <tr
                      key={row.id}
                      style={{
                        cursor: 'pointer',
                        background: row.id === selectedId ? '#f5f8ff' : undefined,
                      }}
                      onClick={() => setSelectedId(row.id)}
                    >
                      <td>
                        {row.date} {row.requestedAt}
                      </td>
                      <td>
                        {row.driverName}
                        <div className="muted" style={{ fontSize: 10 }}>
                          {row.driverId}
                        </div>
                      </td>
                      <td>{row.vehicleName}</td>
                      <td>{row.reason}</td>
                      <td>
                        <StatusBadge
                          tone={
                            row.decision === 'pending'
                              ? 'orange'
                              : row.decision === 'stop' || row.decision === 'cancelled'
                                ? 'red'
                                : 'green'
                          }
                        >
                          {row.decision === 'pending'
                            ? '확인 대기'
                            : row.decision === 'stop'
                              ? '운행 중단'
                              : row.decision === 'cancelled'
                                ? '요청 취소'
                                : '계속 운행'}
                        </StatusBadge>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div>
            {!selected ? (
              <div className="muted">왼쪽에서 요청을 선택하세요.</div>
            ) : (
              <>
                <div className="grid grid-2" style={{ marginBottom: 12 }}>
                  {[
                    ['기사', `${selected.driverName} (${selected.driverId})`],
                    ['차량', selected.vehicleName],
                    ['노선', selected.routeName],
                    ['요청 사유', selected.reason],
                    ['요청 시각', `${selected.date} ${selected.requestedAt}`],
                    [
                      '처리 상태',
                      selected.decision === 'pending'
                        ? '확인 대기'
                        : selected.decision === 'stop'
                          ? '운행 중단'
                          : selected.decision === 'cancelled'
                            ? '요청 취소'
                            : '계속 운행',
                    ],
                  ].map(([k, v]) => (
                    <div key={k} className="field">
                      <label>{k}</label>
                      <input className="input" value={v} readOnly />
                    </div>
                  ))}
                </div>
                <div className="field" style={{ marginBottom: 16 }}>
                  <label>상세 사유</label>
                  <textarea
                    className="input"
                    readOnly
                    rows={5}
                    value={selected.detailReason?.trim() || '입력된 상세 사유가 없습니다.'}
                    style={{ resize: 'vertical', minHeight: 110, lineHeight: 1.5 }}
                  />
                </div>
                {selected.decision === 'pending' ? (
                  <div className="toolbar" style={{ justifyContent: 'flex-end', gap: 8 }}>
                    <button
                      className="btn btn-outline"
                      type="button"
                      disabled={busy}
                      onClick={() => void onDecide('continue')}
                    >
                      계속 운행
                    </button>
                    <button
                      className="btn btn-danger"
                      type="button"
                      disabled={busy}
                      onClick={() => void onDecide('stop')}
                    >
                      운행 중단
                    </button>
                  </div>
                ) : (
                  <div className="alert alert-info">이미 처리된 요청입니다. 차량 상태는 실시간 운행 목록에 반영됩니다.</div>
                )}
              </>
            )}
          </div>
        </div>
      </section>
    </div>
  )
}
