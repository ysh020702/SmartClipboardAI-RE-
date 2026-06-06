# Agent Mobile Implementation Harness

> **문서 버전**: 1.0
> **작성일**: 2026-06-01
> **대상 프로젝트**: SmartClipboardAI (Android + Kotlin + Jetpack Compose + Hilt + Room)
> **문서 목적**: 이후 모든 모바일 에이전트 구현 단계에서 참조할 "구현 하네스" 역할. 이 문서는 코드를 수정하지 않고 설계만 기술한다.

---

## 1. 목적

- **Flow A (사용자 주제 입력)**: 사용자가 주제만 입력하면 관련 DataItem을 찾고, 아이템을 추천/편집하게 한 뒤, 선택된 데이터로 가능한 작업을 생성하고, 사용자가 선택한 작업을 도구와 연결해 실행하는 구조를 구현한다.
- **Flow B (앱이 주제 추천)**: 전체 수집 데이터만으로 클러스터를 만들고, 에이전트에게 시킬 만한 추천 주제를 생성한다.
- **Mobile-Bounded Agent**: 완전한 Cline식 무제한 multi-retrieval이 아니라 Android 환경에 맞춘 Mobile-Bounded Agent로 구현한다.

### 핵심 원칙

1. 기존 앱 동작을 깨지 않는다
2. 기존 `TopicAgent` / `GeminiTopicAgent` / `DataRepositoryImpl` / UI 흐름을 바로 대규모 리팩터링하지 않는다
3. 기존 Single-Turn 분석 흐름은 유지하고, 새 에이전트 플로우는 별도 확장 구조로 설계한다
4. LLM은 실제 실행자가 아니라 Planner / Task Generator / Tool Router / Critic / Cluster Labeler 역할로만 둔다
5. DB 검색, 권한 확인, payload 검증, 실제 Tool 실행은 Kotlin 코드가 담당한다
6. 모든 LLM 출력은 DTO 또는 JSON schema로 검증 가능한 구조여야 한다

---

## 2. 핵심 사용자 플로우

### Flow A: 사용자가 주제 입력

```
1. TopicInput          → 사용자가 주제 문자열 입력
2. RetrievalPlan 생성   → TopicPlanner가 검색 계획 수립 (LLM)
3. Local DataItem 검색  → DataRetriever가 DB에서 후보 검색 (Kotlin)
4. CandidateItem 추천   → CandidateItemRanker가 relevance score 부여 (LLM)
5. 사용자 선택/편집      → CandidateItemSelectionScreen에서 선택/제외/편집
6. "다음으로"            → 사용자가 다음 단계로 진행
7. ActionDraft 생성     → ActionPlanner가 선택된 아이템으로 Action 후보 생성 (LLM)
8. 작업 선택             → ActionCandidateScreen에서 사용자가 실행할 작업 선택
9. Tool routing/validation → ToolRouter가 적절한 ToolSpec 매핑 (Kotlin)
10. 실행 전 확인         → ExecutionConfirmSheet에서 payload 표시 (Kotlin UI)
11. 실행                 → ToolExecutor가 Intent 실행 (Kotlin)
12. Observation log 저장 → ToolExecutionResult를 DB에 저장 (Kotlin)
```

### Flow B: 앱이 추천 주제 제안

```
1. 전체 DataItem 클러스터링  → DataRetriever + 로컬 알고리즘 (Kotlin)
2. 클러스터 라벨링           → ClusterTopicAgent가 각 클러스터에 label 부여 (LLM)
3. 추천 주제 생성            → ClusterTopicAgent가 SuggestedTopic 목록 생성 (LLM)
4. 사용자가 추천 주제 선택   → ClusterSuggestionScreen에서 선택
5. Flow A 1단계로 진입
```

---

## 3. 기존 코드베이스 파일맵

아래 표는 2026-06-01 기준 실제 코드베이스 검색 결과를 바탕으로 작성되었다.

