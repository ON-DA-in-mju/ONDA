export const kpiCards = [
  { title: '오늘 배정 운행 수', value: 96, unit: '건', delta: '+4건 (전일 대비)', color: '#266ef4' },
  { title: '현재 운행 중 차량', value: 28, unit: '대', delta: '+3대 (전일 대비)', color: '#3fb46a' },
  { title: '운행 배정 차량', value: 42, unit: '대', delta: '+2대 (전일 대비)', color: '#fdac38' },
  { title: 'GPS·통신 이상', value: 5, unit: '건', delta: '+2건 (전일 대비)', color: '#eb4047' },
  { title: '전체 학생 제보', value: 12, unit: '건', delta: 'DB 연동 전', color: '#7964f2' },
  { title: '긴급 공지', value: 1, unit: '건', delta: '게시 중', color: '#ec181b' },
]

export const recentOps = [
  { status: '운행 시작', tone: 'blue' as const, route: '기흥역 ↔ 캠퍼스', bus: '온다 3호기', driver: '김기사', time: '08:12' },
  { status: '정상 운행', tone: 'green' as const, route: '용인시청 ↔ 캠퍼스', bus: '온다 1호기', driver: '이기사', time: '08:05' },
  { status: '지연 발생', tone: 'orange' as const, route: '수원역 ↔ 캠퍼스', bus: '온다 5호기', driver: '박기사', time: '07:58' },
  { status: 'GPS 이상', tone: 'red' as const, route: '죽전역 ↔ 캠퍼스', bus: '온다 6호기', driver: '최기사', time: '07:51' },
]

export const gpsAlerts = [
  { bus: '온다 6호기', route: '죽전역 ↔ 캠퍼스', location: '죽전역 인근', issue: '신호 미수신', time: '07:51', status: '확인 중', tone: 'red' as const },
  { bus: '온다 2호기', route: '기흥역 ↔ 캠퍼스', location: '캠퍼스 정문', issue: '위치 오차', time: '07:40', status: '조치 중', tone: 'orange' as const },
  { bus: '온다 8호기', route: '수원역 ↔ 캠퍼스', location: '영통 IC', issue: '통신 지연', time: '07:22', status: '조치 완료', tone: 'gray' as const },
]

/** 노선 관리·기사 앱과 동일한 3개 노선 기준 운행 일정 (2026 mju_pier_ 공지) */
export const schedules = [
  {
    no: 1,
    route: '기흥역 통학버스',
    start: '08:15',
    end: '19:15',
    interval: '학기중 평일',
    rounds: 14,
    status: '운행 예정',
    tone: 'blue' as const,
  },
  {
    no: 2,
    route: '명지대역 셔틀',
    start: '08:00',
    end: '19:30',
    interval: '학기중 평일',
    rounds: 54,
    status: '운행 예정',
    tone: 'blue' as const,
  },
  {
    no: 3,
    route: '시내 셔틀',
    start: '08:05',
    end: '20:00',
    interval: '학기중·주말·방학',
    rounds: 10,
    status: '운행 예정',
    tone: 'blue' as const,
  },
]

export const SCHEDULE_ROUTE_OPTIONS = [
  '기흥역 통학버스',
  '명지대역 셔틀',
  '시내 셔틀',
  '시내 셔틀 (주말·공휴일·방학)',
] as const

export const liveVehicles = [
  { bus: '온다 1호기', driver: '이기사', route: '용인시청', stop: '정문 정류장', status: '운행 중', tone: 'green' as const, gps: '정상', last: '방금 전' },
  { bus: '온다 3호기', driver: '김기사', route: '기흥역', stop: '도서관 앞', status: '운행 중', tone: 'green' as const, gps: '정상', last: '12초 전' },
  { bus: '온다 5호기', driver: '박기사', route: '수원역', stop: '영통 IC', status: '정지 중', tone: 'orange' as const, gps: '정상', last: '1분 전' },
  { bus: '온다 6호기', driver: '최기사', route: '죽전역', stop: '신호 없음', status: '실시간 미수신', tone: 'red' as const, gps: '미수신', last: '8분 전' },
]

