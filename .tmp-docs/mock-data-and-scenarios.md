# ON-DA 프론트 mock 데이터 · 시나리오 인수인계

> 대상: 백엔드(Supabase) 담당  
> 기준 코드: `Bus/frontend/admin`, `Bus/frontend/driver`  
> 목적: 현재 로컬 mock / Vite 인메모리 API에 있는 **계정·배차·상태·플로우**를 서버 seed·RPC 설계에 맞게 옮기기 위한 목록

---

## 0. 한눈에 보기

| 구분 | 저장 방식 | 비고 |
|------|-----------|------|
| 관리자 UI 목록(대시보드/공지/제보 등) | `frontend/admin/src/data/mock.ts` | 화면 전용 정적 mock |
| 배차·실시간·안전정차·로그인기록 | Vite 인메모리 (`vite-*-plugin.ts`, `vite-dev-store.ts`) | 관리자 ↔ 기사 앱 로컬 연동 |
| 기사 앱 계정·오늘 운행 | `MockUsers.kt`, `MockTodayOperations.kt` 등 | 로컬 API 없을 때 fallback |
| 관리자 로그인 | Supabase 있으면 Auth, 없으면 데모 통과 | `.env.local` 유무 |

---

## 1. 계정 (시연용)

### 1.1 기사 앱 (`MockUsers.kt`)

| id | 비밀번호 | 이름 | 조직 |
|----|----------|------|------|
| `user01` | `1234` | 박사용 | 명지 셔틀 운영팀 |
| `user02` | `1234` | 최사용 | 명지 셔틀 운영팀 |
| `user03` | `1234` | 정사용 | 명지 셔틀 운영팀 |
| `user04` | `1234` | 한사용 | 명지 셔틀 운영팀 |
| `user05` | `1234` | 임사용 | 명지 셔틀 운영팀 |

- 관리자 웹 `DRIVER_OPTIONS` / `mock.ts` drivers·users 의 `user01~05` 와 **id·이름 정렬**해 둠.

### 1.2 관리자 웹 데모 (`AuthContext` — Supabase 미설정 시)

| email | 역할 | 비고 |
|-------|------|------|
| `admin@mju.ac.kr` | ADMIN | 비밀번호 검사 없이 데모 로그인 |

### 1.3 관리자 웹 mock 사용자 목록 (`mock.ts` `users`)

| id | name | email | role(UI) | status |
|----|------|-------|----------|--------|
| admin | 관리자 | admin@mju.ac.kr | 관리자 | 활성 |
| operator1 | 김운영 | operator1@mju.ac.kr | 운영자 | 활성 |
| operator2 | 이운영 | operator2@mju.ac.kr | 운영자 | 활성 |
| user01 | 박사용 | user01@mju.ac.kr | 일반 | 활성 |
| user02 | 최사용 | user02@mju.ac.kr | 일반 | 활성 |
| user03 | 정사용 | user03@mju.ac.kr | 일반 | 활성 |
| user04 | 한사용 | user04@mju.ac.kr | 일반 | 비활성 |
| user05 | 임사용 | user05@mju.ac.kr | 일반 | 활성 |

> 참고: 공통명세의 `driver01@onda.local` / `Admin1234!` 등과 **아직 코드 mock이 완전히 일치하지 않음**. 서버 seed 시 팀 합의 계정으로 통일 권장.

---

## 2. 노선 · 정류장 · 차량 (정적 mock)

### 2.1 노선 코드(표시명) — 배차/일정 공통

```
기흥역 통학버스
명지대역 셔틀
시내 셔틀
```

(`SCHEDULE_ROUTE_OPTIONS`, `schedules`, `routes`)

### 2.2 정류장 샘플 (`stops`)

| name | routes | lat | lng | guide |
|------|--------|-----|-----|-------|
| 기흥역 5번 출구 | 기흥역 통학버스 | 37.2754 | 127.1159 | 5번 출구 앞 정류장 |
| 채플관 앞 | 기흥역 통학버스 | 37.2240 | 127.1872 | 채플관 정문 버스정류장 |
| 명지대역 | 명지대역 셔틀 | 37.2381 | 127.1905 | 명지대역 2번 출구 |
| 학생회관 | 시내 셔틀 | 37.2225 | 127.1888 | 학생회관 앞 승차장 |

### 2.3 차량 샘플 (`vehicles`)

