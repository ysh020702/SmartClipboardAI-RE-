# Implementation Plan

이 문서는 현재 구현 기준을 가리키는 짧은 작업 인덱스입니다.
과거 T-* task 표는 Multi Retrieval Agent 구현 방향과 충돌할 수 있어 제거했습니다.

## 현재 기준 문서

- 주 구현 기준: `docs/AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`
- 프로젝트 목적과 MVP 원칙: `docs/PROJECT_SPEC.md`
- 앱 구조와 공통 파일 주의사항: `docs/ARCHITECTURE.md`
- 수집 정책과 Android 제약: `docs/DATA_COLLECTION_STRATEGY.md`
- 사용자 흐름: `docs/UX_FLOW.md`
- 브랜치와 PR 규칙: `docs/BRANCH_RULES.md`
- UI 색상 기준: `docs/one-ui-palette.md`

## 구현 대상

현재 구현 대상은 `AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`의 Multi-LLM Agent Pipeline입니다.

핵심 흐름:

1. 사용자가 Topic 제목을 입력합니다.
2. Planner/Retriever가 관련 `DataItem` 후보를 검색하고 추천합니다.
3. 사용자가 후보를 선택하거나 제외합니다.
4. Action Drafter가 선택된 자료로 실행 가능한 초안을 생성합니다.
5. Tool Executor가 사용자 확인 뒤 Android Intent 또는 내부 저장 흐름으로 연결합니다.
6. Cluster Agent는 온디맨드 분석으로 자료 묶음과 Topic 후보를 제안합니다.

## 권장 구현 순서

`AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`의 Phase 순서를 따릅니다.

1. Phase A: 도메인 모델과 인터페이스 정의
2. Phase B: PlannerAgent와 로컬 Retriever 구현
3. Phase D: ToolExecutor 계약과 Intent 실행 구현
4. Phase E: AgentPipelineViewModel과 AgentPipelineScreen 통합
5. Phase C: LocalClusterAgent와 GeminiClusterRefiner 구현
6. Phase F: ClusterViewModel과 ClusterScreen 통합

MVP는 먼저 P0 범위인 Planner, Retriever, Action Drafter 연동, Tool Executor, Agent Pipeline UI를 완성합니다.
분석 에이전트와 클러스터 주제 추천은 P1로 진행합니다.

## 작업 금지 기준

- `docs/AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`와 충돌하는 과거 Single-Turn 가이드나 임의 UX 시뮬레이션을 구현 기준으로 삼지 않습니다.
- 사용자 확인 없이 외부 앱에 일정, 노트, 리마인더를 바로 생성하지 않습니다.
- 백그라운드 클립보드 지속 감시, 화면 자동 감시, 접근성 API 기반 무단 수집을 구현하지 않습니다.
- 공통 파일을 수정해야 하면 작업 범위와 이유를 먼저 설명하고 승인받습니다.

## 완료 보고 기준

작업 후 아래 내용을 한국어로 정리합니다.

- 구현한 Phase 또는 세부 작업
- 작업 브랜치
- 변경 파일 목록
- 테스트/빌드 결과
- 수동 확인 결과
- 남은 이슈
- 범위 밖 수정 여부
- PR에 적을 요약

