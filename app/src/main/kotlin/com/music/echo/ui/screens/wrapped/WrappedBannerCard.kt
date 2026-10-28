package com.music.echo.ui.screens.wrapped

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo.music.iad1tya.R

/**
 * Clean, minimal, Pixel UI / Material You inspired banner card for Echo Wrapped.
 * Adapts to Monet dynamic color and provides a dismissible cross button.
 */
@Composable
fun WrappedBannerCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onDismiss: (() -> Unit)? = null
) {
  var isVisible by remember { mutableStateOf(true) }

  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically()
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 2.dp,
      border = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
      ),
      modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .clickable(onClick = onClick)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Pixel-style Tonal Squircle Leading Icon
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_echo_wrapped),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Text Content
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = "2026 REPLAY",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.primary
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "Your Year in Music",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold,
              letterSpacing = (-0.2).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "Tap to explore your top songs, artists & stats",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Right side: Close / Dismiss button (Cross) or Arrow
        if (onDismiss != null) {
          IconButton(
            onClick = {
              isVisible = false
              onDismiss()
            },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              painter = painterResource(R.drawable.close),
              contentDescription = "Dismiss",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
              modifier = Modifier.size(18.dp)
            )
          }
        } else {
          Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .padding(end = 8.dp)
              .size(18.dp)
          )
        }
      }
    }
  }
}
