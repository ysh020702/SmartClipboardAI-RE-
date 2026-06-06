# SmartClipboardAI — 모바일 AI 에이전트 설계 및 구현 계획

> **작성일**: 2026-05-31
> **상태**: 설계 확정 — Multi-LLM Agent Pipeline 구현 예정
> **관련 문서**: `docs/AI_AGENT_IMPLEMENTATION_GUIDE.md`, `docs/ARCHITECTURE.md`, `docs/UX_FLOW.md`
> **기술 난도**: 상 — 모바일 환경에서 Cline 스타일의 Multi-Turn Agentic Workflow를 제한된 리소스로 구현

---

## 1. 개요: SmartClipboardAI의 이중 에이전트 아키텍처

SmartClipboardAI는 두 개의 LLM 에이전트가 협력하여 사용자의 데이터를 작업으로 전환하는 모바일 AI 에이전트 시스템이다.

### 역할 1 — 작업 에이전트 (Task Agent = Planner + Action Drafter + Tool Executor)

사용자가 **주제만 입력**하면 다음 파이프라인을 자동으로 수행한다:

```
[사용자 주제 입력]
       │
       ▼
┌─────────────────────────────────────────────┐
│ Step 1: RETRIEVE                            │
│   Planner LLM이 주제와 관련된 DataItem 검색  │
│   - KnowledgeRepository.fullTextSearch()     │
│   - SourceExtractor로 메타데이터 활용         │
│   - 검색된 후보를 사용자에게 제시             │
├─────────────────────────────────────────────┤
│ Step 2: RECOMMEND & EDIT                     │
│   Planner LLM이 어떤 아이템을 고를지 추천    │
│   - 아이템별 relevance score + 추천 이유     │
│   - 사용자가 선택/제외/편집 가능              │
├─────────────────────────────────────────────┤
│ Step 3: DRAFT ACTIONS ("다음으로")            │
│   Action Drafter LLM이 선택된 아이템으로     │
│   실행 가능한 Action 초안 생성                │
│   - SUMMARY, CALENDAR, REMINDER, TODO,       │
│     SHARE_DRAFT 중 적합한 타입 선택          │
│   - 각 Action에 confidence, payload 포함     │
├─────────────────────────────────────────────┤
│ Step 4: EXECUTE                              │
│   사용자가 Action 선택 → Tool Executor가     │
│   Android Intent로 외부 앱 연동 수행          │
│   - Calendar: ACTION_INSERT                  │
│   - Notes: ACTION_SEND (Share Sheet)         │
│   - Reminder: UI 표시 → 사용자 확인 후 등록  │
│   - TODO: Internal DB 저장                    │
└─────────────────────────────────────────────┘
```

### 역할 2 — 분석 에이전트 (Analysis Agent = Clusterer + Topic Recommender)

**백그라운드/온디맨드**로 전체 DataItem을 분석하여:

1. **클러스터링**: 유사한 DataItem을 그룹화 (텍스트 유사도, 시간 근접성, 출처 기준)
2. **주제 추천**: 각 클러스터에 적합한 Topic 제목과 설명을 추천
3. **"에이전트에게 시킬 주제" 추천**: 사용자가 작업 에이전트에 입력할 만한 주제 후보를 제안

```
[전체 DataItem]
       │
       ▼
┌─────────────────────────────────────────────┐
│ Phase 1: CLUSTERING                          │
│   - 텍스트 임베딩 기반 유사도 계산            │
│   - DBSCAN 또는 계층적 클러스터링             │
│   - 시간 윈도우 기반 co-occurrence 분석       │
├─────────────────────────────────────────────┤
│ Phase 2: TOPIC RECOMMENDATION                │
│   - 클러스터별 LLM 요약 → Topic 후보 생성     │
│   - Topic 후보를 사용자에게 제시              │
│   - 사용자가 선택/수정 → Task Agent 트리거    │
└─────────────────────────────────────────────┘
```

### LLM 사용 위치 요약

| LLM 역할 | 사용 위치 | 입력 | 출력 |
|----------|-----------|------|------|
| **Planner** | Step 1-2 (Retrieve + Recommend) | Topic 제목, 전체 DataItem 메타데이터 | 검색 쿼리, relevance score, 추천 이유 |
| **Action Drafter** | Step 3 (Draft Actions) | 선택된 DataItem 목록, Topic 정보 | AgentResult (summary + keyPoints + actions) |
| **Tool Executor** | Step 4 (Execute) | 선택된 ActionDraft | Android Intent 실행 (LLM 불필요, 로컬 로직) |
| **Clusterer** | Phase 1 (Clustering) | 전체 DataItem 텍스트 | clusterId 할당 (로컬 알고리즘 + LLM 검증) |
| **Topic Recommender** | Phase 2 (Recommend Topics) | 클러스터별 DataItem 요약 | Topic 후보 목록 |

---

## 2. 기존 Single-Turn 구조와의 비교

현재 구현된 `TopicAgent.analyze()`는 **Single-Turn**이다:
- 한 번의 Gemini API 호출로 Topic 분석 → Action 생성
- 프롬프트에 모든 것을 담고 한 번에 응답
- 장점: 빠름 (1~3초), 단순함
- 단점: 검색(Retrieval), 추천, 편집 피드백 루프가 없음

**이 에이전트 아키텍처는 다음을 추가한다:**

| 기능 | Single-Turn (현재) | Agentic Multi-Turn (목표) |
|------|-------------------|--------------------------|
| 데이터 검색 | Topic에 미리 연결된 item만 사용 | Planner가 전체 DB에서 지능적 검색 |
| 아이템 추천 | 없음 | LLM 기반 relevance score + 이유 |
| 사용자 편집 | Action 본문만 수정 가능 | 아이템 선택/제외, Action 편집 모두 가능 |
| 작업 생성 | 한 번에 모든 Action 생성 | 컨텍스트를 누적하며 점진적 생성 |
| 연동 실행 | HandoffLauncher 수동 호출 | Tool Executor가 자동 매핑 |
| 클러스터링 | 없음 | 로컬 알고리즘 + LLM 검증 |
| 주제 추천 | 없음 | 분석 에이전트가 자동 제안 |

---

## 3. 기술적 난도: 모바일에서 Cline 스타일 에이전트를 구현할 때의 과제

Cline(코드 어시스턴트)은 데스크톱 환경에서 다음과 같은 패턴으로 동작한다:
- **탐색 → 추론 → 실행 → 관찰 → 반복**
- 수백 MB의 컨텍스트, 무제한 API 호출, 빠른 네트워크

모바일(SmartClipboardAI)에서 동일한 패턴을 구현하려면 다음 과제를 해결해야 한다:

### 3-1. LLM 호출 횟수 관리

| 과제 | 해결 전략 |
|------|-----------|
| API 비용 폭증 | 각 LLM 역할별로 **경량 모델**(Gemini Flash-Lite) 사용, 로컬 캐싱으로 중복 호출 방지 |
| QPS 제한 | 호출 간 최소 간격(500ms) 적용, Exponential Backoff 재시도 |
| 배터리/데이터 | Wi-Fi 연결 시에만 전체 분석 수행, 셀룰러에서는 경량화 |

### 3-2. 지연 시간 관리

