package com.music.echo.metadata.cleaner

/**
 * Cleans short display text (artist names, titles) coming from the YouTube Music API.
 *
 * YouTube sometimes HTML-escapes `&` and then splits artist runs on the raw `&`, leaving orphan
 * fragments such as `"amp; The Pharaoh"` as an artist run (observed for "Sabin Rai & The Pharaoh":
 * runs are `Sabin Rai` / ` & ` / `amp; The Pharaoh`, each with its own browseId). Rendered via
 * `joinToString(", ")`, that shows up as `"Sabin Rai, amp; The Pharaoh"`.
 *
 * This decodes HTML entities and drops those orphan `xxx;` fragments, which can never be legitimate
 * display text.
 *
 * Only named-entity fragments (`amp;`, `quot;`, `apos;`, `lt;`, `gt;`, `nbsp;`) are treated as
 * split remnants: as standalone tokens they never occur in legitimate text. Numeric fragments
 * such as `#123;` are left alone because they occur naturally (e.g. "Issue #123; please check").
 */
object ApiText {

  private val HEX_ENTITY_REGEX = "&#x([0-9a-fA-F]+);".toRegex()
  private val DEC_ENTITY_REGEX = "&#(\\d+);".toRegex()
  private val ORPHAN_FRAGMENT_REGEX =
    "(?:^|(?<=\\s))(?:amp|quot|apos|lt|gt|nbsp);\\s*".toRegex()

  fun clean(raw: String): String {
    var text = raw
    text =
      HEX_ENTITY_REGEX.replace(text) { match ->
        match.groupValues[1]
          .toIntOrNull(16)
          ?.takeIf { it in 0..0x10FFFF }
          ?.let { String(Character.toChars(it)) } ?: match.value
      }
    text =
      DEC_ENTITY_REGEX.replace(text) { match ->
        match.groupValues[1]
          .toIntOrNull()
          ?.takeIf { it in 0..0x10FFFF }
          ?.let { String(Character.toChars(it)) } ?: match.value
      }
    text =
      text
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
    text = ORPHAN_FRAGMENT_REGEX.replace(text, "")
    return text
  }
}
