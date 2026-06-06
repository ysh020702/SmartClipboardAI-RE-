# AI Agent Pipeline 작업 요약

> 작성일: 2026-06-01
> 기준 커밋: `2d68d6f feat: add mobile agent pipeline foundation`
> 기준 문서: `docs/AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`, `docs/AGENT_MOBILE_IMPLEMENTATION_HARNESS.md`

---

## 1. 작업 결론

이번 작업은 SmartClipboardAI에 모바일 AI Agent 파이프라인의 기반을 추가한 작업이다.

기존 앱은 사용자가 저장한 `DataItem`을 목록, Topic, 분석 초안 중심으로 다루고 있었다. 이번 변경으로 앱 안에 별도의 `AI` 탭과 에이전트 세션 구조가 추가되었고, 사용자는 주제를 입력하거나 추천 주제를 선택해 다음 흐름을 시작할 수 있게 되었다.

```text
주제 입력 또는 추천 주제 선택
    -> 검색 계획 생성
    -> 저장된 DataItem 검색
    -> 후보 아이템 점수화 및 추천
    -> 사용자가 아이템 선택/해제
    -> 선택 아이템 기반 Action 초안 생성
    -> 사용자가 Action 후보 확인 및 보완 요청
```

코드 관점에서는 아래 기반이 추가되었다.

- `domain/agent`: Topic planning, item recommendation, action planning, refinement, cluster topic suggestion 계약
- `domain/retrieval`: 로컬 데이터 검색과 후보 점수화 계약
- `domain/tool`: Tool registry, routing, execution 계약
- `domain/model`: `AgentSession`, `AgentSessionState`, `CandidateItem`, `RetrievalPlan`, `DataCluster`, `SuggestedTopic`, `ToolSpec`, `ToolExecutionResult` 등 세션/도구/추천 모델
- `data/agent`: Gemini 실패 또는 오프라인 상황에서 사용할 fallback agent
- `data/gemini`: Gemini 응답을 구조화하기 위한 prompt/parser/agent 구현
- `data/retrieval`: 로컬 검색, 후보 점수화, 로컬 클러스터링 구현
- `data/tool`: 도구 목록, action-to-tool routing, Android 실행 구현
- `presentation/agent`: 에이전트 세션 화면, 추천 주제 화면, 후보 선택 화면, action 후보 화면, 실행 확인/결과 화면
- `di/AgentModule.kt`: 새 에이전트 구성요소의 Hilt 주입
- `presentation/main`: 하단 `AI` 탭과 `MainScreenMode.AGENT` 연결
- `app/src/test`: 상태 모델, JSON parser, fallback/refine, tool router 중심 단위 테스트

현재 단계의 성격은 "최종 제품 기능 완성"이라기보다, 이후 각 단계가 붙을 수 있는 모바일 에이전트 골격을 세운 것이다. 주제 입력부터 후보 추천과 Action 초안 생성까지의 흐름은 사용자 화면으로 이어졌고, 추천 주제 생성도 AI 탭 안에서 노출된다.

중요한 제한도 있다. `ToolRouterImpl`과 `ToolExecutorImpl`은 구현되어 있지만, `AgentSessionViewModel` 안의 `routeSelectedAction()`, `confirmExecution()`, `cancelExecution()`, `finishObservation()`, `runAnotherAction()`은 아직 실제 상태 전이를 수행하지 않는 stub 상태다. 따라서 사용자가 Action 후보를 선택한 뒤 실제 Tool 실행까지 이어지는 마지막 연결은 후속 작업에서 완성해야 한다.

---

## 2. 사용자가 보는 사용 흐름

### 2-1. AI 탭 진입

사용자는 앱 하단 내비게이션에서 `AI` 탭을 누른다.

`MainScreenMode.AGENT`가 선택되면 `AgentSessionScreen`이 표시된다. 이 화면은 두 가지 진입 방식을 제공한다.

1. 사용자가 직접 주제를 입력한다.
2. 앱이 저장된 데이터를 분석해 제안한 추천 주제를 선택한다.

### 2-2. 직접 주제 입력 흐름

사용자는 `TopicInputScreen`에서 예를 들어 다음과 같은 주제를 입력한다.

```text
회의 준비 자료 정리
```

