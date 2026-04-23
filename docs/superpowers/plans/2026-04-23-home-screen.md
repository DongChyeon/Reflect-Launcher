# 홈 화면 UI 구현 (issue #1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 멀티모듈 클린 아키텍처 기반으로 홈 화면(폰 없이 보낸 시간·방문 횟수·의도 문장·앱 슬롯 4개)을 구현하고, UsageStats 권한 안내 플로우를 포함하여 issue #1 체크리스트를 완료한다.

**Architecture:** `:core:domain`(순수 Kotlin, Hilt 없음) → `:core:data`(DataStore, UsageStatsManager, Hilt) ← `:app`(HiltAndroidApp, BroadcastReceiver) / `:feature:home`(Compose, HiltViewModel). `HomeRepositoryImpl`이 3개 DataSource를 `combine`으로 합쳐 `Flow<HomeData>`를 방출한다.

**Tech Stack:** Jetpack Compose, Hilt + KSP, DataStore Preferences, Kotlin Coroutines/Flow, MockK, JUnit4

---

### Task 1: Gradle 멀티모듈 구조 및 의존성 설정

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Create: `core/domain/build.gradle.kts`
- Create: `core/domain/src/main/AndroidManifest.xml`
- Create: `core/data/build.gradle.kts`
- Create: `core/data/src/main/AndroidManifest.xml`
- Create: `feature/home/build.gradle.kts`
- Create: `feature/home/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: `gradle/libs.versions.toml` 전체 교체**

```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.27"
ktlint = "12.1.2"
coreKtx = "1.18.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
appcompat = "1.6.1"
material = "1.10.0"
composeBom = "2025.01.00"
hilt = "2.51.1"
datastore = "1.1.1"
coroutines = "1.9.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
mockk = "1.13.13"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
```

- [ ] **Step 2: `settings.gradle.kts` 교체**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Reflect Launcher"
include(":app")
include(":core:domain")
include(":core:data")
include(":feature:home")
```

- [ ] **Step 3: 루트 `build.gradle.kts` 교체**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
}
```

- [ ] **Step 4: `core/domain/build.gradle.kts` 생성**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.dongchyeon.reflect.core.domain"
    compileSdk = 36

    defaultConfig { minSdk = 24 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 5: `core/domain/src/main/AndroidManifest.xml` 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 6: `core/data/build.gradle.kts` 생성**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.dongchyeon.reflect.core.data"
    compileSdk = 36

    defaultConfig { minSdk = 24 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 7: `core/data/src/main/AndroidManifest.xml` 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 8: `feature/home/build.gradle.kts` 생성**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.dongchyeon.reflect.feature.home"
    compileSdk = 36

    defaultConfig { minSdk = 24 }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 9: `feature/home/src/main/AndroidManifest.xml` 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 10: `app/build.gradle.kts` 전체 교체**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.dongchyeon.reflect.launcher"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.dongchyeon.reflect.launcher"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.activity.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

- [ ] **Step 11: 빌드 확인**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 12: 커밋**

```bash
git add gradle/libs.versions.toml settings.gradle.kts build.gradle.kts \
    core/domain/build.gradle.kts core/domain/src/main/AndroidManifest.xml \
    core/data/build.gradle.kts core/data/src/main/AndroidManifest.xml \
    feature/home/build.gradle.kts feature/home/src/main/AndroidManifest.xml \
    app/build.gradle.kts
git commit -m "feat/#1: 멀티모듈 Gradle 구조 및 의존성 설정"
```

---

### Task 2: :core:domain — 도메인 모델

**Files:**
- Create: `core/domain/src/main/kotlin/com/dongchyeon/reflect/core/domain/model/DomainError.kt`
- Create: `core/domain/src/main/kotlin/com/dongchyeon/reflect/core/domain/model/AppSlot.kt`
- Create: `core/domain/src/main/kotlin/com/dongchyeon/reflect/core/domain/model/HomeData.kt`

- [ ] **Step 1: `DomainError.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.domain.model

sealed class DomainError : Throwable() {
    data class Storage(override val message: String?) : DomainError()
    data class Permission(override val message: String?) : DomainError()
    data class Business(override val message: String?) : DomainError()

    companion object {
        fun from(throwable: Throwable): DomainError = when (throwable) {
            is DomainError -> throwable
            is SecurityException -> Permission(throwable.message)
            else -> Storage(throwable.message)
        }
    }
}
```

- [ ] **Step 2: `AppSlot.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.domain.model

