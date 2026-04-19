---
paths:
  - "**/navigation/**/*.kt"
  - "**/*NavGraph*.kt"
  - "**/*Navigation*.kt"
---

# 내비게이션 규칙 (Compose Navigation)

## Route 정의
- [필수] Route는 `sealed class` 또는 `object`로 타입 안전하게 정의한다.
- [필수] 문자열 리터럴을 `navigate()` 호출부에 직접 작성하지 않는다.

```kotlin
// DO — sealed class로 타입 안전하게 Route 정의
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{id}") {
        fun createRoute(id: String) = "detail/$id"
    }
}

// DON'T — 문자열 리터럴 직접 사용, 오타 시 런타임 오류
navController.navigate("detail/$id")        // 정의와 불일치 시 즉시 크래시
navController.navigate("hom")               // 오타가 컴파일 타임에 잡히지 않음
```

## 화면 간 데이터 전달
- [필수] 화면 간에는 **최소 단위(ID 등)만** 전달한다.
- [필수] 복잡한 객체를 Navigation 인자로 직접 전달하지 않는다.
- [필수] 전달받은 ID로 ViewModel에서 데이터를 다시 조회한다.

```kotlin
// DO — ID만 전달, 데이터는 ViewModel에서 조회
navController.navigate(Screen.Detail.createRoute(itemId = item.id))

// DON'T — 복잡한 객체 직접 전달 (직렬화 오류, 상태 불일치 위험)
navController.navigate(Screen.Detail.createRoute(item = item)) // Parcelable 전달
```

## ViewModel 공유

| 함수 | 사용 시점 |
|---|---|
| `hiltViewModel()` | 해당 화면 전용 ViewModel |
| `hiltNavGraphViewModel()` | 특정 NavGraph 내 여러 화면이 공유하는 ViewModel |

## 내비게이션 이벤트 처리
- [필수] 내비게이션 이벤트는 `SideEffect`(일회성 이벤트)로 처리한다.
- [필수] ViewModel이 `NavController`를 직접 참조하지 않는다.

```kotlin
// DON'T — ViewModel이 NavController를 직접 참조
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val navController: NavController, // 금지
) : ViewModel()

// DO — SideEffect로 이벤트 발행, UI에서 처리
_sideEffect.emit(LoginSideEffect.NavigateToHome)
```

## Back Stack 관리
- [권장] 중복 화면 진입 방지: `launchSingleTop = true`
- [권장] 로그인 후 뒤로가기 방지 등: `popUpTo`로 스택 정리

```kotlin
// DO — 로그인 성공 후 로그인 화면 스택 제거
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Login.route) { inclusive = true }
    launchSingleTop = true
}
```
