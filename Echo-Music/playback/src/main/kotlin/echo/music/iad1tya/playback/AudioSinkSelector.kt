package echo.music.iad1tya.playback

import androidx.media3.exoplayer.audio.AudioSink

object AudioSinkSelector {
    fun selectSink(
        isBitPerfectActive: Boolean,
        usbDacAudioSink: AudioSink?,
        defaultAudioSink: AudioSink
    ): AudioSink {
        return if (isBitPerfectActive && usbDacAudioSink != null) {
            usbDacAudioSink
        } else {
            defaultAudioSink
        }
    }

    fun createRoutingSink(
        defaultAudioSink: AudioSink,
        usbDacAudioSink: AudioSink,
        isBitPerfectActive: () -> Boolean
    ): AudioSink {
        return RoutingAudioSink(
            defaultSink = defaultAudioSink,
            usbSink = usbDacAudioSink,
            isBitPerfectActive = isBitPerfectActive
        )
    }
}
