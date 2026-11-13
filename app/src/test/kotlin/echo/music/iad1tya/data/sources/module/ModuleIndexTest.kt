package echo.music.iad1tya.data.sources.module

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleIndexTest {

  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  @Test
  fun parseModulesFiltersExcludedAndAggregatesCategories() {
    val jsonPayload =
      """
      {
          "category:music": [
              {
                  "id": "mod-flac",
                  "name": "Hi-Res FLAC Source",
                  "author": "Audiophile",
                  "version": "1.0.0",
                  "tags": ["FLAC", "HI-RES", "LOSSLESS"],
                  "download": "mod-flac.js"
              }
          ],
          "category:community": [
              {
                  "id": "mod-atmos",
                  "name": "Spatial Audio Source",
                  "author": "DolbyFan",
                  "version": "2.1.0",
                  "labels": ["ATMOS", "DOLBY"],
                  "download": "mod-atmos.js"
              }
          ],
          "category:testing": [
              {
                  "id": "mod-test",
                  "name": "Test Module",
                  "download": "test.js"
              }
          ]
      }
      """
        .trimIndent()

    val modules = ModuleIndex.parseModules(json, jsonPayload)
    assertEquals(2, modules.size)

    val flac = modules.first { it.id == "mod-flac" }
    assertTrue(flac.isLossless)
    assertTrue(flac.hasHiRes)
    assertFalse(flac.isDolbyAtmos)

    val atmos = modules.first { it.id == "mod-atmos" }
    assertTrue(atmos.isDolbyAtmos)
    assertEquals(listOf("ATMOS", "DOLBY"), atmos.tags)
  }
}
