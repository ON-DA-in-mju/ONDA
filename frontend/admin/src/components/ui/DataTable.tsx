import type { ReactNode } from 'react'
import { StatusBadge } from '../ui/Form'

export type TableColumn<T> = {
  key: string
  header: string
  render: (row: T) => ReactNode
}

type DataTableProps<T> = {
  columns: TableColumn<T>[]
  rows: T[]
  rowKey: (row: T) => string
  onRowClick?: (row: T) => void
  selectedKey?: string
}

export function DataTable<T>({ columns, rows, rowKey, onRowClick, selectedKey }: DataTableProps<T>) {
  return (
    <table className="data-table">
      <thead>
        <tr>
          {columns.map((col) => (
            <th key={col.key}>{col.header}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => {
          const key = rowKey(row)
          return (
            <tr
              key={key}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              style={{
                cursor: onRowClick ? 'pointer' : undefined,
                background: selectedKey === key ? '#f5f8ff' : undefined,
              }}
            >
              {columns.map((col) => (
                <td key={col.key}>{col.render(row)}</td>
              ))}
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

type PageHeaderProps = {
  title?: string
  subtitle?: string
  actions?: ReactNode
}

export function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  if (!title && !subtitle && !actions) return null
  return (
    <div className="card-head" style={{ marginBottom: 4 }}>
      <div>
        {title ? <h2 className="page-title">{title}</h2> : null}
        {subtitle ? <p className="page-subtitle">{subtitle}</p> : null}
      </div>
      {actions}
    </div>
  )
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="card card-pad" style={{ textAlign: 'center', color: '#8b92a4' }}>
      {message}
    </div>
  )
}

export { StatusBadge }
