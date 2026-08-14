import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { RefreshCw } from 'lucide-react'
import {
  fetchLiveVehiclesList,
  enrichLiveVehiclesGps,
  applyLocationPatch,
  subscribeLiveUpdates,
  type LiveSnapshot,
  type LiveVehicle,
} from '../lib/liveApi'
import {
  decideSafeStop,
  fetchSafeStopRequests,
  type SafeStopRequest,
} from '../lib/safeStopApi'
import { markNotificationRead } from '../lib/adminNotificationsApi'
import { LiveVehiclesMap } from '../components/LiveVehiclesMap'
import { LIVE_MAP_ROUTES } from '../data/cityShuttleStops'
import { fetchRouteCatalog, type RouteCatalogItem } from '../lib/routesApi'
import { StatusBadge } from '../components/ui/Form'
import { ListPagination } from '../components/ui/ListPagination'
import '../styles/live.css'

/** "명지대역 셔틀 (18시 이후)" → base + suffix 두 줄 표시용 */
function splitRouteName(routeName: string): { base: string; suffix: string | null } {
  const m = routeName.trim().match(/^(.*?)\s*(\([^)]+\))\s*$/)
  if (!m) return { base: routeName, suffix: null }
  return { base: m[1].trim() || routeName, suffix: m[2] }
}

function RouteNameCell({ name }: { name: string }) {
  const { base, suffix } = splitRouteName(name)
  if (!suffix) return <>{base || '-'}</>
  return (
    <span className="live-route-name">
      <span className="live-route-name-base">{base}</span>
      <span className="live-route-name-suffix">{suffix}</span>
    </span>
  )
}

const empty: LiveSnapshot = {
  vehicles: [],
  stats: { ok: 0, none: 0, error: 0, total: 0, rate: 0, inProgress: 0, ended: 0, idle: 0, stopped: 0 },
}

const LIVE_POLL_MS = 30_000
const LIVE_LIST_PAGE_SIZE = 7
/** 페이지 번호 버튼에 한 번에 보이는 개수 */
const LIVE_PAGE_WINDOW = 10

function formatClock(d: Date) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function liveFingerprint(data: LiveSnapshot): string {
  const s = data.stats
  return [
    s.total,
    s.inProgress,
    s.idle,
    s.ended,
    s.stopped ?? 0,
    s.ok,
    s.none,
    s.error,
    data.vehicles.length,
    // updatedAt/last 는 시간이 지나며 바뀌므로 제외 — 매 폴링 전체 리렌더 방지
    data.vehicles
      .map(
        (v) =>
          `${v.id}:${v.status}:${v.gpsKind}:${v.lat ?? ''}:${v.lng ?? ''}:${v.routeName}`,
      )
      .join('|'),
  ].join('~')
}

function useLiveSnapshot(pollMs = LIVE_POLL_MS) {
  const [snapshot, setSnapshot] = useState<LiveSnapshot>(empty)
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const fingerprintRef = useRef('')
  /** Strict Mode remount / 연속 요청 시 최신 결과만 반영 */
  const requestIdRef = useRef(0)

  const applySnapshot = useCallback((data: LiveSnapshot) => {
    const next = liveFingerprint(data)
    if (fingerprintRef.current !== next) {
      fingerprintRef.current = next
      setSnapshot(data)
    }
    setLastUpdatedAt(new Date())
  }, [])

  const loadListThenGps = useCallback(async () => {
    const requestId = ++requestIdRef.current
    // 1) 목록·비율은 operations만으로 즉시
    const list = await fetchLiveVehiclesList()
    if (requestId !== requestIdRef.current) return
    applySnapshot(list)
    // 2) GPS는 이후에 붙임 (지도 마커) — 목록 표시를 막지 않음
    const enriched = await enrichLiveVehiclesGps(list)
    if (requestId !== requestIdRef.current) return
    applySnapshot(enriched)
  }, [applySnapshot])

  const refresh = useCallback(async () => {
    setRefreshing(true)
    try {
      await loadListThenGps()
    } finally {
      setRefreshing(false)
    }
  }, [loadListThenGps])

  useEffect(() => {
    let alive = true
    let statusDebounce: number | null = null

    const load = async () => {
      if (!alive) return
      try {
        await loadListThenGps()
      } catch (e) {
        console.warn('[live] load', e)
      }
    }

    void load()

    const unsub = subscribeLiveUpdates({
      onLocation: (row) => {
        if (!alive) return
        setSnapshot((prev) => {
          const next = applyLocationPatch(prev, row)
          if (next === prev) return prev
          fingerprintRef.current = liveFingerprint(next)
          return next
        })
        setLastUpdatedAt(new Date())
      },
      onOperationChange: () => {
        if (statusDebounce != null) window.clearTimeout(statusDebounce)
        statusDebounce = window.setTimeout(() => {
          void load()
        }, 700)
      },
    })

    const timer = window.setInterval(() => {
      void load()
    }, pollMs)

    return () => {
      alive = false
      // 진행 중 요청 무효화 — Strict Mode에서 첫 요청이 두 번째 mount를 막지 않음
      requestIdRef.current += 1
      unsub()
      window.clearInterval(timer)
      if (statusDebounce != null) window.clearTimeout(statusDebounce)
    }
  }, [pollMs, loadListThenGps])

  return { snapshot, lastUpdatedAt, refresh, refreshing }
}