data class AppSlot(
    val packageName: String,
    val label: String,
    val usageFraction: Float
)
```

- [ ] **Step 3: `HomeData.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.domain.model

import kotlin.time.Duration

data class HomeData(
    val timeWithoutPhone: Duration,
    val visitCount: Int,
    val intention: String?,
    val appSlots: List<AppSlot>,
    val hasUsagePermission: Boolean
)
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew :core:domain:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add core/domain/src/main/kotlin/
git commit -m "feat/#1: 도메인 모델 추가 (DomainError, HomeData, AppSlot)"
```

---

### Task 3: :core:domain — Repository 인터페이스, UseCase, 단위 테스트

**Files:**
- Create: `core/domain/src/main/kotlin/com/dongchyeon/reflect/core/domain/repository/HomeRepository.kt`
- Create: `core/domain/src/main/kotlin/com/dongchyeon/reflect/core/domain/usecase/GetHomeDataUseCase.kt`
- Create: `core/domain/src/main/kotlin/com/dongchyeon/reflect/core/domain/usecase/IncrementVisitCountUseCase.kt`
- Test: `core/domain/src/test/kotlin/com/dongchyeon/reflect/core/domain/usecase/GetHomeDataUseCaseTest.kt`

- [ ] **Step 1: `HomeRepository.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.domain.repository

import com.dongchyeon.reflect.core.domain.model.HomeData
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getHomeData(): Flow<HomeData>
    suspend fun incrementVisitCount()
    suspend fun saveLastScreenOffTime(timestamp: Long)
}
```

- [ ] **Step 2: `GetHomeDataUseCase.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.domain.usecase

import com.dongchyeon.reflect.core.domain.model.DomainError
import com.dongchyeon.reflect.core.domain.model.HomeData
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetHomeDataUseCase(
    private val homeRepository: HomeRepository
) {
    operator fun invoke(): Flow<Result<HomeData>> =
        homeRepository.getHomeData()
            .map { Result.success(it) }
            .catch { emit(Result.failure(DomainError.from(it))) }
}
```

- [ ] **Step 3: `IncrementVisitCountUseCase.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.domain.usecase

import com.dongchyeon.reflect.core.domain.repository.HomeRepository

class IncrementVisitCountUseCase(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke() = homeRepository.incrementVisitCount()
}
```

- [ ] **Step 4: `GetHomeDataUseCaseTest.kt` 작성 (먼저 실패하는 테스트)**

```kotlin
package com.dongchyeon.reflect.core.domain.usecase

