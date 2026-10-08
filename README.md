# 🚌 ON-DA | 셔틀버스 통합 안내 시스템

**명지대학교 자연캠퍼스 셔틀버스를 위한 실시간 운행 정보 및 통합 관리 서비스**

학생용 Android 앱, 기사용 Android 앱, 관리자용 웹을 연동하여 셔틀버스의 실시간 위치 확인부터 운행 관리, 공지사항 및 알림까지 하나의 시스템으로 제공합니다.

🏆 **명지대학교 창의적 SW프로그램 경진대회 대상 수상**

## 📌 Project Overview

기존 셔틀버스 이용 과정에서 발생하는 운행 시간표와 실제 도착 시간의 차이, 실시간 위치 정보 부족, 공지사항 분산 등의 문제를 해결하기 위해 개발했습니다.

| 항목 | 내용 |
|---|---|
| Project | ON-DA |
| Type | Team Project |
| Platform | Android App / Web |
| Target | 명지대학교 셔틀버스 이용 학생 및 운영 관리자 |
| Achievement | 명지대학교 창의적 SW프로그램 경진대회 대상 |

## ✨ Key Features

### 🎓 Student App
- 실시간 셔틀버스 위치 및 운행 상태 확인
- 정류장별 셔틀버스 진행 상황 확인
- 운행 관련 공지사항 조회
- 셔틀버스 운행 알림 수신
- 커뮤니티를 통한 셔틀버스 관련 건의사항 작성

### 🚌 Driver App
- 셔틀버스 운행 시작 및 종료
- GPS 기반 실시간 차량 위치 전송
- 운행 상태 및 정류장 진행 상황 관리

### 🖥️ Admin Web
- 셔틀버스 운행 일정 및 노선 관리
- 실시간 차량 위치 및 운행 현황 관제
- 차량, 기사 및 정류장 정보 관리
- 공지사항 등록 및 운영 현황 관리

## 🛠️ Tech Stack

**Android**

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

**Web**

![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)

**Backend & Database**

![Supabase](https://img.shields.io/badge/Supabase-3FCF8E?style=for-the-badge&logo=supabase&logoColor=white)

**Maps & Location**

![Naver Maps](https://img.shields.io/badge/Naver_Maps-03C75A?style=for-the-badge&logo=naver&logoColor=white)

## 🏗️ System Architecture

```text
Student App ──────┐
                  │
Driver App ───────┼── Supabase
                  │   ├── Database
Admin Web ────────┘   ├── Authentication
                      └── Realtime

Driver GPS → Location Update → Realtime Sync
                              ├── Student App
                              └── Admin Web
```

## 📂 Repository Structure

```text
ONDA/
├── frontend/
│   ├── admin/       # 관리자 웹 (React + TypeScript)
│   ├── driver/      # 기사 앱 (Kotlin)
│   └── student/     # 학생 앱 (Kotlin)
├── docs/            # 프로젝트 문서
├── Docs/            # 명세서 및 제안서
├── image/           # 이미지 자료
└── README.md
```

## 👥 Team

**Team Name: ON-DA**

| Member | GitHub |
|---|---|
| 이다윤 | [@dayun6530](https://github.com/dayun6530) |
| 정윤호 | [@Yuno-Jung](https://github.com/Yuno-Jung) |
| 구자중 | [@wkwndrn](https://github.com/wkwndrn) |
