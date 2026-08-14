import {
  AlertTriangle,
  Bus,
  CalendarDays,
  Megaphone,
  MessageSquareWarning,
  RadioTower,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  fetchLiveVehicles,
  fetchRecentOperationFeed,
  type LiveSnapshot,
  type LiveVehicle,
  type RecentOpFeedItem,
} from '../lib/liveApi'
import {
  countStudentReports,
  fetchNotices,
  subscribeStudentReports,
  type NoticeRow,
} from '../lib/api'
import { todayDateKey } from '../types/assignment'
import { isSupabaseConfigured, supabase } from '../lib/supabase'
import { createExclusivePoll } from '../lib/exclusivePoll'
import { StatCard, StatusBadge } from '../components/ui/Form'

const iconNodes = [CalendarDays, Bus, Bus, RadioTower, MessageSquareWarning, Megaphone]

const reportLegend = [
  { label: '승하차 불편', color: '#9870d7' },
  { label: '운행 문의', color: '#fea907' },
  { label: '기사 서비스', color: '#3fb46a' },
  { label: '분실물', color: '#266ef4' },
  { label: '기타', color: '#c4c9d7' },
]

const emptySnap: LiveSnapshot = {
  vehicles: [],
  stats: { ok: 0, none: 0, error: 0, total: 0, rate: 0, inProgress: 0, ended: 0, idle: 0, stopped: 0 },
}

