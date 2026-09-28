package com.obsidis.app.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import android.animation.ValueAnimator
import kotlin.random.Random

/**
 * The fixed "dynamic island" pill at the top of the screen: a live eye on
 * the left, a keyboard glyph on the right. It never leaves the top of the
 * screen and isn't draggable — tapping either half is the only interaction.
 * The eye blinks on its own on a randomized loop so the pill reads as
 * alive, not static chrome.
 */
class IslandView(context: Context) : View(context) {

    private val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0B0B") }
    private val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2A2A"); style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val iris = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7F77DD") }
    private val pupil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0B0B") }
    private val glint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 210 }
    private val eyelid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0B0B") }
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CFCFCF"); style = Paint.Style.STROKE; strokeWidth = 2.4f }

    /** 1f = fully open, 0f = fully shut. */
    private var openness = 1f
    private val blinkAnimator = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
        duration = 160
        interpolator = LinearInterpolator()
        addUpdateListener { openness = it.animatedValue as Float; invalidate() }
    }

    private val idleLoop = object : Runnable {
        override fun run() {
            if (!blinkAnimator.isRunning) blinkAnimator.start()
            postDelayed(this, Random.nextLong(1800, 4200))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postDelayed(idleLoop, Random.nextLong(800, 2000))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(idleLoop)
        blinkAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val rect = RectF(2f, 2f, w - 2f, h - 2f)
        val radius = h / 2f
        canvas.drawRoundRect(rect, radius, radius, pillBg)
        canvas.drawRoundRect(rect, radius, radius, pillBorder)

        // Eye, left half
        val eyeCx = h; val eyeCy = h / 2f; val eyeR = h * 0.34f
        canvas.save()
        canvas.scale(1f, openness.coerceIn(0.08f, 1f), eyeCx, eyeCy)
        canvas.drawCircle(eyeCx, eyeCy, eyeR, iris)
        canvas.drawCircle(eyeCx, eyeCy, eyeR * 0.42f, pupil)
        canvas.drawCircle(eyeCx - eyeR * 0.16f, eyeCy - eyeR * 0.2f, eyeR * 0.14f, glint)
        canvas.restore()

        // Keyboard glyph, right half
        val kx = w - h * 1.05f; val ky = h * 0.28f
        val kw = h * 0.66f; val kh = h * 0.44f
        val kRect = RectF(kx, ky, kx + kw, ky + kh)
        canvas.drawRoundRect(kRect, 5f, 5f, keyPaint)
        val dotR = 1.8f
        val rows = 2; val cols = 4
        for (r in 0 until rows) for (c in 0 until cols) {
            val px = kx + kw * (c + 0.5f) / cols
            val py = ky + kh * (r + 0.5f) / rows
            canvas.drawCircle(px, py, dotR, keyPaint)
        }
    }
}
