# 홈 화면 UI 구현 설계 (issue #1)

> 생성일: 2026-04-22

## 범위

issue #1 체크리스트 전체 구현:

- 폰 없이 보낸 시간 표시 (마지막 화면 OFF 타임스탬프 기반)
- 시간 값에 따른 텍스트 선명도·크기 동적 변화
- 오늘 홈 화면 방문 횟수 `#N` 표시
- 오늘의 의도 문장 표시 (없으면 숨김)
- 홈 앱 슬롯 최대 4개 텍스트 레이블 표시
- 앱 슬롯 사용량 비례 텍스트 투명도 적용
- `PACKAGE_USAGE_STATS` 권한 요청 플로우 포함

또한 이 이슈에서 전체 기술 스택을 세팅한다:

- 멀티모듈 구조 (`app`, `feature:home`, `core:domain`, `core:data`)
- Jetpack Compose + Hilt
- DataStore Preferences

---

## 모듈 구조

```
:app
  ├── :feature:home
  └── :core:data
        └── :core:domain (interface only)
:feature:home
  └── :core:domain
```

| 모듈 | 책임 | 주요 의존성 |
|---|---|---|
| `:app` | Application, Hilt 컴포넌트, 진입점 | Hilt, `:feature:home`, `:core:data` |
| `:feature:home` | 홈 화면 UI, ViewModel, MVI | Compose, Hilt, `:core:domain` |
| `:core:domain` | Repository 인터페이스, UseCase, 도메인 모델, DomainError | 순수 Kotlin (Android 의존성 없음) |
| `:core:data` | DataStore, UsageStatsManager 구현체 | DataStore, `:core:domain` |

---

## 도메인 모델

```kotlin
// :core:domain
data class HomeData(
    val timeWithoutPhone: Duration,
    val visitCount: Int,
    val intention: String?,
    val appSlots: List<AppSlot>,
    val hasUsagePermission: Boolean
)

data class AppSlot(
    val packageName: String,
    val label: String,
    val usageFraction: Float  // 0.0(미사용) ~ 1.0(슬롯 중 최다 사용)
)

sealed class DomainError : Throwable() {
    data class Storage(override val message: String?) : DomainError()
    data class Permission(override val message: String?) : DomainError()
    data class Business(override val message: String?) : DomainError()
}
```

---

## 데이터 레이어

### Repository 인터페이스

```kotlin
// :core:domain
interface HomeRepository {
    fun getHomeData(): Flow<HomeData>
    suspend fun incrementVisitCount()
    suspend fun saveLastScreenOffTime(timestamp: Long)
}
```

### UseCase

```kotlin
// :core:domain
class GetHomeDataUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {
    operator fun invoke(): Flow<Result<HomeData>> =
        homeRepository.getHomeData()
            .map { Result.success(it) }
            .catch { emit(Result.failure(DomainError.from(it))) }
}

class IncrementVisitCountUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke() = homeRepository.incrementVisitCount()
}
```

### DataSource 역할 분담

| DataSource | 저장소 | 제공 데이터 |
|---|---|---|
| `ScreenTimeDataSource` | DataStore | 마지막 화면 OFF 타임스탬프 |
| `PreferencesDataSource` | DataStore | 방문 횟수, 마지막 방문 날짜, 의도 문장, 앱 슬롯 목록 |
| `UsageStatsDataSource` | UsageStatsManager | 앱별 오늘 사용 시간(ms) |

### HomeRepositoryImpl 동작

1. `PreferencesDataSource`에서 날짜 비교 후 자정 리셋 처리
2. `ScreenTimeDataSource`에서 타임스탬프 읽어 `Duration` 계산
3. `UsageStatsDataSource`에서 앱 슬롯별 사용 시간 조회 후 `usageFraction` 계산
4. 세 소스를 조합해 `Flow<HomeData>` 방출

### 자정 리셋 로직

별도 알람/워커 없이 홈 화면 진입 시점에 처리:

1. DataStore에서 `lastVisitDate` 읽기
2. 오늘 날짜와 다르면 → `visitCount = 1`, `intention = null`, `lastVisitDate = today`
3. 같은 날이면 → `visitCount++`

### 화면 OFF 추적

```kotlin
// :app — ReflectApplication.kt
class ReflectApplication : Application() {
    private val screenReceiver = ScreenStateReceiver()

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(screenReceiver)
    }
}
```

`ACTION_SCREEN_OFF` 수신 시 현재 타임스탬프를 DataStore에 저장. `ScreenStateReceiver`는 Hilt로 `HomeRepository`를 주입받고, suspend 호출을 위해 `applicationScope` (`@ApplicationScope CoroutineScope`)을 함께 주입받는다.

> **한계:** 앱이 강제 종료되면 추적이 중단된다. 런처는 시스템이 잘 종료하지 않으므로 허용 가능한 트레이드오프로 판단하고, Foreground Service는 사용하지 않는다.