function shiftDateKey(dateKey: string, days: number): string {
  const [y, m, d] = dateKey.split('-').map(Number)
  const dt = new Date(y, m - 1, d)
  dt.setDate(dt.getDate() + days)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}`
}

function deltaLabel(today: number, yesterday: number | null, unit: string): string {
  if (yesterday == null) return '오늘 기준'
  const diff = today - yesterday
  if (diff === 0) return `전일과 동일 (${yesterday}${unit})`
  const sign = diff > 0 ? '+' : ''
  return `${sign}${diff}${unit} (전일 대비)`
}

async function countOperationsOn(dateKey: string): Promise<number | null> {
  if (!isSupabaseConfigured) return null
  const { count, error } = await supabase
    .from('operations')
    .select('*', { count: 'exact', head: true })
    .eq('operation_date', dateKey)
  if (error) return null
  return count ?? 0
}

function hasGpsIssue(v: LiveVehicle): boolean {
  return v.gpsKind === 'error' || (v.status === 'in_progress' && v.gpsKind !== 'ok')
}

function formatAlertTime(updatedAt: number): string {
  if (!updatedAt) return '—'
  const d = new Date(updatedAt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function gpsIssueLabel(v: LiveVehicle): string {
  if (v.gpsKind === 'error') return 'GPS 오류'
  if (v.gpsKind === 'none') return '신호 미수신'
  return '정상'
}

type GpsAlertRow = {
  id: string
  bus: string
  route: string
  location: string
  issue: string
  time: string
  status: string
  tone: 'green' | 'orange' | 'red' | 'gray'
  hasIssue: boolean
}

/** 1) 이상 → 2) 최근 운행(updatedAt) → 3) 나머지 */
function buildGpsAlertRows(vehicles: LiveVehicle[], limit = 8): GpsAlertRow[] {
  const sorted = [...vehicles].sort((a, b) => {
    const ai = hasGpsIssue(a) ? 1 : 0
    const bi = hasGpsIssue(b) ? 1 : 0
    if (ai !== bi) return bi - ai
    if (a.updatedAt !== b.updatedAt) return b.updatedAt - a.updatedAt
    return a.vehicleName.localeCompare(b.vehicleName, 'ko')
  })

  return sorted.slice(0, limit).map((v) => {
    const issue = hasGpsIssue(v)
    return {
      id: v.id,
      bus: v.vehicleName || '미정',
      route: v.routeName || '—',
      location: v.stop || (v.lat != null && v.lng != null ? `${v.lat.toFixed(4)}, ${v.lng.toFixed(4)}` : '위치 없음'),
      issue: gpsIssueLabel(v),
      time: formatAlertTime(v.updatedAt),
      status: issue ? '확인 필요' : v.gpsKind === 'ok' ? '정상' : '대기',
      tone: issue ? (v.gpsKind === 'error' ? 'red' : 'orange') : v.gpsKind === 'ok' ? 'green' : 'gray',
      hasIssue: issue,
    }
  })
}

function isLiveUrgentNotice(row: NoticeRow): boolean {
  if (row.type !== 'URGENT') return false
  if (row.status === 'ENDED') return false
  const now = Date.now()
  if (row.ends_at) {
    const end = Date.parse(row.ends_at)
    if (Number.isFinite(end) && end < now) return false
  }
  if (row.starts_at) {
    const start = Date.parse(row.starts_at)
    if (Number.isFinite(start) && start > now) return false
  }
  return true
}

function noticeTitlePlain(title: string): string {
  return title.replace(/^\[.*?\]\s*/, '')
}

function truncateNoticeText(value: string, max = 10): string {
  const text = value.trim()
  if (text.length <= max) return text
  return `${text.slice(0, max)}...`
}

function noticePeriodLabel(row: NoticeRow): string {
  const fmt = (iso: string | null | undefined) => {
    if (!iso) return ''
    const d = new Date(iso)
    if (!Number.isFinite(d.getTime())) return ''
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
  }
  const start = fmt(row.starts_at)
  const end = fmt(row.ends_at)
  if (!start && !end) return '-'
  if (start && end) return `${start} ~ ${end}`
  return start || end
}

function noticeBodyPreview(html: string, max = 10): string {
  const text = html
    .replace(/<div[^>]*onda-notice-attach[\s\S]*?<\/div>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  if (!text) return ''
  return truncateNoticeText(text, max)
}

/** ADM-02 대시보드 — Figma 위젯 구조 반영 */
export function DashboardPage() {
  const navigate = useNavigate()
  const [snapshot, setSnapshot] = useState<LiveSnapshot>(emptySnap)
  const [yesterdayTotal, setYesterdayTotal] = useState<number | null>(null)
  const [recentOpsFeed, setRecentOpsFeed] = useState<RecentOpFeedItem[]>([])
  const [urgentNotices, setUrgentNotices] = useState<NoticeRow[]>([])
  const [studentReportStats, setStudentReportStats] = useState<{
    total: number
    pending: number
  } | null>(null)

  useEffect(() => {
    let alive = true
    const load = createExclusivePoll(async () => {
      const snap = await fetchLiveVehicles()
      if (!alive) return
      setSnapshot((prev) => {
        if (
          prev.stats.total === snap.stats.total &&
          prev.stats.inProgress === snap.stats.inProgress &&
          prev.stats.idle === snap.stats.idle &&
          prev.stats.ended === snap.stats.ended &&
          prev.stats.error === snap.stats.error &&
          prev.stats.ok === snap.stats.ok &&
          prev.stats.none === snap.stats.none &&
          prev.vehicles.length === snap.vehicles.length &&
          prev.vehicles.every((v, i) => {
            const n = snap.vehicles[i]
            return (
              n &&
              v.id === n.id &&
              v.gpsKind === n.gpsKind &&
              v.status === n.status &&
              v.updatedAt === n.updatedAt &&
              v.stop === n.stop
            )
          })
        ) {
          return prev
        }
        return snap
      })
    })
    void load()
    const timer = window.setInterval(() => {
      void load()
    }, 20_000)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [])

  useEffect(() => {
    let alive = true
    const loadRecent = createExclusivePoll(async () => {
      const rows = await fetchRecentOperationFeed(8)
      if (alive) setRecentOpsFeed(rows)
    })
    void loadRecent()
    const timer = window.setInterval(() => {
      void loadRecent()
    }, 20_000)
    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [])

  useEffect(() => {
    let alive = true
    void countOperationsOn(shiftDateKey(todayDateKey(), -1)).then((n) => {
      if (alive) setYesterdayTotal(n)
    })
    return () => {
      alive = false
    }
  }, [])

  useEffect(() => {
    let alive = true
    void fetchNotices().then((res) => {
      if (!alive) return
      setUrgentNotices(res.rows.filter(isLiveUrgentNotice).slice(0, 3))
    })
    return () => {
      alive = false
    }
  }, [])

  // 전체 학생 제보 건수 — reports DB + Realtime
  useEffect(() => {
    let alive = true
    const refresh = createExclusivePoll(async () => {
      const stats = await countStudentReports()
      if (!alive || !stats) return
      setStudentReportStats((prev) => {
        if (prev && prev.total === stats.total && prev.pending === stats.pending) return prev
        return stats
      })
    })
    void refresh()
    const unsub = subscribeStudentReports(() => {
      void refresh()
    })
    const timer = window.setInterval(() => {
      void refresh()
    }, 20_000)
    return () => {
      alive = false
      unsub()
      window.clearInterval(timer)
    }
  }, [])

  const vehicles = snapshot.vehicles

  const gpsAlertRows = useMemo(() => buildGpsAlertRows(vehicles), [vehicles])
  const gpsIssueCount = useMemo(() => vehicles.filter(hasGpsIssue).length, [vehicles])

  const kpiCards = useMemo(() => {
    const { stats } = snapshot
    const assignedBuses = new Set(
      vehicles.map((v) => v.vehicleName).filter((name) => name && name !== '미정'),
    ).size
    const gpsIssues = gpsIssueCount

    const liveCards = [
      {
        title: '오늘 배정 운행 수',
        value: stats.total,
        unit: '건',
        delta: deltaLabel(stats.total, yesterdayTotal, '건'),
        color: '#266ef4',
      },
      {
        title: '현재 운행 중 차량',
        value: stats.inProgress,
        unit: '대',
        delta: `대기 ${stats.idle} · 종료 ${stats.ended}`,
        color: '#3fb46a',
      },
      {
        title: '운행 배정 차량',
        value: assignedBuses,
        unit: '대',
        delta: `오늘 배차 ${stats.total}건`,
        color: '#fdac38',
      },
      {
        title: 'GPS·통신 이상',
        value: gpsIssues,
        unit: '건',
        delta: gpsIssues > 0 ? '확인 필요' : '이상 없음',
        color: '#eb4047',
      },
      {
        title: '전체 학생 제보',
        value: studentReportStats?.total ?? 0,
        unit: '건',
        delta: studentReportStats
          ? `처리 대기 ${studentReportStats.pending}건`
          : isSupabaseConfigured
            ? '불러오는 중…'
            : 'DB 미설정',
        color: '#7964f2',
      },
    ]

    return [
      ...liveCards,
      {
        title: '긴급 공지',
        value: urgentNotices.length,
        unit: '건',
        delta: urgentNotices.length > 0 ? '게시 중' : '없음',
        color: '#ec181b',
      },
    ]
  }, [snapshot, vehicles, yesterdayTotal, gpsIssueCount, studentReportStats, urgentNotices.length])

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
            <button
              className="btn btn-ghost"
              type="button"
              style={{ height: 30, fontSize: 12 }}
              onClick={() => navigate('/live')}
            >
              실시간 운행 보기
            </button>
          </div>
          <button
            type="button"
            className="map-frame dash-map-preview"
            onClick={() => navigate('/live')}
            aria-label="실시간 운행 지도로 이동"
          >
            <img src="/dash-live-map.png" alt="" className="dash-map-preview-img" />
          </button>
        </section>

        <section className="card card-pad">
          <div className="card-head">
            <h3>최근 운행 현황</h3>
            <Link to="/live" className="muted" style={{ fontSize: 12 }}>
              모든 운행 보기
            </Link>
          </div>
          <div className="ops-list">
            {recentOpsFeed.length === 0 ? (
              <div className="muted" style={{ fontSize: 13, padding: '12px 0' }}>
                오늘 표시할 운행 기록이 없습니다.
              </div>
            ) : (
              recentOpsFeed.map((item) => (
                <div className="ops-item" key={item.id}>
                  <StatusBadge tone={item.tone}>{item.status}</StatusBadge>
                  <div className="ops-body">
                    <strong>{item.route}</strong>
                    <span>
                      {item.bus} · {item.driver}
                    </span>
                  </div>
                  <span className="ops-time">{item.time}</span>
                </div>
              ))
            )}
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
                총 {studentReportStats?.total ?? '—'}건
                <br />
                전체 제보
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
            {studentReportStats
              ? `처리 대기 ${studentReportStats.pending}건 · Realtime 반영`
              : '학생 앱 제보와 동일한 reports DB를 사용합니다.'}
          </p>
        </section>
      </div>

      <div className="dash-bottom">
        <section className="card card-pad">
          <div className="card-head">
            <h3>GPS·통신 이상 경고</h3>
            <button
              className="btn btn-ghost"
              type="button"
              style={{ height: 30, fontSize: 12 }}
              onClick={() => navigate('/live')}
            >
              실시간 운행 보기
            </button>
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
              {gpsIssueCount === 0 ? (
                <tr>
                  <td>-</td>
                  <td>-</td>
                  <td>-</td>
                  <td>-</td>
                  <td>-</td>
                  <td>
                    <StatusBadge tone="green">이상 없음</StatusBadge>
                  </td>
                </tr>
              ) : (
                gpsAlertRows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.bus}</td>
                    <td>{row.route}</td>
                    <td>{row.location}</td>
                    <td>{row.issue}</td>
                    <td>{row.time}</td>
                    <td>
                      <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </section>

        <section className="card card-pad">
          <div className="card-head">
            <h3>최근 긴급 공지</h3>
          </div>
          {urgentNotices.length === 0 ? (
            <div className="muted" style={{ fontSize: 13, padding: '8px 0 0' }}>
              게시 중인 긴급 공지가 없습니다.
            </div>
          ) : (
            <div className="alert-stack" style={{ gap: 4, marginTop: 4 }}>
              {urgentNotices.map((row) => (
                <div
                  key={row.id}
                  className="alert alert-danger"
                  style={{ display: 'flex', alignItems: 'flex-start', gap: 8, padding: '4px 10px', margin: 0 }}
                >
                  <AlertTriangle size={14} style={{ flexShrink: 0, marginTop: 1 }} />
                  <div style={{ lineHeight: 1.25 }}>
                    <strong>{truncateNoticeText(noticeTitlePlain(row.title))}</strong>
                    <div>{noticePeriodLabel(row)}</div>
                    {noticeBodyPreview(row.content) ? <div>{noticeBodyPreview(row.content)}</div> : null}
                  </div>
                </div>
              ))}
            </div>
          )}
          <div style={{ marginTop: 8, textAlign: 'right' }}>
            <Link to="/notices" className="muted" style={{ fontSize: 12, color: 'var(--color-primary)' }}>
              공지 전체 보기 &gt;
            </Link>
          </div>
        </section>
      </div>
    </div>
  )
}