export const reports = [
  { type: '무정차 통과', target: '기흥역 / 온다 3호기', time: '09:12', likes: 12, bookmarks: 3, status: '처리 대기', tone: 'orange' as const },
  { type: '과속 운행', target: '수원역 / 온다 5호기', time: '08:44', likes: 8, bookmarks: 1, status: '완료', tone: 'blue' as const },
  { type: '정류장 혼잡', target: '캠퍼스 정문', time: '08:20', likes: 21, bookmarks: 5, status: '처리 대기', tone: 'orange' as const },
  { type: '불친절', target: '용인시청 / 온다 1호기', time: '07:55', likes: 4, bookmarks: 0, status: '비활성', tone: 'gray' as const },
]

export const notices = [
  { no: 128, type: '긴급', tone: 'red' as const, title: '폭설로 인한 운행 지연 안내', target: '전체', period: '2024.05.20 - 2024.05.20', views: 1245, status: '게시중' },
  { no: 127, type: '중요', tone: 'orange' as const, title: '5월 3주차 운행 시간 조정 안내', target: '전체', period: '2024.05.18 - 2024.05.24', views: 1102, status: '게시중' },
  { no: 126, type: '운행 변경', tone: 'blue' as const, title: '노선 15-1 경로 일부 변경 안내', target: '노선 15-1', period: '2024.05.15 - 2024.06.15', views: 856, status: '게시중' },
  { no: 125, type: '일반', tone: 'gray' as const, title: '여름 방학 셔틀 운행 안내', target: '전체', period: '2024.05.10 - 2024.06.30', views: 732, status: '게시중' },
  { no: 124, type: '일반', tone: 'gray' as const, title: '어린이날 운행 안내', target: '전체', period: '2024.05.02 - 2024.05.06', views: 642, status: '종료' },
  { no: 123, type: '운행 변경', tone: 'blue' as const, title: '노선 9-2 정류장 위치 변경', target: '노선 9-2', period: '2024.04.25 - 2024.05.25', views: 521, status: '종료' },
  { no: 122, type: '일반', tone: 'gray' as const, title: '앱 업데이트 안내', target: '전체', period: '2024.04.20 - 2024.04.27', views: 412, status: '종료' },
  { no: 121, type: '일반', tone: 'gray' as const, title: '셔틀 만족도 조사 참여 안내', target: '3개 노선', period: '2024.04.15 - 2024.04.30', views: 598, status: '종료' },
]

export const routes = [
  {
    name: '기흥역 통학버스',
    stops: 3,
    buses: '최대 5대',
    status: '운행 중',
    type: '왕복',
    days: '학기중 평일',
    hours: '08:00 ~ 19:30',
    desc: '채플관 앞 → 기흥역 5번 출구 → 채플관 앞. 계절학기·방학 제외.',
  },
  {
    name: '명지대역 셔틀',
    stops: 12,
    buses: '4대',
    status: '운행 중',
    type: '진입로(명지대역)',
    days: '학기중 평일·계절학기',
    hours: '08:00 ~ 18:10',
    desc: '버스관리사무소 → 상공회의소 → 진입로(럭스나인 앞) → 경전철 명지대역 → … → 함박관 → 창조관 → 버스관리사무소.',
  },
  {
    name: '시내 셔틀',
    stops: 13,
    buses: '1대',
    status: '운행 중',
    type: '시내',
    days: '학기중 평일',
    hours: '08:05 ~ 18:10',
    desc: '버스관리사무소 → 상공회의소 → … → 제1공학관 → 제3공학관 → 함박관 → 창조관 → 버스관리사무소.',
  },
  {
    name: '시내 셔틀 (주말·공휴일·방학)',
    stops: 13,
    buses: '1대',
    status: '운행 중',
    type: '시내',
    days: '주말·공휴일·방학',
    hours: '08:20 ~ 18:00',
    desc: '생활관(명현관) 기점 순환 10회.',
  },
]

