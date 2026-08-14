import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import {
  Bell,
  Bus,
  CalendarDays,
  IdCard,
  MapPin,
  Phone,
  Plus,
  Route as RouteIcon,
  Search,
  TriangleAlert,
  UserPlus,
  UserRound,
  Wrench,
} from 'lucide-react'
import {
  drivers as mockDrivers,
  maintenances,
  routes as mockRoutes,
  vehicles as mockVehicles,
} from '../data/mock'
import { createRoute, fetchRouteCatalog, fetchStopCatalog, upsertStop, type StopCatalogItem } from '../lib/routesApi'
import { StatusBadge } from '../components/ui/Form'
import '../styles/extra-pages.css'

const extraDrivers = mockDrivers.map((d, i) => ({
  ...d,
  bus: `버스 ${i + 1}호차`,
  route: '-',
}))

const mockNotifications = [
  {
    id: 'n1',
    category: 'GPS',
    title: '온다 6호차 GPS 미수신',
    body: '죽전역 노선 차량의 위치 신호가 32분째 수신되지 않습니다.',
    time: '방금 전',
    unread: true,
    tone: 'red' as const,
    href: '/live/detail',
  },
  {
    id: 'n2',
    category: '제보',
    title: '신규 제보 3건 접수',
    body: '만석·대기줄 관련 제보가 처리 대기 상태입니다.',
    time: '12분 전',
    unread: true,
    tone: 'orange' as const,
    href: '/reports',
  },
  {
    id: 'n3',
    category: '정비',
    title: '정기 점검 일정 알림',
    body: '72버 1234 차량의 정기점검이 2일 후 예정되어 있습니다.',
    time: '1시간 전',
    unread: true,
    tone: 'orange' as const,
    href: '/vehicles/maintenance/detail',
  },
]

const scheduleExceptions = [
  {
    date: '2026.08.14',
    day: '금',
    reason: '개교기념일',
    route: '전체 노선',
    action: '운행 축소',
    status: '예정',
    note: '첫차·막차 유지, 중간 배차 30분 간격으로 조정',
  },
  {
    date: '2026.08.20',
    day: '목',
    reason: '기상악화(폭우)',
    route: '기흥역 통학버스',
    action: '임시 중단',
    status: '예정',
    note: '안전 운행 불가 시 학생 앱 긴급 공지와 함께 중단',
  },
]

function Crumb({ items }: { items: { label: string; to?: string }[] }) {
  return (
    <nav className="extra-crumb" aria-label="breadcrumb">
      {items.map((item, i) => (
        <span key={`${item.label}-${i}`} style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          {i > 0 ? <span>/</span> : null}
          {item.to ? <Link to={item.to}>{item.label}</Link> : <span>{item.label}</span>}
        </span>
      ))}
    </nav>
  )
}

