# SmartClipboardAI

SmartClipboardAI는 사용자가 일상적으로 모으는 사진, 스크린샷, 복사한 텍스트, 복사한 링크를 하나의 데이터 흐름으로 정리하고, 이를 작업 주제와 AI Agent의 실행 가능한 행동으로 연결하는 Android 앱입니다.

핵심 흐름은 다음과 같습니다.

1. 사용자가 정보를 입력하거나 공유합니다.
2. 입력 정보를 전처리해서 `DataItem`으로 저장합니다.
3. 저장된 데이터를 임베딩하고 유사한 것끼리 클러스터링합니다.
4. AI 또는 사용자가 작업 주제인 `Topic`을 구성합니다.
5. AI Agent가 주제와 데이터를 분석해 요약과 실행 계획을 만듭니다.
6. Samsung Notes, Samsung Calendar 등 외부 앱과 연동해 실제 작업으로 이어집니다.

## 입력받는 정보

SmartClipboardAI가 다루는 입력 정보는 다음과 같습니다.

- 직접 찍은 사진
- 스크린샷
- 복사한 텍스트
- 복사한 링크

서로 다른 형태의 입력은 전처리 과정을 거쳐 공통 데이터 모델인 `DataItem`으로 변환됩니다. 이후 모든 저장, 검색, 클러스터링, 주제 추천, AI Agent 분석은 이 `DataItem`을 기준으로 수행됩니다.

## 데이터 저장 구조

전처리된 정보는 도메인 레이어에서 `DataItem`으로 표현됩니다.

```kotlin
data class DataItem(
    val id: Long,
    val type: DataItemType,
    val content: String,
    val title: String? = null,
    val source: String? = null,
    val mimeType: String? = null,
    val createdAt: Long
)
```

이 클래스는 `data/model/DataItemEntity`로 변환되어 DB에 저장됩니다.

```kotlin
@Entity(tableName = "data_items")
data class DataItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String,
    val content: String,
    val title: String? = null,
    val source: String? = null,
    val mimeType: String? = null,
    val createdAt: Long
)
```

현재 DB에는 데이터 원문과 기본 메타데이터가 저장됩니다. 앞으로는 클러스터링 결과를 반영하기 위해 `DataItem`과 `DataItemEntity`에 클러스터 관련 필드를 추가해야 합니다.

예상 필드는 다음과 같습니다.

```kotlin
val clusterId: Long? = null
val clusterLabel: String? = null
val clusterConfidence: Float? = null
```

DB 수정 전에는 `DataItem`, `DataItemEntity`, DAO, Repository, ViewModel 사용처를 확인해야 합니다. 특히 기존 화면에서 `DataItem`을 직접 표시하거나 정렬하는 부분이 있다면 클러스터 필드 추가 후에도 기존 흐름이 깨지지 않도록 마이그레이션과 기본값을 함께 설계해야 합니다.

## 클러스터링

저장된 데이터는 데이터 그 자체의 내용과 날짜 정보를 함께 사용해 클러스터링합니다.

클러스터링 기준은 다음을 포함합니다.

- 데이터 내용의 의미적 유사성
- 생성 날짜 또는 수집 시점의 근접성
- 입력 타입
- 사용자가 직접 선택하거나 수정한 주제 정보

앱 실행 시에는 우선 로딩 시간을 두고 전체 데이터를 다시 클러스터링합니다. 초기 구현에서는 전체 재분석 방식으로 단순하게 시작하고, 이후에는 새로 추가되거나 수정된 데이터만 다시 처리하는 증분 클러스터링으로 최적화합니다.

이 단계가 끝나면 DB에는 클러스터링 정보를 포함한 `DataItem`이 준비됩니다.

## 주제 생성 흐름

클러스터링된 데이터는 작업의 목적이 되는 `Topic`으로 이어집니다.

### 1-1. AI가 데이터만으로 작업 주제를 추천

AI는 저장된 데이터와 클러스터를 기반으로 사용자가 진행할 만한 작업 주제를 추천할 수 있습니다.

현재 이 흐름을 위해 임시로 만들어진 클래스는 `AiProposal`입니다.

```kotlin
data class AiProposal(
    val id: Long = 0L,
    val title: String,
    val description: String,
    val confidence: Float,
    val category: String,
    val itemIds: List<Long>,
    val createdAt: Long
)
```

이 클래스는 AI가 데이터들을 기반으로 주제 추천을 한다는 전제를 위해 만들어진 임시 구조입니다. 현재는 AI가 추천했던 주제들도 DB에 저장하고 있지만, 이 구조를 유지할지 여부는 아직 결정이 필요합니다.

결정해야 할 점은 다음과 같습니다.

