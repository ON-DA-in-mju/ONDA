import { useEffect, useRef, useState } from 'react'
import mapFallback from '../../assets/map.png'
import { isNaverMapConfigured, loadNaverMaps, shouldFreezeMarker } from '../../lib/naverMaps'
import type { NaverMapInstance, NaverMarkerInstance } from '../../types/naver-maps'
import '../../styles/naver-map.css'

export type MapVehicle = {
  id: string
  label: string
  subLabel?: string
  lat: number
  lng: number
  /** green | blue | orange | red | gray | purple */
  tone?: string
  /** DELAYED / 미수신 등이면 좌표 갱신 안 함 */
  gpsStatus?: string
}

type NaverMapProps = {
  vehicles: MapVehicle[]
  className?: string
  /** 명지대 자연캠 인근 기본 중심 */
  center?: { lat: number; lng: number }
  zoom?: number
  showFallbackPins?: boolean
}

const DEFAULT_CENTER = { lat: 37.2219, lng: 127.1888 }

function markerHtml(v: MapVehicle, frozen: boolean) {
  const tone = v.tone ?? 'green'
  return `
    <div class="nm-pin nm-pin-${tone}${frozen ? ' nm-pin-frozen' : ''}">
      <strong>${v.label}</strong>
      ${v.subLabel ? `<span>${v.subLabel}</span>` : ''}
      ${frozen ? '<em>미수신</em>' : ''}
    </div>
  `
}

/**
 * 네이버 지도: SDK·맵 인스턴스는 1회만 생성.
 * vehicles 변경 시 Marker.setPosition 만 호출 (지도 API 재호출 없음).
 */
export function NaverMap({
  vehicles,
  className,
  center = DEFAULT_CENTER,
  zoom = 15,
}: NaverMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<NaverMapInstance | null>(null)
  const markersRef = useRef<Map<string, NaverMarkerInstance>>(new Map())
  const lastGoodPosRef = useRef<Map<string, { lat: number; lng: number }>>(new Map())
  const [ready, setReady] = useState(false)
  const [error, setError] = useState('')
  const configured = isNaverMapConfigured()

  // 맵 1회 초기화
  useEffect(() => {
    if (!configured || !containerRef.current) return
    let cancelled = false

    void loadNaverMaps()
      .then(() => {
        if (cancelled || !containerRef.current || !window.naver?.maps) return
        const maps = window.naver.maps
        const map = new maps.Map(containerRef.current, {
          center: new maps.LatLng(center.lat, center.lng),
          zoom,
          minZoom: 12,
          zoomControl: true,
          zoomControlOptions: { position: maps.Position.TOP_RIGHT },
        })
        mapRef.current = map
        setReady(true)
        setError('')
      })
      .catch((e: Error) => {
        setError(e.message || '지도 로드 실패')
        setReady(false)
      })

    return () => {
      cancelled = true
      markersRef.current.forEach((m) => m.setMap(null))
      markersRef.current.clear()
      mapRef.current = null
    }
    // center/zoom은 최초 마운트 기준 (의도적으로 의존성 최소화 = 맵 재생성 방지)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [configured])

  // 마커만 갱신 (setPosition) — 지도 reload 없음
  useEffect(() => {
    if (!ready || !mapRef.current || !window.naver?.maps) return
    const maps = window.naver.maps
    const map = mapRef.current
    const alive = new Set(vehicles.map((v) => v.id))

    // 제거된 차량 마커 정리
    markersRef.current.forEach((marker, id) => {
      if (!alive.has(id)) {
        marker.setMap(null)
        markersRef.current.delete(id)
        lastGoodPosRef.current.delete(id)
      }
    })

    vehicles.forEach((v) => {
      const frozen = shouldFreezeMarker(v.gpsStatus) || v.tone === 'red'
      const last = lastGoodPosRef.current.get(v.id)
      const lat = frozen && last ? last.lat : v.lat
      const lng = frozen && last ? last.lng : v.lng
      if (!frozen) lastGoodPosRef.current.set(v.id, { lat: v.lat, lng: v.lng })

      const pos = new maps.LatLng(lat, lng)
      const existing = markersRef.current.get(v.id)

      if (existing) {
        if (!frozen) existing.setPosition(pos)
        existing.setIcon({
          content: markerHtml(v, frozen),
          anchor: new maps.Point(46, 48),
        })
        return
      }

      const marker = new maps.Marker({
        position: pos,
        map,
        title: v.label,
        icon: {
          content: markerHtml(v, frozen),
          anchor: new maps.Point(46, 48),
        },
        zIndex: frozen ? 20 : 10,
      })
      markersRef.current.set(v.id, marker)
      if (!frozen) lastGoodPosRef.current.set(v.id, { lat: v.lat, lng: v.lng })
    })
  }, [vehicles, ready])

  if (!configured || error) {
    return (
      <div className={`nm-wrap ${className ?? ''}`}>
        <img className="nm-fallback-img" src={mapFallback} alt="지도 미리보기" />
        <div className="nm-fallback-banner">
          {!configured
            ? '네이버 지도 키 미설정 · .env.local 에 VITE_NAVER_MAP_CLIENT_ID 추가 후 새로고침'
            : `지도 로드 실패 · ${error}`}
        </div>
      </div>
    )
  }

  return (
    <div className={`nm-wrap ${className ?? ''}`}>
      <div ref={containerRef} className="nm-canvas" />
      {!ready ? <div className="nm-loading">지도 불러오는 중…</div> : null}
    </div>
  )
}
