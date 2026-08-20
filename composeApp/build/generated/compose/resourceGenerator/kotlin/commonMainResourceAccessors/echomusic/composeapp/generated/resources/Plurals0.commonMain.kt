@file:OptIn(InternalResourceApi::class)

package echomusic.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/echomusic.composeapp.generated.resources/"

@delegate:ResourceContentHash(-391_611_470)
internal val Res.plurals.n_song: PluralStringResource by lazy {
      PluralStringResource("plurals:n_song", "n_song", setOf(
        ResourceItem(setOf(LanguageQualifier("ar"), ), "${MD}values-ar/strings.commonMain.cvr", 10, 148),
        ResourceItem(setOf(LanguageQualifier("az"), ), "${MD}values-az/strings.commonMain.cvr", 10, 50),
        ResourceItem(setOf(LanguageQualifier("bg"), ), "${MD}values-bg/strings.commonMain.cvr", 10, 66),
        ResourceItem(setOf(LanguageQualifier("ca"), ), "${MD}values-ca/strings.commonMain.cvr", 10, 58),
        ResourceItem(setOf(LanguageQualifier("de"), ), "${MD}values-de/strings.commonMain.cvr", 10, 50),
        ResourceItem(setOf(LanguageQualifier("es"), ), "${MD}values-es/strings.commonMain.cvr", 10, 58),
        ResourceItem(setOf(LanguageQualifier("fa"), ), "${MD}values-fa/strings.commonMain.cvr", 10, 58),
        ResourceItem(setOf(LanguageQualifier("fi"), ), "${MD}values-fi/strings.commonMain.cvr", 10, 62),
        ResourceItem(setOf(LanguageQualifier("fr"), ), "${MD}values-fr/strings.commonMain.cvr", 10, 50),
        ResourceItem(setOf(LanguageQualifier("hi"), ), "${MD}values-hi/strings.commonMain.cvr", 10, 66),
        ResourceItem(setOf(LanguageQualifier("id"), ), "${MD}values-id/strings.commonMain.cvr", 10, 33),
        ResourceItem(setOf(LanguageQualifier("in"), ), "${MD}values-in/strings.commonMain.cvr", 10, 33),
        ResourceItem(setOf(LanguageQualifier("it"), ), "${MD}values-it/strings.commonMain.cvr", 10, 58),
        ResourceItem(setOf(LanguageQualifier("iw"), ), "${MD}values-iw/strings.commonMain.cvr", 10, 109),
        ResourceItem(setOf(LanguageQualifier("ja"), ), "${MD}values-ja/strings.commonMain.cvr", 10, 29),
        ResourceItem(setOf(LanguageQualifier("ko"), ), "${MD}values-ko/strings.commonMain.cvr", 10, 37),
        ResourceItem(setOf(LanguageQualifier("pl"), ), "${MD}values-pl/strings.commonMain.cvr", 10, 89),
        ResourceItem(setOf(LanguageQualifier("pt"), ), "${MD}values-pt/strings.commonMain.cvr", 10, 58),
        ResourceItem(setOf(LanguageQualifier("ru"), ), "${MD}values-ru/strings.commonMain.cvr", 10, 117),
        ResourceItem(setOf(LanguageQualifier("th"), ), "${MD}values-th/strings.commonMain.cvr", 10, 41),
        ResourceItem(setOf(LanguageQualifier("tr"), ), "${MD}values-tr/strings.commonMain.cvr", 10, 66),
        ResourceItem(setOf(LanguageQualifier("uk"), ), "${MD}values-uk/strings.commonMain.cvr", 10, 173),
        ResourceItem(setOf(LanguageQualifier("vi"), ), "${MD}values-vi/strings.commonMain.cvr", 10, 37),
        ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("CN"), ), "${MD}values-zh-rCN/strings.commonMain.cvr", 10, 37),
        ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("TW"), ), "${MD}values-zh-rTW/strings.commonMain.cvr", 10, 37),
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 10, 50),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainPlurals0Resources(map: MutableMap<String, PluralStringResource>) {
  map.put("n_song", Res.plurals.n_song)
}
