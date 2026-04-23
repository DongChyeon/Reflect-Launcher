package com.dongchyeon.reflect.core.domain.usecase

import com.dongchyeon.reflect.core.domain.repository.HomeRepository

class IncrementVisitCountUseCase(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke() = homeRepository.incrementVisitCount()
}