/** 헤더 벨 → 알림 센터 */
export function NotificationsPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState(mockNotifications)
  const unread = items.filter((n) => n.unread).length

  return (
    <div className="page extra-page">
      <div className="extra-kpis">
        {[
          { label: '전체 알림', value: String(items.length), unit: '건', tone: 'blue', icon: Bell },
          { label: '미확인', value: String(unread), unit: '건', tone: 'red', icon: TriangleAlert },
          { label: 'GPS·운행', value: '2', unit: '건', tone: 'orange', icon: Bus },
          { label: '제보·정비', value: '2', unit: '건', tone: 'purple', icon: Wrench },
        ].map((kpi) => {
          const Icon = kpi.icon
          return (
            <div key={kpi.label} className="extra-kpi">
              <div className={`extra-kpi-icon ${kpi.tone}`}>
                <Icon size={18} />
              </div>
              <div>
                <div className="label">{kpi.label}</div>
                <div className="value">
                  {kpi.value}
                  <em>{kpi.unit}</em>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <Bell size={17} />
            알림 센터
          </h3>
          <button
            className="btn btn-outline btn-xs"
            type="button"
            onClick={() => setItems((prev) => prev.map((n) => ({ ...n, unread: false })))}
          >
            모두 읽음
          </button>
        </div>
        <ul className="notif-list">
          {items.map((n) => (
            <li key={n.id}>
              <button
                type="button"
                className={`notif-item${n.unread ? ' unread' : ''}`}
                onClick={() => {
                  setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, unread: false } : x)))
                  navigate(n.href)
                }}
              >
                <span className={`notif-dot ${n.tone}`} />
                <div>
                  <strong>{n.title}</strong>
                  <p>{n.body}</p>
                </div>
                <div className="notif-meta">
                  <span className="notif-cat">{n.category}</span>
                  <time>{n.time}</time>
                </div>
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}

/** 정류장 관리 */
export function StopsPage() {
  const [query, setQuery] = useState('')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [catalog, setCatalog] = useState<StopCatalogItem[] | null>(null)

  useEffect(() => {
    void fetchStopCatalog().then(setCatalog)
  }, [])

  const list = useMemo(() => {
    const rows = catalog ?? []
    if (!query.trim()) return rows
    const q = query.trim().toLowerCase()
    return rows.filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        s.routes.toLowerCase().includes(q) ||
        `${s.lat} ${s.lng}`.includes(q),
    )
  }, [catalog, query])

  useEffect(() => {
    if (!list.length) {
      setSelectedId(null)
      return
    }
    if (!selectedId || !list.some((s) => s.id === selectedId)) setSelectedId(list[0].id)
  }, [list, selectedId])

  const detail = list.find((s) => s.id === selectedId) ?? list[0]
  const linkedRouteCount = new Set((catalog ?? []).flatMap((s) => s.routeIds)).size
  const coordPct =
    catalog && catalog.length
      ? Math.round((catalog.filter((s) => Number.isFinite(s.lat) && Number.isFinite(s.lng)).length / catalog.length) * 100)
      : 0

  return (
    <div className="page extra-page">
      <Crumb items={[{ label: '노선 관리', to: '/routes' }, { label: '정류장 관리' }]} />
      <div className="extra-kpis">
        {[
          { label: '전체 정류장', value: String(catalog?.length ?? 0), unit: '개소', tone: 'blue', icon: MapPin },
          { label: '연결 노선', value: String(linkedRouteCount), unit: '개', tone: 'green', icon: Bus },
          { label: '목록 표시', value: String(list.length), unit: '개소', tone: 'orange', icon: RouteIcon },
          { label: '좌표 등록', value: String(coordPct), unit: '%', tone: 'purple', icon: MapPin },
        ].map((kpi) => {
          const Icon = kpi.icon
          return (
            <div key={kpi.label} className="extra-kpi">
              <div className={`extra-kpi-icon ${kpi.tone}`}>
                <Icon size={18} />
              </div>
              <div>
                <div className="label">{kpi.label}</div>
                <div className="value">
                  {kpi.value}
                  <em>{kpi.unit}</em>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="extra-split">
        <section className="extra-panel">
          <div className="extra-panel-head">
            <h3>
              <MapPin size={17} />
              정류장 목록
            </h3>
            <Link className="btn btn-primary btn-xs" to="/stops/new">
              <Plus size={14} />
              정류장 등록
            </Link>
          </div>
          <div className="extra-toolbar">
            <label className="extra-search">
              <Search size={14} />
              <input
                className="input"
                placeholder="정류장명, 노선, 좌표 검색"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                }}
              />
            </label>
          </div>
          <table className="data-table dense">
            <thead>
              <tr>
                <th>정류장명</th>
                <th>이용 노선</th>
                <th>좌표</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {catalog === null ? (
                <tr>
                  <td colSpan={4} className="muted">
                    불러오는 중…
                  </td>
                </tr>
              ) : null}
              {catalog && !list.length ? (
                <tr>
                  <td colSpan={4} className="muted">
                    등록된 정류장이 없습니다.
                  </td>
                </tr>
              ) : null}
              {list.map((row) => (
                <tr
                  key={row.id}
                  className={row.id === detail?.id ? 'selected' : undefined}
                  style={{ cursor: 'pointer' }}
                  onClick={() => setSelectedId(row.id)}
                >
                  <td style={{ fontWeight: 700 }}>{row.name}</td>
                  <td>{row.routes}</td>
                  <td>
                    {row.lat}, {row.lng}
                  </td>
                  <td>
                    <Link
                      className="btn btn-outline btn-xs"
                      to={`/stops/new?id=${encodeURIComponent(row.id)}`}
                      onClick={(e) => e.stopPropagation()}
                    >
                      수정
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="extra-panel">
          <div className="extra-panel-head">
            <h3>
              <MapPin size={17} />
              정류장 상세
            </h3>
          </div>
          {detail ? (
            <>
              <div className="extra-detail-cards" style={{ gridTemplateColumns: '1fr 1fr' }}>
                {[
                  ['정류장명', detail.name],
                  ['이용 노선', detail.routes],
                  ['위도', String(detail.lat)],
                  ['경도', String(detail.lng)],
                ].map(([k, v]) => (
                  <div key={k} className="extra-detail-card">
                    <div className="muted">{k}</div>
                    <strong>{v}</strong>
                  </div>
                ))}
              </div>
              <div className="extra-actions" style={{ borderTop: 'none', paddingTop: 0, marginTop: 0 }}>
                <Link className="btn btn-outline btn-xs" to="/routes">
                  노선으로
                </Link>
                <Link className="btn btn-primary btn-xs" to={`/stops/new?id=${encodeURIComponent(detail.id)}`}>
                  수정하기
                </Link>
              </div>
            </>
          ) : (
            <p className="muted">{catalog === null ? '불러오는 중…' : '검색 결과가 없습니다.'}</p>
          )}
        </section>
      </div>
    </div>
  )
}

/** 정류장 등록/수정 */
export function StopFormPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const editId = params.get('id')
  const presetRouteId = params.get('routeId') ?? ''
  const [stops, setStops] = useState<StopCatalogItem[] | null>(null)
  const [routes, setRoutes] = useState<{ id: string; name: string }[]>([])
  const [name, setName] = useState('')
  const [lat, setLat] = useState('')
  const [lng, setLng] = useState('')
  const [routeId, setRouteId] = useState(presetRouteId)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    void Promise.all([fetchStopCatalog(), fetchRouteCatalog()]).then(([stopRows, routeRows]) => {
      setStops(stopRows)
      setRoutes(routeRows.map((r) => ({ id: r.id, name: r.name })))
    })
  }, [])

  const existing = stops?.find((s) => s.id === editId) ?? null

  useEffect(() => {
    if (!existing) return
    setName(existing.name)
    setLat(String(existing.lat))
    setLng(String(existing.lng))
    if (!presetRouteId) setRouteId(existing.routeIds[0] ?? '')
  }, [existing, presetRouteId])

  const onSubmit = async () => {
    setSaving(true)
    const result = await upsertStop({
      id: existing?.id,
      name,
      lat: Number(lat),
      lng: Number(lng),
      routeId: routeId || undefined,
    })
    setSaving(false)
    if (!result.ok) {
      window.alert(result.message ?? '저장에 실패했습니다.')
      return
    }
    navigate('/stops')
  }

  const isEdit = Boolean(existing)

  return (
    <div className="page extra-page">
      <Crumb
        items={[
          { label: '정류장 관리', to: '/stops' },
          { label: isEdit ? '정류장 수정' : '정류장 등록' },
        ]}
      />
      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <MapPin size={17} />
            {isEdit ? '정류장 수정' : '정류장 등록'}
          </h3>
        </div>
        <div className="extra-form-grid">
          <div className="field">
            <label>
              정류장명<span className="req">*</span>
            </label>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="예: 기흥역 5번 출구" />
          </div>
          <div className="field">
            <label>이용 노선</label>
            <select className="select" value={routeId} onChange={(e) => setRouteId(e.target.value)}>
              <option value="">연결 안 함</option>
              {routes.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>
              위도<span className="req">*</span>
            </label>
            <input className="input" value={lat} onChange={(e) => setLat(e.target.value)} placeholder="37.2754" />
          </div>
          <div className="field">
            <label>
              경도<span className="req">*</span>
            </label>
            <input className="input" value={lng} onChange={(e) => setLng(e.target.value)} placeholder="127.1159" />
          </div>
        </div>
        <div className="extra-actions">
          <button className="btn btn-outline" type="button" onClick={() => navigate('/stops')}>
            취소
          </button>
          <button className="btn btn-primary" type="button" onClick={() => void onSubmit()} disabled={saving}>
            {saving ? '저장 중…' : isEdit ? '저장' : '등록'}
          </button>
        </div>
      </section>
    </div>
  )
}

/** 기사 계정 관리 */
export function DriversPage() {
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState(0)

  const list = useMemo(() => {
    if (!query.trim()) return extraDrivers
    const q = query.trim().toLowerCase()
    return extraDrivers.filter(
      (d) =>
        d.name.toLowerCase().includes(q) ||
        d.email.toLowerCase().includes(q) ||
        d.bus.toLowerCase().includes(q) ||
        d.phone.includes(q),
    )
  }, [query])

  const detail = list[selected] ?? list[0]

  function statusTone(status: string): 'green' | 'blue' | 'gray' | 'red' | 'orange' {
    if (status === '운행 중') return 'blue'
    if (status === '운행 가능') return 'green'
    if (status === 'GPS 이상') return 'red'
    if (status === '휴무') return 'gray'
    return 'orange'
  }

  return (
    <div className="page extra-page">
      <Crumb items={[{ label: '사용자 관리', to: '/users' }, { label: '기사 계정 관리' }]} />
      <div className="extra-kpis">
        {[
          { label: '전체 기사', value: String(extraDrivers.length), unit: '명', tone: 'blue', icon: UserRound },
          { label: '운행 중', value: String(extraDrivers.filter((d) => d.status === '운행 중').length), unit: '명', tone: 'green', icon: Bus },
          { label: '운행 가능', value: String(extraDrivers.filter((d) => d.status === '운행 가능').length), unit: '명', tone: 'orange', icon: IdCard },
          { label: '이상/휴무', value: String(extraDrivers.filter((d) => d.status === '휴무' || d.status === 'GPS 이상').length), unit: '명', tone: 'red', icon: TriangleAlert },
        ].map((kpi) => {
          const Icon = kpi.icon
          return (
            <div key={kpi.label} className="extra-kpi">
              <div className={`extra-kpi-icon ${kpi.tone}`}>
                <Icon size={18} />
              </div>
              <div>
                <div className="label">{kpi.label}</div>
                <div className="value">
                  {kpi.value}
                  <em>{kpi.unit}</em>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="extra-split">
        <section className="extra-panel">
          <div className="extra-panel-head">
            <h3>
              <UserRound size={17} />
              기사 목록
            </h3>
            <Link className="btn btn-primary btn-xs" to="/drivers/new">
              <Plus size={14} />
              기사 계정 생성
            </Link>
          </div>
          <div className="extra-toolbar">
            <label className="extra-search">
              <Search size={14} />
              <input
                className="input"
                placeholder="이름, 이메일, 차량, 연락처 검색"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                  setSelected(0)
                }}
              />
            </label>
            <Link className="btn btn-outline btn-xs" to="/drivers/contact">
              <Phone size={13} />
              기사 연락
            </Link>
          </div>
          <table className="data-table dense">
            <thead>
              <tr>
                <th>이름</th>
                <th>배정 차량</th>
                <th>노선</th>
                <th>상태</th>
                <th>최근 운행</th>
              </tr>
            </thead>
            <tbody>
              {list.map((row, idx) => (
                <tr
                  key={row.email}
                  className={idx === selected ? 'selected' : undefined}
                  style={{ cursor: 'pointer' }}
                  onClick={() => setSelected(idx)}
                >
                  <td style={{ fontWeight: 700 }}>{row.name}</td>
                  <td>{row.bus}</td>
                  <td>{row.route}</td>
                  <td>
                    <StatusBadge tone={statusTone(row.status)}>{row.status}</StatusBadge>
                  </td>
                  <td>{row.lastTrip}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="extra-panel">
          <div className="extra-panel-head">
            <h3>
              <IdCard size={17} />
              기사 상세
            </h3>
          </div>
          {detail ? (
            <>
              <div className="extra-detail-cards" style={{ gridTemplateColumns: '1fr 1fr' }}>
                {[
                  ['이름', detail.name],
                  ['상태', detail.status],
                  ['이메일', detail.email],
                  ['연락처', detail.phone],
                  ['배정 차량', detail.bus],
                  ['담당 노선', detail.route],
                ].map(([k, v]) => (
                  <div key={k} className="extra-detail-card">
                    <div className="muted">{k}</div>
                    <strong>{v}</strong>
                  </div>
                ))}
              </div>
              <div className="extra-actions" style={{ borderTop: 'none', paddingTop: 0, marginTop: 0 }}>
                <Link className="btn btn-outline btn-xs" to="/drivers/contact">
                  <Phone size={13} />
                  연락하기
                </Link>
                <Link className="btn btn-primary btn-xs" to={`/drivers/new?edit=${encodeURIComponent(detail.email)}`}>
                  수정
                </Link>
              </div>
            </>
          ) : null}
        </section>
      </div>
    </div>
  )
}

/** 기사 계정 생성/수정 */
export function DriverFormPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const editEmail = params.get('edit')
  const existing = extraDrivers.find((d) => d.email === editEmail)

  return (
    <div className="page extra-page">
      <Crumb
        items={[
          { label: '기사 계정 관리', to: '/drivers' },
          { label: existing ? '기사 수정' : '기사 계정 생성' },
        ]}
      />
      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <UserPlus size={17} />
            {existing ? '기사 정보 수정' : '기사 계정 생성'}
          </h3>
        </div>
        <div className="extra-form-grid">
          <div className="field">
            <label>
              이름<span className="req">*</span>
            </label>
            <input className="input" defaultValue={existing?.name ?? ''} placeholder="기사 이름" />
          </div>
          <div className="field">
            <label>
              연락처<span className="req">*</span>
            </label>
            <input className="input" defaultValue={existing?.phone ?? ''} placeholder="010-0000-0000" />
          </div>
          <div className="field">
            <label>
              이메일<span className="req">*</span>
            </label>
            <input className="input" defaultValue={existing?.email ?? ''} placeholder="driver@onda.local" />
          </div>
          <div className="field">
            <label>배정 차량</label>
            <select className="select" defaultValue={existing?.bus ?? mockVehicles[0]?.bus}>
              {mockVehicles.map((v) => (
                <option key={v.plate}>{v.bus}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>담당 노선</label>
            <select className="select" defaultValue={existing?.route ?? '15-1'}>
              <option>15-1</option>
              <option>5-2</option>
              <option>12-1</option>
              <option>7-1</option>
              <option>9-2</option>
            </select>
          </div>
          <div className="field">
            <label>상태</label>
            <select className="select" defaultValue={existing?.status ?? '운행 가능'}>
              <option>운행 가능</option>
              <option>운행 중</option>
              <option>휴무</option>
              <option>GPS 이상</option>
            </select>
          </div>
        </div>
        <div className="extra-actions">
          <button className="btn btn-outline" type="button" onClick={() => navigate('/drivers')}>
            취소
          </button>
          <button className="btn btn-primary" type="button" onClick={() => navigate('/drivers')}>
            {existing ? '저장' : '계정 생성'}
          </button>
        </div>
      </section>
    </div>
  )
}

/** 기사에게 연락 */
export function DriverContactPage() {
  return (
    <div className="page extra-page">
      <Crumb items={[{ label: '실시간 운행', to: '/live' }, { label: '기사에게 연락' }]} />
      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <Phone size={17} />
            기사에게 연락
          </h3>
          <Link className="btn btn-outline btn-xs" to="/drivers">
            기사 목록
          </Link>
        </div>
        <div className="driver-contact-card">
          {extraDrivers.map((d) => (
            <div key={d.email} className="driver-contact-row">
              <div>
                <strong>
                  {d.name} · {d.bus}
                </strong>
                <p>
                  {d.route} · {d.status} · {d.phone}
                </p>
              </div>
              <div className="driver-contact-actions">
                <a className="btn btn-outline btn-xs" href={`tel:${d.phone.replace(/-/g, '')}`}>
                  전화
                </a>
                <a className="btn btn-primary btn-xs" href={`sms:${d.phone.replace(/-/g, '')}`}>
                  문자
                </a>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}

/** 예외 일정 등록/상세 */
export function ExceptionSchedulePage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const date = params.get('date')
  const existing = scheduleExceptions.find((e) => e.date === date) ?? (date ? null : scheduleExceptions[0])
  const isCreate = params.get('mode') === 'new' || !existing

  return (
    <div className="page extra-page">
      <Crumb
        items={[
          { label: '오늘의 운행', to: '/schedules' },
          { label: isCreate ? '예외 일정 등록' : '예외 일정 상세' },
        ]}
      />
      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <CalendarDays size={17} />
            {isCreate ? '예외 일정 등록' : '예외 일정 상세'}
          </h3>
          {!isCreate ? (
            <Link className="btn btn-outline btn-xs" to="/schedules/exception?mode=new">
              <Plus size={13} />
              새 예외 등록
            </Link>
          ) : null}
        </div>
        <div className="extra-form-grid">
          <div className="field">
            <label>
              일자<span className="req">*</span>
            </label>
            <input className="input" defaultValue={existing?.date ?? ''} placeholder="2026.08.14" />
          </div>
          <div className="field">
            <label>요일</label>
            <input className="input" defaultValue={existing?.day ?? ''} placeholder="금" />
          </div>
          <div className="field">
            <label>
              사유<span className="req">*</span>
            </label>
            <input className="input" defaultValue={existing?.reason ?? ''} placeholder="개교기념일, 기상악화 등" />
          </div>
          <div className="field">
            <label>
              적용 노선<span className="req">*</span>
            </label>
            <select className="select" defaultValue={existing?.route ?? '전체 노선'}>
              <option>전체 노선</option>
              {mockRoutes.map((r) => (
                <option key={r.name}>{r.name}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>
              조치<span className="req">*</span>
            </label>
            <select className="select" defaultValue={existing?.action ?? '운행 축소'}>
              <option>운행 축소</option>
              <option>임시 중단</option>
              <option>우회 운행</option>
              <option>시간 변경</option>
            </select>
          </div>
          <div className="field">
            <label>상태</label>
            <select className="select" defaultValue={existing?.status ?? '예정'}>
              <option>예정</option>
              <option>적용 중</option>
              <option>종료</option>
            </select>
          </div>
          <div className="field full">
            <label>비고 / 안내</label>
            <textarea className="textarea" rows={4} defaultValue={existing?.note ?? ''} placeholder="학생 앱 안내 문구에 반영할 내용" />
          </div>
        </div>
        <div className="extra-actions">
          <button className="btn btn-outline" type="button" onClick={() => navigate('/schedules')}>
            목록으로
          </button>
          <button className="btn btn-primary" type="button" onClick={() => navigate('/schedules')}>
            {isCreate ? '등록' : '저장'}
          </button>
        </div>
      </section>
    </div>
  )
}

/** 사용자 추가 */
export function UserCreatePage() {
  const navigate = useNavigate()

  return (
    <div className="page extra-page">
      <Crumb items={[{ label: '사용자 관리', to: '/users' }, { label: '사용자 추가' }]} />
      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <UserPlus size={17} />
            사용자 추가
          </h3>
        </div>
        <div className="extra-form-grid">
          <div className="field">
            <label>
              아이디<span className="req">*</span>
            </label>
            <input className="input" placeholder="로그인 아이디" />
          </div>
          <div className="field">
            <label>
              이름<span className="req">*</span>
            </label>
            <input className="input" placeholder="이름" />
          </div>
          <div className="field">
            <label>
              이메일<span className="req">*</span>
            </label>
            <input className="input" type="email" placeholder="email@mju.ac.kr" />
          </div>
          <div className="field">
            <label>
              역할<span className="req">*</span>
            </label>
            <select className="select" defaultValue="운영자">
              <option>관리자</option>
              <option>운영자</option>
              <option>일반 사용자</option>
            </select>
          </div>
          <div className="field">
            <label>초기 비밀번호</label>
            <input className="input" type="password" placeholder="임시 비밀번호" />
          </div>
          <div className="field">
            <label>상태</label>
            <select className="select" defaultValue="활성">
              <option>활성</option>
              <option>비활성</option>
            </select>
          </div>
        </div>
        <div className="extra-actions">
          <button className="btn btn-outline" type="button" onClick={() => navigate('/users')}>
            취소
          </button>
          <button className="btn btn-primary" type="button" onClick={() => navigate('/users')}>
            추가
          </button>
        </div>
      </section>
    </div>
  )
}

/** 노선 추가 */
export function RouteCreatePage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [direction, setDirection] = useState('셔틀')
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [description, setDescription] = useState('')
  const [saving, setSaving] = useState(false)

  const onSubmit = async () => {
    setSaving(true)
    const result = await createRoute({ name, direction, start, end, description })
    setSaving(false)
    if (!result.ok || !result.id) {
      window.alert(result.message ?? '등록에 실패했습니다.')
      return
    }
    navigate(`/routes/detail?id=${encodeURIComponent(result.id)}`)
  }

  return (
    <div className="page extra-page">
      <Crumb items={[{ label: '노선 관리', to: '/routes' }, { label: '노선 추가' }]} />
      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <RouteIcon size={17} />
            노선 추가
          </h3>
        </div>
        <div className="extra-form-grid">
          <div className="field">
            <label>
              노선명<span className="req">*</span>
            </label>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="예: 수원역 통학버스" />
          </div>
          <div className="field">
            <label>
              노선 유형<span className="req">*</span>
            </label>
            <select className="select" value={direction} onChange={(e) => setDirection(e.target.value)}>
              <option>통학</option>
              <option>셔틀</option>
              <option>순환</option>
            </select>
          </div>
          <div className="field">
            <label>기점</label>
            <input className="input" value={start} onChange={(e) => setStart(e.target.value)} placeholder="기흥역" />
          </div>
          <div className="field">
            <label>종점</label>
            <input className="input" value={end} onChange={(e) => setEnd(e.target.value)} placeholder="명지대 정문" />
          </div>
          <div className="field full">
            <label>노선 설명</label>
            <textarea
              className="textarea"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="노선 소개 및 운행 특징"
            />
          </div>
        </div>
        <p className="muted" style={{ margin: '8px 0 0', fontSize: 12 }}>
          운행 요일·시간은 schedules 시간표가 있으면 자동으로 표시됩니다.
        </p>
        <div className="extra-actions">
          <button className="btn btn-outline" type="button" onClick={() => navigate('/routes')}>
            취소
          </button>
          <button className="btn btn-primary" type="button" onClick={() => void onSubmit()} disabled={saving}>
            {saving ? '등록 중…' : '등록 후 상세로'}
          </button>
        </div>
      </section>
    </div>
  )
}

/** 정비 등록 */
export function MaintenanceCreatePage() {
  const navigate = useNavigate()

  return (
    <div className="page extra-page">
      <Crumb items={[{ label: '차량·정비 관리', to: '/vehicles' }, { label: '정비 등록' }]} />
      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <Wrench size={17} />
            정비 등록
          </h3>
        </div>
        <div className="extra-form-grid">
          <div className="field">
            <label>
              차량 번호<span className="req">*</span>
            </label>
            <select className="select" defaultValue={mockVehicles[0]?.plate}>
              {mockVehicles.map((v) => (
                <option key={v.plate}>{v.plate}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>
              정비일<span className="req">*</span>
            </label>
            <input className="input" type="date" defaultValue="2026-08-08" />
          </div>
          <div className="field">
            <label>
              정비 항목<span className="req">*</span>
            </label>
            <input className="input" placeholder="예: 엔진오일 교환" />
          </div>
          <div className="field">
            <label>정비 유형</label>
            <select className="select" defaultValue="정기">
              <option>정기</option>
              <option>수리</option>
              <option>점검</option>
              <option>소모품</option>
            </select>
          </div>
          <div className="field">
            <label>정비사</label>
            <input className="input" placeholder="담당 정비사" />
          </div>
          <div className="field">
            <label>예상 비용(원)</label>
            <input className="input" placeholder="120000" />
          </div>
          <div className="field">
            <label>상태</label>
            <select className="select" defaultValue="예정">
              <option>예정</option>
              <option>점검중</option>
              <option>완료</option>
            </select>
          </div>
          <div className="field full">
            <label>메모</label>
            <textarea className="textarea" rows={3} placeholder="정비 상세 메모" />
          </div>
        </div>
        <div className="extra-actions">
          <button className="btn btn-outline" type="button" onClick={() => navigate('/vehicles')}>
            취소
          </button>
          <button className="btn btn-primary" type="button" onClick={() => navigate('/vehicles/maintenance/detail')}>
            등록
          </button>
        </div>
      </section>
    </div>
  )
}

/** 정비 상세 */
export function MaintenanceDetailPage() {
  const row = maintenances[0]

  return (
    <div className="page extra-page">
      <Crumb items={[{ label: '차량·정비 관리', to: '/vehicles' }, { label: '정비 상세' }]} />
      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <Wrench size={17} />
            정비 상세
          </h3>
          <div className="toolbar">
            <Link className="btn btn-outline btn-xs" to="/vehicles/maintenance/new">
              새 정비 등록
            </Link>
            <Link className="btn btn-primary btn-xs" to="/vehicles">
              목록
            </Link>
          </div>
        </div>
        <div className="extra-detail-cards">
          {[
            ['정비일', row.date],
            ['차량 번호', row.plate],
            ['정비 항목', row.item],
            ['정비 유형', row.type],
            ['정비사', row.mechanic],
            ['비용', `${row.cost}원`],
            ['상태', row.status],
            ['다음 점검', '2026.08.20'],
          ].map(([k, v]) => (
            <div key={k} className="extra-detail-card">
              <div className="muted">{k}</div>
              <strong>{v}</strong>
            </div>
          ))}
        </div>
        <p style={{ margin: 0, fontSize: 13, color: '#54627c', lineHeight: 1.5 }}>
          정기 점검 항목이며, 부품 교체 후 시운전까지 완료된 기록입니다. 이상 징후 발생 시 즉시 운행 중단 후 재점검하세요.
        </p>
      </section>

      <section className="extra-panel">
        <div className="extra-panel-head">
          <h3>
            <CalendarDays size={17} />
            관련 예정 정비
          </h3>
        </div>
        <ul className="extra-side-list">
          {maintenances
            .filter((m) => m.status === '예정')
            .slice(0, 4)
            .map((m) => (
              <li key={`${m.date}-${m.plate}-${m.item}`}>
                <div>
                  <strong>
                    {m.plate} · {m.item}
                  </strong>
                  <span>
                    {m.date} · {m.type}
                  </span>
                </div>
                <StatusBadge tone={m.tone}>{m.status}</StatusBadge>
              </li>
            ))}
        </ul>
      </section>
    </div>
  )
}
