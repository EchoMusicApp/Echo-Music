package android.content

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class ClipData private constructor(val text: CharSequence) {
    companion object { fun newPlainText(label: CharSequence?, text: CharSequence) = ClipData(text) }
}

class ClipboardManager {
    fun setPrimaryClip(clip: ClipData) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(clip.text.toString()), null) }
    }
}

class ComponentName(val packageName: String, val className: String) {
    constructor(context: Context, cls: Class<*>) : this(context.packageName, cls.name)
}
