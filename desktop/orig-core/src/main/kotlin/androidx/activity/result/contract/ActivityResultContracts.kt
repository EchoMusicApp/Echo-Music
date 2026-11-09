package androidx.activity.result.contract

import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

abstract class ActivityResultContract<I, O> {
    abstract fun run(input: I): O
}

// shortcut: plain AWT/Swing file dialogs; MIME filters are ignored (the app checks file types itself).
private fun openFile(): Uri? = FileDialog(null as Frame?, "Open", FileDialog.LOAD).apply { isVisible = true }
    .let { d -> d.file?.let { Uri.parse(File(d.directory, it).toURI().toString()) } }

object ActivityResultContracts {
    class OpenDocument : ActivityResultContract<Array<String>, Uri?>() { override fun run(input: Array<String>) = openFile() }
    class GetContent : ActivityResultContract<String, Uri?>() { override fun run(input: String) = openFile() }
    class CreateDocument(mimeType: String = "*/*") : ActivityResultContract<String, Uri?>() {
        override fun run(input: String): Uri? = FileDialog(null as Frame?, "Save", FileDialog.SAVE).apply { file = input; isVisible = true }
            .let { d -> d.file?.let { Uri.parse(File(d.directory, it).toURI().toString()) } }
    }
    class OpenDocumentTree : ActivityResultContract<Uri?, Uri?>() {
        override fun run(input: Uri?): Uri? = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
            .let { if (it.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) Uri.parse(it.selectedFile.toURI().toString()) else null }
    }
    class PickVisualMedia : ActivityResultContract<PickVisualMediaRequest, Uri?>() {
        override fun run(input: PickVisualMediaRequest) = openFile()
        object ImageOnly
        object VideoOnly
        object ImageAndVideo
    }
    // desktop has no runtime permissions
    class RequestPermission : ActivityResultContract<String, Boolean>() { override fun run(input: String) = true }
    class RequestMultiplePermissions : ActivityResultContract<Array<String>, Map<String, Boolean>>() { override fun run(input: Array<String>) = input.associateWith { true } }
    class StartActivityForResult : ActivityResultContract<android.content.Intent, ActivityResult>() { override fun run(input: android.content.Intent): ActivityResult {
        // the only intent the app launches is the cover "crop": copy the picked image to the destination unchanged
        val src = input.extras["src"] as? Uri
        val dst = input.extras["dst"] as? Uri
        if (src == null || dst == null) return ActivityResult(0, null)
        return runCatching {
            File(java.net.URI(src.toString())).copyTo(File(java.net.URI(dst.toString())), overwrite = true)
            ActivityResult(-1, android.content.Intent("ucrop").putExtra("dst", dst))
        }.getOrDefault(ActivityResult(0, null))
    } }
}
