package com.yalantis.ucrop

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri

// shortcut: desktop has no crop screen; the "crop" copies the picked image unchanged (see StartActivityForResult).
class UCrop private constructor(private val source: Uri, private val dest: Uri) {
    class Options {
        fun setCompressionFormat(f: Bitmap.CompressFormat) {}
        fun setCompressionQuality(q: Int) {}
        fun setHideBottomControls(b: Boolean) {}
        fun setToolbarTitle(t: String) {}
        fun setStatusBarLight(b: Boolean) {}
        fun setToolbarColor(c: Int) {}
        fun setToolbarWidgetColor(c: Int) {}
        fun setRootViewBackgroundColor(c: Int) {}
        fun setLogoColor(c: Int) {}
    }
    fun withAspectRatio(x: Float, y: Float) = this
    fun withOptions(o: Options) = this
    fun getIntent(context: Context): Intent = Intent("ucrop").putExtra("src", source).putExtra("dst", dest)
    companion object {
        fun of(source: Uri, dest: Uri) = UCrop(source, dest)
        fun getOutput(data: Intent): Uri? = data.extras["dst"] as? Uri
    }
}