| 과제 | 해결 전략 |
|------|-----------|
| Multi-Turn 총 5~15초 | 각 Step마다 **스트리밍 UI**(부분 결과부터 표시), Step 1 결과가 도착하면 바로 렌더링 |
| 사용자 이탈 방지 | Step 1(검색)은 1~2초 내 완료, Step 2(추천)는 Step 1과 병렬 가능 |
| 백그라운드 처리 | 분석 에이전트(Clustering + Topic Recommendation)는 앱이 유휴 상태일 때 백그라운드에서 실행 (`WorkManager`) |

### 3-3. 컨텍스트 윈도우 제한

| 과제 | 해결 전략 |
|------|-----------|
| Gemini Flash-Lite 컨텍스트 1M 토큰이지만, 모바일 메모리 제한 | DataItem.content를 `take(300)`자로 제한, 전체 텍스트는 로컬 임베딩으로 벡터화하여 관련성 높은 항목만 LLM에 전달 |
| Multi-Turn 간 컨텍스트 누적 | 각 Turn의 핵심 결과(검색 결과, 추천 목록)를 요약하여 다음 Turn 프롬프트에 주입 |

### 3-4. 로컬-클라우드 하이브리드

| 과제 | 해결 전략 |
|------|-----------|
| 네트워크 불안정 | **로컬 폴백**: 클러스터링은 TfLite 임베딩 + DBSCAN으로 로컬 처리, LLM은 검증/요약만 담당 |
| 오프라인 동작 | 검색, 클러스터링, 기본 추천은 로컬에서 동작, LLM 기반 Action 생성만 온라인 필요 |

### 3-5. 상태 관리 복잡도

| 과제 | 해결 전략 |
|------|-----------|
| Multi-Step 파이프라인 상태 | `AgentPipelineState` sealed class로 각 Step 상태를 명시적으로 모델링 |
| 부분 실패 처리 | 각 Step은 독립적으로 재시도 가능, 실패한 Step만 복구 |

---

## 4. 아키텍처 상세 설계

### 4-1. 새로 추가될 클래스

```
domain/ai/
├── TopicAgent.kt              ← 기존 (Action Drafter LLM 역할)
├── PlannerAgent.kt             ← 신규: 검색 + 아이템 추천 담당
├── ClusterAgent.kt             ← 신규: 클러스터링 + 주제 추천 담당
├── ToolExecutor.kt             ← 신규: Action → Android Intent 매핑
├── RetrieverAgent.kt           ← 신규: Planner의 검색 도구 (로컬 DB 검색)

domain/model/
├── AgentResult.kt              ← 기존 (Action Drafter 출력)
├── PlannerResult.kt            ← 신규: Planner 출력 (검색 결과 + 추천)
├── ClusterResult.kt            ← 신규: ClusterAgent 출력 (클러스터 + 주제 후보)
├── AgentPipelineState.kt       ← 신규: 전체 파이프라인 상태 관리

data/ai/
├── GeminiTopicAgent.kt         ← 기존 (Action Drafter 구현체)
├── GeminiPlannerAgent.kt       ← 신규: Planner LLM 구현체
├── LocalRetrieverAgent.kt      ← 신규: 로컬 DB 검색 구현체
├── LocalClusterAgent.kt        ← 신규: 로컬 클러스터링 (TfLite + DBSCAN)
├── GeminiClusterRefiner.kt     ← 신규: LLM으로 클러스터 검증/주제 추천
├── ToolExecutorImpl.kt         ← 신규: Tool Executor 구현체

presentation/main/
├── AgentPipelineViewModel.kt   ← 신규: 에이전트 파이프라인 UI 상태 관리
├── AgentPipelineScreen.kt      ← 신규: Step 1~4 UI
```

### 4-2. 도메인 모델

#### PlannerResult.kt
```kotlin
data class PlannerResult(
    val topicId: Long,
    val retrievedItems: List<RetrievedItem>,  // 검색된 아이템 + relevance
    val recommendationReason: String,          // 왜 이 아이템들을 추천하는지
    val suggestedQueries: List<String>         // 사용자가 검색어를 수정할 수 있도록 제안
)

data class RetrievedItem(
    val item: DataItem,
    val relevanceScore: Float,       // 0.0~1.0
    val relevanceReason: String      // "날짜 정보가 주제와 일치", "텍스트 유사도 0.87"
)
```

#### ClusterResult.kt
```kotlin
data class ClusterResult(
    val clusters: List<ItemCluster>,
    val generatedAt: Long
)

data class ItemCluster(
    val clusterId: String,
    val clusterLabel: String,         // LLM이 생성한 클러스터 이름
    val itemIds: List<Long>,
    val topicCandidates: List<TopicCandidate>  // 이 클러스터를 기반으로 한 주제 후보
)

data class TopicCandidate(
    val suggestedTitle: String,
    val description: String,
    val confidence: Float,
    val reason: String                // "회의 일정과 관련된 링크 3개, 메모 2개가 있습니다"
)
```

#### AgentPipelineState.kt
```kotlin
sealed class AgentPipelineState {
    data object Idle : AgentPipelineState()
    data class Retrieving(val query: String, val progress: Float) : AgentPipelineState()
    data class Retrieved(
        val result: PlannerResult,
        val selectedItemIds: Set<Long>
    ) : AgentPipelineState()
    data class DraftingActions(val selectedCount: Int) : AgentPipelineState()
    data class ActionsReady(
        val agentResult: AgentResult,
        val selectedActionIndex: Int?
    ) : AgentPipelineState()
    data class Executing(val action: AgentActionDraft) : AgentPipelineState()
    data class Executed(val action: AgentActionDraft, val success: Boolean) : AgentPipelineState()
    data class Error(val step: String, val message: String, val recoverable: Boolean) : AgentPipelineState()
}
```

### 4-3. 데이터 흐름 시퀀스

```
User                   AgentPipelineVM      PlannerAgent      GeminiTopicAgent    ToolExecutor
 │                          │                    │                   │                │
 │ "회의 준비" 입력          │                    │                   │                │
 │─────────────────────────►│                    │                   │                │
 │                          │ retrieve(topic)    │                   │                │
 │                          │───────────────────►│                   │                │
 │                          │                    │ fullTextSearch()  │                │
 │                          │                    │──→ KnowledgeRepo  │                │
 │                          │                    │←── results        │                │
 │                          │                    │ gemini.plan()     │                │
 │                          │                    │──→ LLM            │                │
 │                          │                    │←── relevance score│                │
 │                          │ PlannerResult      │                   │                │
 │                          │◄───────────────────│                   │                │
 │                          │                    │                   │                │
 │  "검색 결과: 5개 중        │                    │                   │                │
 │   추천 3개 표시"           │                    │                   │                │
 │◄─────────────────────────│                    │                   │                │
 │                          │                    │                   │                │
 │ 아이템 선택/제외/편집      │                    │                   │                │
 │─────────────────────────►│                    │                   │                │
 │                          │                    │                   │                │
 │ "다음으로"                │                    │                   │                │
 │─────────────────────────►│                    │                   │                │
 │                          │ analyze(topic,     │                   │                │
 │                          │   selectedItems)   │                   │                │
 │                          │──────────────────────────────────────►│                │
 │                          │                    │                   │ gemini.run()   │
 │                          │                    │                   │──→ LLM         │
 │                          │                    │                   │←── AgentResult │
 │                          │ AgentResult        │                   │                │
 │                          │◄──────────────────────────────────────│                │
 │                          │                    │                   │                │
 │  "Action 3개 생성됨"      │                    │                   │                │
 │◄─────────────────────────│                    │                   │                │
 │                          │                    │                   │                │
 │ Action 선택 + 실행        │                    │                   │                │
 │─────────────────────────►│                    │                   │                │
 │                          │ execute(action)    │                   │                │
 │                          │─────────────────────────────────────────────────────────►│
 │                          │                    │                   │                │ Intent
 │                          │                    │                   │                │──→ App
 │                          │ Executed           │                   │                │
 │                          │◄────────────────────────────────────────────────────────│
 │  "Calendar에 일정 전달됨"  │                    │                   │                │
 │◄─────────────────────────│                    │                   │                │
```

