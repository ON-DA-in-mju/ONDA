/** ONDA Supabase public schema — 실제 DB와 동기화 */

export type UserRole = 'STUDENT' | 'DRIVER' | 'ADMIN'
/** @deprecated user_role 사용. 하위 호환용 alias */
export type AdminRole = UserRole

export type BusStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'
export type OperationStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type ReportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED'
export type NotificationType = 'NOTICE' | 'SYSTEM' | 'OPERATION'
export type Weekday = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN'
export type SemesterType = 'SEMESTER' | 'VACATION'
export type OperationLogType =
  | 'START'
  | 'END'
  | 'GPS_CONNECTED'
  | 'GPS_DISCONNECTED'
  | 'LOCATION_UPDATED'
  | 'DRIVER_CHANGED'
  | 'STATUS_CHANGED'
  | 'SYSTEM'

export type Json = string | number | boolean | null | { [key: string]: Json | undefined } | Json[]

type Tables = {
  buses: {
    Row: {
      id: string
      bus_name: string
      vehicle_number: string
      capacity: number
      status: BusStatus
      created_at: string
      updated_at: string
    }
    Insert: {
      id?: string
      bus_name: string
      vehicle_number: string
      capacity?: number
      status?: BusStatus
      created_at?: string
      updated_at?: string
    }
    Update: Partial<Tables['buses']['Insert']>
    Relationships: []
  }
  routes: {
    Row: {
      id: string
      route_name: string
      direction: string | null
      description: string | null
      is_active: boolean
      created_at: string
      updated_at: string
      start_location: string | null
      end_location: string | null
    }
    Insert: {
      id?: string
      route_name: string
      direction?: string | null
      description?: string | null
      is_active?: boolean
      created_at?: string
      updated_at?: string
      start_location?: string | null
      end_location?: string | null
    }
    Update: Partial<Tables['routes']['Insert']>
    Relationships: []
  }
  stops: {
    Row: {
      id: string
      stop_name: string
      latitude: number
      longitude: number
      created_at: string
      updated_at: string
    }
    Insert: {
      id?: string
      stop_name: string
      latitude: number
      longitude: number
      created_at?: string
      updated_at?: string
    }
    Update: Partial<Tables['stops']['Insert']>
    Relationships: []
  }
  route_stops: {
    Row: {
      id: string
      route_id: string
      stop_id: string
      stop_order: number
      expected_minutes: number | null
      created_at: string
    }
    Insert: {
      id?: string
      route_id: string
      stop_id: string
      stop_order: number
      expected_minutes?: number | null
      created_at?: string
    }
    Update: Partial<Tables['route_stops']['Insert']>
    Relationships: []
  }
  schedules: {
    Row: {
      id: string
      route_id: string
      departure_time: string
      weekday: Weekday
      semester: SemesterType
      created_at: string
      updated_at: string
    }
    Insert: {
      id?: string
      route_id: string
      departure_time: string
      weekday: Weekday
      semester?: SemesterType
      created_at?: string
      updated_at?: string
    }
    Update: Partial<Tables['schedules']['Insert']>
    Relationships: []
  }
  users: {
    Row: {
      id: string
      name: string
      phone: string | null
      role: UserRole
      student_no: string | null
      profile_image: string | null
      created_at: string | null
      updated_at: string | null
      email: string | null
      /** 기사 앱 mock id (user01~user05) */
      login_id: string | null
    }
    Insert: {
      id: string
      name: string
      phone?: string | null
      role?: UserRole
      student_no?: string | null
      profile_image?: string | null
      created_at?: string | null
      updated_at?: string | null
      email?: string | null
      login_id?: string | null
    }
    Update: Partial<Omit<Tables['users']['Insert'], 'id'>> & { id?: string }
    Relationships: []
  }
  operations: {
    Row: {
      id: string
      schedule_id: string
      driver_id: string
      bus_id: string
      operation_date: string
      status: OperationStatus
      started_at: string | null
      ended_at: string | null
      created_at: string | null
      updated_at: string | null
      /** 로컬 mock 배차 id (op-0905 …) */
      external_id: string | null
      round: number | null
      origin: string | null
      destination: string | null
      expected_end_time: string | null
    }
    Insert: {
      id?: string
      schedule_id: string
      driver_id: string
      bus_id: string
      operation_date: string
      status?: OperationStatus
      started_at?: string | null
      ended_at?: string | null
      created_at?: string | null
      updated_at?: string | null
      external_id?: string | null
      round?: number | null
      origin?: string | null
      destination?: string | null
      expected_end_time?: string | null
    }
    Update: Partial<Tables['operations']['Insert']>
    Relationships: []
  }
  vehicle_locations: {
    Row: {
      id: string
      operation_id: string
      latitude: number
      longitude: number
      speed: number | null
      heading: number | null
      recorded_at: string | null
    }
    Insert: {
      id?: string
      operation_id: string
      latitude: number
      longitude: number
      speed?: number | null
      heading?: number | null
      recorded_at?: string | null
    }
    Update: Partial<Tables['vehicle_locations']['Insert']>
    Relationships: []
  }
  notices: {
    Row: {
      id: string
      title: string
      content: string
      author_id: string | null
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id?: string
      title: string
      content: string
      author_id?: string | null
      created_at?: string | null
      updated_at?: string | null
    }
    Update: Partial<Tables['notices']['Insert']>
    Relationships: []
  }
  reports: {
    Row: {
      id: string
      user_id: string
      title: string
      content: string
      status: ReportStatus
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id?: string
      user_id: string
      title: string
      content: string
      status?: ReportStatus
      created_at?: string | null
      updated_at?: string | null
    }
    Update: Partial<Tables['reports']['Insert']>
    Relationships: []
  }
  notifications: {
    Row: {
      id: string
      user_id: string
      title: string
      message: string
      type: NotificationType
      is_read: boolean | null
      created_at: string | null
    }
    Insert: {
      id?: string
      user_id: string
      title: string
      message: string
      type: NotificationType
      is_read?: boolean | null
      created_at?: string | null
    }
    Update: Partial<Tables['notifications']['Insert']>
    Relationships: []
  }
  operation_logs: {
    Row: {
      id: string
      operation_id: string
      log_message: string
      created_at: string | null
      event_type: OperationLogType
    }
    Insert: {
      id?: string
      operation_id: string
      log_message: string
      created_at?: string | null
      event_type: OperationLogType
    }
    Update: Partial<Tables['operation_logs']['Insert']>
    Relationships: []
  }
  vehicles: {
    Row: {
      id: string
      name: string
      plate: string
      status: string
      mileage: string | null
      next_maintenance: string | null
      created_at: string
    }
    Insert: {
      id?: string
      name: string
      plate: string
      status?: string
      mileage?: string | null
      next_maintenance?: string | null
      created_at?: string
    }
    Update: Partial<Tables['vehicles']['Insert']>
    Relationships: []
  }
  maintenances: {
    Row: {
      id: string
      maintained_at: string
      plate: string
      item: string
      type: string
      mechanic: string | null
      cost: number
      status: string
      created_at: string
    }
    Insert: {
      id?: string
      maintained_at: string
      plate: string
      item: string
      type: string
      mechanic?: string | null
      cost?: number
      status?: string
      created_at?: string
    }
    Update: Partial<Tables['maintenances']['Insert']>
    Relationships: []
  }
  system_logs: {
    Row: {
      id: string
      logged_at: string
      type: string
      action: string
      actor: string | null
      ip: string | null
      target: string | null
      result: string
    }
    Insert: {
      id?: string
      logged_at?: string
      type: string
      action: string
      actor?: string | null
      ip?: string | null
      target?: string | null
      result?: string
    }
    Update: Partial<Tables['system_logs']['Insert']>
    Relationships: []
  }
  safe_stop_requests: {
    Row: {
      id: string
      operation_id: string | null
      driver_id: string
      reason: string
      detail_reason: string | null
      decision: 'pending' | 'continue' | 'stop' | 'cancelled'
      requested_at: string
      decided_at: string | null
      created_at: string
    }
    Insert: {
      id?: string
      operation_id?: string | null
      driver_id: string
      reason: string
      detail_reason?: string | null
      decision?: 'pending' | 'continue' | 'stop' | 'cancelled'
      requested_at?: string
      decided_at?: string | null
      created_at?: string
    }
    Update: Partial<Tables['safe_stop_requests']['Insert']>
    Relationships: []
  }
}

export type Database = {
  public: {
    Tables: Tables
    Views: Record<string, never>
    Functions: Record<string, never>
    Enums: {
      user_role: UserRole
      bus_status: BusStatus
      operation_status: OperationStatus
      report_status: ReportStatus
      notification_type: NotificationType
      weekday: Weekday
      semester_type: SemesterType
      operation_log_type: OperationLogType
    }
    CompositeTypes: Record<string, never>
  }
}
