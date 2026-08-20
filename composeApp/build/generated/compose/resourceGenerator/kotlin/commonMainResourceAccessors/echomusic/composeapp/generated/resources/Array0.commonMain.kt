@file:OptIn(InternalResourceApi::class)

package echomusic.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringArrayResource

private const val MD: String = "composeResources/echomusic.composeapp.generated.resources/"

@delegate:ResourceContentHash(502_362_606)
internal val Res.array.region_code_array: StringArrayResource by lazy {
      StringArrayResource("string-array:region_code_array", "region_code_array", setOf(
        ResourceItem(setOf(), "${MD}values/arrays.commonMain.cvr", 10, 340),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainArray0Resources(map: MutableMap<String, StringArrayResource>) {
  map.put("region_code_array", Res.array.region_code_array)
}
