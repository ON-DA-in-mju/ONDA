/**
 * 네이버 Cloud Platform Maps — Web Dynamic Map
 * - 지도 SDK는 페이지당 1회만 로드
 * - 차량 이동은 Marker.setPosition 만 갱신 (지도 재호출 금지)
 */

const SCRIPT_ID = 'naver-maps-sdk'

export function getNaverMapClientId(): string {
  return (import.meta.env.VITE_NAVER_MAP_CLIENT_ID as string | undefined)?.trim() ?? ''
}

export function isNaverMapConfigured(): boolean {
  return Boolean(getNaverMapClientId())
}

let loadingPromise: Promise<void> | null = null

export function loadNaverMaps(): Promise<void> {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('window unavailable'))
  }
  if (window.naver?.maps) {
    return Promise.resolve()
  }
  if (loadingPromise) return loadingPromise

  const clientId = getNaverMapClientId()
  if (!clientId) {
    return Promise.reject(new Error('VITE_NAVER_MAP_CLIENT_ID 미설정'))
  }

  loadingPromise = new Promise((resolve, reject) => {
    const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null
    if (existing) {
      existing.addEventListener('load', () => resolve(), { once: true })
      existing.addEventListener('error', () => reject(new Error('네이버 지도 SDK 로드 실패')), { once: true })
      if (window.naver?.maps) resolve()
      return
    }

    const script = document.createElement('script')
    script.id = SCRIPT_ID
    script.async = true
    // ncpKeyId: 신형 NCP Maps 키 / ncpClientId: 구형 호환
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(clientId)}`
    script.onload = () => {
      if (window.naver?.maps) resolve()
      else reject(new Error('네이버 지도 객체를 찾을 수 없습니다'))
    }
    script.onerror = () => {
      loadingPromise = null
      reject(new Error('네이버 지도 SDK 로드 실패 (키·도메인 허용 확인)'))
    }
    document.head.appendChild(script)
  })

  return loadingPromise
}

/** 비정상/지연 GPS는 마커를 움직이지 않음 */
export function shouldFreezeMarker(status?: string): boolean {
  if (!status) return false
  const s = status.toUpperCase()
  return (
    s.includes('DELAY') ||
    s.includes('미수신') ||
    s.includes('끊') ||
    s.includes('OFFLINE') ||
    s.includes('ERROR')
  )
}