- AI 추천 주제를 전부 DB에 저장할 것인가
- 사용자가 선택한 추천만 `Topic`으로 저장할 것인가
- `AiProposal`을 영구 모델로 유지할 것인가
- 임시 UI 상태로만 관리하고 나중에 제거할 것인가

### 1-2. 사용자가 주제를 입력하고 AI가 데이터를 선택

사용자가 직접 작업 주제를 입력하면, AI가 해당 주제와 관련된 데이터를 찾아옵니다.

예시는 다음과 같습니다.

- 사용자 입력 주제: "회의 준비 자료 정리"
- AI 선택 데이터: 회의 관련 캡처 이미지, 복사한 링크, 공유받은 텍스트

이 흐름에서는 사용자가 목적을 정하고, AI가 그 목적에 맞는 데이터를 찾아주는 역할을 합니다.

### 1-3. 사용자가 주제를 입력하고 직접 데이터를 선택

사용자가 직접 작업 주제를 입력하고, 관련 데이터를 직접 고를 수도 있습니다.

이 방식은 AI 추천이 부정확하거나 사용자의 의도가 분명한 경우에 필요합니다. AI는 보조 역할을 할 수 있지만, 최종 데이터 선택권은 사용자에게 있어야 합니다.

### 1-4. 최종 주제 선정

AI가 선택하든, 사용자가 직접 결정하든, 최종 주제 선정은 사용자가 합니다.

이 단계가 끝나면 데이터의 목적이 되는 `Topic`이 준비됩니다.

## Topic 모델

결정된 작업 주제는 아래와 같이 정의됩니다.

```kotlin
data class Topic(
    val id: Long,
    val title: String,
    val itemCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
```

각 `Topic`은 여러 `DataItem`과 연결됩니다. 이 관계를 통해 AI Agent는 현재 주제가 어떤 정보들을 기반으로 만들어졌는지 알 수 있습니다.

## AI Agent 흐름

작업 주제와 주제에 맞는 데이터가 골라지면, AI Agent가 그 정보를 기반으로 해야 할 작업을 판별하고 요약 노트를 생성합니다.

AI Agent의 입력은 다음과 같습니다.

- 현재 주제
- 주제에 선택되거나 묶인 데이터
- 추가적인 사용자 의도 또는 의견

AI Agent의 출력은 다음과 같습니다.

- 어떤 작업을 수행할지 결정하는 플래너
- 사용할 Tool
- 내용의 요약 정리
- 사용자가 수정할 수 있는 실행 초안

### 2-1. 연동 대상 앱

AI Agent는 다음 앱과 연동되는 것을 목표로 합니다.

- Samsung Notes
- Samsung Calendar
- Reminder 앱
- 공유 또는 메시지 앱
- 기타 생산성 도구

초기에는 Samsung Notes와 Samsung Calendar를 우선 고려합니다. Notes는 요약 노트 생성과 잘 맞고, Calendar는 날짜가 포함된 데이터에서 일정 생성으로 이어지기 쉽기 때문입니다.

### 2-2. 주제 분석 결과 생성

AI Agent는 먼저 선택된 주제와 데이터를 분석합니다.

현재 분석 결과를 표현하는 모델은 `TopicAnalysis`입니다.

```kotlin
data class TopicAnalysis(
    val id: Long,
    val topicId: Long,
    val summary: String,
    val keyPoints: List<String>,
    val sourceItemIds: List<Long>,
    val createdAt: Long
)
```

`TopicAnalysis`는 원본 데이터와 실제 작업 사이의 중간 결과입니다. 여기에는 주제 요약, 핵심 포인트, 분석에 사용된 원본 데이터 ID가 포함됩니다.

### 2-3. 실행 가능한 작업 생성

분석이 끝나면 AI Agent는 하나 이상의 작업 초안을 생성합니다.

현재 작업 초안을 표현하는 모델은 `TopicAction`입니다.

