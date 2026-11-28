@file:JvmName("UriKt")
package androidx.core.net

import android.net.Uri
import java.io.File

// Desktop copies of the androidx.core.net helpers the app uses.
fun String.toUri(): Uri = Uri.parse(this)
fun File.toUri(): Uri = Uri.parse(toURI().toString())
fun Uri.toFile(): File = File(java.net.URI(toString()))
