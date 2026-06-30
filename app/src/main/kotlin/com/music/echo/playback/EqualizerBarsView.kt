package iad1tya.echo.music.playback

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tiny animated equalizer: a row of bars that bounce while music plays.
 * Shown in the collapsed island pill in place of a static icon. Call [start]/[stop].
 */
class EqualizerBarsView
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    View(context, attrs, defStyle) {

    private val barCount = 4
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val rect = RectF()
    private var phase = 0f

    private val animator =
        ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
        }

    fun start() {
        if (!animator.isStarted) animator.start()
    }

    fun stop() {
        animator.cancel()
        phase = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val gap = w / (barCount * 2f)
        val barW = gap
        val minH = h * 0.25f
        for (i in 0 until barCount) {
            val wave = abs(sin(phase + i * 0.9f))
            val barH = if (animator.isRunning) minH + (h - minH) * wave else minH
            val left = gap + i * (barW + gap)
            val top = (h - barH) / 2f
            rect.set(left, top, left + barW, top + barH)
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
