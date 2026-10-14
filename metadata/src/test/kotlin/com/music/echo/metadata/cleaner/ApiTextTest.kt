package com.music.echo.metadata.cleaner

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * YouTube Music sometimes returns display text where `&` was HTML-escaped and then split on the raw
 * `&`, leaving orphan fragments such as `"amp; The Pharaoh"` as an artist run (see search for
 * "Samjhanchu Sabin Rai": runs are `Sabin Rai` / ` & ` / `amp; The Pharaoh`, each with its own
 * browseId).
 */
class ApiTextTest {

  @Test
  fun testOrphanAmpFragmentIsStripped() {
    assertEquals("The Pharaoh", ApiText.clean("amp; The Pharaoh"))
  }

  @Test
  fun testEscapedAmpersandIsDecoded() {
    assertEquals("Sabin Rai & The Pharaoh", ApiText.clean("Sabin Rai &amp; The Pharaoh"))
  }

  @Test
  fun testPlainAmpersandIsPreserved() {
    assertEquals("Sabin Rai & The Pharaoh", ApiText.clean("Sabin Rai & The Pharaoh"))
  }

  @Test
  fun testNamedEntitiesAreDecoded() {
    assertEquals("Rock \"Roll\" 'King'", ApiText.clean("Rock &quot;Roll&quot; &#39;King&#39;"))
  }

  @Test
  fun testNumericEntitiesAreDecoded() {
    assertEquals("A A A", ApiText.clean("A &#65; &#x41;"))
  }

  @Test
  fun testMidStringOrphanFragmentIsRepaired() {
    assertEquals(
      "Dhanyavaad (feat. The Pharaoh)",
      ApiText.clean("Dhanyavaad (feat. amp; The Pharaoh)"),
    )
  }

  @Test
  fun testPlainTextIsUnchanged() {
    assertEquals("Alan Walker", ApiText.clean("Alan Walker"))
    assertEquals("R&B", ApiText.clean("R&B"))
  }

  @Test
  fun testIssueReferenceIsPreserved() {
    assertEquals("Issue #123; please check", ApiText.clean("Issue #123; please check"))
  }

  @Test
  fun testHashNumberFragmentIsPreserved() {
    assertEquals("Episode #5; the finale", ApiText.clean("Episode #5; the finale"))
  }
}
