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
import kotlinx.coroutines.launch
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
        val job = launch(mainDispatcherRule.testDispatcher) {
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
