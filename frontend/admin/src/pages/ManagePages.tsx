import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  AlignLeft,
  Bell,
  Bold,
  Building2,
  Bus,
  CalendarDays,
  Car,
  CircleDollarSign,
  Clock3,
  Database,
  Download,
  ExternalLink,
  Eye,
  FileText,
  Gauge,
  GripVertical,
  Hourglass,
  Image as ImageIcon,
  Italic,
  Link2,
  List,
  ListChecks,
  ListOrdered,
  MapPin,
  Megaphone,
  MessageSquare,
  MoreHorizontal,
  PieChart,
  Plus,
  RadioTower,
  RefreshCw,
  Route as RouteIcon,
  Search,
  Check,
  Info,
  Minus,
  PencilLine,
  Settings2,
  Shield,
  Smartphone,
  ThumbsDown,
  ThumbsUp,
  TriangleAlert,
  Underline,
  UserRound,
  UserX,
  Users,
  Wrench,
} from 'lucide-react'
import mapImg from '../assets/map.png'
import {
  createNotice,
  fetchRoutes,
  type RouteRow,
} from '../lib/api'
import { maintenances, notices as mockNotices, reports as mockReports, routes as mockRoutes, systemLogs, users as mockUsers, vehicles as mockVehicles } from '../data/mock'
import { StatusBadge } from '../components/ui/Form'
import { isNaverMapConfigured } from '../lib/naverMaps'
import { useAuth } from '../state/AuthContext'
import '../styles/figma-pages.css'
import '../styles/reports.css'
import '../styles/notices.css'
import '../styles/routes.css'
import '../styles/vehicles.css'
import '../styles/users.css'
import '../styles/system.css'
import '../styles/settings.css'

type ReportTypeKey = (typeof mockReports)[number]['typeKey']

const reportKpis = [
  { label: '오늘 제보 수', value: '38', unit: '건', hint: '▲ 6건 (전일 대비)', hintTone: 'up' as const, icon: FileText, tone: 'blue' },
  { label: '처리 대기', value: '12', unit: '건', hint: '▲ 3건 (전일 대비)', hintTone: 'up' as const, icon: Hourglass, tone: 'orange' },
  { label: '자동 만료 예정', value: '4', unit: '건', hint: '24시간 이내 만료', hintTone: 'muted' as const, icon: Clock3, tone: 'green' },
  { label: '비활성 처리', value: '26', unit: '건', hint: '전체 제보의 14.6%', hintTone: 'muted' as const, icon: UserX, tone: 'gray' },
]

function ReportTypeIcon({ typeKey }: { typeKey: ReportTypeKey }) {
  if (typeKey === 'crowded' || typeKey === 'queue') return <Users size={14} />
  if (typeKey === 'traffic') return <Car size={14} />
  if (typeKey === 'passed') return <Bus size={14} />
  return <MessageSquare size={14} />
}

/** ADM-05 커뮤니티 제보 관리 */
export function ReportsPage() {
  const [selected, setSelected] = useState(3)
  const [reason, setReason] = useState('허위 또는 사실 아님')
  const [reasonDetail, setReasonDetail] = useState('버스 운행 기록 확인 결과, 해당 시간대에 정상 운행.')
  const [typeFilter, setTypeFilter] = useState('전체 유형')
  const [statusFilter, setStatusFilter] = useState('전체 상태')
  const [query, setQuery] = useState('')

  const list = useMemo(() => {
    return mockReports.filter((row) => {
      if (typeFilter !== '전체 유형' && row.type !== typeFilter) return false
      if (statusFilter !== '전체 상태' && row.status !== statusFilter) return false
      if (!query.trim()) return true
      const q = query.trim().toLowerCase()
      return (
        row.content.toLowerCase().includes(q) ||
        row.route.toLowerCase().includes(q) ||
        row.stop.toLowerCase().includes(q) ||
        row.type.toLowerCase().includes(q)
      )
    })
  }, [typeFilter, statusFilter, query])

  const item = list[Math.min(selected, Math.max(list.length - 1, 0))] ?? list[0]

  return (
    <div className="page reports-page">
      <p className="page-subtitle">학생들의 제보를 검토하고 신뢰도를 관리하는 공간입니다.</p>

      <div className="reports-kpis">
        {reportKpis.map((kpi) => {
          const Icon = kpi.icon
          return (
            <div key={kpi.label} className="reports-kpi">
              <div className={`reports-kpi-icon ${kpi.tone}`}>
                <Icon size={18} />
              </div>
              <div>
                <div className="label">{kpi.label}</div>
                <div className="value">
                  {kpi.value}
                  <em>{kpi.unit}</em>
                </div>
                <div className={`hint ${kpi.hintTone}`}>{kpi.hint}</div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="reports-main">
        <section className="reports-panel">
          <div className="reports-panel-head">
            <h3>
              <ListChecks size={17} />
              제보 목록
            </h3>
          </div>

          <div className="reports-filters">
            <select className="input" value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value); setSelected(0) }}>
              <option>전체 유형</option>
              <option>만석</option>
              <option>대기줄 김</option>
              <option>교통 정체</option>
              <option>버스 지나감</option>
              <option>기타</option>
            </select>
            <select className="input" value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setSelected(0) }}>
              <option>전체 상태</option>
              <option>처리 대기</option>
              <option>검토 중</option>
              <option>처리 완료</option>
              <option>비활성</option>
            </select>
            <div className="reports-date">
              <CalendarDays size={14} />
              2026-07-01 ~ 2026-07-08
            </div>
            <label className="reports-search">
              <Search size={14} />
              <input
                className="input"
                placeholder="검색어 입력 (내용, 노선, 정류장)"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                  setSelected(0)
                }}
              />
            </label>
          </div>

          <div className="reports-table-wrap">
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>제보 유형</th>
                  <th>노선/차량/정류장</th>
                  <th>등록 시간</th>
                  <th>좋아요</th>
                  <th>싫어요</th>
                  <th>상태</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {list.map((row, idx) => (
                  <tr
                    key={`${row.type}-${row.time}-${idx}`}
                    className={idx === selected ? 'selected' : undefined}
                    onClick={() => setSelected(idx)}
                    style={{ cursor: 'pointer' }}
                  >
                    <td>
                      <span className="reports-type">
                        <span className={`reports-type-ico ${row.typeKey}`}>
                          <ReportTypeIcon typeKey={row.typeKey} />
                        </span>
                        {row.type}
                      </span>
                    </td>
                    <td>
                      <div className="reports-target">
                        <strong>노선 {row.route} / 차량 {row.vehicle}</strong>
                        <span>{row.stop}</span>
                      </div>
                    </td>
                    <td>{row.time}</td>
                    <td>{row.likes}</td>
                    <td>{row.dislikes}</td>
                    <td>
                      <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                    </td>
                    <td>
                      <button
                        className="btn btn-outline btn-xs"
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          setSelected(idx)
                        }}
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="reports-pager">
            <span>전체 126건</span>
            <div className="reports-pages">
              <button type="button">‹</button>
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} type="button" className={n === 1 ? 'active' : undefined}>
                  {n}
                </button>
              ))}
              <button type="button">›</button>
              <select defaultValue="10개씩">
                <option>10개씩</option>
                <option>20개씩</option>
                <option>50개씩</option>
              </select>
            </div>
          </div>
        </section>

        <section className="reports-panel reports-detail">
          <div className="reports-panel-head">
            <h3>
              <FileText size={17} />
              제보 상세
            </h3>
            {item ? <StatusBadge tone={item.tone}>{item.status}</StatusBadge> : null}
          </div>

          {item ? (
            <>
              <div className="reports-section">
                <p className="reports-section-title">제보 정보</p>
                <div className="reports-info-grid">
                  <div className="reports-info-row">
                    <span>제보 유형</span>
                    <strong>
                      <span className={`reports-type-ico ${item.typeKey}`}>
                        <ReportTypeIcon typeKey={item.typeKey} />
                      </span>
                      {item.type}
                    </strong>
                  </div>
                  <div className="reports-info-row">
                    <span>등록 시각</span>
                    <strong>{item.datetime}</strong>
                  </div>
                  <div className="reports-info-row">
                    <span>제보자</span>
                    <strong>
                      {item.reporter} <span className="muted">(ID: {item.reporterId})</span>
                    </strong>
                  </div>
                  <div className="reports-info-row">
                    <span>노선/정류장</span>
                    <strong>
                      노선 {item.route} / 차량 {item.vehicle} / {item.stop}
                    </strong>
                  </div>
                  <div className="reports-info-row">
                    <span>반응</span>
                    <strong className="reports-votes">
                      <em>
                        <ThumbsUp size={13} /> {item.likes}
                      </em>
                      <em>
                        <ThumbsDown size={13} /> {item.dislikes}
                      </em>
                    </strong>
                  </div>
                </div>
              </div>

              <div className="reports-section">
                <p className="reports-section-title">제보 내용</p>
                <blockquote className="reports-content-box">{item.content}</blockquote>
              </div>

              <div className="reports-section">
                <p className="reports-section-title">첨부/상황 정보</p>
                <div className="reports-attach">
                  <div className="reports-thumb">
                    <img src={mapImg} alt="첨부 이미지 1" />
                  </div>
                  <div className="reports-thumb">
                    <img src={mapImg} alt="첨부 이미지 2" style={{ objectPosition: '70% 40%' }} />
                  </div>
                </div>
                <div className="reports-meta">
                  <span>등록 기기: Android / Chrome</span>
                  <span>위치: 37.5665, 126.9780</span>
                  <span>반경: 50m</span>
                </div>
              </div>

              <div className="reports-section">
                <p className="reports-section-title">상태 관리</p>
                <div className="reports-status-box">
                  <StatusBadge tone={item.tone}>{item.status}</StatusBadge>
                  <p>
                    {item.status === '비활성'
                      ? '신뢰도가 낮거나 허위로 판단된 제보입니다.'
                      : item.status === '처리 완료'
                        ? '검토가 완료되어 통계에 반영된 제보입니다.'
                        : '관리자 검토가 필요한 제보입니다.'}
                  </p>
                </div>
                <div className="reports-form">
                  <label>
                    <span>비활성 사유</span>
                    <select className="input" value={reason} onChange={(e) => setReason(e.target.value)}>
                      <option>허위 또는 사실 아님</option>
                      <option>중복 제보</option>
                      <option>정보 부족</option>
                      <option>기타</option>
                    </select>
                  </label>
                  <label>
                    <span>상세 사유 (선택)</span>
                    <textarea
                      className="input"
                      maxLength={200}
                      value={reasonDetail}
                      onChange={(e) => setReasonDetail(e.target.value)}
                    />
                    <div className="reports-char">{reasonDetail.length}/200</div>
                  </label>
                </div>
              </div>

              <div className="reports-actions">
                <button type="button" className="btn btn-ghost">
                  비활성화
                </button>
                <button type="button" className="btn btn-danger">
                  삭제
                </button>
                <button type="button" className="btn btn-primary">
                  완료 처리
                </button>
              </div>

              <div className="reports-expire">
                <Clock3 size={14} />
                24시간 후 자동 만료 예정
              </div>
              <p className="reports-note">
                제보는 등록 후 24시간이 지나면 자동 만료됩니다. 비활성 처리된 제보는 통계에서 제외됩니다.
              </p>
            </>
          ) : (
            <p className="muted">표시할 제보가 없습니다.</p>
          )}
        </section>
      </div>
    </div>
  )
}

