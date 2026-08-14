/** ONDA Supabase public schema — 실제 DB와 동기화 */

export type UserRole = 'STUDENT' | 'DRIVER' | 'ADMIN'
/** @deprecated user_role 사용. 하위 호환용 alias */
export type AdminRole = UserRole

export type BusStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'
export type OperationStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type ReportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED'
/** reports.source — 학생 제보 / 기사 문의 */
export type ReportSource = 'STUDENT' | 'DRIVER'
/** notices.type — 긴급 / 중요 / 운행 변경 / 일반 */
export type NoticeType = 'URGENT' | 'IMPORTANT' | 'OPERATION_CHANGE' | 'GENERAL'
/** notices.audience 요소 — 학생·기사 다중 선택 */
export type NoticeAudience = 'STUDENT' | 'DRIVER'
/** notices.status — 목록용 게시 상태 */
export type NoticeStatus = 'DRAFT' | 'SCHEDULED' | 'PUBLISHED' | 'ENDED'
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
      /** 운행 중단 종료 시각. 지나면 자동으로 운행 가능 */
      suspended_until: string | null
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
      suspended_until?: string | null
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
      /** 미배정이면 null */
      driver_id: string | null
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
      /** 출발 정류장 (stops.id) */
      origin_stop_id: string | null
      /** 도착 정류장 (stops.id) */
      destination_stop_id: string | null
      expected_end_time: string | null
    }
    Insert: {
      id?: string
      schedule_id: string
      driver_id?: string | null
      bus_id: string
      operation_date: string
      status?: OperationStatus
      started_at?: string | null
      ended_at?: string | null
      created_at?: string | null
      updated_at?: string | null
      external_id?: string | null
      round?: number | null
      origin_stop_id?: string | null
      destination_stop_id?: string | null
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
  /** 운행당 최신 기기 heartbeat — GPS vs 네트워크 구분용 */
  operation_device_status: {
    Row: {
      operation_id: string
      /** 최근 GPS fix 있음 */
      gps_ok: boolean
      /** 기기 위치 서비스 on/off */
      gps_enabled: boolean
      last_location_at: string | null
      last_accuracy: number | null
      /** heartbeat 시각 (네트워크 생존 신호) */
      updated_at: string
    }
    Insert: {
      operation_id: string
      gps_ok?: boolean
      gps_enabled?: boolean
      last_location_at?: string | null
      last_accuracy?: number | null
      updated_at?: string
    }
    Update: Partial<Omit<Tables['operation_device_status']['Insert'], 'operation_id'>>
    Relationships: []
  }
  /** 운행당 최신 정류장 진행 — 화면 나갔다 와도 복원 */
  operation_stop_progress: {
    Row: {
      operation_id: string
      last_arrived_stop_id: string | null
      last_passed_stop_id: string | null
      last_arrived_index: number
      last_passed_index: number
      updated_at: string
    }
    Insert: {
      operation_id: string
      last_arrived_stop_id?: string | null
      last_passed_stop_id?: string | null
      last_arrived_index?: number
      last_passed_index?: number
      updated_at?: string
    }
    Update: Partial<Omit<Tables['operation_stop_progress']['Insert'], 'operation_id'>>
    Relationships: []
  }
  notices: {
    Row: {
      id: string
      title: string
      content: string
      author_id: string | null
      /** URGENT | IMPORTANT | OPERATION_CHANGE | GENERAL */
      type: NoticeType
      /** 대상: STUDENT / DRIVER (둘 다 가능) */
      audience: NoticeAudience[]
      /** 게시 시작. 상시 게시면 null */
      starts_at: string | null
      /** 게시 종료. 상시 게시면 null */
      ends_at: string | null
      /** 푸시 동시 발송 여부 */
      is_push: boolean
      /** 고유 조회수 (사용자당 1회) */
      view_count: number
      /** DRAFT | SCHEDULED | PUBLISHED | ENDED */
      status: NoticeStatus
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id?: string
      title: string
      content: string
      author_id?: string | null
      type?: NoticeType
      audience?: NoticeAudience[]
      starts_at?: string | null
      ends_at?: string | null
      is_push?: boolean
      view_count?: number
      status?: NoticeStatus
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
      /** STUDENT = 학생 제보, DRIVER = 기사 문의 */
      source: ReportSource
      /** 문의 유형 (account, assignment, gps …) */
      category: string | null
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id?: string
      user_id: string
      title: string
      content: string
      status?: ReportStatus
      source?: ReportSource
      category?: string | null
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
      bus_id: string | null
      created_at: string
    }
    Insert: {
      id?: string
      name: string
      plate: string
      status?: string
      mileage?: string | null
      next_maintenance?: string | null
      bus_id?: string | null
      created_at?: string
    }
    Update: Partial<Tables['vehicles']['Insert']>
    Relationships: []
  }
  maintenances: {
    Row: {
      id: string
      bus_id: string | null
      vehicle_id: string | null
      maintained_at: string
      /** 레거시 번호판. 정규 키는 bus_id */
      plate: string | null
      item: string
      type: string
      mechanic: string | null
      cost: number
      status: string
      memo: string | null
      created_at: string
      updated_at: string | null
    }
    Insert: {
      id?: string
      bus_id?: string | null
      vehicle_id?: string | null
      maintained_at: string
      plate?: string | null
      item: string
      type?: string
      mechanic?: string | null
      cost?: number
      status?: string
      memo?: string | null
      created_at?: string
      updated_at?: string | null
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
      actor_id: string | null
      ip: string | null
      target: string | null
      result: string
    }
    Insert: {
      id?: string
      logged_at?: string
      type: string
      action: string
      actor_id?: string | null
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
    Functions: {
      increment_notice_view: {
        Args: { p_notice_id: string }
        Returns: number
      }
    }
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
