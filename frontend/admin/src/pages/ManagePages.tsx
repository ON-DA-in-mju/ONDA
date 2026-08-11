import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  AlertTriangle,
  AlignCenter,
  AlignJustify,
  AlignLeft,
  AlignRight,
  Bold,
  CalendarDays,
  Eye,
  Image as ImageIcon,
  Italic,
  Link2,
  List,
  ListOrdered,
  Megaphone,
  Underline,
} from 'lucide-react'
import { maintenances, notices, reports, routes, systemLogs as mockSystemLogs, users } from '../data/mock'
import {
  createNotice,
  fetchNotices,
  fetchReports,
  fetchRoutes,
  fetchUsers,
  type NoticeRow,
  type ReportRow,
  type RouteRow,
  type UserRow,
} from '../lib/api'
import { fetchLoginHistory, toLastLoginDisplay, type LoginHistoryEntry } from '../lib/loginHistoryApi'
import { fetchGpsReceiveLogs, GPS_LOGS_MAX, type GpsReceiveLog } from '../lib/gpsLogsApi'
import { isSupabaseConfigured } from '../lib/supabase'
import { fetchSystemLogs, type SystemLogRow } from '../lib/systemLogsApi'
import { useAuth } from '../state/AuthContext'
import { StatusBadge } from '../components/ui/Form'
import '../styles/figma-pages.css'

const reportStatusKo: Record<string, string> = {
  PENDING: '처리 대기',
  PROCESSING: '검토 중',
  COMPLETED: '처리 완료',
}

const NOTICE_BODY_MAX = 2000

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