/** ADM-06 공지·긴급 알림 관리 */
export function NoticesPage() {
  const { user } = useAuth()
  const [noticeType, setNoticeType] = useState<'긴급' | '중요' | '운행 변경' | '일반'>('긴급')
  const [push, setPush] = useState(true)
  const [title, setTitle] = useState('폭설로 인한 운행 지연 안내')
  const [body, setBody] = useState(
    '폭설로 인해 일부 노선의 운행이 지연되고 있습니다.\n자세한 내용은 노선별 운행 정보에서 확인해 주세요.\n이용에 불편을 드려 죄송합니다.',
  )
  const [target, setTarget] = useState<'all' | 'route'>('all')
  const [routeName, setRouteName] = useState('기흥역 통학버스')
  const [alwaysOn, setAlwaysOn] = useState(false)
  const [typeFilter, setTypeFilter] = useState('전체 유형')
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState(0)
  const [saving, setSaving] = useState(false)
  const [saveMsg, setSaveMsg] = useState('')

  const list = useMemo(() => {
    return mockNotices.filter((row) => {
      if (typeFilter !== '전체 유형' && row.type !== typeFilter) return false
      if (!query.trim()) return true
      const q = query.trim().toLowerCase()
      return row.title.toLowerCase().includes(q) || row.target.toLowerCase().includes(q)
    })
  }, [typeFilter, query])

  const onCreate = async () => {
    setSaving(true)
    setSaveMsg('')
    const result = await createNotice({
      title: title.trim() || '제목 없음',
      content: body.trim() || '',
      author_id: user?.id ?? null,
    })
    setSaving(false)
    if (!result.ok) {
      setSaveMsg(result.message ?? '등록 실패 (로컬 미리보기만 반영)')
      return
    }
    setSaveMsg('등록되었습니다.')
  }

  const onSelectRow = (idx: number) => {
    setSelected(idx)
    const row = list[idx]
    if (!row) return
    setNoticeType(row.type as '긴급' | '중요' | '운행 변경' | '일반')
    setTitle(row.title)
    setTarget(row.target === '전체' ? 'all' : 'route')
    if (row.target.startsWith('노선')) setRouteName(row.target.replace('노선 ', ''))
  }

  const typeTone =
    noticeType === '긴급' ? 'red' : noticeType === '중요' ? 'orange' : noticeType === '운행 변경' ? 'blue' : 'gray'

  const typeLabel =
    noticeType === '긴급'
      ? '긴급 공지'
      : noticeType === '중요'
        ? '중요 공지'
        : noticeType === '운행 변경'
          ? '운행 변경'
          : '일반 공지'

  return (
    <div className="page notices-page">
      <div className="notices-kpis">
        {[
          { label: '전체 공지', value: '128', unit: '건', hint: '최근 30일 기준', icon: Megaphone, tone: 'blue' },
          { label: '긴급 공지', value: '8', unit: '건', hint: '최근 30일 기준', icon: TriangleAlert, tone: 'red' },
          { label: '예약 공지', value: '15', unit: '건', hint: '게시 예정', icon: CalendarDays, tone: 'orange' },
          { label: '최근 조회수', value: '23,450', unit: '회', hint: '최근 30일 기준', icon: Eye, tone: 'purple' },
        ].map((kpi) => {
          const Icon = kpi.icon
          return (
            <div key={kpi.label} className="notices-kpi">
              <div className={`notices-kpi-icon ${kpi.tone}`}>
                <Icon size={18} />
              </div>
              <div>
                <div className="label">{kpi.label}</div>
                <div className="value">
                  {kpi.value}
                  <em>{kpi.unit}</em>
                </div>
                <div className="hint">{kpi.hint}</div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="notices-main">
        <section className="notices-panel">
          <div className="notices-panel-head">
            <h3>
              <ListChecks size={17} />
              공지 목록 <span className="muted">(전체 128건)</span>
            </h3>
          </div>

          <div className="notices-filters">
            <select
              className="input"
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value)
                setSelected(0)
              }}
            >
              <option>전체 유형</option>
              <option>긴급</option>
              <option>중요</option>
              <option>운행 변경</option>
              <option>일반</option>
            </select>
            <label className="notices-search">
              <Search size={14} />
              <input
                className="input"
                placeholder="제목 또는 내용을 검색하세요."
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                  setSelected(0)
                }}
              />
            </label>
            <button className="btn btn-primary btn-xs" type="button">
              검색
            </button>
          </div>

          <div className="notices-table-wrap">
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
                {list.map((row, idx) => (
                  <tr
                    key={row.no}
                    className={idx === selected ? 'selected' : undefined}
                    style={{ cursor: 'pointer' }}
                    onClick={() => onSelectRow(idx)}
                  >
                    <td>{row.no}</td>
                    <td>
                      <StatusBadge tone={row.tone}>{row.type}</StatusBadge>
                    </td>
                    <td className="notices-title-cell" title={row.title}>
                      {row.title}
                    </td>
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
          </div>

          <div className="notices-pager">
            <span>전체 128건</span>
            <div className="notices-pages">
              <button type="button">‹</button>
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} type="button" className={n === 1 ? 'active' : undefined}>
                  {n}
                </button>
              ))}
              <button type="button">›</button>
              <select defaultValue="10개씩">
                <option>10개씩</option>
                <option>20개씩</option>
                <option>50개씩</option>
              </select>
            </div>
          </div>
          <p className="notices-foot-note">* 공지사항은 학생 앱 [알림] 탭과 푸시 알림으로 발송됩니다.</p>
        </section>

        <section className="notices-panel">
          <div className="notices-panel-head">
            <h3>
              <FileText size={17} />
              공지 등록/수정
            </h3>
            <div className="notices-head-actions">
              <button className="btn btn-outline btn-xs" type="button">
                <Eye size={12} />
                미리보기
              </button>
              <button className="btn btn-danger btn-xs" type="button" style={{ background: 'transparent', color: '#eb4047', border: '1px solid #eb4047' }}>
                삭제
              </button>
              <button className="btn btn-outline btn-xs" type="button">
                수정
              </button>
              <button className="btn btn-primary btn-xs" type="button" disabled={saving} onClick={() => void onCreate()}>
                {saving ? '등록 중...' : '공지 등록'}
              </button>
              <button className="btn btn-danger btn-xs" type="button">
                긴급 공지 발송
              </button>
            </div>
          </div>
          {saveMsg ? <p className="notices-save-msg">{saveMsg}</p> : null}

          <div className="notices-form-layout">
            <div className="notices-form">
              <div className="notices-field">
                <span className="label">공지 유형</span>
                <div className="notices-type-grid">
                  {(
                    [
                      ['긴급', 'danger'],
                      ['중요', 'warn'],
                      ['운행 변경', ''],
                      ['일반', ''],
                    ] as const
                  ).map(([t, tone]) => (
                    <button
                      key={t}
                      type="button"
                      className={`notices-type-btn${noticeType === t ? ` active${tone ? ` ${tone}` : ''}` : ''}`}
                      onClick={() => setNoticeType(t)}
                    >
                      {t === '긴급' || t === '일반' ? `${t} 공지` : t}
                    </button>
                  ))}
                </div>
              </div>

              <div className="notices-field">
                <label htmlFor="notice-title">제목</label>
                <div className="notices-title-row">
                  <input
                    id="notice-title"
                    className="input"
                    maxLength={100}
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="공지 제목을 입력하세요"
                  />
                  <div className="notices-char">{title.length}/100</div>
                </div>
              </div>

              <div className="notices-field">
                <span className="label">내용</span>
                <div className="notices-editor">
                  <div className="notices-toolbar">
                    <button type="button" aria-label="굵게"><Bold size={14} /></button>
                    <button type="button" aria-label="기울임"><Italic size={14} /></button>
                    <button type="button" aria-label="밑줄"><Underline size={14} /></button>
                    <button type="button" aria-label="글머리"><List size={14} /></button>
                    <button type="button" aria-label="번호 목록"><ListOrdered size={14} /></button>
                    <button type="button" aria-label="정렬"><AlignLeft size={14} /></button>
                    <button type="button" aria-label="링크"><Link2 size={14} /></button>
                    <button type="button" aria-label="이미지"><ImageIcon size={14} /></button>
                  </div>
                  <textarea
                    maxLength={2000}
                    value={body}
                    onChange={(e) => setBody(e.target.value)}
                    placeholder="공지 내용을 입력하세요"
                  />
                </div>
                <div className="notices-char">{body.length}/2000</div>
              </div>

              <div className="notices-field">
                <span className="label">대상</span>
                <div className="notices-target-row">
                  <label className="notices-radio">
                    <input type="radio" name="target" checked={target === 'all'} onChange={() => setTarget('all')} />
                    전체 학생
                  </label>
                  <label className="notices-radio">
                    <input type="radio" name="target" checked={target === 'route'} onChange={() => setTarget('route')} />
                    특정 노선 선택
                  </label>
                  <select
                    className="input"
                    style={{ height: 34, minWidth: 160, fontSize: 12 }}
                    disabled={target !== 'route'}
                    value={routeName}
                    onChange={(e) => setRouteName(e.target.value)}
                  >
                    {mockRoutes.map((r) => (
                      <option key={r.name}>{r.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="notices-field">
                <span className="label">게시 기간</span>
                <div className="notices-period">
                  <div className="notices-period-box">
                    <input className="input" type="datetime-local" defaultValue="2026-07-08T09:00" disabled={alwaysOn} />
                  </div>
                  <span className="notices-period-sep">~</span>
                  <div className="notices-period-box">
                    <input className="input" type="datetime-local" defaultValue="2026-07-15T18:00" disabled={alwaysOn} />
                  </div>
                </div>
                <label className="notices-check">
                  <input type="checkbox" checked={alwaysOn} onChange={(e) => setAlwaysOn(e.target.checked)} />
                  게시 기간 없음 (상시 게시)
                </label>
              </div>

              <div className="notices-push">
                <div>
                  <strong>푸시 알림 동시 발송</strong>
                  <p>등록과 동시에 학생 앱으로 푸시 알림을 보냅니다. 긴급 공지 발송 시 권장됩니다.</p>
                </div>
                <label className="notices-switch">
                  <input type="checkbox" checked={push} onChange={(e) => setPush(e.target.checked)} />
                  <span />
                </label>
              </div>
            </div>

            <aside className="notices-previews">
              <p className="notices-preview-cap">학생 앱 미리보기</p>
              <div className="notices-phone">
                <div className="notices-phone-notch" />
                <div className="notices-phone-screen">
                  <div className="notices-phone-top">
                    <span>‹ 공지사항</span>
                    <span>⋯</span>
                  </div>
                  <span className={`notices-phone-tag ${typeTone}`}>{typeLabel}</span>
                  <strong>{title || '제목'}</strong>
                  <div className="notices-phone-time">2026.07.08 · 관리자</div>
                  <p className="notices-phone-body">{body || '내용이 여기에 표시됩니다.'}</p>
                  <button type="button" className="notices-phone-btn">
                    오늘 하루 보지 않기
                  </button>
                </div>
              </div>

              <p className="notices-preview-cap">푸시 알림 미리보기</p>
              <div className="notices-push-preview">
                <div className="notices-push-card">
                  <div className="notices-push-app">ON</div>
                  <div>
                    <span className="notices-push-when">지금</span>
                    <strong>ON-DA · {typeLabel}</strong>
                    <p>{title || '공지 제목'}</p>
                    <p>{body || '공지 내용 미리보기'}</p>
                  </div>
                </div>
              </div>
            </aside>
          </div>
        </section>
      </div>
    </div>
  )
}

type RouteView = {
  id: string
  name: string
  status: string
  buses: string
  type: string
  days: string
  hours: string
  desc: string
  stopCount: string
  distance: string
  duration: string
  interval: string
  origin: string
  destination: string
}

const cityStops = [
  { no: 1, name: '버스관리사무소', tag: 'start' as const, time: '17:15' },
  { no: 2, name: '상공회의소', tag: null, time: '17:18' },
  { no: 3, name: '진입로(럭스나인 앞)', tag: null, time: '17:21' },
  { no: 4, name: '동부경찰서 중앙지구대', tag: null, time: '17:24' },
  { no: 5, name: '용인 CGV', tag: null, time: '17:28' },
  { no: 6, name: '중앙공영주차장', tag: 'end' as const, time: '17:32' },
]

const cityTimetable = [
  { no: '1', time: '07:15', interval: '60분', buses: '3대', next: false },
  { no: '2', time: '08:15', interval: '60분', buses: '3대', next: false },
  { no: '3', time: '09:15', interval: '60분', buses: '2대', next: false },
  { no: '4', time: '10:15', interval: '60분', buses: '2대', next: false },
  { no: '5', time: '11:15', interval: '60분', buses: '2대', next: false },
  { no: '18', time: '17:15', interval: '60분', buses: '2대', next: true },
  { no: '19', time: '18:15', interval: '60분', buses: '2대', next: false },
  { no: '20', time: '19:15', interval: '60분', buses: '2대', next: false },
]

const cityBuses = [
  { no: 1, name: '1호차', plate: '70가 1234', spec: '45인승 | 현대 유니버스' },
  { no: 2, name: '2호차', plate: '70가 5678', spec: '45인승 | 현대 유니버스' },
  { no: 3, name: '3호차', plate: '70가 9012', spec: '45인승 | 현대 유니버스' },
]

/** ADM-04 노선·운행 관리 */
export function RoutesPage() {
  const [selected, setSelected] = useState(2)
  const [tab, setTab] = useState<'basic' | 'stops' | 'timetable' | 'buses'>('basic')
  const [dbRoutes, setDbRoutes] = useState<RouteRow[] | null>(null)

  useEffect(() => {
    void fetchRoutes().then((data) => {
      if (data && data.length > 0) {
        setDbRoutes(data)
        setSelected(0)
      }
    })
  }, [])

  const list: RouteView[] =
    dbRoutes && dbRoutes.length > 0
      ? dbRoutes.map((r) => {
          const mock = mockRoutes.find((m) => m.name === r.route_name)
          return {
            id: r.id,
            name: r.route_name,
            status: r.is_active ? '운행 중' : '비활성',
            buses: mock?.buses ?? '—',
            type: r.direction ?? mock?.type ?? '—',
            days: mock?.days ?? '월~금',
            hours: mock?.hours ?? '—',
            desc: r.description ?? mock?.desc ?? '',
            stopCount: mock ? `${mock.stops}개소` : '—',
            distance: '12.4 km',
            duration: '28분',
            interval: '20분',
            origin: '버스관리사무소',
            destination: '중앙공영주차장',
          }
        })
      : mockRoutes.map((r, i) => ({
          id: String(i),
          name: r.name,
          status: r.status,
          buses: r.buses,
          type: r.type,
          days: r.days,
          hours: r.hours,
          desc: r.desc,
          stopCount: `${r.stops}개소`,
          distance: '12.4 km',
          duration: '28분',
          interval: '20분',
          origin: i === 2 ? '버스관리사무소' : '기흥역',
          destination: i === 2 ? '중앙공영주차장' : '명지대 정문',
        }))

  const detail = list[selected] ?? list[0]

  return (
    <div className="page routes-page">
      <div className="routes-split">
        <section className="routes-panel">
          <div className="routes-panel-head">
            <h3>
              <ListChecks size={17} />
              노선 목록
            </h3>
            <Link className="btn btn-primary btn-xs" to="/routes/new">
              <Plus size={14} />
              노선 추가
            </Link>
          </div>
          <div className="routes-table-wrap">
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>노선명</th>
                  <th>운행 상태</th>
                  <th>배정 차량 수</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {list.map((row, idx) => (
                  <tr
                    key={row.id}
                    className={idx === selected ? 'selected' : undefined}
                    onClick={() => setSelected(idx)}
                  >
                    <td style={{ fontWeight: 700 }}>{row.name}</td>
                    <td>
                      <StatusBadge tone={row.status.includes('운행') ? 'green' : 'gray'}>{row.status}</StatusBadge>
                    </td>
                    <td>{row.buses}</td>
                    <td>
                      <button
                        className="btn btn-outline btn-xs"
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          setSelected(idx)
                        }}
                      >
                        수정
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="routes-pager">
            <button type="button">‹</button>
            <button type="button" className="active">
              1
            </button>
            <button type="button">›</button>
          </div>
        </section>

        <section className="routes-panel">
          <div className="routes-panel-head">
            <h3>
              <RouteIcon size={17} />
              노선 상세 - {detail?.name ?? '-'}
            </h3>
            <Link className="btn btn-outline btn-xs" to="/routes/detail">
              <ExternalLink size={12} />
              상세 보기
            </Link>
          </div>

          <div className="routes-tabs">
            {(
              [
                ['basic', '기본 정보'],
                ['stops', '정류장'],
                ['timetable', '시간표'],
                ['buses', '배정 차량'],
              ] as const
            ).map(([key, label]) => (
              <button
                key={key}
                type="button"
                className={`routes-tab${tab === key ? ' active' : ''}`}
                onClick={() => setTab(key)}
              >
                {label}
              </button>
            ))}
          </div>

          {tab === 'basic' ? (
            <>
              <div className="routes-meta-row">
                {[
                  ['노선 유형', detail?.type],
                  ['운행 상태', detail?.status],
                  ['운행 요일', detail?.days],
                  ['운행 시간', detail?.hours],
                  ['배정 차량 수', detail?.buses],
                ].map(([k, v]) => (
                  <div key={k} className="routes-meta-item">
                    <div className="label">{k}</div>
                    <div className="value">
                      {k === '운행 상태' ? <StatusBadge tone="green">{v}</StatusBadge> : v}
                    </div>
                  </div>
                ))}
              </div>

              <div className="routes-section">
                <span className="label">노선 설명</span>
                <p className="routes-desc">{detail?.desc}</p>
              </div>

              <div className="routes-section" style={{ flex: 1 }}>
                <span className="label">노선 경로 미리보기</span>
                <div className="routes-map">
                  <img src={mapImg} alt="노선 경로" />
                  <div className="routes-map-controls">
                    <button type="button">+</button>
                    <button type="button">−</button>
                    <button type="button">◎</button>
                  </div>
                  <div className="routes-map-stops">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <span key={n}>{n}</span>
                    ))}
                  </div>
                </div>
                <div className="routes-stats">
                  {[
                    { label: '총 정류장 수', value: detail?.stopCount, icon: MapPin },
                    { label: '총 운행 거리', value: detail?.distance, icon: RouteIcon },
                    { label: '예상 소요 시간', value: detail?.duration, icon: Clock3 },
                    { label: '운행 간격', value: detail?.interval, icon: RefreshCw },
                  ].map((s) => {
                    const Icon = s.icon
                    return (
                      <div key={s.label} className="routes-stat">
                        <div className="label">
                          <Icon size={12} />
                          {s.label}
                        </div>
                        <div className="value">{s.value}</div>
                      </div>
                    )
                  })}
                </div>
              </div>

              <div className="routes-actions">
                <button className="btn btn-outline btn-xs" type="button">
                  취소
                </button>
                <button className="btn btn-primary btn-xs" type="button">
                  저장
                </button>
              </div>
            </>
          ) : (
            <div className="routes-empty-tab" style={{ display: 'grid', gap: 10, justifyItems: 'start' }}>
              {tab === 'stops' ? (
                <>
                  <span>정류장 순서·좌표는 정류장 관리에서 등록하고, 노선 상세에서 배치합니다.</span>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Link className="btn btn-outline btn-xs" to="/stops">
                      정류장 관리
                    </Link>
                    <Link className="btn btn-primary btn-xs" to="/routes/detail">
                      상세 보기
                    </Link>
                  </div>
                </>
              ) : null}
              {tab === 'timetable' ? (
                <>
                  <span>시간표 상세는 [상세 보기]에서 편집할 수 있습니다.</span>
                  <Link className="btn btn-primary btn-xs" to="/routes/detail">
                    상세 보기
                  </Link>
                </>
              ) : null}
              {tab === 'buses' ? (
                <>
                  <span>배정 차량은 차량·기사 관리와 연동됩니다.</span>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Link className="btn btn-outline btn-xs" to="/vehicles">
                      차량 관리
                    </Link>
                    <Link className="btn btn-outline btn-xs" to="/drivers">
                      기사 관리
                    </Link>
                  </div>
                </>
              ) : null}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

/** ADM-04-01 노선 상세 */
export function RouteDetailPage() {
  const [tab, setTab] = useState<'basic' | 'stops' | 'timetable' | 'buses'>('basic')

  const showStops = tab === 'basic' || tab === 'stops'
  const showTimetable = tab === 'basic' || tab === 'timetable'
  const showBuses = tab === 'basic' || tab === 'buses'

  return (
    <div className="page route-detail-page">
      <p className="route-detail-crumb">
        <Link to="/routes">노선 관리</Link> &gt; 노선 상세
      </p>

      <section className="route-detail-hero">
        <div className="route-detail-hero-ico">
          <Bus size={24} />
        </div>
        <div className="route-detail-hero-main">
          <h2>시내 셔틀</h2>
          <StatusBadge tone="green">운행 중</StatusBadge>
        </div>
        <div className="route-detail-hero-meta">
          <div>
            <div className="label">출발지</div>
            <div className="value">버스관리사무소</div>
          </div>
          <div>
            <div className="label">도착지</div>
            <div className="value">중앙공영주차장</div>
          </div>
          <div>
            <div className="label">학생 앱 노출 여부</div>
            <div className="route-detail-exposed">
              <i />
              노출 중
            </div>
          </div>
        </div>
        <button type="button" className="route-detail-more" aria-label="더보기">
          <MoreHorizontal size={16} />
        </button>
      </section>

      <div className="routes-tabs" style={{ background: '#fff', borderRadius: 10, padding: '0 12px', border: '1px solid #eef1f6' }}>
        {(
          [
            ['basic', '기본 정보'],
            ['stops', '정류장'],
            ['timetable', '시간표'],
            ['buses', '배정 차량'],
          ] as const
        ).map(([key, label]) => (
          <button
            key={key}
            type="button"
            className={`routes-tab${tab === key ? ' active' : ''}`}
            onClick={() => setTab(key)}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="route-detail-body">
        <div className="route-detail-left">
          {(showStops || showTimetable) && (
            <div className="route-detail-grid-2">
              {showStops ? (
                <section className="rd-card">
                  <div className="rd-card-head">
                    <h3>정류장 순서</h3>
                  </div>
                  <ul className="rd-stop-list">
                    {cityStops.map((s) => (
                      <li key={s.no} className="rd-stop-item">
                        <span className="rd-stop-no">{s.no}</span>
                        <span>{s.name}</span>
                        {s.tag === 'start' ? <span className="rd-stop-tag start">출발지</span> : null}
                        {s.tag === 'end' ? <span className="rd-stop-tag end">도착지</span> : null}
                        {!s.tag ? <span /> : null}
                        <GripVertical size={14} className="rd-grip" />
                      </li>
                    ))}
                  </ul>
                  <button type="button" className="rd-add-btn">
                    <Plus size={14} />
                    정류장 추가
                  </button>
                </section>
              ) : null}

              {showTimetable ? (
                <section className="rd-card">
                  <div className="rd-card-head">
                    <h3>
                      시간표 <span className="chip">편도</span>
                    </h3>
                    <button className="btn btn-outline btn-xs" type="button">
                      시간표 편집
                    </button>
                  </div>
                  <div className="rd-table-wrap">
                    <table className="data-table dense">
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
                        {cityTimetable.map((row) => (
                          <tr key={row.no} className={row.next ? 'next' : undefined}>
                            <td>{row.no}</td>
                            <td>
                              {row.time}
                              {row.next ? <span className="rd-next-tag">다음 출발</span> : null}
                            </td>
                            <td>{row.interval}</td>
                            <td>{row.buses}</td>
                            <td>{row.next ? '다음 출발' : '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <p className="rd-note">※ 교통 상황에 따라 ±5분 정도 오차가 발생할 수 있습니다.</p>
                </section>
              ) : null}
            </div>
          )}

          {showBuses ? (
            <section className="rd-card">
              <div className="rd-card-head">
                <h3>배정 차량</h3>
                <button className="btn btn-outline btn-xs" type="button">
                  차량 변경
                </button>
              </div>
              <div className="rd-bus-list">
                {cityBuses.map((b) => (
                  <div key={b.no} className="rd-bus-card">
                    <span className="rd-bus-no">{b.no}</span>
                    <div>
                      <strong>
                        {b.name}
                        <StatusBadge tone="green">운행 중</StatusBadge>
                      </strong>
                      <p>
                        {b.plate} · {b.spec}
                      </p>
                    </div>
                    <div className="rd-bus-thumb">
                      <Bus size={22} />
                    </div>
                  </div>
                ))}
              </div>
              <button type="button" className="rd-add-btn">
                <Plus size={14} />
                차량 추가
              </button>
            </section>
          ) : null}
        </div>

        <aside className="rd-card rd-preview">
          <div className="rd-preview-cap">
            <h3>학생 앱 미리보기</h3>
            <a href="#preview">
              전체 화면 보기
              <ExternalLink size={11} />
            </a>
          </div>
          <div className="rd-phone">
            <div className="rd-phone-notch" />
            <div className="rd-phone-screen">
              <div className="rd-phone-head">
                <strong>시내 셔틀</strong>
                <StatusBadge tone="green">운행 중</StatusBadge>
              </div>
              <p className="rd-phone-summary">현재 3대 운행 중 · 다음 출발 17:15</p>
              <ul className="rd-timeline">
                {cityStops.map((s, idx) => (
                  <li key={s.no}>
                    <span className={`dot${idx === 1 ? ' bus' : ''}`} />
                    <span className="name">
                      {s.name}
                      {s.tag === 'start' ? <span className="rd-stop-tag start">출발지</span> : null}
                      {s.tag === 'end' ? <span className="rd-stop-tag end">도착지</span> : null}
                    </span>
                    <span className="time">{s.time}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
          <div className="rd-side-actions">
            <button type="button" className="rd-phone-btn" aria-label="앱 미리보기">
              <Smartphone size={18} />
            </button>
            <button className="btn btn-outline" type="button">
              저장
            </button>
            <button className="btn btn-primary" type="button">
              오늘 운행에 반영
            </button>
          </div>
        </aside>
      </div>
    </div>
  )
}

const maintTypeSegments = [
  { label: '정기 점검', count: 6, pct: 50, color: '#266ef4' },
  { label: '수리', count: 3, pct: 25, color: '#3fb46a' },
  { label: '소모품 교체', count: 2, pct: 16.7, color: '#fdac38' },
  { label: '기타', count: 1, pct: 8.3, color: '#7964f2' },
]

const costTrend = [
  { month: '02', value: 2100 },
  { month: '03', value: 3300 },
  { month: '04', value: 4600 },
  { month: '05', value: 3900 },
  { month: '06', value: 5200 },
  { month: '07', value: 4850 },
]

const scheduledMaint = [
  { date: '07.24', plate: '73버 1122', item: '브레이크 패드 점검', due: '2일 후' },
  { date: '07.24', plate: '74버 7788', item: '엔진오일 교환', due: '2일 후' },
  { date: '07.25', plate: '72버 5678', item: '타이어 위치 교환', due: '3일 후' },
  { date: '07.26', plate: '75버 9900', item: '에어컨 필터 교체', due: '4일 후' },
]

const maintAlerts = [
  { tone: 'orange' as const, text: '72버 1234 차량의 정기점검이 예정되어 있습니다.', time: '20분 전' },
  { tone: 'red' as const, text: '75버 9900 차량의 타이어 교체가 필요합니다.', time: '1시간 전' },
  { tone: 'orange' as const, text: '73버 1122 차량의 브레이크 패드 점검이 다가옵니다.', time: '2시간 전' },
  { tone: 'green' as const, text: '74버 7788 차량의 엔진오일 교환이 완료되었습니다.', time: '어제' },
]

function donutBackground(segments: { pct: number; color: string }[]) {
  let start = 0
  const parts = segments.map((s) => {
    const end = start + s.pct
    const part = `${s.color} ${start}% ${end}%`
    start = end
    return part
  })
  return { background: `conic-gradient(${parts.join(', ')})` }
}

function CostTrendChart() {
  const w = 360
  const h = 168
  const pad = { t: 28, r: 20, b: 28, l: 20 }
  const plotH = h - pad.t - pad.b
  const plotW = w - pad.l - pad.r
  const max = 6000
  const min = 0
  const xs = costTrend.map((_, i) => pad.l + (i * plotW) / (costTrend.length - 1))
  const ys = costTrend.map((d) => pad.t + ((max - d.value) / (max - min)) * plotH)

  let line = `M ${xs[0]} ${ys[0]}`
  for (let i = 0; i < xs.length - 1; i += 1) {
    const cx = (xs[i] + xs[i + 1]) / 2
    line += ` C ${cx} ${ys[i]}, ${cx} ${ys[i + 1]}, ${xs[i + 1]} ${ys[i + 1]}`
  }
  const area = `${line} L ${xs[xs.length - 1]} ${h - pad.b} L ${xs[0]} ${h - pad.b} Z`
  const gridYs = [0.25, 0.5, 0.75].map((t) => pad.t + plotH * t)

  return (
    <svg className="vehicles-area" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="xMidYMid meet" aria-hidden>
      <defs>
        <linearGradient id="vehCostGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#266ef4" stopOpacity="0.28" />
          <stop offset="100%" stopColor="#266ef4" stopOpacity="0.03" />
        </linearGradient>
      </defs>
      {gridYs.map((y) => (
        <line key={y} x1={pad.l} y1={y} x2={w - pad.r} y2={y} stroke="#eef1f6" strokeWidth="1" />
      ))}
      <path d={area} fill="url(#vehCostGrad)" />
      <path d={line} fill="none" stroke="#266ef4" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      {xs.map((x, i) => (
        <g key={costTrend[i].month}>
          <circle cx={x} cy={ys[i]} r="4" fill="#fff" stroke="#266ef4" strokeWidth="2" />
          <text x={x} y={ys[i] - 10} textAnchor="middle" fontSize="10" fontWeight="700" fill="#54627c">
            {costTrend[i].value.toLocaleString()}
          </text>
          <text x={x} y={h - 8} textAnchor="middle" fontSize="10" fontWeight="700" fill="#8b92a4">
            2026-{costTrend[i].month}
          </text>
        </g>
      ))}
    </svg>
  )
}

/** ADM-07 차량·정비 관리 */
export function VehiclesPage() {
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('전체 상태')

  const list = useMemo(() => {
    return maintenances.filter((row) => {
      if (statusFilter !== '전체 상태' && row.status !== statusFilter) return false
      if (!query.trim()) return true
      const q = query.trim().toLowerCase()
      return row.plate.toLowerCase().includes(q) || row.item.toLowerCase().includes(q) || row.mechanic.toLowerCase().includes(q)
    })
  }, [query, statusFilter])

  return (
    <div className="page vehicles-page">
      <div className="vehicles-toolbar">
        <select className="input" defaultValue="전체 차량">
          <option>전체 차량</option>
          {mockVehicles.map((v) => (
            <option key={v.plate}>{v.plate}</option>
          ))}
        </select>
        <div className="vehicles-date">
          <CalendarDays size={14} />
          2026.07.01 ~ 2026.07.31
        </div>
        <Link className="btn btn-primary btn-xs" to="/vehicles/maintenance/new">
          <Plus size={14} />
          정비 등록
        </Link>
      </div>

      <div className="vehicles-kpis">
        {[
          { label: '전체 차량', value: '28', unit: '대', hint: '정상 20 | 정비중 4 | 점검 필요 4', tone: 'blue', icon: Wrench, hintTone: '' },
          { label: '예정 정비', value: '6', unit: '건', hint: '이번 주 2 | 이번 달 6', tone: 'green', icon: CalendarDays, hintTone: '' },
          { label: '정비 완료 (이달)', value: '12', unit: '건', hint: '지난달 대비 ▲ 20%', tone: 'orange', icon: Clock3, hintTone: 'up' },
          { label: '정비 비용 (이달)', value: '4,850', unit: '만원', hint: '지난달 대비 ▼ 15%', tone: 'red', icon: CircleDollarSign, hintTone: 'down' },
          { label: '가동률', value: '92.6', unit: '%', hint: '목표 90%', tone: 'purple', icon: Gauge, hintTone: '' },
        ].map((kpi) => {
          const Icon = kpi.icon
          return (
            <div key={kpi.label} className="vehicles-kpi">
              <div className={`vehicles-kpi-icon ${kpi.tone}`}>
                <Icon size={18} />
              </div>
              <div>
                <div className="label">{kpi.label}</div>
                <div className="value">
                  {kpi.value}
                  <em>{kpi.unit}</em>
                </div>
                <div className={`hint ${kpi.hintTone}`}>{kpi.hint}</div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="vehicles-mid">
        <section className="vehicles-panel">
          <div className="vehicles-panel-head">
            <h3>
              <ListChecks size={17} />
              정비 이력
            </h3>
          </div>
          <div className="vehicles-filters">
            <label className="vehicles-search">
              <Search size={14} />
              <input
                className="input"
                placeholder="차량 번호 / 정비 항목 검색"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </label>
            <select className="input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option>전체 상태</option>
              <option>완료</option>
              <option>점검중</option>
              <option>예정</option>
            </select>
          </div>
          <div className="vehicles-table-wrap">
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
                {list.map((row) => (
                  <tr key={`${row.date}-${row.plate}-${row.item}`}>
                    <td>{row.date}</td>
                    <td style={{ fontWeight: 700 }}>{row.plate}</td>
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
          </div>
          <div className="vehicles-pager">
            <button type="button">‹</button>
            {[1, 2, 3, 4].map((n) => (
              <button key={n} type="button" className={n === 1 ? 'active' : undefined}>
                {n}
              </button>
            ))}
            <button type="button">›</button>
          </div>
        </section>

        <div className="vehicles-charts">
          <section className="vehicles-panel">
            <div className="vehicles-panel-head">
              <h3>
                <PieChart size={17} />
                정비 유형별 통계 (이달)
              </h3>
            </div>
            <div className="vehicles-donut-row">
              <div className="vehicles-donut" style={donutBackground(maintTypeSegments)}>
                <div className="vehicles-donut-hole">
                  <strong>12건</strong>
                  <span>총</span>
                </div>
              </div>
              <ul className="vehicles-donut-legend">
                {maintTypeSegments.map((s) => (
                  <li key={s.label}>
                    <span className="dot" style={{ background: s.color }} />
                    <span className="name">{s.label}</span>
                    <strong>
                      {s.count} ({s.pct}%)
                    </strong>
                  </li>
                ))}
              </ul>
            </div>
          </section>

          <section className="vehicles-panel">
            <div className="vehicles-panel-head">
              <h3>
                <CircleDollarSign size={17} />
                월별 정비 비용 추이 (만원)
              </h3>
            </div>
            <CostTrendChart />
          </section>
        </div>
      </div>

      <div className="vehicles-bottom">
        <section className="vehicles-panel">
          <div className="vehicles-panel-head">
            <h3>
              <CalendarDays size={17} />
              예정 정비
            </h3>
            <Link to="/vehicles/maintenance/detail" className="vehicles-more">
              더보기 →
            </Link>
          </div>
          <ul className="vehicles-sched-list">
            {scheduledMaint.map((row) => (
              <li key={`${row.date}-${row.plate}`}>
                <span className="vehicles-sched-date">{row.date}</span>
                <div>
                  <strong>{row.plate}</strong>
                  <p>
                    {row.item} · <span className="due">{row.due}</span>
                  </p>
                </div>
                <Link className="btn btn-outline btn-xs" to="/vehicles/maintenance/detail">
                  상세
                </Link>
              </li>
            ))}
          </ul>
        </section>

        <section className="vehicles-panel">
          <div className="vehicles-panel-head">
            <h3>
              <TriangleAlert size={17} />
              정비 알림
            </h3>
            <Link to="/notifications" className="vehicles-more">
              더보기 →
            </Link>
          </div>
          <ul className="vehicles-alerts">
            {maintAlerts.map((a) => (
              <li key={a.text}>
                <span className={`bullet ${a.tone}`} />
                <div>
                  {a.text}
                  <time>{a.time}</time>
                </div>
              </li>
            ))}
          </ul>
        </section>

        <section className="vehicles-panel">
          <div className="vehicles-panel-head">
            <h3>
              <Gauge size={17} />
              정비 통계 요약
            </h3>
          </div>
          <div className="vehicles-summary-grid">
            {[
              { label: '완료율', value: '75%', meta: '12/16', tone: '' },
              { label: '평균 정비 주기', value: '45일', meta: '▼ 5일', tone: 'down' },
              { label: '평균 정비 비용', value: '405천원', meta: '▼ 12%', tone: 'down' },
              { label: '가동률', value: '92.6%', meta: '▲ 2.6%', tone: 'up' },
            ].map((s) => (
              <div key={s.label} className="vehicles-summary-card">
                <div className="label">{s.label}</div>
                <div className="value">{s.value}</div>
                <div className={`meta ${s.tone}`}>{s.meta}</div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}

/** ADM-08 사용자 관리 — Figma 430:19862 + public.users */
const rolePermMatrix = [
  { menu: '대시보드', admin: true, operator: true, user: true },
  { menu: '운행 관리', admin: true, operator: true, user: false },
  { menu: '차량 관리', admin: true, operator: true, user: false },
  { menu: '사용자 관리', admin: true, operator: false, user: false },
  { menu: '시스템 관리', admin: true, operator: false, user: false },
]

const recentLogins = [
  { name: '김관리', id: 'admin', time: '2026.07.20 09:32', ip: '203.241.xx.12' },
  { name: '김운영', id: 'operator1', time: '2026.07.20 08:15', ip: '203.241.xx.44' },
  { name: '박사용', id: 'user01', time: '2026.07.20 07:50', ip: '121.130.xx.8' },
  { name: '이운영', id: 'operator2', time: '2026.07.19 17:45', ip: '203.241.xx.51' },
  { name: '최사용', id: 'user02', time: '2026.07.18 14:20', ip: '175.223.xx.3' },
]

function roleTone(role: string): 'red' | 'orange' | 'purple' | 'gray' {
  if (role === '관리자') return 'red'
  if (role === '운영자') return 'orange'
  if (role.includes('일반')) return 'purple'
  return 'gray'
}

/** ADM-08 사용자 관리 */
export function UsersPage() {
  const [selected, setSelected] = useState(0)
  const [roleFilter, setRoleFilter] = useState('전체 역할')
  const [query, setQuery] = useState('')

  const list = useMemo(() => {
    return mockUsers.filter((u) => {
      if (roleFilter !== '전체 역할' && u.role !== roleFilter) return false
      if (!query.trim()) return true
      const q = query.trim().toLowerCase()
      return u.id.toLowerCase().includes(q) || u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    })
  }, [roleFilter, query])

  return (
    <div className="page users-page">
      <div className="users-kpis">
        {[
          { label: '전체 사용자', value: '36', unit: '명', hint: '활성 32명', tone: 'blue', icon: Users },
          { label: '관리자', value: '5', unit: '명', hint: '전체의 13.9%', tone: 'green', icon: Shield },
          { label: '운영자', value: '12', unit: '명', hint: '전체의 33.3%', tone: 'orange', icon: UserRound },
          { label: '일반 사용자', value: '19', unit: '명', hint: '전체의 52.8%', tone: 'purple', icon: Users },
        ].map((kpi) => {
          const Icon = kpi.icon
          return (
            <div key={kpi.label} className="users-kpi">
              <div className={`users-kpi-icon ${kpi.tone}`}>
                <Icon size={18} />
              </div>
              <div>
                <div className="label">{kpi.label}</div>
                <div className="value">
                  {kpi.value}
                  <em>{kpi.unit}</em>
                </div>
                <div className="hint">{kpi.hint}</div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="users-main">
        <section className="users-panel">
          <div className="users-panel-head">
            <h3>
              <ListChecks size={17} />
              사용자 목록 <span className="muted">(36명)</span>
            </h3>
          </div>

          <div className="users-filters">
            <select
              className="input"
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value)
                setSelected(0)
              }}
            >
              <option>전체 역할</option>
              <option>관리자</option>
              <option>운영자</option>
              <option>일반 사용자</option>
            </select>
            <label className="users-search">
              <Search size={14} />
              <input
                className="input"
                placeholder="이름, 아이디, 이메일 검색"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                  setSelected(0)
                }}
              />
            </label>
            <Link className="btn btn-primary btn-xs" to="/users/new">
              <Plus size={14} />
              사용자 추가
            </Link>
          </div>

          <div className="users-table-wrap">
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
                {list.map((row, idx) => (
                  <tr
                    key={row.id}
                    className={idx === selected ? 'selected' : undefined}
                    style={{ cursor: 'pointer' }}
                    onClick={() => setSelected(idx)}
                  >
                    <td style={{ fontWeight: 700 }}>{row.id}</td>
                    <td>{row.name}</td>
                    <td>{row.email}</td>
                    <td>
                      <StatusBadge tone={roleTone(row.role)}>{row.role}</StatusBadge>
                    </td>
                    <td>
                      <StatusBadge tone={row.status === '활성' ? 'green' : 'gray'}>{row.status}</StatusBadge>
                    </td>
                    <td>{row.lastLogin}</td>
                    <td>
                      <span className="users-manage">
                        <button
                          className="btn btn-outline btn-xs"
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation()
                            setSelected(idx)
                          }}
                        >
                          상세
                        </button>
                        <button type="button" className="users-more" aria-label="더보기">
                          <MoreHorizontal size={14} />
                        </button>
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="users-pager">
            <div className="users-pages">
              <button type="button">‹</button>
              {[1, 2, 3, 4].map((n) => (
                <button key={n} type="button" className={n === 1 ? 'active' : undefined}>
                  {n}
                </button>
              ))}
              <button type="button">›</button>
            </div>
            <select defaultValue="10개씩 보기">
              <option>10개씩 보기</option>
              <option>20개씩 보기</option>
              <option>50개씩 보기</option>
            </select>
          </div>
        </section>

        <div className="users-side">
          <section className="users-panel">
            <div className="users-panel-head">
              <h3>
                <Shield size={17} />
                역할 권한 설정
              </h3>
              <Link to="/settings#security" className="users-link">
                권한 가이드
              </Link>
            </div>
            <table className="users-perm-table">
              <thead>
                <tr>
                  <th>메뉴</th>
                  <th>관리자</th>
                  <th>운영자</th>
                  <th>일반</th>
                </tr>
              </thead>
              <tbody>
                {rolePermMatrix.map((row) => (
                  <tr key={row.menu}>
                    <td>{row.menu}</td>
                    <td>
                      <span className="users-perm-ok"><Check size={14} strokeWidth={2.5} /></span>
                    </td>
                    <td>
                      {row.operator ? (
                        <span className="users-perm-ok"><Check size={14} strokeWidth={2.5} /></span>
                      ) : (
                        <span className="users-perm-no"><Minus size={14} strokeWidth={2.5} /></span>
                      )}
                    </td>
                    <td>
                      {row.user ? (
                        <span className="users-perm-ok"><Check size={14} strokeWidth={2.5} /></span>
                      ) : (
                        <span className="users-perm-no"><Minus size={14} strokeWidth={2.5} /></span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

          <section className="users-panel">
            <div className="users-panel-head">
              <h3>
                <Clock3 size={17} />
                최근 로그인 기록
              </h3>
              <Link to="/system" className="users-link">
                전체 보기 →
              </Link>
            </div>
            <ul className="users-login-list">
              {recentLogins.map((log) => (
                <li key={`${log.id}-${log.time}`}>
                  <strong>
                    {log.name} <span>({log.id})</span>
                  </strong>
                  <span>
                    {log.time} · IP {log.ip}
                  </span>
                </li>
              ))}
            </ul>
          </section>

          <section className="users-panel">
            <div className="users-panel-head">
              <h3>
                <Settings2 size={17} />
                보안 정책
              </h3>
              <Link to="/settings#security" className="users-link">
                <Settings2 size={12} />
                정책 설정
              </Link>
            </div>
            <ul className="users-policy-list">
              <li>
                <span>비밀번호 최소 길이</span>
                <strong>10자 이상</strong>
              </li>
              <li>
                <span>비밀번호 변경 주기</span>
                <strong>90일</strong>
              </li>
              <li>
                <span>로그인 실패 제한</span>
                <strong>5회</strong>
              </li>
              <li>
                <span>세션 타임아웃</span>
                <strong>30분</strong>
              </li>
            </ul>
          </section>
        </div>
      </div>
    </div>
  )
}

const systemTypeSegments = [
  { label: '운영 기록', count: '1,362', pct: 55.4, color: '#266ef4' },
  { label: '사용자 활동', count: '736', pct: 29.9, color: '#3fb46a' },
  { label: '시스템 변경', count: '248', pct: 10.1, color: '#fdac38' },
  { label: '오류 / 경고', count: '112', pct: 4.6, color: '#eb4047' },
]

function systemDonutStyle(segments: { pct: number; color: string }[]) {
  let start = 0
  const parts = segments.map((s) => {
    const end = start + s.pct
    const part = `${s.color} ${start}% ${end}%`
    start = end
    return part
  })
  return { background: `conic-gradient(${parts.join(', ')})` }
}

/** ADM-09 시스템 관리 */
export function SystemPage() {
  const [typeFilter, setTypeFilter] = useState('전체')
  const [userFilter, setUserFilter] = useState('전체')
  const [query, setQuery] = useState('')

  const list = useMemo(() => {
    return systemLogs.filter((row) => {
      if (typeFilter !== '전체') {
        const map: Record<string, string[]> = {
          '운영 기록': ['운행 변경', '노선 수정', '차량 상태 변경', '공지사항 등록'],
          '사용자 활동': ['사용자 로그인', '로그인 실패', '권한 변경'],
          '시스템 변경': ['시스템 설정 변경', '데이터 내보내기'],
          '오류 / 경고': ['오류 발생', '로그인 실패'],
        }
        if (!(map[typeFilter] ?? []).includes(row.type)) return false
      }
      if (userFilter !== '전체' && !row.actor.includes(userFilter)) return false
      if (!query.trim()) return true
      const q = query.trim().toLowerCase()
      return (
        row.action.toLowerCase().includes(q) ||
        row.actor.toLowerCase().includes(q) ||
        row.type.toLowerCase().includes(q) ||
        row.target.toLowerCase().includes(q)
      )
    })
  }, [typeFilter, userFilter, query])

  return (
    <div className="page system-page">
      <section className="system-panel">
        <div className="system-panel-head">
          <h3>
            <Search size={17} />
            기록조회
          </h3>
        </div>
        <div className="system-filters">
          <label className="system-field">
            <span>기록 유형</span>
            <select className="input" value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
              <option>전체</option>
              <option>운영 기록</option>
              <option>사용자 활동</option>
              <option>시스템 변경</option>
              <option>오류 / 경고</option>
            </select>
          </label>
          <label className="system-field">
            <span>사용자</span>
            <select className="input" value={userFilter} onChange={(e) => setUserFilter(e.target.value)}>
              <option>전체</option>
              <option>김기사</option>
              <option>이운영</option>
              <option>박담당</option>
              <option>시스템</option>
            </select>
          </label>
          <label className="system-field">
            <span>기간</span>
            <div className="system-date">
              <CalendarDays size={14} />
              2026.07.13 ~ 2026.07.20
            </div>
          </label>
          <div className="system-field" style={{ flex: 1, minWidth: 220 }}>
            <span>키워드 검색</span>
            <div className="system-search">
              <Search size={14} />
              <input
                className="input"
                placeholder="검색어를 입력하세요"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
          </div>
          <div className="system-filter-actions">
            <button
              className="btn btn-outline"
              type="button"
              onClick={() => {
                setTypeFilter('전체')
                setUserFilter('전체')
                setQuery('')
              }}
            >
              초기화
            </button>
            <button className="btn btn-primary" type="button">
              조회하기
            </button>
          </div>
        </div>
      </section>

      <section className="system-panel">
        <div className="system-panel-head">
          <h3>
            <PieChart size={17} />
            기록 요약 <span className="muted">(2026.07.13 ~ 2026.07.20)</span>
          </h3>
          <button className="btn btn-outline btn-xs" type="button">
            <Download size={12} />
            엑셀 다운로드
          </button>
        </div>
        <div className="system-kpis">
          {[
            { label: '전체 기록 수', value: '2,458', unit: '건', hint: '일 평균 351건', tone: 'blue', icon: FileText },
            { label: '운영 기록', value: '1,362', unit: '건', hint: '55.4%', tone: 'green', icon: Settings2 },
            { label: '사용자 활동', value: '736', unit: '건', hint: '29.9%', tone: 'purple', icon: UserRound },
            { label: '시스템 변경', value: '248', unit: '건', hint: '10.1%', tone: 'orange', icon: PencilLine },
            { label: '오류 / 경고', value: '112', unit: '건', hint: '4.6%', tone: 'red', icon: TriangleAlert },
          ].map((kpi) => {
            const Icon = kpi.icon
            return (
              <div key={kpi.label} className="system-kpi">
                <div className={`system-kpi-icon ${kpi.tone}`}>
                  <Icon size={18} />
                </div>
                <div>
                  <div className="label">{kpi.label}</div>
                  <div className="value">
                    {kpi.value}
                    <em>{kpi.unit}</em>
                  </div>
                  <div className="hint">{kpi.hint}</div>
                </div>
              </div>
            )
          })}
        </div>
      </section>

      <div className="system-main">
        <section className="system-panel">
          <div className="system-panel-head">
            <h3>
              <ListChecks size={17} />
              시스템 기록 목록
            </h3>
            <select className="input" style={{ height: 30, fontSize: 12, minWidth: 110 }} defaultValue="10개씩 보기">
              <option>10개씩 보기</option>
              <option>20개씩 보기</option>
              <option>50개씩 보기</option>
            </select>
          </div>
          <div className="system-table-wrap">
            <table className="data-table dense">
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
                {list.map((row) => (
                  <tr key={`${row.time}-${row.action}`}>
                    <td>{row.time}</td>
                    <td
                      className={
                        row.type === '로그인 실패'
                          ? 'system-type-fail'
                          : row.type === '오류 발생'
                            ? 'system-type-error'
                            : undefined
                      }
                    >
                      {row.type}
                    </td>
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
          </div>
          <div className="system-pager">
            <button type="button">«</button>
            <button type="button">‹</button>
            {[1, 2, 3, 4, 5].map((n) => (
              <button key={n} type="button" className={n === 1 ? 'active' : undefined}>
                {n}
              </button>
            ))}
            <button type="button">›</button>
            <button type="button">»</button>
          </div>
          <p className="system-note">
            <Info size={12} />
            시스템 시간 기준으로 기록이 저장됩니다.
          </p>
        </section>

        <div className="system-side">
          <section className="system-panel">
            <div className="system-panel-head">
              <h3>
                <PieChart size={17} />
                기록 유형 분포
              </h3>
            </div>
            <div className="system-donut-wrap">
              <div className="system-donut" style={systemDonutStyle(systemTypeSegments)}>
                <div className="system-donut-hole">
                  <strong>2,458건</strong>
                  <span>총</span>
                </div>
              </div>
              <ul className="system-donut-legend">
                {systemTypeSegments.map((s) => (
                  <li key={s.label}>
                    <span className="dot" style={{ background: s.color }} />
                    <span className="name">{s.label}</span>
                    <strong>
                      {s.count} ({s.pct}%)
                    </strong>
                  </li>
                ))}
              </ul>
            </div>
          </section>

          <section className="system-panel system-policy">
            <div className="system-panel-head">
              <h3>
                <Settings2 size={17} />
                보관 정책
              </h3>
            </div>
            <p>시스템 기록은 1년간 보관됩니다. 보관 기간 이후 데이터는 자동 삭제됩니다.</p>
            <Link className="btn btn-outline" to="/settings#retention">
              보관 정책 관리
            </Link>
          </section>
        </div>
      </div>
    </div>
  )
}

const settingsNav = [
  { id: 'account', label: '계정·조직', icon: UserRound },
  { id: 'security', label: '보안 정책', icon: Shield },
  { id: 'notifications', label: '알림', icon: Bell },
  { id: 'operations', label: '운행·서비스', icon: Bus },
  { id: 'retention', label: '시스템·보관', icon: Database },
  { id: 'integrations', label: '연동 상태', icon: Link2 },
] as const

type SettingsSectionId = (typeof settingsNav)[number]['id']

/** 설정 — 운영 정책·알림·연동 (사용자/시스템 관리와 연결) */
export function SettingsPage() {
  const { user, usingSupabase } = useAuth()
  const location = useLocation()
  const [active, setActive] = useState<SettingsSectionId>('account')
  const [savedMsg, setSavedMsg] = useState('')

  const [pwMin, setPwMin] = useState('10')
  const [pwCycle, setPwCycle] = useState('90')
  const [loginFail, setLoginFail] = useState('5')
  const [sessionTimeout, setSessionTimeout] = useState('30')

  const [notifyEmail, setNotifyEmail] = useState(user?.email ?? 'admin@mju.ac.kr')
  const [pushNotice, setPushNotice] = useState(true)
  const [pushReport, setPushReport] = useState(true)
  const [pushMaint, setPushMaint] = useState(true)
  const [pushGps, setPushGps] = useState(true)

  const [timezone, setTimezone] = useState('Asia/Seoul')
  const [reportExpire, setReportExpire] = useState('24')
  const [gpsLossMin, setGpsLossMin] = useState('30')
  const [defaultRoute, setDefaultRoute] = useState('시내 셔틀')

  const [logRetain, setLogRetain] = useState('365')
  const [autoDelete, setAutoDelete] = useState(true)

  useEffect(() => {
    const hash = (location.hash || '#account').replace('#', '') as SettingsSectionId
    if (settingsNav.some((n) => n.id === hash)) {
      setActive(hash)
      const el = document.getElementById(`settings-${hash}`)
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [location.hash])

  const goSection = (id: SettingsSectionId) => {
    setActive(id)
    window.history.replaceState(null, '', `#${id}`)
    const el = document.getElementById(`settings-${id}`)
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const onSave = () => {
    setSavedMsg('설정이 저장되었습니다. (로컬 UI 반영)')
    window.setTimeout(() => setSavedMsg(''), 2500)
  }

  const roleLabel = user?.role === 'ADMIN' ? '관리자' : user?.role === 'DRIVER' ? '기사' : '학생'

  return (
    <div className="page settings-page">
      <div className="settings-top">
        <p className="page-subtitle">
          보안·알림·운행 정책을 관리합니다. 사용자 관리·시스템 관리 화면과 연결된 설정입니다.
        </p>
        <div className="settings-actions">
          {savedMsg ? <p className="settings-toast">{savedMsg}</p> : null}
          <button className="btn btn-outline" type="button" onClick={() => goSection('security')}>
            보안 바로가기
          </button>
          <button className="btn btn-primary" type="button" onClick={onSave}>
            변경사항 저장
          </button>
        </div>
      </div>

      <div className="settings-layout">
        <nav className="settings-nav" aria-label="설정 메뉴">
          {settingsNav.map((item) => {
            const Icon = item.icon
            return (
              <button
                key={item.id}
                type="button"
                className={active === item.id ? 'active' : undefined}
                onClick={() => goSection(item.id)}
              >
                <Icon size={16} />
                {item.label}
              </button>
            )
          })}
        </nav>

        <div className="settings-content">
          <section className="settings-panel" id="settings-account">
            <div className="settings-panel-head">
              <div>
                <h3>
                  <UserRound size={17} />
                  계정·조직
                </h3>
                <p className="settings-panel-desc">현재 로그인 관리자 계정과 조직 기본 정보입니다.</p>
              </div>
            </div>
            <div className="settings-profile">
              <div className="settings-avatar">{(user?.name ?? '관').slice(0, 1)}</div>
              <div>
                <strong>{user?.name ?? '관리자'}</strong>
                <span>
                  {user?.email ?? 'admin@mju.ac.kr'} · {roleLabel}
                </span>
              </div>
            </div>
            <div className="settings-grid">
              <div className="settings-field">
                <label htmlFor="org-name">운영 기관</label>
                <input id="org-name" className="input" defaultValue="명지대학교 셔틀 운영팀" />
              </div>
              <div className="settings-field">
                <label htmlFor="org-contact">대표 문의 이메일</label>
                <input id="org-contact" className="input" defaultValue="onda@mju.ac.kr" />
              </div>
              <div className="settings-field full">
                <label htmlFor="org-desc">서비스 소개 (학생 앱 노출)</label>
                <input id="org-desc" className="input" defaultValue="ON-DA 캠퍼스 셔틀 실시간 안내 서비스" />
              </div>
            </div>
            <div className="settings-links">
              <Link className="btn btn-outline btn-xs" to="/users">
                <Users size={12} />
                사용자 관리로 이동
              </Link>
              <Link className="btn btn-outline btn-xs" to="/drivers">
                <UserRound size={12} />
                기사 계정 관리
              </Link>
            </div>
          </section>

          <section className="settings-panel" id="settings-security">
            <div className="settings-panel-head">
              <div>
                <h3>
                  <Shield size={17} />
                  보안 정책
                </h3>
                <p className="settings-panel-desc">
                  사용자 관리 &gt; 보안 정책과 동일한 항목입니다. 변경 시 관리자 로그인·세션에 적용됩니다.
                </p>
              </div>
              <Link className="btn btn-outline btn-xs" to="/users">
                사용자 관리
              </Link>
            </div>
            <div className="settings-grid">
              <div className="settings-field">
                <label htmlFor="pw-min">비밀번호 최소 길이</label>
                <input id="pw-min" className="input" value={pwMin} onChange={(e) => setPwMin(e.target.value)} />
                <span className="hint">권장 10자 이상</span>
              </div>
              <div className="settings-field">
                <label htmlFor="pw-cycle">비밀번호 변경 주기 (일)</label>
                <input id="pw-cycle" className="input" value={pwCycle} onChange={(e) => setPwCycle(e.target.value)} />
              </div>
              <div className="settings-field">
                <label htmlFor="login-fail">로그인 실패 제한 (회)</label>
                <input id="login-fail" className="input" value={loginFail} onChange={(e) => setLoginFail(e.target.value)} />
              </div>
              <div className="settings-field">
                <label htmlFor="session-to">세션 타임아웃 (분)</label>
                <input
                  id="session-to"
                  className="input"
                  value={sessionTimeout}
                  onChange={(e) => setSessionTimeout(e.target.value)}
                />
              </div>
            </div>
          </section>

          <section className="settings-panel" id="settings-notifications">
            <div className="settings-panel-head">
              <div>
                <h3>
                  <Bell size={17} />
                  알림
                </h3>
                <p className="settings-panel-desc">
                  공지·제보·정비·GPS 이상 알림 수신 설정입니다. 공지 관리의 푸시 발송과 연동됩니다.
                </p>
              </div>
              <Link className="btn btn-outline btn-xs" to="/notices">
                공지 관리
              </Link>
            </div>
            <div className="settings-grid" style={{ marginBottom: 12 }}>
              <div className="settings-field full">
                <label htmlFor="notify-email">알림 수신 이메일</label>
                <input
                  id="notify-email"
                  className="input"
                  value={notifyEmail}
                  onChange={(e) => setNotifyEmail(e.target.value)}
                />
              </div>
            </div>
            <div className="settings-grid">
              {[
                { key: 'notice', label: '긴급·공지 푸시', desc: '공지 등록/긴급 발송 시 관리자 알림', on: pushNotice, set: setPushNotice },
                { key: 'report', label: '제보 접수 알림', desc: '커뮤니티 제보 등록·장시간 미처리 알림', on: pushReport, set: setPushReport },
                { key: 'maint', label: '정비 일정 알림', desc: '예정 정비·점검 필요 차량 알림', on: pushMaint, set: setPushMaint },
                { key: 'gps', label: 'GPS 미수신 알림', desc: '실시간 운행 장시간 미수신 경고', on: pushGps, set: setPushGps },
              ].map((row) => (
                <div key={row.key} className="settings-switch-row full">
                  <div>
                    <strong>{row.label}</strong>
                    <p>{row.desc}</p>
                  </div>
                  <label className="settings-switch">
                    <input type="checkbox" checked={row.on} onChange={(e) => row.set(e.target.checked)} />
                    <span />
                  </label>
                </div>
              ))}
            </div>
            <div className="settings-links">
              <Link className="btn btn-outline btn-xs" to="/reports">
                제보 관리
              </Link>
              <Link className="btn btn-outline btn-xs" to="/live">
                실시간 운행
              </Link>
              <Link className="btn btn-outline btn-xs" to="/vehicles">
                차량 관리
              </Link>
            </div>
          </section>

          <section className="settings-panel" id="settings-operations">
            <div className="settings-panel-head">
              <div>
                <h3>
                  <Bus size={17} />
                  운행·서비스
                </h3>
                <p className="settings-panel-desc">
                  제보 자동 만료, GPS 미수신 기준, 기본 노선 등 운영 기본값입니다.
                </p>
              </div>
              <Link className="btn btn-outline btn-xs" to="/routes">
                노선 관리
              </Link>
            </div>
            <div className="settings-grid">
              <div className="settings-field">
                <label htmlFor="tz">기본 타임존</label>
                <select id="tz" className="input" value={timezone} onChange={(e) => setTimezone(e.target.value)}>
                  <option value="Asia/Seoul">Asia/Seoul (KST)</option>
                  <option value="UTC">UTC</option>
                </select>
              </div>
              <div className="settings-field">
                <label htmlFor="default-route">기본 노선 (미리보기)</label>
                <select
                  id="default-route"
                  className="input"
                  value={defaultRoute}
                  onChange={(e) => setDefaultRoute(e.target.value)}
                >
                  {mockRoutes.map((r) => (
                    <option key={r.name}>{r.name}</option>
                  ))}
                </select>
              </div>
              <div className="settings-field">
                <label htmlFor="report-expire">제보 자동 만료 (시간)</label>
                <input
                  id="report-expire"
                  className="input"
                  value={reportExpire}
                  onChange={(e) => setReportExpire(e.target.value)}
                />
                <span className="hint">제보 관리 화면의 24시간 만료와 연동</span>
              </div>
              <div className="settings-field">
                <label htmlFor="gps-loss">GPS 장시간 미수신 기준 (분)</label>
                <input
                  id="gps-loss"
                  className="input"
                  value={gpsLossMin}
                  onChange={(e) => setGpsLossMin(e.target.value)}
                />
                <span className="hint">실시간 운행 관제 경고 기준</span>
              </div>
            </div>
          </section>

          <section className="settings-panel" id="settings-retention">
            <div className="settings-panel-head">
              <div>
                <h3>
                  <Database size={17} />
                  시스템·보관
                </h3>
                <p className="settings-panel-desc">
                  시스템 관리 &gt; 보관 정책과 연결됩니다. 기록 보관 기간과 자동 삭제 여부를 설정합니다.
                </p>
              </div>
              <Link className="btn btn-outline btn-xs" to="/system">
                시스템 관리
              </Link>
            </div>
            <div className="settings-grid">
              <div className="settings-field">
                <label htmlFor="log-retain">시스템 기록 보관 기간 (일)</label>
                <input id="log-retain" className="input" value={logRetain} onChange={(e) => setLogRetain(e.target.value)} />
                <span className="hint">기본 365일 (1년)</span>
              </div>
              <div className="settings-field">
                <span className="label">자동 삭제</span>
                <div className="settings-switch-row" style={{ padding: '8px 12px' }}>
                  <div>
                    <strong>보관 기간 이후 자동 삭제</strong>
                    <p>만료된 시스템 기록을 자동으로 정리합니다.</p>
                  </div>
                  <label className="settings-switch">
                    <input type="checkbox" checked={autoDelete} onChange={(e) => setAutoDelete(e.target.checked)} />
                    <span />
                  </label>
                </div>
              </div>
            </div>
          </section>

          <section className="settings-panel" id="settings-integrations">
            <div className="settings-panel-head">
              <div>
                <h3>
                  <Link2 size={17} />
                  연동 상태
                </h3>
                <p className="settings-panel-desc">Supabase·알림·지도 등 외부/백엔드 연동 상태입니다.</p>
              </div>
            </div>
            <div className="settings-status">
              <div className="settings-status-item">
                <div>
                  <strong>Supabase Auth / DB</strong>
                  <p>{usingSupabase ? '환경 변수가 설정되어 있습니다.' : '미설정 — 로컬 mock/데모 모드로 동작합니다.'}</p>
                </div>
                <span className={`settings-pill ${usingSupabase ? 'ok' : 'warn'}`}>
                  {usingSupabase ? '연결됨' : '데모 모드'}
                </span>
              </div>
              <div className="settings-status-item">
                <div>
                  <strong>푸시 알림 채널</strong>
                  <p>공지·긴급 알림 발송용 채널 (UI 준비 완료)</p>
                </div>
                <span className="settings-pill warn">연동 대기</span>
              </div>
              <div className="settings-status-item">
                <div>
                  <strong>네이버 지도 (Dynamic Map)</strong>
                  <p>
                    {isNaverMapConfigured()
                      ? 'Client ID 설정됨 · 지도 1회 로드 + 마커 setPosition 방식'
                      : '미설정 — map.png 폴백. MAPS.md 참고해 VITE_NAVER_MAP_CLIENT_ID 추가'}
                  </p>
                </div>
                <span className={`settings-pill ${isNaverMapConfigured() ? 'ok' : 'warn'}`}>
                  {isNaverMapConfigured() ? '연동 준비됨' : '키 필요'}
                </span>
              </div>
            </div>
            <div className="settings-links">
              <Link className="btn btn-outline btn-xs" to="/dashboard">
                <Building2 size={12} />
                대시보드
              </Link>
              <Link className="btn btn-outline btn-xs" to="/live">
                <RadioTower size={12} />
                실시간 운행
              </Link>
            </div>
          </section>

          <div className="settings-actions">
            <button className="btn btn-outline" type="button" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
              맨 위로
            </button>
            <button className="btn btn-primary" type="button" onClick={onSave}>
              변경사항 저장
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
