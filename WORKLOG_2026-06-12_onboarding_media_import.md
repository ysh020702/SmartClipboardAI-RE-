# 2026-06-12 온보딩 미디어 Import 작업일지

## 목표

권한 승인 이후 스크린샷 수집 진행 상태를 사용자에게 명확히 보여주고, AI가 수집 데이터를 기반으로 정리할 만한 주제를 추천한다는 핵심 기능으로 첫 실행 흐름을 연결한다.

## 진행 원칙

- 사용자 승인에 따라 `main` 브랜치에서 직접 수정한다.
- commit/push는 사용자 별도 승인 전 수행하지 않는다.
- 변경 이유, 변경 파일, 검증 결과를 이 파일에 계속 기록한다.
- 백그라운드 클립보드 감시, 화면 자동 감시, 사용자 확인 없는 외부 앱 실행은 구현하지 않는다.

## 수정 예정 파일

- `app/src/main/java/com/samsung/smartclipboard/presentation/main/MainActivity.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/permission/OnboardingMediaImportScreen.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/permission/PermissionScreen.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/home/HomeScreen.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/home/HomeViewModel.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/data/DataScreen.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/data/DataViewModel.kt`
- `app/src/main/java/com/samsung/smartclipboard/data/source/screenshot/ScreenshotImportHandler.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/MainScreen.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/home/HomeComponent.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/StorageScreen.kt`

## 진행 기록

- 2026-06-12: 작업 시작. 기존 흐름은 `PermissionScreen -> OnboardingDateScreen -> MainScreen`이며, 스크린샷 import가 `HomeScreen`과 `DataScreen` 진입 시 자동 실행되는 것을 확인했다.
- 2026-06-12: 이전 브라우저 목업 시도에서 생성한 `.superpowers/brainstorm/.../content/onboarding-concepts.html` 파일은 구현 범위와 무관한 임시 파일로 정리한다.
- 2026-06-12: `OnboardingMediaImportScreen.kt`를 추가했다. 포털/별빛 Canvas 애니메이션, 진행/완료/실패 문구, 계속하기 버튼을 포함한다.
- 2026-06-12: `MainActivity.kt`에서 DataStore 로딩 전 빈 화면을 추가하고, 권한 승인 후 `OnboardingMediaImportScreen`을 거치도록 진입 분기를 정리했다.
- 2026-06-12: `PermissionScreen.kt` 문구를 사진 권한 중심에서 "AI 주제 추천을 위한 스크린샷 자료 준비" 중심으로 변경했다.
- 2026-06-12: `HomeScreen.kt`, `DataScreen.kt` 진입 시 숨어서 실행되던 스크린샷 import 호출을 제거했다.
- 2026-06-12: `HomeViewModel.kt`, `DataViewModel.kt`에서 자동 import 전용 의존성과 메서드를 제거했다.
- 2026-06-12: `ScreenshotImportHandler.kt`에서 최근 미디어가 없는 경우를 오류가 아니라 성공 0건으로 반환하도록 조정했다. 온보딩에서 "새로 찾은 스크린샷 없음" 상태로 안내하기 위함이다.
- 2026-06-12: 1차 빌드에서 `OnboardingMediaImportScreen.kt`의 `AnimatedContent` 전환식이 기존 Compose API와 맞지 않아 실패했다.
- 2026-06-12: 기존 `AiTopicSelectionScreen.kt` 패턴과 동일하게 `togetherWith` 전환식을 수정했다.
- 2026-06-12: 이전 브라우저 목업 서버 상태 파일을 `.superpowers/`에서 정리했다.
- 2026-06-12: 온보딩 화면 전환이 즉시 교체되어 부자연스럽다는 피드백을 받았다. `MainActivity` 단계 전환을 `AnimatedContent`로 감싸고, 미디어 import 화면의 계속하기 버튼에는 짧은 exit 애니메이션을 추가한다.
- 2026-06-12: `MainActivity.kt`에 `MainEntryStep`과 단계별 `AnimatedContent` 전환을 추가했다. 권한 화면에서 미디어 import 화면으로 갈 때는 crossfade, 미디어 import에서 날짜 온보딩으로 갈 때는 fade + vertical slide를 사용한다.
- 2026-06-12: `OnboardingMediaImportScreen.kt`에 `isLeaving` 상태를 추가했다. 계속하기를 누르면 문구와 버튼이 먼저 사라지고, 포털 광휘가 커진 뒤 다음 화면으로 넘어간다.
- 2026-06-12: 설정 패널에서 설정 화면으로 이동할 때 패널 `Popup`과 `NavHost` 전환이 동시에 겹쳐 부자연스러운 원인을 확인했다.
- 2026-06-12: `HomeComponent.kt`에서 설정 클릭 시 패널을 먼저 닫고 짧은 지연 뒤 `Storage`로 이동하도록 변경했다.
- 2026-06-12: `MainScreen.kt`에 `homePanel -> storage`와 `storage -> home(openPanel)` 전용 전환을 추가했다. 기본 큰 horizontal slide 대신 짧은 fade와 약한 slide를 사용한다.
- 2026-06-12: `StorageScreen.kt`의 뒤로가기 복귀 파라미터를 `openPanel=instant`에서 `openPanel=true`로 바꿔, 홈 복귀 후 설정 패널이 즉시 붙지 않고 자연스럽게 열리도록 조정했다.
- 2026-06-12: 미디어 import 온보딩의 완료 문구가 중앙에 뜨고 계속하기 버튼이 필요한 흐름을 제거했다. 준비/진행/완료/실패 문구를 모두 하단 같은 위치에서 전환하도록 정리했다.
- 2026-06-12: `ScreenshotImportHandler.kt`에 진행률 콜백을 추가했다. 권한 확인, MediaStore 조회, 스크린샷 필터링, 기존 항목 확인, 저장 단계에 맞춰 포털 중앙 링이 실제 진행률을 반영한다.
- 2026-06-12: 로딩 중에는 "안녕하세요."와 "스크린샷을 살펴보는 중이에요." 문구가 1.5초 간격으로 번갈아 표시되도록 했다.
- 2026-06-12: 완료/실패 후 계속하기 버튼 대신 3, 2, 1 카운트다운을 보여주고 자동으로 다음 온보딩 단계로 넘어가도록 변경했다.