---

## 5. 단계별 구현 계획 (Cline 프롬프트)

아래는 각 단계를 구현할 때 Cline에게 지시할 수 있는 프롬프트 템플릿이다.
각 프롬프트는 **독립적인 작업 단위**이며, 순서대로 실행해야 한다.
모든 프롬프트는 `docs/AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`의 설계를 기준으로 한다.

---

### Phase A: 도메인 모델 및 인터페이스 정의

#### A-1. PlannerAgent 인터페이스 및 PlannerResult 모델 생성

```
## 작업: PlannerAgent 인터페이스 및 PlannerResult 모델 생성

### 목적
사용자가 입력한 Topic 제목을 바탕으로, 전체 DataItem 중 관련성 높은 아이템을
검색·추천하는 PlannerAgent의 계약(인터페이스)을 정의한다.

### 생성할 파일
1. `domain/ai/PlannerAgent.kt` — PlannerAgent 인터페이스
2. `domain/model/PlannerResult.kt` — 검색/추천 결과 모델

### 요구사항

**domain/model/PlannerResult.kt:**
```
data class PlannerResult(
    val topicId: Long,
    val retrievedItems: List<RetrievedItem>,
    val recommendationReason: String,
    val suggestedQueries: List<String>
)

data class RetrievedItem(
    val item: DataItem,
    val relevanceScore: Float,
    val relevanceReason: String
)
```

**domain/ai/PlannerAgent.kt:**
```kotlin
interface PlannerAgent {
    suspend fun retrieve(
        topic: Topic,
        allItems: List<DataItem>,
        userInstruction: String? = null
    ): Result<PlannerResult>

    suspend fun refineSelection(
        topic: Topic,
        previousResult: PlannerResult,
        userFeedback: String
    ): Result<PlannerResult>
}
```

### 규칙
- `PlannerAgent.retrieve()`의 `topic` 파라미터는 Topic 객체 전체(id, title 등)를 받는다.
- `allItems`는 현재 DB에 저장된 모든 DataItem이다. (과도한 크기는 Repository에서 제한)
- `userInstruction`은 사용자가 추가한 검색 조건("날짜가 있는 것만", "링크만" 등).
- `retrieve()`는 최소 3개, 최대 10개의 아이템을 반환한다.
- `relevanceScore`는 0.0~1.0, `relevanceReason`은 한글로 한 줄 설명.
- `suggestedQueries`는 사용자가 검색어를 바꿔볼 수 있도록 2~3개 제안.

### 금지
- 기존 `TopicAgent.kt`, `AgentResult.kt` 수정 금지
- `DataRepository` 인터페이스 수정 금지
- DI 모듈 수정 금지 (다음 단계에서)
- 임의의 새 Kotlin 파일을 추가하지 말고 위 2개 파일만 생성

### 검증
- `./gradlew :app:compileDebugKotlin`이 통과해야 함
```

---

#### A-2. ClusterAgent 인터페이스 및 ClusterResult 모델 생성

```
## 작업: ClusterAgent 인터페이스 및 ClusterResult 모델 생성

### 목적
전체 DataItem을 클러스터링하고, 클러스터별로 적합한 Topic 후보를 추천하는
분석 에이전트의 계약을 정의한다.

### 생성할 파일
1. `domain/ai/ClusterAgent.kt` — ClusterAgent 인터페이스
2. `domain/model/ClusterResult.kt` — 클러스터링 결과 모델

### 요구사항

**domain/model/ClusterResult.kt:**
```kotlin
data class ClusterResult(
    val clusters: List<ItemCluster>,
    val generatedAt: Long
)

data class ItemCluster(
    val clusterId: String,
    val clusterLabel: String,
    val itemIds: List<Long>,
    val topicCandidates: List<TopicCandidate>
)

data class TopicCandidate(
    val suggestedTitle: String,
    val description: String,
    val confidence: Float,
    val reason: String
)
```

**domain/ai/ClusterAgent.kt:**
```kotlin
interface ClusterAgent {
    suspend fun cluster(
        items: List<DataItem>
    ): Result<ClusterResult>

    suspend fun recommendTopics(
        items: List<DataItem>,
        maxTopics: Int = 3
    ): Result<List<TopicCandidate>>
}
```

### 규칙
- `ClusterAgent.cluster()`는 로컬 알고리즘(TfLite 임베딩 + DBSCAN)으로 1차 클러스터링 후,
  LLM으로 클러스터 레이블과 Topic 후보를 생성하는 파이프라인이다.
- `clusterId`는 UUID 문자열 형식 (예: "a1b2c3d4-...").
- `clusterLabel`은 "회의 관련 자료", "여행 계획 링크 모음"처럼 사용자가 이해할 수 있는 한글.
- `TopicCandidate.confidence`는 0.0~1.0.
- `TopicCandidate.reason`은 왜 이 Topic이 적합한지 설명.
- `recommendTopics()`는 클러스터링 없이 전체 아이템에서 바로 Topic 후보를 생성한다.
  (사용자가 클러스터링을 건너뛰고 싶을 때 사용)

### 금지
- 기존 모델 파일 수정 금지
- Room DB 스키마 변경 금지
- Android 의존성 추가 금지 (domain 계층)

### 검증
- `./gradlew :app:compileDebugKotlin`이 통과해야 함
```

---

#### A-3. AgentPipelineState 및 ToolExecutor 인터페이스 생성

```
## 작업: AgentPipelineState 및 ToolExecutor 인터페이스 생성

### 목적
에이전트 파이프라인의 Step별 상태를 관리하는 sealed class와
Action을 Android Intent로 변환하는 Tool Executor의 계약을 정의한다.

### 생성할 파일
1. `domain/model/AgentPipelineState.kt` — 파이프라인 상태 모델
2. `domain/ai/ToolExecutor.kt` — Tool Executor 인터페이스

### 요구사항

**domain/model/AgentPipelineState.kt:**
```kotlin
sealed class AgentPipelineState {
    data object Idle : AgentPipelineState()
    data class Retrieving(val query: String, val progress: Float) : AgentPipelineState()
    data class Retrieved(
        val result: PlannerResult,
        val selectedItemIds: Set<Long>
    ) : AgentPipelineState()
    data class DraftingActions(val selectedCount: Int) : AgentPipelineState()
    data class ActionsReady(
        val agentResult: AgentResult,
        val selectedActionIndex: Int?
    ) : AgentPipelineState()
    data class Executing(val action: AgentActionDraft) : AgentPipelineState()
    data class Executed(val action: AgentActionDraft, val success: Boolean) : AgentPipelineState()
    data class Error(val step: String, val message: String, val recoverable: Boolean) : AgentPipelineState()
}
```

**domain/ai/ToolExecutor.kt:**
```kotlin
interface ToolExecutor {
    /**
     * AgentActionDraft를 Android Intent로 변환하여 실행한다.
     * @param action 실행할 Action
     * @param context Android Context (Intent 실행에 필요)
     * @return Result<Boolean> 실행 성공 여부
     */
    fun execute(
        action: AgentActionDraft,
        context: android.content.Context
    ): Result<Boolean>

