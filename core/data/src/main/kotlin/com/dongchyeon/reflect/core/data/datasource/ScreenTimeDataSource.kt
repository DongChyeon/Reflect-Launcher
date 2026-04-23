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
