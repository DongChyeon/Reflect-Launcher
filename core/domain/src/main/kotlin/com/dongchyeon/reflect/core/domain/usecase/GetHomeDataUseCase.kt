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
