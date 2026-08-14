export type LatLngPoint = { lat: number; lng: number }

export type NaverDrivingResult = {
  path: LatLngPoint[]
  distanceMeters?: number
  durationMs?: number
  error?: string
  cached?: boolean
}

type CacheEntry = {
  path: LatLngPoint[]
  distanceMeters?: number
  durationMs?: number
  at: number
}

/** 세션 동안 노선별 Directions 결과 재사용 (지도 타일과 무관, 경로 API 절약) */
const memoryCache = new Map<string, CacheEntry>()
const CLIENT_TTL_MS = 24 * 60 * 60_000
const STORAGE_KEY = 'onda-naver-driving-cache-v2'

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

function getCached(key: string): CacheEntry | null {
  const fresh = (entry: CacheEntry | undefined) =>
    entry?.path?.length && Date.now() - entry.at < CLIENT_TTL_MS ? entry : null
  const mem = fresh(memoryCache.get(key))
  if (mem) return mem
  const disk = fresh(readStorage()[key])
  if (disk) {
    memoryCache.set(key, disk)
    return disk
  }
  return null
}

function setCached(key: string, entry: Omit<CacheEntry, 'at'>) {
  const next = { ...entry, at: Date.now() }
  memoryCache.set(key, next)
  writeStorage(key, next)
}

/**
 * 네이버 자동차 길찾기 경로 (Vite 서버 프록시 → Directions 5).
 * 정류장 좌표열 기준으로 캐시 — 정류장 순서가 바뀌면 다시 조회.
 */
export async function fetchNaverDrivingPath(
  points: LatLngPoint[],
  opts?: { cacheKey?: string },
): Promise<NaverDrivingResult> {
  if (points.length < 2) return { path: [] }
  const key = opts?.cacheKey?.trim() || pointsKey(points)
  const hit = getCached(key)
  if (hit?.path.length) {
    return {
      path: hit.path,
      distanceMeters: hit.distanceMeters,
      durationMs: hit.durationMs,
      cached: true,
    }
  }

  try {
    const res = await fetch('/api/naver/driving', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ points }),
    })
    const data = (await res.json()) as {
      ok?: boolean
      path?: LatLngPoint[]
      distanceMeters?: number
      durationMs?: number
      message?: string
      cached?: boolean
    }
    if (!res.ok || !data.ok || !data.path?.length) {
      return { path: [], error: data.message || `Directions 실패 (${res.status})` }
    }
    setCached(key, {
      path: data.path,
      distanceMeters: data.distanceMeters,
      durationMs: data.durationMs,
    })
    return {
      path: data.path,
      distanceMeters: data.distanceMeters,
      durationMs: data.durationMs,
      cached: Boolean(data.cached),
    }
  } catch (e) {
    return {
      path: [],
      error: e instanceof Error ? e.message : 'Directions 요청 오류',
    }
  }
}

export function metersToKm1(meters: number): number {
  return Math.round(meters / 100) / 10
}
