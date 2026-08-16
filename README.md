# ON-DA (ONDA)

명지대 자연캠퍼스 셔틀 — 관리자 웹 · 기사 앱 · 학생 앱

`develop` = `develop-driver` + `develop-student` 통합 브랜치입니다.

## 폴더 구조

```
Bus/
├── frontend/
│   ├── admin/              # 관리자 웹 (Vite + React)
│   ├── driver/             # 기사 앱 (Android / Kotlin)
│   └── student/            # 학생 앱 (Android / Kotlin)
├── docs/
└── README.md
```

## 관리자 웹

```bash
cd frontend/admin
npm install
npm run dev
```

환경 변수: `frontend/admin/.env.local` (`VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`)

### Vercel 배포

Vercel에 올리는 것은 **관리자 웹만**입니다. 기사/학생 앱은 Android, 서버(DB)는 이미 Supabase입니다.

1. [vercel.com](https://vercel.com) → GitHub 저장소 import
2. **Root Directory** 를 `frontend/admin` 으로 지정 (권장)
3. Environment Variables:
   - `VITE_SUPABASE_URL`
   - `VITE_SUPABASE_ANON_KEY`
   - `VITE_NAVER_MAP_CLIENT_ID` (지도)
4. Deploy 후 나온 `https://….vercel.app` 을
   - 네이버 클라우드 Maps **Web 서비스 URL**
   - Supabase Authentication → URL Configuration **Site URL / Redirect URLs**
   에 추가

Root Directory를 비워 두면 저장소 루트의 `vercel.json`이 `frontend/admin`을 빌드합니다.

## 기사 앱

Android Studio에서 **`frontend/driver`** 폴더를 Open 하세요.

```bash
cd frontend/driver
./gradlew :app:assembleDebug
```

## 학생 앱

Android Studio에서 **`frontend/student`** 폴더를 Open 하세요.

```bash
cd frontend/student
./gradlew :app:assembleDebug
```
