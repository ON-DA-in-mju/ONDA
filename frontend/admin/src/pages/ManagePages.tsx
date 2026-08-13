import { useEffect, useMemo, useRef, useState, type ChangeEvent, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
  AlertTriangle,
  AlignCenter,
  AlignJustify,
  AlignLeft,
  AlignRight,
  Bell,
  Bold,
  CalendarDays,
  ChevronRight,
  Eye,
  Italic,
  List,
  ListOrdered,
  Megaphone,
  Paperclip,
  Underline,
} from 'lucide-react'
import { maintenances, notices, reports, routes, systemLogs as mockSystemLogs, users } from '../data/mock'
import {
  createNotice,
  endNotice,
  fetchNotices,
  fetchReports,
  fetchRoutes,
  fetchUsers,
  updateNotice,
  type NoticeRow,
  type ReportRow,
  type RouteRow,
  type UserRow,
} from '../lib/api'
import type { NoticeAudience, NoticeStatus, NoticeType } from '../types/database'
import { noticeAudienceLabel, normalizeNoticeAudience } from '../lib/noticeAudience'
import { fetchLoginHistory, toLastLoginDisplay, type LoginHistoryEntry } from '../lib/loginHistoryApi'
import { fetchGpsReceiveLogs, GPS_LOGS_MAX, type GpsReceiveLog } from '../lib/gpsLogsApi'
import { isSupabaseConfigured } from '../lib/supabase'
import { fetchSystemLogs, type SystemLogRow } from '../lib/systemLogsApi'
import { useAuth } from '../state/AuthContext'
import { StatusBadge } from '../components/ui/Form'
import { ListPagination } from '../components/ui/ListPagination'
import '../styles/figma-pages.css'

const reportStatusKo: Record<string, string> = {
  PENDING: '처리 대기',
  PROCESSING: '검토 중',
  COMPLETED: '처리 완료',
}

const NOTICE_BODY_MAX = 2000
const NOTICE_PAGE_SIZE = 10
const USERS_PAGE_SIZE = 10
const REPORTS_PAGE_SIZE = 10
const ROUTES_PAGE_SIZE = 10
const SYSTEM_LOGS_PAGE_SIZE = 5
const GPS_LOGS_PAGE_SIZE = 10

const DEFAULT_NOTICE_BODY = [
  '폭설로 인해 일부 노선의 운행이 지연되고 있습니다.',
  '자세한 내용은 노선별 운행 정보에서 확인해 주세요.',
  '이용에 불편을 드려 죄송합니다.',
].join('\n')

function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function plainTextToEditorHtml(text: string): string {
  if (!text.trim()) return ''
  return text
    .split('\n')
    .map((line) => `<div>${line ? escapeHtml(line) : '<br>'}</div>`)
    .join('')
}

function htmlToPlainText(html: string): string {
  const el = document.createElement('div')
  el.innerHTML = html
  return el.innerText.replace(/\u00a0/g, ' ')
}

const NOTICE_TYPE_KO: Record<NoticeType, string> = {
  URGENT: '긴급',
  IMPORTANT: '중요',
  OPERATION_CHANGE: '운행 변경',
  GENERAL: '일반',
}

const NOTICE_TYPE_FROM_KO: Record<string, NoticeType> = {
  긴급: 'URGENT',
  중요: 'IMPORTANT',
  '운행 변경': 'OPERATION_CHANGE',
  일반: 'GENERAL',
}

function noticeTypeTone(type: string): 'red' | 'orange' | 'blue' | 'gray' {
  if (type === '긴급' || type === 'URGENT' || type === '중요' || type === 'IMPORTANT') return 'red'
  if (type === '운행 변경' || type === 'OPERATION_CHANGE' || type === '일반' || type === 'GENERAL') {
    return 'blue'
  }
  return 'gray'
}

function noticeTypeLabel(row: NoticeRow): string {
  if (row.type && NOTICE_TYPE_KO[row.type]) return NOTICE_TYPE_KO[row.type]
  const m = row.title.match(/^\[(.+?)\]/)
  if (m?.[1]) return m[1].replace(/\s*공지$/, '')
  return '일반'
}

function noticeTitlePlain(row: NoticeRow): string {
  return row.title.replace(/^\[.*?\]\s*/, '')
}

function truncateChars(value: string, max: number): string {
  if (value.length <= max) return value
  return `${value.slice(0, max)}...`
}

