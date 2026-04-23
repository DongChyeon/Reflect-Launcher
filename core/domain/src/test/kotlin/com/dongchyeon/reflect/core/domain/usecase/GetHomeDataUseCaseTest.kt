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
