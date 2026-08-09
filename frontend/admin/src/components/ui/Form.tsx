import { useState, type InputHTMLAttributes, type ReactNode } from 'react'
import { Eye, EyeOff } from 'lucide-react'

type FieldProps = {
  label: string
  required?: boolean
  hint?: string
  children: ReactNode
}

export function Field({ label, required, hint, children }: FieldProps) {
  return (
    <div className="field">
      <label>
        {label}
        {required ? <span className="req">*</span> : null}
      </label>
      {children}
      {hint ? <span className="field-hint">{hint}</span> : null}
    </div>
  )
}

type IconInputProps = InputHTMLAttributes<HTMLInputElement> & {
  leftIcon?: ReactNode
  rightSlot?: ReactNode
}

export function IconInput({ leftIcon, rightSlot, className = '', ...props }: IconInputProps) {
  return (
    <div className={`input-wrap ${rightSlot ? 'has-right' : ''}`}>
      {leftIcon ? <span className="icon-left">{leftIcon}</span> : null}
      <input className={`input ${className}`} {...props} />
      {rightSlot ? <span className="icon-right">{rightSlot}</span> : null}
    </div>
  )
}

export function PasswordInput(props: Omit<IconInputProps, 'type' | 'rightSlot'>) {
  const [show, setShow] = useState(false)
  return (
    <IconInput
      {...props}
      type={show ? 'text' : 'password'}
      rightSlot={
        <button
          type="button"
          aria-label={show ? '비밀번호 숨기기' : '비밀번호 보기'}
          onClick={() => setShow((v) => !v)}
        >
          {show ? <EyeOff size={16} /> : <Eye size={16} />}
        </button>
      }
    />
  )
}

export function StatusBadge({
  tone,
  children,
}: {
  tone: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'gray'
  children: ReactNode
}) {
  return <span className={`badge badge-${tone}`}>{children}</span>
}

export function StatCard({
  title,
  value,
  unit,
  delta,
  icon,
  color,
}: {
  title: string
  value: string | number
  unit?: string
  delta?: string
  icon: ReactNode
  color: string
}) {
  return (
    <div className="card card-pad" style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
      <div
        style={{
          width: 42,
          height: 42,
          borderRadius: 999,
          background: color,
          color: '#fff',
          display: 'grid',
          placeItems: 'center',
          flexShrink: 0,
        }}
      >
        {icon}
      </div>
      <div>
        <div style={{ fontSize: 12, color: '#4b5563', fontWeight: 600 }}>{title}</div>
        <div style={{ fontSize: 20, fontWeight: 800, lineHeight: 1.2 }}>
          {value}
          {unit ? <span style={{ fontSize: 12, fontWeight: 600 }}> {unit}</span> : null}
        </div>
        {delta ? (
          <div style={{ fontSize: 11, color: '#8b92a4', marginTop: 2 }}>{delta}</div>
        ) : null}
      </div>
    </div>
  )
}
