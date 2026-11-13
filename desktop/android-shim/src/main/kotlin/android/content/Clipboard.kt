package android.content

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class ClipData private constructor(val text: CharSequence) {
    companion object {
        fun newPlainText(label: CharSequence?, text: CharSequence) = ClipData(text)
        fun newUri(resolver: ContentResolver?, label: CharSequence?, uri: android.net.Uri) = ClipData(uri.toString())
    }
}

class ClipboardManager {
    fun setPrimaryClip(clip: ClipData) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(clip.text.toString()), null) }
    }
}

// shortcut: no content provider on desktop, so URI permission calls do nothing.
class ContentResolver {
    fun takePersistableUriPermission(uri: android.net.Uri, flags: Int) {}
    // shortcut: only file: URIs (and plain paths) can be opened; content:// URIs do not exist on desktop.
    private fun fileOf(uri: android.net.Uri): java.io.File = if (uri.scheme == "file") java.io.File(java.net.URI(uri.toString())) else java.io.File(uri.toString())
    fun openInputStream(uri: android.net.Uri): java.io.InputStream? = fileOf(uri).takeIf { it.isFile }?.inputStream()
    fun openOutputStream(uri: android.net.Uri): java.io.OutputStream? = fileOf(uri).also { it.parentFile?.mkdirs() }.outputStream()
    fun openOutputStream(uri: android.net.Uri, mode: String): java.io.OutputStream? = openOutputStream(uri)
}

class ComponentName(val packageName: String, val className: String) {
    constructor(context: Context, cls: Class<*>) : this(context.packageName, cls.name)
}
