# UI 규칙

> Compose 전용 규칙(네이밍, @Stable/@Immutable, LazyLayout 등)은 `compose-rules.md`를 참고한다.

## 어노테이션 규칙 [필수]

- 어노테이션은 반드시 **별도 줄**에 선언한다. 인자 없는 단일 어노테이션도 예외 없음.

```kotlin
// DO
@Volatile
var disposable: Disposable? = null

// DON'T
@Volatile var disposable: Disposable? = null
```

---

## Enum 상수 규칙 [필수]

- Enum 상수는 `PascalCase`를 사용한다. (`UPPER_SNAKE_CASE` 금지)

```kotlin
// DO
enum class Status { Idle, Busy, Error }

// DON'T
enum class Status { IDLE, BUSY, ERROR }
```

---

## 리소스 파일명 규칙

- [필수] 리소스 파일명은 **snake_case** + 아래 prefix를 사용한다.

| 리소스 종류 | prefix | 예시 |
|---|---|---|
| 아이콘 (vector) | `ic_` | `ic_arrow_back.xml` |
| 배경 drawable | `bg_` | `bg_button_primary.xml` |
| 이미지 | `img_` | `img_onboarding.webp` |
| selector | `sel_` | `sel_tab_item.xml` |

---

## 접근성 규칙 [필수]

- 의미 있는 이미지에는 반드시 `contentDescription`을 지정한다.
- 장식용 이미지는 명시적으로 null 처리한다.

```kotlin
// DO — 의미 있는 이미지
Icon(
    painter = painterResource(R.drawable.ic_close),
    contentDescription = stringResource(R.string.desc_close),
)

// DO — 장식용 이미지
Icon(
    painter = painterResource(R.drawable.ic_decoration),
    contentDescription = null,
)

// DON'T — contentDescription 누락
Icon(
    painter = painterResource(R.drawable.ic_close),
    contentDescription = "close", // 하드코딩 금지 — strings.xml 사용
)
```
