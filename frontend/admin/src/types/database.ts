/** ONDA Supabase public schema — 3NF + Relationships */

export type UserRole = 'STUDENT' | 'DRIVER' | 'ADMIN'
export type AdminRole = UserRole
export type Weekday = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN'
export type SemesterType = 'SEMESTER' | 'VACATION'
export type NoticeAudience = 'STUDENT' | 'DRIVER' | 'ADMIN'

export type Json = string | number | boolean | null | { [key: string]: Json | undefined } | Json[]

type Tables = {
  users: {
    Row: {
      id: string
      name: string
      phone: string | null
      role: UserRole
      student_no: string | null
      profile_image: string | null
      email: string | null
      login_id: string | null
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id: string
      name: string
      phone?: string | null
      role: UserRole
      student_no?: string | null
      profile_image?: string | null
      email?: string | null
      login_id?: string | null
    }
    Update: Partial<Omit<Tables['users']['Insert'], 'id'>>
    Relationships: []
  }
  buses: {
    Row: {
      id: string
      bus_name: string
      vehicle_number: string
      capacity: number
      status: string
      created_at: string
      updated_at: string
    }
    Insert: {
      id?: string
      bus_name: string
      vehicle_number: string
      capacity?: number
      status?: string
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
    }
    Insert: {
      id?: string
      route_name: string
      direction?: string | null
      description?: string | null
      is_active?: boolean
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
    }
    Update: Partial<Tables['route_stops']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'route_stops_route_id_fkey'
        columns: ['route_id']
        isOneToOne: false
        referencedRelation: 'routes'
        referencedColumns: ['id']
      },
      {
        foreignKeyName: 'route_stops_stop_id_fkey'
        columns: ['stop_id']
        isOneToOne: false
        referencedRelation: 'stops'
        referencedColumns: ['id']
      },
    ]
  }
  schedules: {
    Row: {
      id: string
      route_id: string
      departure_time: string
      weekday: string
      semester: string
      created_at: string
      updated_at: string
    }
    Insert: {
      id?: string
      route_id: string
      departure_time: string
      weekday: string
      semester: string
    }
    Update: Partial<Tables['schedules']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'schedules_route_id_fkey'
        columns: ['route_id']
        isOneToOne: false
        referencedRelation: 'routes'
        referencedColumns: ['id']
      },
    ]
  }
  operations: {
    Row: {
      id: string
      schedule_id: string
      driver_id: string
      bus_id: string
      operation_date: string
      status: string
      started_at: string | null
      ended_at: string | null
      external_id: string | null
      round: number | null
      origin_stop_id: string | null
      destination_stop_id: string | null
      expected_end_time: string | null
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id?: string
      schedule_id: string
      driver_id: string
      bus_id: string
      operation_date: string
      status?: string
      started_at?: string | null
      ended_at?: string | null
      external_id?: string | null
      round?: number | null
      origin_stop_id?: string | null
      destination_stop_id?: string | null
      expected_end_time?: string | null
    }
    Update: Partial<Tables['operations']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'operations_schedule_id_fkey'
        columns: ['schedule_id']
        isOneToOne: false
        referencedRelation: 'schedules'
        referencedColumns: ['id']
      },
      {
        foreignKeyName: 'operations_driver_id_fkey'
        columns: ['driver_id']
        isOneToOne: false
        referencedRelation: 'users'
        referencedColumns: ['id']
      },
      {
        foreignKeyName: 'operations_bus_id_fkey'
        columns: ['bus_id']
        isOneToOne: false
        referencedRelation: 'buses'
        referencedColumns: ['id']
      },
      {
        foreignKeyName: 'operations_origin_stop_id_fkey'
        columns: ['origin_stop_id']
        isOneToOne: false
        referencedRelation: 'stops'
        referencedColumns: ['id']
      },
      {
        foreignKeyName: 'operations_destination_stop_id_fkey'
        columns: ['destination_stop_id']
        isOneToOne: false
        referencedRelation: 'stops'
        referencedColumns: ['id']
      },
    ]
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
    }
    Update: Partial<Tables['vehicle_locations']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'vehicle_locations_operation_id_fkey'
        columns: ['operation_id']
        isOneToOne: false
        referencedRelation: 'operations'
        referencedColumns: ['id']
      },
    ]
  }
  notices: {
    Row: {
      id: string
      title: string
      content: string
      author_id: string | null
      type: string | null
      starts_at: string | null
      ends_at: string | null
      is_push: boolean | null
      status: string | null
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id?: string
      title: string
      content: string
      author_id?: string | null
      type?: string | null
      starts_at?: string | null
      ends_at?: string | null
      is_push?: boolean | null
      status?: string | null
    }
    Update: Partial<Tables['notices']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'notices_author_id_fkey'
        columns: ['author_id']
        isOneToOne: false
        referencedRelation: 'users'
        referencedColumns: ['id']
      },
    ]
  }
  notice_audiences: {
    Row: {
      notice_id: string
      audience: NoticeAudience
    }
    Insert: {
      notice_id: string
      audience: NoticeAudience
    }
    Update: Partial<Tables['notice_audiences']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'notice_audiences_notice_id_fkey'
        columns: ['notice_id']
        isOneToOne: false
        referencedRelation: 'notices'
        referencedColumns: ['id']
      },
    ]
  }
  reports: {
    Row: {
      id: string
      user_id: string
      title: string
      content: string
      status: string
      source: string | null
      operation_id: string | null
      route_id: string | null
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id?: string
      user_id: string
      title: string
      content: string
      status?: string
      source?: string | null
      operation_id?: string | null
      route_id?: string | null
    }
    Update: Partial<Omit<Tables['reports']['Insert'], 'user_id'>>
    Relationships: [
      {
        foreignKeyName: 'reports_user_id_fkey'
        columns: ['user_id']
        isOneToOne: false
        referencedRelation: 'users'
        referencedColumns: ['id']
      },
      {
        foreignKeyName: 'reports_operation_id_fkey'
        columns: ['operation_id']
        isOneToOne: false
        referencedRelation: 'operations'
        referencedColumns: ['id']
      },
      {
        foreignKeyName: 'reports_route_id_fkey'
        columns: ['route_id']
        isOneToOne: false
        referencedRelation: 'routes'
        referencedColumns: ['id']
      },
    ]
  }
  notifications: {
    Row: {
      id: string
      user_id: string
      title: string
      message: string
      type: string
      is_read: boolean | null
      created_at: string | null
    }
    Insert: {
      id?: string
      user_id: string
      title: string
      message: string
      type: string
      is_read?: boolean
    }
    Update: Partial<Tables['notifications']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'notifications_user_id_fkey'
        columns: ['user_id']
        isOneToOne: false
        referencedRelation: 'users'
        referencedColumns: ['id']
      },
    ]
  }
  operation_logs: {
    Row: {
      id: string
      operation_id: string
      log_message: string
      created_at: string | null
      event_type: string
    }
    Insert: {
      id?: string
      operation_id: string
      log_message: string
      event_type: string
    }
    Update: Partial<Tables['operation_logs']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'operation_logs_operation_id_fkey'
        columns: ['operation_id']
        isOneToOne: false
        referencedRelation: 'operations'
        referencedColumns: ['id']
      },
    ]
  }
  /** plate는 buses.vehicle_number 조인 (v_vehicles) */
  vehicles: {
    Row: {
      id: string
      name: string | null
      status: string | null
      mileage: string | null
      next_maintenance: string | null
      bus_id: string | null
      created_at: string | null
    }
    Insert: {
      id?: string
      name?: string | null
      status?: string | null
      mileage?: string | null
      next_maintenance?: string | null
      bus_id?: string | null
    }
    Update: Partial<Tables['vehicles']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'vehicles_bus_id_fkey'
        columns: ['bus_id']
        isOneToOne: true
        referencedRelation: 'buses'
        referencedColumns: ['id']
      },
    ]
  }
  maintenances: {
    Row: {
      id: string
      bus_id: string | null
      type: string | null
      status: string | null
      cost: number | null
      created_at: string | null
    }
    Insert: {
      id?: string
      bus_id?: string | null
      type?: string | null
      status?: string | null
      cost?: number | null
    }
    Update: Partial<Tables['maintenances']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'maintenances_bus_id_fkey'
        columns: ['bus_id']
        isOneToOne: false
        referencedRelation: 'buses'
        referencedColumns: ['id']
      },
    ]
  }
  system_logs: {
    Row: {
      id: string
      actor_id: string | null
      action: string | null
      target: string | null
    }
    Insert: {
      id?: string
      actor_id?: string | null
      action?: string | null
      target?: string | null
    }
    Update: Partial<Tables['system_logs']['Insert']>
    Relationships: [
      {
        foreignKeyName: 'system_logs_actor_id_fkey'
        columns: ['actor_id']
        isOneToOne: false
        referencedRelation: 'users'
        referencedColumns: ['id']
      },
    ]
  }
}

export type Database = {
  public: {
    Tables: Tables
    Views: {
      v_operations: {
        Row: Tables['operations']['Row'] & {
          origin: string | null
          destination: string | null
        }
      }
      v_routes: {
        Row: Tables['routes']['Row'] & {
          start_location: string | null
          end_location: string | null
        }
      }
      v_notices: {
        Row: Tables['notices']['Row'] & {
          audience: NoticeAudience[]
        }
      }
      v_vehicles: {
        Row: Tables['vehicles']['Row'] & {
          plate: string | null
          bus_name: string | null
        }
      }
      v_maintenances: {
        Row: Tables['maintenances']['Row'] & {
          plate: string | null
          bus_name: string | null
        }
      }
      v_system_logs: {
        Row: Tables['system_logs']['Row'] & {
          actor: string | null
        }
      }
    }
    Functions: Record<string, never>
    Enums: {
      user_role: UserRole
    }
    CompositeTypes: Record<string, never>
  }
}