import com.dongchyeon.reflect.core.domain.model.AppSlot
import com.dongchyeon.reflect.core.domain.model.DomainError
import com.dongchyeon.reflect.core.domain.model.HomeData
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class GetHomeDataUseCaseTest {

    private lateinit var homeRepository: HomeRepository
    private lateinit var useCase: GetHomeDataUseCase

    @Before
    fun setUp() {
        homeRepository = mockk()
        useCase = GetHomeDataUseCase(homeRepository)
    }

    @Test
    fun `invoke emits success with HomeData from repository`() = runTest {
        val expected = fakeHomeData()
        every { homeRepository.getHomeData() } returns flowOf(expected)

        val result = useCase().first()

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `invoke wraps repository exception as DomainError`() = runTest {
        every { homeRepository.getHomeData() } returns flow { throw RuntimeException("db error") }

        val result = useCase().first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError)
    }

    @Test
    fun `invoke passes through already-typed DomainError unchanged`() = runTest {
        val domainError = DomainError.Permission("no permission")
        every { homeRepository.getHomeData() } returns flow { throw domainError }

        val result = useCase().first()

        assertTrue(result.isFailure)
        assertEquals(domainError, result.exceptionOrNull())
    }

    private fun fakeHomeData() = HomeData(
        timeWithoutPhone = 30.minutes,
        visitCount = 3,
        intention = null,
        appSlots = listOf(AppSlot("com.example.app", "Example", 0.5f)),
        hasUsagePermission = true
    )
}
```

- [ ] **Step 5: 테스트 실행 — 실패 확인**

```bash
./gradlew :core:domain:test
```

Expected: 이미 UseCase가 구현되어 있으므로 PASS (Step 2-3에서 이미 구현). 모두 GREEN이어야 함.

- [ ] **Step 6: 커밋**

```bash
git add core/domain/src/
git commit -m "feat/#1: HomeRepository 인터페이스, UseCase, 단위 테스트 추가"
```

---

### Task 4: :core:data — DataSource 3개 구현

**Files:**
- Create: `core/data/src/main/kotlin/com/dongchyeon/reflect/core/data/datasource/ScreenTimeDataSource.kt`
- Create: `core/data/src/main/kotlin/com/dongchyeon/reflect/core/data/datasource/PreferencesDataSource.kt`
- Create: `core/data/src/main/kotlin/com/dongchyeon/reflect/core/data/datasource/UsageStatsDataSource.kt`

- [ ] **Step 1: `ScreenTimeDataSource.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScreenTimeDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val lastScreenOffKey = longPreferencesKey("last_screen_off_timestamp")

    val lastScreenOffTimestamp: Flow<Long> =
        dataStore.data.map { it[lastScreenOffKey] ?: 0L }

    suspend fun saveLastScreenOffTime(timestamp: Long) {
        dataStore.edit { it[lastScreenOffKey] = timestamp }
    }
}
```

- [ ] **Step 2: `PreferencesDataSource.kt` 생성**

`todayDateString()` 헬퍼는 minSdk 24 호환을 위해 `Calendar` 사용.

```kotlin
package com.dongchyeon.reflect.core.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val visitCountKey = intPreferencesKey("visit_count")
    private val lastVisitDateKey = stringPreferencesKey("last_visit_date")
    private val intentionKey = stringPreferencesKey("intention")
    private val appSlotsKey = stringPreferencesKey("app_slots")

    val visitCount: Flow<Int> = dataStore.data.map { it[visitCountKey] ?: 0 }
    val intention: Flow<String?> = dataStore.data.map {
        it[intentionKey]?.takeIf { s -> s.isNotBlank() }
    }
    val appSlotPackages: Flow<List<String>> = dataStore.data.map {
        it[appSlotsKey]?.split(",")?.filter { pkg -> pkg.isNotBlank() } ?: emptyList()
    }

    suspend fun incrementOrResetVisitCount() {
        val today = todayDateString()
        dataStore.edit { prefs ->
            val lastDate = prefs[lastVisitDateKey]
            if (lastDate != today) {
                prefs[visitCountKey] = 1
                prefs[lastVisitDateKey] = today
                prefs[intentionKey] = ""
            } else {
                prefs[visitCountKey] = (prefs[visitCountKey] ?: 0) + 1
            }
        }
    }

    private fun todayDateString(): String {
        val cal = Calendar.getInstance()
        return String.format(
            "%d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
```

- [ ] **Step 3: `UsageStatsDataSource.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.data.datasource

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject

data class AppUsageInfo(
    val packageName: String,
    val label: String,
    val usageMs: Long
)

class UsageStatsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageForPackages(packageNames: List<String>): List<AppUsageInfo> {
        if (packageNames.isEmpty()) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val usageMap = usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, System.currentTimeMillis())
            .associateBy { it.packageName }

        return packageNames.map { pkg ->
            val label = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                pkg
            }
            AppUsageInfo(
                packageName = pkg,
                label = label,
                usageMs = usageMap[pkg]?.totalTimeInForeground ?: 0L
            )
        }
    }
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew :core:data:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add core/data/src/main/kotlin/
git commit -m "feat/#1: DataSource 3개 구현 (ScreenTime, Preferences, UsageStats)"
```

---

### Task 5: :core:data — HomeRepositoryImpl, Hilt 모듈, 단위 테스트

**Files:**
- Create: `core/data/src/main/kotlin/com/dongchyeon/reflect/core/data/repository/HomeRepositoryImpl.kt`
- Create: `core/data/src/main/kotlin/com/dongchyeon/reflect/core/data/di/DataModule.kt`
- Test: `core/data/src/test/kotlin/com/dongchyeon/reflect/core/data/repository/HomeRepositoryImplTest.kt`

- [ ] **Step 1: 실패하는 테스트 먼저 작성**

`core/data/src/test/kotlin/com/dongchyeon/reflect/core/data/repository/HomeRepositoryImplTest.kt`:

```kotlin
package com.dongchyeon.reflect.core.data.repository

