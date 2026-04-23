package com.dongchyeon.reflect.core.data.repository

import com.dongchyeon.reflect.core.data.datasource.AppUsageInfo
import com.dongchyeon.reflect.core.data.datasource.PreferencesDataSource
import com.dongchyeon.reflect.core.data.datasource.ScreenTimeDataSource
import com.dongchyeon.reflect.core.data.datasource.UsageStatsDataSource
import com.dongchyeon.reflect.core.domain.model.AppSlot
import com.dongchyeon.reflect.core.domain.model.HomeData
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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
