export type LatLngPoint = { lat: number; lng: number }

type CacheEntry = { path: LatLngPoint[]; at: number }

/** 세션 동안 노선별 Directions 결과 재사용 (지도 타일과 무관, 경로 API 절약) */
const memoryCache = new Map<string, CacheEntry>()
const CLIENT_TTL_MS = 24 * 60 * 60_000
const STORAGE_KEY = 'onda-naver-driving-cache-v1'

function pointsKey(points: LatLngPoint[]): string {
  return points.map((p) => `${p.lat.toFixed(5)},${p.lng.toFixed(5)}`).join('|')
}

function readStorage(): Record<string, CacheEntry> {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as Record<string, CacheEntry>
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function writeStorage(key: string, entry: CacheEntry) {
  try {
    const all = readStorage()
    all[key] = entry
    // 용량 보호: 오래된 것부터 정리
    const keys = Object.keys(all)
    if (keys.length > 40) {
      keys
        .sort((a, b) => (all[a]?.at ?? 0) - (all[b]?.at ?? 0))
        .slice(0, keys.length - 40)
        .forEach((k) => delete all[k])
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  } catch {
    /* quota / private mode */
  }
}

function getCached(key: string): LatLngPoint[] | null {
  const mem = memoryCache.get(key)
  if (mem && Date.now() - mem.at < CLIENT_TTL_MS) return mem.path
  const disk = readStorage()[key]
  if (disk?.path?.length && Date.now() - disk.at < CLIENT_TTL_MS) {
    memoryCache.set(key, disk)
    return disk.path
  }
  return null
}

function setCached(key: string, path: LatLngPoint[]) {
  const entry = { path, at: Date.now() }
  memoryCache.set(key, entry)
  writeStorage(key, entry)
}

/**
 * 네이버 자동차 길찾기 경로 (Vite 서버 프록시 → Directions 5).
 * `cacheKey`(보통 route id) 또는 좌표열 기준으로 클라이언트 캐시 — 노선당 1회만 네트워크.
 */
export async function fetchNaverDrivingPath(
  points: LatLngPoint[],
  opts?: { cacheKey?: string },
): Promise<{ path: LatLngPoint[]; error?: string; cached?: boolean }> {
  if (points.length < 2) return { path: [] }
  const key = opts?.cacheKey?.trim() || pointsKey(points)
  const hit = getCached(key)
  if (hit?.length) return { path: hit, cached: true }

  try {
    const res = await fetch('/api/naver/driving', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ points }),
    })
    const data = (await res.json()) as {
      ok?: boolean
      path?: LatLngPoint[]
      message?: string
      cached?: boolean
    }
    if (!res.ok || !data.ok || !data.path?.length) {
      return { path: [], error: data.message || `Directions 실패 (${res.status})` }
    }
    setCached(key, data.path)
    return { path: data.path, cached: Boolean(data.cached) }
  } catch (e) {
    return {
      path: [],
      error: e instanceof Error ? e.message : 'Directions 요청 오류',
    }
  }
}
