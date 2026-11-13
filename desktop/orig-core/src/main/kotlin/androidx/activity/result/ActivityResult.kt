package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContract
import javax.swing.SwingUtilities

class ActivityResultLauncher<I>(private val contract: ActivityResultContract<I, *>, private val deliver: (Any?) -> Unit) {
    // Contracts open a native file dialog (or answer immediately); the result is delivered on a worker thread.
    fun launch(input: I) {
        Thread {
            @Suppress("UNCHECKED_CAST")
            val out = runCatching { (contract as ActivityResultContract<I, Any?>).run(input) }.getOrNull()
            deliver(out)
        }.start()
    }
}

class PickVisualMediaRequest(val mediaType: Any? = null) {
    class Builder { fun setMediaType(t: Any?) = this; fun build() = PickVisualMediaRequest() }
}

class ActivityResult(val resultCode: Int, val data: android.content.Intent?)
