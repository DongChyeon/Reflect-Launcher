package com.dongchyeon.reflect.core.domain.repository

import com.dongchyeon.reflect.core.domain.model.HomeData
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getHomeData(): Flow<HomeData>
    suspend fun incrementVisitCount()
    suspend fun saveLastScreenOffTime(timestamp: Long)
}
