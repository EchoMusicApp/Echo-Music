


package iad1tya.echo.music.utils

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import iad1tya.echo.music.constants.AccountChannelHandleKey
import iad1tya.echo.music.constants.AccountEmailKey
import iad1tya.echo.music.constants.AccountNameKey
import iad1tya.echo.music.constants.DataSyncIdKey
import iad1tya.echo.music.constants.InnerTubeCookieKey
import iad1tya.echo.music.constants.PoTokenGvsKey
import iad1tya.echo.music.constants.PoTokenKey
import iad1tya.echo.music.constants.PoTokenPlayerKey
import iad1tya.echo.music.constants.PoTokenSourceUrlKey
import iad1tya.echo.music.constants.VisitorDataKey
import iad1tya.echo.music.constants.WebClientPoTokenEnabledKey
import iad1tya.echo.music.innertube.PlaybackAuthState
import iad1tya.echo.music.innertube.YouTube
import kotlinx.coroutines.flow.first

fun Preferences.toPlaybackAuthState(): PlaybackAuthState =
    PlaybackAuthState(
        cookie = this[InnerTubeCookieKey],
        visitorData = this[VisitorDataKey],
        dataSyncId = this[DataSyncIdKey],
        poToken = this[PoTokenKey],
        poTokenGvs = this[PoTokenGvsKey],
        poTokenPlayer = this[PoTokenPlayerKey],
        webClientPoTokenEnabled = this[WebClientPoTokenEnabledKey] ?: false,
    ).normalized()

fun MutablePreferences.clearPlaybackAuthSession(clearAccountIdentity: Boolean = true) {
    remove(InnerTubeCookieKey)
    remove(VisitorDataKey)
    remove(DataSyncIdKey)
    remove(PoTokenKey)
    remove(PoTokenGvsKey)
    remove(PoTokenPlayerKey)
    remove(PoTokenSourceUrlKey)
    if (clearAccountIdentity) {
        remove(AccountNameKey)
        remove(AccountEmailKey)
        remove(AccountChannelHandleKey)
    }
}

fun MutablePreferences.clearPlaybackLoginContext() {
    remove(DataSyncIdKey)
}

fun MutablePreferences.putLegacyPoToken(value: String?) {
    val normalized = value?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    if (normalized == null) {
        remove(PoTokenKey)
    } else {
        this[PoTokenKey] = normalized
    }
    remove(PoTokenGvsKey)
    remove(PoTokenPlayerKey)
}

suspend fun Context.resetPlaybackLoginContext(): PlaybackAuthState {
    dataStore.edit { preferences ->
        preferences.clearPlaybackLoginContext()
    }
    val authState = dataStore.data.first().toPlaybackAuthState()
    YouTube.authState = authState
    YTPlayerUtils.clearPlaybackAuthCaches()
    return authState
}

suspend fun <T> Context.retryWithoutPlaybackLoginContext(
    block: suspend () -> Result<T>,
): Result<T> {
    val initialAuthState = YouTube.currentPlaybackAuthState()
    val initialResult = block()
    val failure = initialResult.exceptionOrNull()

    val currentAuthState = YouTube.currentPlaybackAuthState()
    if (!shouldRetryWithoutPlaybackLoginContext(initialAuthState, currentAuthState, failure)) {
        return initialResult
    }

    YouTube.authState = currentAuthState.withoutPlaybackLoginContext()
    YTPlayerUtils.clearPlaybackAuthCaches()
    return block()
}

internal fun shouldRetryWithoutPlaybackLoginContext(
    initialAuthState: PlaybackAuthState,
    currentAuthState: PlaybackAuthState,
    failure: Throwable?,
): Boolean {
    if (failure !is YTPlayerUtils.InvalidPlaybackLoginContextException) return false
    if (!initialAuthState.hasPlaybackLoginContext) return false
    if (!currentAuthState.hasPlaybackLoginContext) return false
    if (currentAuthState.fingerprint != initialAuthState.fingerprint) return false
    return true
}
