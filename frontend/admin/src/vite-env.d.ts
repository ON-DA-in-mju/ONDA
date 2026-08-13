/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SUPABASE_URL?: string
  readonly VITE_SUPABASE_ANON_KEY?: string
  /** 네이버 클라우드 Maps 클라이언트 ID (ncpKeyId 또는 ncpClientId) */
  readonly VITE_NAVER_MAP_CLIENT_ID?: string
  /** 기본 ncpKeyId. 구 콘솔이면 ncpClientId */
  readonly VITE_NAVER_MAP_KEY_PARAM?: 'ncpKeyId' | 'ncpClientId'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare namespace naver.maps {
  class LatLng {
    constructor(lat: number, lng: number)
  }
  class Map {
    constructor(el: string | HTMLElement, options?: object)
    setCenter(latlng: LatLng): void
    getZoom(): number
    setZoom(zoom: number): void
    fitBounds(bounds: LatLngBounds, margins?: number | number[]): void
  }
  enum ZoomControlStyle {
    LARGE,
    SMALL,
  }
  class LatLngBounds {
    constructor(sw?: LatLng, ne?: LatLng)
    extend(latlng: LatLng): void
    isEmpty(): boolean
  }
  class Marker {
    constructor(options: object)
    setMap(map: Map | null): void
    setPosition(latlng: LatLng): void
    setIcon(icon: object): void
    setTitle(title: string): void
  }
  class InfoWindow {
    constructor(options: object)
    open(map: Map, marker?: Marker): void
    close(): void
    setContent(content: string): void
  }
  class Polyline {
    constructor(options: object)
    setMap(map: Map | null): void
    setPath(path: LatLng[]): void
  }
  namespace Event {
    function addListener(target: object, eventName: string, listener: (...args: unknown[]) => void): object
  }
  enum Position {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    LEFT_CENTER,
    CENTER,
    RIGHT_CENTER,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
  }
}

interface Window {
  naver?: {
    maps: typeof naver.maps
  }
}
