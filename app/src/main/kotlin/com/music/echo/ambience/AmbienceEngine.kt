package echo.music.iad1tya.ambience

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
 * Procedurally synthesised nature ambience (no audio assets, so no APK size cost) played on its own
 * [AudioTrack]. It follows the music: it fades out and the audio thread stops when playback is
 * paused or stopped, and resumes with it. The music player itself is never touched.
 */
object AmbienceEngine {
  private const val SR = 44100
  /** New sounds always start at 30% so ambience never begins loud. */
  const val DEFAULT_LEVEL = 0.3f
  private val sounds = AmbienceSound.entries
  private val n = sounds.size

  private val _levels = MutableStateFlow(sounds.associateWith { 0f })
  val levels: StateFlow<Map<AmbienceSound, Float>> = _levels.asStateFlow()

  private val _master = MutableStateFlow(1f)
  val master: StateFlow<Float> = _master.asStateFlow()

  private val _clickOnTransition = MutableStateFlow(false)
  val clickOnTransition: StateFlow<Boolean> = _clickOnTransition.asStateFlow()

  private val lock = Any()
  @Volatile private var target = FloatArray(n)
  @Volatile private var masterVol = 1f
  @Volatile private var clickPending = false
  @Volatile private var running = false
  @Volatile private var musicPlaying = true

  val isActive: Boolean
    get() = _levels.value.values.any { it > 0f }

  fun setLevel(sound: AmbienceSound, value: Float) {
    if (_auto.value) {
      _auto.value = false
      _autoSummary.value = ""
    }
    _levels.update { it + (sound to value.coerceIn(0f, 1f)) }
    target = FloatArray(n) { _levels.value[sounds[it]] ?: 0f }
    if (musicPlaying) ensureRunning()
  }

  fun toggle(sound: AmbienceSound) {
    val now = _levels.value[sound] ?: 0f
    setLevel(sound, if (now > 0f) 0f else DEFAULT_LEVEL)
  }

  fun setMaster(value: Float) {
    _master.value = value.coerceIn(0f, 1f)
    masterVol = _master.value
  }

  fun setClickOnTransition(enabled: Boolean) {
    _clickOnTransition.value = enabled
    if (enabled && musicPlaying) playClick()
  }

  private val _auto = MutableStateFlow(false)
  val auto: StateFlow<Boolean> = _auto.asStateFlow()
  private val _autoSummary = MutableStateFlow("")
  val autoSummary: StateFlow<String> = _autoSummary.asStateFlow()

  fun setAuto(enabled: Boolean, title: String? = null, artist: String? = null) {
    _auto.value = enabled
    if (enabled) applyAuto(title, artist) else _autoSummary.value = ""
  }

  private var autoBase: Map<AmbienceSound, Float> = emptyMap()
  private var autoSeed = 0f

  /**
   * Auto (AI) mode: builds a mix for this particular song (title/artist mood words, tempo when it
   * is known, time of day and a per-song seed) so every song gets its own soundscape.
   */
  fun applyAuto(title: String?, artist: String?, bpm: Float? = null) {
    if (!_auto.value) return
    val seed = (((title.orEmpty() + "|" + artist.orEmpty()).hashCode() and 0x7fffffff) % 1000) / 1000f
    autoSeed = seed
    autoBase = AmbienceAuto.suggest(title, artist, java.time.LocalTime.now().hour, bpm, seed)
    _autoSummary.value =
      autoBase.entries.filter { it.value > 0f }.sortedByDescending { it.value }
        .joinToString(" · ") { it.key.label }
    pushAuto(0f)
  }

  /** Called every second or so with how far through the song we are: the mix slowly evolves. */
  fun autoProgress(fraction: Float) {
    if (!_auto.value || autoBase.isEmpty()) return
    pushAuto(fraction.coerceIn(0f, 1f))
  }

