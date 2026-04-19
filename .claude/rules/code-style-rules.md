# 코드 스타일 규칙

## 네이밍 요약

| 대상 | 규칙 | 예시 |
|---|---|---|
| 클래스 / 인터페이스 | PascalCase 명사 | `ImmutableList` |
| 함수 | camelCase 동사 | `sendMessage()` |
| 상수 (`const val`, top-level `val`) | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Enum 상수 | PascalCase | `Idle`, `Busy` |
| Sealed class 객체 | PascalCase | `Loading`, `Done` |
| 일반 변수 / 파라미터 | camelCase | `userName` |
| Backing property | `_` prefix | `_uiState` |
| Boolean 변수 / 함수 | `is` / `has` / `should` / `can` prefix | `isVisible`, `hasData` |
| 패키지 | lowercase, 언더스코어 없음 | `com.example.deepspace` |
| 테스트 클래스 | `[대상클래스]Test` | `UserViewModelTest` |

## 파일명
- [필수] 단일 최상위 클래스 → 클래스명과 동일 (예: `MyClass.kt`)
- [필수] 복수 최상위 선언 → PascalCase 설명적 이름 (예: `Extensions.kt`)

## 포맷팅
- [필수] 들여쓰기: 공백 4칸 (탭 금지)
- [필수] 줄 길이: **100자 제한**
- [필수] 한 줄에 하나의 구문. 세미콜론 금지.
- [필수] 와일드카드 import 금지. import는 ASCII 정렬, 단일 목록.

```kotlin
// DON'T — 와일드카드 import, 어떤 심볼이 사용되는지 불명확
import com.example.util.*

// DO — 명시적 import
import com.example.util.DateFormatter
import com.example.util.StringExt
```

## 중괄호
- [필수] `if`, `for`, `when`, `do`, `while` 블록은 단일 구문이어도 **중괄호 필수**.
- 단, 한 줄로 표현 가능한 `if` 표현식은 예외.
- [필수] K&R 스타일: 여는 중괄호는 같은 줄에.

```kotlin
// DON'T — 중괄호 없이 멀티라인, 리팩터링 시 범위 오류 위험
if (condition)
    doSomething()

// DO — 중괄호 필수
if (condition) {
    doSomething()
}

// DO — 한 줄 표현식은 예외 허용
val result = if (condition) valueA else valueB
```

