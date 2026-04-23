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
