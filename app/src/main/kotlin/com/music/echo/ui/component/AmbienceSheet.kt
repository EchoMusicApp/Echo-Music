@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo.music.iad1tya.ambience.AmbienceEngine
import echo.music.iad1tya.ambience.AmbienceSound

private val Mint = Color(0xFF7CF0A8)

/** Liquid-glass AMBIENCE pill shown on the Now Playing screen. */
@Composable
fun AmbienceButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val levels by AmbienceEngine.levels.collectAsState()
  val active = levels.values.any { it > 0f }
  val shape = RoundedCornerShape(50)
  Row(
    modifier =
      modifier
        .clip(shape)
        .background(
          Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (active) 0.30f else 0.22f), Color.White.copy(alpha = 0.06f))
          )
        )
        .border(
          BorderStroke(
            1.dp,
            Brush.verticalGradient(
              listOf(
                (if (active) Mint else Color.White).copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.08f)
              )
            )
          ),
          shape
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    Text(if (active) "🌿" else "🍃", fontSize = 11.sp)
    Text(
      "AMBIENCE",
      color = Color.White.copy(alpha = 0.92f),
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.6.sp,
      maxLines = 1
    )
  }
}

@Composable
fun AmbienceSheet(onDismiss: () -> Unit) {
  val levels by AmbienceEngine.levels.collectAsState()
  val master by AmbienceEngine.master.collectAsState()
  val click by AmbienceEngine.clickOnTransition.collectAsState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = Color(0xFF07160E).copy(alpha = 0.96f),
    contentColor = Color.White
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text("AMBIENCE", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 3.sp)
          Text(
            "Layer nature sounds under your music",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
          )
        }
        TextButton(onClick = { AmbienceEngine.stopAll() }) { Text("Stop all", color = Mint) }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Volume", modifier = Modifier.width(64.dp), color = Color.White.copy(alpha = 0.8f))
        Slider(
          value = master,
          onValueChange = AmbienceEngine::setMaster,
          colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = Color.White.copy(alpha = 0.18f))
        )
      }

      AmbienceSound.entries.forEach { sound ->
        val level = levels[sound] ?: 0f
        val on = level > 0f
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .background(
                Brush.verticalGradient(
                  listOf(Color.White.copy(alpha = if (on) 0.14f else 0.08f), Color.White.copy(alpha = 0.03f))
                )
              )
              .border(1.dp, (if (on) Mint else Color.White).copy(alpha = if (on) 0.45f else 0.10f), RoundedCornerShape(20.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            sound.emoji,
            fontSize = 22.sp,
            modifier = Modifier.clip(CircleShape).clickable { AmbienceEngine.toggle(sound) }.padding(8.dp)
          )
          Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(sound.label, fontWeight = FontWeight.SemiBold, color = if (on) Mint else Color.White)
            Slider(
              value = level,
              onValueChange = { AmbienceEngine.setLevel(sound, it) },
              colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = Color.White.copy(alpha = 0.18f)),
              modifier = Modifier.height(28.dp)
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(Modifier.weight(1f)) {
          Text("Transition click", fontWeight = FontWeight.SemiBold)
          Text(
            "A soft tick when the song changes",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
          )
        }
        Switch(
          checked = click,
          onCheckedChange = AmbienceEngine::setClickOnTransition,
          colors = SwitchDefaults.colors(checkedTrackColor = Mint, checkedThumbColor = Color(0xFF04110A))
        )
      }
      Spacer(Modifier.height(8.dp))
    }
  }
}
