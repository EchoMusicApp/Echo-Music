package iad1tya.echo.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.core.content.ContextCompat
import iad1tya.echo.music.echomusic.updater.extractUrls
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
@Composable
fun AnimatedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false,
    enabled: Boolean = true,
    buttonHeight: androidx.compose.ui.unit.Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = tween(durationMillis = 200),
        label = "btnMorph"
    )

    if (isOutlined) {
        androidx.compose.material3.OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(buttonHeight),
            shape = RoundedCornerShape(cornerPercent),
            enabled = enabled,
            interactionSource = interactionSource,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(buttonHeight),
            shape = RoundedCornerShape(cornerPercent),
            enabled = enabled,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 20 else 50,
        animationSpec = tween(durationMillis = 200),
        label = "corner"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(cornerPercent),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ErrorSnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
            actionContentColor = MaterialTheme.colorScheme.inversePrimary,
            dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}


private const val ConnectedCornerRadius = 4
private const val EndCornerRadius = 16

fun leadingItemShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = EndCornerRadius.dp,
    topEnd = EndCornerRadius.dp,
    bottomStart = ConnectedCornerRadius.dp,
    bottomEnd = ConnectedCornerRadius.dp
)

fun middleItemShape(): RoundedCornerShape = RoundedCornerShape(ConnectedCornerRadius.dp)

fun endItemShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = ConnectedCornerRadius.dp,
    topEnd = ConnectedCornerRadius.dp,
    bottomStart = EndCornerRadius.dp,
    bottomEnd = EndCornerRadius.dp
)

fun detachedItemShape(): RoundedCornerShape = RoundedCornerShape(EndCornerRadius.dp)

@Composable
fun String.parseMarkdown(): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    var currentIndex = 0
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    val pattern = Regex("(\\*\\*(.*?)\\*\\*)|(\\*([^*]+)\\*)|(`([^`]+)`)|(\\[([^\\]]+)\\]\\(([^)]+)\\))|((?:https?://|www\\.)[\\w-]+(?:\\.[\\w-]+)+(?:[/?][\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?)")

    val matches = pattern.findAll(this)
    for (match in matches) {
        if (match.range.first > currentIndex) {
            builder.append(this.substring(currentIndex, match.range.first))
        }
        
        when {
            match.groups[1] != null -> { // **bold**
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groups[2]!!.value)
                }
            }
            match.groups[3] != null -> { // *italic*
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groups[4]!!.value)
                }
            }
            match.groups[5] != null -> { // `code`
                builder.withStyle(SpanStyle(
                    background = codeBgColor,
                    fontFamily = FontFamily.Monospace
                )) {
                    append(match.groups[6]!!.value)
                }
            }
            match.groups[7] != null -> { // [link](url)
                val text = match.groups[8]!!.value
                val url = match.groups[9]!!.value
                val startIndex = builder.length
                builder.withStyle(SpanStyle(
                    color = primaryColor,
                    textDecoration = TextDecoration.Underline
                )) {
                    append(text)
                }
                builder.addStringAnnotation("URL", url, startIndex, builder.length)
            }
            match.groups[10] != null -> { // bare url
                val url = match.groups[10]!!.value
                val startIndex = builder.length
                val fullUrl = if (url.startsWith("http")) url else "https://$url"
                builder.withStyle(SpanStyle(
                    color = primaryColor,
                    textDecoration = TextDecoration.Underline
                )) {
                    append(url)
                }
                builder.addStringAnnotation("URL", fullUrl, startIndex, builder.length)
            }
        }
        currentIndex = match.range.last + 1
    }
    
    if (currentIndex < this.length) {
        builder.append(this.substring(currentIndex))
    }
    
    return builder.toAnnotatedString()
}

@Composable
fun ChangelogItem(
    text: String,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        val context = LocalContext.current
        val annotatedText = text.parseMarkdown()

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
            ClickableText(
                text = annotatedText,
                onClick = { offset ->
                    annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                        ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}
