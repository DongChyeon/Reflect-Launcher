# 에러 처리 규칙

## Data 레이어
- [필수] 로컬 데이터 소스 예외를 `runCatching`으로 잡아 `DomainError`로 변환한 후 전파한다.
- [필수] Raw `Exception`을 상위 레이어로 직접 전파하지 않는다.

```kotlin
// DO
override suspend fun getAppUsage(packageName: String): Result<AppUsage> = runCatching {
    usageStatsManager.queryUsageStats(packageName).toDomain()
}.fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(DomainError.from(it)) }
)

// DON'T — raw Exception 전파, 상위 레이어가 직접 처리해야 함
override suspend fun getAppUsage(packageName: String): AppUsage =
    usageStatsManager.queryUsageStats(packageName).toDomain()
```

## Domain 레이어
- [필수] `DomainError` sealed class를 Domain 레이어에 정의한다.
- [필수] 오류 원인에 따라 타입을 분리한다.

```kotlin
// DO
sealed class DomainError : Throwable() {
    data class Storage(override val message: String?) : DomainError()
    data class Permission(override val message: String?) : DomainError()
    data class Business(override val message: String?) : DomainError()
}

// DON'T — 단일 타입으로 모든 오류 처리, UI에서 오류 종류를 구분할 수 없음
data class DomainError(override val message: String?) : Throwable()
```

## Presentation (ViewModel) 레이어
- [필수] ViewModel은 도메인 에러를 UI 메시지로 **매핑하는 책임**만 가진다.
- [필수] raw `try-catch`로 `Exception`을 직접 처리하지 않는다.

```kotlin
// DO
viewModelScope.launch {
    getAppUsageUseCase(packageName)
        .onSuccess { usage -> _uiState.update { it.copy(appUsage = usage) } }
        .onFailure { error -> _uiState.update { it.copy(errorMessage = error.toUiMessage()) } }
}

// DON'T
viewModelScope.launch {
    try {
        val usage = usageStatsRepository.getAppUsage(packageName)
        _uiState.update { it.copy(appUsage = usage) }
    } catch (e: Exception) {
        _uiState.update { it.copy(errorMessage = e.message) }
    }
}
```

- [필수] UI 메시지 변환 (`error.toUiMessage()`)은 **Presentation 레이어에서만** 수행한다.
