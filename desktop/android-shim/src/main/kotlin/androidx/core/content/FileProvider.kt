package androidx.core.content

import android.content.Context
import android.net.Uri
import java.io.File

object FileProvider {
    // shortcut: no content:// indirection on desktop; the "provider" URI is just the file's own URI.
    fun getUriForFile(context: Context, authority: String, file: File): Uri = Uri.parse(file.toURI().toString())
}
