/*
 * Desktop (Skia) version of the Android-only file with the same name in the original app.
 * Original: Kyant0/backdrop v2.0.0, Apache License 2.0. Android RuntimeShader/RenderEffect -> Skia equivalents.
 */
package echo.music.iad1tya.ui.component.backdrop

import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

fun RuntimeShader(shaderString: String): RuntimeShader = SkiaRuntimeShader(RuntimeShaderBuilder(RuntimeEffect.makeForShader(shaderString)))

fun RuntimeShader.asSkiaShader(): org.jetbrains.skia.Shader = asSkiaBuilder().makeShader()

fun RuntimeShader.asSkiaBuilder(): RuntimeShaderBuilder = (this as SkiaRuntimeShader).builder

internal class SkiaRuntimeShader(val builder: RuntimeShaderBuilder) : RuntimeShader {
    override fun setFloatUniform(name: String, value: Float) = builder.uniform(name, value)
    override fun setFloatUniform(name: String, value1: Float, value2: Float) = builder.uniform(name, value1, value2)
    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) = builder.uniform(name, value1, value2, value3)
    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float) =
        builder.uniform(name, value1, value2, value3, value4)
    override fun setFloatUniform(name: String, values: FloatArray) = builder.uniform(name, values)
    override fun setIntUniform(name: String, value: Int) = builder.uniform(name, value)
    override fun setIntUniform(name: String, value1: Int, value2: Int) = builder.uniform(name, value1.toFloat(), value2.toFloat())
    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int) = builder.uniform(name, value1.toFloat(), value2.toFloat(), value3.toFloat())
    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int) =
        builder.uniform(name, value1.toFloat(), value2.toFloat(), value3.toFloat(), value4.toFloat())
    // shortcut: multi-int uniforms are passed as floats (skiko lacks them); nothing in the app uses them
    override fun setIntUniform(name: String, values: IntArray) = builder.uniform(name, FloatArray(values.size) { values[it].toFloat() })
    override fun setColorUniform(name: String, color: Color) = builder.uniform(name, Color4f(color.red, color.green, color.blue, color.alpha))
}

interface RuntimeShader {
    fun setFloatUniform(name: String, value: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float)
    fun setFloatUniform(name: String, values: FloatArray)
    fun setIntUniform(name: String, value: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int)
    fun setIntUniform(name: String, values: IntArray)
    fun setColorUniform(name: String, color: Color)
}
