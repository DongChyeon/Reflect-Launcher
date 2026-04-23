package com.dongchyeon.reflect.core.domain.model

import kotlin.time.Duration

data class HomeData(
    val timeWithoutPhone: Duration,
    val visitCount: Int,
    val intention: String?,
    val appSlots: List<AppSlot>,
    val hasUsagePermission: Boolean
)
