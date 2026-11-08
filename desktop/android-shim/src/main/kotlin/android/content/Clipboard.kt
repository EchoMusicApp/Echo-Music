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
}

class ComponentName(val packageName: String, val className: String) {
    constructor(context: Context, cls: Class<*>) : this(context.packageName, cls.name)
}