| 역할 | 현재 파일 | 설명 | 변경 필요 여부 |
|---|---|---|---|
| **TopicAgent (인터페이스)** | `domain/ai/TopicAgent.kt` | `suspend fun analyze(topic, items, userInstruction?): Result<AgentResult>` — Single-Turn 분석 계약 | **유지** (삭제 금지) |
| **GeminiTopicAgent** | `data/ai/GeminiTopicAgent.kt` | `TopicAgent`의 Gemini 구현체. 프롬프트 빌드 → GeminiManager.run() → AgentJsonParser.parse() | **유지** (기존 분석 흐름 보존) |
| **GeminiManager (인터페이스)** | `domain/ai/GeminiManager.kt` | `suspend fun run(prompt: String): String` — 단일 LLM 호출 계약 | **유지** |
| **DefaultGeminiManager** | `data/ai/DefaultGeminiManager.kt` | OkHttp 기반 Gemini API 호출 (gemini-3.1-flash-lite 모델). `@Named("gemini_api_key")` 주입 | **유지** |
| **AgentJsonParser** | `data/ai/AgentJsonParser.kt` | Gemini JSON 응답 → AgentResult 변환. markdown fence 제거, kotlinx.serialization.json 사용 | **유지** (참고용. 새 Planner/Action Parser는 별도 구현) |
| **DataRepository (인터페이스)** | `domain/repository/DataRepository.kt` | `observeItems()`, `observeTopics()`, `addItemsToTopic()`, `runTopicAnalysis()`, `updateTopicActionDraft()` 등 | **확장** (필요한 검색/조회 메서드 추가는 M2에서 검토) |
| **DataRepositoryImpl** | `data/repository/DataRepositoryImpl.kt` | DataItemDao + TopicDao + TopicAgent 조합. `runTopicAnalysis()`에서 `topicAgent.analyze()` 호출 | **유지** (에이전트 플로우는 별도 ViewModel에서 처리) |
| **KnowledgeRepository (인터페이스)** | `domain/repository/KnowledgeRepository.kt` | 지식(Knack) 검색 및 저장 계약 | **유지** |
| **KnowledgeRepositoryImpl** | `data/repository/KnowledgeRepositoryImpl.kt` | Gemini + KnowledgeDao 조합 | **유지** |
| **MainViewModel** | `presentation/main/MainViewModel.kt` | `@HiltViewModel`. MainScreen 상태 관리. 아이템 선택/필터/핸드오프/토픽 분석 호출. `findCandidateItemsForPrompt()` 로컬 검색 포함 | **유지** (새 AgentPipelineViewModel은 별도 파일) |
| **KnowledgeViewModel** | `presentation/main/KnowledgeViewModel.kt` | 지식 화면 상태 관리 | **유지** |
| **MainScreen** | `presentation/main/MainScreen.kt` | Compose UI. HOME/DATA/TASKS/TOPIC_DETAIL 모드 | **유지** (새 에이전트 Screen은 별도 파일) |
| **MainContract** | `presentation/main/MainContract.kt` | MainIntent sealed class, MainUiState, MainScreenMode enum | **유지** |
| **AgentResult (도메인 모델)** | `domain/model/AgentResult.kt` | `AgentResult(topicId, summary, keyPoints, sourceItemIds, actions: List<AgentActionDraft>)` | **유지** |
| **AgentActionDraft (도메인 모델)** | `domain/model/AgentResult.kt` | `AgentActionDraft(type, confidence, reason, title, body, payload?, sourceItemIds)` | **유지** |
| **Topic (도메인 모델)** | `domain/model/Topic.kt` | `Topic(id, title, itemCount, createdAt, updatedAt)` | **유지** |
| **DataItem (도메인 모델)** | `domain/model/DataItem.kt` | `DataItem(id, type, content, title?, source?, mimeType?, createdAt)` | **유지** |
| **TopicActionType** | `domain/model/TopicAction.kt` | enum: SUMMARY, CALENDAR, REMINDER, SHARE_DRAFT, TODO | **유지** (필요시 확장) |
| **TopicActionStatus** | `domain/model/TopicAction.kt` | enum: DRAFT, EDITED, EXECUTED, DISMISSED | **유지** |
| **SmartClipboardDatabase** | `data/source/local/SmartClipboardDatabase.kt` | Room DB v5. entities: DataItemEntity, TopicEntity, TopicItemCrossRefEntity, TopicAnalysisEntity, TopicActionEntity, KnowledgeEntity | **유지** |
| **TopicDao** | `data/source/local/TopicDao.kt` | Topic CRUD, TopicItemCrossRef, TopicAnalysis, TopicAction 쿼리 | **유지** |
| **DataItemDao** | `data/source/local/DataItemDao.kt` | DataItem CRUD (추정, DataRepositoryImpl에서 사용) | **유지** |
| **KnowledgeDao** | `data/source/local/KnowledgeDao.kt` | KnowledgeEntity CRUD, fullTextSearch | **유지** |
| **AiModule (DI)** | `di/AiModule.kt` | GeminiManager, GeminiClient, SourceExtractor, KnowledgeRepository, TopicAgent 바인딩 + API Key 제공 | **확장** (새 Agent 바인딩 추가) |
| **AppModule (DI)** | `di/AppModule.kt` | Database, DAO, Repository, MediaImportHandler 제공 | **유지** |
| **Tests (unit)** | `app/src/test/` | ExampleUnitTest.kt만 존재. 실제 테스트 미구축 | **신규** (M12에서 구축) |
| **Tests (instrumented)** | `app/src/androidTest/` | ExampleInstrumentedTest.kt만 존재 | **신규** (M12에서 구축) |

---

## 4. 목표 패키지 구조

실제 프로젝트 패키지 구조(`com.samsung.smartclipboard`)에 맞춰 설계한다.

