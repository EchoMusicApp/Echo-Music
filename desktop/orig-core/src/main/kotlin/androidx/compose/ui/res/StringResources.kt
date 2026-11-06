package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import echo.music.iad1tya.RTable

/** Desktop versions of Android's string lookups, backed by the generated table from strings.xml. */
@Composable
fun stringResource(id: Int): String = RTable.strings[id] ?: "string#$id"

@Composable
fun stringResource(id: Int, vararg formatArgs: Any): String = String.format(RTable.strings[id] ?: "string#$id", *formatArgs)

@Composable
fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String {
    val forms = RTable.plurals[id] ?: return "plural#$id"
    val form = forms[if (count == 1) "one" else "other"] ?: forms["other"] ?: forms.values.first()
    return String.format(form, *(if (formatArgs.isEmpty()) arrayOf<Any>(count) else formatArgs))
}
