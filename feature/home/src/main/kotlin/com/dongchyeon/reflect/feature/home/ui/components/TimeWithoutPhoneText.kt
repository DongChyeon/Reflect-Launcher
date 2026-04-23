package com.dongchyeon.reflect.feature.home.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration

@Composable
fun TimeWithoutPhoneText(
    duration: Duration,
    modifier: Modifier = Modifier
) {
    val totalMinutes = duration.inWholeMinutes
    val fraction = (totalMinutes / 120f).coerceIn(0f, 1f)
    // Float lerp via inline calculation; TextUnit lerp via androidx.compose.ui.unit.lerp
    val alpha = 0.2f + (1.0f - 0.2f) * fraction
    val textSize = lerp(32.sp, 52.sp, fraction)

    Column(modifier = modifier) {
        Text(
            text = duration.toDisplayString(),
            style = MaterialTheme.typography.displayMedium.copy(fontSize = textSize),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
        )
        Text(
            text = "without phone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha * 0.7f)
        )
    }
}

private fun Duration.toDisplayString(): String {
    val totalMinutes = inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
