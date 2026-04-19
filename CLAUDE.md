# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 앱 개요

ReflectLauncher는 숫자와 경고 대신 시각적 변화만으로 폰 사용 패턴을 자각하게 만드는 미니멀 안드로이드 런처 앱이다. 네트워크 통신 없이 `UsageStatsManager`와 `SharedPreferences`/`DataStore`만 사용한다.

상세 설계는 `references/2026-04-08-minimal-awareness-launcher-design.md` 참고.

## 빌드 및 테스트 명령어

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease

# 전체 테스트
./gradlew test

# 단일 테스트 클래스
./gradlew test --tests "com.dongchyeon.reflect.launcher.ExampleUnitTest"

# 단일 테스트 메서드
./gradlew test --tests "com.dongchyeon.reflect.launcher.ExampleUnitTest.addition_isCorrect"

# 코드 스타일 검사
./gradlew ktlintCheck

# 코드 스타일 자동 수정
./gradlew ktlintFormat

# 린트
./gradlew lint
```

## 아키텍처

멀티모듈 클린 아키텍처를 채택한다. 레이어 의존 방향:

```
Presentation → Domain ← Data
```

- **Domain**: Repository 인터페이스, UseCase, DomainError sealed class. Android 의존성 없음.
- **Data**: UsageStatsManager, DataStore, SharedPreferences 구현체. Domain 인터페이스를 구현.
- **Presentation**: Compose UI, ViewModel. MVI 패턴(UiState / UiEvent / SideEffect).

ViewModel은 Repository를 직접 의존하지 않고 UseCase를 통해서만 접근한다.

## UI 패턴

Jetpack Compose + MVI. 각 화면은 다음 구조를 따른다:

```kotlin
@Immutable data class FooUiState(...)
@Immutable sealed interface FooUiEvent { ... }
@Immutable sealed interface FooSideEffect { ... }

@HiltViewModel
class FooViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFooUseCase: GetFooUseCase,
) : ViewModel()
```

- `StateFlow` + UDF(단방향 데이터 흐름)
- 일회성 이벤트(Toast, Navigation 등)는 `SharedFlow`로 SideEffect 처리
- Composable에서 Flow 수집은 `collectAsStateWithLifecycle()` 사용

## 에러 처리

Data 레이어에서 `runCatching`으로 예외를 잡아 `DomainError`로 변환 후 상위에 전파한다.

```kotlin
sealed class DomainError : Throwable() {
    data class Storage(override val message: String?) : DomainError()
    data class Permission(override val message: String?) : DomainError()
    data class Business(override val message: String?) : DomainError()
}
```

## 주요 Android API

- `UsageStatsManager`: 앱별 오늘 사용 시간 조회 (`PACKAGE_USAGE_STATS` 권한 필요)
- `BroadcastReceiver`: `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` 수신으로 화면 상태 추적
- `DataStore` (Preferences): 오늘 방문 횟수, 의도 문장, 앱 슬롯 저장

## 브랜치 및 커밋 규칙

브랜치: `main` (배포) / `develop` (통합) / `feature/기능명` / `hotfix/이슈명`

커밋 메시지는 Conventional Commits + 이슈 번호:
```
issue #1 feat: 홈 화면 UI 구현
issue #3 fix: 앱 서랍 스크롤 위치 초기화 버그 수정
```

`main`에 직접 push 금지. PR은 최소 1인 리뷰 + CI 통과 필요.