    /**
     * Action type별로 실행 가능한지 미리 확인한다.
     * Calendar의 경우 캘린더 앱 설치 여부 확인 등.
     */
    fun canExecute(action: AgentActionDraft, context: android.content.Context): Boolean
}
```

### 규칙
- `AgentPipelineState`는 전체 파이프라인의 상태를 단일 `StateFlow`로 관리할 수 있도록 설계.
- `Retrieving.progress`는 0.0~1.0.
- `Error.step`은 오류가 발생한 Step 이름 ("retrieve", "analyze", "execute").
- `Error.recoverable`이 true면 해당 Step만 다시 실행 가능.
- `ToolExecutor.execute()`는 동기 함수. IO 스레드에서 호출할 것.
- Action type별 Intent 매핑은 `ToolExecutorImpl`에서 수행 (다음 Phase에서 구현).

### 금지
- Android Context를 domain 계층에 하드코딩하지 말 것 (인터페이스 파라미터로 받음).
- 기존 `AgentResult.kt`, `AgentActionDraft` 수정 금지.

### 검증
- `./gradlew :app:compileDebugKotlin`이 통과해야 함
```

---

### Phase B: PlannerAgent 구현

#### B-1. 로컬 검색 구현체 (RetrieverAgent)

```
## 작업: RetrieverAgent 구현 — PlannerAgent의 로컬 검색 도구

### 목적
사용자 Topic과 전체 DataItem을 입력받아, 텍스트 유사도 기반으로
관련성 높은 아이템을 검색하는 로컬 검색 엔진을 구현한다.
PlannerAgent가 LLM을 호출하기 전에 후보군을 1차 필터링하는 역할.

### 생성/수정할 파일
1. `domain/ai/RetrieverAgent.kt` — RetrieverAgent 인터페이스 (domain 계층)
2. `data/ai/LocalRetrieverAgent.kt` — 로컬 검색 구현체 (data 계층)

### 요구사항

**domain/ai/RetrieverAgent.kt:**
```kotlin
interface RetrieverAgent {
    suspend fun search(
        query: String,
        items: List<DataItem>,
        maxResults: Int = 10
    ): List<DataItem>

    suspend fun searchByKeywords(
        keywords: List<String>,
        items: List<DataItem>,
        maxResults: Int = 10
    ): List<DataItem>
}
```

**data/ai/LocalRetrieverAgent.kt:**
- `javax.inject.Inject` + `@Singleton` 사용
- 검색 알고리즘:
  1. `query`를 공백 기준으로 키워드 분리
  2. 각 DataItem에 대해:
     - `title`과 `content`(take 300)에서 키워드 포함 횟수 계산
     - `content`가 `query`를 포함하면 가중치 2배
     - `type`이 LINK이고 `source`가 query를 포함하면 가중치 1.5배
  3. 점수 기준 상위 `maxResults`개 반환
- Kotlin 표준 라이브러리만 사용 (외부 의존성 추가 금지)
- DataItem.content는 최대 300자까지 검색 (긴 텍스트 성능 보호)

### 규칙
- `search()`와 `searchByKeywords()`는 suspend 함수. Room DB에 접근하지 않고 메모리 내에서 검색.
- 검색 결과 점수는 단순 TF(term frequency) 기반. 추후 개선 여지 있음.
- 유니코드 정규화(NFC)를 적용해 한글 자모 분리 문제 방지.

### 금지
- Gemini API 호출하지 말 것 (순수 로컬 로직)
- Room DAO를 직접 호출하지 말 것 (호출자가 items를 주입)
- Jsoup, TfLite 등 외부 라이브러리 추가 금지

### 검증
- `./gradlew :app:compileDebugKotlin` 통과
- `LocalRetrieverAgent` 단위 테스트 작성 (선택 사항)
```

---

#### B-2. GeminiPlannerAgent 구현

```
## 작업: GeminiPlannerAgent 구현 — LLM 기반 아이템 추천

### 목적
PlannerAgent 인터페이스의 Gemini 구현체.
RetrieverAgent로 1차 필터링된 후보군을 LLM으로 평가하여
relevanceScore와 추천 이유를 생성한다.

### 생성/수정할 파일
1. `data/ai/GeminiPlannerAgent.kt` — Gemini 구현체 (신규)
2. `data/ai/PlannerJsonParser.kt` — Gemini JSON 응답 파서 (신규)
3. `di/AiModule.kt` — PlannerAgent, RetrieverAgent 바인딩 추가 (수정)

### 요구사항

**data/ai/GeminiPlannerAgent.kt:**
```kotlin
@Singleton
class GeminiPlannerAgent @Inject constructor(
    private val geminiManager: GeminiManager,
    private val retrieverAgent: RetrieverAgent,
    private val parser: PlannerJsonParser
) : PlannerAgent {

    override suspend fun retrieve(
        topic: Topic,
        allItems: List<DataItem>,
        userInstruction: String?
    ): Result<PlannerResult> {
        // 1. 로컬 검색으로 1차 필터링 (최대 20개)
        val candidates = retrieverAgent.search(topic.title, allItems, 20)
        if (candidates.isEmpty()) {
            return Result.failure(IllegalStateException("관련 자료를 찾을 수 없습니다."))
        }

        // 2. Gemini에 평가 요청
        val prompt = buildPlanPrompt(topic, candidates, userInstruction)
        val rawResponse = geminiManager.run(prompt)
        return parser.parse(topic.id, candidates, rawResponse)
    }

    // ...
}
```

**프롬프트 설계 (buildPlanPrompt):**
```
당신은 SmartClipboardAI의 자료 추천 Planner입니다.
주어진 Topic과 자료 후보들을 분석해, 관련성 높은 자료를 추천하세요.

[금지]
- 설명 문장 금지. JSON만 출력
- 없는 item id를 생성하지 마세요
- relevanceScore는 정직하게 판단하세요 (0.0~1.0)

[Topic]
title: {topic.title}
{userInstruction}

[자료 후보] ({candidates.size}개)
각 자료의 id, type, title, content(일부)가 주어집니다.
...

[JSON 스키마]
{
  "recommendedItems": [
    {
      "itemId": 1,
      "relevanceScore": 0.85,
      "relevanceReason": "제목과 내용이 주제와 직접 관련됨"
    }
  ],
  "recommendationReason": "전체 추천 요약 설명",
  "suggestedQueries": ["다른 검색어 제안1", "제안2"]
}
```

**data/ai/PlannerJsonParser.kt:**
- `AgentJsonParser`를 참고하여 구현
- Gemini 응답에서 markdown fence 제거 → JSON 파싱 → `PlannerResult` 변환
- `itemId`가 candidates에 실제 존재하는지 검증
- `relevanceScore`는 0.0~1.0으로 coerceIn
- 3~10개 아이템만 허용

**di/AiModule.kt 수정:**
```kotlin
@Binds @Singleton
abstract fun bindPlannerAgent(impl: GeminiPlannerAgent): PlannerAgent