| bus | plate | status |
|-----|-------|--------|
| 온다 1호기 | 72버 1234 | 운행 중 |
| 온다 2호기 | 73버 1122 | 정비 예정 |
| 온다 3호기 | 72버 5678 | 운행 중 |
| 온다 6호기 | 75버 9900 | 통신 이상 |

배차 템플릿에서는 표시명 `1호차`~`4호차` 를 사용 (위 “온다 N호기”와 표기만 다름 → 서버에서는 통일 필요).

---

## 3. 핵심 시나리오: 오늘 배차 템플릿

소스: `frontend/admin/vite-dev-store.ts`  
기사 fallback: `MockTodayOperations.kt`  
**요일 무관, 매일 동일 패턴.** 날짜만 `YYYY-MM-DD`로 바뀜. 자정 지나면 status 리셋.

### 3.1 공통 필드 (`TodayAssignment`)

```
id, date, driverId, driverName, routeName, vehicleName,
departTime (HH:mm), expectedEndTime (HH:mm),
origin, destination, round, status
```

### 3.2 status enum (관리자/기사 공통 매핑)

| 코드 | 의미 |
|------|------|
| `waiting` | 운행 대기 |
| `departing_soon` | 곧 출발 |
| `scheduled` | 운행 예정 |
| `in_progress` | 운행 중 |
| `ended` | 운행 종료 |

기사 앱: `Waiting` / `DepartingSoon` / `Scheduled` / `InProgress` / `Ended`

### 3.3 user01 (박사용) — 3건

| id | routeName | vehicleName | depart | end | origin → destination |
|----|-----------|-------------|--------|-----|----------------------|
| `op-0905` | 기흥역 통학버스 | 2호차 | 09:05 | 09:25 | 채플관 앞 → 기흥역 5번 출구 |
| `op-1000` | 명지대역 셔틀 | 1호차 | 10:00 | 10:25 | 자연캠퍼스 → 명지대역 |
| `op-1200` | 시내 셔틀 | 3호차 | 12:00 | 12:40 | 채플관 앞 → 용인시청 |

### 3.4 user02 (최사용) — 3건

| id | routeName | vehicleName | depart | end | origin → destination |
|----|-----------|-------------|--------|-----|----------------------|
| `d02-op-0840` | 기흥역 통학버스 | 1호차 | 08:40 | 09:10 | 채플관 앞 → 기흥역 5번 출구 |
| `d02-op-1110` | 명지대역 셔틀 | 1호차 | 11:10 | 11:40 | 자연캠퍼스 → 명지대역 |
| `d02-op-1420` | 시내 셔틀 | 4호차 | 14:20 | 15:00 | 채플관 앞 → 용인시청 |

### 3.5 user03 / user04 / user05

- 기본 seed: **배정 0건** (빈 화면 시나리오)
- 관리자 웹에서 배차 추가하면 Vite store에 쌓임 → 기사 앱이 `/api/assignments`로 조회

---

## 4. 통합 플로우 시나리오 (로컬 API)

관리자 `npm run dev` (Vite) 가 떠 있을 때 기사 앱이 `http://10.0.2.2:5173` 등으로 호출.

### 4.1 API 목록

| Method | Path | 역할 |
|--------|------|------|
| GET/POST/PATCH/DELETE | `/api/assignments` | 배차 CRUD · 날짜 쿼리 |
| GET | `/api/live/vehicles` | 실시간 목록 |
| POST | `/api/live/heartbeat` | 기사 GPS/상태 heartbeat |
| GET/POST/PATCH | `/api/safe-stop` | 안전 정차 요청·결정 |
| GET/POST | `/api/login-history` | 로그인 기록 |
| GET/PATCH | `/api/admin-notifications` | 관리자 종 알림 |

### 4.2 시나리오 A — 배차 확인 → 운행 시작 → GPS → 종료

1. 관리자: 당일 템플릿 배차 존재 (`scheduled`)
2. 기사 `user01` 로그인 → 오늘의 운행 3건 표시
3. 기사: 특정 `operationId`(예: `op-0905`) 운행 시작  
   → assignment `in_progress` + live heartbeat
4. 관리자: `/live` 에서 해당 차량·상태 표시
5. 기사: 운행 종료 → `ended`  
   → 이후 heartbeat 무시(로컬 규칙)

### 4.3 시나리오 B — 안전 정차 요청

1. 기사: 운행 중 안전 정차 요청 (사유 + 상세)
2. POST `/api/safe-stop` → `decision: pending`
3. 관리자 알림 토스트 + `/live/suspend`
4. 관리자: **계속 운행** (`continue`) 또는 **중단 승인** (`stop`)
5. 기사 앱 폴링으로 결과 반영 / 취소(`cancelled`) 가능
6. **날짜가 바뀌면** ended/중단 상태 일일 리셋 (템플릿 재시드)

