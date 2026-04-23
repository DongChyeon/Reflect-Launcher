package com.dongchyeon.reflect.core.data.repository

import com.dongchyeon.reflect.core.data.datasource.AppUsageInfo
import com.dongchyeon.reflect.core.data.datasource.PreferencesDataSource
import com.dongchyeon.reflect.core.data.datasource.ScreenTimeDataSource
import com.dongchyeon.reflect.core.data.datasource.UsageStatsDataSource
import io.mockk.coEvery
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
        coEvery { preferencesDataSource.incrementOrResetVisitCount() } returns Unit

        repository.incrementVisitCount()

        coVerify { preferencesDataSource.incrementOrResetVisitCount() }
    }
}