```
com.samsung.smartclipboard
├─ domain
│  ├─ agent                          ← 신규: 에이전트 도메인 인터페이스
│  │  ├─ TopicPlanner.kt             ← 검색 계획 수립 LLM 역할
│  │  ├─ ItemRecommendationAgent.kt  ← 아이템 추천 LLM 역할
│  │  ├─ ActionPlanner.kt            ← Action 후보 생성 LLM 역할
│  │  ├─ RefineAgent.kt              ← 피드백 기반 재생성 LLM 역할
│  │  └─ ClusterTopicAgent.kt        ← 클러스터 라벨링 + 주제 추천 LLM 역할
│  ├─ retrieval                       ← 신규: 로컬 검색 도메인
│  │  ├─ DataRetriever.kt            ← 인터페이스
│  │  ├─ RetrievalPlan.kt            ← LLM이 생성한 검색 계획 DTO
│  │  └─ CandidateItemRanker.kt      ← 인터페이스 (로컬 구현은 data 계층)
│  ├─ tool                            ← 신규: Tool 실행 도메인
│  │  ├─ ToolRegistry.kt             ← 인터페이스
│  │  ├─ ToolSpec.kt                 ← 도구 스펙 DTO
│  │  ├─ ToolRouter.kt              ← 인터페이스
│  │  └─ ToolExecutor.kt            ← 인터페이스
│  ├─ model                           ← 기존 + 신규 DTO
│  │  ├─ AgentResult.kt              ← 기존 유지
│  │  ├─ DataItem.kt                 ← 기존 유지
│  │  ├─ Topic.kt                    ← 기존 유지
│  │  ├─ TopicAction.kt              ← 기존 유지
│  │  ├─ TopicAnalysis.kt            ← 기존 유지
│  │  ├─ AgentSession.kt             ← 신규: 에이전트 세션 DTO
│  │  ├─ AgentSessionState.kt        ← 신규: 상태 머신 sealed class
│  │  ├─ CandidateItem.kt            ← 신규: 검색 + 점수 + 이유
│  │  ├─ AgentActionDraft.kt         ← 기존 유지 (AgentResult.kt에 있음)
│  │  ├─ ToolExecutionResult.kt      ← 신규: Tool 실행 결과
│  │  ├─ SuggestedTopic.kt           ← 신규: Flow B 추천 주제
│  │  ├─ DataCluster.kt              ← 신규: 클러스터 정보
│  │  └─ RetrievalPlan.kt            ← 신규 (retrieval 패키지 또는 model 패키지)
│  ├─ ai                              ← 기존 + 필요시 신규
│  │  ├─ TopicAgent.kt               ← 기존 유지
│  │  ├─ GeminiManager.kt            ← 기존 유지
│  │  ├─ GeminiClient.kt             ← 기존 유지
│  │  └─ SourceExtractor.kt          ← 기존 유지
│  └─ repository
│     ├─ DataRepository.kt           ← 기존 유지
│     └─ KnowledgeRepository.kt      ← 기존 유지
│
├─ data
│  ├─ gemini                           ← 신규: Gemini 구현체
│  │  ├─ GeminiPromptBuilder.kt       ← 공통 프롬프트 빌더
│  │  ├─ GeminiAgentJsonParser.kt     ← 공통 JSON 파서 유틸
│  │  ├─ GeminiTopicPlanner.kt        ← TopicPlanner 구현체
│  │  ├─ GeminiItemRecommendationAgent.kt ← ItemRecommendationAgent 구현체
│  │  ├─ GeminiActionPlanner.kt       ← ActionPlanner 구현체
│  │  └─ GeminiClusterTopicAgent.kt   ← ClusterTopicAgent 구현체
│  ├─ retrieval                        ← 신규: 로컬 검색 구현체
│  │  ├─ LocalDataRetriever.kt        ← DataRetriever 구현 (Room 검색)
│  │  └─ LocalCandidateItemRanker.kt  ← CandidateItemRanker 구현
│  ├─ tool                             ← 신규: Tool 실행 구현체
│  │  ├─ ToolRegistryImpl.kt          ← ToolRegistry 구현
│  │  ├─ ToolRouterImpl.kt            ← ToolRouter 구현
│  │  └─ ToolExecutorImpl.kt          ← ToolExecutor 구현
│  ├─ ai                               ← 기존 유지
│  │  ├─ GeminiTopicAgent.kt
│  │  ├─ DefaultGeminiManager.kt
│  │  ├─ DefaultGeminiClient.kt
│  │  ├─ DefaultSourceExtractor.kt
│  │  └─ AgentJsonParser.kt
│  ├─ repository                       ← 기존 유지 (+ 필요시 신규)
│  │  ├─ DataRepositoryImpl.kt
│  │  ├─ KnowledgeRepositoryImpl.kt
│  │  ├─ AgentRepositoryImpl.kt       ← 신규: AgentSession CRUD (필요시)
│  │  └─ ToolExecutionRepository.kt   ← 신규: Observation log 저장
│  ├─ model                            ← 기존 유지
│  │  ├─ DataItemEntity.kt
│  │  ├─ TopicEntity.kt
│  │  ├─ TopicItemCrossRefEntity.kt
│  │  ├─ TopicAnalysisEntity.kt
│  │  ├─ TopicActionEntity.kt
│  │  └─ KnowledgeEntity.kt
│  └─ source/local                     ← 기존 유지
│     ├─ SmartClipboardDatabase.kt
│     ├─ DataItemDao.kt
│     ├─ TopicDao.kt
│     ├─ KnowledgeDao.kt
│     └─ KeywordConverters.kt
│
├─ presentation
│  ├─ main                             ← 기존 유지
│  │  ├─ MainViewModel.kt
│  │  ├─ MainScreen.kt
│  │  ├─ MainContract.kt
│  │  └─ KnowledgeViewModel.kt
│  ├─ agent                            ← 신규: 에이전트 UI
│  │  ├─ AgentSessionViewModel.kt     ← Flow A 상태 관리
│  │  ├─ ClusterSuggestionViewModel.kt ← Flow B 상태 관리
│  │  ├─ TopicInputScreen.kt          ← 주제 입력
│  │  ├─ CandidateItemSelectionScreen.kt ← 아이템 선택/편집
│  │  ├─ ActionCandidateScreen.kt     ← Action 후보 표시
│  │  ├─ ExecutionConfirmSheet.kt     ← 실행 확인 바텀시트
│  │  └─ ClusterSuggestionScreen.kt   ← 클러스터/주제 추천
│  └─ handoff                          ← 기존 유지
│     └─ HandoffDraftFormatter.kt
│
└─ di
   ├─ AiModule.kt                      ← 확장 (새 바인딩)
   └─ AppModule.kt                     ← 유지
```

---

## 5. 상태 머신

### AgentSessionState 정의

```kotlin
sealed class AgentSessionState {
    /** 초기 상태. TopicInputScreen 표시 */
    data object Idle : AgentSessionState()

    /** LLM이 검색 계획 수립 중 */
    data object PlanningRetrieval : AgentSessionState()

    /** 로컬 DB에서 DataItem 검색 중 */
    data class RetrievingItems(val query: String, val progress: Float) : AgentSessionState()

    /** 검색 완료, CandidateItem 목록 표시. 사용자 선택 대기 */
    data class AwaitingItemSelection(
        val candidateItems: List<CandidateItem>,
        val recommendationReason: String,
        val suggestedQueries: List<String>,
        val selectedItemIds: Set<String>
    ) : AgentSessionState()

    /** Action 후보 생성 중 (LLM 호출) */
    data class GeneratingActions(val selectedItemCount: Int) : AgentSessionState()

    /** Action 후보 표시. 사용자 선택 대기 */
    data class AwaitingActionSelection(
        val actionDrafts: List<AgentActionDraft>,
        val selectedActionIndex: Int?
    ) : AgentSessionState()

    /** 선택된 Action에 맞는 Tool routing 중 */
    data class RoutingTool(val action: AgentActionDraft) : AgentSessionState()

    /** 실행 전 확인. payload 표시 */
    data class AwaitingExecutionConfirm(
        val action: AgentActionDraft,
        val toolSpec: ToolSpec,
        val resolvedPayload: Map<String, String>
    ) : AgentSessionState()

    /** Tool 실행 중 */
    data class Executing(val action: AgentActionDraft) : AgentSessionState()

    /** 실행 완료. 결과 표시 */
    data class Observing(val result: ToolExecutionResult) : AgentSessionState()

    /** 사용자 피드백 기반 재생성 */
    data class Refining(val feedback: String) : AgentSessionState()

    /** 전체 플로우 완료 */
    data class Completed(val sessionId: String) : AgentSessionState()

    /** 오류 발생 */
    data class Failed(val step: String, val message: String, val recoverable: Boolean) : AgentSessionState()
}
```

