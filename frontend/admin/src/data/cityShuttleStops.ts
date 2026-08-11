/** 지도 정류장 핀 / 노선 레이어 (Supabase routes + route_stops + stops 기준) */

export type RouteStopPin = {
  id: string
  name: string
  lat: number
  lng: number
  order: number
  /** 참고용 주소 */
  address?: string
}

export type MapRouteLayer = {
  id: string
  name: string
  /** 핀·노선 색 */
  color: string
  stops: RouteStopPin[]
}

export const CITY_SHUTTLE_STOPS: RouteStopPin[] = [
  { id: "22222222-2222-2222-2222-222222222202", name: "버스관리사무소", lat: 37.22405, lng: 127.18735, order: 1 },
  { id: "22222222-2222-2222-2222-222222222215", name: "상공회의소", lat: 37.2318, lng: 127.1894, order: 2 },
  { id: "22222222-2222-2222-2222-222222222217", name: "진입로(럭스나인 앞)", lat: 37.2362, lng: 127.1915, order: 3 },
  { id: "22222222-2222-2222-2222-222222222218", name: "동부경찰서 중앙지구대", lat: 37.2349, lng: 127.1988, order: 4 },
  { id: "22222222-2222-2222-2222-222222222206", name: "용인CGV", lat: 37.23509, lng: 127.20561, order: 5 },
  { id: "22222222-2222-2222-2222-222222222207", name: "중앙공영주차장", lat: 37.23455, lng: 127.2072, order: 6 },
  { id: "22222222-2222-2222-2222-222222222204", name: "진입로(역북동 주민센터)", lat: 37.23446, lng: 127.1883, order: 7 },
  { id: "22222222-2222-2222-2222-222222222216", name: "이마트", lat: 37.23143, lng: 127.18916, order: 8 },
  { id: "22222222-2222-2222-2222-222222222220", name: "제1공학관", lat: 37.22185, lng: 127.18615, order: 9 },
  { id: "22222222-2222-2222-2222-222222222209", name: "제3공학관", lat: 37.22125, lng: 127.18675, order: 10 },
  { id: "22222222-2222-2222-2222-222222222221", name: "함박관", lat: 37.22135, lng: 127.18555, order: 11 },
  { id: "22222222-2222-2222-2222-222222222222", name: "창조관", lat: 37.22305, lng: 127.18665, order: 12 },
  { id: "22222222-2222-2222-2222-222222222202", name: "버스관리사무소", lat: 37.22405, lng: 127.18735, order: 13 },
]

export const MYONGJI_STATION_SHUTTLE_STOPS: RouteStopPin[] = [
  { id: "22222222-2222-2222-2222-222222222202", name: "버스관리사무소", lat: 37.22405, lng: 127.18735, order: 1 },
  { id: "22222222-2222-2222-2222-222222222215", name: "상공회의소", lat: 37.2318, lng: 127.1894, order: 2 },
  { id: "22222222-2222-2222-2222-222222222217", name: "진입로(럭스나인 앞)", lat: 37.2362, lng: 127.1915, order: 3 },
  { id: "22222222-2222-2222-2222-222222222205", name: "경전철 명지대역", lat: 37.23811, lng: 127.19057, order: 4 },
  { id: "22222222-2222-2222-2222-222222222219", name: "명지대역 사거리 정류장", lat: 37.23755, lng: 127.19185, order: 5 },
  { id: "22222222-2222-2222-2222-222222222204", name: "진입로(역북동 주민센터)", lat: 37.23446, lng: 127.1883, order: 6 },
  { id: "22222222-2222-2222-2222-222222222216", name: "이마트", lat: 37.23143, lng: 127.18916, order: 7 },
  { id: "22222222-2222-2222-2222-222222222208", name: "명진당", lat: 37.22255, lng: 127.18695, order: 8 },
  { id: "22222222-2222-2222-2222-222222222209", name: "제3공학관", lat: 37.22125, lng: 127.18675, order: 9 },
  { id: "22222222-2222-2222-2222-222222222221", name: "함박관", lat: 37.22135, lng: 127.18555, order: 10 },
  { id: "22222222-2222-2222-2222-222222222222", name: "창조관", lat: 37.22305, lng: 127.18665, order: 11 },
  { id: "22222222-2222-2222-2222-222222222202", name: "버스관리사무소", lat: 37.22405, lng: 127.18735, order: 12 },
]

