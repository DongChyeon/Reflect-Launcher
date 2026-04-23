package com.dongchyeon.reflect.feature.home.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