```kotlin
enum class TopicActionType {
    SUMMARY,
    CALENDAR,
    REMINDER,
    SHARE_DRAFT,
    TODO
}

enum class TopicActionStatus {
    DRAFT,
    EDITED,
    EXECUTED,
    DISMISSED
}

data class TopicAction(
    val id: Long,
    val topicId: Long,
    val analysisResultId: Long?,
    val type: TopicActionType,
    val title: String,
    val body: String,
    val status: TopicActionStatus,
    val editablePayload: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

`TopicAction`은 바로 실행되는 명령이 아니라 사용자가 검토할 수 있는 초안입니다.

작업 타입별 예시는 다음과 같습니다.

- `SUMMARY`: 관련 데이터를 요약해 Samsung Notes에 기록할 노트 초안 생성
- `CALENDAR`: 날짜, 장소, 회의 정보가 있는 데이터를 기반으로 일정 초안 생성
- `REMINDER`: 마감일이나 후속 작업을 기반으로 리마인더 초안 생성
- `SHARE_DRAFT`: 메시지, 이메일, 공유 텍스트 초안 생성
- `TODO`: 해야 할 일을 추출해 작업 목록 생성

### 2-4. 사용자 검토 후 실행

AI Agent가 생성한 작업은 사용자가 확인하기 전까지 실행되지 않아야 합니다.

예상 흐름은 다음과 같습니다.

1. AI Agent가 `TopicAction`을 `DRAFT` 상태로 생성합니다.
2. 사용자가 내용을 검토합니다.
3. 필요하면 사용자가 제목, 본문, 날짜, 대상 앱 등을 수정합니다.
4. 사용자가 실행하면 상태가 `EXECUTED`로 변경됩니다.
5. 사용자가 사용하지 않으면 상태가 `DISMISSED`로 변경됩니다.

이 구조는 AI가 사용자를 대신해 판단하되, 최종 실행 권한은 사용자에게 남겨두기 위한 설계입니다.

### 2-5. Tool 구조

AI Agent가 사용하는 외부 앱 연동은 Tool 단위로 정의합니다.

각 Tool은 다음을 가져야 합니다.

- 입력 payload 구조
- 사용할 Android Intent, API, SDK
- 사용자 확인이 필요한지 여부
- 실행 성공 또는 실패 결과
- 실행 후 `TopicActionStatus` 업데이트 방식

예를 들어 Samsung Notes Tool은 다음 입력을 받을 수 있습니다.

- 노트 제목
- 노트 본문
- 원본 `DataItem` ID 목록
- 관련 `Topic` 정보

Samsung Calendar Tool은 다음 입력을 받을 수 있습니다.

- 일정 제목
- 시작 시간과 종료 시간
- 일정 설명
- 관련 `DataItem` ID 목록

AI Agent는 어떤 Tool을 사용할지 결정하고, 앱은 실제 실행 권한과 사용자 확인 흐름을 관리합니다.

### 2-6. 실패 처리

Tool 실행은 실패할 수 있으므로 실패 처리도 모델에 포함되어야 합니다.

예상되는 실패 상황은 다음과 같습니다.

- 대상 앱이 설치되어 있지 않음
- 필요한 권한이 없음
- Intent 실행 실패
- Calendar 날짜 정보가 불완전함
- Notes에 전달할 본문이 너무 길거나 비어 있음

실패 시에는 사용자가 다시 수정하거나 다른 Tool을 선택할 수 있어야 합니다. 실패한 작업은 바로 삭제하지 않고, 상태와 실패 이유를 남겨 재시도할 수 있도록 하는 방향이 좋습니다.

### 2-7. 최종 목표

최종적으로 SmartClipboardAI는 단순한 클립보드 저장 앱이 아니라, 흩어진 정보를 모아 사용자의 다음 작업으로 이어주는 앱이 되는 것을 목표로 합니다.

입력 데이터는 `DataItem`으로 저장되고, 관련 데이터는 `Topic`으로 묶이며, AI Agent는 `TopicAnalysis`와 `TopicAction`을 통해 요약, 일정, 리마인더, 공유 초안, TODO로 변환합니다.

## 현재 기술 스택

- Kotlin
- Android Jetpack Compose
- Room
- Hilt
- Coroutines
- Coil
- Gradle Version Catalog

## Roadmap

- `DataItem`, `DataItemEntity`에 클러스터링 필드 추가
- 클러스터링 필드 추가에 따른 Room migration 작성
- 앱 실행 시 전체 데이터 클러스터링 구현
- `AiProposal`을 영구 저장할지 임시 상태로 둘지 결정
- AI 주제 추천 흐름 개선
- 사용자 직접 주제 입력 흐름 구현
- 사용자 직접 데이터 선택 흐름 구현
- `TopicAnalysis` 생성 로직 구현
- `TopicAction` 초안 생성 로직 구현
- Samsung Notes Tool 연동
- Samsung Calendar Tool 연동
- 작업 실행 전 사용자 확인 플로우 구현
- 전체 재클러스터링을 증분 클러스터링으로 최적화

## 개발 메모

- `DataItem`과 `DataItemEntity`를 수정하기 전 전체 usage를 확인해야 합니다.
- 클러스터링에는 내용 유사도뿐 아니라 날짜 정보가 반드시 포함되어야 합니다.
- `AiProposal`은 현재 실험적 구조이므로 변경 또는 삭제 가능성을 열어둡니다.
- 최종 주제 선정은 AI가 아니라 사용자가 해야 합니다.
- AI Agent의 출력은 바로 실행하지 않고 사용자가 확인 가능한 초안으로 관리합니다.
