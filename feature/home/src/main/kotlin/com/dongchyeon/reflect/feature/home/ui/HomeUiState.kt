package com.dongchyeon.reflect.feature.home.ui

import androidx.compose.runtime.Immutable
import kotlin.time.Duration

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val hasUsagePermission: Boolean = false,
    val timeWithoutPhone: Duration = Duration.ZERO,
    val visitCount: Int = 0,
    val intention: String? = null,
    val appSlots: List<AppSlotUi> = emptyList(),
    val errorMessage: String? = null
)

@Immutable
data class AppSlotUi(
    val packageName: String,
    val label: String,
    val alpha: Float
)

@Immutable
sealed interface HomeUiEvent {
    data object ScreenResumed : HomeUiEvent  // Activity.onResume에서 발행
    data object PermissionGuideClicked : HomeUiEvent
    data class AppSlotClicked(val packageName: String) : HomeUiEvent
}

@Immutable
sealed interface HomeSideEffect {
    data object OpenUsageAccessSettings : HomeSideEffect
    data class LaunchApp(val packageName: String) : HomeSideEffect
}