function formatNoticeDate(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (!Number.isFinite(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

function noticePeriodLabel(row: NoticeRow): string {
  const start = formatNoticeDate(row.starts_at)
  const end = formatNoticeDate(row.ends_at)
  if (!start && !end) return '-'
  if (start && end) return `${start} ~ ${end}`
  return start || end
}

function todayDateInputValue(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function noticeStatusLabel(row: NoticeRow): '예약' | '게시 중' | '종료' {
  if (row.status === 'ENDED') return '종료'
  const now = Date.now()
  if (row.ends_at) {
    const end = Date.parse(row.ends_at)
    if (Number.isFinite(end) && end < now) return '종료'
  }
  if (row.status === 'SCHEDULED' || row.starts_at) {
    const start = row.starts_at ? Date.parse(row.starts_at) : NaN
    if (Number.isFinite(start) && start > now) return '예약'
  }
  return '게시 중'
}

function noticeStatusTone(label: '예약' | '게시 중' | '종료'): 'orange' | 'blue' | 'gray' {
  if (label === '예약') return 'orange'
  if (label === '게시 중') return 'blue'
  return 'gray'
}

function resolveNoticeWriteStatus(opts: {
  permanent: boolean
  startsAt: string | null
  endsAt: string | null
}): NoticeStatus {
  const now = Date.now()
  if (!opts.permanent && opts.endsAt) {
    const end = Date.parse(opts.endsAt)
    if (Number.isFinite(end) && end < now) return 'ENDED'
  }
  if (!opts.permanent && opts.startsAt) {
    const start = Date.parse(opts.startsAt)
    if (Number.isFinite(start) && start > now) return 'SCHEDULED'
  }
  return 'PUBLISHED'
}

function noticeViewCount(row: NoticeRow): number {
  const extra = row as NoticeRow & { view_count?: number | null; views?: number | null }
  return Number(extra.view_count ?? extra.views ?? 0) || 0
}

type EditorCmdState = {
  bold: boolean
  italic: boolean
  underline: boolean
  ul: boolean
  ol: boolean
  left: boolean
  center: boolean
  right: boolean
  full: boolean
}

const emptyEditorCmds: EditorCmdState = {
  bold: false,
  italic: false,
  underline: false,
  ul: false,
  ol: false,
  left: false,
  center: false,
  right: false,
  full: false,
}

/** ADM-05 커뮤니티 제보 관리 */
export function ReportsPage() {
  const [selected, setSelected] = useState(0)
  const [listPage, setListPage] = useState(1)
  const [dbReports, setDbReports] = useState<ReportRow[] | null>(null)

  useEffect(() => {
    void fetchReports().then((data) => {
      if (data) setDbReports(data)
    })
  }, [])

  const usingDb = Boolean(dbReports && dbReports.length >= 0 && isSupabaseConfigured && dbReports !== null)
  const reportTotal = dbReports?.length ?? reports.length
  const reportPageCount = Math.max(1, Math.ceil(reportTotal / REPORTS_PAGE_SIZE))
  const safeListPage = Math.min(listPage, reportPageCount)
  const pagedDbReports = useMemo(() => {
    if (!dbReports) return null
    const start = (safeListPage - 1) * REPORTS_PAGE_SIZE
    return dbReports.slice(start, start + REPORTS_PAGE_SIZE)
  }, [dbReports, safeListPage])
  const pagedMockReports = useMemo(() => {
    if (dbReports) return null
    const start = (safeListPage - 1) * REPORTS_PAGE_SIZE
    return reports.slice(start, start + REPORTS_PAGE_SIZE)
  }, [dbReports, safeListPage])

  useEffect(() => {
    if (listPage > reportPageCount) setListPage(reportPageCount)
  }, [listPage, reportPageCount])

  const item = usingDb && dbReports?.[selected] ? dbReports[selected] : null
  const mockItem = reports[Math.min(selected, reports.length - 1)] ?? reports[0]

  return (
    <div className="page">
      <p className="page-subtitle">
        학생들의 제보를 검토하고 신뢰도를 관리하는 공간입니다.
        {isSupabaseConfigured ? (dbReports ? ` · Supabase reports ${dbReports.length}건` : ' · DB 로딩/권한 확인') : ' · mock'}
      </p>
      <div className="grid grid-4">
        {[
          ['오늘 제보 수', `${dbReports?.length ?? 38}건`, dbReports ? 'DB' : '+8 전일 대비', 'blue'],
          ['처리 대기', `${dbReports?.filter((r) => r.status === 'PENDING').length ?? 12}건`, 'PENDING', 'orange'],
          ['처리 중', `${dbReports?.filter((r) => r.status === 'PROCESSING').length ?? 4}건`, 'PROCESSING', 'green'],
          ['완료', `${dbReports?.filter((r) => r.status === 'COMPLETED').length ?? 26}건`, 'COMPLETED', 'gray'],
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
          <table className="data-table">
            <thead>
              <tr>
                <th>{dbReports ? '제목' : '유형'}</th>
                <th>{dbReports ? '상태' : '대상'}</th>
                <th>시간</th>
                {!dbReports ? <th>좋아요</th> : null}
                {!dbReports ? <th>상태</th> : null}
                <th />
              </tr>
            </thead>
            <tbody>
              {pagedDbReports
                ? pagedDbReports.map((row, idx) => {
                    const absoluteIdx = (safeListPage - 1) * REPORTS_PAGE_SIZE + idx
                    return (
                      <tr key={row.id} style={absoluteIdx === selected ? { background: '#f5f8ff' } : undefined}>
                        <td style={{ fontWeight: 700 }}>{row.title}</td>
                        <td>
                          <StatusBadge
                            tone={
                              row.status === 'PENDING' ? 'orange' : row.status === 'PROCESSING' ? 'blue' : 'green'
                            }
                          >
                            {reportStatusKo[row.status] ?? row.status}
                          </StatusBadge>
                        </td>
                        <td>{row.created_at ? new Date(row.created_at).toLocaleString('ko-KR') : '-'}</td>
                        <td>
                          <button
                            className="btn btn-outline"
                            type="button"
                            style={{ height: 28 }}
                            onClick={() => setSelected(absoluteIdx)}
                          >
                            상세
                          </button>
                        </td>
                      </tr>
                    )
                  })
                : (pagedMockReports ?? []).map((row, idx) => {
                    const absoluteIdx = (safeListPage - 1) * REPORTS_PAGE_SIZE + idx
                    return (
                      <tr
                        key={row.type + row.time}
                        style={absoluteIdx === selected ? { background: '#f5f8ff' } : undefined}
                      >
                        <td>{row.type}</td>
                        <td>{row.target}</td>
                        <td>{row.time}</td>
                        <td>{row.likes}</td>
                        <td>
                          <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                        </td>
                        <td>
                          <button
                            className="btn btn-outline"
                            type="button"
                            style={{ height: 28 }}
                            onClick={() => setSelected(absoluteIdx)}
                          >
                            상세
                          </button>
                        </td>
                      </tr>
                    )
                  })}
            </tbody>
          </table>
          <ListPagination
            total={reportTotal}
            page={safeListPage}
            pageSize={REPORTS_PAGE_SIZE}
            onPageChange={setListPage}
            ariaLabel="제보 목록 페이지"
          />
        </section>

        <section className="card card-pad">
          <div className="card-head">
            <h3>제보 상세</h3>
            {item ? (
              <StatusBadge tone={item.status === 'PENDING' ? 'orange' : item.status === 'PROCESSING' ? 'blue' : 'green'}>
                {reportStatusKo[item.status] ?? item.status}
              </StatusBadge>
            ) : (
              <StatusBadge tone={mockItem.tone}>{mockItem.status}</StatusBadge>
            )}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 13 }}>
            <div>
              <strong>{item?.title ?? mockItem.type}</strong>
              <div className="muted">
                {item
                  ? `${item.created_at ? new Date(item.created_at).toLocaleString('ko-KR') : '-'} · ${item.user_id.slice(0, 8)}`
                  : `${mockItem.time} · student_1024`}
              </div>
            </div>
            <div className="card card-pad" style={{ boxShadow: 'none', background: '#fafbff', whiteSpace: 'pre-wrap' }}>
              {item?.content ??
                '버스가 학생회관 앞 정류장을 정차하지 않고 통과했습니다. 대기 학생이 다수 있었습니다.'}
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}

/** ADM-06 공지·긴급 알림 관리 — Figma 430:19126 */
export function NoticesPage() {
  const { user } = useAuth()
  const [dbNotices, setDbNotices] = useState<NoticeRow[] | null>(null)
  const [noticeType, setNoticeType] = useState('긴급')
  const [push, setPush] = useState(true)
  const [title, setTitle] = useState('폭설로 인한 운행 지연 안내')
  const [bodyHtml, setBodyHtml] = useState(() => plainTextToEditorHtml(DEFAULT_NOTICE_BODY))
  const [bodyLen, setBodyLen] = useState(() => DEFAULT_NOTICE_BODY.length)
  const [targetStudent, setTargetStudent] = useState(true)
  const [targetDriver, setTargetDriver] = useState(false)
  const [permanent, setPermanent] = useState(false)
  const [startDate, setStartDate] = useState(() => todayDateInputValue())
  const [startHour, setStartHour] = useState('00')
  const [startMinute, setStartMinute] = useState('00')
  const [endDate, setEndDate] = useState(() => todayDateInputValue())
  const [endHour, setEndHour] = useState('23')
  const [endMinute, setEndMinute] = useState('59')
  const [saving, setSaving] = useState(false)
  const [flash, setFlash] = useState('')
  const [showPreview, setShowPreview] = useState(false)
  const [selectedNoticeId, setSelectedNoticeId] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const [listPage, setListPage] = useState(1)
  const [listQuery, setListQuery] = useState('')
  const [listTypeFilter, setListTypeFilter] = useState('전체 유형')
  const [editorCmds, setEditorCmds] = useState<EditorCmdState>(emptyEditorCmds)
  const editorRef = useRef<HTMLDivElement | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const hourOptions = useMemo(() => Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0')), [])
  const minuteOptions = useMemo(() => Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0')), [])

  const refreshEditorCmds = () => {
    try {
      setEditorCmds({
        bold: document.queryCommandState('bold'),
        italic: document.queryCommandState('italic'),
        underline: document.queryCommandState('underline'),
        ul: document.queryCommandState('insertUnorderedList'),
        ol: document.queryCommandState('insertOrderedList'),
        left: document.queryCommandState('justifyLeft'),
        center: document.queryCommandState('justifyCenter'),
        right: document.queryCommandState('justifyRight'),
        full: document.queryCommandState('justifyFull'),
      })
    } catch {
      setEditorCmds(emptyEditorCmds)
    }
  }

  const syncEditorState = () => {
    const el = editorRef.current
    if (!el) return
    const plain = htmlToPlainText(el.innerHTML)
    if (plain.length > NOTICE_BODY_MAX) {
      document.execCommand('undo')
      return
    }
    setBodyHtml(el.innerHTML)
    setBodyLen(plain.replace(/\n$/g, '').length)
    refreshEditorCmds()
  }

  const setEditorContent = (htmlOrPlain: string, asHtml = false) => {
    const html = asHtml ? htmlOrPlain : plainTextToEditorHtml(htmlOrPlain)
    if (editorRef.current) editorRef.current.innerHTML = html || ''
    setBodyHtml(html)
    setBodyLen(htmlToPlainText(html).replace(/\n$/g, '').length)
  }

  const runEditorCommand = (command: string, value?: string) => {
    const el = editorRef.current
    if (!el) return
    el.focus()
    document.execCommand(command, false, value)
    syncEditorState()
    refreshEditorCmds()
  }

  const onAttachFile = () => {
    fileInputRef.current?.click()
  }

  const onFileAttachChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    if (file.type.startsWith('image/')) {
      const reader = new FileReader()
      reader.onload = () => {
        const src = typeof reader.result === 'string' ? reader.result : ''
        if (!src) return
        runEditorCommand('insertImage', src)
      }
      reader.readAsDataURL(file)
      return
    }
    const el = editorRef.current
    if (!el) return
    el.focus()
    document.execCommand('insertText', false, `[첨부: ${file.name}]`)
    syncEditorState()
  }

  const formLocked = Boolean(selectedNoticeId) && !editing

  const buildNoticePayload = () => {
    const contentPlain = htmlToPlainText(editorRef.current?.innerHTML ?? bodyHtml).trim()
    const audience: NoticeAudience[] = []
    if (targetStudent) audience.push('STUDENT')
    if (targetDriver) audience.push('DRIVER')
    const type = NOTICE_TYPE_FROM_KO[noticeType] ?? 'GENERAL'
    const toIso = (date: string, hour: string, minute: string) =>
      new Date(`${date}T${hour}:${minute}:00`).toISOString()
    const startsAt = permanent ? null : toIso(startDate, startHour, startMinute)
    const endsAt = permanent ? null : toIso(endDate, endHour, endMinute)
    return {
      title: title.trim(),
      content: contentPlain,
      type,
      audience,
      is_push: push,
      starts_at: startsAt,
      ends_at: endsAt,
      status: resolveNoticeWriteStatus({ permanent, startsAt, endsAt }),
    }
  }

  const loadNoticeIntoForm = (row: NoticeRow) => {
    setSelectedNoticeId(row.id)
    setEditing(false)
    setShowPreview(false)
    const typeKo = noticeTypeLabel(row)
    setNoticeType(typeKo)
    setTitle(noticeTitlePlain(row))
    setEditorContent(row.content)
    const audience = normalizeNoticeAudience(row.audience)
    setTargetStudent(audience.includes('STUDENT'))
    setTargetDriver(audience.includes('DRIVER'))
    const hasPeriod = Boolean(row.starts_at || row.ends_at)
    setPermanent(!hasPeriod)
    if (row.starts_at) {
      const d = new Date(row.starts_at)
      setStartDate(
        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
      )
      setStartHour(String(d.getHours()).padStart(2, '0'))
      setStartMinute(String(d.getMinutes()).padStart(2, '0'))
    }
    if (row.ends_at) {
      const d = new Date(row.ends_at)
      setEndDate(
        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
      )
      setEndHour(String(d.getHours()).padStart(2, '0'))
      setEndMinute(String(d.getMinutes()).padStart(2, '0'))
    }
    setPush(Boolean(row.is_push))
  }

  const resetFormForCreate = () => {
    setSelectedNoticeId(null)
    setEditing(false)
    setShowPreview(false)
    setNoticeType('긴급')
    setTitle('')
    setEditorContent(DEFAULT_NOTICE_BODY)
    setTargetStudent(true)
    setTargetDriver(false)
    setPermanent(false)
    const today = todayDateInputValue()
    setStartDate(today)
    setStartHour('00')
    setStartMinute('00')
    setEndDate(today)
    setEndHour('23')
    setEndMinute('59')
    setPush(true)
  }

  useEffect(() => {
    if (editorRef.current) {
      editorRef.current.innerHTML = plainTextToEditorHtml(DEFAULT_NOTICE_BODY)
    }
  }, [])

  useEffect(() => {
    void fetchNotices().then((data) => {
      if (data) setDbNotices(data)
    })
  }, [])

  const onCreate = async () => {
    if (selectedNoticeId) {
      setFlash('목록 공지를 수정 중이면 「저장」을 눌러 주세요. 새 공지는 선택을 해제한 뒤 등록하세요.')
      return
    }
    if (!targetStudent && !targetDriver) {
      setFlash('대상을 학생 또는 기사 중 하나 이상 선택하세요.')
      return
    }
    setSaving(true)
    setFlash('')
    const payload = buildNoticePayload()
    const res = await createNotice({
      ...payload,
      author_id: user?.id ?? null,
    })
    setSaving(false)
    if (!res.ok) {
      setFlash(res.message ?? '등록 실패')
      return
    }
    setFlash('공지 등록 완료')
    setListPage(1)
    const data = await fetchNotices()
    if (data) setDbNotices(data)
  }

  const onStartEdit = () => {
    if (!selectedNoticeId) {
      setFlash('목록에서 수정할 공지를 먼저 선택하세요.')
      return
    }
    const selected = dbNotices?.find((n) => n.id === selectedNoticeId)
    if (selected && noticeStatusLabel(selected) === '종료') {
      setFlash('종료된 공지는 수정할 수 없습니다.')
      return
    }
    setEditing(true)
    setFlash('수정 모드입니다. 내용을 바꾼 뒤 「저장」을 눌러 주세요.')
  }

  const onSave = async () => {
    if (!selectedNoticeId) {
      setFlash('저장할 공지가 없습니다.')
      return
    }
    if (!targetStudent && !targetDriver) {
      setFlash('대상을 학생 또는 기사 중 하나 이상 선택하세요.')
      return
    }
    setSaving(true)
    setFlash('')
    const payload = buildNoticePayload()
    const res = await updateNotice(selectedNoticeId, payload)
    setSaving(false)
    if (!res.ok) {
      setFlash(res.message ?? '저장 실패')
      return
    }
    setEditing(false)
    setFlash('공지 저장 완료')
    const data = await fetchNotices()
    if (data) setDbNotices(data)
  }

  const onDelete = async () => {
    if (!selectedNoticeId) {
      setFlash('목록에서 종료할 공지를 먼저 선택하세요.')
      return
    }
    const selected = dbNotices?.find((n) => n.id === selectedNoticeId)
    if (selected && noticeStatusLabel(selected) === '종료') {
      setFlash('이미 종료된 공지입니다.')
      return
    }
    const ok = window.confirm(
      '이 공지를 종료할까요?\n학생·기사 앱에서는 더 이상 보이지 않고, 관리자 목록에는 「종료」로 남습니다.',
    )
    if (!ok) return
    setSaving(true)
    setFlash('')
    const res = await endNotice(selectedNoticeId)
    setSaving(false)
    if (!res.ok) {
      setFlash(res.message ?? '종료 실패')
      return
    }
    setEditing(false)
    setFlash('공지를 종료했습니다. 앱에서는 더 이상 표시되지 않습니다.')
    const data = await fetchNotices()
    if (data) {
      setDbNotices(data)
      const updated = data.find((n) => n.id === selectedNoticeId)
      if (updated) loadNoticeIntoForm(updated)
    }
  }

  const noticeKpis = useMemo(() => {
    const rows = dbNotices ?? []
    const now = Date.now()
    const day30 = 30 * 24 * 60 * 60 * 1000
    const recent = rows.filter((n) => {
      if (!n.created_at) return true
      const t = Date.parse(n.created_at)
      return Number.isFinite(t) && now - t <= day30
    })
    const urgent = recent.filter((n) => noticeTypeLabel(n) === '긴급').length
    const scheduled = recent.filter((n) => noticeStatusLabel(n) === '예약').length
    const views = recent.reduce((sum, n) => sum + noticeViewCount(n), 0)
    return {
      total: recent.length,
      urgent,
      scheduled,
      views,
    }
  }, [dbNotices])

  const noticeRows = dbNotices
  const filteredDbNotices = useMemo(() => {
    if (!noticeRows) return null
    const q = listQuery.trim().toLowerCase()
    return noticeRows.filter((row) => {
      if (listTypeFilter !== '전체 유형' && noticeTypeLabel(row) !== listTypeFilter) return false
      if (q) {
        const hay = `${noticeTitlePlain(row)} ${row.content ?? ''}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      return true
    })
  }, [noticeRows, listQuery, listTypeFilter])
  const filteredMockNotices = useMemo(() => {
    if (noticeRows) return null
    const q = listQuery.trim().toLowerCase()
    return notices.filter((row) => {
      if (listTypeFilter !== '전체 유형' && row.type !== listTypeFilter) return false
      if (q && !`${row.title}`.toLowerCase().includes(q)) return false
      return true
    })
  }, [noticeRows, listQuery, listTypeFilter])
  const noticeTotal = filteredDbNotices?.length ?? filteredMockNotices?.length ?? 0
  const noticePageCount = Math.max(1, Math.ceil(noticeTotal / NOTICE_PAGE_SIZE))
  const safeListPage = Math.min(listPage, noticePageCount)
  const pagedDbNotices = useMemo(() => {
    if (!filteredDbNotices) return null
    const start = (safeListPage - 1) * NOTICE_PAGE_SIZE
    return filteredDbNotices.slice(start, start + NOTICE_PAGE_SIZE)
  }, [filteredDbNotices, safeListPage])
  const pagedMockNotices = useMemo(() => {
    if (filteredDbNotices) return null
    const start = (safeListPage - 1) * NOTICE_PAGE_SIZE
    return (filteredMockNotices ?? []).slice(start, start + NOTICE_PAGE_SIZE)
  }, [filteredDbNotices, filteredMockNotices, safeListPage])
  useEffect(() => {
    if (listPage > noticePageCount) setListPage(noticePageCount)
  }, [listPage, noticePageCount])
  useEffect(() => {
    setListPage(1)
  }, [listQuery, listTypeFilter])

  const kpis = [
    {
      label: '전체 공지',
      value: `${noticeKpis.total.toLocaleString('ko-KR')}건`,
      hint: '최근 30일 기준',
      tone: 'blue',
      icon: <Megaphone size={40} strokeWidth={2.8} />,
    },
    {
      label: '긴급 공지',
      value: `${noticeKpis.urgent.toLocaleString('ko-KR')}건`,
      hint: '최근 30일 기준',
      tone: 'red',
      icon: <AlertTriangle size={40} strokeWidth={2.8} />,
    },
    {
      label: '예약 공지',
      value: `${noticeKpis.scheduled.toLocaleString('ko-KR')}건`,
      hint: '게시 예정',
      tone: 'orange',
      icon: <CalendarDays size={40} strokeWidth={2.8} />,
    },
    {
      label: '최근 조회수',
      value: `${noticeKpis.views.toLocaleString('ko-KR')}회`,
      hint: '최근 30일 기준',
      tone: 'purple',
      icon: <Eye size={40} strokeWidth={2.8} />,
    },
  ] as const

  const previewTypeLabel = noticeType === '긴급' || noticeType === '일반' ? `${noticeType} 공지` : noticeType
  const driverAlarmTitle =
    noticeType === '긴급'
      ? '긴급 공지'
      : noticeType === '중요'
        ? '중요 공지'
        : noticeType === '운행 변경'
          ? '운행 변경'
          : '일반 공지'
  const previewPlainBody = htmlToPlainText(bodyHtml).trim()
  const driverAlarmBody = previewPlainBody
    ? `${title.trim() || '제목'} — ${previewPlainBody}`
    : title.trim() || '제목'
  const previewAsDriver = targetDriver

  return (
    <div className="page">
      {flash ? <div className="alert alert-info">{flash}</div> : null}
      <div className="figma-kpis notice-kpis">
        {kpis.map((k) => (
          <div key={k.label} className="figma-kpi">
            <div className={`figma-kpi-icon ${k.tone}`}>{k.icon}</div>
            <div className="figma-kpi-text">
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
              공지 목록{' '}
              <span className="muted">
                (전체 {(dbNotices?.length ?? notices.length).toLocaleString('ko-KR')}건
                {noticeTotal !== (dbNotices?.length ?? notices.length)
                  ? ` · 검색 ${noticeTotal.toLocaleString('ko-KR')}건`
                  : ''}
                )
              </span>
            </h3>
          </div>
          <div className="toolbar" style={{ marginBottom: 8 }}>
            <select
              className="select"
              style={{ width: 110, height: 32 }}
              value={listTypeFilter}
              onChange={(e) => setListTypeFilter(e.target.value)}
            >
              <option>전체 유형</option>
              <option>긴급</option>
              <option>중요</option>
              <option>운행 변경</option>
              <option>일반</option>
            </select>
            <input
              className="input"
              style={{ flex: 1, height: 32 }}
              placeholder="제목 또는 내용을 검색하세요."
              value={listQuery}
              onChange={(e) => setListQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') setListPage(1)
              }}
            />
            <button className="btn btn-primary btn-xs" type="button" onClick={() => setListPage(1)}>
              검색
            </button>
          </div>
          <table className="data-table dense">
            <thead>
              <tr>
                <th className="notice-col-center">번호</th>
                <th className="notice-col-center">유형</th>
                <th>제목</th>
                <th className="notice-col-center">대상</th>
                <th className="notice-col-center">게시 기간</th>
                <th className="notice-col-center">조회수</th>
                <th className="notice-col-center">상태</th>
              </tr>
            </thead>
            <tbody>
              {pagedDbNotices
                ? pagedDbNotices.map((row, idx) => {
                    const typeKo = noticeTypeLabel(row)
                    const statusKo = noticeStatusLabel(row)
                    const period = noticePeriodLabel(row)
                    const no = (safeListPage - 1) * NOTICE_PAGE_SIZE + idx + 1
                    return (
                      <tr
                        key={row.id}
                        className={selectedNoticeId === row.id ? 'is-selected' : undefined}
                        style={{ cursor: 'pointer' }}
                        onClick={() => loadNoticeIntoForm(row)}
                      >
                        <td className="notice-col-center">{no}</td>
                        <td className="notice-col-center">
                          <StatusBadge tone={noticeTypeTone(typeKo)}>{typeKo}</StatusBadge>
                        </td>
                        <td title={noticeTitlePlain(row)}>{truncateChars(noticeTitlePlain(row), 8)}</td>
                        <td className="notice-col-center">{noticeAudienceLabel(row.audience)}</td>
                        <td className="notice-period-cell notice-col-center" title={period === '-' ? undefined : period}>
                          {period}
                        </td>
                        <td className="notice-col-center">{noticeViewCount(row).toLocaleString('ko-KR')}</td>
                        <td className="notice-col-center">
                          <StatusBadge tone={noticeStatusTone(statusKo)}>{statusKo}</StatusBadge>
                        </td>
                      </tr>
                    )
                  })
                : (pagedMockNotices ?? []).map((row, idx) => (
                    <tr key={row.no}>
                      <td className="notice-col-center">
                        {(safeListPage - 1) * NOTICE_PAGE_SIZE + idx + 1}
                      </td>
                      <td className="notice-col-center">
                        <StatusBadge tone={row.tone}>{row.type}</StatusBadge>
                      </td>
                      <td title={row.title}>{truncateChars(row.title, 8)}</td>
                      <td className="notice-col-center">{row.target === '전체' ? '학생/기사' : row.target}</td>
                      <td className="notice-period-cell notice-col-center">{row.period.replace(' - ', ' ~ ')}</td>
                      <td className="notice-col-center">{row.views.toLocaleString()}</td>
                      <td className="notice-col-center">
                        <StatusBadge tone={row.status === '게시중' || row.status === '게시 중' ? 'blue' : 'gray'}>
                          {row.status === '게시중' ? '게시 중' : row.status}
                        </StatusBadge>
                      </td>
                    </tr>
                  ))}
            </tbody>
          </table>
          <ListPagination
            total={noticeTotal}
            page={safeListPage}
            pageSize={NOTICE_PAGE_SIZE}
            onPageChange={setListPage}
            ariaLabel="공지 목록 페이지"
          />
          <p className="muted" style={{ fontSize: 11, marginTop: 8 }}>
            학생 대상은 학생 앱 공지·푸시로, 기사 대상은 기사 앱 [운행 알림](종) 목록으로 전달됩니다.
          </p>
        </section>

        <section className="figma-panel notice-edit-panel">
          <div className="figma-panel-head">
            <h3>공지 등록/수정</h3>
          </div>

          <div className={`notice-form-grid${showPreview ? '' : ' preview-off'}`}>
            <div className={`notice-form-main${formLocked ? ' is-readonly' : ''}`}>
              <div className="notice-form-actions">
                <button
                  className={`btn btn-outline btn-xs notice-preview-btn${showPreview ? ' is-active' : ''}`}
                  type="button"
                  onClick={() => setShowPreview((v) => !v)}
                >
                  <Eye size={12} /> 미리보기
                </button>
                <button
                  className="btn btn-outline btn-xs"
                  type="button"
                  disabled={saving || !selectedNoticeId}
                  onClick={() => void onDelete()}
                  title={selectedNoticeId ? '앱에서 숨기고 목록에는 종료로 남깁니다' : '목록에서 공지를 선택하세요'}
                >
                  삭제
                </button>
                <button
                  className={`btn btn-outline btn-xs${editing ? ' is-active' : ''}`}
                  type="button"
                  disabled={saving || !selectedNoticeId || editing}
                  onClick={onStartEdit}
                  title={!selectedNoticeId ? '목록에서 공지를 선택하세요' : '수정 모드로 전환'}
                >
                  수정
                </button>
                {selectedNoticeId && editing ? (
                  <button
                    className="btn btn-primary btn-xs"
                    type="button"
                    disabled={saving}
                    onClick={() => void onSave()}
                  >
                    {saving ? '저장 중...' : '저장'}
                  </button>
                ) : null}
                {!selectedNoticeId ? (
                  <button
                    className="btn btn-primary btn-xs"
                    type="button"
                    disabled={saving}
                    onClick={() => void onCreate()}
                  >
                    {saving ? '등록 중...' : '공지 등록'}
                  </button>
                ) : null}
                {selectedNoticeId ? (
                  <button
                    className="btn btn-ghost btn-xs"
                    type="button"
                    disabled={saving}
                    onClick={resetFormForCreate}
                  >
                    새 공지
                  </button>
                ) : null}
              </div>

              {formLocked ? (
                <p className="muted" style={{ fontSize: 11, marginBottom: 8 }}>
                  목록에서 선택한 공지입니다. 내용을 바꾸려면 「수정」을 눌러 주세요.
                </p>
              ) : null}

              <div className="field">
                <label>공지 유형</label>
                <div className="type-pills">
                  {(
                    [
                      ['긴급', 'danger'],
                      ['중요', 'danger'],
                      ['운행 변경', 'info'],
                      ['일반', 'info'],
                    ] as const
                  ).map(([t, tone]) => (
                    <button
                      key={t}
                      type="button"
                      className={`type-pill${noticeType === t ? ` active ${tone}` : ''}`}
                      onClick={() => setNoticeType(t)}
                      disabled={formLocked}
                    >
                      {t === '긴급' || t === '일반' ? `${t} 공지` : t}
                    </button>
                  ))}
                </div>
              </div>

              <div className="field">
                <div className="field-label-row">
                  <label>제목</label>
                  <span className="field-hint">{title.length}/100</span>
                </div>
                <input
                  className="input"
                  style={{ height: 36 }}
                  maxLength={100}
                  placeholder="제목을 입력하세요."
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  disabled={formLocked}
                  readOnly={formLocked}
                />
              </div>

              <div className="field">
                <label>내용</label>
                <div className="notice-editor">
                  <div className="notice-editor-toolbar" role="toolbar" aria-label="본문 서식">
                    <div className="notice-editor-group">
                      <button type="button" className={`notice-editor-btn${editorCmds.bold ? ' is-active' : ''}`} title="굵게" aria-label="굵게" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('bold')}>
                        <Bold size={14} strokeWidth={2.5} />
                      </button>
                      <button type="button" className={`notice-editor-btn${editorCmds.italic ? ' is-active' : ''}`} title="기울임" aria-label="기울임" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('italic')}>
                        <Italic size={14} strokeWidth={2.5} />
                      </button>
                      <button type="button" className={`notice-editor-btn${editorCmds.underline ? ' is-active' : ''}`} title="밑줄" aria-label="밑줄" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('underline')}>
                        <Underline size={14} strokeWidth={2.5} />
                      </button>
                    </div>
                    <span className="notice-editor-sep" />
                    <div className="notice-editor-group">
                      <button type="button" className={`notice-editor-btn${editorCmds.ul ? ' is-active' : ''}`} title="글머리 기호" aria-label="글머리 기호" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('insertUnorderedList')}>
                        <List size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className={`notice-editor-btn${editorCmds.ol ? ' is-active' : ''}`} title="번호 목록" aria-label="번호 목록" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('insertOrderedList')}>
                        <ListOrdered size={14} strokeWidth={2.2} />
                      </button>
                    </div>
                    <span className="notice-editor-sep" />
                    <div className="notice-editor-group">
                      <button type="button" className={`notice-editor-btn${editorCmds.left ? ' is-active' : ''}`} title="왼쪽 정렬" aria-label="왼쪽 정렬" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('justifyLeft')}>
                        <AlignLeft size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className={`notice-editor-btn${editorCmds.center ? ' is-active' : ''}`} title="가운데 정렬" aria-label="가운데 정렬" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('justifyCenter')}>
                        <AlignCenter size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className={`notice-editor-btn${editorCmds.right ? ' is-active' : ''}`} title="오른쪽 정렬" aria-label="오른쪽 정렬" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('justifyRight')}>
                        <AlignRight size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className={`notice-editor-btn${editorCmds.full ? ' is-active' : ''}`} title="양쪽 정렬" aria-label="양쪽 정렬" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('justifyFull')}>
                        <AlignJustify size={14} strokeWidth={2.2} />
                      </button>
                    </div>
                    <span className="notice-editor-sep" />
                    <div className="notice-editor-group">
                      <button
                        type="button"
                        className="notice-editor-btn is-chip"
                        title="파일 첨부"
                        aria-label="파일 첨부"
                        onMouseDown={(e) => e.preventDefault()}
                        onClick={onAttachFile}
                      >
                        <Paperclip size={14} strokeWidth={2.2} />
                      </button>
                    </div>
                  </div>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.hwp,.hwpx,.txt,.zip"
                    hidden
                    onChange={onFileAttachChange}
                  />
                  <div
                    ref={editorRef}
                    className={`notice-editor-body${bodyLen === 0 ? ' is-empty' : ''}`}
                    contentEditable={!formLocked}
                    role="textbox"
                    aria-multiline="true"
                    aria-label="공지 내용"
                    aria-readonly={formLocked}
                    data-placeholder="공지 내용을 입력하세요."
                    suppressContentEditableWarning
                    onInput={syncEditorState}
                    onKeyUp={refreshEditorCmds}
                    onMouseUp={refreshEditorCmds}
                  />
                  <div className="notice-editor-foot">
                    <span>
                      {bodyLen}/{NOTICE_BODY_MAX}
                    </span>
                  </div>
                </div>
              </div>

              <div className="field">
                <label>대상</label>
                <div className="notice-target-line">
                  <button
                    type="button"
                    className={`type-pill${targetStudent ? ' active' : ''}`}
                    onClick={() => setTargetStudent((v) => !v)}
                    disabled={formLocked}
                  >
                    학생
                  </button>
                  <button
                    type="button"
                    className={`type-pill${targetDriver ? ' active' : ''}`}
                    onClick={() => setTargetDriver((v) => !v)}
                    disabled={formLocked}
                  >
                    기사
                  </button>
                  <label className="check-row notice-inline-check">
                    <input
                      type="checkbox"
                      checked={permanent}
                      disabled={formLocked}
                      onChange={(e) => setPermanent(e.target.checked)}
                    />
                    게시 기간 없음 (상시 게시)
                  </label>
                  <label className="check-row notice-inline-check">
                    <input
                      type="checkbox"
                      checked={push}
                      disabled={formLocked}
                      onChange={(e) => setPush(e.target.checked)}
                    />
                    푸시 알림 동시 발송
                  </label>
                </div>
                {push ? (
                  <div className="muted" style={{ fontSize: 11, marginTop: 4 }}>
                    ONDA 셔틀 앱 푸시 알림으로 즉시 발송됩니다.
                  </div>
                ) : null}
              </div>

              <div className={`notice-datetime-stack${permanent || formLocked ? ' is-disabled' : ''}`}>
                <div className="field">
                  <label>시작일</label>
                  <div className="notice-datetime">
                    <input
                      className="input notice-date-input"
                      type="date"
                      value={startDate}
                      disabled={permanent}
                      onChange={(e) => {
                        const next = e.target.value
                        setStartDate(next)
                        if (next) setEndDate(next)
                      }}
                    />
                    <select
                      className="select notice-time-select"
                      value={startHour}
                      disabled={permanent}
                      onChange={(e) => setStartHour(e.target.value)}
                      aria-label="시작 시"
                    >
                      {hourOptions.map((h) => (
                        <option key={h} value={h}>
                          {h}시
                        </option>
                      ))}
                    </select>
                    <select
                      className="select notice-time-select"
                      value={startMinute}
                      disabled={permanent}
                      onChange={(e) => setStartMinute(e.target.value)}
                      aria-label="시작 분"
                    >
                      {minuteOptions.map((m) => (
                        <option key={m} value={m}>
                          {m}분
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="field">
                  <label>종료일</label>
                  <div className="notice-datetime">
                    <input
                      className="input notice-date-input"
                      type="date"
                      value={endDate}
                      disabled={permanent}
                      onChange={(e) => setEndDate(e.target.value)}
                    />
                    <select
                      className="select notice-time-select"
                      value={endHour}
                      disabled={permanent}
                      onChange={(e) => setEndHour(e.target.value)}
                      aria-label="종료 시"
                    >
                      {hourOptions.map((h) => (
                        <option key={h} value={h}>
                          {h}시
                        </option>
                      ))}
                    </select>
                    <select
                      className="select notice-time-select"
                      value={endMinute}
                      disabled={permanent}
                      onChange={(e) => setEndMinute(e.target.value)}
                      aria-label="종료 분"
                    >
                      {minuteOptions.map((m) => (
                        <option key={m} value={m}>
                          {m}분
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            </div>

            {showPreview ? (
              <aside className="phone-preview">
                {previewAsDriver ? (
                  <>
                    <div className="cap">기사 앱 [운행 알림] 목록 · 상세 미리보기입니다.</div>
                    <div className="screen driver-alarm-preview">
                      <div className="driver-alarm-preview-head">
                        <span>운행 알림</span>
                        <Bell size={14} strokeWidth={2.4} />
                      </div>
                      <div className="driver-alarm-chip-row">
                        <span className="driver-alarm-chip is-active">전체</span>
                        <span className="driver-alarm-chip">미확인</span>
                        <span className="driver-alarm-chip">공지</span>
                      </div>
                      <div className="driver-alarm-card">
                        <div className="driver-alarm-icon" aria-hidden>
                          <Bell size={16} strokeWidth={2.4} />
                        </div>
                        <div className="driver-alarm-text">
                          <strong>{driverAlarmTitle}</strong>
                          <p>{driverAlarmBody}</p>
                        </div>
                        <ChevronRight size={16} className="driver-alarm-chevron" aria-hidden />
                      </div>
                    </div>
                    <div className="screen" style={{ marginTop: 10 }}>
                      <div className="muted" style={{ fontSize: 10, marginBottom: 4 }}>
                        공지사항
                      </div>
                      <span className={`tag tone-${noticeTypeTone(noticeType)}`}>{driverAlarmTitle}</span>
                      <strong>{title || '제목'}</strong>
                      <div className="time">
                        {permanent
                          ? '상시 게시'
                          : `${startDate.replaceAll('-', '.')} ${startHour}:${startMinute}`}
                      </div>
                      <div
                        className="notice-preview-body"
                        dangerouslySetInnerHTML={{
                          __html:
                            bodyLen > 0
                              ? bodyHtml
                              : '<span style="color:#9ca3af">공지 내용을 입력하세요.</span>',
                        }}
                      />
                      <div className="muted" style={{ fontSize: 10, marginTop: 12, textAlign: 'center' }}>
                        오늘 하루 보지 않기
                      </div>
                    </div>
                    {targetStudent ? (
                      <div className="muted" style={{ fontSize: 10, marginTop: 8 }}>
                        학생도 선택됨 — 학생 앱 공지에도 함께 노출됩니다.
                      </div>
                    ) : null}
                  </>
                ) : (
                  <>
                    <div className="cap">실제 학생 앱에 표시되는 화면입니다.</div>
                    <div className="screen">
                      <div className="muted" style={{ fontSize: 10, marginBottom: 4 }}>
                        공지사항
                      </div>
                      <span className={`tag tone-${noticeTypeTone(noticeType)}`}>{previewTypeLabel}</span>
                      <strong>{title || '제목'}</strong>
                      <div className="time">
                        {permanent
                          ? '상시 게시'
                          : `${startDate.replaceAll('-', '.')} ${startHour}:${startMinute}`}
                      </div>
                      <div
                        className="notice-preview-body"
                        dangerouslySetInnerHTML={{
                          __html:
                            bodyLen > 0
                              ? bodyHtml
                              : '<span style="color:#9ca3af">공지 내용을 입력하세요.</span>',
                        }}
                      />
                      <div className="muted" style={{ fontSize: 10, marginTop: 12, textAlign: 'center' }}>
                        오늘 하루 보지 않기
                      </div>
                    </div>
                    <div className="push">
                      <span className="app">ONDA 셔틀</span>
                      <span className="when">지금</span>
                      <div className="body">
                        {previewPlainBody.split('\n')[0] || '푸시 미리보기'}
                      </div>
                    </div>
                  </>
                )}
              </aside>
            ) : (
              <aside className="phone-preview is-placeholder">
                <div className="cap">미리보기</div>
                <div className="notice-preview-empty">
                  미리보기 버튼을 누르면
                  <br />
                  {previewAsDriver ? '기사 앱 알림 화면이' : '학생 앱 화면이'} 표시됩니다.
                </div>
              </aside>
            )}
          </div>
        </section>
      </div>
    </div>
  )
}

/** ADM-04 노선·운행 관리 — Figma 430:18166 */
export function RoutesPage() {
  const [selected, setSelected] = useState(0)
  const [listPage, setListPage] = useState(1)
  const [dbRoutes, setDbRoutes] = useState<RouteRow[] | null>(null)

  useEffect(() => {
    void fetchRoutes().then((data) => {
      if (data) setDbRoutes(data)
    })
  }, [])

  const list = dbRoutes?.length
    ? dbRoutes.map((r) => ({
        name: r.route_name,
        status: r.is_active ? '운행 중' : '중지',
        buses: '-',
        type: r.direction ?? '노선',
        days: '-',
        hours: '-',
        desc: r.description ?? `${r.start_location ?? ''} → ${r.end_location ?? ''}`,
      }))
    : routes

  const routePageCount = Math.max(1, Math.ceil(list.length / ROUTES_PAGE_SIZE))
  const safeListPage = Math.min(listPage, routePageCount)
  const pagedList = useMemo(() => {
    const start = (safeListPage - 1) * ROUTES_PAGE_SIZE
    return list.slice(start, start + ROUTES_PAGE_SIZE)
  }, [list, safeListPage])

  useEffect(() => {
    if (listPage > routePageCount) setListPage(routePageCount)
  }, [listPage, routePageCount])

  const detail = list[selected] ?? list[0]

  return (
    <div className="page">
      <p className="page-subtitle" style={{ marginTop: 0 }}>
        {dbRoutes ? `Supabase routes ${dbRoutes.length}건` : 'mock 노선'}
      </p>
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
              {pagedList.map((row, idx) => {
                const absoluteIdx = (safeListPage - 1) * ROUTES_PAGE_SIZE + idx
                return (
                  <tr
                    key={row.name + absoluteIdx}
                    style={absoluteIdx === selected ? { background: '#f5f8ff' } : undefined}
                    onClick={() => setSelected(absoluteIdx)}
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
                )
              })}
            </tbody>
          </table>
          <ListPagination
            total={list.length}
            page={safeListPage}
            pageSize={ROUTES_PAGE_SIZE}
            onPageChange={setListPage}
            ariaLabel="노선 목록 페이지"
          />
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
      <div className="split-14">
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
        <div className="stack" />
      </div>
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
  const [maintQuery, setMaintQuery] = useState('')
  const [maintStatus, setMaintStatus] = useState('전체 상태')

  const filteredMaintenances = useMemo(() => {
    const q = maintQuery.trim().toLowerCase()
    return maintenances.filter((row) => {
      if (maintStatus !== '전체 상태' && row.status !== maintStatus) return false
      if (q) {
        const hay = `${row.plate} ${row.item} ${row.type} ${row.mechanic}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      return true
    })
  }, [maintQuery, maintStatus])

  const kpis = [
    { label: '전체 차량', value: '28', unit: '대', meta: '정상 20 · 정비중 4 · 점검 필요 4', tone: 'blue' },
    { label: '예정 정비', value: '6', unit: '건', meta: '이번 주 2 · 이번 달 6', tone: 'orange' },
    { label: '정비 완료', value: '12', unit: '건', meta: '지난달 대비 20%', tone: 'green' },
    { label: '정비 비용', value: '4,850', unit: '만원', meta: '(이번달) · 지난달 대비 15%', tone: 'purple' },
    { label: '가동률', value: '92.6', unit: '%', meta: '목표 90%', tone: 'blue' },
  ] as const

  return (
    <div className="page">
      <div className="toolbar vehicle-filter-toolbar" style={{ justifyContent: 'flex-end' }}>
        <select className="select vehicle-filter-control" style={{ width: 120, height: 32 }}>
          <option>전체 차량</option>
        </select>
        <input
          className="input vehicle-filter-control"
          style={{ width: 220, height: 32 }}
          defaultValue="2026.07.01 ~ 2026.07.31"
        />
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
              <input
                className="input"
                style={{ width: 220, height: 32 }}
                placeholder="차량 번호 / 정비 항목 검색"
                value={maintQuery}
                onChange={(e) => setMaintQuery(e.target.value)}
              />
              <select
                className="select"
                style={{ width: 110, height: 32 }}
                value={maintStatus}
                onChange={(e) => setMaintStatus(e.target.value)}
              >
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
              {filteredMaintenances.length === 0 ? (
                <tr>
                  <td colSpan={7} className="muted" style={{ textAlign: 'center', padding: 24 }}>
                    검색 결과가 없습니다.
                  </td>
                </tr>
              ) : (
                filteredMaintenances.map((row) => (
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
                ))
              )}
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
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [listPage, setListPage] = useState(1)
  const [listQuery, setListQuery] = useState('')
  const [roleFilter, setRoleFilter] = useState('전체 역할')
  const [loginHistory, setLoginHistory] = useState<LoginHistoryEntry[]>([])
  const [dbUsers, setDbUsers] = useState<UserRow[] | null>(null)

  useEffect(() => {
    void fetchUsers().then((data) => {
      if (data) setDbUsers(data)
    })
  }, [])

  const list = dbUsers?.length
    ? dbUsers.map((u) => ({
        id: u.email ?? u.id.slice(0, 8),
        name: u.name,
        email: u.email ?? '-',
        role: u.role === 'ADMIN' ? '관리자' : u.role === 'DRIVER' ? '기사' : '일반 사용자',
        lastLogin: u.updated_at ? new Date(u.updated_at).toLocaleString('ko-KR') : '-',
        status: '활성',
      }))
    : users

  const filteredList = useMemo(() => {
    const q = listQuery.trim().toLowerCase()
    return list.filter((row) => {
      if (roleFilter !== '전체 역할') {
        if (roleFilter === '관리자' && !row.role.includes('관리')) return false
        if (roleFilter === '운영자' && !row.role.includes('운영') && !row.role.includes('기사')) return false
        if (roleFilter === '일반' && !row.role.includes('일반')) return false
      }
      if (q) {
        const hay = `${row.id} ${row.name} ${row.email}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      return true
    })
  }, [list, listQuery, roleFilter])

  const userPageCount = Math.max(1, Math.ceil(filteredList.length / USERS_PAGE_SIZE))
  const safeListPage = Math.min(listPage, userPageCount)
  const pagedList = useMemo(() => {
    const start = (safeListPage - 1) * USERS_PAGE_SIZE
    return filteredList.slice(start, start + USERS_PAGE_SIZE)
  }, [filteredList, safeListPage])

  useEffect(() => {
    if (listPage > userPageCount) setListPage(userPageCount)
  }, [listPage, userPageCount])

  useEffect(() => {
    setListPage(1)
  }, [listQuery, roleFilter])

  const user = filteredList.find((u) => u.id === selectedId) ?? filteredList[0] ?? list[0]

  useEffect(() => {
    if (selectedId && !filteredList.some((u) => u.id === selectedId)) {
      setSelectedId(filteredList[0]?.id ?? null)
    }
  }, [filteredList, selectedId])

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
          { label: '전체 사용자', value: `${list.length}명`, hint: dbUsers ? 'Supabase users' : 'mock', tone: 'blue' },
          { label: '관리자', value: `${list.filter((u) => u.role.includes('관리')).length}명`, hint: 'ADMIN', tone: 'purple' },
          { label: '기사', value: `${list.filter((u) => u.role.includes('기사')).length}명`, hint: 'DRIVER', tone: 'orange' },
          { label: '일반 사용자', value: `${list.filter((u) => u.role.includes('일반')).length}명`, hint: 'STUDENT', tone: 'gray' },
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
              사용자 목록{' '}
              <span className="muted">
                ({filteredList.length}명{dbUsers ? ' · DB' : ''}
                {filteredList.length !== list.length ? ` / 전체 ${list.length}명` : ''})
              </span>
            </h3>
          </div>
          <div className="toolbar" style={{ marginBottom: 8 }}>
            <select
              className="select"
              style={{ width: 120, height: 32 }}
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
            >
              <option>전체 역할</option>
              <option>관리자</option>
              <option>운영자</option>
              <option>일반</option>
            </select>
            <input
              className="input"
              style={{ flex: 1, height: 32 }}
              placeholder="이름, 아이디, 이메일 검색"
              value={listQuery}
              onChange={(e) => setListQuery(e.target.value)}
            />
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
              {pagedList.length === 0 ? (
                <tr>
                  <td colSpan={7} className="muted" style={{ textAlign: 'center', padding: 24 }}>
                    검색 결과가 없습니다.
                  </td>
                </tr>
              ) : (
                pagedList.map((row) => {
                  const isSelected = (selectedId ?? filteredList[0]?.id) === row.id
                  return (
                  <tr key={row.id} style={isSelected ? { background: '#f5f8ff' } : undefined}>
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
                      <button
                        className="btn btn-outline btn-xs"
                        type="button"
                        onClick={() => setSelectedId(row.id)}
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                  )
                })
              )}
            </tbody>
          </table>
          <ListPagination
            total={filteredList.length}
            page={safeListPage}
            pageSize={USERS_PAGE_SIZE}
            onPageChange={setListPage}
            ariaLabel="사용자 목록 페이지"
          />
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
function matchesSystemLogType(type: string, result: string, filter: string): boolean {
  if (filter === '전체') return true
  const t = type.toLowerCase()
  if (filter === '운영 기록') {
    return /운행|노선|차량|배차|정비|공지/.test(t)
  }
  if (filter === '사용자 활동') {
    return /로그인|사용자|권한/.test(t)
  }
  if (filter === '시스템 변경') {
    return /시스템|설정|내보내기/.test(t)
  }
  if (filter === '오류 / 경고') {
    return result === '실패' || result === '경고' || /실패|오류|경고/.test(t)
  }
  return true
}

function parseSystemLogPeriod(period: string): { start: Date | null; end: Date | null } {
  const m = period.trim().match(
    /^(\d{4})[./-](\d{1,2})[./-](\d{1,2})\s*[~～-]\s*(\d{4})[./-](\d{1,2})[./-](\d{1,2})$/,
  )
  if (!m) return { start: null, end: null }
  const start = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), 0, 0, 0)
  const end = new Date(Number(m[4]), Number(m[5]) - 1, Number(m[6]), 23, 59, 59)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return { start: null, end: null }
  return { start, end }
}

function systemLogTimeMs(time: string): number | null {
  const m = time.match(/^(\d{4})[./-](\d{1,2})[./-](\d{1,2})\s+(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?/)
  if (!m) return null
  const d = new Date(
    Number(m[1]),
    Number(m[2]) - 1,
    Number(m[3]),
    Number(m[4]),
    Number(m[5]),
    Number(m[6] ?? 0),
  )
  const t = d.getTime()
  return Number.isFinite(t) ? t : null
}

export function SystemPage() {
  const [gpsLogs, setGpsLogs] = useState<GpsReceiveLog[]>([])
  const [gpsStatus, setGpsStatus] = useState<'idle' | 'loading' | 'ok' | 'error'>('idle')
  const [gpsUpdatedAt, setGpsUpdatedAt] = useState<string | null>(null)
  const [gpsPage, setGpsPage] = useState(1)

  const [dbSystemLogs, setDbSystemLogs] = useState<SystemLogRow[]>([])
  const [systemLogsStatus, setSystemLogsStatus] = useState<'idle' | 'loading' | 'ok' | 'error'>('idle')
  const [systemLogsPage, setSystemLogsPage] = useState(1)
  const allSystemLogs: SystemLogRow[] = isSupabaseConfigured
    ? dbSystemLogs
    : (mockSystemLogs as SystemLogRow[])

  const [draftTypeFilter, setDraftTypeFilter] = useState('전체')
  const [draftActorFilter, setDraftActorFilter] = useState('전체')
  const [draftPeriod, setDraftPeriod] = useState('')
  const [draftKeyword, setDraftKeyword] = useState('')
  const [appliedTypeFilter, setAppliedTypeFilter] = useState('전체')
  const [appliedActorFilter, setAppliedActorFilter] = useState('전체')
  const [appliedPeriod, setAppliedPeriod] = useState('')
  const [appliedKeyword, setAppliedKeyword] = useState('')

  const [selectedSystemLog, setSelectedSystemLog] = useState<SystemLogRow | null>(null)
  const [selectedGpsLog, setSelectedGpsLog] = useState<GpsReceiveLog | null>(null)

  const actorOptions = useMemo(() => {
    const set = new Set<string>()
    for (const row of allSystemLogs) {
      const actor = (row.actor ?? '').trim()
      if (actor) set.add(actor)
    }
    return Array.from(set).sort((a, b) => a.localeCompare(b, 'ko'))
  }, [allSystemLogs])

  const systemLogsToShow = useMemo(() => {
    const q = appliedKeyword.trim().toLowerCase()
    const { start, end } = parseSystemLogPeriod(appliedPeriod)
    return allSystemLogs.filter((row) => {
      if (!matchesSystemLogType(row.type, row.result, appliedTypeFilter)) return false
      if (appliedActorFilter !== '전체' && (row.actor ?? '') !== appliedActorFilter) return false
      if (start && end) {
        const t = systemLogTimeMs(row.time)
        if (t != null && (t < start.getTime() || t > end.getTime())) return false
      }
      if (q) {
        const hay = `${row.type} ${row.action} ${row.actor ?? ''} ${row.target ?? ''} ${row.result} ${row.ip ?? ''}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      return true
    })
  }, [allSystemLogs, appliedTypeFilter, appliedActorFilter, appliedPeriod, appliedKeyword])

  const applySystemLogFilters = () => {
    setAppliedTypeFilter(draftTypeFilter)
    setAppliedActorFilter(draftActorFilter)
    setAppliedPeriod(draftPeriod)
    setAppliedKeyword(draftKeyword)
    setSystemLogsPage(1)
    setSelectedSystemLog(null)
  }

  const resetSystemLogFilters = () => {
    setDraftTypeFilter('전체')
    setDraftActorFilter('전체')
    setDraftPeriod('')
    setDraftKeyword('')
    setAppliedTypeFilter('전체')
    setAppliedActorFilter('전체')
    setAppliedPeriod('')
    setAppliedKeyword('')
    setSystemLogsPage(1)
    setSelectedSystemLog(null)
  }

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      if (!isSupabaseConfigured) {
        if (!cancelled) {
          setGpsLogs([])
          setGpsStatus('idle')
        }
        return
      }
      if (!cancelled) setGpsStatus((s) => (s === 'ok' ? s : 'loading'))
      const rows = await fetchGpsReceiveLogs()
      if (cancelled) return
      if (rows == null) {
        setGpsStatus('error')
        return
      }
      setGpsLogs(rows)
      setGpsStatus('ok')
      setGpsUpdatedAt(new Date().toLocaleTimeString('ko-KR', { hour12: false }))
      setGpsPage(1)
      setSelectedGpsLog(null)
    }
    void load()
    const timer = window.setInterval(() => {
      void load()
    }, 5_000)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      if (!isSupabaseConfigured) {
        if (!cancelled) {
          setDbSystemLogs([])
          setSystemLogsStatus('idle')
        }
        return
      }
      if (!cancelled) setSystemLogsStatus((s) => (s === 'ok' ? s : 'loading'))
      const rows = await fetchSystemLogs()
      if (cancelled) return
      if (rows == null) {
        setSystemLogsStatus('error')
        return
      }
      setDbSystemLogs(rows)
      setSystemLogsStatus('ok')
    }
    void load()
    const timer = window.setInterval(() => {
      void load()
    }, 15_000)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [])

  const gpsPageClamped = Math.min(gpsPage, Math.max(1, Math.ceil(gpsLogs.length / GPS_LOGS_PAGE_SIZE)))
  const visibleGpsLogs = gpsLogs.slice(
    (gpsPageClamped - 1) * GPS_LOGS_PAGE_SIZE,
    gpsPageClamped * GPS_LOGS_PAGE_SIZE,
  )

  const systemLogsPageClamped = Math.min(
    systemLogsPage,
    Math.max(1, Math.ceil(systemLogsToShow.length / SYSTEM_LOGS_PAGE_SIZE)),
  )
  const visibleSystemLogs = systemLogsToShow.slice(
    (systemLogsPageClamped - 1) * SYSTEM_LOGS_PAGE_SIZE,
    systemLogsPageClamped * SYSTEM_LOGS_PAGE_SIZE,
  )

  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>기록조회</h3>
        </div>
        <div className="toolbar" style={{ flexWrap: 'wrap' }}>
          <div className="field" style={{ minWidth: 120 }}>
            <label>기록 유형</label>
            <select
              className="select"
              value={draftTypeFilter}
              onChange={(e) => setDraftTypeFilter(e.target.value)}
            >
              <option>전체</option>
              <option>운영 기록</option>
              <option>사용자 활동</option>
              <option>시스템 변경</option>
              <option>오류 / 경고</option>
            </select>
          </div>
          <div className="field" style={{ minWidth: 120 }}>
            <label>사용자</label>
            <select
              className="select"
              value={draftActorFilter}
              onChange={(e) => setDraftActorFilter(e.target.value)}
            >
              <option>전체</option>
              {actorOptions.map((actor) => (
                <option key={actor} value={actor}>
                  {actor}
                </option>
              ))}
            </select>
          </div>
          <div className="field" style={{ minWidth: 220 }}>
            <label>기간</label>
            <input
              className="input"
              value={draftPeriod}
              onChange={(e) => setDraftPeriod(e.target.value)}
              placeholder="YYYY.MM.DD ~ YYYY.MM.DD"
            />
          </div>
          <div className="field" style={{ flex: 1, minWidth: 180 }}>
            <label>키워드 검색</label>
            <input
              className="input"
              placeholder="검색어를 입력하세요."
              value={draftKeyword}
              onChange={(e) => setDraftKeyword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') applySystemLogFilters()
              }}
            />
          </div>
          <button
            className="btn btn-outline"
            type="button"
            style={{ alignSelf: 'end' }}
            onClick={resetSystemLogFilters}
          >
            초기화
          </button>
          <button
            className="btn btn-primary"
            type="button"
            style={{ alignSelf: 'end' }}
            onClick={applySystemLogFilters}
          >
            조회하기
          </button>
        </div>
      </section>

      <div className="card-head">
        <h3>
          기록 요약 <span className="muted">({appliedPeriod.trim() || '전체 기간'})</span>
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
            <span className="muted" style={{ fontSize: 12 }}>
              {systemLogsStatus === 'loading'
                ? '불러오는 중…'
                : systemLogsStatus === 'error'
                  ? '조회 실패 (관리자 권한/RLS 확인)'
                  : systemLogsStatus === 'ok'
                    ? `${systemLogsToShow.length}건`
                    : !isSupabaseConfigured
                      ? 'mock'
                      : ''}
            </span>
          </div>
          <table className="data-table system-logs-table">
            <thead>
              <tr>
                <th className="col-time">시간</th>
                <th className="col-type">기록 유형</th>
                <th className="col-action">상세 내용</th>
                <th className="col-actor">사용자</th>
                <th className="col-target">대상</th>
                <th className="col-result">결과</th>
              </tr>
            </thead>
            <tbody>
              {systemLogsToShow.length === 0 ? (
                <tr>
                  <td colSpan={6} className="muted" style={{ textAlign: 'center', padding: 24 }}>
                    {systemLogsStatus === 'error'
                      ? '시스템 기록을 불러오지 못했습니다. 관리자 계정으로 로그인했는지 확인해 주세요.'
                      : systemLogsStatus === 'loading'
                        ? '불러오는 중…'
                        : '저장된 시스템 기록이 없습니다.'}
                  </td>
                </tr>
              ) : (
                visibleSystemLogs.map((row) => (
                <tr
                  key={row.id ?? row.time + row.action}
                  role="button"
                  tabIndex={0}
                  onClick={() => setSelectedSystemLog(row)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') setSelectedSystemLog(row)
                  }}
                  style={{
                    cursor: 'pointer',
                    outline: 'none',
                    background: selectedSystemLog && 'id' in selectedSystemLog && 'id' in row && selectedSystemLog.id === row.id ? '#f2f6ff' : undefined,
                  }}
                >
                  <td className="col-time">{row.time}</td>
                  <td className="col-type">{row.type}</td>
                  <td className="col-action">{row.action}</td>
                  <td className="col-actor">{row.actor}</td>
                  <td className="col-target">{row.target}</td>
                  <td className="col-result">
                    <StatusBadge
                      tone={row.result === '성공' ? 'green' : row.result === '경고' ? 'orange' : 'red'}
                    >
                      {row.result}
                    </StatusBadge>
                  </td>
                </tr>
              ))
              )}
            </tbody>
          </table>
          <ListPagination
            total={systemLogsToShow.length}
            page={systemLogsPageClamped}
            pageSize={SYSTEM_LOGS_PAGE_SIZE}
            onPageChange={setSystemLogsPage}
            ariaLabel="시스템 로그 페이지"
          />
          <p className="muted" style={{ fontSize: 11, marginTop: 8 }}>
            시스템 시간 기준으로 기록이 저장됩니다.
          </p>

          {selectedSystemLog ? (
            <div
              style={{
                marginTop: 12,
                border: '1px solid var(--color-border)',
                borderRadius: 12,
                padding: 12,
                background: 'var(--color-surface)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                <div style={{ fontWeight: 800 }}>선택된 로그 상세</div>
                <button className="btn btn-outline btn-xs" type="button" onClick={() => setSelectedSystemLog(null)}>
                  닫기
                </button>
              </div>
              <div className="muted" style={{ fontSize: 12, lineHeight: 1.7 }}>
                <div>시간: {selectedSystemLog.time}</div>
                <div>유형: {selectedSystemLog.type}</div>
                <div>상세 내용: {selectedSystemLog.action}</div>
                <div>사용자: {selectedSystemLog.actor ?? '-'}</div>
                <div>대상: {selectedSystemLog.target ?? '-'}</div>
                <div>결과: {selectedSystemLog.result}</div>
              </div>
            </div>
          ) : null}
        </section>

        <div className="stack">
          <section className="card card-pad">
            <div className="card-head">
              <h3>보관 정책</h3>
              <button className="btn btn-outline" type="button" style={{ height: 28, fontSize: 12 }}>
                보관 정책 관리
              </button>
            </div>
            <p className="muted" style={{ fontSize: 12, margin: 0, lineHeight: 1.6 }}>
              시스템 기록은 1년간 보관됩니다. 보관 기간 이후 데이터는 자동 삭제됩니다.
              <br />
              operation_logs(GPS 포함)는 우선 최대 {GPS_LOGS_MAX}건만 유지합니다.
            </p>
          </section>
        </div>
      </div>

      <div className="split-14">
        <section className="card card-pad">
          <div className="card-head">
          <h3>GPS 수신 목록</h3>
          <span className="muted" style={{ fontSize: 12 }}>
            {isSupabaseConfigured
              ? gpsStatus === 'error'
                ? 'DB 조회 실패 · RLS/테이블 확인'
                : gpsStatus === 'loading' && gpsLogs.length === 0
                  ? '불러오는 중…'
                  : `LOCATION_UPDATED · ${gpsLogs.length}/${GPS_LOGS_MAX}건${gpsUpdatedAt ? ` · 갱신 ${gpsUpdatedAt}` : ''} · 5초 폴링`
              : 'Supabase 미설정'}
          </span>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>수신 시각</th>
              <th>차량</th>
                <th>위도</th>
                <th>경도</th>
                <th>정확도</th>
                <th>상세</th>
              </tr>
            </thead>
            <tbody>
              {gpsLogs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="muted">
                    {isSupabaseConfigured
                      ? '아직 GPS 수신 로그가 없습니다. 기사 앱에서 운행을 시작하면 LOCATION_UPDATED가 쌓입니다.'
                      : 'Supabase를 연결하면 operation_logs GPS가 표시됩니다.'}
                  </td>
                </tr>
              ) : (
                visibleGpsLogs.map((row) => (
                  <tr
                    key={row.id}
                    role="button"
                    tabIndex={0}
                    onClick={() => setSelectedGpsLog(row)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') setSelectedGpsLog(row)
                    }}
                    style={{ cursor: 'pointer', background: selectedGpsLog?.id === row.id ? '#f2f6ff' : undefined }}
                  >
                    <td>{row.createdAtLabel}</td>
                    <td>{row.vehicleName}</td>
                    <td>{row.lat != null ? row.lat.toFixed(6) : '-'}</td>
                    <td>{row.lng != null ? row.lng.toFixed(6) : '-'}</td>
                    <td>{row.accuracy ?? '-'}</td>
                    <td style={{ maxWidth: 360, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {row.message}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          <ListPagination
            total={gpsLogs.length}
            page={gpsPageClamped}
            pageSize={GPS_LOGS_PAGE_SIZE}
            onPageChange={setGpsPage}
            ariaLabel="GPS 로그 페이지"
          />
          <p className="muted" style={{ fontSize: 11, marginTop: 8 }}>
            DB `operation_logs`의 GPS 수신만 표시합니다. {GPS_LOGS_MAX}건을 넘으면 오래된 로그부터 삭제됩니다.
          </p>

          {selectedGpsLog ? (
            <div
              style={{
                marginTop: 12,
                border: '1px solid var(--color-border)',
                borderRadius: 12,
                padding: 12,
                background: 'var(--color-surface)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                <div style={{ fontWeight: 800 }}>선택된 GPS 상세</div>
                <button className="btn btn-outline btn-xs" type="button" onClick={() => setSelectedGpsLog(null)}>
                  닫기
                </button>
              </div>
              <div className="muted" style={{ fontSize: 12, lineHeight: 1.7 }}>
                <div>수신 시각: {selectedGpsLog.createdAtLabel}</div>
                <div>차량: {selectedGpsLog.vehicleName}</div>
                <div>운행 ID: {selectedGpsLog.operationId}</div>
                <div>위도: {selectedGpsLog.lat != null ? selectedGpsLog.lat.toFixed(6) : '-'}</div>
                <div>경도: {selectedGpsLog.lng != null ? selectedGpsLog.lng.toFixed(6) : '-'}</div>
                <div>정확도: {selectedGpsLog.accuracy ?? '-'}</div>
                <div>메시지: {selectedGpsLog.message}</div>
                <div>이벤트 타입: {selectedGpsLog.eventType}</div>
              </div>
            </div>
          ) : null}
        </section>
        <div className="stack" />
      </div>
    </div>
  )
}

type SettingsSection = 'account' | 'security' | 'notifications' | 'operations' | 'integrations'

type AdminSettings = {
  loginFailLimit: number
  sessionTimeoutMin: number
  passwordMinLength: number
  passwordRotateDays: number
  notifyEmail: string
  timezone: string
  notifySafetyStop: boolean
  notifyOperationChange: boolean
  notifySystemAlert: boolean
  notifyEmailDigest: boolean
  defaultBusCapacity: number
  liveMapRefreshSec: number
  autoAssignDriver: boolean
  showInactiveRoutes: boolean
}

const SETTINGS_STORAGE_KEY = 'onda-admin-settings'

const DEFAULT_SETTINGS: AdminSettings = {
  loginFailLimit: 5,
  sessionTimeoutMin: 30,
  passwordMinLength: 10,
  passwordRotateDays: 90,
  notifyEmail: 'admin@mju.ac.kr',
  timezone: 'Asia/Seoul',
  notifySafetyStop: true,
  notifyOperationChange: true,
  notifySystemAlert: true,
  notifyEmailDigest: false,
  defaultBusCapacity: 45,
  liveMapRefreshSec: 3,
  autoAssignDriver: false,
  showInactiveRoutes: false,
}

function loadAdminSettings(): AdminSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_STORAGE_KEY)
    if (!raw) return { ...DEFAULT_SETTINGS }
    return { ...DEFAULT_SETTINGS, ...(JSON.parse(raw) as Partial<AdminSettings>) }
  } catch {
    return { ...DEFAULT_SETTINGS }
  }
}

function SettingsToggle({
  checked,
  onChange,
  label,
}: {
  checked: boolean
  onChange: (next: boolean) => void
  label: string
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      className={`settings-switch${checked ? ' is-on' : ''}`}
      onClick={() => onChange(!checked)}
    >
      <span className="settings-switch-knob" />
    </button>
  )
}

function SettingsRow({
  title,
  description,
  children,
}: {
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <div className="settings-row">
      <div className="settings-row-copy">
        <div className="settings-row-title">{title}</div>
        {description ? <div className="settings-row-desc">{description}</div> : null}
      </div>
      <div className="settings-row-control">{children}</div>
    </div>
  )
}

export function SettingsPage() {
  const { user, usingSupabase } = useAuth()
  const [section, setSection] = useState<SettingsSection>('account')
  const [draft, setDraft] = useState<AdminSettings>(() => loadAdminSettings())
  const [saved, setSaved] = useState<AdminSettings>(() => loadAdminSettings())
  const [saveMessage, setSaveMessage] = useState<string | null>(null)
  const naverMapReady = Boolean((import.meta.env.VITE_NAVER_MAP_CLIENT_ID || '').trim())

  const dirty = useMemo(() => JSON.stringify(draft) !== JSON.stringify(saved), [draft, saved])

  const patch = <K extends keyof AdminSettings>(key: K, value: AdminSettings[K]) => {
    setDraft((prev) => ({ ...prev, [key]: value }))
    setSaveMessage(null)
  }

  const onSave = () => {
    localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(draft))
    setSaved(draft)
    setSaveMessage('설정이 저장되었습니다.')
  }

  const onReset = () => {
    setDraft(saved)
    setSaveMessage(null)
  }

  const onRestoreDefaults = () => {
    const next = {
      ...DEFAULT_SETTINGS,
      notifyEmail: user?.email || DEFAULT_SETTINGS.notifyEmail,
    }
    setDraft(next)
    setSaveMessage(null)
  }

  useEffect(() => {
    if (user?.email && draft.notifyEmail === DEFAULT_SETTINGS.notifyEmail) {
      setDraft((prev) => ({ ...prev, notifyEmail: user.email }))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.email])

  const sections: { id: SettingsSection; label: string; hint: string }[] = [
    { id: 'account', label: '계정', hint: '로그인 계정 정보' },
    { id: 'security', label: '보안', hint: '세션·비밀번호 정책' },
    { id: 'notifications', label: '알림', hint: '수신 채널·이벤트' },
    { id: 'operations', label: '운행', hint: '관제·배차 기본값' },
    { id: 'integrations', label: '연동', hint: '외부 서비스 상태' },
  ]

  const roleLabel =
    user?.role === 'ADMIN' ? '관리자' : user?.role === 'DRIVER' ? '기사' : user?.role === 'STUDENT' ? '학생' : '-'

  return (
    <div className="page">
      <div className="settings-hero">
        <div>
          <h2 className="page-title">설정</h2>
          <p className="page-subtitle">관리자 계정, 보안 정책, 알림·운행 기본값을 관리합니다.</p>
        </div>
        <div className="settings-hero-actions">
          {saveMessage ? <span className="settings-save-toast">{saveMessage}</span> : null}
          <button className="btn btn-outline" type="button" onClick={onRestoreDefaults}>
            기본값
          </button>
          <button className="btn btn-ghost" type="button" disabled={!dirty} onClick={onReset}>
            취소
          </button>
          <button className="btn btn-primary" type="button" disabled={!dirty} onClick={onSave}>
            변경 사항 저장
          </button>
        </div>
      </div>

      <div className="settings-layout">
        <nav className="card settings-nav" aria-label="설정 메뉴">
          {sections.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`settings-nav-item${section === item.id ? ' is-active' : ''}`}
              onClick={() => setSection(item.id)}
            >
              <span className="settings-nav-label">{item.label}</span>
              <span className="settings-nav-hint">{item.hint}</span>
            </button>
          ))}
        </nav>

        <div className="settings-main">
          {section === 'account' ? (
            <section className="card card-pad settings-panel">
              <div className="card-head">
                <div>
                  <h3>계정</h3>
                  <p className="settings-panel-desc">현재 로그인한 관리자 계정 정보입니다.</p>
                </div>
                <StatusBadge tone={usingSupabase ? 'blue' : 'gray'}>
                  {usingSupabase ? 'Supabase Auth' : '로컬 데모'}
                </StatusBadge>
              </div>
              <div className="settings-profile">
                <div className="settings-avatar" aria-hidden>
                  {(user?.name || '관').slice(0, 1)}
                </div>
                <div>
                  <div className="settings-profile-name">{user?.name || '관리자'}</div>
                  <div className="muted">{user?.email || '-'}</div>
                </div>
              </div>
              <div className="settings-divider" />
              <div className="grid grid-2">
                <div className="field">
                  <label>이름</label>
                  <input className="input" value={user?.name || ''} readOnly />
                </div>
                <div className="field">
                  <label>역할</label>
                  <input className="input" value={roleLabel} readOnly />
                </div>
                <div className="field">
                  <label>이메일</label>
                  <input className="input" value={user?.email || ''} readOnly />
                </div>
                <div className="field">
                  <label>사용자 ID</label>
                  <input className="input" value={user?.id || '-'} readOnly />
                </div>
              </div>
            </section>
          ) : null}

          {section === 'security' ? (
            <section className="card card-pad settings-panel">
              <div className="card-head">
                <div>
                  <h3>보안</h3>
                  <p className="settings-panel-desc">로그인·세션·비밀번호 정책을 설정합니다.</p>
                </div>
              </div>
              <div className="grid grid-2">
                <div className="field">
                  <label>연속 로그인 실패 허용 횟수</label>
                  <input
                    className="input"
                    type="number"
                    min={1}
                    max={20}
                    value={draft.loginFailLimit}
                    onChange={(e) => patch('loginFailLimit', Number(e.target.value) || 1)}
                  />
                  <div className="field-hint">초과 시 계정 잠금이 적용됩니다.</div>
                </div>
                <div className="field">
                  <label>세션 타임아웃 (분)</label>
                  <input
                    className="input"
                    type="number"
                    min={5}
                    max={480}
                    value={draft.sessionTimeoutMin}
                    onChange={(e) => patch('sessionTimeoutMin', Number(e.target.value) || 5)}
                  />
                  <div className="field-hint">미사용 상태가 지속되면 자동 로그아웃됩니다.</div>
                </div>
                <div className="field">
                  <label>비밀번호 최소 길이</label>
                  <input
                    className="input"
                    type="number"
                    min={8}
                    max={64}
                    value={draft.passwordMinLength}
                    onChange={(e) => patch('passwordMinLength', Number(e.target.value) || 8)}
                  />
                </div>
                <div className="field">
                  <label>비밀번호 변경 주기 (일)</label>
                  <input
                    className="input"
                    type="number"
                    min={30}
                    max={365}
                    value={draft.passwordRotateDays}
                    onChange={(e) => patch('passwordRotateDays', Number(e.target.value) || 30)}
                  />
                </div>
              </div>
            </section>
          ) : null}

          {section === 'notifications' ? (
            <section className="card card-pad settings-panel">
              <div className="card-head">
                <div>
                  <h3>알림</h3>
                  <p className="settings-panel-desc">관제·운행 이벤트 수신 방식을 선택합니다.</p>
                </div>
              </div>
              <div className="field" style={{ maxWidth: 420, marginBottom: 8 }}>
                <label>알림 수신 이메일</label>
                <input
                  className="input"
                  type="email"
                  value={draft.notifyEmail}
                  onChange={(e) => patch('notifyEmail', e.target.value)}
                />
              </div>
              <div className="settings-divider" />
              <SettingsRow
                title="안전 정차 요청"
                description="기사 앱에서 안전 정차가 요청되면 즉시 알림을 받습니다."
              >
                <SettingsToggle
                  label="안전 정차 요청 알림"
                  checked={draft.notifySafetyStop}
                  onChange={(v) => patch('notifySafetyStop', v)}
                />
              </SettingsRow>
              <SettingsRow
                title="운행·배차 변경"
                description="운행 중단, 배차 변경, 긴급 공지 등록 시 알림을 받습니다."
              >
                <SettingsToggle
                  label="운행·배차 변경 알림"
                  checked={draft.notifyOperationChange}
                  onChange={(v) => patch('notifyOperationChange', v)}
                />
              </SettingsRow>
              <SettingsRow
                title="시스템 경고"
                description="GPS 수신 지연, 연동 오류 등 시스템 경고를 받습니다."
              >
                <SettingsToggle
                  label="시스템 경고 알림"
                  checked={draft.notifySystemAlert}
                  onChange={(v) => patch('notifySystemAlert', v)}
                />
              </SettingsRow>
              <SettingsRow
                title="일일 이메일 요약"
                description="하루 운행·제보·알림 요약을 이메일로 받습니다."
              >
                <SettingsToggle
                  label="일일 이메일 요약"
                  checked={draft.notifyEmailDigest}
                  onChange={(v) => patch('notifyEmailDigest', v)}
                />
              </SettingsRow>
            </section>
          ) : null}

          {section === 'operations' ? (
            <section className="card card-pad settings-panel">
              <div className="card-head">
                <div>
                  <h3>운행</h3>
                  <p className="settings-panel-desc">실시간 관제와 배차 화면의 기본값을 설정합니다.</p>
                </div>
              </div>
              <div className="grid grid-2">
                <div className="field">
                  <label>기본 타임존</label>
                  <select
                    className="select"
                    value={draft.timezone}
                    onChange={(e) => patch('timezone', e.target.value)}
                  >
                    <option value="Asia/Seoul">Asia/Seoul (한국 표준시)</option>
                    <option value="UTC">UTC</option>
                  </select>
                </div>
                <div className="field">
                  <label>실시간 지도 갱신 주기 (초)</label>
                  <select
                    className="select"
                    value={draft.liveMapRefreshSec}
                    onChange={(e) => patch('liveMapRefreshSec', Number(e.target.value))}
                  >
                    <option value={3}>3초</option>
                    <option value={5}>5초</option>
                    <option value={10}>10초</option>
                  </select>
                  <div className="field-hint">기사 GPS 업로드 주기와 맞춰 두는 것을 권장합니다.</div>
                </div>
                <div className="field">
                  <label>기본 차량 정원</label>
                  <input
                    className="input"
                    type="number"
                    min={1}
                    max={60}
                    value={draft.defaultBusCapacity}
                    onChange={(e) => patch('defaultBusCapacity', Number(e.target.value) || 1)}
                  />
                </div>
              </div>
              <div className="settings-divider" />
              <SettingsRow
                title="기사 자동 배정 제안"
                description="배차 등록 시 가용 기사를 우선순위로 제안합니다."
              >
                <SettingsToggle
                  label="기사 자동 배정 제안"
                  checked={draft.autoAssignDriver}
                  onChange={(v) => patch('autoAssignDriver', v)}
                />
              </SettingsRow>
              <SettingsRow
                title="비활성 노선 표시"
                description="노선 관리 목록에 비활성 노선도 함께 표시합니다."
              >
                <SettingsToggle
                  label="비활성 노선 표시"
                  checked={draft.showInactiveRoutes}
                  onChange={(v) => patch('showInactiveRoutes', v)}
                />
              </SettingsRow>
            </section>
          ) : null}

          {section === 'integrations' ? (
            <section className="card card-pad settings-panel">
              <div className="card-head">
                <div>
                  <h3>연동</h3>
                  <p className="settings-panel-desc">외부 서비스 연결 상태를 확인합니다. 키 값은 노출하지 않습니다.</p>
                </div>
              </div>
              <div className="settings-status-list">
                <div className="settings-status-item">
                  <div>
                    <div className="settings-row-title">Supabase</div>
                    <div className="settings-row-desc">인증 · DB · Realtime</div>
                  </div>
                  <StatusBadge tone={usingSupabase ? 'green' : 'orange'}>
                    {usingSupabase ? '연결됨' : '미설정'}
                  </StatusBadge>
                </div>
                <div className="settings-status-item">
                  <div>
                    <div className="settings-row-title">네이버 지도</div>
                    <div className="settings-row-desc">실시간 관제 지도 · Dynamic Map</div>
                  </div>
                  <StatusBadge tone={naverMapReady ? 'green' : 'orange'}>
                    {naverMapReady ? '연결됨' : '미설정'}
                  </StatusBadge>
                </div>
                <div className="settings-status-item">
                  <div>
                    <div className="settings-row-title">환경</div>
                    <div className="settings-row-desc">관리자 웹 런타임</div>
                  </div>
                  <StatusBadge tone="gray">{import.meta.env.MODE}</StatusBadge>
                </div>
              </div>
              <div className="field-hint" style={{ marginTop: 12 }}>
                연동 키는 `.env.local`에서만 관리하세요. 프론트엔드에 Client Secret을 넣지 마세요.
              </div>
            </section>
          ) : null}
        </div>
      </div>
    </div>
  )
}
