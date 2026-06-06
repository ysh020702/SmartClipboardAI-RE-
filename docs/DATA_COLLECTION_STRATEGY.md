# 데이터 수집 전략

SmartClipboardAI는 "몰래 수집"하는 앱이 아니라, Android 정책 안에서 사용자 액션을 최소화해 저장하고 나중에 작업으로 연결하는 앱입니다.

## Android API 제약상 불가능한 것

### 백그라운드 클립보드 지속 감시 불가

Android 10(API 29)부터 포커스 없는 백그라운드 앱은 클립보드를 읽을 수 없습니다. 우리 앱이 foreground이거나, 투명 Activity로 포커스를 얻은 순간에만 가장 최근 Primary Clip을 읽는 방향으로 설계합니다.

### 다른 앱의 공유 자동 가로채기 불가

다른 앱이 다른 대상으로 공유하려는 흐름을 중간에서 가로챌 수 없습니다. Android Share Sheet에서 사용자가 SmartClipboardAI를 직접 선택한 경우만 수집합니다.

### 화면 내용 자동 읽기 금지

화면 내용을 자동으로 읽어 저장하는 방식은 정책 리스크가 높습니다. 접근성 API를 무단 수집 목적으로 사용하지 않습니다.

## 가능한 수집 흐름

### 1. Share Target 기반 링크/텍스트/이미지 수신

사용자 액션:

- 다른 앱에서 공유 버튼을 누릅니다.
- Share Sheet에서 SmartClipboardAI를 선택합니다.

필요 컴포넌트:

- `ShareReceiverActivity`
- `AndroidShareContentHandler`
- `ACTION_SEND`
- `ACTION_SEND_MULTIPLE`

Manifest 작업:

- `text/plain`
- `image/*`
- `application/pdf`
- `application/octet-stream`

권한:

- 공유받은 URI는 intent grant 범위 안에서 읽습니다.
- 필요한 경우 내부 저장소로 복사합니다.

실패 처리:

- 지원하지 않는 intent action
- 읽기 권한 없음
- 파일 복사 실패
- 비어 있는 공유 데이터

현재 상태:

- 기본 구현이 존재합니다.
- 저장 피드백과 UX는 개선 task에서 다시 다룹니다.

### 2. Quick Settings Tile + Transparent Activity 기반 최근 클립보드 수집

사용자 액션:

- 사용자가 텍스트/링크를 복사합니다.
- Quick Settings Tile을 누릅니다.

필요 컴포넌트:

- `ClipboardCaptureTileService`
- `ClipboardCaptureActivity`
- `AndroidClipboardDataSource`
- `DefaultClipboardCaptureHandler`

Manifest 작업:

- `TileService` 등록
- `android.permission.BIND_QUICK_SETTINGS_TILE`
- 투명 Activity theme

권한:

- 별도 runtime permission은 없지만 foreground/focus 제약이 있습니다.

실패 처리:

- 클립보드 비어 있음
- 텍스트 변환 실패
- 중복 저장
- Activity launch 실패

현재 상태:

- 기본 구현이 존재합니다.
- TileService가 직접 읽지 않고 Activity를 여는 방향은 유지합니다.

### 3. 앱 실행 시 Last Sync Time 기준 MediaStore Batch Query

사용자 액션:

- 사용자가 앱을 엽니다.
- 이미지 접근 권한을 허용합니다.
- 새로 발견된 항목을 검토하고 저장합니다.

필요 컴포넌트:

- `MainActivity`
- `MainMediaPermissionHelper`
- `AndroidMediaStoreDataSource`
- `MediaImportHandler`
- 새 검토 UI

Manifest/권한:

- API 33 이상: `READ_MEDIA_IMAGES`
- API 34 이상: `READ_MEDIA_VISUAL_USER_SELECTED` 고려
- API 32 이하: `READ_EXTERNAL_STORAGE`

구현 방향:

- Last Sync Time 저장소를 둡니다.
- MediaStore에서 Last Sync Time 이후 이미지/스크린샷 후보를 query합니다.
- 후보를 자동 저장하지 않고 검토 화면에 보여줍니다.
- 사용자가 선택한 항목만 `DataItem`으로 저장합니다.

실패 처리:

- 권한 없음
- 부분 접근으로 일부 URI만 보임
- MediaStore column 접근 실패
- 중복 URI
- 이미지 로딩 실패

현재 상태:

- 최근 100개 MediaStore 조회와 screenshot 판별이 있습니다.
- Last Sync Time 기반 검토 후 저장 흐름은 아직 없습니다.

### 4. Storage Access Framework 직접 파일 선택

사용자 액션:

- 사용자가 앱에서 파일 선택 버튼을 누릅니다.
- Android system picker에서 파일을 선택합니다.

필요 컴포넌트:

- `ActivityResultContracts.GetContent`
- `ActivityResultContracts.GetMultipleContents`
- 또는 `Intent.ACTION_GET_CONTENT`

권한:

- SAF picker가 반환한 URI grant를 사용합니다.
- 장기 접근이 필요하면 persistable permission 여부를 별도로 검토합니다.

실패 처리:

- 선택 취소
- 지원하지 않는 MIME type
- URI 읽기 실패
- 파일 크기 과대

현재 상태:

- 명시적 SAF picker 구현은 아직 없습니다.

## 링크 OG 태그 추출

링크 미리보기는 Open Graph tag를 읽어 보강합니다.

필요 정보:

- `og:title`
- `og:description`
- `og:image`

구현 원칙:

- Jsoup 같은 HTML parser 사용 가능
- 네트워크 작업이므로 `Dispatchers.IO`에서 처리
- 실패해도 원본 URL 저장은 유지

## 이미지 OCR

이미지 OCR은 가능한 경우 ML Kit 또는 팀에서 개발 중인 로컬 OCRProcessor 구조를 활용합니다.

현재 레포에는 `OCRProcessor` 클래스가 아직 없습니다. 이후 OCR task에서 인터페이스와 저장 위치를 먼저 정의해야 합니다.