### 상태 전이 테이블

| 현재 상태 | 트리거 | 다음 상태 |
|---|---|---|
| Idle | 사용자 주제 입력 + "시작" | PlanningRetrieval |
| PlanningRetrieval | LLM 검색 계획 완료 | RetrievingItems |
| RetrievingItems | 로컬 검색 + LLM 추천 완료 | AwaitingItemSelection |
| AwaitingItemSelection | 사용자 "다음으로" | GeneratingActions |
| GeneratingActions | LLM Action 생성 완료 | AwaitingActionSelection |
| AwaitingActionSelection | 사용자 Action 선택 | RoutingTool |
| RoutingTool | ToolSpec 매핑 완료 | AwaitingExecutionConfirm |
| AwaitingExecutionConfirm | 사용자 "실행" 승인 | Executing |
| AwaitingExecutionConfirm | 사용자 "취소" | AwaitingActionSelection |
| Executing | Tool 실행 완료 | Observing |
| Observing | 사용자 "완료" | Completed |
| Observing | 사용자 "수정 요청" | Refining |
| Refining | LLM 재생성 완료 | AwaitingItemSelection (또는 AwaitingActionSelection) |
| 모든 상태 | 오류 발생 | Failed |
| Failed (recoverable=true) | 사용자 "재시도" | 이전 상태로 복귀 |
| Failed (recoverable=false) | 사용자 "취소" | Idle |
| Completed | 사용자 "새로 시작" | Idle |

---

## 6. 주요 도메인 모델 초안

### 6-1. AgentSession

장기 실행 에이전트 세션을 추적하는 모델.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| sessionId | String | No | AgentSessionViewModel, DB | Yes (DB PK) |
| topicTitle | String | No | 전체 Flow | Yes |
| state | AgentSessionState | No | ViewModel StateFlow | Yes (enum string) |
| candidateItems | List\<CandidateItem\> | No | 아이템 선택 단계 | Yes |
| actionDrafts | List\<AgentActionDraft\> | Yes | Action 선택 단계 | Yes |
| selectedActionIndex | Int? | Yes | Action 선택 단계 | No |
| toolResults | List\<ToolExecutionResult\> | Yes | Observation | Yes |
| createdAt | Long | No | DB | Yes |
| updatedAt | Long | No | DB | Yes |

### 6-2. RetrievalPlan

LLM이 생성하는 검색 계획.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| keywords | List\<String\> | No | DataRetriever | Yes |
| typeFilters | List\<DataItemType\>? | Yes | DataRetriever | Yes |
| dateRangeDays | Int? | Yes | DataRetriever | Yes |
| maxResults | Int | No | DataRetriever | Yes |

### 6-3. CandidateItem

검색/추천된 DataItem wrapper.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| item | DataItem | No | UI 표시 | Yes |
| relevanceScore | Float (0.0~1.0) | No | 정렬/표시 | Yes |
| relevanceReason | String | No | UI 설명 | Yes |

### 6-4. AgentActionDraft (기존 유지)

기존 `AgentResult.kt`에 정의된 모델을 재사용한다. 추가 필드는 기존 호환성을 깨지 않는 선에서만 허용.

### 6-5. ToolCandidate

ToolRouter가 생성하는 실행 후보.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| toolName | String | No | ToolRegistry | Yes |
| displayName | String | No | UI 표시 | No |
| description | String | No | UI 표시 | No |
| requiredInputs | List\<RequiredInput\> | No | 실행 전 검증 | Yes |
| riskLevel | String ("low"/"medium"/"high") | No | UI 표시 | No |

### 6-6. RequiredInput

Tool 실행에 필요한 입력 필드 정의.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| key | String | No | payload | Yes |
| label | String | No | UI | No |
| value | String? | Yes | payload (사용자 입력 또는 LLM 추출) | Yes |
| required | Boolean | No | 검증 | Yes |

### 6-7. ToolSpec

Tool 스펙 정의.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| toolName | String | No | ToolRegistry | Yes |
| description | String | No | 문서화 | No |
| riskLevel | String | No | 확인 UI | No |
| requiresConfirmation | Boolean | No | 실행 Flow | No |
| androidAction | String | No | Intent 생성 | No |
| requiredInputs | List\<RequiredInput\> | No | payload 검증 | Yes |

### 6-8. ToolExecutionResult

Tool 실행 결과.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| resultId | String | No | DB PK | Yes |
| sessionId | String | No | 연관 세션 | Yes |
| toolName | String | No | 로그 | Yes |
| success | Boolean | No | UI | Yes |
| message | String | No | UI | Yes |
| executedAt | Long | No | DB | Yes |
| errorDetail | String? | Yes | 디버깅 | Yes |

### 6-9. SuggestedTopic

Flow B에서 생성하는 추천 주제.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| suggestedTitle | String | No | UI | Yes |
| description | String | No | UI | Yes |
| confidence | Float (0.0~1.0) | No | UI | Yes |
| reason | String | No | UI | Yes |
| relatedClusterId | String? | Yes | 연관 클러스터 | Yes |

### 6-10. DataCluster

클러스터링 결과.

| 필드명 | 타입 | Nullable | 사용 위치 | Serialization |
|---|---|---|---|---|
| clusterId | String | No | 식별자 | Yes |
| clusterLabel | String | No | UI | Yes |
| itemIds | List\<Long\> | No | 연관 아이템 | Yes |
| topicCandidates | List\<SuggestedTopic\> | No | Flow B | Yes |
| generatedAt | Long | No | 타임스탬프 | Yes |