시작 버튼을 누르면 에이전트 세션이 만들어지고 `AgentSessionState.PlanningRetrieval` 상태로 들어간다. 이 상태에서 TopicPlanner가 검색 계획을 만든다.

검색 계획에는 다음 값이 들어간다.

- 검색 키워드 목록
- 검색할 `DataItemType` 필터
- 최근 며칠 이내 데이터만 볼지에 대한 날짜 범위
- 최대 검색 결과 개수

Gemini가 정상 응답하면 Gemini 기반 계획을 사용하고, 실패하면 `FallbackTopicPlanner`가 주제 문자열에서 키워드와 타입 힌트를 추출한다.

### 2-3. 후보 데이터 추천

검색 계획이 만들어지면 `LocalDataRetriever`가 `DataRepository.observeItems()`로 현재 저장된 모든 `DataItem`을 가져온다. 이후 키워드, 타입, 날짜 범위 조건으로 후보를 추린다.

그 다음 `LocalCandidateItemRanker`가 각 후보에 `relevanceScore`와 `relevanceReason`을 붙인다.

사용자는 `CandidateItemSelectionScreen`에서 추천된 데이터를 본다.

사용자가 할 수 있는 일은 다음과 같다.

- 추천된 후보를 선택하거나 해제한다.
- 추천된 항목을 모두 선택한다.
- 선택을 초기화한다.
- 추천 이유와 관련 검색어를 확인한다.
- 선택한 데이터로 다음 단계로 넘어간다.

이 단계에서 최종 데이터 선택권은 사용자에게 있다. AI는 후보를 추천하지만, 어떤 자료를 사용할지는 사용자가 결정한다.

### 2-4. Action 후보 생성

사용자가 하나 이상의 후보 데이터를 선택하고 다음 단계로 넘어가면 `ActionPlanner`가 실행된다.

Gemini가 성공하면 Gemini가 선택 데이터에 맞는 Action 초안을 만든다. 실패하면 `FallbackActionPlanner`가 로컬 규칙으로 기본 Action 후보를 만든다.

생성 가능한 Action 유형은 기존 `TopicActionType`을 따른다.

- `SUMMARY`: 선택한 자료 요약
- `SHARE_DRAFT`: 공유 가능한 텍스트 초안
- `TODO`: 할 일 목록
- `CALENDAR`: 날짜/시간 단서가 있을 때 일정 초안
- `REMINDER`: 후속 알림 초안

사용자는 `ActionCandidateScreen`에서 Action 후보를 확인하고 하나를 선택할 수 있다.

### 2-5. Action 보완 요청

이번 작업에는 사용자 피드백 기반 보완 흐름도 들어갔다.

Action 후보가 마음에 들지 않으면 사용자는 보완 요청을 입력할 수 있다.

예시는 다음과 같다.

```text
회의 안건 중심으로 다시 정리하고, 일정 후보는 빼줘.
```

`RefineAgent`는 기존 Action 후보, 선택된 아이템, 검색 계획, 사용자 피드백을 함께 받아 Action 후보를 다시 만든다. 보완 결과가 비어 있거나 유효하지 않으면 기존 Action 후보를 유지하고 오류 메시지를 표시한다.

### 2-6. 추천 주제 선택 흐름

AI 탭의 초기 화면에는 `ClusterSuggestionScreen`도 함께 표시된다.

이 화면은 저장된 `DataItem`을 기반으로 추천 주제를 만든다.

흐름은 다음과 같다.

1. `ClusterSuggestionViewModel`이 저장된 `DataItem` 전체를 읽는다.
2. `LocalClusterer`가 비슷한 데이터를 묶는다.
3. `ClusterTopicAgent`가 각 묶음에서 사용자가 시킬 만한 주제 후보를 만든다.
4. 화면에는 `SuggestedTopic` 카드가 표시된다.
5. 사용자가 추천 주제를 누르면 해당 제목으로 에이전트 세션이 바로 시작된다.

추천 주제는 자동 확정되는 Topic이 아니다. 사용자가 눌러야 Flow A가 시작된다.

---

## 3. 사용 시 데이터 흐름

### 3-1. 입력 데이터

에이전트가 사용하는 원천 데이터는 기존 앱에 저장된 `DataItem`이다.

