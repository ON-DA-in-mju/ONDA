import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { fetchStopCatalog, replaceRouteStops } from '../lib/routesApi'

const MIN_SLOTS = 3

type Row = { key: string; stopId: string }

let rowSeq = 0
function nextKey() {
  rowSeq += 1
  return `stop-row-${rowSeq}`
}

function filledIds(rows: Row[]): string[] {
  return rows.map((r) => r.stopId).filter(Boolean)
}

function sameIds(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false
  return a.every((id, i) => id === b[i])
}

function rowsFromIds(ids: string[]): Row[] {
  const next = ids.map((stopId) => ({ key: nextKey(), stopId }))
  while (next.length < MIN_SLOTS) next.push({ key: nextKey(), stopId: '' })
  return next.length ? next : [{ key: nextKey(), stopId: '' }]
}

type Props = {
  routeId: string
  initialStopIds: string[]
  /** false면 하단 저장을 부모(상세 페이지)에 맡김 */
  embeddedSave?: boolean
  onDraftChange?: (stopIds: string[], dirty: boolean) => void
  onSaved?: () => void
}

export function RouteStopsEditor({
  routeId,
  initialStopIds,
  embeddedSave = true,
  onDraftChange,
  onSaved,
}: Props) {
  const [options, setOptions] = useState<{ id: string; name: string }[] | null>(null)
  const [rows, setRows] = useState<Row[]>(() => rowsFromIds(initialStopIds))
  const [saving, setSaving] = useState(false)
  const initialKey = initialStopIds.join('|')
  const onDraftChangeRef = useRef(onDraftChange)
  onDraftChangeRef.current = onDraftChange

  useEffect(() => {
    void fetchStopCatalog().then((stops) => {
      setOptions(stops.map((s) => ({ id: s.id, name: s.name })))
    })
  }, [])

  useEffect(() => {
    setRows(rowsFromIds(initialStopIds))
  }, [routeId, initialKey])

  const dirty = useMemo(() => !sameIds(filledIds(rows), initialStopIds), [rows, initialStopIds])

  useEffect(() => {
    onDraftChangeRef.current?.(filledIds(rows), dirty)
  }, [rows, dirty])

  const setStopAt = (key: string, stopId: string) => {
    setRows((prev) => prev.map((row) => (row.key === key ? { ...row, stopId } : row)))
  }

  const addRow = () => {
    setRows((prev) => [...prev, { key: nextKey(), stopId: '' }])
  }

  const removeRow = (key: string) => {
    setRows((prev) => {
      const next = prev.filter((row) => row.key !== key)
      return next.length ? next : [{ key: nextKey(), stopId: '' }]
    })
  }

  const onSave = async () => {
    setSaving(true)
    const result = await replaceRouteStops(routeId, filledIds(rows))
    setSaving(false)
    if (!result.ok) {
      window.alert(result.message ?? '정류장 저장에 실패했습니다.')
      return
    }
    onSaved?.()
  }

  return (
    <div className="route-stops-editor">
      <table className="data-table">
        <thead>
          <tr>
            <th style={{ width: 56 }}>순번</th>
            <th>정류장</th>
            <th style={{ width: 72 }} />
          </tr>
        </thead>
        <tbody>
          {options === null ? (
            <tr>
              <td colSpan={3} className="muted">
                정류장 목록을 불러오는 중…
              </td>
            </tr>
          ) : (
            rows.map((row, idx) => (
              <tr key={row.key}>
                <td>{idx + 1}</td>
                <td>
                  <select
                    className="select"
                    value={row.stopId}
                    onChange={(e) => setStopAt(row.key, e.target.value)}
                  >
                    <option value="">정류장 선택</option>
                    {options.map((stop) => (
                      <option key={stop.id} value={stop.id}>
                        {stop.name}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <button
                    className="btn btn-outline btn-xs"
                    type="button"
                    aria-label="이 정류장 제거"
                    onClick={() => removeRow(row.key)}
                    disabled={rows.length <= 1 && !row.stopId}
                  >
                    <Trash2 size={13} />
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      <div className="toolbar" style={{ marginTop: 10, justifyContent: 'space-between', gap: 8 }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <button className="btn btn-outline" type="button" style={{ height: 30 }} onClick={addRow}>
            <Plus size={13} />
            정류장 추가
          </button>
          <Link className="muted" to={`/stops/new?routeId=${encodeURIComponent(routeId)}`} style={{ fontSize: 12 }}>
            새 정류장 등록
          </Link>
        </div>
        {embeddedSave ? (
          <button
            className="btn btn-primary"
            type="button"
            style={{ height: 30 }}
            disabled={!dirty || saving}
            onClick={() => void onSave()}
          >
            {saving ? '저장 중…' : '저장'}
          </button>
        ) : null}
      </div>
    </div>
  )
}