  private fun pushAuto(f: Float) {
    val next =
      sounds.associateWith { s ->
        val base = autoBase[s] ?: 0f
        if (base <= 0f) 0f
        else
          (base * (0.8f + 0.4f * sin(2f * PI.toFloat() * (f * 1.5f + s.ordinal * 0.37f + autoSeed))))
            .coerceIn(0.05f, 0.45f)
      }
    _levels.value = next
    target = FloatArray(n) { next[sounds[it]] ?: 0f }
    if (musicPlaying) ensureRunning()
  }

  fun stopAll() {
    _auto.value = false
    _autoSummary.value = ""
    _levels.value = sounds.associateWith { 0f }
    target = FloatArray(n)
  }

  /** Called with the music player's play state: ambience fades out on pause and back in on play. */
  fun setMusicPlaying(playing: Boolean) {
    musicPlaying = playing
    if (playing && isActive) ensureRunning()
  }

  fun playClick() {
    clickPending = true
    ensureRunning()
  }

  fun onTrackTransition() {
    if (_clickOnTransition.value && musicPlaying) playClick()
  }


  private fun ensureRunning() {
    synchronized(lock) {
      if (running) return
      running = true
      Thread({ runLoop() }, "PattukalAmbience").apply { isDaemon = true }.start()
    }
  }

  private class Synth {
    val twoPi = (2 * PI).toFloat()
    var rnd = 987654321
    fun noise(): Float {
      rnd = rnd * 1664525 + 1013904223
      return (rnd ushr 8) / 8388608f - 1f
    }
    fun chance(perSecond: Float) = (noise() + 1f) * 0.5f < perSecond / SR
    fun lp(fc: Float) = 1f - exp(-twoPi * fc / SR)

    // coefficients
    val aR1 = lp(1500f); val aR2 = lp(6500f)
    val aT1 = lp(45f); val aT2 = lp(28f)
    val aV1 = lp(1300f); val aV2 = lp(220f); val aV3 = lp(3200f); val aV4 = lp(900f)
    val aO1 = lp(80f); val aO2 = lp(420f); val aO3 = lp(2200f)
    val aW1 = lp(40f); val aW2 = lp(380f); val aW3 = lp(120f)
    val aF1 = lp(2500f); val aF2 = lp(130f)

    // state
    var r1 = 0f; var r2 = 0f
    val dropEnv = FloatArray(4); val dropPh = FloatArray(4); val dropF = FloatArray(4); var dropIdx = 0
    var tA = 0f; var tB = 0f; var thunderAge = -1f; var thunderWait = 3f; var thunderHp = 0f
    var chirpLeft = 0; var chirpLen = 1; var chirpF0 = 3000f; var chirpF1 = 3500f; var chirpType = 0
    var chirpPhase = 0f; var birdGap = SR / 2; var burstLeft = 0
    var v1 = 0f; var v2 = 0f; var v3 = 0f; var v4 = 0f
    var gEnv = 0f; var gPh = 0f; var gF = 500f
    var o1 = 0f; var o2 = 0f; var o3 = 0f
    var w1 = 0f; var w2 = 0f; var w3 = 0f
    var f1 = 0f; var f2 = 0f; var popEnv = 0f
    var ck1 = 0f; var ck2 = 0f
    var clickEnv = 0f; var clickPhase = 0f

    private fun pulse(x: Float, d: Float, a: Float): Float {
      if (x < d) return 0f
      val y = x - d
      return a * min(1f, y / 0.25f) * exp(-y / 3.2f)
    }

