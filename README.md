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