export const GIHEUNG_SHUTTLE_STOPS: RouteStopPin[] = [
  { id: "22222222-2222-2222-2222-222222222211", name: "채플관 앞", lat: 37.22415, lng: 127.18705, order: 1 },
  { id: "22222222-2222-2222-2222-222222222201", name: "기흥역 5번 출구", lat: 37.27597, lng: 127.11669, order: 2 },
  { id: "22222222-2222-2222-2222-222222222211", name: "채플관 앞", lat: 37.22415, lng: 127.18705, order: 3 },
]

export const CITY_SHUTTLE_VACATION_STOPS: RouteStopPin[] = [
  { id: "22222222-2222-2222-2222-222222222210", name: "생활관(명현관)", lat: 37.22015, lng: 127.18515, order: 1 },
  { id: "22222222-2222-2222-2222-222222222221", name: "함박관", lat: 37.22135, lng: 127.18555, order: 2 },
  { id: "22222222-2222-2222-2222-222222222223", name: "정문", lat: 37.22455, lng: 127.18875, order: 3 },
  { id: "22222222-2222-2222-2222-222222222215", name: "상공회의소", lat: 37.2318, lng: 127.1894, order: 4 },
  { id: "22222222-2222-2222-2222-222222222217", name: "진입로(럭스나인 앞)", lat: 37.2362, lng: 127.1915, order: 5 },
  { id: "22222222-2222-2222-2222-222222222218", name: "동부경찰서 중앙지구대", lat: 37.2349, lng: 127.1988, order: 6 },
  { id: "22222222-2222-2222-2222-222222222206", name: "용인CGV", lat: 37.23509, lng: 127.20561, order: 7 },
  { id: "22222222-2222-2222-2222-222222222207", name: "중앙공영주차장", lat: 37.23455, lng: 127.2072, order: 8 },
  { id: "22222222-2222-2222-2222-222222222205", name: "경전철 명지대역", lat: 37.23811, lng: 127.19057, order: 9 },
  { id: "22222222-2222-2222-2222-222222222204", name: "진입로(역북동 주민센터)", lat: 37.23446, lng: 127.1883, order: 10 },
  { id: "22222222-2222-2222-2222-222222222216", name: "이마트", lat: 37.23143, lng: 127.18916, order: 11 },
  { id: "22222222-2222-2222-2222-222222222220", name: "제1공학관", lat: 37.22185, lng: 127.18615, order: 12 },
  { id: "22222222-2222-2222-2222-222222222210", name: "생활관(명현관)", lat: 37.22015, lng: 127.18515, order: 13 },
]

export const CITY_SHUTTLE_ROUTE_NAME = '시내 셔틀'
export const CITY_SHUTTLE_VACATION_ROUTE_NAME = '시내 셔틀 (주말·공휴일·방학)'
export const MYONGJI_STATION_ROUTE_NAME = '명지대역 셔틀'
export const MYONGJI_STATION_AFTER18_ROUTE_NAME = '명지대역 셔틀 (18시 이후)'
export const GIHEUNG_ROUTE_NAME = '기흥역 통학버스'

/**
 * DB에 route_stops가 아직 없어 주간 명지대역 셔틀과 동일 정류장·좌표를 사용.
 * (route_id: 66666666-6666-6666-6666-666666666001)
 */
export const MYONGJI_STATION_AFTER18_STOPS: RouteStopPin[] = MYONGJI_STATION_SHUTTLE_STOPS.map(
  (s) => ({ ...s }),
)

/** 범례·지도 색: 주 → 노 → 초 → 파 → 보 */
export const LIVE_MAP_ROUTES: MapRouteLayer[] = [
  {
    id: 'city',
    name: CITY_SHUTTLE_ROUTE_NAME,
    color: '#f97316', // 주
    stops: CITY_SHUTTLE_STOPS,
  },
  {
    id: 'myongji',
    name: MYONGJI_STATION_ROUTE_NAME,
    color: '#eab308', // 노
    stops: MYONGJI_STATION_SHUTTLE_STOPS,
  },
  {
    id: 'giheung',
    name: GIHEUNG_ROUTE_NAME,
    color: '#22c55e', // 초
    stops: GIHEUNG_SHUTTLE_STOPS,
  },
  {
    id: 'city-vacation',
    name: CITY_SHUTTLE_VACATION_ROUTE_NAME,
    color: '#3b82f6', // 파
    stops: CITY_SHUTTLE_VACATION_STOPS,
  },
  {
    id: 'myongji-after18',
    name: MYONGJI_STATION_AFTER18_ROUTE_NAME,
    color: '#8b5cf6', // 보
    stops: MYONGJI_STATION_AFTER18_STOPS,
  },
]
