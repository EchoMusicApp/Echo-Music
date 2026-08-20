@file:OptIn(InternalResourceApi::class)

package echomusic.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/echomusic.composeapp.generated.resources/"

@delegate:ResourceContentHash(-511_337_128)
internal val Res.font.poppins_bold: FontResource by lazy {
      FontResource("font:poppins_bold", setOf(
        ResourceItem(setOf(), "${MD}font/poppins_bold.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(102_288_014)
internal val Res.font.poppins_medium: FontResource by lazy {
      FontResource("font:poppins_medium", setOf(
        ResourceItem(setOf(), "${MD}font/poppins_medium.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-228_938_793)
internal val Res.font.poppins_regular: FontResource by lazy {
      FontResource("font:poppins_regular", setOf(
        ResourceItem(setOf(), "${MD}font/poppins_regular.ttf", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainFont0Resources(map: MutableMap<String, FontResource>) {
  map.put("poppins_bold", Res.font.poppins_bold)
  map.put("poppins_medium", Res.font.poppins_medium)
  map.put("poppins_regular", Res.font.poppins_regular)
}
