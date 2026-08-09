# 네이버 지도 연동 (관리자 웹)

## 아키텍처 (비용 0원 방어)
1. **지도 SDK·맵 인스턴스 = 페이지당 1회** (`NaverMap` 마운트 시)
2. **차량 이동 = `Marker.setPosition` 만** — 지도 reload 금지
3. 이후 GPS는 Supabase `vehicle_locations` + Realtime 구독으로 좌표만 밀어 넣으면 됨
4. `DELAYED` / 미수신 좌표는 마커를 **멈춤** (튀는 현상 방지)

## 환경 변수
`frontend/admin/.env.local`

```env
VITE_NAVER_MAP_CLIENT_ID=발급받은_Client_ID
```

키 없으면 기존 `map.png` 폴백 + 안내 배너가 표시됩니다.

## 네이버 클라우드 설정
1. [Naver Cloud Platform](https://www.ncloud.com/) → AI·NAVER API → Maps
2. Application 등록 후 **Maps** 상품 선택 (Dynamic Map)
3. Client ID 발급
4. **Web 서비스 URL**에 추가:
   - 로컬: `http://localhost:5173`
   - 배포: Vercel 도메인 (예: `https://onda-admin.vercel.app`)
5. `.env.local`에 `VITE_NAVER_MAP_CLIENT_ID` 넣고 `npm run dev` 재시작

> 관리자 웹은 **Vite + React** 입니다 (Next.js 아님). 환경 변수는 `VITE_` 접두사가 필요합니다.

## 코드 위치
- `src/lib/naverMaps.ts` — SDK 1회 로드
- `src/components/map/NaverMap.tsx` — 맵 1회 + 마커 setPosition
- 사용: 대시보드, 실시간 운행 관제

## 다음 단계 (Realtime)
기사 앱 → `vehicle_locations` upsert → 관리자에서 Supabase channel 구독 → `vehicles` state만 갱신하면 지도 API 추가 호출 없이 실시간 이동됩니다.
