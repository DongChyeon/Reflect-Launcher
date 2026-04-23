package com.dongchyeon.reflect.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongchyeon.reflect.feature.home.ui.components.AppSlotList
import com.dongchyeon.reflect.feature.home.ui.components.PermissionGuideOverlay
import com.dongchyeon.reflect.feature.home.ui.components.TimeWithoutPhoneText

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