### 4.4 시나리오 C — 로그인 기록

seed:

| userId | name | source |
|--------|------|--------|
| admin | 관리자 | admin-web |
| operator1 | 김운영 | admin-web |
| user01 | 박사용 | driver-app |
| operator2 | 이운영 | admin-web |

기사/관리자 로그인 시 POST로 추가 → 사용자 관리 「최근 로그인」에 표시.

### 4.5 시나리오 D — 배정 없음

`user03` 로그인 → 오늘 배정 0건 UI (문의 유도).

---

## 5. 안전 정차 사유 (기사 UI mock)

`MockStopReasonSelect.kt`

| id | label |
|----|-------|
| `breakdown` | 차량 고장 |
| `accident` | 교통사고 |
| `weather` | 기상악화 |
| `road_control` | 도로 통제 |
| `passenger` | 승객 안전 문제 |
| `other` | 기타 (상세 10자 이내) |

요청 결정 상태: `pending` | `continue` | `stop` | `cancelled`

---

## 6. 관리자 화면 전용 정적 mock (참고)

파일: `frontend/admin/src/data/mock.ts`

포함 목록:

- `kpiCards` — 대시보드 KPI 숫자
- `recentOps` / `gpsAlerts` / `liveVehicles` — 대시보드·관제 데모 행
- `schedules` — 노선별 운행 시간대 요약
- `reports` — 학생 제보 샘플 4건
- `notices` — 공지 샘플 8건
- `maintenances` — 정비 이력
- `systemLogs` — 시스템 로그 샘플

> 배차·실시간 **실연 연동 데이터는 4절 Vite store**가 우선. mock.ts 는 UI 채우기용.

---

## 7. 기사 앱 Mock 파일 목록 (화면 카피/상태)

대부분 `feature/*/data/Mock*.kt` — UI 문구·단일 화면 상태용.  
서버 이관 우선순위는 **계정 / 오늘 배차 / 운행 상태 / GPS / 안전정차**.

중요 파일:

| 파일 | 내용 |
|------|------|
| `data/mock/MockUsers.kt` | 로그인 계정 |
| `feature/home/data/MockTodayOperations.kt` | 오늘 배정 seed |
| `feature/auth/data/MockAuthRepository.kt` | 로컬 로그인 |
| `feature/operation/data/MockOperationDetail.kt` | 운행 상세 |
| `feature/precheck/data/MockPreOperationCheck.kt` | 출발 전 점검 |
| `feature/inoperation/data/MockInOperation*.kt` | 운행 중 |
| `feature/settings/data/MockSafeStop*.kt` / Stop* | 안전 정차 UI |
| `feature/history/data/MockOperationHistory.kt` | 이력 |
| `core/DemoReset.kt` | 데모 상태 초기화 |

---

## 8. 백엔드 쪽에 맞춰 달라고 부탁할 것 (체크)

- [ ] Auth 사용자: `user01~05` + admin (비밀번호·role 합의)
- [ ] 매일 동일 배차 템플릿 6건 seed (`op-*`, `d02-op-*` id 유지 또는 매핑 테이블)
- [ ] status enum 및 전이: scheduled → … → in_progress → ended / force_end / stop
- [ ] GPS: RUNNING(또는 in_progress) 일 때만 수신
- [ ] 안전 정차: request → admin resolve → driver notify
- [ ] 노선 표시명 3개 · 정류장 좌표 샘플 반영
- [ ] 차량 표기 `N호차` vs `온다 N호기` 통일
- [ ] 공통명세 `profiles` vs 현재 코드 `users`/`profiles` 혼재 → 테이블명 확정

---

## 9. 원본 위치 (복사용)

```
frontend/admin/src/data/mock.ts
frontend/admin/src/types/assignment.ts
frontend/admin/vite-dev-store.ts
frontend/admin/vite-assignments-plugin.ts
frontend/admin/vite-live-plugin.ts
frontend/admin/vite-safe-stop-plugin.ts
frontend/admin/vite-login-history-plugin.ts
frontend/admin/vite-admin-notifications-plugin.ts
frontend/driver/.../MockUsers.kt
frontend/driver/.../MockTodayOperations.kt
```

질문 있으면 프론트 담당에게 operationId / status 전이 기준으로 맞추면 됩니다.
