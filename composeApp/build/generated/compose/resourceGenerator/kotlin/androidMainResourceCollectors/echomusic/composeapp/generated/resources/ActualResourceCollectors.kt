@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package echomusic.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.Map
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource

internal actual val Res.allDrawableResources: Map<String, DrawableResource> by lazy {
  val map = mutableMapOf<String, DrawableResource>()
  _collectCommonMainDrawable0Resources(map)
  return@lazy map
}

internal actual val Res.allStringResources: Map<String, StringResource> by lazy {
  val map = mutableMapOf<String, StringResource>()
  _collectCommonMainString0Resources(map)
  _collectCommonMainString1Resources(map)
  _collectCommonMainString2Resources(map)
  _collectCommonMainString3Resources(map)
  _collectCommonMainString4Resources(map)
  _collectCommonMainString5Resources(map)
  _collectCommonMainString6Resources(map)
  return@lazy map
}

internal actual val Res.allStringArrayResources: Map<String, StringArrayResource> by lazy {
  val map = mutableMapOf<String, StringArrayResource>()
  _collectCommonMainArray0Resources(map)
  return@lazy map
}

internal actual val Res.allPluralStringResources: Map<String, PluralStringResource> by lazy {
  val map = mutableMapOf<String, PluralStringResource>()
  _collectCommonMainPlurals0Resources(map)
  return@lazy map
}

internal actual val Res.allFontResources: Map<String, FontResource> by lazy {
  val map = mutableMapOf<String, FontResource>()
  _collectCommonMainFont0Resources(map)
  return@lazy map
}