/** ADM-05 커뮤니티 제보 관리 */
export function ReportsPage() {
  const [selected, setSelected] = useState(0)
  const [dbReports, setDbReports] = useState<ReportRow[] | null>(null)

  useEffect(() => {
    void fetchReports().then((data) => {
      if (data) setDbReports(data)
    })
  }, [])

  const usingDb = Boolean(dbReports && dbReports.length >= 0 && isSupabaseConfigured && dbReports !== null)
  const item = usingDb && dbReports?.[selected] ? dbReports[selected] : null
  const mockItem = reports[selected]

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
              {dbReports
                ? dbReports.map((row, idx) => (
                    <tr key={row.id} style={idx === selected ? { background: '#f5f8ff' } : undefined}>
                      <td style={{ fontWeight: 700 }}>{row.title}</td>
                      <td>
                        <StatusBadge tone={row.status === 'PENDING' ? 'orange' : row.status === 'PROCESSING' ? 'blue' : 'green'}>
                          {reportStatusKo[row.status] ?? row.status}
                        </StatusBadge>
                      </td>
                      <td>{row.created_at ? new Date(row.created_at).toLocaleString('ko-KR') : '-'}</td>
                      <td>
                        <button className="btn btn-outline" type="button" style={{ height: 28 }} onClick={() => setSelected(idx)}>
                          상세
                        </button>
                      </td>
                    </tr>
                  ))
                : reports.map((row, idx) => (
                    <tr key={row.type + row.time} style={idx === selected ? { background: '#f5f8ff' } : undefined}>
                      <td>{row.type}</td>
                      <td>{row.target}</td>
                      <td>{row.time}</td>
                      <td>{row.likes}</td>
                      <td>
                        <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                      </td>
                      <td>
                        <button className="btn btn-outline" type="button" style={{ height: 28 }} onClick={() => setSelected(idx)}>
                          상세
                        </button>
                      </td>
                    </tr>
                  ))}
            </tbody>
          </table>
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
  const [startDate, setStartDate] = useState('2024-05-20')
  const [startHour, setStartHour] = useState('00')
  const [startMinute, setStartMinute] = useState('00')
  const [endDate, setEndDate] = useState('2024-05-20')
  const [endHour, setEndHour] = useState('23')
  const [endMinute, setEndMinute] = useState('59')
  const [saving, setSaving] = useState(false)
  const [flash, setFlash] = useState('')
  const editorRef = useRef<HTMLDivElement | null>(null)
  const imageInputRef = useRef<HTMLInputElement | null>(null)

  const hourOptions = useMemo(() => Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0')), [])
  const minuteOptions = useMemo(() => Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0')), [])

  const syncEditorState = () => {
    const el = editorRef.current
    if (!el) return
    const plain = htmlToPlainText(el.innerHTML)
    if (plain.length > NOTICE_BODY_MAX) {
      // 초과 시 마지막 입력을 되돌림
      document.execCommand('undo')
      return
    }
    setBodyHtml(el.innerHTML)
    setBodyLen(plain.replace(/\n$/g, '').length)
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
  }

  const onInsertLink = () => {
    const url = window.prompt('링크 URL을 입력하세요.', 'https://')
    if (!url?.trim()) return
    runEditorCommand('createLink', url.trim())
  }

  const onInsertImage = () => {
    imageInputRef.current?.click()
  }

  const onImageFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => {
      const src = typeof reader.result === 'string' ? reader.result : ''
      if (!src) return
      runEditorCommand('insertImage', src)
    }
    reader.readAsDataURL(file)
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
    setSaving(true)
    setFlash('')
    const contentPlain = htmlToPlainText(editorRef.current?.innerHTML ?? bodyHtml).trim()
    const res = await createNotice({
      title: `[${noticeType}] ${title.trim()}`,
      content: contentPlain,
      author_id: user?.id ?? null,
    })
    setSaving(false)
    if (!res.ok) {
      setFlash(res.message ?? '등록 실패')
      return
    }
    setFlash('공지 등록 완료 (notices.title/content)')
    const data = await fetchNotices()
    if (data) setDbNotices(data)
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
    const urgent = recent.filter((n) => /긴급|\[URGENT\]/i.test(n.title)).length
    // type/starts_at/views 컬럼 전까지: 예약·조회수는 DB에 없으므로 0
    return {
      total: recent.length,
      urgent,
      scheduled: 0,
      views: 0,
    }
  }, [dbNotices])

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
              <span className="muted">(전체 {(dbNotices?.length ?? 0).toLocaleString('ko-KR')}건)</span>
            </h3>
          </div>
          <div className="toolbar" style={{ marginBottom: 8 }}>
            <select className="select" style={{ width: 110, height: 32 }}>
              <option>전체 유형</option>
              <option>긴급</option>
              <option>중요</option>
              <option>운행 변경</option>
              <option>일반</option>
            </select>
            <input className="input" style={{ flex: 1, height: 32 }} placeholder="제목 또는 내용을 검색하세요." />
            <button className="btn btn-primary btn-xs" type="button">
              검색
            </button>
          </div>
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
              {dbNotices
                ? dbNotices.map((row) => (
                    <tr
                      key={row.id}
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        setTitle(row.title.replace(/^\[.*?\]\s*/, ''))
                        setEditorContent(row.content)
                      }}
                    >
                      <td colSpan={2} style={{ fontWeight: 700 }}>
                        {row.title}
                      </td>
                      <td colSpan={3} style={{ maxWidth: 260, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {row.content}
                      </td>
                      <td>{row.created_at ? new Date(row.created_at).toLocaleDateString('ko-KR') : '-'}</td>
                      <td>
                        <StatusBadge tone="blue">DB</StatusBadge>
                      </td>
                    </tr>
                  ))
                : notices.map((row) => (
                <tr key={row.no}>
                  <td>{row.no}</td>
                  <td>
                    <StatusBadge tone={row.tone}>{row.type}</StatusBadge>
                  </td>
                  <td>{row.title}</td>
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
          <div className="pagination">
            <div className="pagination-pages">
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} className={`page-chip${n === 1 ? ' active' : ''}`} type="button">
                  {n}
                </button>
              ))}
            </div>
            <select className="select" style={{ width: 110, height: 28 }}>
              <option>10개씩 보기</option>
            </select>
          </div>
          <p className="muted" style={{ fontSize: 11, marginTop: 8 }}>
            공지사항은 학생 앱 [알림] 탭과 푸시 알림으로 발송됩니다.
          </p>
        </section>

        <section className="figma-panel notice-edit-panel">
          <div className="figma-panel-head">
            <h3>공지 등록/수정</h3>
          </div>

          <div className="notice-form-grid">
            <div className="notice-form-main">
              <div className="notice-form-actions">
                <button className="btn btn-outline btn-xs" type="button">
                  <Eye size={12} /> 미리보기
                </button>
                <button className="btn btn-outline btn-xs" type="button">
                  삭제
                </button>
                <button className="btn btn-outline btn-xs" type="button">
                  수정
                </button>
                <button className="btn btn-primary btn-xs" type="button" disabled={saving} onClick={() => void onCreate()}>
                  {saving ? '등록 중...' : '공지 등록'}
                </button>
              </div>

              <div className="field">
                <label>공지 유형</label>
                <div className="type-pills">
                  {(
                    [
                      ['긴급', true],
                      ['중요', false],
                      ['운행 변경', false],
                      ['일반', false],
                    ] as const
                  ).map(([t, danger]) => (
                    <button
                      key={t}
                      type="button"
                      className={`type-pill${noticeType === t ? ` active${danger ? ' danger' : ''}` : ''}`}
                      onClick={() => setNoticeType(t)}
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
                />
              </div>

              <div className="field">
                <label>내용</label>
                <div className="notice-editor">
                  <div className="notice-editor-toolbar" role="toolbar" aria-label="본문 서식">
                    <div className="notice-editor-group">
                      <button type="button" className="notice-editor-btn" title="굵게" aria-label="굵게" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('bold')}>
                        <Bold size={14} strokeWidth={2.5} />
                      </button>
                      <button type="button" className="notice-editor-btn" title="기울임" aria-label="기울임" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('italic')}>
                        <Italic size={14} strokeWidth={2.5} />
                      </button>
                      <button type="button" className="notice-editor-btn" title="밑줄" aria-label="밑줄" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('underline')}>
                        <Underline size={14} strokeWidth={2.5} />
                      </button>
                    </div>
                    <span className="notice-editor-sep" />
                    <div className="notice-editor-group">
                      <button type="button" className="notice-editor-btn" title="글머리 기호" aria-label="글머리 기호" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('insertUnorderedList')}>
                        <List size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className="notice-editor-btn" title="번호 목록" aria-label="번호 목록" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('insertOrderedList')}>
                        <ListOrdered size={14} strokeWidth={2.2} />
                      </button>
                    </div>
                    <span className="notice-editor-sep" />
                    <div className="notice-editor-group">
                      <button type="button" className="notice-editor-btn" title="왼쪽 정렬" aria-label="왼쪽 정렬" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('justifyLeft')}>
                        <AlignLeft size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className="notice-editor-btn" title="가운데 정렬" aria-label="가운데 정렬" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('justifyCenter')}>
                        <AlignCenter size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className="notice-editor-btn" title="오른쪽 정렬" aria-label="오른쪽 정렬" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('justifyRight')}>
                        <AlignRight size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className="notice-editor-btn" title="양쪽 정렬" aria-label="양쪽 정렬" onMouseDown={(e) => e.preventDefault()} onClick={() => runEditorCommand('justifyFull')}>
                        <AlignJustify size={14} strokeWidth={2.2} />
                      </button>
                    </div>
                    <span className="notice-editor-sep" />
                    <div className="notice-editor-group">
                      <button type="button" className="notice-editor-btn is-chip" title="링크" aria-label="링크" onMouseDown={(e) => e.preventDefault()} onClick={onInsertLink}>
                        <Link2 size={14} strokeWidth={2.2} />
                      </button>
                      <button type="button" className="notice-editor-btn" title="이미지" aria-label="이미지" onMouseDown={(e) => e.preventDefault()} onClick={onInsertImage}>
                        <ImageIcon size={14} strokeWidth={2.2} />
                      </button>
                    </div>
                  </div>
                  <input
                    ref={imageInputRef}
                    type="file"
                    accept="image/*"
                    hidden
                    onChange={onImageFileChange}
                  />
                  <div
                    ref={editorRef}
                    className={`notice-editor-body${bodyLen === 0 ? ' is-empty' : ''}`}
                    contentEditable
                    role="textbox"
                    aria-multiline="true"
                    aria-label="공지 내용"
                    data-placeholder="공지 내용을 입력하세요."
                    suppressContentEditableWarning
                    onInput={syncEditorState}
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
                  >
                    학생
                  </button>
                  <button
                    type="button"
                    className={`type-pill${targetDriver ? ' active' : ''}`}
                    onClick={() => setTargetDriver((v) => !v)}
                  >
                    기사
                  </button>
                  <label className="check-row notice-inline-check">
                    <input type="checkbox" checked={permanent} onChange={(e) => setPermanent(e.target.checked)} />
                    게시 기간 없음 (상시 게시)
                  </label>
                  <label className="check-row notice-inline-check">
                    <input type="checkbox" checked={push} onChange={(e) => setPush(e.target.checked)} />
                    푸시 알림 동시 발송
                  </label>
                </div>
                {push ? (
                  <div className="muted" style={{ fontSize: 11, marginTop: 4 }}>
                    ONDA 셔틀 앱 푸시 알림으로 즉시 발송됩니다.
                  </div>
                ) : null}
              </div>

              <div className={`notice-datetime-stack${permanent ? ' is-disabled' : ''}`}>
                <div className="field">
                  <label>시작일</label>
                  <div className="notice-datetime">
                    <input
                      className="input notice-date-input"
                      type="date"
                      value={startDate}
                      disabled={permanent}
                      onChange={(e) => setStartDate(e.target.value)}
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

            <aside className="phone-preview">
              <div className="cap">실제 학생 앱에 표시되는 화면입니다.</div>
              <div className="screen">
                <div className="muted" style={{ fontSize: 10, marginBottom: 4 }}>
                  공지사항
                </div>
                <span className="tag">{noticeType === '긴급' || noticeType === '일반' ? `${noticeType} 공지` : noticeType}</span>
                <strong>{title || '제목'}</strong>
                <div className="time">
                  {permanent
                    ? '상시 게시'
                    : `${startDate.replaceAll('-', '.')} ${startHour}:${startMinute}`}
                </div>
                <div
                  className="notice-preview-body"
                  dangerouslySetInnerHTML={{
                    __html: bodyLen > 0 ? bodyHtml : '<span style="color:#9ca3af">공지 내용을 입력하세요.</span>',
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
                  {htmlToPlainText(bodyHtml).trim().split('\n')[0] || '푸시 미리보기'}
                </div>
              </div>
            </aside>
          </div>
        </section>
      </div>
    </div>
  )
}