---

## 7. LLM 사용 지점과 금지 지점

### LLM을 사용하는 지점

| 역할 | 설명 | 호출 시점 | 모델 | Fallback |
|---|---|---|---|---|
| **TopicPlanner** | 주제 → 검색 키워드, type filter, 날짜 범위 생성 | TopicInput 후 | Gemini Flash-Lite | 키워드 분리 룰 기반 |
| **ItemRecommendationAgent** | 검색된 후보에 relevanceScore + 이유 부여 | RetrievingItems 후 | Gemini Flash-Lite | keyword match 점수만 사용 |
| **ActionPlanner** | 선택된 아이템 → ActionDraft 목록 생성 | "다음으로" 클릭 후 | Gemini Flash | 기존 GeminTopicAgent 재사용 |
| **ToolRouter (payload draft)** | ActionDraft → payload 값 추출 (필요한 경우만) | Action 선택 후 | Gemini Flash-Lite | 빈 payload + 사용자 입력 |
| **RefineAgent** | 사용자 피드백 기반 재생성 | Refining 상태 | Gemini Flash | 원본 결과 재사용 |
| **ClusterTopicAgent** | 클러스터 라벨링 + SuggestedTopic 생성 | Flow B 시작 시 | Gemini Flash | 클러스터 내 top word 라벨 |

### LLM을 사용하지 않는 지점

- **DB 검색**: Room Query / LIKE / FTS (Kotlin)
- **정렬/페이징**: Comparator / take(N) (Kotlin)
- **중복 제거**: distinctBy / content similarity (Kotlin)
- **권한 확인**: ContextCompat.checkSelfPermission (Android API)
- **payload validation**: RequiredInput 리스트 대조 (Kotlin)
- **실제 Tool 실행**: Intent 생성 + startActivity (Android API)
- **네트워크 재시도**: Exponential Backoff (Kotlin)
- **민감정보 마스킹**: Regex replace (Kotlin)
- **클러스터링 (1차)**: Jaccard 유사도 / Union-Find (Kotlin)

---

## 8. Tool Registry 초안

### 초기 도구

| toolName | 설명 | payload 필드 | riskLevel | requiresConfirmation | Android 구현 방식 | MVP 포함 |
|---|---|---|---|---|---|---|
| **save_note** | 내부 노트 저장 | noteTitle, noteBody, sourceItemIds | low | false | Room DB insert (TopicAction status=EXECUTED) | O |
| **copy_to_clipboard** | 텍스트 복사 | textToCopy | low | false | ClipboardManager.setPrimaryClip | O |
| **share_text** | 공유 시트 | shareTitle, shareText | low | true | Intent.ACTION_SEND + Intent.createChooser | O |
| **open_url** | URL 열기 | url | low | true | Intent.ACTION_VIEW + Uri.parse | O |
| **compose_email** | 이메일 초안 | to, subject, body | medium | true | Intent.ACTION_SENDTO + mailto: | O |

### 선택 도구 (MVP 이후)

| toolName | 설명 | payload 필드 | riskLevel | requiresConfirmation | Android 구현 방식 | MVP 포함 |
|---|---|---|---|---|---|---|
| **create_calendar_event** | 캘린더 일정 | eventTitle, eventDescription, startTime, endTime, location? | medium | true | Intent.ACTION_INSERT + CalendarContract | X (MVP 이후) |
| **open_map_search** | 지도 검색 | query, latitude?, longitude? | low | true | Intent.ACTION_VIEW + geo: | X |
| **create_internal_tasks** | 내부 할 일 | tasks: [{title, description, dueDate?}] | low | false | Room DB insert | X |

### 도구 실행 규칙

1. `requiresConfirmation = true`인 도구는 반드시 ExecutionConfirmSheet를 표시한다
2. `riskLevel = "high"`인 도구는 MVP에서 추가하지 않는다
3. payload 필드가 누락된 경우 사용자 입력을 요청한다 (LLM 추정값을 자동으로 사용하지 않음)
4. Tool 실행 실패 시 ToolExecutionResult.errorDetail에 원인을 기록하고 Observing 상태로 전환한다

---

## 9. 구현 마일스톤

### M0. Harness and Codebase Map ← **현재 단계**

- **목표**: 구현 하네스 문서(`AGENT_MOBILE_IMPLEMENTATION_HARNESS.md`) 확정
- **수정 예상 파일**: 없음 (문서만 생성)
- **새로 만들 파일**: `docs/AGENT_MOBILE_IMPLEMENTATION_HARNESS.md`
- **수용 기준**: 모든 섹션이 실제 코드베이스에 기반해 작성되었고, 빌드가 성공함
- **테스트 명령**: `./gradlew :app:compileDebugKotlin`
- **다음 단계로 넘겨야 할 정보**: M1에서 추가할 모델 파일 목록

### M1. AgentSession / DTO / enum 추가

- **목표**: `domain/model/`에 새 DTO와 상태 머신 추가. 기존 파일 수정 없음
- **수정 예상 파일**: 없음
- **새로 만들 파일**:
  - `domain/model/AgentSessionState.kt`
  - `domain/model/CandidateItem.kt`
  - `domain/model/RetrievalPlan.kt`
  - `domain/model/ToolSpec.kt`
  - `domain/model/RequiredInput.kt`
  - `domain/model/ToolExecutionResult.kt`
  - `domain/model/SuggestedTopic.kt`
  - `domain/model/DataCluster.kt`
- **수용 기준**: `./gradlew :app:compileDebugKotlin` 통과. 모든 새 DTO가 kotlinx.serialization 호환 (필요시 @Serializable)
- **테스트 명령**: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
- **다음 단계로 넘겨야 할 정보**: 새 DTO의 import 경로 목록

### M2. Local Retrieval / Candidate Ranking