### UsageStats 권한 체크

`onResume`마다 `AppOpsManager`로 `PACKAGE_USAGE_STATS` 권한을 확인한다.

- 허용: 정상 홈 화면 (`hasUsagePermission = true`)
- 거부: `hasUsagePermission = false` → 권한 안내 오버레이 표시

---

## Presentation 레이어

### UiState / UiEvent / SideEffect

```kotlin
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val hasUsagePermission: Boolean = false,
    val timeWithoutPhone: Duration = Duration.ZERO,
    val visitCount: Int = 0,
    val intention: String? = null,
    val appSlots: List<AppSlotUi> = emptyList(),
    val errorMessage: String? = null
)

@Immutable
data class AppSlotUi(
    val packageName: String,
    val label: String,
    val alpha: Float
)

@Immutable
sealed interface HomeUiEvent {
    data object ScreenResumed : HomeUiEvent  // Activity.onResume에서 발행
    data object PermissionGuideClicked : HomeUiEvent
    data class AppSlotClicked(val packageName: String) : HomeUiEvent
}

@Immutable
sealed interface HomeSideEffect {
    data object OpenUsageAccessSettings : HomeSideEffect
    data class LaunchApp(val packageName: String) : HomeSideEffect
}
```

### 시각적 변화 계산식

**폰 없이 보낸 시간 — 선명도·크기**

```kotlin
val fraction = (elapsedMinutes / 120f).coerceIn(0f, 1f)
val alpha    = lerp(0.2f, 1.0f, fraction)   // 0분 → 0.2, 120분 이상 → 1.0
val textSize = lerp(32.sp, 52.sp, fraction)  // 0분 → 32sp, 120분 이상 → 52sp
```

2시간 이상 폰을 안 쓰면 완전히 선명해진다. 선형 보간으로 단순하게 유지.

**앱 슬롯 사용량 비례 투명도**

```kotlin
// HomeRepositoryImpl에서 usageFraction 계산 (0.0 ~ 1.0)
val maxUsage     = slots.maxOf { it.usageMs }
val usageFraction = if (maxUsage == 0L) 0f else (slot.usageMs / maxUsage.toFloat())

// AppSlotUi 변환 시 alpha 계산
val alpha = lerp(1.0f, 0.2f, appSlot.usageFraction)  // 미사용 → 불투명, 최다 사용 → 0.2
```

`usageFraction`은 Repository에서 계산해 `AppSlot` 도메인 모델에 저장. `AppSlotUi.alpha`는 ViewModel에서 변환 시 계산. 모든 슬롯이 미사용이면 전부 불투명.

### 권한 미허용 UI

`hasUsagePermission = false`일 때 하단 오버레이:

```
┌─────────────────────────────────────────┐
│  사용 데이터에 접근할 수 없어요            │
│  앱 사용 현황 권한을 허용하면             │
│  시각적 피드백이 활성화됩니다             │
│                                         │
│  설정에서 허용하기 →                     │
└─────────────────────────────────────────┘
```

탭 시 `HomeSideEffect.OpenUsageAccessSettings` 발행 → `Settings.ACTION_USAGE_ACCESS_SETTINGS`로 이동.

---

## 파일 구조 (예상)

```
:app/
  ReflectApplication.kt
  ScreenStateReceiver.kt
  MainActivity.kt
  di/
    AppModule.kt

:feature:home/
  ui/
    HomeScreen.kt
    HomeViewModel.kt
    HomeUiState.kt
    components/
      TimeWithoutPhoneText.kt
      AppSlotList.kt
      PermissionGuideOverlay.kt

:core:domain/
  model/
    HomeData.kt
    AppSlot.kt
    DomainError.kt
  repository/
    HomeRepository.kt
  usecase/
    GetHomeDataUseCase.kt
    IncrementVisitCountUseCase.kt

:core:data/
  repository/
    HomeRepositoryImpl.kt
  datasource/
    ScreenTimeDataSource.kt
    PreferencesDataSource.kt
    UsageStatsDataSource.kt
  di/
    DataModule.kt
```

---

## 의존성 (추가 필요)

| 라이브러리 | 용도 |
|---|---|
| `compose-bom` | Jetpack Compose UI |
| `hilt-android` + `hilt-compiler` | 의존성 주입 |
| `datastore-preferences` | 영속성 저장 |
| `lifecycle-viewmodel-compose` | ViewModel + Compose |
| `activity-compose` | ComponentActivity Compose 지원 |

---

## 미포함 범위 (별도 이슈)

- 아침 첫 진입 의도 입력 플로우
- 앱 슬롯 롱 프레스 편집 (앱 선택)
- 앱 서랍
- 설정 화면
- 스와이프 업 제스처 네비게이션
