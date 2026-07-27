/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.accessibility.debug

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import timber.log.Timber

/**
 * Debug-only floating button that captures the accessibility tree of the screen behind it.
 *
 * Uses `TYPE_ACCESSIBILITY_OVERLAY` like the timer overlay, so it needs no extra
 * permission. Tap dumps immediately; long-press starts a five-second countdown so
 * transient screens (a story, a dialog) can be captured after navigating to them.
 */
internal class InspectorOverlayManager {

    private companion object {
        const val COUNTDOWN_SECONDS = 5
        const val DRAG_SLOP_PX = 12f
        const val LABEL_IDLE = "DUMP"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var service: AccessibilityService? = null
    private var windowManager: WindowManager? = null
    private var buttonView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var dumpCounter = 0
    private var isCountingDown = false

    private var initialX = 0
    private var initialY = 0
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false

    fun show(service: AccessibilityService) {
        if (buttonView != null) return
        this.service = service
        val wm = service.getSystemService(WindowManager::class.java) ?: return
        windowManager = wm

        val density = service.resources.displayMetrics.density
        fun dp(value: Float) = (value * density).toInt()

        val view = TextView(service).apply {
            text = LABEL_IDLE
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            background = GradientDrawable().apply {
                setColor(Color.argb(220, 200, 30, 30))
                cornerRadius = dp(20f).toFloat()
            }
            elevation = dp(8f).toFloat()
        }

        // Top-start; the timer overlay lives top-end, so the two never overlap.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12f)
            y = dp(120f)
        }

        view.setOnTouchListener { _, event -> handleTouch(view, params, event) }

        try {
            wm.addView(view, params)
            buttonView = view
            layoutParams = params
            Timber.i("Accessibility inspector overlay shown")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show inspector overlay")
        }
    }

    private fun handleTouch(view: TextView, params: WindowManager.LayoutParams, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                touchStartX = event.rawX
                touchStartY = event.rawY
                isDragging = false
                view.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MILLIS)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - touchStartX
                val dy = event.rawY - touchStartY
                if (!isDragging && (kotlin.math.abs(dx) > DRAG_SLOP_PX || kotlin.math.abs(dy) > DRAG_SLOP_PX)) {
                    isDragging = true
                    view.removeCallbacks(longPressRunnable)
                }
                if (isDragging) {
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    runCatching { windowManager?.updateViewLayout(view, params) }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.removeCallbacks(longPressRunnable)
                if (!isDragging && event.action == MotionEvent.ACTION_UP) {
                    captureNow(label = "manual")
                }
            }
        }
        return true
    }

    private val longPressRunnable = Runnable { startCountdown() }

    private fun startCountdown() {
        if (isCountingDown) return
        isCountingDown = true
        countdown(COUNTDOWN_SECONDS)
    }

    private fun countdown(remaining: Int) {
        val view = buttonView ?: return
        if (remaining == 0) {
            view.text = LABEL_IDLE
            isCountingDown = false
            captureNow(label = "delayed")
            return
        }
        view.text = remaining.toString()
        mainHandler.postDelayed({ countdown(remaining - 1) }, 1_000L)
    }

    private fun captureNow(label: String) {
        val service = service ?: return
        val view = buttonView

        // Hide our own button so it cannot appear in — or shift — the captured screen.
        view?.visibility = View.INVISIBLE
        mainHandler.post {
            val result = AccessibilityTreeDumper.dump(
                service = service,
                label = "${++dumpCounter}-$label",
                nowMillis = System.currentTimeMillis(),
            )
            view?.visibility = View.VISIBLE
            val message = result.file?.let { "Dumped ${result.nodeCount} nodes → ${it.name}" }
                ?: "Dump failed (${result.nodeCount} nodes)"
            Toast.makeText(service, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun cleanup() {
        mainHandler.removeCallbacksAndMessages(null)
        buttonView?.let { view ->
            view.removeCallbacks(longPressRunnable)
            runCatching { windowManager?.removeView(view) }
        }
        buttonView = null
        layoutParams = null
        windowManager = null
        service = null
    }
}

private const val LONG_PRESS_TIMEOUT_MILLIS = 500L
