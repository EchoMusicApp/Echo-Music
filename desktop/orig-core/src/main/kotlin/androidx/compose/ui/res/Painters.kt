package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import echo.music.iad1tya.RTable
import org.xml.sax.InputSource

// Desktop versions of Android's painterResource / vectorResource, reading the drawable files from the classpath (see tools/gen_res.py).
// shortcut: Android-only vector features (theme tints like ?attr/..., gradients via aapt:attr) may fail to load; fix per icon when it shows up.
private fun stream(id: Int) = Thread.currentThread().contextClassLoader
    .getResourceAsStream("drawable/" + (RTable.drawables[id] ?: error("no drawable with id $id")))
    ?: error("drawable file missing for id $id")

@Composable
fun ImageVector.Companion.vectorResource(id: Int): ImageVector {
    val density = LocalDensity.current
    return remember(id, density) { stream(id).use { loadXmlImageVector(InputSource(it), density) } }
}

@Composable
fun painterResource(id: Int): Painter {
    val name = RTable.drawables[id] ?: error("no drawable with id $id")
    return if (name.endsWith(".xml")) rememberVectorPainter(ImageVector.vectorResource(id))
    else remember(id) { BitmapPainter(stream(id).use { loadImageBitmap(it) }) }
}
