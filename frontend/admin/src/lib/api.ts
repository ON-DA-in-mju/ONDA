import { isSupabaseConfigured, supabase } from './supabase'
import type { Database } from '../types/database'

export type NoticeRow = Database['public']['Tables']['notices']['Row']
export type UserRow = Database['public']['Tables']['users']['Row']
export type RouteRow = Database['public']['Tables']['routes']['Row']
export type BusRow = Database['public']['Tables']['buses']['Row']
export type ReportRow = Database['public']['Tables']['reports']['Row']
export type StopRow = Database['public']['Tables']['stops']['Row']
export type ScheduleRow = Database['public']['Tables']['schedules']['Row']

async function selectAll<T>(table: keyof Database['public']['Tables'], order = 'created_at'): Promise<T[] | null> {
  if (!isSupabaseConfigured) return null
  const { data, error } = await supabase.from(table).select('*').order(order, { ascending: false })
  if (error) {
    console.error(`[${table}]`, error.message)
    return null
  }
  return data as T[]
}

export async function fetchNotices(): Promise<NoticeRow[] | null> {
  return selectAll<NoticeRow>('notices')
}

export async function createNotice(payload: {
  title: string
  content: string
  author_id?: string | null
}): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const { error } = await supabase.from('notices').insert({
    title: payload.title,
    content: payload.content,
    author_id: payload.author_id ?? null,
  })
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}

export async function fetchUsers(): Promise<UserRow[] | null> {
  return selectAll<UserRow>('users')
}

export async function fetchRoutes(): Promise<RouteRow[] | null> {
  return selectAll<RouteRow>('routes')
}

export async function fetchBuses(): Promise<BusRow[] | null> {
  return selectAll<BusRow>('buses')
}

export async function fetchReports(): Promise<ReportRow[] | null> {
  return selectAll<ReportRow>('reports')
}

export async function fetchStops(): Promise<StopRow[] | null> {
  return selectAll<StopRow>('stops')
}

export async function fetchSchedules(): Promise<ScheduleRow[] | null> {
  return selectAll<ScheduleRow>('schedules')
}

export async function updateReportStatus(
  id: string,
  status: Database['public']['Tables']['reports']['Row']['status'],
): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const { error } = await supabase.from('reports').update({ status }).eq('id', id)
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}
