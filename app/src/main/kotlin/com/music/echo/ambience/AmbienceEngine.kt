package echo.music.iad1tya.ambience

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AmbienceSound(val label: String, val emoji: String) {
  RAIN("Rain", "🌧️"),
  THUNDER("Thunder", "⛈️"),
  BIRDS("Birds", "🐦"),
  RIVER("River flow", "🏞️"),
  OCEAN("Ocean waves", "🌊"),
  WIND("Wind", "🍃"),
  FIRE("Campfire", "🔥"),
  NIGHT("Night crickets", "🌙"),
}

/**
 * Procedurally synthesised nature ambience. Everything is generated in code (no audio assets, so
 * no APK size cost) and played on its own [AudioTrack]; the music player is never touched.
 */
object AmbienceEngine {
  private const val SR = 22050
  private val sounds = AmbienceSound.entries
  private val n = sounds.size

  private val _levels = MutableStateFlow(sounds.associateWith { 0f })
  val levels: StateFlow<Map<AmbienceSound, Float>> = _levels.asStateFlow()

  private val _master = MutableStateFlow(0.8f)
  val master: StateFlow<Float> = _master.asStateFlow()

  private val _clickOnTransition = MutableStateFlow(false)
  val clickOnTransition: StateFlow<Boolean> = _clickOnTransition.asStateFlow()

  private val lock = Any()
  @Volatile private var target = FloatArray(n)
  @Volatile private var masterVol = 0.8f
  @Volatile private var clickPending = false
  @Volatile private var running = false

  val isActive: Boolean
    get() = _levels.value.values.any { it > 0f }

  fun setLevel(sound: AmbienceSound, value: Float) {
    _levels.update { it + (sound to value.coerceIn(0f, 1f)) }
    target = FloatArray(n) { _levels.value[sounds[it]] ?: 0f }
    ensureRunning()
  }

  fun toggle(sound: AmbienceSound) {
    val now = _levels.value[sound] ?: 0f
    setLevel(sound, if (now > 0f) 0f else 0.6f)
  }

  fun setMaster(value: Float) {
    _master.value = value.coerceIn(0f, 1f)
    masterVol = _master.value
  }

  fun setClickOnTransition(enabled: Boolean) {
    _clickOnTransition.value = enabled
    if (enabled) playClick()
  }

  fun stopAll() {
    _levels.value = sounds.associateWith { 0f }
    target = FloatArray(n)
  }

  /** Soft transition tick, used when the song changes. */
  fun playClick() {
    clickPending = true
    ensureRunning()
  }

  fun onTrackTransition() {
    if (_clickOnTransition.value) playClick()
  }

  private fun ensureRunning() {
    synchronized(lock) {
      if (running) return
      running = true
      Thread({ runLoop() }, "PattukalAmbience").apply { isDaemon = true }.start()
    }
  }