export const vehicles = [
  { bus: '온다 1호기', plate: '72버 1234', status: '운행 중', mileage: '84,220km', next: '2026.08.20' },
  { bus: '온다 2호기', plate: '73버 1122', status: '정비 예정', mileage: '91,040km', next: '2026.08.08' },
  { bus: '온다 3호기', plate: '72버 5678', status: '운행 중', mileage: '67,510km', next: '2026.09.01' },
  { bus: '온다 6호기', plate: '75버 9900', status: '통신 이상', mileage: '102,300km', next: '2026.08.07' },
]

export const maintenances = [
  { date: '2026-07-20', plate: '72버 1234', item: '엔진오일 교환', type: '정기', mechanic: '김기사', cost: '120,000', status: '완료', tone: 'green' as const },
  { date: '2026-07-19', plate: '73버 1122', item: '브레이크 패드 교체', type: '수리', mechanic: '이운영', cost: '450,000', status: '완료', tone: 'green' as const },
  { date: '2026-07-18', plate: '72버 5678', item: '타이어 교체', type: '수리', mechanic: '박정비', cost: '300,000', status: '완료', tone: 'green' as const },
  { date: '2026-07-17', plate: '75버 9900', item: '에어컨 필터 교체', type: '정기', mechanic: '김기사', cost: '50,000', status: '예정', tone: 'orange' as const },
  { date: '2026-07-16', plate: '74버 7788', item: '배터리 점검', type: '점검', mechanic: '박정비', cost: '30,000', status: '점검중', tone: 'blue' as const },
  { date: '2026-07-15', plate: '73버 3344', item: '냉각수 보충', type: '정기', mechanic: '이운영', cost: '20,000', status: '완료', tone: 'green' as const },
  { date: '2026-07-14', plate: '72버 1234', item: '차량 하부 점검', type: '점검', mechanic: '김기사', cost: '40,000', status: '완료', tone: 'green' as const },
  { date: '2026-07-13', plate: '75버 9900', item: '타이어 위치 교환', type: '정기', mechanic: '박정비', cost: '60,000', status: '예정', tone: 'orange' as const },
]

export const users = [
  { id: 'admin', name: '관리자', email: 'admin@mju.ac.kr', role: '관리자', lastLogin: '2026.07.20 09:32', status: '활성' },
  { id: 'operator1', name: '김운영', email: 'operator1@mju.ac.kr', role: '운영자', lastLogin: '2026.07.20 08:15', status: '활성' },
  { id: 'operator2', name: '이운영', email: 'operator2@mju.ac.kr', role: '운영자', lastLogin: '2026.07.19 17:45', status: '활성' },
  { id: 'user01', name: '박사용', email: 'user01@mju.ac.kr', role: '일반', lastLogin: '2026.07.20 07:50', status: '활성' },
  { id: 'user02', name: '최사용', email: 'user02@mju.ac.kr', role: '일반', lastLogin: '2026.07.18 14:20', status: '활성' },
  { id: 'user03', name: '정사용', email: 'user03@mju.ac.kr', role: '일반', lastLogin: '2026.07.19 11:05', status: '활성' },
  { id: 'user04', name: '한사용', email: 'user04@mju.ac.kr', role: '일반', lastLogin: '2026.07.20 06:40', status: '비활성' },
  { id: 'user05', name: '임사용', email: 'user05@mju.ac.kr', role: '일반', lastLogin: '2026.07.17 09:12', status: '활성' },
]

export const stops = [
  { name: '기흥역 5번 출구', routes: '기흥역 통학버스', lat: '37.2754', lng: '127.1159', guide: '5번 출구 앞 정류장' },
  { name: '채플관 앞', routes: '기흥역 통학버스', lat: '37.2240', lng: '127.1872', guide: '채플관 정문 버스정류장' },
  { name: '명지대역', routes: '명지대역 셔틀', lat: '37.2381', lng: '127.1905', guide: '명지대역 2번 출구' },
  { name: '학생회관', routes: '시내 셔틀', lat: '37.2225', lng: '127.1888', guide: '학생회관 앞 승차장' },
]

