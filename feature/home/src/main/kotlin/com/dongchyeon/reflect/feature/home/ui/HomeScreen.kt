package com.dongchyeon.reflect.feature.home.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongchyeon.reflect.feature.home.ui.components.AppSlotList
import com.dongchyeon.reflect.feature.home.ui.components.PermissionGuideOverlay
import com.dongchyeon.reflect.feature.home.ui.components.TimeWithoutPhoneText
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
fun HomeScreen(
    onLaunchApp: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.LaunchApp -> onLaunchApp(effect.packageName)
                HomeSideEffect.OpenUsageAccessSettings -> onOpenSettings()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(HomeUiEvent.ScreenResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp)
        ) {
            Text(
                text = "#${uiState.visitCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.End),
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                TimeWithoutPhoneText(duration = uiState.timeWithoutPhone)
            }

            uiState.intention?.let { intention ->
                Text(
                    text = intention,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            AppSlotList(
                slots = uiState.appSlots,
                onSlotClick = { packageName ->
                    onEvent(HomeUiEvent.AppSlotClicked(packageName))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!uiState.hasUsagePermission) {
            PermissionGuideOverlay(
                onGuideClick = { onEvent(HomeUiEvent.PermissionGuideClicked) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "홈 - 일반 (권한 있음)", showBackground = true)
@Preview(name = "홈 - 다크 (권한 있음)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeContentPreview() {
    MaterialTheme {
        Surface {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    hasUsagePermission = true,
                    timeWithoutPhone = 2.hours + 34.minutes,
                    visitCount = 8,
                    intention = null,
                    appSlots = persistentListOf(
                        AppSlotUi("com.whatsapp", "Messages", alpha = 1.0f),
                        AppSlotUi("com.instagram.android", "Instagram", alpha = 0.3f),
                        AppSlotUi("com.google.android.apps.camera", "Camera", alpha = 0.6f),
                        AppSlotUi("com.google.keep", "Notes", alpha = 0.9f),
                    ),
                ),
                onEvent = {},
            )
        }
    }
}

@Preview(name = "홈 - 의도 문장 있음", showBackground = true)
@Composable
private fun HomeContentWithIntentionPreview() {
    MaterialTheme {
        Surface {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    hasUsagePermission = true,
                    timeWithoutPhone = 45.minutes,
                    visitCount = 3,
                    intention = "오늘 가장 중요한 한 가지는 집중",
                    appSlots = persistentListOf(
                        AppSlotUi("com.whatsapp", "Messages", alpha = 0.8f),
                        AppSlotUi("com.google.keep", "Notes", alpha = 1.0f),
                    ),
                ),
                onEvent = {},
            )
        }
    }
}

@Preview(name = "홈 - 권한 없음 (오버레이)", showBackground = true)
@Composable
private fun HomeContentNoPermissionPreview() {
    MaterialTheme {
        Surface {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    hasUsagePermission = false,
                    timeWithoutPhone = 1.hours + 10.minutes,
                    visitCount = 2,
                    intention = null,
                    appSlots = persistentListOf(
                        AppSlotUi("com.whatsapp", "Messages", alpha = 0.4f),
                        AppSlotUi("com.instagram.android", "Instagram", alpha = 0.4f),
                        AppSlotUi("com.google.keep", "Notes", alpha = 0.4f),
                    ),
                ),
                onEvent = {},
            )
        }
    }
}

@Preview(name = "홈 - 로딩 중", showBackground = true)
@Composable
private fun HomeContentLoadingPreview() {
    MaterialTheme {
        Surface {
            HomeContent(
                uiState = HomeUiState(isLoading = true),
                onEvent = {},
            )
        }
    }
}

@Preview(name = "홈 - 폰 오래 안 씀 (2시간+)", showBackground = true)
@Preview(name = "홈 - 폰 막 켰을 때 (0분)", showBackground = true)
@Composable
private fun HomeContentTimeVariantPreview() {
    MaterialTheme {
        Surface {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    hasUsagePermission = true,
                    timeWithoutPhone = 0.minutes,
                    visitCount = 1,
                    appSlots = persistentListOf(
                        AppSlotUi("com.whatsapp", "Messages", alpha = 1.0f),
                    ),
                ),
                onEvent = {},
            )
        }
    }
}
