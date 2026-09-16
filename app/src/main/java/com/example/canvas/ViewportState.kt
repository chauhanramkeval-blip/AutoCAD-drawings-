package com.example.canvas

import androidx.compose.ui.geometry.Offset
import com.example.parser.BoundingBox
import com.example.parser.Point2D
import kotlin.math.max
import kotlin.math.min

data class ViewportState(
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val canvasWidth: Float = 1000f,
    val canvasHeight: Float = 1000f
) {
    /**
     * Converts a CAD world point (Y up) to screen coordinates (Y down).
     */
    fun worldToScreen(point: Point2D): Offset {
        val screenX = point.x * scale + offsetX
        // Standard CAD coordinate systems have positive Y upwards
        val screenY = canvasHeight - (point.y * scale + offsetY)
        return Offset(screenX, screenY)
    }

    /**
     * Converts a screen pixel offset (Y down) to CAD world coordinates (Y up).
     */
    fun screenToWorld(screenOffset: Offset): Point2D {
        val worldX = (screenOffset.x - offsetX) / scale
        val worldY = ((canvasHeight - screenOffset.y) - offsetY) / scale
        return Point2D(worldX, worldY)
    }

    /**
     * Computes the new ViewportState to center and scale the CAD drawing to fit the screen.
     */
    fun fitToBounds(
        bbox: BoundingBox,
        viewWidth: Float,
        viewHeight: Float,
        paddingPx: Float = 60f
    ): ViewportState {
        val availW = (viewWidth - paddingPx * 2).coerceAtLeast(10f)
        val availH = (viewHeight - paddingPx * 2).coerceAtLeast(10f)

        val scaleX = availW / bbox.width
        val scaleY = availH / bbox.height
        val newScale = min(scaleX, scaleY).coerceIn(0.001f, 1000f)

        // Center point in CAD world
        val centerCadX = bbox.centerX
        val centerCadY = bbox.centerY

        // We want world center to map to screen center:
        // screenX = centerCadX * newScale + newOffsetX == viewWidth / 2
        val newOffsetX = (viewWidth / 2f) - (centerCadX * newScale)
        // (viewHeight - (centerCadY * newScale + newOffsetY)) == viewHeight / 2
        val newOffsetY = (viewHeight / 2f) - (centerCadY * newScale)

        return copy(
            scale = newScale,
            offsetX = newOffsetX,
            offsetY = newOffsetY,
            canvasWidth = viewWidth,
            canvasHeight = viewHeight
        )
    }

    fun zoom(factor: Float, focus: Offset): ViewportState {
        val clampedFactor = factor.coerceIn(0.2f, 5.0f)
        val newScale = (scale * clampedFactor).coerceIn(0.01f, 500f)
        val actualFactor = newScale / scale

        // Adjust offsets so focus point remains stable in world coordinates
        val newOffsetX = focus.x - (focus.x - offsetX) * actualFactor
        val newOffsetY = (canvasHeight - focus.y) - ((canvasHeight - focus.y) - offsetY) * actualFactor

        return copy(
            scale = newScale,
            offsetX = newOffsetX,
            offsetY = newOffsetY
        )
    }

    fun pan(dx: Float, dy: Float): ViewportState {
        return copy(
            offsetX = offsetX + dx,
            offsetY = offsetY - dy // because dy in screen is inverted relative to CAD Y
        )
    }
}