- **목표**: DataRetriever (로컬 DB 검색)와 CandidateItemRanker (규칙 기반 점수) 구현
- **수정 예상 파일**: 없음 (또는 DataRepository에 getItemsByIds, searchItems 추가 검토)
- **새로 만들 파일**:
  - `domain/retrieval/DataRetriever.kt`
  - `domain/retrieval/CandidateItemRanker.kt`
  - `data/retrieval/LocalDataRetriever.kt`
  - `data/retrieval/LocalCandidateItemRanker.kt`
- **수용 기준**: `LocalDataRetriever`가 키워드/타입/날짜 필터로 검색 가능. `LocalCandidateItemRanker`가 score 0.0~1.0 반환
- **테스트 명령**:
  - `./gradlew :app:compileDebugKotlin`
  - `./gradlew :app:testDebugUnitTest --tests "*LocalDataRetriever*"`
- **다음 단계로 넘겨야 할 정보**: DataRetriever 검색 결과 포맷, Ranker 점수 기준

### M3. TopicPlanner

- **목표**: LLM에게 검색 키워드/필터를 생성하게 하는 TopicPlanner 구현
- **수정 예상 파일**: `di/AiModule.kt` (바인딩 추가)
- **새로 만들 파일**:
  - `domain/agent/TopicPlanner.kt`
  - `data/gemini/GeminiTopicPlanner.kt`
  - `data/gemini/GeminiPromptBuilder.kt` (또는 M4로 연기)
  - `data/gemini/GeminiAgentJsonParser.kt` (공통 유틸)
- **수용 기준**: 주제 입력 → 검색 키워드 3~5개 + type filter + maxResults 반환. JSON schema 검증 통과
- **테스트 명령**:
  - `./gradlew :app:compileDebugKotlin`
  - `./gradlew :app:testDebugUnitTest --tests "*GeminiTopicPlanner*"`
- **다음 단계로 넘겨야 할 정보**: 프롬프트 템플릿, JSON 스키마

### M4. ItemRecommendationAgent

- **목표**: LLM에게 CandidateItem relevance score 부여하게 하는 Agent 구현
- **수정 예상 파일**: `di/AiModule.kt` (바인딩 추가)
- **새로 만들 파일**:
  - `domain/agent/ItemRecommendationAgent.kt`
  - `data/gemini/GeminiItemRecommendationAgent.kt`
- **수용 기준**: 검색된 후보 리스트에 relevanceScore + 이유 부여. itemId 검증. 3~10개 반환
- **테스트 명령**: `./gradlew :app:compileDebugKotlin`
- **다음 단계로 넘겨야 할 정보**: 프롬프트 템플릿

### M5. AgentSessionViewModel / 아이템 선택 UI

- **목표**: Flow A의 전반부 (Idle → AwaitingItemSelection)를 담당하는 ViewModel + UI
- **수정 예상 파일**: 없음 (기존 MainViewModel/Screen 수정 금지)
- **새로 만들 파일**:
  - `presentation/agent/AgentSessionViewModel.kt`
  - `presentation/agent/TopicInputScreen.kt`
  - `presentation/agent/CandidateItemSelectionScreen.kt`
- **수용 기준**: 주제 입력 → 검색 진행 표시 → CandidateItem 목록 표시 → 체크박스 선택/해제 → "다음으로" 활성화
- **테스트 명령**: `./gradlew :app:compileDebugKotlin`
- **다음 단계로 넘겨야 할 정보**: Navigation 연결점 (어디서 AgentSessionScreen으로 진입하는지)

### M6. ActionPlanner / 작업 후보 UI

- **목표**: 선택된 아이템으로 ActionDraft를 생성하는 LLM Agent + UI
- **수정 예상 파일**: `di/AiModule.kt` (바인딩 추가)
- **새로 만들 파일**:
  - `domain/agent/ActionPlanner.kt`
  - `data/gemini/GeminiActionPlanner.kt`
  - `presentation/agent/ActionCandidateScreen.kt`
- **수용 기준**: AgentSessionState.GeneratingActions → AwaitingActionSelection 전이. Action 목록 표시 + 선택 가능
- **참고**: 기존 `GeminiTopicAgent`의 프롬프트를 ActionPlanner가 재사용하거나 확장할 수 있다
- **테스트 명령**: `./gradlew :app:compileDebugKotlin`
- **다음 단계로 넘겨야 할 정보**: ActionPlanner 프롬프트 템플릿

### M7. ToolRegistry / ToolRouter / ToolExecutor

- **목표**: Action을 실제 Android Intent로 실행하는 도구 체인 구현
- **수정 예상 파일**: `di/AiModule.kt` (바인딩 추가), `di/AppModule.kt` (Context 제공 검토)
- **새로 만들 파일**:
  - `domain/tool/ToolRegistry.kt`
  - `domain/tool/ToolSpec.kt` (M1에서 선행)
  - `domain/tool/ToolRouter.kt`
  - `domain/tool/ToolExecutor.kt`
  - `data/tool/ToolRegistryImpl.kt`
  - `data/tool/ToolRouterImpl.kt`
  - `data/tool/ToolExecutorImpl.kt`
- **수용 기준**: 5개 MVP 도구 등록. ActionDraft → ToolSpec 매핑. Intent 생성 및 실행
- **테스트 명령**:
  - `./gradlew :app:compileDebugKotlin`
  - `./gradlew :app:testDebugUnitTest --tests "*ToolRegistry*"`
- **다음 단계로 넘겨야 할 정보**: 등록된 ToolSpec 목록

### M8. 실행 확인 UI / Observation Log

- **목표**: ExecutionConfirmSheet + ToolExecutionResult 표시 UI
- **수정 예상 파일**: 없음
- **새로 만들 파일**:
  - `presentation/agent/ExecutionConfirmSheet.kt`
  - AgentSessionViewModel에 `executeAction()` + `confirmExecution()` 로직 추가
