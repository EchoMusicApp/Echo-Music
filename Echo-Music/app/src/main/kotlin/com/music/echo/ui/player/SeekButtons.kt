package echo.music.iad1tya.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R

@Composable
fun SeekButtons(enabled: Boolean, color: Color, onSeek: (Long) -> Unit) {
  val backwardLabel = stringResource(R.string.seek_back_ten_seconds)
  val forwardLabel = stringResource(R.string.seek_forward_ten_seconds)
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
    TextButton(
      onClick = { onSeek(-10_000L) },
      enabled = enabled,
      colors = ButtonDefaults.textButtonColors(contentColor = color),
      modifier = Modifier.heightIn(min = 48.dp),
    ) {
      Text(
        text = stringResource(R.string.seek_back_ten_short),
        modifier = Modifier.clearAndSetSemantics { contentDescription = backwardLabel },
      )
    }
    TextButton(
      onClick = { onSeek(10_000L) },
      enabled = enabled,
      colors = ButtonDefaults.textButtonColors(contentColor = color),
      modifier = Modifier.heightIn(min = 48.dp),
    ) {
      Text(
        text = stringResource(R.string.seek_forward_ten_short),
        modifier = Modifier.clearAndSetSemantics { contentDescription = forwardLabel },
      )
    }
  }
}
