package echo.music.iad1tya.data.sources.module

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Parses a raw module-index JSON body into a flat list of [SpineModule]s. */
object ModuleIndex {

  private val excludedCategories = setOf("category:artworks", "category:testing")
  private val listSerializer = ListSerializer(SpineModule.serializer())

  fun parseModules(json: Json, body: String): List<SpineModule> {
    val obj = json.decodeFromString(JsonObject.serializer(), body)
    return obj.entries
      .filter { it.key.startsWith("category:") && it.key !in excludedCategories }
      .flatMap { (_, value) ->
        runCatching { json.decodeFromJsonElement(listSerializer, value) }.getOrElse { emptyList() }
      }
      .distinctBy { it.id }
  }
}
