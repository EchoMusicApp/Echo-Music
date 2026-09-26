package echo.music.iad1tya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    color = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  ) {
    itemsIndexed(values) { index, value ->
      val shape = RoundedCornerShape(16.dp)

      Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
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
            )
            if (valueDescription != null && valueDescription(value).isNotEmpty()) {
              Text(
                text = valueDescription(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}
