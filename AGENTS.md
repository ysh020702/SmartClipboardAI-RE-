# SmartClipboardAI AGENTS.md

이 문서는 SmartClipboardAI 레포에서 Codex와 모든 작업자가 반드시 따라야 하는 협업 규칙입니다. 개인 Codex 세션의 기억보다 `README.md`, `AGENTS.md`, `docs/` 문서를 우선합니다.

## 기본 작업 원칙

- 모든 설명, 작업 요약, PR 설명은 한국어로 작성합니다.
- Android Studio + Kotlin + Jetpack Compose 기준을 유지합니다.
- `main` 브랜치에서 직접 작업하지 않습니다.
- 기능별 브랜치에서만 작업합니다.
- 승인되지 않은 task를 시작하지 않습니다.
- 현재 task 범위 밖 파일을 수정하지 않습니다.
- 공통 파일 수정이 필요하면 먼저 이유를 설명하고 사용자 또는 프로젝트 오너에게 승인 요청합니다.
- 요청 범위 밖 대규모 리팩토링을 하지 않습니다.
- 완료된 task 또는 다른 사람이 담당한 task를 수정하지 않습니다.
- 전체 `docs/IMPLEMENTATION_PLAN.md`의 완료 체크는 프로젝트 오너가 관리합니다.
- 각 작업자는 자기 task 문서의 체크리스트만 갱신합니다.
- 사용자 승인 전 commit/push를 하지 않습니다.
- 구현 후 변경 파일, 테스트 결과, 남은 이슈, PR 작성 내용을 정리합니다.

## 작업 전 반드시 읽을 문서

작업자는 구현 또는 문서 변경 전 아래 문서를 읽어야 합니다.

1. `README.md`
2. `docs/PROJECT_SPEC.md`
3. `docs/ARCHITECTURE.md`
4. `docs/DATA_COLLECTION_STRATEGY.md`
5. `docs/UX_FLOW.md`
6. `docs/IMPLEMENTATION_PLAN.md`
7. `docs/BRANCH_RULES.md`
8. 담당 `docs/tasks/T-*.md`

## 금지 구현

아래 구현은 기술적으로 부적절하거나 정책 리스크가 있으므로 금지합니다.

- 백그라운드 클립보드 지속 감시
- 화면 내용 자동 감시
- 접근성 API를 통한 무단 수집
- 다른 앱의 공유 흐름 자동 가로채기
- 사용자 검토 없이 AI가 외부 앱에 일정, 노트, 리마인더를 바로 생성하는 흐름
- 클립보드 히스토리 전체를 Android 공개 API로 읽으려는 구현

## 작업 의존성 규칙

- Status가 `Ready`인 작업만 추천합니다.
- `Not Ready`, `Blocked`, `In Progress`, `Done` 상태의 작업은 시작하지 않습니다.
- `Depends on`에 적힌 선행 작업이 끝나지 않은 경우, 왜 지금 시작할 수 없는지 설명합니다.
- 의존성을 무시하고 구현하지 않습니다.
- 모든 관련 작업이 막혀 있다면 새 작업을 임의로 만들지 말고 blocker를 보고합니다.
- 사용자가 명시적으로 요청하지 않는 한 새 task를 만들지 않습니다.
- 사용자가 "다음에 뭐 해야 해?"라고 물으면 다음을 구분해서 답합니다.
  1. 지금 가능한 작업
  2. 아직 시작하면 안 되는 작업과 이유
  3. 추천하는 다음 작업

## 상태 변경 권한

- `Ready` 전환: 프로젝트 오너 또는 해당 phase 책임자가 승인합니다.
- `In Progress` 전환: 작업을 맡은 owner가 브랜치 생성 직전 또는 직후 표시합니다.
- `Blocked` 전환: 작업자가 blocker를 발견하면 즉시 기록합니다.
- `Done` 전환: PR merge 후 프로젝트 오너가 최종 표시합니다.

## 공통 파일 보호 규칙

다음 파일은 충돌 위험이 높으므로 task 문서에 명시되지 않으면 수정하지 않습니다.

- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/java/com/samsung/smartclipboard/domain/model/`
- `app/src/main/java/com/samsung/smartclipboard/data/model/`
- `app/src/main/java/com/samsung/smartclipboard/data/source/local/`
- `app/src/main/java/com/samsung/smartclipboard/domain/repository/DataRepository.kt`
- `app/src/main/java/com/samsung/smartclipboard/data/repository/DataRepositoryImpl.kt`
- `app/src/main/java/com/samsung/smartclipboard/presentation/main/`
- `app/src/main/java/com/samsung/smartclipboard/ui/theme/`
- `README.md`
- `AGENTS.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/ARCHITECTURE.md`

## 작업 완료 보고 형식

작업 후 아래 내용을 정리합니다.

- 작업한 task id
- 작업 브랜치
- 변경 파일 목록
- 테스트/빌드 결과
- 수동 확인 결과
- 남은 이슈
- 범위 밖 수정 여부
- PR에 적을 요약

## MVP 기준 결정

- MVP 1순위는 `Topic/Agent 초안 경험`입니다.
- 이미지/스크린샷은 새로 발견한 항목을 검토 후 저장합니다.
- `AiProposal`은 영구 DB 모델이 아니라 임시 추천 UI 상태로 봅니다.
- Cluster 정보는 `DataItem` / `DataItemEntity` 필드로 추가합니다.
- OCR은 로컬 알고리즘 개발 결과를 연동합니다.
- 나머지 AI/LLM 기능은 Gemini 연동을 전제로 합니다.
- Samsung Notes 연동은 초기 MVP에서 공유 초안 전달로 허용합니다.