import com.dongchyeon.reflect.core.data.datasource.AppUsageInfo
import com.dongchyeon.reflect.core.data.datasource.PreferencesDataSource
import com.dongchyeon.reflect.core.data.datasource.ScreenTimeDataSource
import com.dongchyeon.reflect.core.data.datasource.UsageStatsDataSource
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class HomeRepositoryImplTest {

    private lateinit var screenTimeDataSource: ScreenTimeDataSource
    private lateinit var preferencesDataSource: PreferencesDataSource
    private lateinit var usageStatsDataSource: UsageStatsDataSource
    private lateinit var repository: HomeRepositoryImpl

    @Before
    fun setUp() {
        screenTimeDataSource = mockk()
        preferencesDataSource = mockk()
        usageStatsDataSource = mockk()
        repository = HomeRepositoryImpl(
            screenTimeDataSource,
            preferencesDataSource,
            usageStatsDataSource
        )
    }

    @Test
    fun `getHomeData returns correct usageFraction for most-used slot`() = runTest {
        every { screenTimeDataSource.lastScreenOffTimestamp } returns flowOf(0L)
        every { preferencesDataSource.visitCount } returns flowOf(1)
        every { preferencesDataSource.intention } returns flowOf(null)
        every { preferencesDataSource.appSlotPackages } returns flowOf(
            listOf("com.pkg.a", "com.pkg.b")
        )
        every { usageStatsDataSource.hasPermission() } returns true
        every { usageStatsDataSource.getUsageForPackages(any()) } returns listOf(
            AppUsageInfo("com.pkg.a", "App A", usageMs = 60_000L),
            AppUsageInfo("com.pkg.b", "App B", usageMs = 30_000L)
        )

        val result = repository.getHomeData().first()

        assertEquals(1.0f, result.appSlots[0].usageFraction, 0.001f)
        assertEquals(0.5f, result.appSlots[1].usageFraction, 0.001f)
    }

    @Test
    fun `getHomeData sets usageFraction to 0 when all slots unused`() = runTest {
        every { screenTimeDataSource.lastScreenOffTimestamp } returns flowOf(0L)
        every { preferencesDataSource.visitCount } returns flowOf(1)
        every { preferencesDataSource.intention } returns flowOf(null)
        every { preferencesDataSource.appSlotPackages } returns flowOf(listOf("com.pkg.a"))
        every { usageStatsDataSource.hasPermission() } returns true
        every { usageStatsDataSource.getUsageForPackages(any()) } returns listOf(
            AppUsageInfo("com.pkg.a", "App A", usageMs = 0L)
        )

        val result = repository.getHomeData().first()

        assertEquals(0f, result.appSlots[0].usageFraction, 0.001f)
    }

    @Test
    fun `getHomeData sets hasUsagePermission false when no permission`() = runTest {
        every { screenTimeDataSource.lastScreenOffTimestamp } returns flowOf(0L)
        every { preferencesDataSource.visitCount } returns flowOf(1)
        every { preferencesDataSource.intention } returns flowOf(null)
        every { preferencesDataSource.appSlotPackages } returns flowOf(emptyList())
        every { usageStatsDataSource.hasPermission() } returns false
        every { usageStatsDataSource.getUsageForPackages(any()) } returns emptyList()

        val result = repository.getHomeData().first()

        assertTrue(!result.hasUsagePermission)
    }

    @Test
    fun `incrementVisitCount delegates to preferencesDataSource`() = runTest {
        io.mockk.coEvery { preferencesDataSource.incrementOrResetVisitCount() } returns Unit

        repository.incrementVisitCount()

        coVerify { preferencesDataSource.incrementOrResetVisitCount() }
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인 (HomeRepositoryImpl 없음)**

```bash
./gradlew :core:data:test
```

Expected: FAIL (compilation error — HomeRepositoryImpl not found)

- [ ] **Step 3: `HomeRepositoryImpl.kt` 구현**

```kotlin
package com.dongchyeon.reflect.core.data.repository

import com.dongchyeon.reflect.core.data.datasource.AppUsageInfo
import com.dongchyeon.reflect.core.data.datasource.PreferencesDataSource
import com.dongchyeon.reflect.core.data.datasource.ScreenTimeDataSource
import com.dongchyeon.reflect.core.data.datasource.UsageStatsDataSource
import com.dongchyeon.reflect.core.domain.model.AppSlot
import com.dongchyeon.reflect.core.domain.model.HomeData
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class HomeRepositoryImpl @Inject constructor(
    private val screenTimeDataSource: ScreenTimeDataSource,
    private val preferencesDataSource: PreferencesDataSource,
    private val usageStatsDataSource: UsageStatsDataSource
) : HomeRepository {

    override fun getHomeData(): Flow<HomeData> = combine(
        screenTimeDataSource.lastScreenOffTimestamp,
        preferencesDataSource.visitCount,
        preferencesDataSource.intention,
        preferencesDataSource.appSlotPackages
    ) { screenOffTime, visitCount, intention, packageNames ->
        val hasPermission = usageStatsDataSource.hasPermission()
        val usageList: List<AppUsageInfo> = if (hasPermission) {
            usageStatsDataSource.getUsageForPackages(packageNames)
        } else {
            packageNames.map { pkg -> AppUsageInfo(pkg, pkg, 0L) }
        }

        val maxUsage = usageList.maxOfOrNull { it.usageMs } ?: 0L
        val appSlots = usageList.map { info ->
            val fraction = if (maxUsage == 0L) 0f else (info.usageMs / maxUsage.toFloat())
            AppSlot(
                packageName = info.packageName,
                label = info.label,
                usageFraction = fraction
            )
        }

        val elapsed: Duration = if (screenOffTime == 0L) Duration.ZERO
        else (System.currentTimeMillis() - screenOffTime).milliseconds

        HomeData(
            timeWithoutPhone = elapsed,
            visitCount = visitCount,
            intention = intention,
            appSlots = appSlots,
            hasUsagePermission = hasPermission
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun incrementVisitCount() {
        preferencesDataSource.incrementOrResetVisitCount()
    }

    override suspend fun saveLastScreenOffTime(timestamp: Long) {
        screenTimeDataSource.saveLastScreenOffTime(timestamp)
    }
}
```

- [ ] **Step 4: `DataModule.kt` 생성**

```kotlin
package com.dongchyeon.reflect.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dongchyeon.reflect.core.data.repository.HomeRepositoryImpl
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import com.dongchyeon.reflect.core.domain.usecase.GetHomeDataUseCase
import com.dongchyeon.reflect.core.domain.usecase.IncrementVisitCountUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reflect_preferences"
)

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideGetHomeDataUseCase(repo: HomeRepository): GetHomeDataUseCase =
        GetHomeDataUseCase(repo)

    @Provides
    @Singleton
    fun provideIncrementVisitCountUseCase(repo: HomeRepository): IncrementVisitCountUseCase =
        IncrementVisitCountUseCase(repo)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
./gradlew :core:data:test
```

Expected: `BUILD SUCCESSFUL`, 4 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add core/data/src/
git commit -m "feat/#1: HomeRepositoryImpl, DataModule, 단위 테스트 추가"
```

---

### Task 6: :app — AndroidManifest, Application, BroadcastReceiver, AppModule

**Files:**
- Create: `app/src/main/kotlin/com/dongchyeon/reflect/launcher/ReflectApplication.kt`
- Create: `app/src/main/kotlin/com/dongchyeon/reflect/launcher/ScreenStateReceiver.kt`
- Create: `app/src/main/kotlin/com/dongchyeon/reflect/launcher/di/AppModule.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: `AppModule.kt` 생성 — ApplicationScope 정의**

```kotlin
package com.dongchyeon.reflect.launcher.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

- [ ] **Step 2: `ScreenStateReceiver.kt` 생성**

```kotlin
package com.dongchyeon.reflect.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import com.dongchyeon.reflect.launcher.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ScreenStateReceiver(
    private val homeRepository: HomeRepository,
    @ApplicationScope private val scope: CoroutineScope
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            scope.launch {
                homeRepository.saveLastScreenOffTime(System.currentTimeMillis())
            }
        }
    }
}
```

- [ ] **Step 3: `ReflectApplication.kt` 생성**

```kotlin
package com.dongchyeon.reflect.launcher

