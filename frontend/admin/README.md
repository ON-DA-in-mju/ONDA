# ON-DA Admin (React SPA)

Docs(기능명세서·제안서·착수용 공통명세서) + Figma Page2 관리자 웹을 React SPA로 구현합니다.

## 아키텍처

```
App (Routes / SPA)
├── AuthProvider          # 전역 auth state
├── PublicOnlyRoute       # 로그인/회원가입
│   ├── LoginPage  (ADM-01)
│   └── SignupPage
└── ProtectedRoute
    └── AdminLayout       # Sidebar + Header (props)
        ├── Dashboard     (ADM-02)
        ├── Schedules...  (ADM-03~05,08)
        ├── Live...       (ADM-06~07)
        ├── Reports       (ADM-14)
        ├── Notices       (ADM-11)
        ├── Routes/Stops  (ADM-09~10)
        ├── Vehicles      (ADM-12)
        ├── Drivers       (ADM-13)
        └── Users/System/Settings
```

- **SPA**: `react-router-dom` 클라이언트 라우팅 (페이지 새로고침 없이 화면 전환)
- **Virtual DOM**: React가 state/props 변경 시 필요한 컴포넌트만 갱신
- **State**: `AuthContext` + 페이지 로컬 `useState`
- **Props**: `Sidebar`, `Header`, `Field`, `DataTable`, `StatCard` 등 하위 전달

## 실행

```bash
cd frontend/admin
npm install
npm run dev
```

http://localhost:5173

데모 로그인: `admin@mju.ac.kr` / `Admin1234!`
