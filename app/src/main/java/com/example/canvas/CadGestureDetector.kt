package com.example.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlin.math.hypot

/**
 * High-performance multi-touch gesture detector for CAD canvas with Marquee Selection support.
 * 
 * Features:
 * 1. Seamless 2-finger pinch-to-zoom centered on dynamic focal centroid.
 * 2. 2-finger panning in any tool mode without triggering accidental single taps.
 * 3. 1-finger panning in PAN_ZOOM mode.
 * 4. 1-finger rectangular marquee box dragging in MARQUEE_SELECT mode with real-time feedback.
 * 5. Crisp tap selection & double-tap to zoom without pointer consumption conflicts.
 */
suspend fun detectCadMultiTouchGestures(
    pointerScope: PointerInputScope,
    allowSingleFingerPan: Boolean,
    isMarqueeMode: Boolean,
    onTransform: (panX: Float, panY: Float, zoomFactor: Float, centroid: Offset) -> Unit,
    onTap: (Offset) -> Unit,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onPointerMove: ((Offset) -> Unit)? = null,
    onMarqueeUpdate: ((start: Offset, current: Offset) -> Unit)? = null,
    onMarqueeEnd: ((start: Offset, end: Offset) -> Unit)? = null,
    onMarqueeCancel: (() -> Unit)? = null
) {
    var lastTapTime = 0L
    var lastTapOffset = Offset.Zero
    val doubleTapTimeout = 320L
    val doubleTapSlop = 48f

    pointerScope.awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downTime = System.currentTimeMillis()
        val initialDownOffset = down.position
        onPointerMove?.invoke(initialDownOffset)

        var previousCentroid = down.position
        var previousSpan = 0f
        var pastTouchSlop = false
        var isMultiTouch = false
        val touchSlop = viewConfiguration.touchSlop

        while (true) {
            val event = awaitPointerEvent()
            val activePointers = event.changes.filter { it.pressed }

            if (activePointers.isEmpty()) {
                // All fingers lifted
                break
            }

            if (activePointers.size >= 2) {
                if (isMarqueeMode && pastTouchSlop) {
                    onMarqueeCancel?.invoke()
                }
                isMultiTouch = true
                pastTouchSlop = true

                // Calculate centroid of the multi-touch pointers
                var sumX = 0f
                var sumY = 0f
                for (p in activePointers) {
                    sumX += p.position.x
                    sumY += p.position.y
                }
                val centroid = Offset(sumX / activePointers.size, sumY / activePointers.size)

                // Calculate average distance from centroid (span)
                var sumSpan = 0f
                for (p in activePointers) {
                    val dx = p.position.x - centroid.x
                    val dy = p.position.y - centroid.y
                    sumSpan += hypot(dx, dy)
                }
                val span = sumSpan / activePointers.size

                if (previousSpan > 0f && span > 0f) {
                    val zoomDelta = (span / previousSpan).coerceIn(0.2f, 5.0f)
                    val panDelta = centroid - previousCentroid

                    if (zoomDelta != 1f || panDelta != Offset.Zero) {
                        onTransform(panDelta.x, panDelta.y, zoomDelta, centroid)
                        activePointers.forEach { it.consume() }
                    }
                }

                previousCentroid = centroid
                previousSpan = span
            } else if (activePointers.size == 1) {
                val pointer = activePointers.first()
                onPointerMove?.invoke(pointer.position)

                if (isMultiTouch) {
                    // Transitioning from multi-touch back to single touch: reset baseline
                    previousCentroid = pointer.position
                    previousSpan = 0f
                } else if (isMarqueeMode) {
                    // Marquee Box Selection dragging
                    val totalDrag = (pointer.position - initialDownOffset).getDistance()
                    if (!pastTouchSlop) {
                        if (totalDrag > touchSlop) {
                            pastTouchSlop = true
                        }
                    }

                    if (pastTouchSlop) {
                        onMarqueeUpdate?.invoke(initialDownOffset, pointer.position)
                        pointer.consume()
                    }
                    previousCentroid = pointer.position
                } else if (allowSingleFingerPan) {
                    val totalDrag = (pointer.position - initialDownOffset).getDistance()
                    if (!pastTouchSlop) {
                        if (totalDrag > touchSlop) {
                            pastTouchSlop = true
                        }
                    }

                    if (pastTouchSlop) {
                        val panDelta = pointer.position - previousCentroid
                        if (panDelta != Offset.Zero) {
                            onTransform(panDelta.x, panDelta.y, 1.0f, pointer.position)
                            pointer.consume()
                        }
                    }
                    previousCentroid = pointer.position
                } else {
                    // In SELECT / MEASURE tool mode, check touch slop for tap
                    val totalDrag = (pointer.position - initialDownOffset).getDistance()
                    if (totalDrag > touchSlop) {
                        pastTouchSlop = true
                    }
                    previousCentroid = pointer.position
                }
            }
        }

        // Gesture finished (fingers lifted)
        val upTime = System.currentTimeMillis()
        val duration = upTime - downTime

        if (isMarqueeMode && pastTouchSlop && !isMultiTouch) {
            // Completed marquee selection drag
            onMarqueeEnd?.invoke(initialDownOffset, previousCentroid)
        } else if (!pastTouchSlop && !isMultiTouch && duration < 500) {
            val isDoubleTap = onDoubleTap != null &&
                    (upTime - lastTapTime < doubleTapTimeout) &&
                    ((initialDownOffset - lastTapOffset).getDistance() < doubleTapSlop)

            if (isDoubleTap) {
                onDoubleTap?.invoke(initialDownOffset)
                lastTapTime = 0L
                lastTapOffset = Offset.Zero
            } else {
                onTap(initialDownOffset)
                lastTapTime = upTime
                lastTapOffset = initialDownOffset
            }
        }
    }
}