- **수용 기준**: requiresConfirmation 도구는 바텀시트 표시. 실행 결과는 Snackbar 또는 Observing 화면에 표시
- **테스트 명령**: `./gradlew :app:compileDebugKotlin`
- **다음 단계로 넘겨야 할 정보**: ToolExecutionResult 저장 위치 (Room DB or In-Memory)

### M9. Lazy Refinement

- **목표**: 사용자 피드백 기반 재생성 (RefineAgent)
- **수정 예상 파일**: 없음
- **새로 만들 파일**:
  - `domain/agent/RefineAgent.kt`
  - `data/gemini/GeminiRefineAgent.kt` (또는 GeminiActionPlanner에 refine 메서드 추가)
- **수용 기준**: 사용자 피드백 텍스트 → 수정된 CandidateItem 목록 또는 ActionDraft 재생성
- **MVP 우선순위**: 낮음 (MVP에서는 skip 가능)
- **테스트 명령**: `./gradlew :app:compileDebugKotlin`

### M10. Cluster & Topic Suggestion Agent

- **목표**: Flow B 구현 (클러스터링 + 주제 추천)
- **수정 예상 파일**: `di/AiModule.kt` (바인딩 추가)
- **새로 만들 파일**:
  - `domain/agent/ClusterTopicAgent.kt`
  - `data/gemini/GeminiClusterTopicAgent.kt`
  - `data/retrieval/LocalClusterer.kt` (Jaccard + Union-Find)
  - `presentation/agent/ClusterSuggestionViewModel.kt`
  - `presentation/agent/ClusterSuggestionScreen.kt`
- **수용 기준**: 전체 DataItem 클러스터링 → 클러스터 label + SuggestedTopic 생성 → UI 표시 → 선택 시 Flow A 진입
- **MVP 우선순위**: MVP 이후 (P1)
- **테스트 명령**: `./gradlew :app:compileDebugKotlin`

### M11. WorkManager 분리 (선택)

- **목표**: 클러스터링 등 무거운 작업을 WorkManager로 분리
- **수정 예상 파일**: `AndroidManifest.xml` (Worker 등록)
- **MVP 우선순위**: 낮음 (사용자 명시적 트리거로 충분)

### M12. 테스트 / 관찰성 / 품질 게이트

- **목표**: 테스트 인프라 구축
- **새로 만들 파일**:
  - `app/src/test/`에 단위 테스트 추가
  - `app/src/androidTest/`에 instrumentation 테스트 추가
- **테스트 종류**:
  - serialization round-trip test
  - parser validation test (잘못된 JSON, 빈 응답, markdown fence)
  - CandidateItemRanker test
  - FakeTopicPlanner / FakeItemRecommendationAgent / FakeActionPlanner
  - ToolExecutor validate test
  - AgentSessionState transition test
  - ViewModel state test
- **수용 기준**: `./gradlew :app:testDebugUnitTest` 80% 이상 통과

---

## 10. 위험한 리팩터링 제외 목록

이번 구현에서 절대 해서는 안 되는 변경:

- **기존 TopicAgent analyze API 삭제 금지**: `suspend fun analyze(topic, items, userInstruction?): Result<AgentResult>`는 그대로 유지
- **기존 DataRepositoryImpl.runTopicAnalysis() 삭제 금지**: 내부에서 TopicAgent를 호출하는 로직 유지
- **기존 화면 라우팅 전면 교체 금지**: MainScreen/MainViewModel은 그대로 두고, 새 에이전트 Screen은 별도 Navigation route로 추가
- **기존 Repository 책임을 한 번에 갈아엎기 금지**: DataRepository는 그대로 유지, 필요한 경우 새 메서드 추가만 허용
- **LLM이 직접 Intent 실행하도록 만들기 금지**: Tool 실행은 항상 Kotlin 코드(ToolExecutor)가 수행
- **백그라운드 클립보드 감시 구현 금지**: AGENTS.md 원칙에 명시됨
- **schema validation 없이 LLM JSON을 신뢰하기 금지**: 모든 LLM 출력은 parser에서 itemId 존재 여부, score 범위, 필수 필드 검증
- **모든 단계에서 LLM을 호출하도록 만들기 금지**: DB 검색, 정렬, 검증, 실행은 Kotlin에서 처리
- **사용자 확인 없는 외부 앱 실행 금지**: Calendar, Reminder, Share는 반드시 사용자 확인 후 실행
- **데이터 수집 경로 자동화 금지**: 공유 인텐트, 붙여넣기, 파일 가져오기만 사용

---

## 11. 테스트 전략

### 테스트 레이어

| 레이어 | 테스트 유형 | 예시 |
|---|---|---|
| **Model** | Unit Test | serialization round-trip, DTO validation |
| **Parser** | Unit Test | 잘못된 JSON, 빈 응답, markdown fence, itemId 검증 |
| **DataRetriever** | Unit Test (Fake DAO) | 키워드 검색, 타입 필터, 날짜 범위 |
| **CandidateItemRanker** | Unit Test | score 계산, 정렬 순서 |
| **Agent** | Unit Test (Fake LLM) | FakeTopicPlanner, FakeItemRecommendationAgent, FakeActionPlanner |
| **ToolExecutor** | Unit Test | validate (canExecute), payload 검증 |
| **State Machine** | Unit Test | AgentSessionState 상태 전이 검증 |
| **ViewModel** | Unit Test (Fake Repository) | AgentSessionViewModel 상태 변경, trigger 검증 |
| **UI** | Compose Test | TopicInputScreen, CandidateItemSelectionScreen |
| **Integration** | Instrumented Test | E2E 플로우 (주제 입력 → Action 실행) |

### 테스트 더블 전략

- **FakeTopicPlanner**: 실제 LLM 호출 대신 JSON 파일에서 응답 로드
- **FakeItemRecommendationAgent**: 미리 정의된 score + reason 반환
- **FakeActionPlanner**: 미리 정의된 ActionDraft 목록 반환
- **FakeGeminiManager**: 프롬프트에 따라 선택된 fixture 반환

### 실행 명령

