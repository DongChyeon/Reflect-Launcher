# 동시성 규칙 (코루틴)

## 스코프 선택 기준

| 스코프 | 사용 시점 |
|---|---|
| `viewModelScope` | ViewModel 생명주기에 묶인 작업 |
| `lifecycleScope` | Activity / Fragment 생명주기에 묶인 작업 |
| 커스텀 `CoroutineScope` | 생명주기를 직접 관리해야 하는 경우 |

- [필수] **`GlobalScope` 사용을 금지한다.** 앱이 종료되어도 코루틴이 계속 실행되어 메모리 누수를 유발한다.

```kotlin
// DON'T
GlobalScope.launch {
    fetchData()
}

// DO — 적절한 스코프 사용
viewModelScope.launch {
    fetchData()
}
```

## Job 재사용 시 취소
- [필수] `Job` 변수를 재사용할 때는 이전 Job을 반드시 `cancel()`하고 시작한다.

```kotlin
// DO
private var job: Job? = null

fun onEvent() {
    job?.cancel()
    job = scope.launch { doWork() }
}

// DON'T — 이전 Job이 취소되지 않아 중복 실행
private var job: Job? = null

fun onEvent() {
    job = scope.launch { doWork() }
}
```

## async / await 사용 기준
- [필수] `async`는 **병렬 실행**이 필요할 때만 사용한다. 단일 작업에는 `withContext`를 사용한다.

```kotlin
// DO — 두 작업을 병렬로 실행
val userDeferred = async { userRepository.getUser(id) }
val postsDeferred = async { postRepository.getPosts(id) }
val user = userDeferred.await()
val posts = postsDeferred.await()

// DON'T — 단일 작업에 async/await 사용, withContext보다 오버헤드 발생
val user = async { userRepository.getUser(id) }.await()
```

## withContext vs launch 구분
- [필수] 결과값이 필요한 비동기 작업은 `withContext`를 사용한다. 결과가 필요 없는 작업은 `launch`를 사용한다.

```kotlin
// DO — 결과 반환이 필요하면 withContext
val user = withContext(Dispatchers.IO) { api.getUser(id) }

// DON'T — 결과가 필요한데 launch 사용, 타이밍 불일치로 null 참조 위험
var user: User? = null
launch(Dispatchers.IO) { user = api.getUser(id) }
// 이 시점에 user는 아직 null일 수 있음
processUser(user)
```

## coroutineScope vs supervisorScope
- [필수] 자식 코루틴 하나의 실패가 나머지에 영향을 주면 안 될 때는 `supervisorScope`를 사용한다.
- [필수] `coroutineScope`는 자식 하나가 실패하면 나머지 자식도 모두 취소된다. 모든 자식이 함께 성공해야 하는 트랜잭션 성격의 작업에는 `coroutineScope`가 올바른 선택이다.

```kotlin
// DO — 각 작업이 독립적으로 실패/성공해야 할 때 (예: 대시보드 위젯 로딩)
supervisorScope {
    launch { fetchUser() }   // 예외 발생해도
    launch { fetchPosts() }  // 독립적으로 실행 계속됨
}

// DO — 모든 작업이 함께 성공해야 할 때 (예: 결제 처리 — 하나라도 실패하면 전체 취소)
coroutineScope {
    launch { reserveStock() }   // 예외 발생 시
    launch { processPayment() } // 함께 취소되어야 함 (일관성 보장)
}

// DON'T — 독립적이어야 할 작업에 coroutineScope 사용, 연쇄 취소 발생
coroutineScope {
    launch { fetchUserProfile() }  // 실패해도 다른 작업은 계속되어야 함
    launch { fetchRecommendations() }  // 이것도 취소됨 — 의도치 않은 동작
}
```

## async 예외 처리
- [필수] `async`로 시작한 코루틴의 예외는 `await()` 호출 시점에 발생한다. `await()`를 `try-catch`로 감싸거나 `supervisorScope` 안에서 사용한다.
- [필수] `CoroutineExceptionHandler`는 `async` 예외를 잡지 못한다.

```kotlin
// DON'T — await()를 호출하지 않아 예외가 조용히 소실됨
val deferred = async { riskyWork() }
doSomethingElse()
// deferred.await() 누락 → riskyWork()에서 발생한 예외가 전파되지 않고 무시됨

// DON'T — CoroutineExceptionHandler로 async 예외를 잡으려 함 (동작하지 않음)
val handler = CoroutineExceptionHandler { _, e -> log(e) }
val deferred = async(handler) { riskyWork() } // handler는 async 예외를 잡지 못함

// DO — await()를 try-catch로 감싸 예외 처리
val result = try {
    deferred.await()
} catch (e: Exception) {
    fallbackValue
}
```

## CancellationException 재전파
- [필수] `catch (e: Exception)`으로 `CancellationException`을 삼키지 않는다. 잡힌 경우 반드시 재전파한다.
- `CancellationException`을 삼키면 코루틴이 취소 요청을 무시하고 계속 실행된다.

```kotlin
// DON'T — CancellationException이 삼켜져 취소 불가능한 코루틴이 됨
try {
    delay(1000)
} catch (e: Exception) {
    Timber.e(e) // CancellationException도 여기서 소멸
}

// DO
try {
    delay(1000)
} catch (e: CancellationException) {
    throw e  // 반드시 재전파
} catch (e: Exception) {
    Timber.e(e)
}
```

## Flow 예외 처리
- [필수] `catch` 연산자는 업스트림(위쪽) 예외만 처리한다. `collect` 내부 예외는 잡지 못하므로 `collect` 앞에 위치시킨다.

```kotlin
// DON'T — collect 블록 내부 예외는 catch가 처리하지 못함
flow
    .catch { e -> emit(fallback) }
    .collect { processItem(it) }  // 여기서 발생한 예외는 catch를 통과하지 않음

// DO — collect 내부 예외는 collect 블록 안에서 직접 처리
flow
    .catch { e -> emit(fallback) }  // 업스트림(flow 생성 측) 예외 처리
    .collect { item ->
        try { processItem(item) }   // collect 내부 예외는 여기서 처리
        catch (e: Exception) { ... }
    }
```

## 메모리 누수 방지
- 리소스 해제 규칙(스트림, 리스너, 플레이어 등)은 `resource-lifecycle-rules.md` 참고
