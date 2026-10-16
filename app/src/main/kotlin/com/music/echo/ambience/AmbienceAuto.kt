package echo.music.iad1tya.ambience

/**
 * Smart mix for Auto mode. A small on-device rule engine reads the song's title/artist for mood and
 * setting words (English and common Hindi/Malayalam/Tamil transliterations) and blends that with
 * the time of day. Levels stay gentle (never above 40%).
 */
object AmbienceAuto {
  private data class Rule(val words: List<String>, val mix: Map<AmbienceSound, Float>)

  private val rules =
    listOf(
      Rule(
        listOf("rain", "baarish", "barish", "barsaat", "barsat", "monsoon", "megh", "mazha", "mazhai", "cloud", "storm", "thunder", "toofan"),
        mapOf(AmbienceSound.RAIN to 0.35f, AmbienceSound.THUNDER to 0.22f, AmbienceSound.WIND to 0.12f)
      ),
      Rule(
        listOf("sea", "ocean", "beach", "samudra", "kadal", "wave", "shore", "sagar", "island"),
        mapOf(AmbienceSound.OCEAN to 0.38f, AmbienceSound.WIND to 0.14f, AmbienceSound.BIRDS to 0.1f)
      ),
      Rule(
        listOf("river", "nadi", "dariya", "paani", "pani", "water", "stream", "ganga", "puzha", "waterfall"),
        mapOf(AmbienceSound.RIVER to 0.38f, AmbienceSound.BIRDS to 0.15f)
      ),
      Rule(
        listOf("morning", "subah", "sunrise", "bird", "sparrow", "koyal", "spring", "bahar", "sunshine", "garden", "flower", "phool"),
        mapOf(AmbienceSound.BIRDS to 0.35f, AmbienceSound.RIVER to 0.14f, AmbienceSound.WIND to 0.1f)
      ),
      Rule(
        listOf("night", "raat", "ratri", "moon", "chand", "chaand", "star", "taare", "tara", "dream", "sleep", "neend", "midnight", "lullaby"),
        mapOf(AmbienceSound.NIGHT to 0.3f, AmbienceSound.WIND to 0.12f, AmbienceSound.RAIN to 0.1f)
      ),
      Rule(
        listOf("fire", "aag", "flame", "burn", "jalte", "smoke", "campfire", "bonfire"),
        mapOf(AmbienceSound.FIRE to 0.35f, AmbienceSound.NIGHT to 0.15f)
      ),
      Rule(
        listOf("sad", "dard", "broken", "tanha", "alone", "judai", "bichad", "tears", "aansu", "cry", "heartbreak", "miss", "yaad", "virah"),
        mapOf(AmbienceSound.RAIN to 0.3f, AmbienceSound.WIND to 0.15f, AmbienceSound.THUNDER to 0.1f)
      ),
      Rule(
        listOf("love", "pyar", "ishq", "mohabbat", "romantic", "prem", "kadhal", "heart", "dil", "hug"),
        mapOf(AmbienceSound.RAIN to 0.15f, AmbienceSound.BIRDS to 0.12f, AmbienceSound.RIVER to 0.12f)
      ),
    )

  fun suggest(title: String?, artist: String?, hour: Int, bpm: Float? = null, seed: Float = 0f): Map<AmbienceSound, Float> {
    val text = " ${title.orEmpty()} ${artist.orEmpty()} ".lowercase()
    val mix = HashMap<AmbienceSound, Float>()
    var matched = false
    for (rule in rules) {
      if (rule.words.any { text.contains(it) }) {
        matched = true
        rule.mix.forEach { (k, v) -> mix[k] = maxOf(mix[k] ?: 0f, v) }
      }
    }
    // time-of-day base when the song gives no hint (and a light blend otherwise)
    val base =
      when (hour) {
        in 5..9 -> mapOf(AmbienceSound.BIRDS to 0.3f, AmbienceSound.RIVER to 0.14f)
        in 10..16 -> mapOf(AmbienceSound.WIND to 0.18f, AmbienceSound.BIRDS to 0.14f)
        in 17..20 -> mapOf(AmbienceSound.OCEAN to 0.24f, AmbienceSound.WIND to 0.12f)
        else -> mapOf(AmbienceSound.NIGHT to 0.26f, AmbienceSound.RAIN to 0.14f)
      }
    base.forEach { (k, v) -> mix[k] = if (matched) maxOf(mix[k] ?: 0f, v * 0.5f) else v }
    // per-song variation: a seeded accent sound so no two songs sound alike
    val extra = AmbienceSound.entries[(seed * AmbienceSound.entries.size).toInt().coerceIn(0, AmbienceSound.entries.size - 1)]
    if ((mix[extra] ?: 0f) == 0f && extra != AmbienceSound.THUNDER) mix[extra] = 0.1f + seed * 0.08f
    // tempo: slow songs get calm water/rain, fast songs get lively birds/fire/crickets
    if (bpm != null && bpm > 0f) {
      val calm = listOf(AmbienceSound.RAIN, AmbienceSound.OCEAN, AmbienceSound.RIVER, AmbienceSound.WIND)
      val lively = listOf(AmbienceSound.BIRDS, AmbienceSound.FIRE, AmbienceSound.NIGHT)
      val k = if (bpm < 90f) 1.25f else if (bpm > 120f) 0.7f else 1f
      val l = if (bpm < 90f) 0.7f else if (bpm > 120f) 1.3f else 1f
      calm.forEach { c -> mix[c]?.let { v -> mix[c] = v * k } }
      lively.forEach { c -> mix[c]?.let { v -> mix[c] = v * l } }
      if (bpm > 120f) mix[AmbienceSound.THUNDER]?.let { mix[AmbienceSound.THUNDER] = it * 0.4f }
    }
    return mix.mapValues { it.value.coerceIn(0f, 0.4f) }
  }
}