`DataItem`은 텍스트, 링크, 이미지, 파일, 스크린샷을 같은 형태로 다루기 위한 공통 모델이다.

```text
DataItem
  - id
  - type
  - content
  - title
  - source
  - mimeType
  - createdAt
```

이번 작업은 `DataItem` 자체의 DB 구조를 바꾸지 않았다. 즉, Room migration이나 `DataItemEntity` 변경 없이 기존 저장 데이터를 읽어 에이전트 흐름에 사용한다.

### 3-2. Flow A: 주제 입력 기반 에이전트 데이터 흐름

```text
사용자 주제 문자열
    |
    v
TopicPlanner
    |
    v
RetrievalPlan
    |
    v
LocalDataRetriever
    |
    v
List<DataItem>
    |
    v
LocalCandidateItemRanker
    |
    v
List<CandidateItem>
    |
    v
ItemRecommendationAgent
    |
    v
ItemRecommendationResult
    |
    v
사용자 선택/해제
    |
    v
ActionPlanner
    |
    v
List<AgentActionDraft>
```

각 단계의 역할은 다음과 같다.

| 단계 | 입력 | 출력 | 설명 |
| --- | --- | --- | --- |
| `TopicPlanner` | 사용자 주제 | `RetrievalPlan` | 어떤 키워드, 타입, 기간으로 검색할지 정한다. |
| `LocalDataRetriever` | `RetrievalPlan` | `List<DataItem>` | 저장된 전체 데이터에서 조건에 맞는 후보를 찾는다. |
| `LocalCandidateItemRanker` | 주제, 계획, 후보 데이터 | `List<CandidateItem>` | 후보별 관련도 점수와 추천 이유를 만든다. |
| `ItemRecommendationAgent` | 후보 목록 | `ItemRecommendationResult` | 사용자에게 보여줄 추천 목록, 기본 선택값, 추천 이유, 제안 검색어를 만든다. |
| 사용자 선택 | 추천 후보 | `selectedItemIds` | 사용자가 실제로 사용할 자료를 고른다. |
| `ActionPlanner` | 선택된 후보 | `List<AgentActionDraft>` | 요약, 공유 초안, 할 일, 일정, 리마인더 후보를 만든다. |

### 3-3. Flow B: 추천 주제 생성 데이터 흐름

```text
전체 DataItem
    |
    v
LocalClusterer
    |
    v
List<DataCluster>
    |
    v
ClusterTopicAgent
    |
    v
DataCluster(topicCandidates 포함)
    |
    v
List<SuggestedTopic>
    |
    v
사용자가 추천 주제 선택
    |
    v
Flow A 시작
```

`LocalClusterer`는 최대 300개의 최근 아이템을 대상으로 텍스트 토큰을 만들고, Jaccard 유사도와 타입/출처 보정을 사용해 비슷한 아이템을 묶는다.

클러스터가 만들어지면 `ClusterTopicAgent`가 각 클러스터를 사람이 이해할 수 있는 주제 후보로 바꾼다. Gemini가 실패하거나 응답 검증이 실패하면 `FallbackClusterTopicAgent`가 로컬 규칙으로 다음과 같은 후보를 만든다.

- `{클러스터 라벨} 정리하기`
- `{클러스터 라벨}로 할 일 만들기`
- `{클러스터 라벨} 공유 초안 만들기`

### 3-4. Tool 실행 데이터 흐름

이번 작업에서 Tool 계층은 아래 흐름으로 설계 및 구현되었다.

```text
AgentActionDraft
    |
    v
ToolRouterImpl
    |
    v
ToolRouteResult
  - action
  - toolSpec
  - resolvedPayload
  - missingRequiredInputs
    |
    v
ExecutionConfirmSheet
    |
    v
ToolExecutorImpl
    |
    v
ToolExecutionResult
```

현재 등록된 도구는 다음과 같다.

| toolName | 목적 | 실행 방식 | 사용자 확인 |
| --- | --- | --- | --- |
| `copy_to_clipboard` | 텍스트 복사 | `ClipboardManager` | 낮은 위험, 확인 불필요 |
| `share_text` | 공유 초안 전달 | `Intent.ACTION_SEND` | 확인 필요 |
| `open_url` | URL 열기 | `Intent.ACTION_VIEW` | 확인 필요 |
| `compose_email` | 이메일 초안 작성 | `Intent.ACTION_SENDTO` + `mailto:` | 확인 필요 |
| `save_note` | 내부 노트 저장 | 아직 Repository 저장 API 미연결 | 현재 실행 실패 결과 반환 |

