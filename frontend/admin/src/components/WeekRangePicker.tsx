import { useEffect, useMemo, useRef, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import {
  WEEKDAY_LABELS,
  addDays,
  toDateKey,
  weekRangeFromDate,
} from '../lib/weekDate'

type Props = {
  weekStart: Date
  open: boolean
  onClose: () => void
  onPick: (weekStart: Date) => void
}

function monthLabel(d: Date) {
  return `${d.getFullYear()}년 ${String(d.getMonth() + 1).padStart(2, '0')}월`
}

function buildMonthCells(view: Date): (Date | null)[] {
  const first = new Date(view.getFullYear(), view.getMonth(), 1)
  const startPad = first.getDay()
  const daysInMonth = new Date(view.getFullYear(), view.getMonth() + 1, 0).getDate()
  const cells: (Date | null)[] = []
  for (let i = 0; i < startPad; i++) cells.push(null)
  for (let day = 1; day <= daysInMonth; day++) {
    cells.push(new Date(view.getFullYear(), view.getMonth(), day))
  }
  while (cells.length % 7 !== 0) cells.push(null)
  return cells
}

export function WeekRangePicker({ weekStart, open, onClose, onPick }: Props) {
  const rootRef = useRef<HTMLDivElement>(null)
  const [viewMonth, setViewMonth] = useState(
    () => new Date(weekStart.getFullYear(), weekStart.getMonth(), 1),
  )

  const weekEnd = useMemo(() => addDays(weekStart, 6), [weekStart])
  const startKey = toDateKey(weekStart)
  const endKey = toDateKey(weekEnd)
  const cells = useMemo(() => buildMonthCells(viewMonth), [viewMonth])

  useEffect(() => {
    if (!open) return
    setViewMonth(new Date(weekStart.getFullYear(), weekStart.getMonth(), 1))
  }, [open, weekStart])

  useEffect(() => {
    if (!open) return
    const onDoc = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) onClose()
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('mousedown', onDoc)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDoc)
      document.removeEventListener('keydown', onKey)
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="sched-week-picker" ref={rootRef} role="dialog" aria-label="주간 기간 선택">
      <div className="sched-week-picker-head">
        <button
          type="button"
          className="sched-week-picker-nav"
          aria-label="이전 달"
          onClick={() =>
            setViewMonth((m) => new Date(m.getFullYear(), m.getMonth() - 1, 1))
          }
        >
          <ChevronLeft size={18} />
        </button>
        <span className="sched-week-picker-title">{monthLabel(viewMonth)}</span>
        <button
          type="button"
          className="sched-week-picker-nav"
          aria-label="다음 달"
          onClick={() =>
            setViewMonth((m) => new Date(m.getFullYear(), m.getMonth() + 1, 1))
          }
        >
          <ChevronRight size={18} />
        </button>
      </div>

      <div className="sched-week-picker-dow">
        {WEEKDAY_LABELS.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>

      <div className="sched-week-picker-grid">
        {cells.map((date, idx) => {
          if (!date) {
            return <span key={`e-${idx}`} className="sched-week-picker-cell is-empty" />
          }
          const key = toDateKey(date)
          const inRange = key >= startKey && key <= endKey
          const isStart = key === startKey
          const isEnd = key === endKey
          return (
            <button
              key={key}
              type="button"
              className={[
                'sched-week-picker-cell',
                inRange ? 'in-range' : '',
                isStart ? 'is-start' : '',
                isEnd ? 'is-end' : '',
              ]
                .filter(Boolean)
                .join(' ')}
              onClick={() => {
                onPick(weekRangeFromDate(date).start)
                onClose()
              }}
            >
              <span className="sched-week-picker-num">{date.getDate()}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
