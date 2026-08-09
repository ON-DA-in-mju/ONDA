import { isSupabaseConfigured, supabase } from './supabase'
import type { Database } from '../types/database'

export type NoticeRow = Database['public']['Tables']['notices']['Row']
export type ReportRow = Database['public']['Tables']['reports']['Row']
export type RouteRow = Database['public']['Tables']['routes']['Row']
export type BusRow = Database['public']['Tables']['buses']['Row']
export type UserRow = Database['public']['Tables']['users']['Row']

export async function fetchNotices(): Promise<NoticeRow[] | null> {
  if (!isSupabaseConfigured) return null
  const { data, error } = await supabase.from('notices').select('*').order('created_at', { ascending: false })
  if (error) {
    console.error('[notices]', error.message)
    return null
  }
  return data
}

export async function createNotice(payload: {
  title: string
  content: string
  author_id?: string | null
}): Promise<{ ok: boolean; message?: string }> {
  if (!isSupabaseConfigured) return { ok: false, message: 'Supabase 미설정' }
  const { error } = await supabase.from('notices').insert(payload)
  if (error) return { ok: false, message: error.message }
  return { ok: true }
}

export async function fetchReports(): Promise<ReportRow[] | null> {
  if (!isSupabaseConfigured) return null
  const { data, error } = await supabase.from('reports').select('*').order('created_at', { ascending: false })
  if (error) {
    console.error('[reports]', error.message)
    return null
  }
  return data
}

export async function fetchRoutes(): Promise<RouteRow[] | null> {
  if (!isSupabaseConfigured) return null
  const { data, error } = await supabase.from('routes').select('*').order('created_at', { ascending: false })
  if (error) {
    console.error('[routes]', error.message)
    return null
  }
  return data
}

export async function fetchBuses(): Promise<BusRow[] | null> {
  if (!isSupabaseConfigured) return null
  const { data, error } = await supabase.from('buses').select('*').order('created_at', { ascending: false })
  if (error) {
    console.error('[buses]', error.message)
    return null
  }
  return data
}

export async function fetchUsers(): Promise<UserRow[] | null> {
  if (!isSupabaseConfigured) return null
  const { data, error } = await supabase.from('users').select('*').order('created_at', { ascending: false })
  if (error) {
    console.error('[users]', error.message)
    return null
  }
  return data
}
