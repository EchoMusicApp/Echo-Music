package com.music.echo.ui.screens.wrapped

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo.music.iad1tya.R

@Composable
fun WrappedBannerCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "WrappedBannerPulse")
  val borderAlpha by infiniteTransition.animateFloat(
    initialValue = 0.15f,
    targetValue = 0.40f,
    animationSpec = infiniteRepeatable(
      animation = tween(2400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bannerBorderAlpha"
  )

  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(12000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "vinylSpin"
  )

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = Color(0xFF111422),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(26.dp))
        .border(
          width = 1.dp,
          brush = Brush.horizontalGradient(
            listOf(
              Color(0xFF8B5CF6).copy(alpha = borderAlpha),
              Color(0xFFEC4899).copy(alpha = borderAlpha * 0.7f),
              Color.White.copy(alpha = 0.1f)
            )
          ),
          shape = RoundedCornerShape(26.dp)
        )
        .clickable(onClick = onClick),
    shadowElevation = 8.dp
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.radialGradient(
              colors = listOf(
                Color(0xFF281C48).copy(alpha = 0.75f),
                Color(0xFF141726).copy(alpha = 0.95f),
                Color(0xFF0C0E17)
              ),
              radius = 800f
            )
          )
          .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          // Top pill: Modern micro badge
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier =
              Modifier.clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Box(
              modifier =
                Modifier.size(6.dp)
                  .background(Color(0xFF34D399), CircleShape)
            )
            Text(
              "ECHO WRAPPED",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
              ),
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            "Your Year in Music",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.5).sp,
              fontSize = 22.sp
            ),
            color = Color.White
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            "Discover your top songs, artists & musical persona.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
            maxLines = 2
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right side: Spinning Stylized Vinyl with Play Icon
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(64.dp)
        ) {
          // Vinyl disc background
          Box(
            modifier =
              Modifier.size(64.dp)
                .rotate(rotation)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                  Brush.sweepGradient(
                    listOf(
                      Color(0xFF1E2130),
                      Color(0xFF0F1018),
                      Color(0xFF2D1B4E),
                      Color(0xFF1E2130)
                    )
                  )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
          )

          // Center groove ring
          Box(
            modifier =
              Modifier.size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.4f), CircleShape)
          )

          // Center Play Arrow
          Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = "Open Wrapped",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}
