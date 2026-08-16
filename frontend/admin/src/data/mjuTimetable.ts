import type { Weekday, SemesterType } from '../types/database';
import { CITY_SHUTTLE_VACATION_ROUTE_NAME } from './cityShuttleStops';
import { resolveOperationalRouteName } from '../lib/routeVariants';

export const MJU_ROUTE_NAMES = [
  '기흥역 통학버스',
  '명지대역 셔틀',
  '시내 셔틀',
] as const;
export type MjuRouteName = (typeof MJU_ROUTE_NAMES)[number];

/** schedules에 저장되는 실제 노선명(변형 포함) */
export type ScheduleRouteName = string;

export type MjuTripRoute = MjuRouteName | typeof CITY_SHUTTLE_VACATION_ROUTE_NAME;

export type MjuTrip = {
  no: number;
  route: MjuTripRoute;
  departure: string;
  via?: string;
  arrival?: string;
  buses?: number;
};

export type MjuTimetablePack = {
  id: string;
  title: string;
  period: 'SEMESTER' | 'VACATION';
  daysLabel: string;
  weekdays: Weekday[];
  semester: SemesterType;
  trips: MjuTrip[];
  note?: string;
};

const WEEKDAYS: Weekday[] = ['MON', 'TUE', 'WED', 'THU', 'FRI'];
const WEEKEND: Weekday[] = ['SAT', 'SUN'];

/** 학기 중 평일 — 명지대역·시내 (이미지 am+pm) */
const SEMESTER_SHUTTLE: MjuTrip[] = [
  { no: 1, route: '명지대역 셔틀', departure: '08:00', via: '08:15' },
  { no: 2, route: '시내 셔틀', departure: '08:05', via: '08:20' },
  { no: 3, route: '명지대역 셔틀', departure: '08:15', via: '08:30' },
  { no: 4, route: '명지대역 셔틀', departure: '08:20', via: '08:35' },
  { no: 5, route: '명지대역 셔틀', departure: '08:25', via: '08:40' },
  { no: 6, route: '명지대역 셔틀', departure: '08:35', via: '08:50' },
  { no: 7, route: '명지대역 셔틀', departure: '08:45', via: '09:00' },
  { no: 8, route: '명지대역 셔틀', departure: '08:50', via: '09:05' },
  { no: 9, route: '시내 셔틀', departure: '08:55', via: '09:10' },
  { no: 10, route: '명지대역 셔틀', departure: '09:00', via: '09:15' },
  { no: 11, route: '명지대역 셔틀', departure: '09:15', via: '09:30' },
  { no: 12, route: '명지대역 셔틀', departure: '09:25', via: '09:40' },
  { no: 13, route: '명지대역 셔틀', departure: '09:30', via: '09:45' },
  { no: 14, route: '명지대역 셔틀', departure: '09:35', via: '09:50' },
  { no: 15, route: '명지대역 셔틀', departure: '09:40', via: '09:55' },
  { no: 16, route: '명지대역 셔틀', departure: '09:55', via: '10:10' },
  { no: 17, route: '명지대역 셔틀', departure: '10:00', via: '10:15' },
  { no: 18, route: '시내 셔틀', departure: '10:10', via: '10:25' },
  { no: 19, route: '명지대역 셔틀', departure: '10:20', via: '10:35' },
  { no: 20, route: '명지대역 셔틀', departure: '10:30', via: '10:45' },
  { no: 21, route: '명지대역 셔틀', departure: '10:40', via: '10:55' },
  { no: 22, route: '명지대역 셔틀', departure: '10:45', via: '11:00' },
  { no: 23, route: '명지대역 셔틀', departure: '11:00', via: '11:15' },
  { no: 24, route: '시내 셔틀', departure: '11:20', via: '11:35' },
  { no: 25, route: '명지대역 셔틀', departure: '11:25', via: '11:40' },
  { no: 26, route: '명지대역 셔틀', departure: '11:30', via: '11:45' },
  { no: 27, route: '명지대역 셔틀', departure: '11:45', via: '12:00' },
  { no: 28, route: '명지대역 셔틀', departure: '11:55', via: '12:10' },
  { no: 29, route: '명지대역 셔틀', departure: '12:05', via: '12:20' },
  { no: 30, route: '명지대역 셔틀', departure: '12:20', via: '12:35' },
  { no: 31, route: '명지대역 셔틀', departure: '12:30', via: '12:45' },
  { no: 32, route: '명지대역 셔틀', departure: '12:45', via: '13:00' },
  { no: 33, route: '명지대역 셔틀', departure: '13:00', via: '13:15' },
  { no: 34, route: '시내 셔틀', departure: '13:10', via: '13:25' },
  { no: 35, route: '명지대역 셔틀', departure: '13:25', via: '13:40' },
  { no: 36, route: '명지대역 셔틀', departure: '13:40', via: '13:55' },
  { no: 37, route: '명지대역 셔틀', departure: '14:00', via: '14:15' },
  { no: 38, route: '명지대역 셔틀', departure: '14:10', via: '14:25' },
  { no: 39, route: '명지대역 셔틀', departure: '14:15', via: '14:30' },
  { no: 40, route: '시내 셔틀', departure: '14:20', via: '14:35' },
  { no: 41, route: '명지대역 셔틀', departure: '14:30', via: '14:45' },
  { no: 42, route: '명지대역 셔틀', departure: '14:50', via: '15:05' },
  { no: 43, route: '명지대역 셔틀', departure: '15:00', via: '15:15' },
  { no: 44, route: '명지대역 셔틀', departure: '15:10', via: '15:25' },
  { no: 45, route: '명지대역 셔틀', departure: '15:25', via: '15:40' },
  { no: 46, route: '명지대역 셔틀', departure: '15:30', via: '15:45' },
  { no: 47, route: '시내 셔틀', departure: '15:40', via: '15:55' },
  { no: 48, route: '명지대역 셔틀', departure: '15:55', via: '16:10' },
  { no: 49, route: '명지대역 셔틀', departure: '16:10', via: '16:25' },
  { no: 50, route: '명지대역 셔틀', departure: '16:25', via: '16:40' },
  { no: 51, route: '명지대역 셔틀', departure: '16:30', via: '16:45' },
  { no: 52, route: '시내 셔틀', departure: '16:35', via: '16:50' },
  { no: 53, route: '명지대역 셔틀', departure: '16:50', via: '17:05' },
  { no: 54, route: '명지대역 셔틀', departure: '17:00', via: '17:15' },
  { no: 55, route: '명지대역 셔틀', departure: '17:10', via: '17:25' },
  { no: 56, route: '명지대역 셔틀', departure: '17:20', via: '17:35' },
  { no: 57, route: '명지대역 셔틀', departure: '17:30', via: '17:45' },
  { no: 58, route: '명지대역 셔틀', departure: '17:45', via: '18:00' },
  { no: 59, route: '명지대역 셔틀', departure: '18:00', via: '18:15' },
  { no: 60, route: '시내 셔틀', departure: '18:10', via: '18:25' },
];

