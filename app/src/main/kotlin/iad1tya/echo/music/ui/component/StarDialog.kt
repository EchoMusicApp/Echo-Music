

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package iad1tya.echo.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarDialog(
    onDismissRequest: () -> Unit,
    onStar: () -> Unit,
    onLater: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Support development", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Hey there! I\'m iad1tya, the developer of Echo Music. I have been putting a lot of love into making this app better every day.\n\nIf you enjoy using the app, please support its development through the links below!",
                    style = MaterialTheme.typography.bodyMedium,
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                CategorySection(title = "Follow developer") {
                    SocialChip(icon = R.drawable.ic_instagram_new, label = "Instagram") {
                        uriHandler.openUri("https://instagram.com/iad1tya")
                    }
                    SocialChip(icon = R.drawable.ic_x_new, label = "X (Twitter)") {
                        uriHandler.openUri("https://x.com/xad1tya")
                    }
                    SocialChip(icon = R.drawable.github, label = "GitHub") {
                        uriHandler.openUri("https://github.com/iad1tya")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CategorySection(title = "Support") {
                    SocialChip(icon = R.drawable.coffee, label = "Buy Me Coffee") {
                        uriHandler.openUri("https://buymeacoffee.com/iad1tya")
                    }
                    SocialChip(icon = R.drawable.ic_patreon_new, label = "Patreon") {
                        uriHandler.openUri("https://www.patreon.com/cw/iad1tya")
                    }
                    SocialChip(icon = R.drawable.upi_new, label = "UPI") {
                        uriHandler.openUri("https://intradeus.github.io/http-protocol-redirector/?r=upi://pay?pa=iad1tya@upi&pn=Aditya%20Yadav&am=&tn=Thank%20You%20so%20much%20for%20this%20support")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CategorySection(title = "Star the repo") {
                    SocialChip(icon = R.drawable.github, label = "GitHub Echo Music") {
                        uriHandler.openUri("https://echomusic.fun")
                        onStar()
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CategorySection(title = "Find us on") {
                    SocialChip(icon = R.drawable.ic_discord_new, label = "Discord") {
                        uriHandler.openUri("https://discord.gg/EcfV3AxH5c")
                    }
                    SocialChip(icon = R.drawable.ic_telegram_new, label = "Telegram") {
                        uriHandler.openUri("https://t.me/EchoMusicApp")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onLater, shapes = ButtonDefaults.shapes()) {
                Text(text = "Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun SocialChip(icon: Int, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}
