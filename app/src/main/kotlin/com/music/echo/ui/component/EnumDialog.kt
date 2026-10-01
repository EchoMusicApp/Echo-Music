package echo.music.iad1tya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun <T> EnumDialog(
  onDismiss: () -> Unit,
  onSelect: (T) -> Unit,
  title: String,
  current: T,
  values: List<T>,
  valueText: @Composable (T) -> String,
  valueDescription: (@Composable (T) -> String)? = null,
) {
  ListDialog(
    onDismiss = onDismiss,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  ) {
    itemsIndexed(values) { index, value ->
      val shape =
        when {
          values.size == 1 -> RoundedCornerShape(24.dp)
          index == 0 ->
            RoundedCornerShape(
              topStart = 24.dp,
              topEnd = 24.dp,
              bottomStart = 4.dp,
              bottomEnd = 4.dp
            )
          index == values.size - 1 ->
            RoundedCornerShape(
              topStart = 4.dp,
              topEnd = 4.dp,
              bottomStart = 24.dp,
              bottomEnd = 24.dp
            )
          else -> RoundedCornerShape(4.dp)
        }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 2.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { onSelect(value) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        RadioButton(
          selected = value == current,
          onClick = null,
        )

        Column(
          modifier = Modifier.padding(start = 16.dp),
        ) {
          Text(
            text = valueText(value),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (valueDescription != null && valueDescription(value).isNotEmpty()) {
            Text(
              text = valueDescription(value),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
