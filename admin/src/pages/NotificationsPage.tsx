import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  fetchAdminNotifications,
  markNotificationRead,
  type AdminNotification,
} from '../lib/adminNotificationsApi'
import { StatusBadge } from '../components/ui/Form'

function formatTime(ts: number) {
  const d = new Date(ts)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${hh}:${mm}`
}

export function NotificationsPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<AdminNotification[]>([])

  const load = async () => {
    const data = await fetchAdminNotifications()
    setItems(data.items)
  }

  useEffect(() => {
    void load()
    const timer = window.setInterval(() => void load(), 5_000)
    return () => window.clearInterval(timer)
  }, [])

  const onOpen = async (item: AdminNotification) => {
    if (!item.read) await markNotificationRead(item.id)
    navigate(item.href || '/live/suspend')
  }

  return (
    <div className="page">
      <div className="toolbar" style={{ marginBottom: 4 }}>
        <button
          className="btn btn-ghost"
          type="button"
          style={{ height: 30 }}
          onClick={() => navigate(-1)}
        >
          ← 이전으로
        </button>
      </div>
      <section className="card card-pad">
        <div className="card-head">
          <h3>알림</h3>
          <button className="btn btn-outline btn-xs" type="button" onClick={() => void load()}>
            새로고침
          </button>
        </div>
        {items.length === 0 ? (
          <div className="muted">표시할 알림이 없습니다.</div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {items.map((item) => (
              <button
                key={item.id}
                type="button"
                className="card card-pad"
                onClick={() => void onOpen(item)}
                style={{
                  textAlign: 'left',
                  cursor: 'pointer',
                  border: item.read ? '1px solid #eef1f6' : '1px solid #c7d7fb',
                  background: item.read ? '#fff' : '#f5f8ff',
                }}
              >
                <div className="toolbar" style={{ justifyContent: 'space-between', marginBottom: 6 }}>
                  <strong style={{ fontSize: 14 }}>{item.title}</strong>
                  <StatusBadge tone={item.read ? 'gray' : 'orange'}>
                    {item.read ? '확인됨' : '미확인'}
                  </StatusBadge>
                </div>
                <div style={{ fontSize: 13, color: '#374151', marginBottom: 6 }}>{item.body}</div>
                <div className="muted" style={{ fontSize: 11 }}>
                  {formatTime(item.createdAt)}
                </div>
              </button>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
