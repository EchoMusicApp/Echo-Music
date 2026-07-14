package iad1tya.echo.music.echomusic.updater.downloadmanager

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

@OptIn(UnstableApi::class)
class EchoNotificationProvider(
    private val context: Context,
    notificationIdProvider: DefaultMediaNotificationProvider.NotificationIdProvider,
    channelId: String,
    channelNameResourceId: Int,
) : MediaNotification.Provider {

    private val defaultProvider = DefaultMediaNotificationProvider(
        context,
        notificationIdProvider,
        channelId,
        channelNameResourceId
    )

    fun setSmallIcon(iconResId: Int): EchoNotificationProvider {
        defaultProvider.setSmallIcon(iconResId)
        return this
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val mediaNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback
        )

        val player = mediaSession.player
        val shouldBeOngoing = player.playWhenReady &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED

        val updatedNotification = Notification.Builder
            .recoverBuilder(context, mediaNotification.notification)
            .setOngoing(shouldBeOngoing)
            .build()

        copyMediaSessionToken(mediaNotification.notification, updatedNotification)

        return MediaNotification(mediaNotification.notificationId, updatedNotification)
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        defaultProvider.handleCustomCommand(session, action, extras)

    private fun copyMediaSessionToken(source: Notification, target: Notification) {
        if (Build.VERSION.SDK_INT >= 33) {
            source.extras.getParcelable(
                Notification.EXTRA_MEDIA_SESSION,
                android.media.session.MediaSession.Token::class.java
            )?.let {
                target.extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, it)
            }
        } else {
            @Suppress("DEPRECATION")
            source.extras.getParcelable<android.media.session.MediaSession.Token>(
                Notification.EXTRA_MEDIA_SESSION
            )?.let {
                target.extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, it)
            }
        }
    }
}
