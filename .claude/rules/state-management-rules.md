---
paths:
  - "**/*ViewModel*.kt"
---

# 상태 관리 규칙

## 기본 원칙
- [필수] `StateFlow` + UDF(단방향 데이터 흐름) 패턴을 준수한다.
- [필수] `_uiState: MutableStateFlow`는 `private`으로 선언하고, 외부에는 `StateFlow`로 노출한다.

```kotlin
// DO
private val _uiState = MutableStateFlow(MyUiState())
val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

// DON'T — MutableStateFlow를 외부에 직접 노출, 외부에서 상태 변경 가능
val uiState = MutableStateFlow(MyUiState())
```

## Dispatcher 규칙
- [필수] `viewModelScope.launch { }` 내부에서 Dispatcher를 직접 지정하지 않는다.
- [필수] Dispatcher 지정 책임은 **Repository / UseCase 레이어**에 있다.

```kotlin
// DO — Dispatcher는 Repository에서 지정
class UserRepositoryImpl @Inject constructor(...) : UserRepository {
    override suspend fun getUser(id: String): Result<User> = withContext(Dispatchers.IO) {
        runCatching { api.getUser(id).toDomain() }
    }
}

// DON'T — ViewModel이 Dispatcher를 직접 지정
viewModelScope.launch(Dispatchers.IO) {
    val user = userRepository.getUser(id)
}
```

## SideEffect (일회성 이벤트)
- [필수] `SharedFlow`는 SideEffect(일회성 이벤트)에만 사용한다.
- [필수] Dialog, Toast, Snackbar, Navigation 이벤트는 `SideEffect`로 처리한다.

```kotlin
// DO — SideEffect를 SharedFlow로 발행, 구독자가 없어도 이벤트 유실 없음
private val _sideEffect = MutableSharedFlow<LoginSideEffect>()
val sideEffect: SharedFlow<LoginSideEffect> = _sideEffect.asSharedFlow()

fun login() {
    viewModelScope.launch {
        loginUseCase()
            .onSuccess { _sideEffect.emit(LoginSideEffect.NavigateToHome) }
            .onFailure { _sideEffect.emit(LoginSideEffect.ShowErrorToast(it.message)) }
    }
}

// DON'T — 일회성 이벤트를 UiState에 포함, 화면 복귀 시 이벤트가 재실행됨
data class LoginUiState(
    val navigateToHome: Boolean = false, // 상태 복원 시 자동으로 다시 navigate
)
```

## stateIn 주의사항
- [권장] `map` / `combine`으로 만든 Flow에 `SharingStarted.WhileSubscribed`를 사용하면
  화면 전환 시 데이터가 유실될 수 있으니 주의한다.

```kotlin
// DON'T — 화면 전환(백스택 복귀) 시 재구독으로 초기값이 잠깐 노출됨
val derived = upstream
    .map { it.toUiModel() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

// DO — 재구독 없이 마지막 값 유지가 필요하면 Eagerly 사용
val derived = upstream
    .map { it.toUiModel() }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)
```

## Flow 수집 방식
- [필수] UI에서 Flow를 수집할 때는 `repeatOnLifecycle(Lifecycle.State.STARTED)`를 사용한다.
- [필수] `launchIn`은 사용하지 않는다. Activity / Fragment가 백그라운드에 있어도 수집이 계속되어 메모리 누수를 유발한다.

```kotlin
// DO
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { ... }
    }
}

// DON'T
viewModel.uiState
    .onEach { ... }
    .launchIn(lifecycleScope)
```