import android.app.Application
import android.content.IntentFilter
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import com.dongchyeon.reflect.launcher.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class ReflectApplication : Application() {

    @Inject
    lateinit var homeRepository: HomeRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private val screenReceiver: ScreenStateReceiver by lazy {
        ScreenStateReceiver(homeRepository, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenReceiver, IntentFilter(android.content.Intent.ACTION_SCREEN_OFF))
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(screenReceiver)
    }
}
```

- [ ] **Step 4: `AndroidManifest.xml` 업데이트**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission
        android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />

    <application
        android:name=".ReflectApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ReflectLauncher">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

- [ ] **Step 5: 컴파일 확인**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add app/src/main/kotlin/ app/src/main/AndroidManifest.xml
git commit -m "feat/#1: Application, ScreenStateReceiver, AppModule, Manifest 설정"
```

---

### Task 7: :app — MainActivity

**Files:**
- Create: `app/src/main/kotlin/com/dongchyeon/reflect/launcher/MainActivity.kt`

- [ ] **Step 1: `MainActivity.kt` 생성**

```kotlin
package com.dongchyeon.reflect.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.dongchyeon.reflect.feature.home.ui.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    HomeScreen(
                        onLaunchApp = { packageName ->
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                                ?: Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=$packageName")
                                }
                            startActivity(intent)
                        },
                        onOpenSettings = {
                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: 디버그 빌드 확인**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/kotlin/com/dongchyeon/reflect/launcher/MainActivity.kt
git commit -m "feat/#1: MainActivity 추가"
```

