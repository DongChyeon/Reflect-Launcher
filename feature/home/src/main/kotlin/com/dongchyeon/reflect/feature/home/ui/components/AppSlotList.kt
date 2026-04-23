package com.dongchyeon.reflect.feature.home.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dongchyeon.reflect.feature.home.ui.AppSlotUi
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AppSlotList(
    slots: ImmutableList<AppSlotUi>,
    onSlotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        slots.forEach { slot ->
            Text(
                text = slot.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = slot.alpha),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSlotClick(slot.packageName) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}
