function stripEnv(value) {
  return String(value || '')
    .replace(/[^\x20-\x7E]/g, '')
    .trim()
    .replace(/^['"]+|['"]+$/g, '')
}

function toLngLat(p) {
  return `${p.lng},${p.lat}`
}

function userFacingDirectionsError(raw) {
  if (/401|Authentication Failed/i.test(String(raw))) {
    return 'Directions 인증 실패. NCP에서 Directions 5를 켠 뒤 Client Secret을 확인하세요.'
  }
  return String(raw).slice(0, 180)
}

async function requestOneRoute(clientId, clientSecret, points) {
  const start = toLngLat(points[0])
  const goal = toLngLat(points[points.length - 1])
  const middle = points.slice(1, -1)
  const params = new URLSearchParams({ start, goal, option: 'traoptimal' })
  if (middle.length) params.set('waypoints', middle.map(toLngLat).join('|'))

  const hosts = [
    'https://maps.apigw.ntruss.com/map-direction/v1/driving',
    'https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving',
  ]
  const headers = {
    'x-ncp-apigw-api-key-id': clientId,
    'x-ncp-apigw-api-key': clientSecret,
  }

  let text = ''
  let res = null
  for (const host of hosts) {
    res = await fetch(`${host}?${params}`, { headers })
    text = await res.text()
    if (res.ok) break
  }
  if (!res?.ok) {
    throw new Error(`Directions HTTP ${res?.status}: ${text.slice(0, 200)}`)
  }
  const data = JSON.parse(text)
  if (data.code !== 0) {
    throw new Error(data.message || `Directions code=${data.code}`)
  }
  const trip = data.route?.traoptimal?.[0]
  const rawPath = trip?.path
  if (!rawPath?.length) throw new Error('경로 path가 비어 있습니다')
  return {
    path: rawPath.map(([lng, lat]) => ({ lat, lng })),
    distanceMeters: trip?.summary?.distance ?? 0,
    durationMs: trip?.summary?.duration ?? 0,
  }
}

async function fetchDrivingPath(clientId, clientSecret, points) {
  if (points.length <= 7) {
    return requestOneRoute(clientId, clientSecret, points)
  }
  const out = { path: [], distanceMeters: 0, durationMs: 0 }
  for (let i = 0; i < points.length - 1; i += 6) {
    const chunk = points.slice(i, Math.min(i + 7, points.length))
    if (chunk.length < 2) break
    const part = await requestOneRoute(clientId, clientSecret, chunk)
    if (out.path.length && part.path.length) part.path.shift()
    out.path.push(...part.path)
    out.distanceMeters += part.distanceMeters
    out.durationMs += part.durationMs
  }
  return out
}

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type')
  if (req.method === 'OPTIONS') {
    res.status(204).end()
    return
  }
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, message: 'Method Not Allowed' })
    return
  }

  const clientId = stripEnv(process.env.VITE_NAVER_MAP_CLIENT_ID || process.env.NAVER_MAP_CLIENT_ID || '')
  const clientSecret = stripEnv(process.env.NAVER_MAP_CLIENT_SECRET || '')
  if (!clientId || !clientSecret) {
    res.status(503).json({
      ok: false,
      message: '네이버 Directions 미설정. Vercel에 VITE_NAVER_MAP_CLIENT_ID, NAVER_MAP_CLIENT_SECRET 을 넣으세요.',
    })
    return
  }

  try {
    const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {}
    const points = (body.points ?? []).filter((p) => Number.isFinite(p?.lat) && Number.isFinite(p?.lng))
    if (points.length < 2) {
      res.status(400).json({ ok: false, message: 'points가 2개 이상 필요합니다.' })
      return
    }
    const result = await fetchDrivingPath(clientId, clientSecret, points)
    res.status(200).json({ ok: true, ...result, cached: false })
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Directions 요청 실패'
    res.status(502).json({ ok: false, message: userFacingDirectionsError(message) })
  }
}
