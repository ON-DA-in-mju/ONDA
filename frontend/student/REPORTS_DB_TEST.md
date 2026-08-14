# 학생 제보 ↔ 관리자 웹 DB 연동 테스트

학생 앱 커뮤니티 제보가 Supabase `reports` 테이블과 관리자 웹 **제보 관리**에 연결됩니다.

## 0. 사전 준비 (한 번만)

### 1) Supabase SQL 실행

Supabase Dashboard → SQL Editor에서 아래 파일 내용을 **순서대로** 실행하세요.

1. `frontend/admin/supabase/migrate_reports_student_write.sql`  
   - 학생이 제보 **등록/수정/삭제** 할 수 있는 RLS 정책 추가  
   - `source`, `category` 컬럼 보강

2. `frontend/admin/supabase/migrate_report_reactions.sql`  
   - 좋아요/싫어요용 `report_reactions` 테이블 + RLS  
   - 학생 1명당 제보당 반응 1개 (`LIKE` / `DISLIKE`)

3. `frontend/admin/supabase/migrate_reports_board_type.sql`  
   - `reports.board_type`: `REPORT`(제보) / `POST`(소통 글쓰기)  
   - **`report_reactions`에는 구분 컬럼 불필요** (report_id로 연결)  
   - 관리자 제보 목록은 `board_type = REPORT` 만 표시

4. `frontend/admin/supabase/migrate_report_comments.sql`  
   - 소통 글 댓글용 `report_comments` 테이블 + RLS  
   - POST 글에만 댓글 insert 가능

5. `frontend/admin/supabase/migrate_report_comments_soft_delete.sql`  
   - 댓글 소프트 삭제용 `is_deleted` 컬럼 (삭제 시 "삭제된 댓글입니다." 표시)

> `migrate_community_posts.sql` 은 사용하지 마세요 (통합으로 대체됨).

### 2) 학생 계정

- 학생 앱에 **로그인된 계정**의 Auth UID가 `public.users.id`에 있어야 합니다.
- 없으면 제보 insert 시 FK/권한 오류가 납니다.

### 3) 앱 설정

`frontend/student` (또는 local.properties)에 `SUPABASE_URL`, `SUPABASE_KEY`가 들어 있어야 합니다.

## 1. 학생 앱에서 확인

1. 학생 앱 실행 후 로그인
2. 하단 **커뮤니티** 탭 진입
3. Logcat 필터: `ONDA_REPORTS`
   - 성공 예: `loaded student reports=N, mine=M`
4. **제보하기** → 유형/노선/정류장/내용 입력 → **제보 등록하기**
5. 스낵바: `제보가 등록되었습니다.`
6. 커뮤니티 목록 맨 위에 방금 제보가 보이는지 확인
7. 목록/상세에서 **공감(좋아요)·비공감(싫어요)** 를 눌러 보세요
   - 같은 버튼을 다시 누르면 취소
   - 반대 버튼을 누르면 전환
   - 다른 탭으로 갔다가 돌아와도 선택한 상태가 유지되어야 함
8. MY → **내 제보 내역**에도 같은 제보가 보이는지 확인

## 2. 관리자 웹에서 확인

1. 관리자 웹 실행 (`frontend/admin` → `npm run dev`) → http://localhost:5173
2. 관리자 계정 로그인
3. 왼쪽 메뉴 **제보 관리**
4. 상단이 `Supabase reports N건` 인지 확인 (mock이면 DB 미연결)
5. 목록에서 제목이 대략 아래 형식인지 확인  
   `[만석] 기흥역 통학버스 · 명지대역 사거리`
6. **상세** 클릭 → 본문에 노선/방향/정류장/차량/유형 + 내용이 보이는지 확인
7. 상태가 `PENDING`(처리 대기)인지 확인

## 3. DB에서 직접 확인 (선택)

Supabase → Table Editor → `reports`

| 컬럼 | 기대값 |
|------|--------|
| source | `STUDENT` |
| status | `PENDING` |
| category | `Full`, `LongQueue` 등 (학생 앱 ReportType 이름) |
| title | `[유형] 노선 · 정류장` |
| user_id | 로그인한 학생 Auth UID |

SQL 예시:

```sql
select id, title, status, source, category, user_id, created_at
from public.reports
where source = 'STUDENT'
order by created_at desc
limit 10;

select report_id, user_id, reaction, updated_at
from public.report_reactions
order by updated_at desc
limit 20;
```

## 4. 실패 시 체크리스트

| 증상 | 확인 |
|------|------|
| `제보 DB 연동 실패` / 목록이 mock | SQL 미실행, 네트워크, SUPABASE_KEY |
| `제보 등록 실패` + RLS/policy | `migrate_reports_student_write.sql` 재실행 |
| `반응 저장 실패` | `migrate_report_reactions.sql` 미실행 / RLS |
| insert FK 오류 | `public.users`에 해당 `user_id` 행 존재 여부 |
| 관리자 웹이 mock | admin `.env.local`의 Supabase URL/KEY |

## 5. 데이터 매핑 요약

| 학생 앱 | DB `reports` | 관리자 웹 |
|----------|--------------|-----------|
| 유형+노선+정류장 | title | 목록 제목 |
| 상세 본문+메타 | content | 상세 내용 |
| ReportType | category | (상세 content의 유형 줄) |
| 고정 STUDENT | source | 학생 제보 |
| PENDING | status | 처리 대기 |
