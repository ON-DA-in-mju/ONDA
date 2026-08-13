import logoSrc from '../../assets/onda-logo.png'

type LogoProps = {
  height?: number
  className?: string
}

/** ON-DA 로고 — 투명 배경 PNG */
export function Logo({ height = 40, className = '' }: LogoProps) {
  return (
    <img
      src={logoSrc}
      alt="ON-DA"
      className={`brand-logo ${className}`.trim()}
      style={{
        height,
        width: 'auto',
        display: 'block',
        objectFit: 'contain',
        background: 'transparent',
      }}
    />
  )
}

export { logoSrc }
