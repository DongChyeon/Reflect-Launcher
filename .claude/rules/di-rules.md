---
paths:
  - "**/di/**/*.kt"
  - "**/*Module.kt"
---

# DI 규칙 (Hilt)

## 모듈 분리 기준

| 모듈 | 제공 대상 |
|---|---|
| `AppModule` | 앱 전역 싱글턴 |
| `DataModule` | Repository 구현체, DAO, DataSource |
| `DomainModule` | UseCase |

## 스코프 사용 기준

| 스코프 | 사용 시점 |
|---|---|
| `@Singleton` | 앱 전역에서 하나의 인스턴스만 필요한 경우 |
| `@ViewModelScoped` | ViewModel 생명주기와 함께하는 경우 |
| `@ActivityScoped` | Activity 생명주기와 함께하는 경우 |

- [필수] 스코프는 필요한 최소 범위를 선택한다. 상태를 갖지 않는 클래스에 `@Singleton`을 붙이지 않는다.

```kotlin
// DON'T — 상태 없는 핸들러에 @Singleton 남용
@Singleton
class ButtonClickHandler @Inject constructor() {
    fun onClick() { ... }
}

// DO — 스코프 없이 매번 생성
class ButtonClickHandler @Inject constructor() {
    fun onClick() { ... }
}
```

## 바인딩 방식
- [권장] Interface → Impl 바인딩은 `@Binds`를 우선 사용한다.
- [필수] `@Provides`는 외부 라이브러리 등 직접 생성자를 제어할 수 없는 경우에만 사용한다.

```kotlin
// DO — @Binds 우선
@Binds
abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

// DON'T — 내부 클래스에 @Provides 사용
@Provides
fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl

// @Provides는 외부 라이브러리에만
@Provides
fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = ...
```

## 테스트
- [필수] 테스트 시 `@TestInstallIn`으로 모듈을 교체한다.
- [권장] Fake 구현체를 사용해 외부 의존성을 제거한다.

```kotlin
// DO — @TestInstallIn으로 프로덕션 모듈을 Fake 모듈로 교체
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class],
)
@Module
object FakeDataModule {
    @Provides
    fun provideUserRepository(): UserRepository = FakeUserRepository()
}

// DON'T — 프로덕션 모듈을 그대로 사용, 실제 네트워크/DB 호출 발생
// @HiltAndroidTest 테스트에서 DataModule이 그대로 주입됨 → 테스트 격리 불가
```
