package iad1tya.echo.music.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.InnerTubeCookieKey
import iad1tya.echo.music.constants.PoTokenGvsKey
import iad1tya.echo.music.constants.PoTokenPlayerKey
import iad1tya.echo.music.constants.PoTokenSourceUrlKey
import iad1tya.echo.music.constants.UseVisitorDataKey
import iad1tya.echo.music.constants.VisitorDataKey
import iad1tya.echo.music.constants.WebClientPoTokenEnabledKey
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.EditTextPreference
import iad1tya.echo.music.ui.component.PreferenceEntry
import iad1tya.echo.music.ui.component.PreferenceGroupTitle
import iad1tya.echo.music.ui.component.SwitchPreference
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.rememberPreference

private const val DEFAULT_EXTRACT_URL = "https://youtube.com/account"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoTokenScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val (webClientPoTokenEnabled, onWebClientPoTokenEnabledChange) = rememberPreference(
        WebClientPoTokenEnabledKey,
        false,
    )
    val (useVisitorData, onUseVisitorDataChange) = rememberPreference(UseVisitorDataKey, false)
    val (sourceUrl, onSourceUrlChange) = rememberPreference(PoTokenSourceUrlKey, "")
    val (storedGvsToken, onStoredGvsTokenChange) = rememberPreference(PoTokenGvsKey, "")
    val (storedPlayerToken, onStoredPlayerTokenChange) = rememberPreference(PoTokenPlayerKey, "")
    val (storedVisitorData, onStoredVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")

    val extractionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val gvsToken = data?.getStringExtra(PoTokenExtractionActivity.EXTRA_GVS_TOKEN).orEmpty()
            val playerToken = data?.getStringExtra(PoTokenExtractionActivity.EXTRA_PLAYER_TOKEN).orEmpty()
            val visitorData = data?.getStringExtra(PoTokenExtractionActivity.EXTRA_VISITOR_DATA).orEmpty()

            if (gvsToken.isNotBlank() && playerToken.isNotBlank() && visitorData.isNotBlank()) {
                onStoredGvsTokenChange(gvsToken)
                onStoredPlayerTokenChange(playerToken)
                onStoredVisitorDataChange(visitorData)
                Toast.makeText(context, R.string.tokens_generated, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.token_generation_failed, Toast.LENGTH_LONG).show()
            }
        } else {
            val error = result.data?.getStringExtra(PoTokenExtractionActivity.EXTRA_ERROR).orEmpty()
            if (error.isNotBlank()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.web_client_po_token)) },
            description = stringResource(R.string.web_client_po_token_desc),
            icon = { Icon(painterResource(R.drawable.token), null) },
            checked = webClientPoTokenEnabled,
            onCheckedChange = onWebClientPoTokenEnabledChange,
        )

        if (webClientPoTokenEnabled) {
            PreferenceGroupTitle(title = stringResource(R.string.token_settings))

            SwitchPreference(
                title = { Text(stringResource(R.string.use_visitor_data)) },
                description = stringResource(R.string.use_visitor_data_desc),
                icon = { Icon(painterResource(R.drawable.person), null) },
                checked = useVisitorData,
                onCheckedChange = { enabled ->
                    if (enabled && innerTubeCookie.isNotBlank()) {
                        Toast.makeText(context, R.string.cookies_must_be_disabled, Toast.LENGTH_LONG).show()
                    } else {
                        onUseVisitorDataChange(enabled)
                    }
                },
            )

            EditTextPreference(
                title = { Text(stringResource(R.string.source_url)) },
                icon = { Icon(painterResource(R.drawable.link), null) },
                value = sourceUrl,
                onValueChange = onSourceUrlChange,
            )

            PreferenceEntry(
                title = { Text(stringResource(R.string.regenerate_token)) },
                description = stringResource(R.string.regenerate),
                icon = { Icon(painterResource(R.drawable.sync), null) },
                onClick = {
                    val launchUrl = sourceUrl.takeIf { it.isNotBlank() } ?: DEFAULT_EXTRACT_URL
                    val intent = Intent(context, PoTokenExtractionActivity::class.java).apply {
                        putExtra(PoTokenExtractionActivity.EXTRA_SOURCE_URL, launchUrl)
                    }
                    extractionLauncher.launch(intent)
                }
            )

            PreferenceGroupTitle(title = stringResource(R.string.generated_tokens))

            TokenEntry(
                title = stringResource(R.string.po_token_gvs),
                value = storedGvsToken,
                onCopy = {
                    if (storedGvsToken.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(storedGvsToken))
                        Toast.makeText(context, R.string.token_copied, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            TokenEntry(
                title = stringResource(R.string.po_token_player),
                value = storedPlayerToken,
                onCopy = {
                    if (storedPlayerToken.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(storedPlayerToken))
                        Toast.makeText(context, R.string.token_copied, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            TokenEntry(
                title = stringResource(R.string.visitor_data),
                value = storedVisitorData,
                onCopy = {
                    if (storedVisitorData.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(storedVisitorData))
                        Toast.makeText(context, R.string.token_copied, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.po_token_generation)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        }
    )
}

@Composable
private fun TokenEntry(
    title: String,
    value: String,
    onCopy: () -> Unit,
) {
    PreferenceEntry(
        title = { Text(title) },
        description = value.ifBlank { "-" },
        icon = { Icon(painterResource(R.drawable.content_copy), null) },
        onClick = onCopy,
        content = {
            Text(
                text = value.takeIf { it.isNotBlank() } ?: "-",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    )
}