  private fun runLoop() {
    val track =
      try {
        val minBuf =
          AudioTrack.getMinBufferSize(
            SR,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
          )
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setSampleRate(SR)
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(max(minBuf, 4096) * 2)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      } catch (e: Exception) {
        synchronized(lock) { running = false }
        return
      }
    track.play()

    val buf = ShortArray(1024)
    val cur = FloatArray(n)
    var rnd = 123456789
    fun noise(): Float {
      rnd = rnd * 1664525 + 1013904223
      return (rnd ushr 8) / 8388608f - 1f
    }

    // Filter / generator state.
    var rainLp = 0f; var dropEnv = 0f; var dropLp = 0f
    var rvA = 0f; var rvB = 0f
    var ocean = 0f; var foamLp = 0f
    var wind = 0f
    var brown = 0f; var thunderEnv = 0f; var thunderWait = SR * 4
    var chirpLeft = 0; var chirpLen = 1; var chirpF0 = 3000f; var chirpF1 = 3500f
    var chirpPhase = 0f; var birdGap = SR; var burstLeft = 0
    var popEnv = 0f; var fireLp = 0f; var fireRumble = 0f
    var cricketPhase = 0f
    var clickEnv = 0f; var clickPhase = 0f
    var t = 0L
    var idleBlocks = 0
    val twoPi = (2 * PI).toFloat()

    while (true) {
      val tg = target
      val m = masterVol
      var activeSum = 0f
      for (k in 0 until n) {
        cur[k] += (tg[k] * m - cur[k]) * 0.12f
        if (cur[k] < 0.0005f && tg[k] == 0f) cur[k] = 0f
        activeSum += cur[k]
      }
      if (clickPending) {
        clickPending = false
        clickEnv = 1f
        clickPhase = 0f
      }
      if (activeSum == 0f && clickEnv < 0.001f) {
        idleBlocks++
        if (idleBlocks > 40) {
          val stop =
            synchronized(lock) {
              if (!clickPending && target.all { it == 0f }) {
                running = false
                true
              } else false
            }
          if (stop) break
          idleBlocks = 0
        }
      } else {
        idleBlocks = 0
      }

      for (i in buf.indices) {
        val ts = t / SR.toFloat()
        var out = 0f
        val nz = noise()

        if (cur[0] > 0f) { // rain
          rainLp += 0.35f * (nz - rainLp)
          val hiss = nz - rainLp
          if ((noise() + 1f) * 0.5f < 0.0007f) dropEnv = 1f
          dropEnv *= 0.994f
          dropLp += 0.25f * (nz - dropLp)
          out += cur[0] * (hiss * 0.5f + dropLp * dropEnv * 1.2f)
        }
        if (cur[1] > 0f) { // thunder
          thunderWait--
          if (thunderWait <= 0) {
            thunderEnv = 1f
            thunderWait = SR * (7 + ((noise() + 1f) * 6f).toInt())
          }
          thunderEnv *= exp(-1f / (SR * 2.6f))
          brown += 0.004f * (nz - brown)
          val crack = nz * thunderEnv * thunderEnv * thunderEnv * thunderEnv * 0.25f
          out += cur[1] * (brown * 26f * thunderEnv + crack)
        }
        if (cur[2] > 0f) { // birds
          if (chirpLeft > 0) {
            val p = 1f - chirpLeft / chirpLen.toFloat()
            val f = chirpF0 + (chirpF1 - chirpF0) * p + sin(twoPi * 38f * ts) * 120f
            chirpPhase += twoPi * f / SR
            if (chirpPhase > twoPi) chirpPhase -= twoPi
            out += cur[2] * sin(chirpPhase) * sin(PI.toFloat() * p) * 0.3f
            chirpLeft--
            if (chirpLeft == 0) {
              burstLeft--
              birdGap =
                if (burstLeft > 0) (SR * (0.05f + (noise() + 1f) * 0.05f)).toInt()
                else (SR * (1.5f + (noise() + 1f) * 2.2f)).toInt()
            }
          } else {
            birdGap--
            if (birdGap <= 0) {
              if (burstLeft <= 0) burstLeft = 2 + ((noise() + 1f) * 1.6f).toInt()
              chirpLen = (SR * (0.06f + (noise() + 1f) * 0.05f)).toInt()
              chirpLeft = chirpLen
              chirpF0 = 2300f + (noise() + 1f) * 800f
              chirpF1 = chirpF0 * (0.85f + (noise() + 1f) * 0.3f)
            }
          }
        }
        if (cur[3] > 0f) { // river
          rvA += 0.25f * (nz - rvA)
          rvB += 0.03f * (nz - rvB)
          val mod = 0.75f + 0.25f * sin(twoPi * 0.23f * ts)
          out += cur[3] * (rvA - rvB) * 1.7f * mod
        }
        if (cur[4] > 0f) { // ocean
          ocean += 0.02f * (nz - ocean)
          foamLp += 0.3f * (nz - foamLp)
          val s = 0.5f + 0.5f * sin(twoPi * 0.11f * ts)
          val swell = s * s
          out += cur[4] * (ocean * 9f * swell + (nz - foamLp) * swell * swell * swell * 0.18f)
        }
        if (cur[5] > 0f) { // wind
          wind += 0.01f * (nz - wind)
          val g = 0.6f + 0.4f * sin(twoPi * 0.07f * ts + 1.3f)
          out += cur[5] * wind * 13f * g
        }
        if (cur[6] > 0f) { // campfire
          if ((noise() + 1f) * 0.5f < 0.0006f) popEnv = 1f
          popEnv *= 0.97f
          fireLp += 0.5f * (nz - fireLp)
          fireRumble += 0.02f * (nz - fireRumble)
          out += cur[6] * ((nz - fireLp) * popEnv * 0.9f + fireRumble * 5f)
        }
        if (cur[7] > 0f) { // night crickets
          cricketPhase += twoPi * 4300f / SR
          if (cricketPhase > twoPi) cricketPhase -= twoPi
          val pulse = if (sin(twoPi * 28f * ts) > 0f) 1f else 0f
          val group = if (sin(twoPi * 1.4f * ts) > 0.1f) 1f else 0f
          out += cur[7] * sin(cricketPhase) * pulse * group * 0.14f
        }
        if (clickEnv > 0.001f) {
          clickPhase += twoPi * 1700f / SR
          if (clickPhase > twoPi) clickPhase -= twoPi
          out += (sin(clickPhase) * 0.6f + nz * 0.25f) * clickEnv * 0.5f * max(m, 0.4f)
          clickEnv *= exp(-1f / (SR * 0.012f))
        }

        val shaped = out / (1f + abs(out))
        buf[i] = (shaped * 30000f).toInt().toShort()
        t++
      }
      track.write(buf, 0, buf.size)
    }
    try {
      track.stop()
    } catch (_: Exception) {}
    track.release()
  }
}
