---
paths:
  - "**/*.kt"
---

# Compose 규칙 (Jetpack Compose)

## 네이밍 규칙

| 케이스 | 규칙 | 예시 |
|---|---|---|
| `Unit` 반환 `@Composable` | PascalCase 명사 | `FancyButton`, `NameTag` |
| 값 반환 `@Composable` | camelCase 동사 | `defaultStyle()` |
| `remember {}` 반환 팩토리 함수 | `remember` prefix 필수 | `rememberCoroutineScope()` |
| `CompositionLocal` 키 | `Local` prefix (형용사로) | `LocalTheme` (❌ `ThemeLocal`) |

---

## Element 기본 규칙

- [필수] `@Composable` Element는 반드시 `Unit`을 반환한다. 값을 반환하지 않는다.
- [필수] `modifier: Modifier = Modifier`를 **첫 번째 optional 파라미터**로 선언한다.
- [필수] `Modifier` 파라미터는 하나만 허용한다.
- [필수] 전달받은 `modifier`는 루트 노드에 전달하고, 추가 modifier는 **뒤에** 붙인다.

```kotlin
// DO
@Composable
fun UserCard(
    user: User,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier = modifier.clickable { onClick() }) { ... }
}

// DON'T — modifier가 첫 번째 optional 파라미터가 아님
@Composable
fun UserCard(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier, // 순서 위반
) { ... }
```

---

## 상태(State) 규칙

- [권장] **Stateless** 컴포저블을 우선한다. 상태는 호출자가 소유하고 파라미터로 전달한다.
- [권장] 관련 상태/콜백이 3개 이상이면 `@Stable` 인터페이스로 묶어 호이스팅한다.
- 호이스팅 상태 타입 이름: `[Composable명]State` (예: `VerticalScrollerState`)
- `remember { FooState() }` 형태로 default argument 제공한다.
- `null`을 내부 `remember {}` 여부를 나타내는 sentinel로 사용하지 않는다.
- 호이스팅 상태 타입은 **interface**로 선언한다.

```kotlin
// DO — @Stable interface로 호이스팅 상태 선언, remember로 default 제공
@Stable
interface PlayerState {
    val isPlaying: Boolean
    fun play()
    fun pause()
}

@Composable
fun rememberPlayerState(): PlayerState = remember { PlayerStateImpl() }

@Composable
fun AudioPlayer(
    state: PlayerState = rememberPlayerState(),
    modifier: Modifier = Modifier,
) { ... }

// DON'T — null을 sentinel로 사용해 내부 remember 여부를 표현
@Composable
fun AudioPlayer(
    state: PlayerState? = null,  // null이면 내부에서 remember, 외부에서 주입 가능 — 의도 불명확
    modifier: Modifier = Modifier,
) {
    val resolvedState = state ?: remember { PlayerStateImpl() }
}

// DON'T — 호이스팅 상태를 class로 선언, 테스트 시 상속 불가, Mock 어려움
class PlayerState {  // interface가 아닌 class
    var isPlaying by mutableStateOf(false)
}
```

### Flow 수집
- [필수] Composable에서 ViewModel의 `StateFlow`를 수집할 때는 `collectAsStateWithLifecycle()`을 사용한다.
- [필수] `collectAsState()`는 라이프사이클을 인식하지 못하므로 사용하지 않는다.

```kotlin
// DO
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// DON'T — 백그라운드에서도 수집 지속, 불필요한 리컴포지션 유발
val uiState by viewModel.uiState.collectAsState()
```

---

## @Stable / @Immutable

- [필수] 완전히 불변인 타입(모든 필드가 `val`이고 중첩 타입도 불변)에는 `@Immutable`을 적용한다.
- [필수] 모든 필드가 `State` 또는 `val`이지만 완전 불변이 아닌 경우 `@Stable`을 적용한다.
- [필수] 한 번 선언한 `@Stable` / `@Immutable`은 제거하지 않는다.

```kotlin
// DON'T — @Stable 제거 시 Compose 컴파일러가 해당 타입을 unstable로 간주,
// 파라미터로 전달할 때마다 불필요한 리컴포지션 발생
// @Stable  ← 제거 금지
class ScrollState {
    var value: Int by mutableStateOf(0)
}
```

### UiState / UiEvent / SideEffect 어노테이션
- [필수] `UiState`(`data class`)에는 `@Immutable`을 붙인다.
- [필수] `UiEvent`, `SideEffect`(`sealed interface`)에는 `@Immutable`을 붙인다.

```kotlin
@Immutable
data class FooUiState(
    val items: ImmutableList<Item> = persistentListOf(),
    val isLoading: Boolean = false,
)

@Immutable
sealed interface FooUiEvent {
    data object ClickButton : FooUiEvent
    data class UpdateItem(val item: Item) : FooUiEvent
}

@Immutable
sealed interface FooSideEffect {
    data object NavigateBack : FooSideEffect
    data class ShowToast(val message: String) : FooSideEffect
}
```

---

## 성능 최적화

### 무거운 객체는 remember { } 로 캐싱
- [권장] 리컴포지션마다 재생성되는 무거운 객체는 `remember { }`로 캐싱한다.

```kotlin
// DO
val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

// DON'T — 리컴포지션마다 객체 재생성
val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
```

### 컬렉션 타입
- [필수] `List`, `Map`을 Composable 파라미터로 직접 전달하지 않는다.
- [필수] `ImmutableList` 또는 `@Stable` 래퍼 클래스를 사용한다.

```kotlin
// DO
@Composable
fun ItemList(items: ImmutableList<Item>) { ... }

// DON'T — List는 unstable 타입, 리컴포지션 최적화 불가
@Composable
fun ItemList(items: List<Item>) { ... }
```

### LazyLayout 필수 규칙
- [필수] `key`를 반드시 설정한다. **인덱스를 key로 사용하지 않는다.**
- [필수] `itemsIndexed`를 사용할 때도 `key` 파라미터를 반드시 지정한다.
- [필수] 아이템 종류가 2개 이상이면 `contentType`을 반드시 명시한다.

```kotlin
// DO
LazyColumn {
    items(
        items = list,
        key = { it.id },
        contentType = { it::class },
    ) { item -> ItemRow(item) }
}

// DO — itemsIndexed도 key 필수
LazyColumn {
    itemsIndexed(
        items = list,
        key = { _, item -> item.id },
    ) { index, item -> ItemRow(item) }
}

// DON'T — 인덱스를 key로 사용, 아이템 삽입/삭제 시 잘못된 재사용 발생
LazyColumn {
    itemsIndexed(list) { index, item -> ItemRow(item) }
}
```

### 자주 변경되는 상태 읽기
- [권장] 자주 변경되는 상태는 람다 내부에서 읽어 리컴포지션 범위를 최소화한다.

```kotlin
// DO — 람다 내부에서 읽기, Layout 단계에서만 재실행
Modifier.offset { IntOffset(x = 0, y = scrollState.value) }

// DON'T — Composition 단계에서 읽기, 스크롤마다 리컴포지션 발생
Modifier.offset(y = scrollState.value.dp)
```

### 파라미터 그룹화
- [권장] 파라미터가 너무 많으면 관련 항목을 `data class`로 묶는다.