---

### Task 8: :feature:home — HomeUiState, HomeUiEvent, HomeSideEffect

**Files:**
- Create: `feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/HomeUiState.kt`

- [ ] **Step 1: `HomeUiState.kt` 생성**

```kotlin
package com.dongchyeon.reflect.feature.home.ui

import androidx.compose.runtime.Immutable
import kotlin.time.Duration

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

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew :feature:home:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/HomeUiState.kt
git commit -m "feat/#1: HomeUiState, HomeUiEvent, HomeSideEffect 추가"
```

---

### Task 9: :feature:home — HomeViewModel, 단위 테스트

**Files:**
- Create: `feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/HomeViewModel.kt`
- Test: `feature/home/src/test/kotlin/com/dongchyeon/reflect/feature/home/ui/HomeViewModelTest.kt`
- Test: `feature/home/src/test/kotlin/com/dongchyeon/reflect/feature/home/util/MainDispatcherRule.kt`

- [ ] **Step 1: `MainDispatcherRule.kt` 생성 (테스트 유틸)**

```kotlin
package com.dongchyeon.reflect.feature.home.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

- [ ] **Step 2: `HomeViewModelTest.kt` 작성 (먼저 실패하는 테스트)**

```kotlin
package com.dongchyeon.reflect.feature.home.ui

