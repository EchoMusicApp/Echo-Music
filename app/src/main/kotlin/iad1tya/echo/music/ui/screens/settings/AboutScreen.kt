package iad1tya.echo.music.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val isDarkTheme = MaterialTheme.colorScheme.surface == MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f) || 
                      MaterialTheme.colorScheme.surface.red < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        Spacer(Modifier.height(24.dp))

        // App Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.echo_logo),
                    contentDescription = null,
                    colorFilter = if (!isDarkTheme) ColorFilter.tint(Color.Black) else null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "ECHO",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                val annotatedText = buildAnnotatedString {
                    append("An open-source music streaming app developed by ")
                    
                    withLink(
                        LinkAnnotation.Url(
                            url = "https://iad1tya.cyou",
                            styles = androidx.compose.ui.text.TextLinkStyles(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        )
                    ) {
                        append("Aditya")
                    }
                    
                    append(". Delivering seamless, ad-free music streaming experience.")
                }
                
                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Community Section
        Material3SettingsGroup(
            title = "Community",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text("Website") },
                    description = { Text("echomusic.fun") },
                    onClick = { uriHandler.openUri("https://echomusic.fun") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.github),
                    title = { Text("GitHub") },
                    description = { Text("iad1tya/Echo-Music") },
                    onClick = { uriHandler.openUri("https://github.com/iad1tya/Echo-Music") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.discord),
                    title = { Text("Discord") },
                    description = { Text("Join our community") },
                    onClick = { uriHandler.openUri("https://discord.com/invite/eNFNHaWN97") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.telegram),
                    title = { Text("Telegram") },
                    description = { Text("Follow us on Telegram") },
                    onClick = { uriHandler.openUri("https://t.me/EchoMusicApp") }
                )
            )
        )

        Spacer(Modifier.height(16.dp))

        // Support Section
        Material3SettingsGroup(
            title = "Support Development",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.favorite),
                    title = { Text("Buy Me a Coffee") },
                    description = { Text("Support the developer") },
                    onClick = { uriHandler.openUri("https://www.buymeacoffee.com/iad1tya") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.upi),
                    title = { Text("UPI Payment") },
                    description = { Text("Support via UPI (India)") },
                    onClick = { uriHandler.openUri("https://intradeus.github.io/http-protocol-redirector/?r=upi://pay?pa=8840590272@kotak&pn=Aditya%20Yadav&am=&tn=Thank%20You%20so%20much%20for%20this%20support") }
                )
            )
        )

        Spacer(Modifier.height(16.dp))

        // Contact & Legal Section
        Material3SettingsGroup(
            title = "Contact & Legal",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.mail_filled),
                    title = { Text("Contact") },
                    description = { Text("hello@echomusic.fun") },
                    onClick = { uriHandler.openUri("mailto:hello@echomusic.fun") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lock),
                    title = { Text("Privacy Policy") },
                    description = { Text("How we handle your data") },
                    onClick = { uriHandler.openUri("https://echomusic.fun/p/privacy-policy") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text("Terms & Conditions") },
                    description = { Text("Terms of service") },
                    onClick = { uriHandler.openUri("https://echomusic.fun/p/toc") }
                )
            )
        )

        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { 
            Text(
                text = stringResource(R.string.about),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                    fontWeight = FontWeight.Bold
                )
            )
        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
