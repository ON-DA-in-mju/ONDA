const CLIENT_ID = (import.meta.env.VITE_NAVER_MAP_CLIENT_ID || '').trim()
const KEY_PARAM = (import.meta.env.VITE_NAVER_MAP_KEY_PARAM || 'ncpKeyId').trim()

let loadPromise: Promise<typeof naver.maps> | null = null

export function isNaverMapConfigured(): boolean {
  return CLIENT_ID.length > 0 && !CLIENT_ID.includes('YOUR_')
}

/** DELAYED / 미수신 등이면 마커 좌표를 고정한다. */
export function shouldFreezeMarker(gpsStatus?: string): boolean {
  if (!gpsStatus) return false
  const s = gpsStatus.trim().toUpperCase()
  return (
    s.includes('DELAY') ||
    s.includes('STALE') ||
    s.includes('NONE') ||
    s.includes('ERROR') ||
    s.includes('미수신') ||
    s.includes('지연')
  )
}

export function loadNaverMaps(): Promise<typeof naver.maps> {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('window unavailable'))
  }
  if (window.naver?.maps) {
    return Promise.resolve(window.naver.maps)
  }
  if (!isNaverMapConfigured()) {
    return Promise.reject(new Error('VITE_NAVER_MAP_CLIENT_ID 미설정'))
  }
  if (loadPromise) return loadPromise

  loadPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-onda-naver-maps]')
    if (existing) {
      existing.addEventListener('load', () => {
        if (window.naver?.maps) resolve(window.naver.maps)
        else reject(new Error('네이버 지도 로드 실패'))
      })
      existing.addEventListener('error', () => reject(new Error('네이버 지도 스크립트 오류')))
      return
    }

    const script = document.createElement('script')
    script.dataset.ondaNaverMaps = '1'
    script.async = true
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?${KEY_PARAM}=${encodeURIComponent(CLIENT_ID)}`
    script.onload = () => {
      if (window.naver?.maps) resolve(window.naver.maps)
      else reject(new Error('네이버 지도 객체를 찾을 수 없습니다'))
    }
    script.onerror = () => {
      loadPromise = null
      reject(new Error('네이버 지도 스크립트 로드 실패'))
    }
    document.head.appendChild(script)
  })

  return loadPromise
}
