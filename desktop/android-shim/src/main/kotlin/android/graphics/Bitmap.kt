package android.graphics

import java.awt.image.BufferedImage

// shortcut: only what lyric-sharing and cover cropping need (a pixel buffer that can be written as an image file).
class Bitmap(val image: BufferedImage) {
    val width get() = image.width
    val height get() = image.height
    enum class CompressFormat { JPEG, PNG, WEBP }
}
