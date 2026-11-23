package com.music.echo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BitPerfectBadge(
    sampleRate: Int = 96000,
    bitDepth: Int = 24,
    uacVersion: Int = 2,
    modifier: Modifier = Modifier
) {
    val rateText = when {
        sampleRate >= 1000 -> {
            if (sampleRate % 1000 == 0) {
                "${sampleRate / 1000} kHz"
            } else {
                String.format(java.util.Locale.US, "%.1f kHz", sampleRate / 1000.0)
            }
        }
        else -> "$sampleRate Hz"
    }
    val badgeText = "Bit-Perfect • $bitDepth-bit / $rateText • UAC$uacVersion"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