import com.dongchyeon.reflect.core.domain.model.AppSlot
import com.dongchyeon.reflect.core.domain.model.HomeData
import com.dongchyeon.reflect.core.domain.usecase.GetHomeDataUseCase
import com.dongchyeon.reflect.core.domain.usecase.IncrementVisitCountUseCase
import com.dongchyeon.reflect.feature.home.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getHomeDataUseCase: GetHomeDataUseCase
    private lateinit var incrementVisitCountUseCase: IncrementVisitCountUseCase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        getHomeDataUseCase = mockk()
        incrementVisitCountUseCase = mockk()
    }

    @Test
    fun `uiState reflects HomeData from usecase`() = runTest {
        val homeData = fakeHomeData(visitCount = 5, hasPermission = true)
        every { getHomeDataUseCase() } returns flowOf(Result.success(homeData))
        coEvery { incrementVisitCountUseCase() } returns Unit

        viewModel = HomeViewModel(mockk(relaxed = true), getHomeDataUseCase, incrementVisitCountUseCase)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5, state.visitCount)
        assertTrue(state.hasUsagePermission)
    }

    @Test
    fun `appSlot alpha is low when usageFraction is high`() = runTest {
        val homeData = fakeHomeData(
            appSlots = listOf(AppSlot("pkg", "App", usageFraction = 1.0f))
        )
        every { getHomeDataUseCase() } returns flowOf(Result.success(homeData))
        coEvery { incrementVisitCountUseCase() } returns Unit

        viewModel = HomeViewModel(mockk(relaxed = true), getHomeDataUseCase, incrementVisitCountUseCase)

        val slot = viewModel.uiState.value.appSlots.first()
        assertEquals(0.2f, slot.alpha, 0.001f)
    }

    @Test
    fun `ScreenResumed event calls incrementVisitCountUseCase`() = runTest {
        every { getHomeDataUseCase() } returns flowOf(Result.success(fakeHomeData()))
        coEvery { incrementVisitCountUseCase() } returns Unit

        viewModel = HomeViewModel(mockk(relaxed = true), getHomeDataUseCase, incrementVisitCountUseCase)
        viewModel.onEvent(HomeUiEvent.ScreenResumed)

        coVerify { incrementVisitCountUseCase() }
    }

    @Test
    fun `PermissionGuideClicked emits OpenUsageAccessSettings sideEffect`() = runTest {
        every { getHomeDataUseCase() } returns flowOf(Result.success(fakeHomeData()))
        coEvery { incrementVisitCountUseCase() } returns Unit

        viewModel = HomeViewModel(mockk(relaxed = true), getHomeDataUseCase, incrementVisitCountUseCase)

        val sideEffects = mutableListOf<HomeSideEffect>()
        val job = kotlinx.coroutines.launch(mainDispatcherRule.testDispatcher) {
            viewModel.sideEffect.collect { sideEffects.add(it) }
        }

        viewModel.onEvent(HomeUiEvent.PermissionGuideClicked)

        assertTrue(sideEffects.any { it is HomeSideEffect.OpenUsageAccessSettings })
        job.cancel()
    }

    private fun fakeHomeData(
        visitCount: Int = 1,
        hasPermission: Boolean = false,
        appSlots: List<AppSlot> = emptyList()
    ) = HomeData(
        timeWithoutPhone = 30.minutes,
        visitCount = visitCount,
        intention = null,
        appSlots = appSlots,
        hasUsagePermission = hasPermission
    )
}
```

- [ ] **Step 3: 테스트 실행 — 실패 확인**

```bash
./gradlew :feature:home:test
```

Expected: FAIL (HomeViewModel not found)

- [ ] **Step 4: `HomeViewModel.kt` 구현**

```kotlin
package com.dongchyeon.reflect.feature.home.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongchyeon.reflect.core.domain.model.DomainError
import com.dongchyeon.reflect.core.domain.usecase.GetHomeDataUseCase
import com.dongchyeon.reflect.core.domain.usecase.IncrementVisitCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHomeDataUseCase: GetHomeDataUseCase,
    private val incrementVisitCountUseCase: IncrementVisitCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<HomeSideEffect>()
    val sideEffect: SharedFlow<HomeSideEffect> = _sideEffect.asSharedFlow()

    init {
        observeHomeData()
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.ScreenResumed -> onScreenResumed()
            HomeUiEvent.PermissionGuideClicked -> onPermissionGuideClicked()
            is HomeUiEvent.AppSlotClicked -> onAppSlotClicked(event.packageName)
        }
    }

    private fun observeHomeData() {
        viewModelScope.launch {
            getHomeDataUseCase().collect { result ->
                result
                    .onSuccess { data ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasUsagePermission = data.hasUsagePermission,
                                timeWithoutPhone = data.timeWithoutPhone,
                                visitCount = data.visitCount,
                                intention = data.intention,
                                appSlots = data.appSlots.map { slot ->
                                    AppSlotUi(
                                        packageName = slot.packageName,
                                        label = slot.label,
                                        alpha = lerp(1.0f, 0.2f, slot.usageFraction)
                                    )
                                }
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.toUiMessage()
                            )
                        }
                    }
            }
        }
    }

    private fun onScreenResumed() {
        viewModelScope.launch { incrementVisitCountUseCase() }
    }

    private fun onPermissionGuideClicked() {
        viewModelScope.launch { _sideEffect.emit(HomeSideEffect.OpenUsageAccessSettings) }
    }

    private fun onAppSlotClicked(packageName: String) {
        viewModelScope.launch { _sideEffect.emit(HomeSideEffect.LaunchApp(packageName)) }
    }

    private fun lerp(start: Float, stop: Float, fraction: Float) =
        start + (stop - start) * fraction

    private fun Throwable?.toUiMessage(): String = when (this) {
        is DomainError.Permission -> "사용 통계 권한이 필요합니다"
        is DomainError.Storage -> "데이터를 불러오지 못했습니다"
        else -> "오류가 발생했습니다"
    }
}
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
./gradlew :feature:home:test
```

Expected: `BUILD SUCCESSFUL`, 4 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add feature/home/src/
git commit -m "feat/#1: HomeViewModel, 단위 테스트 추가"
```

---

### Task 10: :feature:home — Composable 컴포넌트 3개

**Files:**
- Create: `feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/components/TimeWithoutPhoneText.kt`
- Create: `feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/components/AppSlotList.kt`
- Create: `feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/components/PermissionGuideOverlay.kt`