export const drivers = [
  { name: '박사용', email: 'user01@mju.ac.kr', status: '운행 가능', lastTrip: '2026.08.06 09:05', phone: '010-1111-2222' },
  { name: '최사용', email: 'user02@mju.ac.kr', status: '운행 중', lastTrip: '2026.08.06 08:40', phone: '010-3333-4444' },
  { name: '정사용', email: 'user03@mju.ac.kr', status: '휴무', lastTrip: '2026.08.05 18:10', phone: '010-5555-6666' },
  { name: '한사용', email: 'user04@mju.ac.kr', status: '운행 가능', lastTrip: '2026.08.04 17:20', phone: '010-7777-8888' },
  { name: '임사용', email: 'user05@mju.ac.kr', status: '운행 가능', lastTrip: '2026.08.03 16:05', phone: '010-9999-0000' },
]

export const systemLogs = [
  {
    time: '2026.07.20 10:30:45',
    type: '운행 변경',
    action: "72버 1234 차량 운행 상태를 '운행 중'으로 변경",
    actor: '김기사 (admin)',
    ip: '192.168.10.25',
    target: '차량: 72버 1234',
    result: '성공',
  },
  {
    time: '2026.07.20 10:28:12',
    type: '노선 수정',
    action: '기흥역 캠퍼스 노선의 정류장 2개 수정',
    actor: '이운영 (manager)',
    ip: '192.168.10.18',
    target: '노선: 기흥역 캠퍼스',
    result: '성공',
  },
  {
    time: '2026.07.20 10:25:33',
    type: '사용자 로그인',
    action: '관리자 로그인 성공',
    actor: '김기사 (admin)',
    ip: '192.168.10.25',
    target: '-',
    result: '성공',
  },
  {
    time: '2026.07.20 10:20:11',
    type: '차량 상태 변경',
    action: "75버 9900 차량 상태를 '운행 중지'로 변경",
    actor: '박담당 (staff)',
    ip: '192.168.10.32',
    target: '차량: 75버 9900',
    result: '성공',
  },
  {
    time: '2026.07.20 10:18:07',
    type: '공지사항 등록',
    action: '공지사항 "여름 방학 셔틀버스 안내" 등록',
    actor: '이운영 (manager)',
    ip: '192.168.10.18',
    target: '공지사항',
    result: '성공',
  },
  {
    time: '2026.07.20 10:15:42',
    type: '권한 변경',
    action: '사용자 "홍길동"의 권한을 "운영자"로 변경',
    actor: '김기사 (admin)',
    ip: '192.168.10.25',
    target: '사용자: 홍길동',
    result: '성공',
  },
  {
    time: '2026.07.20 10:12:30',
    type: '시스템 설정 변경',
    action: '알림 수신 설정 변경 (이메일, 푸시)',
    actor: '시스템',
    ip: '192.168.10.99',
    target: '알림 설정',
    result: '성공',
  },
  {
    time: '2026.07.20 10:08:55',
    type: '로그인 실패',
    action: '로그인 실패 (비밀번호 오류)',
    actor: '박담당 (staff)',
    ip: '192.168.10.32',
    target: '-',
    result: '실패',
  },
  {
    time: '2026.07.20 10:05:21',
    type: '데이터 내보내기',
    action: '운행 기록 데이터를 엑셀로 내보내기',
    actor: '이운영 (manager)',
    ip: '192.168.10.18',
    target: '운행 기록',
    result: '성공',
  },
  {
    time: '2026.07.20 10:02:14',
    type: '오류 발생',
    action: '차량 위치 정보 수신 실패 (연결 끊김)',
    actor: '시스템',
    ip: '192.168.10.99',
    target: '차량: 76버 2468',
    result: '경고',
  },
]

