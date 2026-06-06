# UX Flow

## UX 원칙

SmartClipboardAI는 "알아서 몰래 수집하는 앱"이 아닙니다. 사용자의 명시적 액션을 최소화해 자료를 저장하고, 나중에 작업 주제로 묶어 실행 가능한 초안으로 연결하는 앱입니다.

사용자는 항상 다음 권한을 가집니다.

- 무엇을 저장할지 선택할 권한
- 어떤 Topic으로 묶을지 결정할 권한
- AI Agent 초안을 수정할 권한
- 외부 앱 실행 전 최종 확인할 권한

## 링크/텍스트 저장 플로우

1. 사용자가 브라우저, 메신저, 문서 앱에서 텍스트나 링크를 공유합니다.
2. Share Sheet에서 SmartClipboardAI를 선택합니다.
3. 앱은 투명/바텀시트 형태의 짧은 피드백을 보여줍니다.
4. 데이터는 `DataItemType.TEXT` 또는 `DataItemType.LINK`로 저장됩니다.
5. 앱을 열면 새 항목이 Inbox 또는 데이터 리스트에 보입니다.
6. 사용자는 Topic에 붙이거나 나중에 정리합니다.

대체 흐름:

1. 사용자가 텍스트/링크를 복사합니다.
2. Quick Settings Tile을 누릅니다.
3. 투명 Activity가 잠깐 열려 Primary Clip을 저장합니다.
4. 저장 피드백 후 종료됩니다.

## 스크린샷/이미지 저장 플로우

1. 사용자가 앱을 엽니다.
2. 앱은 Last Sync Time 이후의 MediaStore 후보를 찾습니다.
3. 새 이미지/스크린샷 후보를 검토 화면에 보여줍니다.
4. 사용자는 필요한 항목만 선택해 저장합니다.
5. 저장된 항목은 `DataItemType.SCREENSHOT` 또는 `DataItemType.IMAGE`가 됩니다.
6. OCR/enrichment 상태를 표시합니다.

## 앱 실행 시 새 데이터 감지 및 정리 플로우

1. 앱 진입 시 새 수집 후보와 최근 Topic을 함께 보여줍니다.
2. 새 후보가 있으면 "검토 후 저장" 영역에 표시합니다.
3. 저장된 DataItem은 타입, 날짜, 출처, cluster 정보 기준으로 정렬됩니다.
4. 사용자는 바로 Topic 생성으로 이어갈 수 있습니다.

## 수집 데이터 리스트/필터/선택 플로우

1. 사용자는 수집된 데이터를 전체/텍스트/링크/이미지/스크린샷/파일로 필터링합니다.
2. 날짜 필터와 타입 필터는 동시에 적용될 수 있습니다.
3. 필터로 화면에서 사라진 항목도 이미 선택한 상태라면 선택이 유지됩니다.
4. 선택된 항목은 Topic 생성 또는 기존 Topic 추가로 이어집니다.

## Topic 생성 플로우

1. 사용자가 정리할 주제를 입력합니다.
2. 사용자는 직접 데이터를 선택하거나 Gemini 후보 선택을 요청합니다.
3. Gemini 추천은 임시 UI 상태로 표시됩니다.
4. 사용자가 확인하면 `Topic`이 생성됩니다.
5. Topic 상세에서 연결된 자료, 분석 결과, 작업 초안을 확인합니다.

## AI Agent 분석 결과 확인 플로우

1. 사용자가 Topic 상세에서 분석을 요청합니다.
2. Gemini Agent가 Topic과 DataItem을 입력으로 받습니다.
3. Agent는 `TopicAnalysis`와 `TopicAction` 초안을 생성합니다.
4. UI는 초안임을 명확히 표시합니다.
5. 사용자가 제목, 본문, 날짜, 대상 앱을 수정합니다.

## Samsung Notes/Calendar 연동 전 사용자 검토 플로우

### Notes

- 초기 MVP에서는 Samsung Notes 전용 SDK/Intent에 의존하지 않고 Android 공유 초안으로 전달합니다.
- 사용자가 공유 대상에서 Notes 또는 다른 앱을 선택합니다.

### Calendar

- Calendar insert intent를 사용합니다.
- 일정 제목, 설명, 시작/종료 시간은 초안으로 전달됩니다.
- 사용자가 Calendar 앱에서 저장 여부를 최종 결정합니다.

## 화면 단위 제안

UI/UX 재작업은 아래 화면 단위로 분리합니다.

- Home: Topic 시작, 새 후보 검토, 진행 중 초안
- Capture Feedback: Share/Tile 저장 피드백
- Media Review: 새 이미지/스크린샷 검토 후 저장
- Data Browser: 타입/날짜 필터와 선택
- Topic Create: 주제 입력, 직접 선택, Gemini 후보
- Topic Detail: 자료/분석/작업 초안 탭
- Action Review: 초안 편집과 외부 앱 전달

## 기존 UI에서 참고할 것

- One UI inspired palette
- Share/Tile의 투명 Activity 피드백 패턴
- Data list filter chip 구조
- 선택 상태 유지 아이디어
- Topic 상세의 자료/분석/작업 탭 구조
- Handoff formatter/launcher 초기 구조

## 기존 UI에서 버릴 것 또는 강하게 수정할 것

- 데이터 리스트가 앱의 중심처럼 보이는 흐름
- AI가 아닌 휴리스틱 추천을 AI처럼 보이게 하는 표현
- `MainScreen.kt` 단일 대형 파일 중심 개발
- 자동 저장처럼 보이는 MediaStore import
- 사용자가 다음에 무엇을 해야 하는지 불명확한 CTA
