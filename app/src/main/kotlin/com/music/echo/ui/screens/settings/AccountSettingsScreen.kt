

package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonGroupDefaults
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.utils.parseCookieString
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.constants.*
import echo.music.iad1tya.ui.component.*
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.utils.rememberPreference

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import echo.music.iad1tya.models.AccountData
import echo.music.iad1tya.constants.SavedAccountsKey
import android.content.Intent

import echo.music.iad1tya.viewmodels.AccountSettingsViewModel
import echo.music.iad1tya.viewmodels.HomeViewModel
import echo.music.iad1tya.R
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior, highlightKey: String? = null) {
    val scrollState = androidx.compose.foundation.rememberScrollState()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val (accountNamePref, _) = rememberPreference(AccountNameKey, "")
    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, _) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, _) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, _) = rememberPreference(DataSyncIdKey, "")
    val (savedAccountsJson, onSavedAccountsJsonChange) = rememberPreference(SavedAccountsKey, "[]")

    val (listenBrainzEnabled, onListenBrainzEnabledChange) = rememberPreference(ListenBrainzEnabledKey, false)
    val (listenBrainzToken, onListenBrainzTokenChange) = rememberPreference(ListenBrainzTokenKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showListenBrainzTokenEditor by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
                )
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {

        Spacer(modifier = Modifier.height(16.dp))

        val savedAccounts = try { Json.decodeFromString<List<AccountData>>(savedAccountsJson) } catch (e: Exception) { emptyList() }
        
        LaunchedEffect(isLoggedIn, innerTubeCookie, accountImageUrl, accountName) {
            val nameToUse = if (accountNamePref.isNotBlank()) accountNamePref else accountName
            if (isLoggedIn && nameToUse.isNotBlank() && nameToUse != "Guest") {
                val mutableAccounts = savedAccounts.toMutableList()
                val index = mutableAccounts.indexOfFirst { it.cookie == innerTubeCookie }
                if (index == -1) {
                    mutableAccounts.add(AccountData(
                        name = nameToUse,
                        email = accountEmail,
                        channelHandle = accountChannelHandle,
                        cookie = innerTubeCookie,
                        visitorData = visitorData,
                        dataSyncId = dataSyncId,
                        avatarUrl = accountImageUrl ?: ""
                    ))
                    onSavedAccountsJsonChange(Json.encodeToString(mutableAccounts))
                } else if (mutableAccounts[index].name.isBlank() || mutableAccounts[index].avatarUrl.isBlank()) {
                    mutableAccounts[index] = mutableAccounts[index].copy(
                        name = nameToUse,
                        avatarUrl = accountImageUrl ?: mutableAccounts[index].avatarUrl
                    )
                    onSavedAccountsJsonChange(Json.encodeToString(mutableAccounts))
                }
            }
        }

        if (savedAccounts.isNotEmpty()) {
            Material3SettingsGroup(
                scrollState = scrollState,
                title = "Switch Accounts",
                items = savedAccounts.map { account ->
                    Material3SettingsItem(
                        icon = if (account.avatarUrl.isNotBlank()) null else painterResource(R.drawable.login),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (account.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = account.avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column {
                                    Text(
                                        text = account.name,
                                        color = if (account.cookie == innerTubeCookie) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (account.email.isNotBlank()) {
                                        Text(
                                            text = account.email,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            if (account.cookie != innerTubeCookie) {
                                IconButton(onClick = {
                                    val mutableAccounts = savedAccounts.toMutableList()
                                    mutableAccounts.remove(account)
                                    onSavedAccountsJsonChange(Json.encodeToString(mutableAccounts))
                                }) {
                                    Icon(painterResource(R.drawable.close), contentDescription = "Remove Account")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { showLogoutDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(stringResource(R.string.action_logout))
                                }
                            }
                        },
                        onClick = {
                            if (account.cookie != innerTubeCookie) {
                                accountSettingsViewModel.saveTokenAndRestart(
                                    context = context,
                                    cookie = account.cookie,
                                    visitorData = account.visitorData,
                                    dataSyncId = account.dataSyncId,
                                    accountName = account.name,
                                    accountEmail = account.email,
                                    accountChannelHandle = account.channelHandle
                                )
                            }
                        }
                    )
                } + listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.add),
                        title = { Text("Add another account") },
                        onClick = { navController.navigate("login") }
                    )
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
             Material3SettingsGroup(
                scrollState = scrollState,
                title = "Accounts",
                items = listOf(
                    Material3SettingsItem(
                        icon = if (isLoggedIn && !accountImageUrl.isNullOrBlank()) null else painterResource(R.drawable.login),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLoggedIn && !accountImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = accountImageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = if (isLoggedIn) accountName else "Log In",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        },
                        trailingContent = if (isLoggedIn) ({
                                OutlinedButton(
                                    onClick = { showLogoutDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(stringResource(R.string.action_logout))
                                }
                        }) else null,
                        onClick = { if (!isLoggedIn) navController.navigate("login") }
                    )
                ) + if (isLoggedIn) {
                    listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.add),
                            title = { Text("Add another account") },
                            onClick = { navController.navigate("login") }
                        )
                    )
                } else emptyList()
             )
             Spacer(modifier = Modifier.height(16.dp))
        }


        Material3SettingsGroup(scrollState = scrollState, 
                title = stringResource(R.string.advanced_login),
                items = listOf(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.advanced_login) || highlightKey == stringResource(R.string.token_shown) || highlightKey == stringResource(R.string.token_hidden)),
                        icon = painterResource(R.drawable.token),
                        title = {
                            Text(
                                when {
                                    !isLoggedIn -> stringResource(R.string.advanced_login)
                                    showToken -> stringResource(R.string.token_shown)
                                    else -> stringResource(R.string.token_hidden)
                                }
                            )
                        },
                        onClick = {
                            if (!isLoggedIn) showTokenEditor = true
                            else if (!showToken) showToken = true
                            else showTokenEditor = true
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            
            if (isLoggedIn) {

        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(scrollState = scrollState, 
                    title = stringResource(R.string.settings_section_player_content),
                    items = listOf(
                        Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.more_content)),
                            icon = painterResource(R.drawable.add_circle),
                            title = { Text(stringResource(R.string.more_content)) },
                            description = { Text(stringResource(R.string.more_content_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = useLoginForBrowse,
                                    onCheckedChange = {
                                        YouTube.useLoginForBrowse = it
                                        onUseLoginForBrowseChange(it)
                                    },
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (useLoginForBrowse) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    }
                                )
                            },
                            onClick = {
                                val newValue = !useLoginForBrowse
                                YouTube.useLoginForBrowse = newValue
                                onUseLoginForBrowseChange(newValue)
                            }
                        ),
                        Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.yt_sync)),
                            icon = painterResource(R.drawable.cached),
                            title = { Text(stringResource(R.string.yt_sync)) },
                            description = { Text(stringResource(R.string.yt_sync_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = ytmSync,
                                    onCheckedChange = onYtmSyncChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (ytmSync) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    }
                                )
                            },
                            onClick = { onYtmSyncChange(!ytmSync) }
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Material3SettingsGroup(
                scrollState = scrollState,
                title = stringResource(R.string.integrations),
                items = listOf(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == stringResource(R.string.discord)),
                        icon = painterResource(R.drawable.discord),
                        title = { Text(stringResource(R.string.discord)) },
                        description = { Text(stringResource(R.string.discord_integration_desc)) },
                        onClick = {
                            navController.navigate("settings/discord")
                        }
                    ),
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == stringResource(R.string.lastfm_integration)),
                        icon = painterResource(R.drawable.ic_lastfm),
                        title = { Text(stringResource(R.string.lastfm_integration)) },
                        description = { Text(stringResource(R.string.lastfm_integration_desc)) },
                        onClick = {
                            navController.navigate("settings/lastfm")
                        }
                    )
                )
            )
        }

        if (showTokenEditor) {
            val text = """
                ***INNERTUBE COOKIE*** =$innerTubeCookie
                ***VISITOR DATA*** =$visitorData
                ***DATASYNC ID*** =$dataSyncId
                ***ACCOUNT NAME*** =$accountNamePref
                ***ACCOUNT EMAIL*** =$accountEmail
                ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
            """.trimIndent()

            TextFieldDialog(
                initialTextFieldValue = TextFieldValue(text),
                onDone = { data ->
                    var cookie = ""
                    var visitorDataValue = ""
                    var dataSyncIdValue = ""
                    var accountNameValue = ""
                    var accountEmailValue = ""
                    var accountChannelHandleValue = ""

                    data.split("\n").forEach {
                        when {
                            it.startsWith("***INNERTUBE COOKIE*** =") -> cookie = it.substringAfter("=")
                            it.startsWith("***VISITOR DATA*** =") -> visitorDataValue = it.substringAfter("=")
                            it.startsWith("***DATASYNC ID*** =") -> dataSyncIdValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT NAME*** =") -> accountNameValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT EMAIL*** =") -> accountEmailValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> accountChannelHandleValue = it.substringAfter("=")
                        }
                    }
                    accountSettingsViewModel.saveTokenAndRestart(
                        context = context,
                        cookie = cookie,
                        visitorData = visitorDataValue,
                        dataSyncId = dataSyncIdValue,
                        accountName = accountNameValue,
                        accountEmail = accountEmailValue,
                        accountChannelHandle = accountChannelHandleValue,
                    )
                },
                onDismiss = { showTokenEditor = false },
                singleLine = false,
                maxLines = 8,
                isInputValid = { fullText ->
                    val cookieLine = fullText.lines()
                        .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                    val cookieValue = cookieLine?.substringAfter("***INNERTUBE COOKIE*** =")?.trim() ?: ""
                    cookieValue.isNotEmpty() && "SAPISID" in parseCookieString(cookieValue)
                },
                extraContent = {
                    InfoLabel(text = stringResource(R.string.token_adv_login_description))
                }
            )
        }
        if (showLogoutDialog) {
            DefaultDialog(
                onDismiss = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.logout_dialog_title)) },
                buttons = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                accountSettingsViewModel.logoutKeepData(context, onInnerTubeCookieChange)
                                showLogoutDialog = false
                                navController.navigateUp()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.logout_keep_data))
                        }
                        
                        FilledTonalButton(
                            onClick = {
                                accountSettingsViewModel.logoutAndClearSyncedContent(context, onInnerTubeCookieChange)
                                showLogoutDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.logout_clear_data))
                        }

                        TextButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.logout_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (showListenBrainzTokenEditor) {
            TextFieldDialog(
                initialTextFieldValue = TextFieldValue(listenBrainzToken),
                onDone = { data ->
                    onListenBrainzTokenChange(data)
                    showListenBrainzTokenEditor = false
                },
                onDismiss = { showListenBrainzTokenEditor = false },
                singleLine = true,
                maxLines = 1,
                isInputValid = {
                    it.isNotEmpty()
                },
                extraContent = {
                    InfoLabel(text = stringResource(R.string.listenbrainz_scrobbling_description))
                }
            )
        }
        
        Spacer(Modifier.height(100.dp))
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)))
    }
}
