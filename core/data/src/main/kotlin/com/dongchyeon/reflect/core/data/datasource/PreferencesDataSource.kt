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