/** ADM-04 노선·운행 관리 — Figma 430:18166 */
export function RoutesPage() {
  const [selected, setSelected] = useState(0)
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
              {list.map((row, idx) => (
                <tr
                  key={row.name + idx}
                  style={idx === selected ? { background: '#f5f8ff' } : undefined}
                  onClick={() => setSelected(idx)}
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
              ))}
            </tbody>
          </table>
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
  const kpis = [
    { label: '전체 차량', value: '28', unit: '대', meta: '정상 20 · 정비중 4 · 점검 필요 4', tone: 'blue' },
    { label: '예정 정비', value: '6', unit: '건', meta: '이번 주 2 · 이번 달 6', tone: 'orange' },
    { label: '정비 완료', value: '12', unit: '건', meta: '지난달 대비 20%', tone: 'green' },
    { label: '정비 비용', value: '4,850', unit: '만원', meta: '(이번달) · 지난달 대비 15%', tone: 'purple' },
    { label: '가동률', value: '92.6', unit: '%', meta: '목표 90%', tone: 'blue' },
  ] as const

  return (
    <div className="page">
      <div className="toolbar" style={{ justifyContent: 'flex-end' }}>
        <select className="select" style={{ width: 120, height: 32 }}>
          <option>전체 차량</option>
        </select>
        <input className="input" style={{ width: 220, height: 32 }} defaultValue="2026.07.01 ~ 2026.07.31" />
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
              <input className="input" style={{ width: 220, height: 32 }} placeholder="차량 번호 / 정비 항목 검색" />
              <select className="select" style={{ width: 110, height: 32 }}>
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
              {maintenances.map((row) => (
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
              ))}
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
  const [selected, setSelected] = useState(0)
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

  const user = list[selected] ?? list[0]

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
              사용자 목록 <span className="muted">({list.length}명{dbUsers ? ' · DB' : ''})</span>
            </h3>
          </div>
          <div className="toolbar" style={{ marginBottom: 8 }}>
            <select className="select" style={{ width: 120, height: 32 }}>
              <option>전체 역할</option>
              <option>관리자</option>
              <option>운영자</option>
              <option>일반</option>
            </select>
            <input className="input" style={{ flex: 1, height: 32 }} placeholder="이름, 아이디, 이메일 검색" />
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
              {list.map((row, idx) => (
                <tr key={row.id} style={idx === selected ? { background: '#f5f8ff' } : undefined}>
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
                    <button className="btn btn-outline btn-xs" type="button" onClick={() => setSelected(idx)}>
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pagination">
            <div className="pagination-pages">
              {[1, 2, 3, 4].map((n) => (
                <button key={n} className={`page-chip${n === 1 ? ' active' : ''}`} type="button">
                  {n}
                </button>
              ))}
            </div>
            <select className="select" style={{ width: 110, height: 28 }}>
              <option>10개씩 보기</option>
            </select>
          </div>
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
export function SystemPage() {
  const [gpsLogs, setGpsLogs] = useState<GpsReceiveLog[]>([])
  const [gpsStatus, setGpsStatus] = useState<'idle' | 'loading' | 'ok' | 'error'>('idle')
  const [gpsUpdatedAt, setGpsUpdatedAt] = useState<string | null>(null)
  const [gpsPage, setGpsPage] = useState(0)
  const GPS_PAGE_SIZE = 10

  const [dbSystemLogs, setDbSystemLogs] = useState<SystemLogRow[]>([])
  const [systemLogsStatus, setSystemLogsStatus] = useState<'idle' | 'loading' | 'ok' | 'error'>('idle')
  const [systemLogsPage, setSystemLogsPage] = useState(0)
  const SYSTEM_LOGS_PAGE_SIZE = 5
  const systemLogsToShow: SystemLogRow[] = isSupabaseConfigured
    ? dbSystemLogs
    : (mockSystemLogs as SystemLogRow[])

  const [selectedSystemLog, setSelectedSystemLog] = useState<SystemLogRow | null>(null)
  const [selectedGpsLog, setSelectedGpsLog] = useState<GpsReceiveLog | null>(null)

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
      setGpsPage(0) // 실시간 갱신 시 가장 최신 페이지(1)로 이동
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

  const gpsTotalPages = Math.max(1, Math.ceil(gpsLogs.length / GPS_PAGE_SIZE))
  const pageClamped = Math.min(gpsPage, gpsTotalPages - 1)
  const visibleGpsLogs = gpsLogs.slice(pageClamped * GPS_PAGE_SIZE, (pageClamped + 1) * GPS_PAGE_SIZE)

  const systemLogsTotalPages = Math.max(1, Math.ceil(systemLogsToShow.length / SYSTEM_LOGS_PAGE_SIZE))
  const systemLogsPageClamped = Math.min(systemLogsPage, systemLogsTotalPages - 1)
  const visibleSystemLogs = systemLogsToShow.slice(
    systemLogsPageClamped * SYSTEM_LOGS_PAGE_SIZE,
    (systemLogsPageClamped + 1) * SYSTEM_LOGS_PAGE_SIZE,
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
            <select className="select">
              <option>전체</option>
              <option>운영 기록</option>
              <option>사용자 활동</option>
              <option>시스템 변경</option>
              <option>오류 / 경고</option>
            </select>
          </div>
          <div className="field" style={{ minWidth: 120 }}>
            <label>사용자</label>
            <select className="select">
              <option>전체</option>
            </select>
          </div>
          <div className="field" style={{ minWidth: 220 }}>
            <label>기간</label>
            <input className="input" defaultValue="2026.07.13 ~ 2026.07.20" />
          </div>
          <div className="field" style={{ flex: 1, minWidth: 180 }}>
            <label>키워드 검색</label>
            <input className="input" placeholder="검색어를 입력하세요." />
          </div>
          <button className="btn btn-outline" type="button" style={{ alignSelf: 'end' }}>
            초기화
          </button>
          <button className="btn btn-primary" type="button" style={{ alignSelf: 'end' }}>
            조회하기
          </button>
        </div>
      </section>

      <div className="card-head">
        <h3>
          기록 요약 <span className="muted">(2026.07.13 ~ 2026.07.20)</span>
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
          <div className="pagination">
            <div className="pagination-pages">
              {Array.from({ length: systemLogsTotalPages }).map((_, i) => {
                const n = i + 1
                const active = n - 1 === systemLogsPageClamped
                return (
                  <button
                    key={n}
                    className={`page-chip${active ? ' active' : ''}`}
                    type="button"
                    onClick={() => setSystemLogsPage(i)}
                  >
                    {n}
                  </button>
                )
              })}
            </div>
            <span className="muted" style={{ fontSize: 12 }}>
              {SYSTEM_LOGS_PAGE_SIZE}개씩 보기
            </span>
          </div>
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
          <div className="pagination" style={{ marginTop: 10 }}>
            <div className="pagination-pages">
              {Array.from({ length: gpsTotalPages }).map((_, i) => {
                const n = i + 1
                const active = n - 1 === pageClamped
                return (
                  <button
                    key={n}
                    className={`page-chip${active ? ' active' : ''}`}
                    type="button"
                    onClick={() => setGpsPage(i)}
                  >
                    {n}
                  </button>
                )
              })}
            </div>
          </div>
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

export function SettingsPage() {
  return (
    <div className="page">
      <section className="card card-pad">
        <div className="card-head">
          <h3>설정</h3>
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label>연속 로그인 실패 허용 횟수</label>
            <input className="input" defaultValue="5회" />
          </div>
          <div className="field">
            <label>세션 타임아웃</label>
            <input className="input" defaultValue="30분" />
          </div>
          <div className="field">
            <label>비밀번호 최소 길이</label>
            <input className="input" defaultValue="10자 이상" />
          </div>
          <div className="field">
            <label>비밀번호 변경 주기</label>
            <input className="input" defaultValue="90일" />
          </div>
          <div className="field">
            <label>알림 이메일</label>
            <input className="input" defaultValue="admin@mju.ac.kr" />
          </div>
          <div className="field">
            <label>기본 타임존</label>
            <select className="select" defaultValue="seoul">
              <option value="seoul">Asia/Seoul</option>
            </select>
          </div>
        </div>
        <div className="toolbar" style={{ marginTop: 16, justifyContent: 'flex-end' }}>
          <button className="btn btn-primary" type="button">
            저장
          </button>
        </div>
      </section>
    </div>
  )
}
