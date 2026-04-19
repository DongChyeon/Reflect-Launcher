---
paths:
  - "**/test/**/*.kt"
  - "**/androidTest/**/*.kt"
---

# 테스트 규칙

> **Note**: JUnit5는 별도 Gradle 설정이 필요하다. 현재 `build.gradle.kts`는 JUnit4 기준이므로, JUnit5 도입 전까지는 `@Before`/`@After` 어노테이션을 사용한다.

## 기본 원칙
- [필수] JUnit5 기반으로 작성한다.
- [필수] 테스트 클래스명: `[대상클래스]Test` (예: `UserViewModelTest`)
- [필수] 테스트 함수명: 행동과 기대 결과를 포함해 작성한다.

```kotlin
// DO
fun `로그인 성공 시 홈 화면으로 이동하는 SideEffect가 방출된다`()
fun `잘못된 비밀번호 입력 시 에러 메시지가 표시된다`()

// DON'T — 기대 결과 없음
fun `로그인 테스트`()
fun `test1`()
```

## MockK 사용 규칙
- [필수] `mockk()` / `coEvery` / `coVerify`를 사용한다.
- [권장] Repository 테스트: **Fake 구현체를 우선** 사용한다.
- [필수] Mock은 외부 라이브러리 의존성에만 사용한다.

```kotlin
// DO — Repository 테스트에 Fake 사용
class FakeUserRepository : UserRepository {
    var users: List<User> = emptyList()
    override suspend fun getUsers() = Result.success(users)
}

// DON'T — Repository를 mockk으로 처리
val repo = mockk<UserRepository>()
coEvery { repo.getUsers() } returns Result.success(emptyList())
```

## Flow / StateFlow 테스트
- [필수] Flow 테스트 시 **Turbine**을 사용한다.

```kotlin
// DO — Turbine으로 방출 순서와 완료 상태를 명확하게 검증
@Test
fun `데이터 로드 시 아이템 목록이 방출된다`() = runTest {
    viewModel.items.test {
        val item = awaitItem()
        assertThat(item).isNotEmpty()
        cancelAndIgnoreRemainingEvents()
    }
}

// DON'T — Turbine 없이 직접 수집, 타이밍 의존적이고 완료 여부를 검증하기 어려움
@Test
fun `데이터 로드 테스트`() = runTest {
    val results = mutableListOf<List<Item>>()
    val job = launch { viewModel.items.collect { results.add(it) } }
    advanceUntilIdle()
    assertThat(results.first()).isNotEmpty()
    job.cancel() // 명시적 취소 필요, 실수 시 테스트가 hanging
}
```

## ViewModel 테스트
- [필수] `UnconfinedTestDispatcher`를 사용한다.

```kotlin
@BeforeEach
fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

@AfterEach
fun tearDown() {
    Dispatchers.resetMain()
}
```

```kotlin
// DON'T — Dispatcher 미교체 시 StateFlow 수집 타이밍 불일치 발생
@Test
fun `상태 업데이트 테스트`() = runTest {
    // Dispatchers.setMain 없이 viewModel.uiState 수집하면 불안정
    val state = viewModel.uiState.value
}
```

## 생명주기
- [필수] `@BeforeEach` / `@AfterEach`로 setUp / tearDown을 관리한다.
