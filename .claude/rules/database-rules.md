---
paths:
  - "**/data/**/*.kt"
  - "**/*Repository*.kt"
  - "**/*DataSource*.kt"
---

# 데이터베이스 규칙 (Room / DataStore)

## Room

- [필수] DAO 함수는 `suspend fun` 또는 `Flow`를 반환한다.
- [필수] DB 접근은 반드시 `Dispatchers.IO`에서 수행한다. (Repository에서 처리)
- [필수] DB → Domain 모델 변환은 `.toDomain()` / `.toEntity()` 확장 함수로 분리한다.
- [필수] Migration 시 `fallbackToDestructiveMigration()`을 **프로덕션에서 사용하지 않는다.**

```kotlin
// DO — suspend fun으로 선언
@Query("SELECT * FROM user WHERE id = :id")
suspend fun getUserById(id: String): UserEntity?

// DON'T — blocking 호출, 메인 스레드 호출 시 ANR 위험
@Query("SELECT * FROM user WHERE id = :id")
fun getUserById(id: String): UserEntity?
```

```kotlin
// DO
val migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE user ADD COLUMN age INTEGER")
    }
}

// DON'T — 프로덕션에서 금지, 기존 데이터 전체 삭제됨
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()
    .build()
```

## DataStore

- [권장] 데이터 특성에 따라 DataStore 타입을 선택한다.

| 타입 | 사용 시점 |
|---|---|
| `Preferences DataStore` | 단순 키-값 저장 (설정, 플래그 등) |
| `Proto DataStore` | 복잡한 구조화 데이터, 타입 안전성이 필요한 경우 |

```kotlin
// DO — 복잡한 구조는 Proto DataStore 사용
// user.proto 스키마 기반으로 타입 안전하게 저장
dataStore.updateData { current -> current.toBuilder().setName(user.name).build() }

// DON'T — 복잡한 객체를 Preferences DataStore에 JSON으로 직렬화
// 타입 안전성 없음, 스키마 변경 시 마이그레이션 불가
dataStore.edit { prefs ->
    prefs[USER_KEY] = gson.toJson(user)
}
```