- [ ] **Step 1: `TimeWithoutPhoneText.kt` 생성**

```kotlin
package com.dongchyeon.reflect.feature.home.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration

@Composable
fun TimeWithoutPhoneText(
    duration: Duration,
    modifier: Modifier = Modifier
) {
    val totalMinutes = duration.inWholeMinutes
    val fraction = (totalMinutes / 120f).coerceIn(0f, 1f)
    // import 충돌 방지: Float lerp는 인라인 계산, TextUnit lerp는 androidx.compose.ui.unit.lerp 사용
    val alpha = 0.2f + (1.0f - 0.2f) * fraction
    val textSize = lerp(32.sp, 52.sp, fraction)

    Column(modifier = modifier) {
        Text(
            text = duration.toDisplayString(),
            style = MaterialTheme.typography.displayMedium.copy(fontSize = textSize),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
        )
        Text(
            text = "without phone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha * 0.7f)
        )
    }
}

private fun Duration.toDisplayString(): String {
    val totalMinutes = inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
```

- [ ] **Step 2: `AppSlotList.kt` 생성**

```kotlin
package com.dongchyeon.reflect.feature.home.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dongchyeon.reflect.feature.home.ui.AppSlotUi

@Composable
fun AppSlotList(
    slots: List<AppSlotUi>,
    onSlotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        slots.forEach { slot ->
            Text(
                text = slot.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = slot.alpha),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSlotClick(slot.packageName) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}
```

- [ ] **Step 3: `PermissionGuideOverlay.kt` 생성**

```kotlin
package com.dongchyeon.reflect.feature.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun PermissionGuideOverlay(
    onGuideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable { onGuideClick() }
            .padding(16.dp)
    ) {
        Text(
            text = "사용 데이터에 접근할 수 없어요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "앱 사용 현황 권한을 허용하면 시각적 피드백이 활성화됩니다",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "설정에서 허용하기 →",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew :feature:home:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/components/
git commit -m "feat/#1: Composable 컴포넌트 추가 (TimeWithoutPhoneText, AppSlotList, PermissionGuideOverlay)"
```

---

### Task 11: :feature:home — HomeScreen 통합

**Files:**
- Create: `feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/HomeScreen.kt`

- [ ] **Step 1: `HomeScreen.kt` 생성**

```kotlin
package com.dongchyeon.reflect.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongchyeon.reflect.feature.home.ui.components.AppSlotList
import com.dongchyeon.reflect.feature.home.ui.components.PermissionGuideOverlay
import com.dongchyeon.reflect.feature.home.ui.components.TimeWithoutPhoneText
import androidx.compose.runtime.LaunchedEffect

@Composable
fun HomeScreen(
    onLaunchApp: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.LaunchApp -> onLaunchApp(effect.packageName)
                HomeSideEffect.OpenUsageAccessSettings -> onOpenSettings()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(HomeUiEvent.ScreenResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp)
        ) {
            Text(
                text = "#${uiState.visitCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.End)
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                TimeWithoutPhoneText(duration = uiState.timeWithoutPhone)
            }

            uiState.intention?.let { intention ->
                Text(
                    text = intention,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            AppSlotList(
                slots = uiState.appSlots,
                onSlotClick = { packageName ->
                    viewModel.onEvent(HomeUiEvent.AppSlotClicked(packageName))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!uiState.hasUsagePermission) {
            PermissionGuideOverlay(
                onGuideClick = { viewModel.onEvent(HomeUiEvent.PermissionGuideClicked) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}
```

- [ ] **Step 2: `hilt-navigation-compose` 의존성 추가 (`hiltViewModel()` 사용)**

`feature/home/build.gradle.kts`의 `dependencies`에 추가:

```kotlin
implementation(libs.hilt.navigation.compose)
```

`gradle/libs.versions.toml`의 `[libraries]`에 추가:

```toml
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
```

- [ ] **Step 3: 전체 빌드 및 테스트 확인**

```bash
./gradlew assembleDebug test
```

Expected: `BUILD SUCCESSFUL`, 전체 테스트 PASS

- [ ] **Step 4: 커밋**

```bash
git add feature/home/src/main/kotlin/com/dongchyeon/reflect/feature/home/ui/HomeScreen.kt \
    feature/home/build.gradle.kts gradle/libs.versions.toml
git commit -m "feat/#1: HomeScreen Composable 통합 구현 완료"
```
