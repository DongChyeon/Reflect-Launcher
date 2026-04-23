package com.dongchyeon.reflect.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import com.dongchyeon.reflect.launcher.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ScreenStateReceiver(
    private val homeRepository: HomeRepository,
    @ApplicationScope private val scope: CoroutineScope
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            scope.launch {
                homeRepository.saveLastScreenOffTime(System.currentTimeMillis())
            }
        }
    }
}