/** 계절학기 = 학기 중 평일과 동일 (18:10까지) */
const SEASONAL_SHUTTLE: MjuTrip[] = SEMESTER_SHUTTLE;

/** 학기중 주말·공휴일·방학 — 시내만 (생활관 기점). 평일 시내 셔틀과 겹치지 않음 */
const WEEKEND_VACATION_CITY: MjuTrip[] = [
  { no: 1, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '08:20', arrival: '08:45' },
  { no: 2, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '09:20', arrival: '09:45' },
  { no: 3, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '10:20', arrival: '10:45' },
  { no: 4, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '11:20', arrival: '11:45' },
  { no: 5, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '12:20', arrival: '12:45' },
  { no: 6, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '13:20', arrival: '13:45' },
  { no: 7, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '15:20', arrival: '15:45' },
  { no: 8, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '16:20', arrival: '16:45' },
  { no: 9, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '17:20', arrival: '17:45' },
  { no: 10, route: CITY_SHUTTLE_VACATION_ROUTE_NAME, departure: '18:00', arrival: '18:25' },
];

/** 기흥역 통학버스 — 학기중 평일 (학교→기흥 + 기흥→학교) */
const GIHEUNG_SEMESTER: MjuTrip[] = [
  { no: 1, route: '기흥역 통학버스', departure: '08:00', buses: 1 },
  { no: 2, route: '기흥역 통학버스', departure: '09:05', buses: 3 },
  { no: 3, route: '기흥역 통학버스', departure: '09:10', buses: 2 },
  { no: 4, route: '기흥역 통학버스', departure: '10:00', buses: 3 },
  { no: 5, route: '기흥역 통학버스', departure: '10:05', buses: 2 },
  { no: 6, route: '기흥역 통학버스', departure: '12:00', buses: 1 },
  { no: 7, route: '기흥역 통학버스', departure: '13:00', buses: 1 },
  { no: 8, route: '기흥역 통학버스', departure: '14:00', buses: 1 },
  { no: 9, route: '기흥역 통학버스', departure: '15:15', buses: 2 },
  { no: 10, route: '기흥역 통학버스', departure: '16:15', buses: 3 },
  { no: 11, route: '기흥역 통학버스', departure: '17:15', buses: 5 },
  { no: 12, route: '기흥역 통학버스', departure: '18:15', buses: 2 },
  { no: 13, route: '기흥역 통학버스', departure: '19:15', buses: 1 },
  { no: 14, route: '기흥역 통학버스', departure: '08:15', buses: 3 },
  { no: 15, route: '기흥역 통학버스', departure: '08:20', buses: 2 },
  { no: 16, route: '기흥역 통학버스', departure: '09:15', buses: 3 },
  { no: 17, route: '기흥역 통학버스', departure: '09:20', buses: 2 },
  { no: 18, route: '기흥역 통학버스', departure: '10:15', buses: 3 },
  { no: 19, route: '기흥역 통학버스', departure: '10:20', buses: 2 },
  { no: 20, route: '기흥역 통학버스', departure: '12:15', buses: 1 },
  { no: 21, route: '기흥역 통학버스', departure: '13:15', buses: 1 },
  { no: 22, route: '기흥역 통학버스', departure: '14:15', buses: 1 },
  { no: 23, route: '기흥역 통학버스', departure: '15:30', buses: 2 },
  { no: 24, route: '기흥역 통학버스', departure: '16:30', buses: 3 },
  { no: 25, route: '기흥역 통학버스', departure: '17:30', buses: 1 },
  { no: 26, route: '기흥역 통학버스', departure: '18:30', buses: 1 },
  { no: 27, route: '기흥역 통학버스', departure: '19:30', buses: 1 },
];

