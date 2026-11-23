package com.rstube.app

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils

/** Native clay surface: diffuse opposing shadows, a soft face and an inset rim. */
class ClayDrawable(
    private val density: Float,
    private val base: Int,
    private val radiusDp: Float = 28f,
    private val compact: Boolean = false,
    private val sunken: Boolean = false
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var drawableAlpha = 255
    private var filter: ColorFilter? = null
    private var pressed = false

    private fun px(value: Float) = value * density
    private fun mix(color: Int, weight: Float) = ColorUtils.blendARGB(base, color, weight)

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val inset = px(if (compact) 6f else 11f)
        val shift = if (pressed) px(1.5f) else 0f
        val face = RectF(bounds.left + inset, bounds.top + inset + shift,
            bounds.right - inset, bounds.bottom - inset + shift)
        if (face.width() <= 0 || face.height() <= 0) return
        val corner = px(radiusDp)
        val blur = px(if (compact) 5f else 9f)
        val offset = px(if (compact) 2.5f else 4f) * if (pressed) 0.45f else 1f

        paint.reset()
        paint.isAntiAlias = true
        paint.color = base
        paint.alpha = drawableAlpha
        paint.colorFilter = filter
        paint.setShadowLayer(blur, -offset, -offset, Color.argb(175, 255, 255, 226))
        canvas.drawRoundRect(face, corner, corner, paint)
        paint.setShadowLayer(blur, offset, offset, Color.argb(78, 72, 82, 25))
        canvas.drawRoundRect(face, corner, corner, paint)
        paint.clearShadowLayer()

        val recessed = sunken || pressed
        paint.shader = LinearGradient(face.left, face.top, face.right, face.bottom,
            if (recessed) mix(Color.BLACK, 0.08f) else mix(Color.WHITE, 0.15f),
            if (recessed) mix(Color.WHITE, 0.22f) else mix(Color.BLACK, 0.05f),
            Shader.TileMode.CLAMP)
        canvas.drawRoundRect(face, corner, corner, paint)

        val rim = RectF(face).apply { inset(px(1f), px(1f)) }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = px(1.5f)
        paint.shader = LinearGradient(rim.left, rim.top, rim.right, rim.bottom,
            if (recessed) Color.argb(55, 76, 82, 25) else Color.argb(165, 255, 255, 243),
            if (recessed) Color.argb(150, 255, 255, 235) else Color.argb(35, 62, 71, 20),
            Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rim, corner, corner, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    override fun isStateful() = true
    override fun onStateChange(state: IntArray): Boolean {
        val next = state.contains(android.R.attr.state_pressed)
        if (pressed == next) return false
        pressed = next
        invalidateSelf()
        return true
    }
    override fun setAlpha(alpha: Int) { drawableAlpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { filter = colorFilter; invalidateSelf() }
    @Suppress("DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
