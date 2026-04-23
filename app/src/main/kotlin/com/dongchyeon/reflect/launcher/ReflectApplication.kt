package com.dongchyeon.reflect.launcher

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import com.dongchyeon.reflect.launcher.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class ReflectApplication : Application() {

    @Inject
    lateinit var homeRepository: HomeRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private val screenReceiver: ScreenStateReceiver by lazy {
        ScreenStateReceiver(homeRepository, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(screenReceiver)
    }
}
