export type AdminRole = 'ADMIN' | 'SCHOOL_ADMIN' | 'COMPANY_ADMIN'

export type Json = string | number | boolean | null | { [key: string]: Json | undefined } | Json[]

export type Database = {
  public: {
    Tables: {
      profiles: {
        Row: {
          id: string
          email: string
          name: string
          role: AdminRole
          phone: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          id: string
          email: string
          name: string
          role?: AdminRole
          phone?: string | null
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          email?: string
          name?: string
          role?: AdminRole
          phone?: string | null
          updated_at?: string
        }
        Relationships: []
      }
      notices: {
        Row: {
          id: string
          type: string
          title: string
          body: string
          target: string
          status: string
          views: number
          starts_at: string | null
          ends_at: string | null
          push: boolean
          created_by: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          type: string
          title: string
          body: string
          target?: string
          status?: string
          views?: number
          starts_at?: string | null
          ends_at?: string | null
          push?: boolean
          created_by?: string | null
        }
        Update: Partial<Database['public']['Tables']['notices']['Insert']>
        Relationships: []
      }
      reports: {
        Row: {
          id: string
          type: string
          target: string
          body: string
          status: string
          likes: number
          memo: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          type: string
          target: string
          body: string
          status?: string
          likes?: number
          memo?: string | null
        }
        Update: Partial<Database['public']['Tables']['reports']['Insert']>
        Relationships: []
      }
      routes: {
        Row: {
          id: string
          name: string
          type: string
          status: string
          days: string
          hours: string
          buses: string
          description: string | null
          created_at: string
        }
        Insert: {
          id?: string
          name: string
          type: string
          status?: string
          days?: string
          hours?: string
          buses?: string
          description?: string | null
        }
        Update: Partial<Database['public']['Tables']['routes']['Insert']>
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
        }
        Update: Partial<Database['public']['Tables']['vehicles']['Insert']>
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
        }
        Update: Partial<Database['public']['Tables']['maintenances']['Insert']>
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
        Update: Partial<Database['public']['Tables']['system_logs']['Insert']>
        Relationships: []
      }
    }
    Views: Record<string, never>
    Functions: Record<string, never>
    Enums: {
      admin_role: AdminRole
    }
    CompositeTypes: Record<string, never>
  }
}