주의할 점은 Tool 계층 구현과 화면 상태 모델은 들어왔지만, `AgentSessionViewModel`에서 Action 선택 후 Tool routing 및 실행으로 넘어가는 함수들이 아직 stub라는 점이다. 따라서 현재 앱에서 완전한 end-to-end Tool 실행을 기대하면 안 된다. 후속 작업은 이 연결부를 채우는 것이 자연스럽다.

---

## 4. 사용 결과

### 4-1. 사용자가 얻는 결과

사용자는 AI 탭에서 다음 결과를 얻는다.

- 입력한 주제에 맞는 수집 데이터 후보
- 후보별 관련도 점수와 추천 이유
- AI가 기본으로 선택한 추천 데이터
- 사용자가 직접 조정한 최종 선택 데이터
- 선택 데이터 기반 Action 초안
- Action 후보에 대한 보완 요청 및 재생성 결과
- 저장된 데이터 전체를 기반으로 한 추천 주제 카드

### 4-2. 앱 내부에 남는 결과

현재 `AgentSession`은 MVP 기준으로 메모리 상태에서 관리된다. DB에 영구 저장되지는 않는다.

세션 내부에는 다음 값이 유지된다.

- `sessionId`
- `topicTitle`
- 현재 `AgentSessionState`
- 검색/추천된 `candidateItems`
- 생성된 `actionDrafts`
- 선택된 action index
- tool 실행 결과 목록
- 생성/수정 시각

현재 단계에서는 Tool 실행 결과까지 ViewModel에서 연결되지 않았으므로, `toolResults`가 실제로 쌓이는 흐름은 후속 구현 대상이다.

### 4-3. 테스트 및 검증 결과

직전 커밋 전 아래 검증을 통과했다.

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest
```

결과:

```text
BUILD SUCCESSFUL
```

추가된 테스트는 다음 범위를 확인한다.

- `AgentSessionState` 상태 모델
- 도메인 모델 생성
- Gemini action planner JSON parser
- Gemini item recommendation JSON parser
- fallback refine agent
- tool router

---

## 5. 현재 완성된 것과 남은 것

### 완성된 기반

- AI 탭 진입점 추가
- 에이전트 세션 상태 모델 추가
- 주제 입력 기반 검색 계획 생성 구조 추가
- 로컬 DataItem 검색 및 관련도 점수화 추가
- Gemini agent + fallback agent 구조 추가
- 추천 주제 생성을 위한 로컬 클러스터링 및 Gemini/fallback 주제 추천 구조 추가
- Action 후보 화면 및 보완 요청 화면 추가
- Tool registry/router/executor 계층 추가
- Hilt `AgentModule`로 새 구성요소 주입
- 단위 테스트 기반 추가

### 아직 남은 연결

- `AgentSessionViewModel`의 Tool routing/execution 상태 전이 구현
- `ToolExecutionResult`를 세션과 UI에 실제로 누적하는 흐름
- `save_note` 도구의 실제 저장 API 연결
- Calendar 전용 초안 실행 흐름
- 추천 주제 자동 생성 시점 조정
- 실제 사용자 시나리오 기준 수동 QA
- 장기적으로 `AgentSession` 영구 저장 여부 결정

---

## 6. 사람이 이해할 수 있는 한 줄 요약

이번 작업은 SmartClipboardAI가 단순히 데이터를 저장하는 앱에서, 사용자가 입력한 주제와 저장된 데이터를 바탕으로 "무엇을 할 수 있는지"를 단계적으로 제안하는 모바일 AI Agent 앱으로 넘어가기 위한 첫 번째 큰 기반 작업이다.

사용자는 AI 탭에서 주제를 입력하거나 추천 주제를 누르고, 앱은 관련 자료를 찾아 보여주며, 사용자는 자료를 고른 뒤 실행 가능한 초안을 받는다. 아직 마지막 실행 연결은 남아 있지만, 검색, 추천, 선택, 초안 생성, 보완, 주제 추천을 담는 핵심 구조는 들어왔다.