export const MJU_ROUTES = [
  {
    name: '기흥역 통학버스' as const,
    direction: '왕복',
    description:
      '학기 중 평일만 운행(계절학기·방학 중 제외). 명지대 버스 관리사무소 정류장(채플관 앞) → 기흥역 5번 출구 → 명지대 버스 관리사무소 정류장(채플관 앞). 편도 약 15분(교통상황에 따라 변동).',
    start_location: '채플관 앞',
    end_location: '채플관 앞',
    buses: '최대 5대',
    days: '학기중 평일',
    hours: '08:00 ~ 19:30',
    stopCount: 3,
  },
  {
    name: '명지대역 셔틀' as const,
    direction: '진입로(명지대역)',
    description:
      '학기(계절학기 포함) 중 평일 운행. 4대·50회. 18:10까지만 운행. 버스관리사무소 → 상공회의소 → 진입로(럭스나인 앞) → 경전철 명지대역 → 명지대역 사거리 정류장 → 진입로(역북동 주민센터) → 이마트 → 명진당 → 제3공학관 → 함박관 → 창조관 → 버스관리사무소.',
    start_location: '버스관리사무소',
    end_location: '버스관리사무소',
    buses: '4대',
    days: '학기중 평일 · 계절학기',
    hours: '08:00 ~ 18:10',
    stopCount: 12,
  },
  {
    name: '시내 셔틀' as const,
    direction: '시내',
    description:
      '학기(계절학기 포함) 중 평일만 운행. 주말·공휴일·방학은 「시내 셔틀 (주말·공휴일·방학)」과 겹치지 않는다. 1대·10회. 18:10까지만 운행. 버스관리사무소 → 상공회의소 → 진입로(럭스나인 앞) → 동부경찰서 중앙지구대 → 용인CGV → 중앙공영주차장 → 진입로(역북동 주민센터) → 이마트 → 제1공학관 → 제3공학관 → 함박관 → 창조관 → 버스관리사무소.',
    start_location: '버스관리사무소',
    end_location: '버스관리사무소',
    buses: '1대',
    days: '학기중 평일',
    hours: '08:05 ~ 18:10',
    stopCount: 13,
  },
];

/** 변형 노선 (공휴일·주말·방학 시내) */
export const MJU_ROUTE_VARIANTS = [
  {
    name: '시내 셔틀 (주말·공휴일·방학)',
    direction: '시내',
    description:
      '주말·공휴일·방학 중 운행. 학기 중 평일 「시내 셔틀」과 겹치지 않는다. 1대·10회. 생활관(명현관) → 함박관 → 정문 → 상공회의소 → 진입로(럭스나인 앞) → 동부경찰서 중앙지구대 → 용인 CGV → 중앙공영주차장 → 경전철 명지대역 → 진입로(역북동 주민센터) → 이마트 → 제1공학관 → 생활관(명현관).',
    start_location: '생활관(명현관)',
    end_location: '생활관(명현관)',
  },
] as const;

