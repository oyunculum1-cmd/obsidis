package com.obsidis.app.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * The orange guidance ring — pulses around the real on-screen coordinates of
 * whatever the accessibility service just located, with the instruction
 * text above it. When ringOnly=false it renders as a text-only toast-style
 * banner (used for status messages like "done" or "add an API key").
 */
class HighlightRingView(
    context: Context,
    private val instruction: String,
    private val ringOnly: Boolean = true
) : View(context) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9800")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }
    private val textBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC000000")
    }

    private var pulse = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 900
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener { pulse = it.animatedValue as Float; invalidate() }
        start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        if (!ringOnly) {
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 24f, 24f, textBg)
            canvas.drawText(instruction, cx, height / 2f + 12f, textPaint)
            return
        }
        val textY = 34f
        canvas.drawRoundRect(cx - width / 2f + 10f, 0f, cx + width / 2f - 10f, 48f, 20f, 20f, textBg)
        canvas.drawText(instruction, cx, textY, textPaint)

        val ringCy = height - (height - 60f) / 2f
        val baseR = (width.coerceAtMost(height) - 60f) / 2.4f
        val r = baseR + pulse * 10f
        ringPaint.alpha = (255 * (1f - pulse * 0.5f)).toInt()
        canvas.drawCircle(cx, ringCy, r, ringPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
