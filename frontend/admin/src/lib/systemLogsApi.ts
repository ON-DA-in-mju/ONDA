import { isSupabaseConfigured, supabase } from './supabase'
import type { Database } from '../types/database'

export type SystemLogRow = {
  id?: string
  time: string
  type: string
  action: string
  actor: string | null
  ip: string | null
  target: string | null
  result: string
}

type SystemLogDbRow = Database['public']['Tables']['system_logs']['Row']

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

function formatLoggedAt(iso: string | null): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.getFullYear()}.${pad2(d.getMonth() + 1)}.${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

export async function fetchSystemLogs(): Promise<SystemLogRow[] | null> {
  if (!isSupabaseConfigured) return null

  const { data, error } = await supabase
    .from('system_logs')
    .select('id, logged_at, type, action, actor, ip, target, result')
    .order('logged_at', { ascending: false })
    .limit(100)

  if (error) {
    console.warn('[systemLogs] fetch', error.message)
    return null
  }

  return ((data ?? []) as SystemLogDbRow[]).map((row) => ({
    id: row.id,
    time: formatLoggedAt(row.logged_at),
    type: row.type,
    action: row.action,
    actor: row.actor,
    ip: row.ip,
    target: row.target,
    result: row.result,
  }))
}

