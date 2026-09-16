package com.example.hooks

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.example.canvas.ViewportState
import com.example.parser.Point2D

/**
 * CAD viewport and coordinate translation hooks.
 */
class CadViewportHook(private val viewport: ViewportState) {

    fun toScreen(cadPoint: Point2D): Offset = viewport.worldToScreen(cadPoint)

    fun toWorld(screenOffset: Offset): Point2D = viewport.screenToWorld(screenOffset)

    val currentScale: Float get() = viewport.scale
}

@Composable
fun rememberCadViewport(viewport: ViewportState): CadViewportHook {
    return remember(viewport) {
        CadViewportHook(viewport)
    }
}
