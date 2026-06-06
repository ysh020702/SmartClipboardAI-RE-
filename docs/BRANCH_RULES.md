# Branch Rules

## 기본 원칙

- `main` 브랜치에서 직접 작업하지 않습니다.
- 한 브랜치에는 하나의 task만 포함합니다.
- 한 PR에는 하나의 목적만 포함합니다.
- 선행 task가 `Done`이 아닌 작업은 시작하지 않습니다.
- 사용자 승인 전 commit/push를 하지 않습니다.

## 브랜치 네이밍

허용 prefix:

- `docs/...`
- `chore/...`
- `feat/...`
- `fix/...`
- `refactor/...`
- `test/...`

권장 형식:

```text
feat/T-300-topic-create-flow
docs/T-010-agents-and-docs-setup
fix/T-900-qa-build-test
```

## 공통 파일 수정 규칙

아래 파일 또는 영역을 수정해야 하면 PR 설명에 반드시 명시하고, 작업 전 사전 승인을 받습니다.

- `DataItem`
- `DataItemEntity`
- DAO
- Repository interface/implementation
- Navigation/Main screen contract
- `AndroidManifest.xml`
- Gradle 파일
- Theme
- Room database/migration

## PR 전 확인

- 최신 `main` 기준으로 rebase 또는 merge 상태를 확인합니다.
- 충돌이 있으면 PR 전에 해결합니다.
- 공통 파일을 수정했다면 PR에 이유와 영향 범위를 적습니다.
- 관련 task 문서의 체크리스트를 갱신합니다.
- 테스트/빌드 결과를 PR에 적습니다.

## 금지

- 여러 task를 한 브랜치에 섞지 않습니다.
- 문서 변경과 대규모 구현을 같은 PR에 섞지 않습니다.
- Done task의 파일을 새 task 승인 없이 다시 수정하지 않습니다.
- 다른 owner의 In Progress task 파일을 건드리지 않습니다.