@Binds @Singleton
abstract fun bindRetrieverAgent(impl: LocalRetrieverAgent): RetrieverAgent
```

### 규칙
- Gemini 실패 시 `Result.failure` 반환, ViewModel에서 처리
- 프롬프트는 `GeminiPlannerAgent` 내부에만 존재 (Repository에 노출 금지)
- `PlannerJsonParser`는 `@Singleton` + `@Inject constructor()`

### 금지
- `DataRepository` 인터페이스 수정 금지
- `TopicAgent` 계열 수정 금지
- Room 스키마 변경 금지

### 검증
- `./gradlew :app:compileDebugKotlin` 통과
- `PlannerJsonParser` 단위 테스트: 정상 JSON, 빈 응답, markdown fence 포함 응답 처리 확인
```

---

### Phase C: ClusterAgent 구현

#### C-1. LocalClusterAgent 구현 (로컬 클러스터링)

```
## 작업: LocalClusterAgent 구현 — 로컬 클러스터링 엔진

### 목적
전체 DataItem을 텍스트 유사도 + 시간 근접성을 기준으로 클러스터링하는
로컬 알고리즘을 구현한다. LLM 호출 없이 순수 로컬 처리.

### 생성할 파일
1. `data/ai/LocalClusterAgent.kt` — 로컬 클러스터링 구현체

### 요구사항

**data/ai/LocalClusterAgent.kt:**
```kotlin
@Singleton
class LocalClusterAgent @Inject constructor() {

    /**
     * DataItem 목록을 클러스터링한다.
     * 알고리즘: 간단한 유사도 기반 그리디 클러스터링
     */
    fun clusterItems(items: List<DataItem>): List<Pair<String, List<DataItem>>> {
        // 1. 텍스트 전처리: content에서 공백/특수문자 정규화, 2-gram 생성
        // 2. 아이템 쌍 간 유사도 계산 (Jaccard 유사도)
        // 3. threshold(0.3) 이상인 쌍을 연결
        // 4. 연결 컴포넌트를 클러스터로 반환
    }

    /**
     * 클러스터에 clusterId를 부여하고 clusterLabel 임시 생성
     */
    fun assignClusterLabels(
        clusters: List<Pair<String, List<DataItem>>>,
        items: List<DataItem>
    ): List<ItemCluster>
}
```

### 알고리즘 상세
1. 각 DataItem.content를 최대 200자까지 추출
2. 한글/영문만 남기고 특수문자 제거, 소문자화
3. 2-gram(char bigram) 생성
4. 아이템 쌍의 Jaccard 유사도 = |A ∩ B| / |A ∪ B|
5. 유사도 ≥ 0.25 → 같은 클러스터로 연결 (Union-Find)
6. 각 클러스터에 UUID 부여
7. clusterLabel은 가장 긴 content의 앞 30자로 임시 생성

### 규칙
- Kotlin 표준 라이브러리만 사용
- 최대 500개 아이템까지 2초 이내 처리 목표
- IO Dispatcher에서 호출

### 금지
- Gemini API 호출 금지
- 외부 ML 라이브러리 추가 금지 (TfLite는 향후 도입 검토)
- Room DAO 직접 호출 금지

### 검증
- `./gradlew :app:compileDebugKotlin` 통과
- 테스트: 10개 샘플 아이템으로 클러스터링 결과 확인
```

---

#### C-2. GeminiClusterRefiner 구현 (LLM 기반 주제 추천)

```
## 작업: GeminiClusterRefiner 구현 — LLM으로 클러스터 검증 및 Topic 추천

### 목적
LocalClusterAgent의 클러스터링 결과를 Gemini로 검증하고,
각 클러스터에 적합한 Topic 제목을 추천한다.

### 생성할 파일
1. `data/ai/GeminiClusterRefiner.kt` — ClusterAgent 구현체
2. `data/ai/ClusterJsonParser.kt` — Gemini JSON 응답 파서
3. `di/AiModule.kt` — ClusterAgent 바인딩 추가 (수정)

### 요구사항

**data/ai/GeminiClusterRefiner.kt:**
```kotlin
@Singleton
class GeminiClusterRefiner @Inject constructor(
    private val geminiManager: GeminiManager,
    private val localClusterAgent: LocalClusterAgent,
    private val parser: ClusterJsonParser
) : ClusterAgent {

    override suspend fun cluster(items: List<DataItem>): Result<ClusterResult> {
        // 1. 로컬 클러스터링
        val localResult = localClusterAgent.assignClusterLabels(
            localClusterAgent.clusterItems(items), items
        )
        if (localResult.isEmpty()) {
            return Result.success(ClusterResult(emptyList(), System.currentTimeMillis()))
        }

        // 2. Gemini로 클러스터 검증 + Topic 추천 (클러스터별 1회 호출)
        // 각 클러스터를 Gemini에 보내 clusterLabel 개선, TopicCandidate 생성
        val refined = localResult.map { cluster ->
            refineCluster(cluster, items)
        }

        return Result.success(ClusterResult(refined, System.currentTimeMillis()))
    }

    override suspend fun recommendTopics(
        items: List<DataItem>,
        maxTopics: Int
    ): Result<List<TopicCandidate>> {
        // 전체 아이템을 Gemini에 보내 Topic 후보 직접 생성
        // 클러스터링을 건너뛰는 빠른 경로
    }
}
```

**프롬프트 설계 (refineCluster):**
```
당신은 SmartClipboardAI의 데이터 분석가입니다.
다음은 자동 클러스터링된 자료 묶음입니다.
클러스터 이름을 더 자연스럽게 바꾸고, 이 자료들로 할 수 있는 작업 주제(Topic)를 추천하세요.

[자료 목록]
{items.map { "id:${it.id}, type:${it.type}, content:${it.content.take(100)}" }}

[JSON 스키마]
{
  "clusterLabel": "자연스러운 클러스터 이름 (한글)",
  "topicCandidates": [
    {
      "suggestedTitle": "주제 제목",
      "description": "이 주제로 무엇을 할 수 있는지 설명",
      "confidence": 0.85,
      "reason": "추천 이유"
    }
  ]
}
```

**di/AiModule.kt 수정:**
```kotlin
@Binds @Singleton
abstract fun bindClusterAgent(impl: GeminiClusterRefiner): ClusterAgent
```

### 규칙
- cluster()는 각 클러스터마다 Gemini를 1회 호출하므로, API 호출 횟수 = 클러스터 수.
  최대 5개 클러스터로 제한하여 비용 통제.
- recommendTopics()는 단일 Gemini 호출. 빠른 추천이 필요할 때 사용.
- Gemini 실패 시 로컬 클러스터링 결과를 fallback으로 반환.

### 금지
- 기존 Topic/TopicAnalysis 모델 수정 금지
- Room 스키마 변경 금지
- ClusterAgent를 Repository에 직접 주입하지 말 것 (ViewModel에서 호출)

### 검증
- `./gradlew :app:compileDebugKotlin` 통과
- `ClusterJsonParser` 단위 테스트
```

---

### Phase D: ToolExecutor 구현

#### D-1. ToolExecutorImpl 구현

