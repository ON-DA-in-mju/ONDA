import { useEffect, useMemo, useRef, useState } from 'react'
import type { LiveVehicle } from '../lib/liveApi'
import type { MapRouteLayer, RouteStopPin } from '../data/cityShuttleStops'
import { fetchNaverDrivingPath } from '../lib/naverDirectionsApi'
import { isNaverMapConfigured, loadNaverMaps } from '../lib/naverMaps'

/** 명지대 자연캠퍼스 근처 기본 중심 */
const DEFAULT_CENTER = { lat: 37.2245, lng: 127.1878 }
const DEFAULT_ZOOM = 15

type StopWithRoute = RouteStopPin & { routeId: string; routeName: string; color: string }

function markerColor(vehicle: LiveVehicle, now = Date.now()): string {
  const stale = vehicle.lat != null && vehicle.updatedAt > 0 && now - vehicle.updatedAt > 90_000
  if (vehicle.gpsKind === 'ok' && !stale) return '#22c55e'
  if (stale || vehicle.gpsKind === 'error') return '#eb4047'
  switch (vehicle.status) {
    case 'in_progress':
      return '#3fb46a'
    case 'stopped':
      return '#266ef4'
    case 'idle':
      return '#fdac38'
    case 'ended':
      return '#8b849c'
    default:
      return '#266ef4'
  }
}

function markerIconHtml(color: string, accent: boolean): string {
  if (accent) {
    return `<div style="position:relative;width:18px;height:18px">
      <div style="position:absolute;inset:0;border-radius:50%;background:${color};opacity:.35;transform:scale(1.7)"></div>
      <div style="position:absolute;inset:0;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 1px 5px rgba(0,0,0,.4)"></div>
    </div>`
  }
  return `<div style="width:16px;height:16px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.35)"></div>`
}

function stopPinHtml(order: number, color: string): string {
  return `<div style="display:flex;flex-direction:column;align-items:center;transform:translateY(-4px)">
    <div style="min-width:22px;height:22px;padding:0 5px;border-radius:11px;background:${color};color:#fff;font:700 11px/22px Pretendard,sans-serif;text-align:center;border:2px solid #fff;box-shadow:0 1px 5px rgba(0,0,0,.35)">${order}</div>
    <div style="width:0;height:0;border-left:5px solid transparent;border-right:5px solid transparent;border-top:7px solid ${color};margin-top:-1px;filter:drop-shadow(0 1px 1px rgba(0,0,0,.25))"></div>
  </div>`
}

function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function isPlausibleKoreaCoord(lat: number, lng: number): boolean {
  return lat >= 33 && lat <= 39.5 && lng >= 124 && lng <= 132
}

type Props = {
  vehicles: LiveVehicle[]
  /** @deprecated routes 사용 권장 */
  stops?: RouteStopPin[]
  /** 노선별 정류장·색 (시내/명지대역 등) */
  routes?: MapRouteLayer[]
  className?: string
  height?: number | string
  compact?: boolean
}

