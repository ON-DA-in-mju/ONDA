export type UserRole = 'STUDENT' | 'DRIVER' | 'ADMIN'
export type AdminRole = UserRole

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
      start_location: string | null
      end_location: string | null
      created_at: string
      updated_at: string
    }
    Insert: {
      id?: string
      route_name: string
      direction?: string | null
      description?: string | null
      is_active?: boolean
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
    Relationships: []
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
    Relationships: []
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
      status: string
      created_at: string | null
      updated_at: string | null
    }
    Insert: {
      id?: string
      user_id: string
      title: string
      content: string
      status?: string
    }
    Update: Partial<Omit<Tables['reports']['Insert'], 'user_id'>>
    Relationships: []
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
    Relationships: []
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
    }
    CompositeTypes: Record<string, never>
  }
}
