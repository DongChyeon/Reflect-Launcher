# 아키텍처 규칙

## 클린 아키텍처 레이어 의존 방향

```
Presentation → Domain ← Data
```

- [필수] 외부 레이어(Presentation, Data)는 내부 레이어(Domain)에 의존한다.
- [필수] Domain 레이어는 다른 레이어를 절대 의존하지 않는다.
- Hilt 의존성 추가는 예외적으로 허용한다. (예: `:app` 모듈이 `:data` 모듈을 구현)

### Presentation 레이어
- [필수] UI 관련 모든 것을 담당한다. (ViewModel, Composable)
- [필수] Android 프레임워크 코드는 ViewModel에 포함하지 않는다.
- [필수] `import android.*` 금지. (AAC 관련 `android.arch.*` 는 예외)
- `R.string`, `R.dimen`, `R.color` 사용은 허용한다.
```kotlin
// DO — ViewModel은 프레임워크 독립적, 단위 테스트 가능
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
) : ViewModel() {
    fun onErrorConfirmed() {
        _sideEffect.tryEmit(UserSideEffect.ShowErrorDialog) // UI 이벤트만 발행
    }
}

// DON'T — ViewModel에서 Android 프레임워크 직접 참조, 단위 테스트 불가
import android.content.Context   // 금지
import android.widget.Toast      // 금지

class UserViewModel(private val context: Context) : ViewModel() {
    fun showError() = Toast.makeText(context, "오류", Toast.LENGTH_SHORT).show()
}
```

### Data 레이어
- [필수] 네트워크, DB 등 데이터 소스를 담당한다.
- [필수] Raw `Exception`을 상위 레이어로 전파하지 않는다.

```kotlin
// DO — Data 레이어에서 예외를 DomainError로 변환 후 전파
class UsageStatsRepositoryImpl @Inject constructor(
    private val usageStatsManager: UsageStatsManager,
) : UsageStatsRepository {
    override suspend fun getAppUsage(packageName: String): Result<AppUsage> = runCatching {
        usageStatsManager.queryUsageStats(...).toDomain()
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(DomainError.from(it)) }
    )
}

// DON'T — Exception을 그대로 전파, Presentation 레이어가 직접 처리해야 함
override suspend fun getAppUsage(packageName: String): AppUsage =
    usageStatsManager.queryUsageStats(...).toDomain()
```

### Domain 레이어
- [필수] 핵심 비즈니스 로직만 담당한다.
- [필수] Repository 인터페이스를 정의한다. (구현체는 Data 레이어에 위치)
- [필수] `DomainError` sealed class를 이 레이어에 정의한다.

```kotlin
// DO — Domain 레이어: 인터페이스 + 비즈니스 로직만 포함, 외부 의존성 없음
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
}

class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(id: String): Result<User> = userRepository.getUser(id)
}

// DON'T — Domain 레이어에서 Retrofit/Room 등 외부 라이브러리 직접 참조
import retrofit2.http.GET  // 금지: Domain이 Data 레이어에 의존
interface UserRepository {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto
}
```

---

## ViewModel 규칙

- [필수] `SavedStateHandle`은 반드시 **첫 번째** 생성자 파라미터로 선언한다.

```kotlin
// DO
@HiltViewModel
class XxxViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFooUseCase: GetFooUseCase,
) : ViewModel()

// DON'T — SavedStateHandle이 첫 번째가 아님
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val getFooUseCase: GetFooUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel()
```

---

## UseCase 규칙

- [필수] 단순 Repository 래퍼라도 반드시 UseCase로 선언한다.
- [필수] ViewModel이 Repository를 직접 의존하지 않는다.
- [필수] 행위를 명확히 표현하는 동사+명사 형태의 이름을 사용한다. (`GetUserUseCase` O, `UserUseCase` / `DoUserThings` X)

```kotlin
// DO
@HiltViewModel
class MainViewModel @Inject constructor(
    private val logEventUseCase: LogEventUseCase,
) : ViewModel()

// DON'T — Repository 직접 의존
@HiltViewModel
class MainViewModel @Inject constructor(
    private val analytics: AnalyticsRepository,
) : ViewModel()
```

---

## MVI 패턴

### UiState
- [필수] `data class`로 정의한다.
- [필수] Toast, Snackbar, Dialog 등 일회성 이벤트 관련 정보는 포함하지 않는다.
- 단, 다른 상태와 깊이 연관된 경우에는 예외적으로 포함할 수 있다.

### UiEvent
- [필수] 사용자가 트리거한 이벤트는 항상 포함한다. (예: 버튼 클릭)
- 사용자가 트리거하지 않아도 UiState 변경이 필요한 경우 정의한다.
  - 사용자 트리거 예: `ClickPlayButton`
  - 비사용자 트리거 예: `ReceiveChat`, `FailedLogin`, `LossAudioFocus`

### SideEffect
- [필수] 일회성 이벤트를 정의한다.
- Dialog, Toast, Snackbar, Navigation 이벤트 등

### reduce 함수
- [필수] **순수 함수**로 유지한다. UiState 변경 코드만 작성한다.

```kotlin
// DO — 순수 함수, 입력(state + event)만으로 결과(UiState)가 결정됨
fun reduce(state: FooUiState, event: FooUiEvent): FooUiState = when (event) {
    is FooUiEvent.ClickLogin -> state.copy(isLoading = true)
    is FooUiEvent.FailedLogin -> state.copy(isLoading = false, errorMessage = event.message)
}

// DON'T — reduce 내부에서 사이드 이펙트 발생, 순수 함수 계약 위반
fun reduce(state: FooUiState, event: FooUiEvent): FooUiState {
    if (event is FooUiEvent.ClickLogin) {
        viewModelScope.launch { loginUseCase() } // 금지: 외부 상태 변경
    }
    return state.copy(isLoading = true)
}
```
