# Supabase 직접 연동 (관리자 웹)

## 1. Supabase 프로젝트
1. https://supabase.com 에서 프로젝트 생성
2. **Project Settings → API** 에서 복사
   - Project URL
   - `anon` `public` key  
   ※ `service_role` 키는 프론트에 넣지 마세요.

## 2. 환경 변수
`frontend/admin/.env.local` 생성:

```env
VITE_SUPABASE_URL=https://xxxx.supabase.co
VITE_SUPABASE_ANON_KEY=eyJhbGciOi...
```

## 3. DB 스키마
Supabase Dashboard → **SQL Editor** → `supabase/schema.sql` 내용 실행.

## 4. Auth 설정 (권장 데모)
- Authentication → Providers → Email 활성화
- 개발 중이면 **Confirm email** 끄기 (바로 로그인 가능)
- Authentication → Users 에서 `admin@mju.ac.kr` 수동 생성하거나, 앱 회원가입 사용

## 5. 실행
```bash
cd frontend/admin
npm run dev
```

로그인/회원가입이 Supabase Auth + `profiles` 테이블과 연결됩니다.  
공지 등 화면 데이터는 `src/lib/*Api.ts` 로 점진적으로 mock → Supabase 조회로 교체하면 됩니다.

## 구조
```
React Admin (Vite)
  └─ @supabase/supabase-js (anon key)
       ├─ Auth (로그인/세션)
       └─ Postgres (RLS 정책 아래 테이블 접근)
```
