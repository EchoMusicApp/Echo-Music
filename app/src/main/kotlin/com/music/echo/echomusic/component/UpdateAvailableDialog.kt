package echo.music.iad1tya.echomusic.component

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R
import echo.music.iad1tya.echomusic.updater.ChangelogSection
import echo.music.iad1tya.ui.utils.parseMarkdownToSections
import echo.music.iad1tya.ui.utils.parseSimpleMarkdown
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableDialog(
    version: String,
    changelog: List<ChangelogSection>,
    description: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cardShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 30.dp,
        cornerRadiusTR = 30.dp,
        cornerRadiusBL = 30.dp,
        cornerRadiusBR = 30.dp,
        smoothnessAsPercentTL = 60,
        smoothnessAsPercentTR = 60,
        smoothnessAsPercentBL = 60,
        smoothnessAsPercentBR = 60,
    )
    val blockShape = AbsoluteSmoothCornerShape(22.dp, 60)
    val actionShape = AbsoluteSmoothCornerShape(18.dp, 60)

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = cardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = blockShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = AbsoluteSmoothCornerShape(12.dp, 60),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.update),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.update_available_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Version $version is available",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (changelog.isNotEmpty() || !description.isNullOrEmpty()) {
                    Surface(
                        modifier = Modifier.weight(1f, fill = false),
                        shape = blockShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.changelog),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val (effectiveDescription, effectiveSections) = remember(changelog, description) {
                                if (changelog.isNotEmpty()) {
                                    Pair(description?.takeIf { it.isNotBlank() }, changelog)
                                } else if (!description.isNullOrBlank()) {
                                    parseMarkdownToSections(description)
                                } else {
                                    Pair(null, emptyList())
                                }
                            }

                            if (!effectiveDescription.isNullOrBlank()) {
                                Text(
                                    text = parseSimpleMarkdown(effectiveDescription, MaterialTheme.colorScheme.primary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            if (effectiveSections.isNotEmpty()) {
                                effectiveSections.forEach { section ->
                                    if (section.title.isNotBlank()) {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                    section.items.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = parseSimpleMarkdown(item.trim(), MaterialTheme.colorScheme.primary),
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            } else if (!description.isNullOrEmpty()) {
                                Text(
                                    text = parseSimpleMarkdown(description, MaterialTheme.colorScheme.primary),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = actionShape,
                    ) {
                        Text(text = "Next time")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://echomusic.fun"))
                            context.startActivity(intent)
                        },
                        shape = actionShape,
                    ) {
                        Text(text = "Update")
                    }
                }
            }
        }
    }
}