```
## 작업: ToolExecutorImpl 구현 — Action을 Android Intent로 실행

### 목적
AgentActionDraft의 type과 payload를 분석하여 적절한 Android Intent를 생성하고
실행하는 Tool Executor를 구현한다.

### 생성/수정할 파일
1. `data/ai/ToolExecutorImpl.kt` — ToolExecutor 구현체 (신규)
2. `di/AiModule.kt` — ToolExecutor 바인딩 추가 (수정)

### 요구사항

**data/ai/ToolExecutorImpl.kt:**
```kotlin
@Singleton
class ToolExecutorImpl @Inject constructor() : ToolExecutor {

    override fun execute(
        action: AgentActionDraft,
        context: Context
    ): Result<Boolean> {
        return when (action.type) {
            TopicActionType.CALENDAR -> launchCalendarIntent(action, context)
            TopicActionType.SHARE_DRAFT -> launchShareIntent(action, context)
            TopicActionType.SUMMARY -> launchNotesShareIntent(action, context)
            TopicActionType.TODO -> saveTodoInternal(action, context)
            TopicActionType.REMINDER -> showReminderDraft(action, context)
        }
    }

    override fun canExecute(action: AgentActionDraft, context: Context): Boolean {
        return when (action.type) {
            TopicActionType.CALENDAR -> {
                // 캘린더 앱이 설치되어 있는지 확인
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                }
                intent.resolveActivity(context.packageManager) != null
            }
            else -> true
        }
    }

    private fun launchCalendarIntent(action: AgentActionDraft, context: Context): Result<Boolean> {
        // payload JSON에서 startTime, endTime, eventTitle, eventDescription 파싱
        // Intent.ACTION_INSERT + CalendarContract.Events.CONTENT_URI
        // FLAG_ACTIVITY_NEW_TASK 추가
    }

    private fun launchShareIntent(action: AgentActionDraft, context: Context): Result<Boolean> {
        // Intent.ACTION_SEND + text/plain
        // Intent.createChooser()로 공유 대상 선택
    }

    private fun launchNotesShareIntent(action: AgentActionDraft, context: Context): Result<Boolean> {
        // SUMMARY 타입 → Notes 앱으로 공유 초안 전달
        // launchShareIntent와 동일한 로직, MIME type text/plain
    }

    private fun saveTodoInternal(action: AgentActionDraft, context: Context): Result<Boolean> {
        // TODO 타입 → 내부 DB에 저장만 하고 true 반환
        // (실제 저장은 ViewModel/Repository에서 수행, 여기서는 성공 신호만)
        return Result.success(true)
    }

    private fun showReminderDraft(action: AgentActionDraft, context: Context): Result<Boolean> {
        // REMINDER 타입 → MVP에서는 UI 표시만 하고 사용자 확인 필요
        // 현재는 성공으로 처리, 추후 알람 매니저 연동
        return Result.success(true)
    }
}
```

### 규칙
- 모든 Intent에는 `FLAG_ACTIVITY_NEW_TASK`를 추가.
- `execute()`는 동기 함수. 호출 측에서 IO Dispatcher로 감쌀 것.
- payload JSON 파싱은 `org.json.JSONObject` 사용 (Android 기본).
- Calendar payload 예: `{"eventTitle":"회의","startTime":"2026-06-01T14:00:00+09:00","endTime":"2026-06-01T15:00:00+09:00"}`
- Share payload 예: `{"shareTitle":"공유 제목","shareText":"공유할 본문 내용..."}`

### 금지
- 사용자 확인 없이 Calendar/Reminder 자동 등록 금지 (MVP 원칙)
- Manifest 파일 수정 금지
- 기존 `HandoffLauncher` 수정 금지 (ToolExecutorImpl이 대체)

### 검증
- `./gradlew :app:compileDebugKotlin` 통과
- Calendar intent가 생성되는지 단위 테스트
```

---

### Phase E: ViewModel 및 UI 통합

#### E-1. AgentPipelineViewModel 구현

```
## 작업: AgentPipelineViewModel 구현 — 에이전트 파이프라인 상태 관리

### 목적
Step 1(검색) → Step 2(추천/편집) → Step 3(Action 생성) → Step 4(실행)의
전체 파이프라인 상태를 관리하는 ViewModel을 구현한다.

### 생성/수정할 파일
1. `presentation/main/AgentPipelineViewModel.kt` — ViewModel (신규)
2. `di/AppModule.kt` — ViewModel 바인딩 확인 (기존 Hilt 구조 사용)

### 요구사항

**presentation/main/AgentPipelineViewModel.kt:**
```kotlin
@HiltViewModel
class AgentPipelineViewModel @Inject constructor(
    private val plannerAgent: PlannerAgent,
    private val topicAgent: TopicAgent,
    private val toolExecutor: ToolExecutor,
    private val dataRepository: DataRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AgentPipelineState>(AgentPipelineState.Idle)
    val state: StateFlow<AgentPipelineState> = _state.asStateFlow()

    // 사용자가 Topic 제목을 입력하면 호출
    fun startRetrieval(topicTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = AgentPipelineState.Retrieving(topicTitle, 0f)

            // 1. Topic 객체 생성 (임시)
            val topic = Topic(id = 0L, title = topicTitle, itemCount = 0,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())

            // 2. 전체 DataItem 가져오기
            val allItems = dataRepository.observeAllItems().first()

            // 3. PlannerAgent로 검색 + 추천
            _state.value = AgentPipelineState.Retrieving(topicTitle, 0.5f)
            val result = plannerAgent.retrieve(topic, allItems)

            result.onSuccess { plannerResult ->
                _state.value = AgentPipelineState.Retrieved(
                    result = plannerResult,
                    selectedItemIds = plannerResult.retrievedItems.map { it.item.id }.toSet()
                )
            }.onFailure { e ->
                _state.value = AgentPipelineState.Error("retrieve", e.message ?: "검색 실패", true)
            }
        }
    }

    // 사용자가 아이템 선택/해제
    fun toggleItemSelection(itemId: Long, selected: Boolean) {
        val current = _state.value
        if (current is AgentPipelineState.Retrieved) {
            val newSelection = if (selected) {
                current.selectedItemIds + itemId
            } else {
                current.selectedItemIds - itemId
            }
            _state.value = current.copy(selectedItemIds = newSelection)
        }
    }

    // "다음으로" 버튼 클릭 → Action 생성
    fun startDrafting() {
        val current = _state.value
        if (current !is AgentPipelineState.Retrieved) return

        val selectedItems = current.result.retrievedItems
            .filter { it.item.id in current.selectedItemIds }
            .map { it.item }
        if (selectedItems.isEmpty()) {
            _state.value = AgentPipelineState.Error("draft", "선택된 자료가 없습니다.", true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.value = AgentPipelineState.DraftingActions(selectedItems.size)

            // Topic 객체 (임시)
            val topic = Topic(id = 0L, title = "사용자 주제",
                itemCount = selectedItems.size,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis())

            val result = topicAgent.analyze(topic, selectedItems)
            result.onSuccess { agentResult ->
                _state.value = AgentPipelineState.ActionsReady(
                    agentResult = agentResult,
                    selectedActionIndex = null
                )
            }.onFailure { e ->
                _state.value = AgentPipelineState.Error("analyze", e.message ?: "분석 실패", true)
            }
        }
    }

    // Action 선택 + 실행
    fun executeAction(action: AgentActionDraft) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = AgentPipelineState.Executing(action)
            // Context는 SavedStateHandle 또는 Application에서 획득
            // 여기서는 Application context 사용 가정
        }
    }

    // 에러 복구
    fun retry() {
        val current = _state.value
        if (current is AgentPipelineState.Error && current.recoverable) {
            when (current.step) {
                "retrieve" -> startRetrieval("")  // 실제로는 이전 query 저장 필요
                "analyze" -> startDrafting()
            }
        }
    }

    fun reset() {
        _state.value = AgentPipelineState.Idle
    }
}
```

### 규칙
- 모든 IO 작업은 `Dispatchers.IO`에서 실행.
- `AgentPipelineState`의 각 상태에 UI가 어떻게 반응할지는 Screen 구현에서 결정.
- `startRetrieval`의 topicTitle 저장 및 복구를 위해 내부적으로 마지막 query를 저장.
- `executeAction`은 Context가 필요하므로 `AndroidViewModel` 또는 `Application`을 주입받아 처리.

### 금지
- `MainViewModel.kt` 수정 금지 (충돌 방지)
- `DataRepository` 인터페이스 수정 금지 (필요한 메서드가 없으면 ViewModel 내에서 DataItemDao 직접 사용 검토)
- UI 코드 (Compose)를 ViewModel에 작성하지 말 것

### 검증
- `./gradlew :app:compileDebugKotlin` 통과
```