export function LivePage() {
  const { snapshot, lastUpdatedAt, refresh, refreshing } = useLiveSnapshot()
  const [routeFilter, setRouteFilter] = useState('')
  const [mapRouteFilter, setMapRouteFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [query, setQuery] = useState('')
  const [listPage, setListPage] = useState(1)
  /** 목록에서 선택한 차량 — 지도에 해당 노선만 표시 (재클릭 시 해제) */
  const [selectedVehicleId, setSelectedVehicleId] = useState<string | null>(null)
  const [catalogRoutes, setCatalogRoutes] = useState<RouteCatalogItem[]>([])

  useEffect(() => {
    let alive = true
    void fetchRouteCatalog().then((rows) => {
      if (alive) setCatalogRoutes(rows)
    })
    return () => {
      alive = false
    }
  }, [])

  const liveRouteLayers = useMemo(() => {
    const fromDb = catalogRoutes
      .filter((r) => r.stops.length > 0)
      .map((r) => r.mapLayer)
    return fromDb.length ? fromDb : LIVE_MAP_ROUTES
  }, [catalogRoutes])

  const liveRouteFilterOptions = useMemo(
    () => liveRouteLayers.map((r) => r.name),
    [liveRouteLayers],
  )

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

  const selectedVehicle = useMemo(
    () => (selectedVehicleId ? vehicles.find((v) => v.id === selectedVehicleId) ?? null : null),
    [vehicles, selectedVehicleId],
  )

  useEffect(() => {
    if (selectedVehicleId && !vehicles.some((v) => v.id === selectedVehicleId)) {
      setSelectedVehicleId(null)
    }
  }, [vehicles, selectedVehicleId])

  const mapRoutes = useMemo(() => {
    if (mapRouteFilter) {
      return liveRouteLayers.filter((r) => r.name === mapRouteFilter)
    }
    if (!selectedVehicle?.routeName) return liveRouteLayers
    const matched = liveRouteLayers.filter((r) => r.name === selectedVehicle.routeName)
    return matched.length ? matched : liveRouteLayers
  }, [selectedVehicle, mapRouteFilter, liveRouteLayers])

  const mapVehicles = useMemo(() => {
    const byRoute = mapRouteFilter
      ? vehicles.filter((v) => v.routeName === mapRouteFilter)
      : vehicles
    if (!selectedVehicle) return byRoute
    return byRoute.filter((v) => v.id === selectedVehicle.id)
  }, [vehicles, selectedVehicle, mapRouteFilter])

  const toggleVehicleSelect = useCallback((id: string) => {
    setSelectedVehicleId((prev) => (prev === id ? null : id))
  }, [])

  const listPageCount = Math.max(1, Math.ceil(vehicles.length / LIVE_LIST_PAGE_SIZE))
  const safeListPage = Math.min(listPage, listPageCount)
  const pagedVehicles = useMemo(() => {
    const start = (safeListPage - 1) * LIVE_LIST_PAGE_SIZE
    return vehicles.slice(start, start + LIVE_LIST_PAGE_SIZE)
  }, [vehicles, safeListPage])

  useEffect(() => {
    setListPage(1)
  }, [routeFilter, statusFilter, query])

  useEffect(() => {
    if (listPage > listPageCount) setListPage(listPageCount)
  }, [listPage, listPageCount])

  const { stats } = snapshot
  const staleAlert = useMemo(
    () => snapshot.vehicles.filter((v) => v.gpsKind === 'error' || v.gpsKind === 'none'),
    [snapshot.vehicles],
  )
  const majorAlerts = useMemo(() => {
    return [...staleAlert]
      .sort((a, b) => {
        // 운행 중 이상 우선 → GPS 오류 우선 → 최근 갱신순
        const aRun = a.status === 'in_progress' ? 1 : 0
        const bRun = b.status === 'in_progress' ? 1 : 0
        if (aRun !== bRun) return bRun - aRun
        const aErr = a.gpsKind === 'error' ? 1 : 0
        const bErr = b.gpsKind === 'error' ? 1 : 0
        if (aErr !== bErr) return bErr - aErr
        return b.updatedAt - a.updatedAt
      })
      .slice(0, 3)
  }, [staleAlert])
  const lastUpdatedLabel = lastUpdatedAt ? formatClock(lastUpdatedAt) : '--:--:--'

  const gpsTotal = stats.ok + stats.none + stats.error
  const gpsPct = (n: number) => (gpsTotal <= 0 ? 0 : Math.round((n / gpsTotal) * 1000) / 10)
  const gpsOkPct = gpsPct(stats.ok)
  const gpsNonePct = gpsPct(stats.none)
  const gpsErrorPct = gpsPct(stats.error)
  const gpsDonutBg =
    gpsTotal <= 0
      ? 'conic-gradient(#e5e7eb 0 100%)'
      : `conic-gradient(#3fb46a 0 ${gpsOkPct}%, #fdac38 0 ${gpsOkPct + gpsNonePct}%, #eb4047 0 100%)`

  const opRunning = stats.inProgress
  /** 안전 정차 요청에서 기사가 정차 확인한 차량 */
  const opSafeStopped = stats.stopped ?? 0
  /** 운행 출발 전 대기 */
  const opWaiting = stats.idle
  const opEnded = stats.ended
  const opTotal = opRunning + opSafeStopped + opWaiting + opEnded
  const opPct = (n: number) => (opTotal <= 0 ? 0 : Math.round((n / opTotal) * 1000) / 10)
  const opRunPct = opPct(opRunning)
  const opStopPct = opPct(opSafeStopped)
  const opWaitPct = opPct(opWaiting)
  const opEndPct = opPct(opEnded)
  const opDonutBg =
    opTotal <= 0
      ? 'conic-gradient(#e5e7eb 0 100%)'
      : `conic-gradient(#3fb46a 0 ${opRunPct}%, #fdac38 0 ${opRunPct + opStopPct}%, #266ef4 0 ${opRunPct + opStopPct + opWaitPct}%, #c4c9d7 0 100%)`

  return (
    <div className="page">
      <section className="card card-pad">
        <div className="toolbar live-filter-toolbar">
          <select
            className="select live-filter-control"
            style={{ width: 160, height: 36 }}
            value={routeFilter}
            onChange={(e) => setRouteFilter(e.target.value)}
          >
            <option value="">노선 전체</option>
            {liveRouteFilterOptions.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <select
            className="select live-filter-control"
            style={{ width: 140, height: 36 }}
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
            className="input live-filter-control"
            style={{ width: 240, height: 36 }}
            placeholder="차량 번호, 기사명 검색"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div style={{ flex: 1 }} />
          <Link className="btn btn-outline live-filter-btn" to="/live/detail">
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

      <div className="grid live-map-list-grid" style={{ gridTemplateColumns: '1.2fr 1fr' }}>
        <section className="card card-pad live-map-card">
          <div className="live-map-block">
            <div className="live-map-head">
              <h3 className="live-map-title">실시간 차량 위치</h3>
              <select
                className="select live-map-route-select"
                value={mapRouteFilter}
                onChange={(e) => setMapRouteFilter(e.target.value)}
                aria-label="지도 표시 노선"
              >
                <option value="">전체 노선</option>
                {liveRouteLayers.map((route) => (
                  <option key={route.id} value={route.name}>
                    {route.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="live-status-row">
              <div className="live-status-legend" aria-label="차량 상태 범례">
                <span className="live-status-item">
                  <i style={{ background: '#22c55e' }} />
                  GPS 정상(초록)
                </span>
                <span className="live-status-item">
                  <i style={{ background: '#eb4047' }} />
                  GPS 끊김(빨간·최근좌표 있을 때)
                </span>
                <span className="live-status-item">
                  <i style={{ background: '#266ef4' }} />
                  안전 정차
                </span>
                <span className="live-status-item">
                  <i style={{ background: '#fdac38' }} />
                  대기
                </span>
              </div>
              <button
                type="button"
                className="live-update-meta"
                onClick={() => void refresh()}
                disabled={refreshing}
                aria-label="실시간 위치 새로고침"
                title="새로고침"
              >
                <span>마지막 업데이트 {lastUpdatedLabel}</span>
                <RefreshCw size={14} className={refreshing ? 'is-spinning' : undefined} />
              </button>
            </div>
            <div className="live-map-frame">
              <LiveVehiclesMap vehicles={mapVehicles} routes={mapRoutes} height={400} />
            </div>
            <div className="live-route-legend" aria-label="노선 범례">
              {mapRoutes.map((route) => (
                <span key={route.id} className="live-route-legend-item">
                  <span className="live-route-legend-line" style={{ background: route.color }} />
                  {route.name}
                </span>
              ))}
            </div>
          </div>
        </section>

        <section className="card card-pad live-vehicle-list-card">
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
          <table className="data-table live-vehicle-table">
            <thead>
              <tr>
                <th>순번</th>
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
                  <td colSpan={8} className="muted">
                    오늘 운행 데이터가 없습니다. operations에 당일 배차가 있으면 목록과 아래 비율이
                    자동으로 갱신됩니다.
                  </td>
                </tr>
              ) : (
                <>
                  {pagedVehicles.map((row, idx) => (
                    <LiveVehicleRow
                      key={row.id}
                      row={row}
                      no={(safeListPage - 1) * LIVE_LIST_PAGE_SIZE + idx + 1}
                      selected={row.id === selectedVehicleId}
                      onSelect={() => toggleVehicleSelect(row.id)}
                    />
                  ))}
                  {Array.from({
                    length: Math.max(0, LIVE_LIST_PAGE_SIZE - pagedVehicles.length),
                  }).map((_, i) => (
                    <tr key={`pad-${i}`} className="live-vehicle-row-pad" aria-hidden>
                      <td colSpan={8}>&nbsp;</td>
                    </tr>
                  ))}
                </>
              )}
            </tbody>
          </table>
          <div className="live-list-footer">
            <ListPagination
              total={vehicles.length}
              page={safeListPage}
              pageSize={LIVE_LIST_PAGE_SIZE}
              onPageChange={setListPage}
              ariaLabel="운행 목록 페이지"
              windowSize={LIVE_PAGE_WINDOW}
            />
            {staleAlert.length > 0 ? (
              <div className="alert alert-danger live-list-gps-alert">
                GPS 미수신·오류 차량: {staleAlert[0].vehicleName} ({staleAlert[0].driverName})
                {staleAlert.length > 1 ? ` 외 ${staleAlert.length - 1}대` : ''}
              </div>
            ) : (
              <div className="live-list-gps-alert-slot" aria-hidden />
            )}
          </div>
        </section>
      </div>

      <div className="grid grid-3">
        <section className="card card-pad live-ratio-card">
          <h3 className="live-map-title">GPS 정상률</h3>
          <div className="live-ratio-body">
            <div className="live-donut" style={{ background: gpsDonutBg }} aria-hidden>
              <div className="live-donut-hole">
                <span className="live-donut-kicker">전체</span>
                <strong className="live-donut-value">
                  {gpsTotal === 0 ? '—' : `${stats.rate}%`}
                </strong>
              </div>
            </div>
            <ul className="live-ratio-legend">
              <li>
                <i className="live-ratio-swatch is-ok" />
                <span className="live-ratio-name">정상</span>
                <span className="live-ratio-stat">
                  {stats.ok}대 ({gpsOkPct}%)
                </span>
              </li>
              <li>
                <i className="live-ratio-swatch is-none" />
                <span className="live-ratio-name">미수신</span>
                <span className="live-ratio-stat">
                  {stats.none}대 ({gpsNonePct}%)
                </span>
              </li>
              <li>
                <i className="live-ratio-swatch is-error" />
                <span className="live-ratio-name">오류</span>
                <span className="live-ratio-stat">
                  {stats.error}대 ({gpsErrorPct}%)
                </span>
              </li>
            </ul>
          </div>
          <div className="live-ratio-foot">전체 {gpsTotal}대 기준</div>
        </section>
        <section className="card card-pad live-ratio-card">
          <h3 className="live-map-title">운행 상태 비율</h3>
          <div className="live-ratio-body">
            <div className="live-donut" style={{ background: opDonutBg }} aria-hidden>
              <div className="live-donut-hole">
                <span className="live-donut-kicker">전체</span>
                <strong className="live-donut-value">{opTotal}대</strong>
              </div>
            </div>
            <ul className="live-ratio-legend">
              <li>
                <i className="live-ratio-swatch is-running" />
                <span className="live-ratio-name">운행 중</span>
                <span className="live-ratio-stat">
                  {opRunning}대 ({opRunPct}%)
                </span>
              </li>
              <li>
                <i className="live-ratio-swatch is-at-stop" />
                <span className="live-ratio-name">정차 중</span>
                <span className="live-ratio-stat">
                  {opSafeStopped}대 ({opStopPct}%)
                </span>
              </li>
              <li>
                <i className="live-ratio-swatch is-waiting" />
                <span className="live-ratio-name">대기 중</span>
                <span className="live-ratio-stat">
                  {opWaiting}대 ({opWaitPct}%)
                </span>
              </li>
              <li>
                <i className="live-ratio-swatch is-ended" />
                <span className="live-ratio-name">운행 종료</span>
                <span className="live-ratio-stat">
                  {opEnded}대 ({opEndPct}%)
                </span>
              </li>
            </ul>
          </div>
          <div className="live-ratio-foot">전체 {opTotal}대 기준</div>
        </section>
        <section className="card card-pad">
          <div className="card-head">
            <h3>주요 알림</h3>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {majorAlerts.map((v) => (
              <div key={v.id} className={v.gpsKind === 'error' ? 'alert alert-danger' : 'alert alert-warning'}>
                {v.vehicleName} · {v.driverName} · GPS {v.gps} · {v.last}
              </div>
            ))}
            {majorAlerts.length === 0 ? (
              <div className="alert alert-success">현재 GPS 이상 알림이 없습니다.</div>
            ) : null}
          </div>
        </section>
      </div>
    </div>
  )
}

function LiveVehicleRow({
  row,
  no,
  selected,
  onSelect,
}: {
  row: LiveVehicle
  no: number
  selected?: boolean
  onSelect?: () => void
}) {
  return (
    <tr
      className={`live-vehicle-row${selected ? ' is-selected' : ''}${row.gpsKind === 'error' ? ' is-gps-error' : ''}`}
      onClick={onSelect}
      aria-selected={selected || undefined}
      title={selected ? '다시 클릭하면 노선 필터 해제' : '클릭하면 해당 노선만 지도에 표시'}
    >
      <td>{no}</td>
      <td title={row.vehicleName}>{row.vehicleName}</td>
      <td title={`${row.driverName} ${row.driverId}`}>
        {row.driverName}
        <div className="muted" style={{ fontSize: 10, overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {row.driverId}
        </div>
      </td>
      <td title={row.routeName} className="col-route">
        <RouteNameCell name={row.routeName} />
      </td>
      <td title={row.stop}>{row.stop}</td>
      <td className="col-status">
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
  const { snapshot } = useLiveSnapshot()
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
  const { snapshot } = useLiveSnapshot()
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
  const [searchParams] = useSearchParams()
  const focusId = searchParams.get('id')
  const [requests, setRequests] = useState<SafeStopRequest[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(focusId)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const load = async () => {
    const rows = await fetchSafeStopRequests()
    setRequests(rows)
    setSelectedId((prev) => {
      const preferred = focusId || prev
      if (preferred && rows.some((r) => r.id === preferred)) return preferred
      const firstPending = rows.find((r) => r.decision === 'pending')
      return firstPending?.id ?? rows[0]?.id ?? null
    })
  }

  useEffect(() => {
    void load()
    const timer = window.setInterval(() => void load(), 5_000)
    return () => window.clearInterval(timer)
  }, [focusId])

  useEffect(() => {
    if (focusId) setSelectedId(focusId)
  }, [focusId])

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
    // 알림 뱃지에서도 읽음 처리
    void markNotificationRead(`safe-stop-${selected.id}`)
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