    fun sample(cur: FloatArray, ts: Float, m: Float): Float {
      var out = 0f
      val nz = noise()

      if (cur[0] > 0f) { // rain: broadband hiss + body + individual drops
        r1 += aR1 * (nz - r1)
        r2 += aR2 * (nz - r2)
        val flutter = 0.9f + 0.1f * sin(twoPi * 0.37f * ts)
        var rain = ((r2 - r1) * 0.9f + r1 * 0.75f) * flutter
        if (chance(70f)) {
          dropIdx = (dropIdx + 1) and 3
          dropEnv[dropIdx] = 0.4f + (noise() + 1f) * 0.3f
          dropF[dropIdx] = 1800f + (noise() + 1f) * 1800f
        }
        for (d in 0 until 4) {
          if (dropEnv[d] > 0.001f) {
            dropPh[d] += twoPi * dropF[d] / SR
            if (dropPh[d] > twoPi) dropPh[d] -= twoPi
            rain += sin(dropPh[d]) * dropEnv[d] * 0.25f
            dropEnv[d] *= exp(-1f / (SR * 0.012f))
          }
        }
        out += cur[0] * rain
      }

      if (cur[1] > 0f) { // thunder: crack + long rolling rumble
        if (thunderAge < 0f) {
          thunderWait -= 1f / SR
          if (thunderWait <= 0f) {
            thunderAge = 0f
            thunderWait = 9f + (noise() + 1f) * 6f
          }
        } else {
          thunderAge += 1f / SR
          if (thunderAge > 12f) thunderAge = -1f
        }
        tA += aT1 * (nz - tA)
        tB += aT2 * (tA - tB)
        thunderHp += 0.3f * (nz - thunderHp)
        if (thunderAge >= 0f) {
          val env = pulse(thunderAge, 0f, 1f) + pulse(thunderAge, 1.3f, 0.5f) + pulse(thunderAge, 2.6f, 0.3f)
          val crack = (nz - thunderHp) * exp(-thunderAge * 16f) * 0.45f
          out += cur[1] * (tB * 70f * env + tA * 20f * env + crack)
        }
      }

      if (cur[2] > 0f) { // birds: harmonic chirps, three call types
        if (chirpLeft > 0) {
          val p = 1f - chirpLeft / chirpLen.toFloat()
          var f = chirpF0 + (chirpF1 - chirpF0) * p
          if (chirpType == 2) f += sin(twoPi * 26f * ts) * 650f else f += sin(twoPi * 40f * ts) * 90f
          chirpPhase += twoPi * f / SR
          if (chirpPhase > twoPi) chirpPhase -= twoPi
          val env = max(0f, sin(PI.toFloat() * p)).pow(1.4f)
          val tone = sin(chirpPhase) + 0.35f * sin(2f * chirpPhase) + 0.12f * sin(3f * chirpPhase)
          out += cur[2] * tone * env * 0.22f
          chirpLeft--
          if (chirpLeft == 0) {
            burstLeft--
            birdGap =
              if (burstLeft > 0) (SR * (0.06f + (noise() + 1f) * 0.07f)).toInt()
              else (SR * (1.2f + (noise() + 1f) * 2.5f)).toInt()
          }
        } else {
          birdGap--
          if (birdGap <= 0) {
            if (burstLeft <= 0) burstLeft = 2 + ((noise() + 1f) * 1.8f).toInt()
            chirpType = ((noise() + 1f) * 1.49f).toInt()
            chirpF0 = 2400f + (noise() + 1f) * 900f
            when (chirpType) {
              0 -> { chirpF1 = chirpF0 * 1.35f; chirpLen = (SR * 0.09f).toInt() }
              1 -> { chirpF1 = chirpF0 * 0.72f; chirpLen = (SR * 0.12f).toInt() }
              else -> { chirpF1 = chirpF0; chirpLen = (SR * 0.26f).toInt() }
            }
            chirpLeft = chirpLen
          }
        }
      }

      if (cur[3] > 0f) { // river: two babbling bands + gurgles
        v1 += aV1 * (nz - v1); v2 += aV2 * (nz - v2)
        v3 += aV3 * (nz - v3); v4 += aV4 * (nz - v4)
        val m1 = 0.7f + 0.3f * sin(twoPi * 0.31f * ts)
        val m2 = 0.7f + 0.3f * sin(twoPi * 0.53f * ts + 2f)
        var river = (v1 - v2) * 1.6f * m1 + (v3 - v4) * 1.1f * m2
        if (chance(7f)) { gEnv = 1f; gF = 350f + (noise() + 1f) * 350f }
        if (gEnv > 0.001f) {
          gF *= 1.00006f
          gPh += twoPi * gF / SR
          if (gPh > twoPi) gPh -= twoPi
          river += sin(gPh) * gEnv * 0.14f
          gEnv *= exp(-1f / (SR * 0.045f))
        }
        out += cur[3] * river
      }

      if (cur[4] > 0f) { // ocean: swell-shaped surf + foam hiss
        o1 += aO1 * (nz - o1); o2 += aO2 * (nz - o2); o3 += aO3 * (nz - o3)
        val s = max(0f, 0.5f + 0.5f * sin(twoPi * 0.1f * ts)).pow(1.5f)
        val s2 = max(0f, 0.5f + 0.5f * sin(twoPi * 0.1f * ts + 0.7f)).pow(2.5f)
        out += cur[4] * (o1 * 11f * s + o2 * 2.2f * s + (nz - o3) * 0.22f * s2)
      }

      if (cur[5] > 0f) { // wind: slow gusts plus soft whistle
        w1 += aW1 * (nz - w1); w2 += aW2 * (nz - w2); w3 += aW3 * (nz - w3)
        val gust = 0.55f + 0.45f * sin(twoPi * 0.07f * ts + 1.3f)
        val gust2 = 0.5f + 0.5f * sin(twoPi * 0.19f * ts)
        out += cur[5] * (w1 * 14f * gust + (w2 - w3) * 2.2f * gust2)
      }

      if (cur[6] > 0f) { // campfire: random pops and a warm rumble
        f1 += aF1 * (nz - f1); f2 += aF2 * (nz - f2)
        if (chance(14f)) popEnv = 0.4f + (noise() + 1f) * 0.5f
        popEnv *= exp(-1f / (SR * 0.009f))
        out += cur[6] * ((nz - f1) * popEnv + f2 * 5f * (0.8f + 0.2f * sin(twoPi * 3.1f * ts)))
      }

      if (cur[7] > 0f) { // night crickets: two chirping voices
        ck1 += twoPi * 4300f / SR; if (ck1 > twoPi) ck1 -= twoPi
        ck2 += twoPi * 4750f / SR; if (ck2 > twoPi) ck2 -= twoPi
        val g1 = if (sin(twoPi * 27f * ts) > 0f && sin(twoPi * 1.3f * ts) > 0.1f) 1f else 0f
        val g2 = if (sin(twoPi * 31f * ts) > 0f && sin(twoPi * 0.9f * ts + 1f) > 0.2f) 1f else 0f
        out += cur[7] * (sin(ck1) * g1 + sin(ck2) * g2 * 0.8f) * 0.1f
      }

      if (clickEnv > 0.001f) {
        clickPhase += twoPi * 1700f / SR
        if (clickPhase > twoPi) clickPhase -= twoPi
        out += (sin(clickPhase) * 0.6f + nz * 0.25f) * clickEnv * 0.5f * max(m, 0.4f)
        clickEnv *= exp(-1f / (SR * 0.012f))
      }
      return out
    }
  }

  private fun runLoop() {
    val track =
      try {
        val minBuf =
          AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
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
          .setBufferSizeInBytes(max(minBuf, 8192) * 2)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      } catch (e: Exception) {
        synchronized(lock) { running = false }
        return
      }
    track.play()

    val synth = Synth()
    val buf = ShortArray(1024)
    val cur = FloatArray(n)
    var t = 0L
    var idleBlocks = 0

    while (true) {
      val tg = target
      val m = masterVol
      val playing = musicPlaying
      var activeSum = 0f
      for (k in 0 until n) {
        val goal = if (playing) tg[k] * m else 0f
        cur[k] += (goal - cur[k]) * 0.1f
        if (cur[k] < 0.0005f && goal == 0f) cur[k] = 0f
        activeSum += cur[k]
      }
      if (clickPending) {
        clickPending = false
        synth.clickEnv = 1f
        synth.clickPhase = 0f
      }
      if (activeSum == 0f && synth.clickEnv < 0.001f) {
        idleBlocks++
        if (idleBlocks > 20) {
          val stop =
            synchronized(lock) {
              val wantsSound = musicPlaying && target.any { it > 0f }
              if (!clickPending && !wantsSound) {
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
        val out = synth.sample(cur, t / SR.toFloat(), m)
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