---

#### E-2. AgentPipelineScreen 구현

```
## 작업: AgentPipelineScreen UI 구현

### 목적
에이전트 파이프라인의 4단계를 사용자에게 보여주는 Compose UI를 구현한다.

### 생성/수정할 파일
1. `presentation/main/AgentPipelineScreen.kt` — Compose Screen (신규)
2. `presentation/main/MainContract.kt` — 필요한 경우 AgentPipeline 이벤트 추가 (선택)

### 요구사항

**presentation/main/AgentPipelineScreen.kt** 구조:

```kotlin
@Composable
fun AgentPipelineScreen(
    viewModel: AgentPipelineViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 에이전트") },
                navigationIcon = { /* 뒤로가기 */ }
            )
        }
    ) { padding ->
        when (val s = state) {
            is AgentPipelineState.Idle -> TopicInputStep(
                onSubmit = { topic -> viewModel.startRetrieval(topic) }
            )
            is AgentPipelineState.Retrieving -> RetrievingStep(s.query, s.progress)
            is AgentPipelineState.Retrieved -> RetrievedStep(
                result = s.result,
                selectedIds = s.selectedItemIds,
                onToggleItem = { id, sel -> viewModel.toggleItemSelection(id, sel) },
                onNext = { viewModel.startDrafting() }
            )
            is AgentPipelineState.DraftingActions -> DraftingStep(s.selectedCount)
            is AgentPipelineState.ActionsReady -> ActionsReadyStep(
                result = s.agentResult,
                onExecuteAction = { action -> viewModel.executeAction(action) },
                onBack = { viewModel.reset() }
            )
            is AgentPipelineState.Executing -> ExecutingStep()
            is AgentPipelineState.Executed -> ExecutedStep(s.action, s.success)
            is AgentPipelineState.Error -> ErrorStep(
                message = s.message,
                recoverable = s.recoverable,
                onRetry = { viewModel.retry() },
                onCancel = { viewModel.reset() }
            )
        }
    }
}
```

**Step별 UI 상세:**

**TopicInputStep (Idle 상태):**
- OutlinedTextField: "분석할 주제를 입력하세요" (한글 placeholder)
- "시작하기" Button → `viewModel.startRetrieval(query)`
- 텍스트가 비어 있으면 버튼 비활성화

**RetrievingStep:**
- CircularProgressIndicator + "자료 검색 중..."
- LinearProgressIndicator(progress) + "관련 자료 찾는 중"

**RetrievedStep (핵심):**
- LazyColumn:
  - 각 아이템: Card에 Checkbox + 제목 + 타입 아이콘 + relevanceScoreBadge
  - relevanceReason을 subtitle로 표시
- recommendationReason을 상단에 InfoCard로 표시
- suggestedQueries를 ChipRow로 표시 (터치 시 검색어 변경)
- 하단: "다음으로 →" Button (1개 이상 선택 시 활성화)

**DraftingActionsStep:**
- CircularProgressIndicator + "Action 초안 생성 중..."

**ActionsReadyStep:**
- AgentResult.summary를 상단 Card에 표시
- keyPoints를 LazyColumn에 bullet list로 표시
- actions를 Card 목록으로 표시:
  - 각 Card: type 아이콘, title, confidenceBadge, body 미리보기
  - 탭하면 상세 BottomSheet 또는 펼치기
  - "실행" 버튼 → `viewModel.executeAction(action)`

**ErrorStep:**
- 오류 아이콘 + message
- recoverable이 true면 "다시 시도" 버튼
- "취소" 버튼 → `viewModel.reset()`

### UI 스타일 가이드
- One UI inspired palette 사용 (기존 `SmartClipboardTheme` 준수)
- confidence ≥ 0.7: Green, 0.4~0.7: Yellow, < 0.4: Red
- Card는 elevation 2dp, rounded corner 12dp
- Step 전환 시 간단한 애니메이션 (AnimatedContent)

### 금지
- `MainScreen.kt` 수정 금지
- `MainViewModel.kt` 수정 금지
- 백그라운드에서 자동 실행하는 코드 작성 금지

### 검증
- Compose Preview로 각 Step 상태 시각적 확인
- `./gradlew :app:compileDebugKotlin` 통과
```

---

### Phase F: 분석 에이전트 통합

#### F-1. ClusterViewModel 및 ClusterScreen 구현

```
## 작업: ClusterViewModel + ClusterScreen — 분석 에이전트 UI

### 목적
ClusterAgent를 호출하여 전체 DataItem을 클러스터링하고,
주제 후보를 사용자에게 추천하는 UI를 구현한다.

### 생성할 파일
1. `presentation/main/ClusterViewModel.kt` — ViewModel (신규)
2. `presentation/main/ClusterScreen.kt` — Compose Screen (신규)

### 요구사항

**ClusterViewModel.kt:**
```kotlin
@HiltViewModel
class ClusterViewModel @Inject constructor(
    private val clusterAgent: ClusterAgent,
    private val dataRepository: DataRepository
) : ViewModel() {

    sealed class ClusterUiState {
        data object Idle : ClusterUiState()
        data object Loading : ClusterUiState()
        data class Ready(
            val clusters: List<ItemCluster>,
            val allTopicCandidates: List<TopicCandidate>
        ) : ClusterUiState()
        data class Error(val message: String) : ClusterUiState()
    }

    private val _state = MutableStateFlow<ClusterUiState>(ClusterUiState.Idle)
    val state: StateFlow<ClusterUiState> = _state.asStateFlow()

    fun startClustering() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = ClusterUiState.Loading
            val items = dataRepository.observeAllItems().first()
            if (items.isEmpty()) {
                _state.value = ClusterUiState.Error("수집된 자료가 없습니다.")
                return@launch
            }

            val result = clusterAgent.cluster(items)
            result.onSuccess { clusterResult ->
                val allCandidates = clusterResult.clusters.flatMap { it.topicCandidates }
                _state.value = ClusterUiState.Ready(clusterResult.clusters, allCandidates)
            }.onFailure { e ->
                _state.value = ClusterUiState.Error(e.message ?: "클러스터링 실패")
            }
        }
    }

    fun onTopicCandidateSelected(candidate: TopicCandidate) {
        // 선택된 TopicCandidate를 AgentPipelineScreen으로 전달
        // (Navigation 또는 SharedState 사용)
    }

    fun reset() {
        _state.value = ClusterUiState.Idle
    }
}
```

**ClusterScreen.kt 구조:**
- TopAppBar: "AI 분석"
- Loading: CircularProgressIndicator + "자료 분석 중..."
- Ready:
  - LazyColumn:
    1. Header: "추천 주제" — 전체 topicCandidates를 우선 표시
       - 각 TopicCandidate: Card (title, description, confidence badge, reason, "시작하기" 버튼)
    2. Divider
    3. Header: "자료 클러스터" — 클러스터별로 그룹화된 아이템 표시
       - 각 ItemCluster: ExpandableCard (clusterLabel, itemCount)
       - 펼치면 itemIds 미리보기 (추후 item 상세로 연결)
- Error: 오류 메시지 + "다시 시도" + "취소" 버튼

### 규칙
- TopicCandidate의 "시작하기" 버튼 → `AgentPipelineScreen`으로 이동하며 suggestedTitle을 query로 전달.
- 클러스터링은 무거운 작업이므로 UI에서 명시적으로 "분석 시작" 버튼을 눌러야 실행.
- 앱 최초 실행 시 자동으로 클러스터링하지 않음 (MVP 원칙: 사용자 주도).

### 금지
- 백그라운드에서 자동 클러스터링 금지
- `MainScreen.kt` 수정 금지
- `DataRepository` 인터페이스 수정 금지

### 검증
- Compose Preview 확인
- `./gradlew :app:compileDebugKotlin` 통과
```

