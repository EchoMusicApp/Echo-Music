package iad1tya.echo.music.ui.screen.other

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.extension.adaptiveIconPainterResource
import iad1tya.echo.music.ui.component.RippleIconButton
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.utils.VersionManager
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun CreditScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val context = LocalContext.current
    val hazeState = rememberHazeState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 64.dp)
            .verticalScroll(rememberScrollState())
            .hazeSource(state = hazeState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Simple app icon
        Image(
            painter = adaptiveIconPainterResource(R.mipmap.ic_launcher_round) ?: painterResource(R.drawable.echo_nobg),
            contentDescription = "App Icon",
            modifier = Modifier.size(100.dp),
            colorFilter = null
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App name with version
        Text(
            text = "Echo v${VersionManager.getVersionName()}",
            style = typo.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Attribution
        Text(
            text = "Developed by iad1tya",
            style = typo.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Simple description
        Text(
            text = stringResource(R.string.credit_app),
            style = typo.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Simple resource links arranged horizontally
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SimpleLink(
                    text = stringResource(R.string.website),
                    onClick = {
                        val urlIntent = Intent(Intent.ACTION_VIEW, "https://echomusic.fun".toUri())
                        context.startActivity(urlIntent)
                    }
                )
                SimpleLink(
                    text = stringResource(R.string.github),
                    onClick = {
                        val urlIntent = Intent(Intent.ACTION_VIEW, "https://github.com/iad1tya/Echo-Music".toUri())
                        context.startActivity(urlIntent)
                    }
                )
                SimpleLink(
                    text = "Discord",
                    onClick = {
                        val urlIntent = Intent(Intent.ACTION_VIEW, "https://discord.com/invite/eNFNHaWN97".toUri())
                        context.startActivity(urlIntent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
    
    TopAppBar(
        modifier = Modifier
            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            },
        title = {
            Text(
                text = "About Echo",
                style = typo.titleMedium,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = MarqueeAnimationMode.Immediately,
                    )
                    .focusable(),
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    R.drawable.baseline_arrow_back_ios_new_24,
                    Modifier.size(32.dp),
                    true,
                ) {
                    navController.navigateUp()
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(Color.Transparent),
    )
}

@Composable
private fun SimpleLink(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 1.dp, minWidth = 1.dp)
    ) {
        Text(
            text = text,
            style = typo.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
