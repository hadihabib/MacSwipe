package com.macswipe.remote

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.os.Handler
import android.os.Looper
import kotlin.math.abs
import kotlin.math.hypot

/** 1 finger = cursor/tap/long-press-drag; 2 = scroll/right-click; 3 = Spaces/Mission Control. */
class TrackpadView(context: Context, private val send: (String, Double, Double) -> Unit) : View(context) {
    private val ui = Handler(Looper.getMainLooper())
    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val density = resources.displayMetrics.density
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(39, 43, 64); strokeWidth = density }
    private val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(150, 128, 250); style = Paint.Style.STROKE; strokeWidth = 2.3f * density }
    private val centerText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(105, 107, 137); textSize = 15f * density; textAlign = Paint.Align.CENTER }
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(139, 110, 252) }

    private var startTime = 0L
    private var maxFingers = 0
    private var gestureHandled = false
    private var dragged = false
    private var moved = false
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var touchX = -1f
    private var touchY = -1f
    private var longPress: Runnable? = null
    private var lastMoveSent = 0L

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val interval = 27 * density
        var x = interval
        while (x < width) {
            var y = interval
            while (y < height) {
                canvas.drawCircle(x, y, density, grid)
                y += interval
            }
            x += interval
        }
        canvas.drawText("TOUCH SURFACE", width / 2f, height / 2f - 10f * density, centerText)
        centerText.textSize = 12f * density
        canvas.drawText("1 finger move    •    2 scroll    •    3 swipe", width / 2f, height / 2f + 17f * density, centerText)
        centerText.textSize = 15f * density
        if (touchX >= 0 && touchY >= 0) {
            canvas.drawCircle(touchX, touchY, 20 * density, circle)
            canvas.drawCircle(touchX, touchY, 3 * density, accent)
        }
    }

    private fun centroid(e: MotionEvent): Pair<Float, Float> {
        var x = 0f; var y = 0f
        for (i in 0 until e.pointerCount) { x += e.getX(i); y += e.getY(i) }
        return x / e.pointerCount to y / e.pointerCount
    }

    private fun cancelLongPress() {
        longPress?.let { ui.removeCallbacks(it) }
        longPress = null
    }

    private fun scheduleLongPress() {
        cancelLongPress()
        val task = Runnable {
            if (maxFingers == 1 && !moved && !dragged) {
                dragged = true
                send("drag_start", 0.0, 0.0)
            }
        }
        longPress = task
        ui.postDelayed(task, 430)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val (cx, cy) = centroid(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startTime = System.currentTimeMillis()
                maxFingers = 1
                gestureHandled = false
                moved = false
                dragged = false
                startX = cx; startY = cy
                lastX = cx; lastY = cy
                touchX = cx; touchY = cy
                scheduleLongPress()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                cancelLongPress()
                maxFingers = maxOf(maxFingers, event.pointerCount)
                startX = cx; startY = cy
                lastX = cx; lastY = cy
                moved = false
                touchX = cx; touchY = cy
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = cx; touchY = cy
                if (event.pointerCount == maxFingers && !gestureHandled) {
                    val dx = cx - lastX
                    val dy = cy - lastY
                    val totalX = cx - startX
                    val totalY = cy - startY
                    when (maxFingers) {
                        1 -> {
                            if (hypot(totalX, totalY) > slop) { moved = true; cancelLongPress() }
                            // Cap pointer events to roughly 60 Hz to avoid queue accumulation.
                            val now = System.currentTimeMillis()
                            if (now - lastMoveSent >= 14 && (abs(dx) > 0.25f || abs(dy) > 0.25f)) {
                                send("move", (dx * 1.45).toDouble(), (dy * 1.45).toDouble())
                                lastMoveSent = now
                                lastX = cx; lastY = cy
                            }
                        }
                        2 -> {
                            if (hypot(totalX, totalY) > slop) moved = true
                            if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
                                send("scroll", (dx * 0.85).toDouble(), (dy * 0.85).toDouble())
                            }
                            lastX = cx; lastY = cy
                        }
                        3 -> {
                            if (hypot(totalX, totalY) > 66 * density) {
                                gestureHandled = true
                                val type = if (abs(totalX) > abs(totalY)) {
                                    if (totalX < 0) "space_right" else "space_left"
                                } else {
                                    if (totalY < 0) "mission_control" else "app_expose"
                                }
                                send(type, 0.0, 0.0)
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // Retain the highest finger count and ignore remaining fingers until release.
                cancelLongPress()
            }
            MotionEvent.ACTION_UP -> {
                cancelLongPress()
                if (dragged) {
                    send("drag_end", 0.0, 0.0)
                } else if (System.currentTimeMillis() - startTime < 340 && !gestureHandled) {
                    if (maxFingers == 1 && !moved && hypot(cx - startX, cy - startY) < 2 * slop) {
                        send("click", 0.0, 0.0)
                        performClick()
                    } else if (maxFingers == 2 && !moved) {
                        send("right_click", 0.0, 0.0)
                    }
                }
                touchX = -1f; touchY = -1f
                maxFingers = 0
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
                if (dragged) send("drag_end", 0.0, 0.0)
                maxFingers = 0
                touchX = -1f; touchY = -1f
            }
        }
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