---

## 6. 전체 구현 로드맵

### Phase별 의존 관계

```
Phase A (모델/인터페이스)
  ├── A-1: PlannerAgent + PlannerResult
  ├── A-2: ClusterAgent + ClusterResult
  └── A-3: AgentPipelineState + ToolExecutor
        │
        ▼
Phase B (PlannerAgent 구현)
  ├── B-1: RetrieverAgent (로컬 검색)
  └── B-2: GeminiPlannerAgent (LLM 추천)
        │
        ▼
Phase C (ClusterAgent 구현)
  ├── C-1: LocalClusterAgent (로컬 클러스터링)
  └── C-2: GeminiClusterRefiner (LLM 주제 추천)
        │
        ▼
Phase D (ToolExecutor 구현)
  └── D-1: ToolExecutorImpl (Intent 실행)
        │
        ▼
Phase E (ViewModel + UI 통합)
  ├── E-1: AgentPipelineViewModel
  └── E-2: AgentPipelineScreen
        │
        ▼
Phase F (분석 에이전트 통합)
  └── F-1: ClusterViewModel + ClusterScreen
```

### 구현 우선순위 (MVP)

| 순위 | Phase | 설명 | 예상 시간 |
|------|-------|------|-----------|
| **P0** | A-1, A-3 | PlannerAgent 인터페이스 + PipelineState 모델 | 30분 |
| **P0** | B-1 | RetrieverAgent (로컬 검색) | 1시간 |
| **P0** | B-2 | GeminiPlannerAgent (LLM 추천) | 1.5시간 |
| **P0** | E-1, E-2 | AgentPipelineViewModel + Screen | 2시간 |
| **P0** | D-1 | ToolExecutorImpl | 1시간 |
| **P1** | A-2 | ClusterAgent 인터페이스 | 30분 |
| **P1** | C-1 | LocalClusterAgent | 1.5시간 |
| **P1** | C-2 | GeminiClusterRefiner | 1.5시간 |
| **P1** | F-1 | ClusterViewModel + Screen | 1.5시간 |

**MVP 최소 기능 (P0만 구현):**
1. 사용자가 Topic 제목 입력 → PlannerAgent가 관련 아이템 검색 + 추천
2. 사용자가 아이템 선택/편집
3. "다음으로" → TopicAgent가 Action 생성
4. Action 선택 → ToolExecutor가 외부 앱 연동

**P0 총 예상 시간**: 약 6시간

**P1 (분석 에이전트) 총 예상 시간**: 약 5시간

**전체 총 예상 시간**: 약 11시간

---

## 7. LLM 사용 전략 요약

| LLM 호출 | 사용자 트리거 | 호출 횟수 | 예상 지연 | Fallback |
|----------|-------------|----------|----------|----------|
| **Planner: 검색 + 추천** | Topic 입력 | 1회 (Gemini) + 로컬 검색 | 1~2초 | 로컬 검색만으로 결과 표시 |
| **Action Drafter: Action 생성** | "다음으로" 클릭 | 1회 (Gemini) | 1~3초 | FakeTopicAgent (휴리스틱) |
| **Clusterer: 클러스터 검증** | "분석 시작" 클릭 | N회 (클러스터당 1회) | 3~10초 | 로컬 클러스터만 표시 |
| **Topic Recommender: 주제 추천** | "분석 시작" 클릭 | 포함됨 (Clusterer) | - | 클러스터 레이블을 주제로 사용 |

---

## 8. 위험 요소 및 완화 전략

| 위험 | 영향 | 완화 전략 |
|------|------|-----------|
| Gemini API 응답 지연 | Step 1~3 총 지연 5~10초 | 각 Step마다 부분 결과부터 표시 (Streaming UI), 로컬 폴백 준비 |
| LLM 환각(Hallucination) | 잘못된 itemId, 존재하지 않는 Action | 모든 itemId를 실제 DB와 교차 검증, JsonParser에서 필터링 |
| 클러스터링 정확도 부족 | 의미 없는 Topic 후보 | LLM 검증 단계에서 필터링, 사용자 피드백으로 개선 |
| Intent 실행 실패 | Calendar 앱 없음 등 | `canExecute()`로 사전 확인, 실패 시 대체 액션 제안 |
| 모바일 리소스 제한 | API 호출 중 앱 강제 종료 | ViewModelScope로 관리, onCleared에서 정리, 부분 결과 저장 |

---

## 9. 결론 및 다음 단계

이 문서는 SmartClipboardAI에 **완전한 Agentic Workflow**를 도입하기 위한 설계와 구현 계획을 담고 있다.
기존 `AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`에서 "과투자"로 결론내렸던 Multi-Turn 접근을,
모바일 환경에 최적화된 **Planner → Drafter → Executor** 파이프라인으로 재해석하여 구현한다.

기술적 난도는 높지만, 각 LLM 역할을 명확히 분리하고 로컬-클라우드 하이브리드 전략을 취함으로써
모바일에서도 Cline 스타일의 지능형 에이전트 경험을 제공할 수 있다.

### Phase A부터 Cline에게 지시 가능

위의 각 Phase별 프롬프트(A-1, A-2, A-3, B-1, ...)는 `docs/AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`를
컨텍스트로 제공한 후 그대로 Cline에게 작업 지시로 사용할 수 있다.
각 프롬프트는 독립적인 작업 단위이며, 순서대로 실행해야 한다.

---

> **참고**: 이 문서는 기존 `AI_AGENT_MULTI_RETRIEVAL_ANALYSIS.md`의 "조건부 Multi-Turn" 결론을
> 의도적으로 뒤집고, **완전한 Agentic Pipeline**을 구현 과제로 설정한 것이다.
> 이는 사용자가 설정한 "기술적 난도를 표현하는 과제"의 의도에 부합한다.