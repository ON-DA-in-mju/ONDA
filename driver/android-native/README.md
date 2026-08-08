# ON-DA Driver (Android Native)

Flutter 프로젝트를 참고용으로 유지한 채, Kotlin + Jetpack Compose로 전환한 Android 앱입니다.

## 구조

- `android-native/` — Android Studio 프로젝트 (이 폴더)
- 상위 `lib/`, `assets/` — 기존 Flutter 코드 (삭제하지 않음)

## 기술 스택

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- MVVM (`ViewModel` + `StateFlow`)

## 실행 방법

1. Android Studio에서 `android-native` 폴더를 Open
2. Sync Gradle
3. 에뮬레이터/실기기에서 Run

또는:

```bash
cd android-native
.\gradlew.bat :app:installDebug
```

## Mock 로그인 (관리자 웹 사용자 관리와 동일)

- `user01` / `1234` — 박사용
- `user02` / `1234` — 최사용
- `user03` / `1234` — 정사용
- `user04` / `1234` — 한사용
- `user05` / `1234` — 임사용

## 화면 진행

1. 로그인 (DRI-00-01) — 구현 완료, 확인 대기
2. 이후 화면은 확인 후 순서대로 구현