export function LiveVehiclesMap({
  vehicles,
  stops = [],
  routes = [],
  className,
  height,
  compact = false,
}: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<naver.maps.Map | null>(null)
  const markersRef = useRef<Map<string, naver.maps.Marker>>(new Map())
  const stopMarkersRef = useRef<Map<string, naver.maps.Marker>>(new Map())
  const polylinesRef = useRef<Map<string, naver.maps.Polyline>>(new Map())
  const openStopIdRef = useRef<string | null>(null)
  const infoRef = useRef<naver.maps.InfoWindow | null>(null)
  const hasFramedRef = useRef(false)
  const frameSignatureRef = useRef('')
  const [ready, setReady] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [routePaths, setRoutePaths] = useState<Record<string, Array<{ lat: number; lng: number }>>>({})
  const [routeError, setRouteError] = useState<string | null>(null)

  const routeLayers = useMemo((): MapRouteLayer[] => {
    if (routes.length) return routes
    if (stops.length) {
      return [{ id: 'default', name: '정류장', color: '#1e3a5f', stops }]
    }
    return []
  }, [routes, stops])

  const allStopPins = useMemo((): StopWithRoute[] => {
    const out: StopWithRoute[] = []
    for (const layer of routeLayers) {
      for (const s of layer.stops) {
        if (!Number.isFinite(s.lat) || !Number.isFinite(s.lng)) continue
        if (!isPlausibleKoreaCoord(s.lat, s.lng)) continue
        out.push({
          ...s,
          routeId: layer.id,
          routeName: layer.name,
          color: layer.color,
        })
      }
    }
    return out
  }, [routeLayers])

  const positioned = useMemo(
    () =>
      vehicles.filter((v) => {
        if (v.lat == null || v.lng == null) return false
        if (!Number.isFinite(v.lat) || !Number.isFinite(v.lng)) return false
        return isPlausibleKoreaCoord(v.lat, v.lng)
      }),
    [vehicles],
  )

  useEffect(() => {
    let cancelled = false
    const layers = routeLayers.filter((r) => r.stops.length >= 2)
    if (!layers.length) {
      setRoutePaths({})
      setRouteError(null)
      return
    }

    // 노선별 캐시 — 이미 있는 path는 다시 Directions 호출하지 않음
    void Promise.all(
      layers.map(async (layer) => {
        const ordered = [...layer.stops].sort((a, b) => a.order - b.order)
        const result = await fetchNaverDrivingPath(ordered.map((s) => ({ lat: s.lat, lng: s.lng })))
        return { id: layer.id, name: layer.name, ...result }
      }),
    ).then((results) => {
      if (cancelled) return
      const next: Record<string, Array<{ lat: number; lng: number }>> = {}
      let directionsFailed = false
      for (const r of results) {
        if (r.path.length) next[r.id] = r.path
        else if (r.error) directionsFailed = true
      }
      setRoutePaths((prev) => ({ ...prev, ...next }))
      setRouteError(
        directionsFailed
          ? '도로 길찾기는 꺼져 있어 정류장을 직선으로 이었습니다. NCP 앱에서 Directions 5를 켜면 도로 경로가 나옵니다.'
          : null,
      )
    })

    return () => {
      cancelled = true
    }
  }, [routeLayers])

  const runningWithoutGps = useMemo(
    () =>
      vehicles.filter(
        (v) =>
          v.status === 'in_progress' &&
          (v.lat == null || v.lng == null || !isPlausibleKoreaCoord(v.lat, v.lng)),
      ).length,
    [vehicles],
  )

  useEffect(() => {
    let cancelled = false
    if (!isNaverMapConfigured()) {
      setError('네이버 지도 API 키가 없습니다. .env.local에 VITE_NAVER_MAP_CLIENT_ID를 설정하세요.')
      return
    }

    void loadNaverMaps()
      .then((maps) => {
        if (cancelled || !containerRef.current) return
        if (!mapRef.current) {
          const Point = (maps as unknown as { Point: new (x: number, y: number) => object }).Point
          // 네이버 기본 zoomControl(LARGE 슬라이더)은 sticky 헤더·페이지 스크롤과
          // 겹치면 위치가 깨지므로 끄고, 아래에서 커스텀 +/- 로 대체한다.
          mapRef.current = new maps.Map(containerRef.current, {
            center: new maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng),
            zoom: DEFAULT_ZOOM,
            mapTypeControl: false,
            scaleControl: false,
            logoControl: true,
            mapDataControl: false,
            zoomControl: false,
          })
          infoRef.current = new maps.InfoWindow({
            borderWidth: 0,
            backgroundColor: 'transparent',
            disableAnchor: true,
            pixelOffset: Point ? new Point(0, -10) : undefined,
          })
        }
        setReady(true)
        setError(null)
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message || '네이버 지도를 불러오지 못했습니다.')
      })

    return () => {
      cancelled = true
      if (mapRef.current) {
        try {
          mapRef.current.destroy()
        } catch {
          /* ignore */
        }
        mapRef.current = null
      }
      for (const m of markersRef.current.values()) m.setMap(null)
      markersRef.current.clear()
      for (const m of stopMarkersRef.current.values()) m.setMap(null)
      stopMarkersRef.current.clear()
      for (const line of polylinesRef.current.values()) line.setMap(null)
      polylinesRef.current.clear()
      infoRef.current = null
      hasFramedRef.current = false
      setReady(false)
    }
  }, [compact])

  useEffect(() => {
    if (!ready || !mapRef.current || !window.naver?.maps) return
    const maps = window.naver.maps
    const map = mapRef.current
    const markers = markersRef.current
    const stopMarkers = stopMarkersRef.current
    const polylines = polylinesRef.current
    const Point = (maps as unknown as { Point: new (x: number, y: number) => object }).Point
    const seen = new Set<string>()

    for (const v of positioned) {
      const lat = v.lat as number
      const lng = v.lng as number
      const id = v.id
      seen.add(id)
      const color = markerColor(v)
      const gpsLive = v.gpsKind === 'ok'
      const title = `${v.vehicleName || '차량'} · ${v.statusLabel}${gpsLive ? ' · GPS' : ''}`
      const content = `
        <div style="padding:8px 10px;background:#fff;border:1px solid #e5e7eb;border-radius:10px;box-shadow:0 4px 14px rgba(15,23,42,.12);font:12px/1.45 Pretendard,sans-serif;min-width:140px">
          <div style="font-weight:800;color:#111827">${escapeHtml(v.vehicleName || '차량')}</div>
          <div style="color:#4b5563;margin-top:2px">${escapeHtml(v.routeName || '-')}</div>
          <div style="color:#6b7280;margin-top:4px">${escapeHtml(v.driverName || '-')} · ${escapeHtml(v.statusLabel)}</div>
          <div style="color:${gpsLive ? '#16a34a' : '#6b7280'};margin-top:4px;font-weight:600">${gpsLive ? 'GPS 수신 중' : escapeHtml(v.gps)}</div>
        </div>
      `
      const icon = {
        content: markerIconHtml(color, gpsLive),
        anchor: Point ? new Point(gpsLive ? 9 : 8, gpsLive ? 9 : 8) : undefined,
      }
      const position = new maps.LatLng(lat, lng)

      let marker = markers.get(id)
      if (!marker) {
        marker = new maps.Marker({ map, position, title, icon, zIndex: 100 })
        maps.Event.addListener(marker, 'click', () => {
          openStopIdRef.current = null
          const latest = markersRef.current.get(id)
          infoRef.current?.setContent(content)
          infoRef.current?.open(map, latest ?? marker!)
        })
        markers.set(id, marker)
      } else {
        // 지도 reload 없이 좌표만 이동 (Dynamic Map 추가 호출 없음)
        marker.setPosition(position)
        marker.setTitle(title)
        marker.setIcon(icon)
      }
    }

    for (const [id, marker] of [...markers.entries()]) {
      if (!seen.has(id)) {
        marker.setMap(null)
        markers.delete(id)
      }
    }

    // 노선별 길찾기 경로
    const seenRoutes = new Set<string>()
    for (const layer of routeLayers) {
      const driving = routePaths[layer.id]
      const fallback = [...layer.stops]
        .sort((a, b) => a.order - b.order)
        .filter((s) => Number.isFinite(s.lat) && Number.isFinite(s.lng))
        .map((s) => ({ lat: s.lat, lng: s.lng }))
      const pathPts = driving && driving.length >= 2 ? driving : fallback
      if (pathPts.length < 2) continue
      seenRoutes.add(layer.id)
      const path = pathPts.map((p) => new maps.LatLng(p.lat, p.lng))
      let line = polylines.get(layer.id)
      if (!line) {
        line = new maps.Polyline({
          map,
          path,
          strokeColor: layer.color,
          strokeWeight: 5,
          strokeOpacity: 0.78,
          strokeLineCap: 'round',
          strokeLineJoin: 'round',
          zIndex: 40,
        })
        polylines.set(layer.id, line)
      } else {
        line.setPath(path)
        line.setOptions({
          strokeColor: layer.color,
          strokeWeight: 5,
          strokeOpacity: 0.78,
        })
        line.setMap(map)
      }
    }
    for (const [id, line] of [...polylines.entries()]) {
      if (!seenRoutes.has(id)) {
        line.setMap(null)
        polylines.delete(id)
      }
    }

    // 정류장 핀
    const seenStops = new Set<string>()
    for (const stop of allStopPins) {
      const stopKey = `${stop.routeId}:${stop.id}:${stop.order}`
      seenStops.add(stopKey)
      const title = `${stop.routeName} · ${stop.order}. ${stop.name}`
      const content = `
        <div style="padding:8px 10px;background:#fff;border:1px solid #e5e7eb;border-radius:10px;box-shadow:0 4px 14px rgba(15,23,42,.12);font:12px/1.45 Pretendard,sans-serif;min-width:120px">
          <div style="font-size:11px;color:${escapeHtml(stop.color)};font-weight:700">${escapeHtml(stop.routeName)}</div>
          <div style="font-weight:800;color:#111827;margin-top:2px">${escapeHtml(String(stop.order))}. ${escapeHtml(stop.name)}</div>
          ${stop.address ? `<div style="color:#6b7280;margin-top:4px;font-size:11px">${escapeHtml(stop.address)}</div>` : ''}
        </div>
      `
      const icon = {
        content: stopPinHtml(stop.order, stop.color),
        anchor: Point ? new Point(11, 28) : undefined,
      }
      const position = new maps.LatLng(stop.lat, stop.lng)
      let marker = stopMarkers.get(stopKey)
      if (!marker) {
        marker = new maps.Marker({ map, position, title, icon, zIndex: 50 })
        maps.Event.addListener(marker, 'click', () => {
          if (openStopIdRef.current === stopKey) {
            infoRef.current?.close()
            openStopIdRef.current = null
            return
          }
          infoRef.current?.setContent(content)
          infoRef.current?.open(map, marker!)
          openStopIdRef.current = stopKey
        })
        stopMarkers.set(stopKey, marker)
      } else {
        marker.setPosition(position)
        marker.setTitle(title)
        marker.setIcon(icon)
      }
    }
    for (const [id, marker] of [...stopMarkers.entries()]) {
      if (!seenStops.has(id)) {
        marker.setMap(null)
        stopMarkers.delete(id)
        if (openStopIdRef.current === id) {
          infoRef.current?.close()
          openStopIdRef.current = null
        }
      }
    }

    const frameSignature = routeLayers
      .map((r) => r.id)
      .slice()
      .sort()
      .join('|')
    if (frameSignatureRef.current !== frameSignature) {
      frameSignatureRef.current = frameSignature
      hasFramedRef.current = false
    }

    const framePoints = [
      ...positioned.map((v) => ({ lat: v.lat as number, lng: v.lng as number })),
      ...allStopPins.map((s) => ({ lat: s.lat, lng: s.lng })),
    ]
    if (framePoints.length === 0) {
      hasFramedRef.current = false
      return
    }
    if (hasFramedRef.current) return

    if (framePoints.length === 1) {
      map.setCenter(new maps.LatLng(framePoints[0].lat, framePoints[0].lng))
      map.setZoom(14)
    } else {
      const bounds = new maps.LatLngBounds()
      for (const p of framePoints) {
        bounds.extend(new maps.LatLng(p.lat, p.lng))
      }
      map.fitBounds(bounds, compact ? 28 : 56)
    }
    hasFramedRef.current = true
  }, [ready, positioned, allStopPins, routeLayers, routePaths, compact])

  const styleHeight = height ?? (compact ? 220 : 320)

  const bumpZoom = (delta: number) => {
    const map = mapRef.current
    if (!map) return
    try {
      const next = Math.min(21, Math.max(6, (map.getZoom?.() ?? DEFAULT_ZOOM) + delta))
      map.setZoom(next)
    } catch {
      /* ignore */
    }
  }

  if (error) {
    return (
      <div
        className={className}
        style={{
          height: styleHeight,
          display: 'grid',
          placeItems: 'center',
          padding: 16,
          textAlign: 'center',
          background: '#f8fafc',
          border: '1px solid #eef1f6',
          borderRadius: 10,
          color: '#64748b',
          fontSize: 13,
          lineHeight: 1.5,
        }}
      >
        <div>
          <div style={{ fontWeight: 700, color: '#334155', marginBottom: 6 }}>지도를 표시할 수 없습니다</div>
          <div>{error}</div>
          <div style={{ marginTop: 8, fontSize: 12 }}>
            네이버 클라우드에서 Maps 인증키를 발급하고
            <br />
            웹 서비스 URL에 <code>http://localhost:5173</code> 을 등록하세요.
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className={className} style={{ position: 'relative', height: styleHeight, overflow: 'hidden' }}>
      <div ref={containerRef} style={{ width: '100%', height: '100%' }} />
      {!compact && ready ? (
        <div className="live-map-zoom" role="group" aria-label="지도 확대/축소">
          <button type="button" className="live-map-zoom-btn" aria-label="확대" onClick={() => bumpZoom(1)}>
            +
          </button>
          <button type="button" className="live-map-zoom-btn" aria-label="축소" onClick={() => bumpZoom(-1)}>
            −
          </button>
        </div>
      ) : null}
      {ready && positioned.length === 0 ? (
        <div
          style={{
            position: 'absolute',
            left: 12,
            bottom: 12,
            right: allStopPins.length > 0 ? 'auto' : 12,
            maxWidth: allStopPins.length > 0 ? 280 : undefined,
            zIndex: 5,
            background: 'rgba(255,255,255,.94)',
            border: '1px solid #e5e7eb',
            borderRadius: 8,
            padding: '10px 12px',
            fontSize: 12,
            color: '#64748b',
            lineHeight: 1.45,
            boxShadow: '0 2px 8px rgba(15,23,42,.08)',
          }}
        >
          {runningWithoutGps > 0 ? (
            <>
              <div style={{ fontWeight: 700, color: '#b91c1c', marginBottom: 4 }}>
                운행 중 {runningWithoutGps}대 · GPS 좌표 없음
              </div>
              빨간/초록 점은 <b>기사앱이 보낸 위도·경도</b>가 있을 때만 찍힙니다.
            </>
          ) : allStopPins.length > 0 ? (
            <>
              노선 정류장 표시 중 · GPS 차량 대기
              {routeError ? (
                <>
                  <br />
                  <span style={{ color: '#b45309' }}>{routeError}</span>
                </>
              ) : null}
            </>
          ) : (
            <>위치 수신 중인 차량이 없습니다. 기사앱 GPS가 올라오면 마커가 표시됩니다.</>
          )}
        </div>
      ) : null}
    </div>
  )
}
