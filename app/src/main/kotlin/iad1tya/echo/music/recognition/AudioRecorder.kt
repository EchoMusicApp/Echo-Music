package iad1tya.echo.music.recognition

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import kotlin.math.max

class AudioRecorder(private val scope: CoroutineScope) {
    private var instance: AudioRecord? = null
    private var job: Job? = null
    private val mutex = Mutex()
    private val _active = MutableStateFlow(false)
    private val _duration = MutableStateFlow(0)
    private val _buffer = MutableStateFlow(ByteArray(0))
    val active get() = _active.asStateFlow()
    val duration get() = _duration.asStateFlow()
    val buffer get() = _buffer.asStateFlow()


    @Throws(SecurityException::class)
    fun start() {
        scope.launch {
            mutex.withLock {
                if (_active.value) return@launch
                runCatching {
                    instance = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        BUFFER_SIZE
                    )
                    instance?.startRecording()
                    reset(true)
                    job = scope.launch(Dispatchers.IO) { loop() }
                }.onFailure {
                    it.printStackTrace()
                    stop()
                }
            }
        }
    }

    fun stop() {
        scope.launch {
            mutex.withLock {
                if (!_active.value) return@launch
                job?.cancelAndJoin()
                runCatching {
                    instance?.stop()
                    instance?.release()
                }
                instance = null
                reset(false)
            }
        }
    }

    private suspend fun loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        runCatching {
            val result = mutableListOf<Byte>()
            while (coroutineContext.isActive) {
                val buffer = ByteArray(BUFFER_SIZE)
                val read = instance?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    // Calculate and log max amplitude sporadically
                    if (System.currentTimeMillis() % 2000 < 50) {
                         val shorts = ShortArray(read / 2)
                         java.nio.ByteBuffer.wrap(buffer, 0, read).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                         val maxAmp = shorts.maxOrNull() ?: 0
                         android.util.Log.d("EchoMusic", "AudioRecorder: Read $read bytes, Max Amp: $maxAmp")
                    }

                    val actualRead = buffer.take(read)
                    result.addAll(actualRead)
                    val duration = result.size / (SAMPLE_RATE * SAMPLE_WIDTH * CHANNEL_COUNT)
                    _duration.emit(duration)
                    _buffer.emit(result.toByteArray())
                }
            }
        }.onFailure {
            it.printStackTrace()
            reset(false)
        }
    }

    private suspend fun reset(active: Boolean) {
        _active.emit(active)
        _duration.emit(0)
        _buffer.emit(ByteArray(0))
    }

    companion object {
        private const val SAMPLE_RATE = 16000 // Changed from 44100 to match ShazamSignature requirement
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNEL_COUNT = 1
        private const val SAMPLE_WIDTH = 2
        private const val BUFFER_SIZE_MULTIPLIER = 2
        private val BUFFER_SIZE = max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_MULTIPLIER,
            SAMPLE_RATE * SAMPLE_WIDTH * CHANNEL_COUNT
        )
    }
}