export const MJU_TIMETABLE_PACKS: MjuTimetablePack[] = [
  {
    id: 'semester-giheung',
    title: '기흥역 통학버스 (학기 중 평일)',
    period: 'SEMESTER',
    daysLabel: '평일',
    weekdays: WEEKDAYS,
    semester: 'SEMESTER',
    trips: GIHEUNG_SEMESTER,
    note: '학교→기흥 13회 + 기흥→학교 14회 · 학기중 평일만',
  },
  {
    id: 'semester-shuttle',
    title: '명지대역·시내 셔틀 (학기 중 평일)',
    period: 'SEMESTER',
    daysLabel: '평일',
    weekdays: WEEKDAYS,
    semester: 'SEMESTER',
    trips: SEMESTER_SHUTTLE,
    note: '역북동 행정복지센터 경유 시각 포함',
  },
  {
    id: 'semester-shuttle-pm-ref',
    title: '명지대역·시내 셔틀 오후표 (학기 중)',
    period: 'SEMESTER',
    daysLabel: '평일',
    weekdays: WEEKDAYS,
    semester: 'SEMESTER',
    trips: SEMESTER_SHUTTLE.filter((t) => t.no >= 49),
  },
  {
    id: 'semester-route-info',
    title: '학기 중 노선 안내',
    period: 'SEMESTER',
    daysLabel: '평일',
    weekdays: WEEKDAYS,
    semester: 'SEMESTER',
    trips: [],
  },
  {
    id: 'seasonal-shuttle',
    title: '명지대역·시내 셔틀 (계절학기)',
    period: 'VACATION',
    daysLabel: '계절학기',
    weekdays: WEEKDAYS,
    semester: 'VACATION',
    trips: SEASONAL_SHUTTLE,
  },
  {
    id: 'seasonal-shuttle-pm',
    title: '명지대역·시내 셔틀 오후표 (계절학기)',
    period: 'VACATION',
    daysLabel: '계절학기',
    weekdays: WEEKDAYS,
    semester: 'VACATION',
    trips: SEASONAL_SHUTTLE.filter((t) => t.no >= 23),
  },
  {
    id: 'weekend-vacation-city',
    title: '시내 셔틀 (학기중 주말·공휴일·방학)',
    period: 'VACATION',
    daysLabel: '주말·공휴일·방학',
    weekdays: WEEKEND,
    semester: 'VACATION',
    trips: WEEKEND_VACATION_CITY,
    note: '생활관(명현관) 기점 순환 10회',
  },
];

/** schedules 테이블 insert용 — 노선별 출발시각 × 요일 × semester */
/** schedules insert용 — 18시 이후·주말/방학 시내를 변형 노선으로 분리 */
export function expandScheduleRows(): {
  routeName: ScheduleRouteName;
  departure_time: string;
  weekday: Weekday;
  semester: SemesterType;
}[] {
  const out: {
    routeName: ScheduleRouteName;
    departure_time: string;
    weekday: Weekday;
    semester: SemesterType;
  }[] = [];

  const push = (
    trips: MjuTrip[],
    weekdays: Weekday[],
    semester: SemesterType,
    routeName?: string,
  ) => {
    for (const t of trips) {
      for (const wd of weekdays) {
        const departure_time = `${t.departure}:00`;
        out.push({
          routeName: resolveOperationalRouteName({
            baseRouteName: routeName ?? t.route,
            departureTime: departure_time,
            weekday: wd,
            semester,
          }),
          departure_time,
          weekday: wd,
          semester,
        });
      }
    }
  };

  // 학기 중 평일 — 시내 셔틀은 여기만 (주말·방학 변형과 분리)
  push(GIHEUNG_SEMESTER, WEEKDAYS, 'SEMESTER');
  push(SEMESTER_SHUTTLE, WEEKDAYS, 'SEMESTER');
  // 계절학기 평일 (= 학기 중 평일 셔틀). 시내는 SEMESTER 쪽에만 두고,
  // VACATION semester의 시내는 주말·공휴일·방학 시간표만 사용한다.
  push(
    SEASONAL_SHUTTLE.filter((t) => t.route !== '시내 셔틀'),
    WEEKDAYS,
    'VACATION',
  );
  // 학기중 주말 · 방학 주말 · 방학 평일 → 시내 셔틀 (주말·공휴일·방학)
  push(WEEKEND_VACATION_CITY, WEEKEND, 'SEMESTER');
  push(WEEKEND_VACATION_CITY, WEEKEND, 'VACATION');
  push(WEEKEND_VACATION_CITY, WEEKDAYS, 'VACATION');

  return out;
}

export function summarizeRouteSchedule(
  route: MjuTripRoute,
  semester: SemesterType = 'SEMESTER',
) {
  const trips =
    route === '기흥역 통학버스'
      ? GIHEUNG_SEMESTER
      : route === CITY_SHUTTLE_VACATION_ROUTE_NAME
        ? WEEKEND_VACATION_CITY
        : semester === 'VACATION'
          ? SEASONAL_SHUTTLE.filter((t) => t.route === route)
          : SEMESTER_SHUTTLE.filter((t) => t.route === route);

  const times = trips.map((t) => t.departure).sort();
  return {
    rounds: trips.length,
    start: times[0] ?? '-',
    end: times[times.length - 1] ?? '-',
  };
}
