import logoSrc from '../../assets/onda-logo.png'

type LogoProps = {
  height?: number
  className?: string
}

/** 투명 배경 ON-DA 로고 — 검정 칩/배경 없이 이미지만 표시 */
export function Logo({ height = 48, className = '' }: LogoProps) {
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
