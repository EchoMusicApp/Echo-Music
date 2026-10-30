package echo.music.iad1tya.utils

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

/**
 * Best-effort parsing of the dates YouTube shows for episodes, in the app's language: "19 dic
 * 2025", "2 ott", "Dec 19, 2025", "3 giorni fa", "2 weeks ago", "ieri". Used to order episodes of
 * different shows; null when the text is not recognized.
 */
object EpisodeDates {
  private val relative = Regex("""(\d+)\s*(\p{L}+)""")
  private val dayMonthYear = Regex("""^(\d{1,2})\s+(\p{L}+)\.?(?:\s+(\d{4}))?$""")
  private val monthDayYear = Regex("""^(\p{L}+)\.?\s+(\d{1,2}),?(?:\s+(\d{4}))?$""")

  fun parse(text: String?, now: Long = System.currentTimeMillis()): Long? {
    val value = text?.trim()?.lowercase() ?: return null
    if (value.isEmpty()) return null
    if (value in TODAY) return now
    if (value in YESTERDAY) return now - DAY
    absolute(value, now)?.let { return it }
    if (RELATIVE_HINTS.none { value.contains(it) }) return null
    val match = relative.find(value) ?: return null
    val amount = match.groupValues[1].toLongOrNull() ?: return null
    val unit = match.groupValues[2]
    val step =
      when {
        unit.startsWith("sec") -> 1_000L
        unit.startsWith("min") -> 60_000L
        unit.startsWith("or") || unit.startsWith("h") -> 3_600_000L
        unit.startsWith("giorn") || unit.startsWith("day") -> DAY
        unit.startsWith("settiman") || unit.startsWith("week") -> 7 * DAY
        unit.startsWith("mes") || unit.startsWith("month") -> 30 * DAY
        unit.startsWith("ann") || unit.startsWith("year") -> 365 * DAY
        else -> return null
      }
    return now - amount * step
  }

  private fun absolute(value: String, now: Long): Long? {
    val (day, monthName, year) =
      dayMonthYear.find(value)?.groupValues?.let { Triple(it[1], it[2], it[3]) }
        ?: monthDayYear.find(value)?.groupValues?.let { Triple(it[2], it[1], it[3]) }
        ?: return null
    val month = monthIndex(monthName) ?: return null
    val calendar = Calendar.getInstance().apply { timeInMillis = now }
    val currentYear = calendar.get(Calendar.YEAR)
    calendar.clear()
    calendar.set(year.toIntOrNull() ?: currentYear, month, day.toIntOrNull() ?: return null)
    // Without a year the date is in the current one, unless that would be in the future.
    if (year.isEmpty() && calendar.timeInMillis > now) calendar.add(Calendar.YEAR, -1)
    return calendar.timeInMillis
  }

  private fun monthIndex(name: String): Int? {
    val key = name.trimEnd('.')
    for (locale in listOf(Locale.getDefault(), Locale.ENGLISH)) {
      val symbols = DateFormatSymbols(locale)
      // Both arrays have a 13th, empty entry for lunar calendars.
      val names =
        symbols.months.take(12).map { it.lowercase(locale) } +
          symbols.shortMonths.take(12).map { it.lowercase(locale).trimEnd('.') }
      val index = names.indexOfFirst { it.isNotEmpty() && (it == key || it.startsWith(key)) }
      if (index >= 0) return index % 12
    }
    return null
  }

  private const val DAY = 86_400_000L
  private val TODAY = setOf("oggi", "today")
  private val YESTERDAY = setOf("ieri", "yesterday")
  private val RELATIVE_HINTS = listOf(" fa", "ago")
}
