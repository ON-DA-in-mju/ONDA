/** 네이버 지도 Maps JS API 최소 타입 (사용 부분만) */
export type NaverLatLng = {
  lat: () => number
  lng: () => number
}

export type NaverMapInstance = {
  setCenter: (latlng: NaverLatLng) => void
  setZoom: (level: number, useEffect?: boolean) => void
  getZoom: () => number
  destroy?: () => void
}

export type NaverMarkerInstance = {
  setPosition: (latlng: NaverLatLng) => void
  setMap: (map: NaverMapInstance | null) => void
  setIcon: (icon: unknown) => void
  getPosition: () => NaverLatLng
}

export type NaverMapsNamespace = {
  Map: new (
    el: HTMLElement,
    options: {
      center: NaverLatLng
      zoom: number
      minZoom?: number
      zoomControl?: boolean
      zoomControlOptions?: { position: unknown }
    },
  ) => NaverMapInstance
  LatLng: new (lat: number, lng: number) => NaverLatLng
  Marker: new (options: {
    position: NaverLatLng
    map?: NaverMapInstance | null
    title?: string
    icon?: unknown
    zIndex?: number
  }) => NaverMarkerInstance
  Point: new (x: number, y: number) => unknown
  Size: new (w: number, h: number) => unknown
  Position: {
    TOP_LEFT: unknown
    TOP_RIGHT: unknown
    LEFT_CENTER: unknown
    RIGHT_CENTER: unknown
    BOTTOM_LEFT: unknown
    BOTTOM_RIGHT: unknown
  }
  Event: {
    addListener: (target: unknown, eventName: string, listener: (...args: unknown[]) => void) => void
  }
}

declare global {
  interface Window {
    naver?: {
      maps: NaverMapsNamespace
    }
  }
}

export {}