```bash
# 단위 테스트
./gradlew :app:testDebugUnitTest

# 특정 클래스
./gradlew :app:testDebugUnitTest --tests "*CandidateItemRanker*"

# Instrumented 테스트
./gradlew :app:connectedDebugAndroidTest

# 커버리지 리포트 (프로젝트에 jacoco 설정 필요)
./gradlew :app:jacocoTestReport
```

---

## 12. 현재 단계의 결론

### 지금 코드베이스에서 가장 안전한 첫 구현 지점

1. **M1**: `domain/model/`에 새 DTO 추가 (기존 파일 수정 없음)
   - `AgentSessionState.kt`, `CandidateItem.kt`, `RetrievalPlan.kt` 등
   - 가장 위험도가 낮고, 다른 코드에 영향이 전혀 없는 시작점

2. **M2**: `domain/retrieval/` 인터페이스 + `data/retrieval/` 구현체 추가
   - 기존 `KnowledgeRepository.searchItems()`를 참고하여 Room 기반 검색 구현
   - `MainViewModel.findCandidateItemsForPrompt()`의 로직을 DataRetriever로 이전 가능

### 다음 단계(M1)에서 실제로 추가할 파일 목록

```
domain/model/
├── AgentSessionState.kt      ← 상태 머신 sealed class
├── CandidateItem.kt          ← 검색 결과 + 점수 wrapper
├── RetrievalPlan.kt          ← LLM 검색 계획 DTO
├── ToolSpec.kt               ← 도구 스펙
├── RequiredInput.kt          ← 필수 입력 필드
├── ToolExecutionResult.kt    ← 실행 결과
├── SuggestedTopic.kt         ← 추천 주제 (Flow B)
└── DataCluster.kt            ← 클러스터 정보
```

### 빌드/테스트 실행 결과

```
./gradlew :app:compileDebugKotlin → BUILD SUCCESSFUL in 14s (17 tasks up-to-date)
```

### 미해결 질문 또는 확인 필요한 점

1. **Navigation 진입점**: AgentSessionScreen은 어디서 진입하는가?
   - (A) MainScreen의 새 탭 ("AI 에이전트")
   - (B) FloatingActionButton → 새 Activity/Route
   - (C) MainScreen HOME에서 주제 입력 → 전환
   - **제안**: (A) 방식 — MainScreenMode에 `AGENT` 모드 추가 (MainContract만 수정)

2. **DataRepository 확장 여부**: 새 DataRetriever가 직접 DAO를 주입받을지, 아니면 DataRepository에 검색 메서드를 추가할지
   - **제안**: DataRetriever가 DataItemDao를 직접 주입받음. DataRepository는 기존 인터페이스 유지

3. **AgentSession 영속화**: AgentSession을 Room DB에 저장할지, In-Memory로만 관리할지
   - **제안**: MVP에서는 In-Memory (ViewModel + SavedStateHandle). P1에서 Room Entity 추가

4. **ToolExecutor에 Context 전달**: ToolExecutor가 Application Context를 주입받는 방식
   - **제안**: `@ApplicationContext private val context: Context`를 생성자에 주입

---

[요약]
- 생성/수정한 파일: `docs/AGENT_MOBILE_IMPLEMENTATION_HARNESS.md` (신규)
- 확인한 기존 핵심 파일: TopicAgent.kt, GeminiTopicAgent.kt, AgentJsonParser.kt, GeminiManager.kt, DefaultGeminiManager.kt, DataRepository.kt, DataRepositoryImpl.kt, AgentResult.kt, DataItem.kt, Topic.kt, TopicAction.kt, TopicAnalysis.kt, MainViewModel.kt, MainContract.kt, TopicDao.kt, SmartClipboardDatabase.kt, AiModule.kt
- 다음 단계에서 구현할 첫 코드 영역: M1 - domain/model/에 AgentSessionState, CandidateItem 등 8개 DTO 추가
- 실행한 테스트 명령: `./gradlew :app:compileDebugKotlin`
- 테스트 결과: BUILD SUCCESSFUL in 14s

[중요 발견]
- 기존 TopicAgent 흐름: TopicAgent.analyze()는 Single-Turn. GeminiTopicAgent가 프롬프트 빌드 → DefaultGeminiManager.run() → AgentJsonParser.parse() → AgentResult 반환. DataRepositoryImpl.runTopicAnalysis()에서 호출되어 Room에 저장됨
- 기존 Repository 구조: DataRepositoryImpl은 DataItemDao + TopicDao + TopicAgent를 주입받아 사용. 아이템 검색(findCandidateItemsForPrompt)은 MainViewModel에 하드코딩된 로컬 로직으로 수행
- 기존 UI 진입점: MainScreen → MainViewModel. HOME/DATA/TASKS/TOPIC_DETAIL 모드 전환. TOPIC_DETAIL에서 runTopicAnalysis() 호출
- 위험한 변경 지점: MainViewModel의 findCandidateItemsForPrompt()를 M2에서 DataRetriever로 이전할 때 MainViewModel 로직과 동기화 필요. 기존 AiModule에 새 Agent 바인딩 추가 시 충돌 주의

[다음 단계 제안]
- M1에서 추가할 모델 파일 목록: AgentSessionState.kt, CandidateItem.kt, RetrievalPlan.kt, ToolSpec.kt, RequiredInput.kt, ToolExecutionResult.kt, SuggestedTopic.kt, DataCluster.kt (8개 파일)
- M1에서 추가할 테스트 파일 목록: AgentSessionStateTest.kt (상태 전이 검증), DomainModelSerializationTest.kt (round-trip)
- 주의할 점:
  1. 모든 새 DTO는 kotlinx.serialization이 필요하면 @Serializable 명시
  2. domain/model 디렉터리에 파일 추가만 하고 기존 파일 수정 금지
  3. M1 단계에서는 DI 모듈 수정 금지 (컴파일만 통과시키는 것이 목표)
  4. `./gradlew :app:compileDebugKotlin`으로 각 파일 추가 후 빌드 검증