## 설계 메모

권한 승인 후 새 온보딩 화면을 보여준다.

```text
PermissionScreen
-> Android media permission
-> OnboardingMediaImportScreen
-> OnboardingDateScreen
-> MainScreen
```

수집 완료 문구는 AI 주제 추천 기능을 직접 가리키도록 한다.

```text
새 스크린샷 N개를 찾았어요

AI가 이 자료들을 바탕으로
정리할 만한 주제를 추천해드릴 수 있어요.
```

## 검증 결과

- `.\gradlew.bat assembleDebug` 1차 실행: 실패.
  - 원인: `OnboardingMediaImportScreen.kt`의 `AnimatedContent` 전환식에서 `togetherWith` receiver mismatch 발생.
- `.\gradlew.bat assembleDebug` 2차 실행: 성공.
  - 남은 출력: 기존 파일들의 `Icons.Filled.*` deprecation warning. 이번 작업으로 새로 추가한 실패는 없음.
- `.\gradlew.bat assembleDebug` 최종 실행: 성공.
  - `BUILD SUCCESSFUL in 8s`
- 화면 전환 개선 후 `.\gradlew.bat assembleDebug` 실행: 성공.
  - `BUILD SUCCESSFUL in 10s`
- 설정 화면 전환 개선 후 `.\gradlew.bat assembleDebug` 실행: 성공.
  - `BUILD SUCCESSFUL in 9s`
  - 남은 출력: `StorageScreen.kt`의 기존 `Icons.Filled.*` deprecation warning.
- 미디어 import 진행률/자동 전환 개선 후 `.\gradlew.bat assembleDebug` 실행: 성공.
  - `BUILD SUCCESSFUL in 19s`
  - 남은 출력: 기존 `Icons.Filled.*` deprecation warning.

## 남은 이슈

- 신규/거부/기존 사용자 진입 시나리오 수동 확인 필요.
- 실제 기기에서 권한 화면 → 포털 화면 → 기간 설정 화면 전환의 체감 부드러움 확인 필요.
- 실제 기기에서 설정 패널 → 설정 화면 → 홈 복귀 시 패널 재열림 전환의 체감 부드러움 확인 필요.
- 실제 기기에서 미디어 항목 수가 많을 때 진행률 링이 기대한 속도로 차오르는지 확인 필요.
