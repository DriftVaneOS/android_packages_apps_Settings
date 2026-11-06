/*
 * SPDX-FileCopyrightText: DriftVaneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.driftvane

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * DriftVaneOS easter egg (tap the DriftVaneOS version in About phone a few times):
 * the DriftVane mark with its drift lines waving. Tap to shift the colors, hold for a surprise.
 */
class DriftVaneEggActivity : Activity() {
    private lateinit var view: EggView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = EggView(this)
        setContentView(view)
    }

    override fun onResume() {
        super.onResume()
        view.start()
    }

    override fun onPause() {
        view.stop()
        super.onPause()
    }
}

private class EggView(context: Context) : View(context) {
    private var phase = 0f
    private var hue = 215f // DriftVane blue
    private var party = false
    private var taps = 0
    private val anim = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
        duration = 2600L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            if (party) hue = (hue + 1.5f) % 360f
            invalidate()
        }
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val path = Path()

    init {
        isHapticFeedbackEnabled = true
        setOnLongClickListener {
            party = !party
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            true
        }
    }

    fun start() { if (!anim.isStarted) anim.start() }
    fun stop() = anim.cancel()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && event.eventTime - event.downTime < 400) {
            hue = (hue + 47f) % 360f
            taps++
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            invalidate()
        }
        return super.onTouchEvent(event) || true
    }

    private fun col(shift: Float, alpha: Float): Int =
        Color.HSVToColor((alpha * 255).toInt(), floatArrayOf((hue + shift + 360f) % 360f, 0.62f, 1f))

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w, h * 0.55f) * 0.78f // logo width
        val cx = w / 2f
        val cy = h * 0.42f
        val left = cx - s / 2f
        val amp = s * 0.07f

        // three drift lines, waving, rising to the right
        val waves = listOf(
            Triple(0.16f, 0.090f, 1.0f),
            Triple(0.32f, 0.066f, 0.65f),
            Triple(0.48f, 0.050f, 0.40f),
        )
        var tipX = 0f
        var tipY = 0f
        var tipAngle = 0f
        waves.forEachIndexed { i, (yOff, width, alpha) ->
            path.reset()
            val n = 48
            val len = if (i == 0) 0.82f else 0.78f - i * 0.02f
            var px = 0f
            var py = 0f
            for (k in 0..n) {
                val t = k / n.toFloat()
                val x = left + s * (0.04f + t * len)
                val rise = s * 0.30f * t * t
                val y = cy - s * 0.10f + s * yOff - rise +
                    amp * sin((t * 2.2f * PI).toFloat() + phase - i * 0.6f)
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                if (k == n - 1) { px = x; py = y }
                if (k == n && i == 0) {
                    tipX = x; tipY = y; tipAngle = atan2(y - py, x - px)
                }
            }
            line.strokeWidth = s * width
            line.color = col(i * 18f, alpha)
            canvas.drawPath(path, line)
        }

        // the vane: an arrow head on the top line
        val a = s * 0.13f
        fill.color = col(0f, 1f)
        path.reset()
        path.moveTo(tipX + a * cos(tipAngle), tipY + a * sin(tipAngle))
        path.lineTo(tipX + a * 0.55f * cos(tipAngle + 2.2f), tipY + a * 0.55f * sin(tipAngle + 2.2f))
        path.lineTo(tipX + a * 0.55f * cos(tipAngle - 2.2f), tipY + a * 0.55f * sin(tipAngle - 2.2f))
        path.close()
        canvas.drawPath(path, fill)

        title.textSize = s * 0.15f
        canvas.drawText("DriftVaneOS", cx, cy + s * 0.62f, title)
        sub.textSize = s * 0.05f
        canvas.drawText("Made by Alex Desrosier", cx, cy + s * 0.74f, sub)
        sub.textSize = s * 0.04f
        canvas.drawText(
            when {
                party -> "party mode. hold to stop"
                taps >= 10 -> "ok you really like tapping"
                else -> "tap for colors, hold for a surprise"
            },
            cx, h - s * 0.18f, sub,
        )
    }
}
