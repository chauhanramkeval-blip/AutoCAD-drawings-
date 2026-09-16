package com.example.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.example.measurement.MeasureToolType
import com.example.measurement.SnapMode
import com.example.measurement.SnapResult
import com.example.parser.*
import com.example.store.CadTool
import com.example.store.CadUiState
import kotlin.math.*

@Composable
fun CadCanvas(
    uiState: CadUiState,
    onTransform: (panX: Float, panY: Float, zoom: Float, centroid: Offset) -> Unit,
    onTap: (Offset) -> Unit,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onPointerHover: ((Offset) -> Unit)? = null,
    onMarqueeDragUpdate: ((start: Offset, current: Offset) -> Unit)? = null,
    onMarqueeDragEnd: ((start: Offset, end: Offset) -> Unit)? = null,
    onMarqueeCancel: (() -> Unit)? = null,
    onSizeChanged: (width: Float, height: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val vp = uiState.viewport
    val bgColor = if (uiState.isBlueprintTheme) Color(0xFF0A192F) else Color(0xFF1E1E1E)
    val selectedSet = remember(uiState.selectedEntities) { uiState.selectedEntities.toHashSet() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    onSizeChanged(size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(uiState.activeTool) {
                detectCadMultiTouchGestures(
                    pointerScope = this,
                    allowSingleFingerPan = uiState.activeTool == CadTool.PAN_ZOOM,
                    isMarqueeMode = uiState.activeTool == CadTool.MARQUEE_SELECT,
                    onTransform = onTransform,
                    onTap = onTap,
                    onDoubleTap = onDoubleTap,
                    onPointerMove = onPointerHover,
                    onMarqueeUpdate = onMarqueeDragUpdate,
                    onMarqueeEnd = onMarqueeDragEnd,
                    onMarqueeCancel = onMarqueeCancel
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Grid
            if (uiState.showGrid) {
                drawCadGrid(vp, uiState.isBlueprintTheme)
            }

            // 2. Axes
            if (uiState.showAxes) {
                drawCadAxes(vp)
            }

            // 3. Render Entities with Viewport Culling
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = (12f * density).coerceAtLeast(10f)
                typeface = Typeface.MONOSPACE
                isAntiAlias = true
            }

            // Compute visible viewport world bounding box with a 40px buffer
            val pTopLeft = vp.screenToWorld(Offset(-40f, -40f))
            val pBottomRight = vp.screenToWorld(Offset(size.width + 40f, size.height + 40f))
            val minVpX = min(pTopLeft.x, pBottomRight.x)
            val maxVpX = max(pTopLeft.x, pBottomRight.x)
            val minVpY = min(pTopLeft.y, pBottomRight.y)
            val maxVpY = max(pTopLeft.y, pBottomRight.y)

            for (entity in uiState.document.entities) {
                if (uiState.layerVisibility[entity.layer] == false) continue

                val box = entity.getBoundingBox()
                if (box.maxX < minVpX || box.minX > maxVpX || box.maxY < minVpY || box.minY > maxVpY) {
                    continue
                }

                val isSelected = selectedSet.contains(entity)
                val layerColor = uiState.document.layers[entity.layer]?.color ?: Color(0xFF00E5FF)
                val entityColor = if (isSelected) {
                    Color(0xFFFFD600)
                } else {
                    entity.color ?: layerColor
                }
                val strokeWidth = if (isSelected) 3.5f else 1.5f

                drawEntity(entity, vp, entityColor, strokeWidth, textPaint)
            }

            // 4. Render Saved Persistent Measurements
            drawSavedMeasurements(uiState, vp, density)

            // 5. Render Active Measurement Visuals & Live Previews
            drawActiveMeasurementVisuals(uiState, vp, density)

            // 6. Render OSNAP Visual Marker & Tracking Guidelines
            val snap = uiState.activeMeasurement.activeSnap
            if (snap != null && uiState.snapSettings.isEnabled) {
                drawSnapMarker(snap, vp, density)
            }

            // 7. Marquee Selection Box
            val marquee = uiState.activeMarquee
            if (marquee != null) {
                val left = min(marquee.startScreen.x, marquee.currentScreen.x)
                val top = min(marquee.startScreen.y, marquee.currentScreen.y)
                val width = abs(marquee.currentScreen.x - marquee.startScreen.x)
                val height = abs(marquee.currentScreen.y - marquee.startScreen.y)

                if (width > 2f && height > 2f) {
                    val isWindow = marquee.currentScreen.x >= marquee.startScreen.x
                    val fillColor = if (isWindow) Color(0x3300E5FF) else Color(0x3369F0AE)

                    drawRect(
                        color = fillColor,
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )

                    drawRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                        )
                    )

                    val p1 = vp.screenToWorld(marquee.startScreen)
                    val p2 = vp.screenToWorld(marquee.currentScreen)
                    val wCad = abs(p2.x - p1.x)
                    val hCad = abs(p2.y - p1.y)
                    val badgeText = "Marquee: %.1f × %.1f".format(wCad, hCad)

                    val badgePaint = Paint().apply {
                        color = android.graphics.Color.CYAN
                        textSize = (11f * density).coerceAtLeast(12f)
                        typeface = Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(badgeText, left + 10f, top + 24f, badgePaint)
                }
            }

            // 8. Origin Indicator badge (0,0) in bottom-left corner
            drawOriginWcsBadge(vp)
        }
    }
}

private fun DrawScope.drawSavedMeasurements(
    uiState: CadUiState,
    vp: ViewportState,
    density: Float
) {
    for (record in uiState.savedMeasurements) {
        if (!record.isVisible) continue

        val pts = record.points
        val color = if (record.isHoleSubtraction) Color(0xFFF472B6) else record.color

        when (record.toolType) {
            MeasureToolType.DISTANCE, MeasureToolType.HORIZONTAL, MeasureToolType.VERTICAL -> {
                if (pts.size >= 2) {
                    val s1 = vp.worldToScreen(pts[0])
                    val s2 = vp.worldToScreen(pts[1])
                    drawLine(color, s1, s2, strokeWidth = 2.5f)
                    drawMeasurementMarker(s1, color)
                    drawMeasurementMarker(s2, color)

                    val mid = Offset((s1.x + s2.x) / 2f, (s1.y + s2.y) / 2f)
                    drawCanvasLabelBadge(record.primaryFormatted, mid, color, density)
                }
            }
            MeasureToolType.CONTINUOUS -> {
                if (pts.size >= 2) {
                    for (i in 0 until pts.size - 1) {
                        val s1 = vp.worldToScreen(pts[i])
                        val s2 = vp.worldToScreen(pts[i + 1])
                        drawLine(color, s1, s2, strokeWidth = 2.5f)
                        drawMeasurementMarker(s1, color)
                    }
                    val lastS = vp.worldToScreen(pts.last())
                    drawMeasurementMarker(lastS, color)
                    drawCanvasLabelBadge(record.primaryFormatted, lastS, color, density)
                }
            }
            MeasureToolType.AREA_POLYGON, MeasureToolType.AREA_RECTANGLE, MeasureToolType.AREA_BOUNDARY -> {
                if (pts.size >= 3) {
                    val path = Path()
                    val s0 = vp.worldToScreen(pts[0])
                    path.moveTo(s0.x, s0.y)
                    for (i in 1 until pts.size) {
                        val s = vp.worldToScreen(pts[i])
                        path.lineTo(s.x, s.y)
                    }
                    path.close()
                    drawPath(path, color = color.copy(alpha = 0.2f))
                    drawPath(path, color = color, style = Stroke(width = 2.5f))

                    val centerS = vp.worldToScreen(record.labelPosition)
                    drawCanvasLabelBadge(record.primaryFormatted, centerS, color, density)
                }
            }
            MeasureToolType.ANGLE -> {
                if (pts.size >= 3) {
                    val s1 = vp.worldToScreen(pts[0])
                    val v = vp.worldToScreen(pts[1])
                    val s3 = vp.worldToScreen(pts[2])
                    drawLine(color, v, s1, strokeWidth = 2f)
                    drawLine(color, v, s3, strokeWidth = 2f)
                    drawMeasurementMarker(v, color)
                    drawCanvasLabelBadge(record.primaryFormatted, v, color, density)
                }
            }
            else -> {
                if (pts.isNotEmpty()) {
                    val s = vp.worldToScreen(pts[0])
                    drawMeasurementMarker(s, color)
                    drawCanvasLabelBadge(record.primaryFormatted, s, color, density)
                }
            }
        }
    }
}

private fun DrawScope.drawActiveMeasurementVisuals(
    uiState: CadUiState,
    vp: ViewportState,
    density: Float
) {
    if (uiState.activeTool != CadTool.MEASURE) return

    val active = uiState.activeMeasurement
    val pts = active.pickedPoints
    val liveP = active.livePreviewPoint
    val color = Color(0xFFFF4081)

    // 1. Draw already picked point markers
    pts.forEachIndexed { index, p ->
        val s = vp.worldToScreen(p)
        drawMeasurementMarker(s, color, "P${index + 1}")
    }

    // 2. Draw live preview connecting geometry
    when (active.toolType) {
        MeasureToolType.DISTANCE, MeasureToolType.COORD_DIFFERENCE, MeasureToolType.ELEVATION -> {
            if (pts.isNotEmpty()) {
                val p1 = pts.first()
                val p2 = if (pts.size >= 2) pts[1] else (liveP ?: p1)
                val s1 = vp.worldToScreen(p1)
                val s2 = vp.worldToScreen(p2)

                // Direct dashed line
                drawLine(
                    color = color,
                    start = s1,
                    end = s2,
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )

                // Right-angled triangle components for ΔX and ΔY
                val sCorner = vp.worldToScreen(Point2D(p2.x, p1.y))
                drawLine(
                    color = Color(0x8800E5FF),
                    start = s1,
                    end = sCorner,
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
                drawLine(
                    color = Color(0x8869F0AE),
                    start = sCorner,
                    end = s2,
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                if (pts.size >= 2 || liveP != null) {
                    val mid = Offset((s1.x + s2.x) / 2f, (s1.y + s2.y) / 2f)
                    val label = active.primaryValueDisplay.ifBlank {
                        "%.2f %s".format(p1.distanceTo(p2), uiState.unitConfig.displayDistanceUnit.symbol)
                    }
                    drawCanvasLabelBadge(label, mid, color, density)
                }
            }
        }

        MeasureToolType.HORIZONTAL -> {
            if (pts.isNotEmpty()) {
                val p1 = pts.first()
                val p2 = if (pts.size >= 2) pts[1] else (liveP ?: p1)
                val s1 = vp.worldToScreen(p1)
                val sCorner = vp.worldToScreen(Point2D(p2.x, p1.y))
                drawLine(color, s1, sCorner, strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f))
                val mid = Offset((s1.x + sCorner.x) / 2f, s1.y)
                val label = active.primaryValueDisplay.ifBlank { "ΔX: %.2f".format(abs(p2.x - p1.x)) }
                drawCanvasLabelBadge(label, mid, color, density)
            }
        }

        MeasureToolType.VERTICAL -> {
            if (pts.isNotEmpty()) {
                val p1 = pts.first()
                val p2 = if (pts.size >= 2) pts[1] else (liveP ?: p1)
                val s1 = vp.worldToScreen(p1)
                val sCorner = vp.worldToScreen(Point2D(p1.x, p2.y))
                drawLine(color, s1, sCorner, strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f))
                val mid = Offset(s1.x, (s1.y + sCorner.y) / 2f)
                val label = active.primaryValueDisplay.ifBlank { "ΔY: %.2f".format(abs(p2.y - p1.y)) }
                drawCanvasLabelBadge(label, mid, color, density)
            }
        }

        MeasureToolType.CONTINUOUS -> {
            val allPts = if (liveP != null && !active.isFinished) pts + liveP else pts
            if (allPts.size >= 2) {
                for (i in 0 until allPts.size - 1) {
                    val s1 = vp.worldToScreen(allPts[i])
                    val s2 = vp.worldToScreen(allPts[i + 1])
                    val isLiveSeg = (i == allPts.size - 2 && liveP != null && !active.isFinished)
                    drawLine(
                        color = if (isLiveSeg) Color(0xFFFFD600) else color,
                        start = s1,
                        end = s2,
                        strokeWidth = 2.5f,
                        pathEffect = if (isLiveSeg) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
                    )
                }
                if (active.primaryValueDisplay.isNotBlank()) {
                    val lastS = vp.worldToScreen(allPts.last())
                    drawCanvasLabelBadge(active.primaryValueDisplay, lastS, color, density)
                }
            }
        }

        MeasureToolType.ANGLE -> {
            if (pts.size == 1 && liveP != null) {
                val s1 = vp.worldToScreen(pts[0])
                val sLive = vp.worldToScreen(liveP)
                drawLine(color, s1, sLive, strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
            } else if (pts.size == 2) {
                val s1 = vp.worldToScreen(pts[0])
                val v = vp.worldToScreen(pts[1])
                val s3 = if (liveP != null) vp.worldToScreen(liveP) else s1
                drawLine(color, v, s1, strokeWidth = 2.5f)
                drawLine(Color(0xFFFFD600), v, s3, strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
            } else if (pts.size >= 3) {
                val s1 = vp.worldToScreen(pts[0])
                val v = vp.worldToScreen(pts[1])
                val s3 = vp.worldToScreen(pts[2])
                drawLine(color, v, s1, strokeWidth = 2.5f)
                drawLine(color, v, s3, strokeWidth = 2.5f)
                drawCanvasLabelBadge(active.primaryValueDisplay, v, color, density)
            }
        }

        MeasureToolType.AREA_POLYGON, MeasureToolType.TOTAL_AREA, MeasureToolType.AREA_BOUNDARY -> {
            val allPts = if (liveP != null && !active.isFinished) pts + liveP else pts
            if (allPts.size >= 3) {
                val path = Path()
                val s0 = vp.worldToScreen(allPts[0])
                path.moveTo(s0.x, s0.y)
                for (i in 1 until allPts.size) {
                    val s = vp.worldToScreen(allPts[i])
                    path.lineTo(s.x, s.y)
                }
                path.close()
                drawPath(path, color = Color(0x33FF4081))
                drawPath(path, color = color, style = Stroke(width = 2.5f))

                if (active.primaryValueDisplay.isNotBlank()) {
                    var cx = 0f
                    var cy = 0f
                    for (p in allPts) {
                        val s = vp.worldToScreen(p)
                        cx += s.x
                        cy += s.y
                    }
                    val centerS = Offset(cx / allPts.size, cy / allPts.size)
                    drawCanvasLabelBadge(active.primaryValueDisplay, centerS, color, density)
                }
            } else if (allPts.size == 2) {
                val s1 = vp.worldToScreen(allPts[0])
                val s2 = vp.worldToScreen(allPts[1])
                drawLine(color, s1, s2, strokeWidth = 2.5f)
            }
        }

        MeasureToolType.AREA_RECTANGLE -> {
            if (pts.isNotEmpty()) {
                val p1 = pts.first()
                val p2 = if (pts.size >= 2) pts[1] else (liveP ?: p1)
                val s1 = vp.worldToScreen(p1)
                val s2 = vp.worldToScreen(p2)

                val left = min(s1.x, s2.x)
                val top = min(s1.y, s2.y)
                val width = abs(s2.x - s1.x)
                val height = abs(s2.y - s1.y)

                drawRect(color = Color(0x33FF4081), topLeft = Offset(left, top), size = Size(width, height))
                drawRect(color = color, topLeft = Offset(left, top), size = Size(width, height), style = Stroke(width = 2.5f))

                if (active.primaryValueDisplay.isNotBlank()) {
                    val center = Offset(left + width / 2f, top + height / 2f)
                    drawCanvasLabelBadge(active.primaryValueDisplay, center, color, density)
                }
            }
        }

        MeasureToolType.COORDINATE -> {
            val target = pts.firstOrNull() ?: liveP
            if (target != null) {
                val s = vp.worldToScreen(target)
                drawMeasurementMarker(s, color)
                if (active.primaryValueDisplay.isNotBlank()) {
                    drawCanvasLabelBadge(active.primaryValueDisplay, s, color, density)
                }
            }
        }

        else -> {}
    }
}

private fun DrawScope.drawSnapMarker(
    snap: SnapResult,
    vp: ViewportState,
    density: Float
) {
    val s = vp.worldToScreen(snap.point)
    val snapColor = Color(0xFF00E5FF)
    val sizePx = 18f

    when (snap.mode) {
        SnapMode.ENDPOINT -> {
            // Square
            drawRect(
                color = snapColor,
                topLeft = Offset(s.x - sizePx / 2f, s.y - sizePx / 2f),
                size = Size(sizePx, sizePx),
                style = Stroke(width = 3f)
            )
        }
        SnapMode.MIDPOINT -> {
            // Triangle
            val path = Path().apply {
                moveTo(s.x, s.y - sizePx / 2f)
                lineTo(s.x + sizePx / 2f, s.y + sizePx / 2f)
                lineTo(s.x - sizePx / 2f, s.y + sizePx / 2f)
                close()
            }
            drawPath(path, color = snapColor, style = Stroke(width = 3f))
        }
        SnapMode.CENTER -> {
            // Circle
            drawCircle(snapColor, radius = sizePx / 2f, center = s, style = Stroke(width = 3f))
            drawCircle(snapColor, radius = 2f, center = s)
        }
        SnapMode.INTERSECTION -> {
            // X cross
            drawLine(snapColor, Offset(s.x - sizePx / 2f, s.y - sizePx / 2f), Offset(s.x + sizePx / 2f, s.y + sizePx / 2f), strokeWidth = 3f)
            drawLine(snapColor, Offset(s.x - sizePx / 2f, s.y + sizePx / 2f), Offset(s.x + sizePx / 2f, s.y - sizePx / 2f), strokeWidth = 3f)
        }
        SnapMode.QUADRANT -> {
            // Diamond
            val path = Path().apply {
                moveTo(s.x, s.y - sizePx / 2f)
                lineTo(s.x + sizePx / 2f, s.y)
                lineTo(s.x, s.y + sizePx / 2f)
                lineTo(s.x - sizePx / 2f, s.y)
                close()
            }
            drawPath(path, color = snapColor, style = Stroke(width = 3f))
        }
        SnapMode.PERPENDICULAR -> {
            // Perpendicular symbol
            drawLine(snapColor, Offset(s.x - sizePx / 2f, s.y + sizePx / 2f), Offset(s.x + sizePx / 2f, s.y + sizePx / 2f), strokeWidth = 3f)
            drawLine(snapColor, Offset(s.x, s.y - sizePx / 2f), Offset(s.x, s.y + sizePx / 2f), strokeWidth = 3f)
        }
        SnapMode.NEAREST -> {
            // Hourglass
            val path = Path().apply {
                moveTo(s.x - sizePx / 2f, s.y - sizePx / 2f)
                lineTo(s.x + sizePx / 2f, s.y - sizePx / 2f)
                lineTo(s.x - sizePx / 2f, s.y + sizePx / 2f)
                lineTo(s.x + sizePx / 2f, s.y + sizePx / 2f)
                close()
            }
            drawPath(path, color = snapColor, style = Stroke(width = 2.5f))
        }
        else -> {
            drawCircle(snapColor, radius = 6f, center = s, style = Stroke(width = 3f))
        }
    }

    // Snap Guideline
    if (snap.alignmentGuideline != null) {
        val g1 = vp.worldToScreen(snap.alignmentGuideline.first)
        val g2 = vp.worldToScreen(snap.alignmentGuideline.second)
        drawLine(
            color = Color(0xAA00E5FF),
            start = g1,
            end = g2,
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )
    }

    // Snap Description badge
    val textPaint = Paint().apply {
        color = android.graphics.Color.CYAN
        textSize = (10f * density).coerceAtLeast(11f)
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(snap.description, s.x + 14f, s.y - 10f, textPaint)
}

private fun DrawScope.drawCanvasLabelBadge(
    text: String,
    pos: Offset,
    accentColor: Color,
    density: Float
) {
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = (12f * density).coerceAtLeast(13f)
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val textWidth = paint.measureText(text)
    val textHeight = 28f
    val pad = 12f

    val badgeRect = androidx.compose.ui.geometry.Rect(
        pos.x - textWidth / 2f - pad,
        pos.y - textHeight / 2f - 18f,
        pos.x + textWidth / 2f + pad,
        pos.y + textHeight / 2f - 18f
    )

    // Background pill
    drawRoundRect(
        color = Color(0xF0111827),
        topLeft = Offset(badgeRect.left, badgeRect.top),
        size = Size(badgeRect.width, badgeRect.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(badgeRect.left, badgeRect.top),
        size = Size(badgeRect.width, badgeRect.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(width = 1.5f)
    )

    drawContext.canvas.nativeCanvas.drawText(
        text,
        badgeRect.left + pad,
        badgeRect.bottom - 8f,
        paint
    )
}

private fun DrawScope.drawEntity(
    entity: DxfEntity,
    vp: ViewportState,
    color: Color,
    strokeWidth: Float,
    textPaint: Paint
) {
    when (entity) {
        is DxfLine -> {
            val p1 = vp.worldToScreen(entity.start)
            val p2 = vp.worldToScreen(entity.end)
            drawLine(color, p1, p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
        is DxfCircle -> {
            val center = vp.worldToScreen(entity.center)
            val radiusPx = entity.radius * vp.scale
            if (radiusPx > 0.5f) {
                drawCircle(color, radius = radiusPx, center = center, style = Stroke(width = strokeWidth))
            }
        }
        is DxfArc -> {
            val center = vp.worldToScreen(entity.center)
            val radiusPx = entity.radius * vp.scale
            if (radiusPx > 0.5f) {
                val startScreenDeg = -entity.startAngleDeg
                val sweep = if (entity.endAngleDeg >= entity.startAngleDeg) {
                    -(entity.endAngleDeg - entity.startAngleDeg)
                } else {
                    -(360f - entity.startAngleDeg + entity.endAngleDeg)
                }

                drawArc(
                    color = color,
                    startAngle = startScreenDeg,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                    size = Size(radiusPx * 2f, radiusPx * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        is DxfLwPolyline -> {
            if (entity.vertices.isNotEmpty()) {
                val path = Path()
                val first = vp.worldToScreen(entity.vertices.first())
                path.moveTo(first.x, first.y)
                for (i in 1 until entity.vertices.size) {
                    val p = vp.worldToScreen(entity.vertices[i])
                    path.lineTo(p.x, p.y)
                }
                if (entity.isClosed) {
                    path.close()
                }
                drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            }
        }
        is DxfPolyline -> {
            if (entity.vertices.isNotEmpty()) {
                val path = Path()
                val first = vp.worldToScreen(entity.vertices.first())
                path.moveTo(first.x, first.y)
                for (i in 1 until entity.vertices.size) {
                    val p = vp.worldToScreen(entity.vertices[i])
                    path.lineTo(p.x, p.y)
                }
                if (entity.isClosed) {
                    path.close()
                }
                drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            }
        }
        is DxfEllipse -> {
            val center = vp.worldToScreen(entity.center)
            val majRadius = entity.majorRadius * vp.scale
            val minRadius = majRadius * entity.minorRatio
            if (majRadius > 0.5f) {
                drawOval(
                    color = color,
                    topLeft = Offset(center.x - majRadius, center.y - minRadius),
                    size = Size(majRadius * 2f, minRadius * 2f),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
        is DxfSpline -> {
            if (entity.points.isNotEmpty()) {
                val path = Path()
                val first = vp.worldToScreen(entity.points.first())
                path.moveTo(first.x, first.y)
                for (i in 1 until entity.points.size) {
                    val p = vp.worldToScreen(entity.points[i])
                    path.lineTo(p.x, p.y)
                }
                drawPath(path, color = color, style = Stroke(width = strokeWidth))
            }
        }
        is DxfText -> {
            val pos = vp.worldToScreen(entity.position)
            val scaledTextSize = (entity.height * vp.scale).coerceIn(9f, 60f)
            val p = Paint(textPaint).apply {
                this.color = android.graphics.Color.argb(
                    (color.alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
                textSize = scaledTextSize
            }
            drawContext.canvas.nativeCanvas.drawText(entity.text, pos.x, pos.y, p)
        }
        is DxfDimension -> {
            val p1 = vp.worldToScreen(entity.defPoint)
            val p2 = vp.worldToScreen(entity.textPoint)
            drawLine(color, p1, p2, strokeWidth = strokeWidth * 0.8f)
            if (entity.text.isNotBlank()) {
                val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                val p = Paint(textPaint).apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 24f
                }
                drawContext.canvas.nativeCanvas.drawText(entity.text, mid.x - 20f, mid.y - 8f, p)
            }
        }
        is DxfPoint -> {
            val p = vp.worldToScreen(entity.position)
            drawCircle(color, radius = 3f, center = p)
        }
    }
}

private fun DrawScope.drawCadGrid(vp: ViewportState, isBlueprint: Boolean) {
    val fineGridColor = if (isBlueprint) Color(0x1500E5FF) else Color(0x18FFFFFF)
    val majorGridColor = if (isBlueprint) Color(0x2800E5FF) else Color(0x33FFFFFF)

    val screenStep = 80f
    val rawWorldStep = screenStep / vp.scale
    val magnitude = 10f.pow(floor(log10(rawWorldStep)))
    val normalized = rawWorldStep / magnitude
    val worldStep = when {
        normalized < 2f -> 1f * magnitude
        normalized < 5f -> 2f * magnitude
        else -> 5f * magnitude
    }

    val topLeftWorld = vp.screenToWorld(Offset(0f, 0f))
    val bottomRightWorld = vp.screenToWorld(Offset(size.width, size.height))

    val minX = min(topLeftWorld.x, bottomRightWorld.x)
    val maxX = max(topLeftWorld.x, bottomRightWorld.x)
    val minY = min(topLeftWorld.y, bottomRightWorld.y)
    val maxY = max(topLeftWorld.y, bottomRightWorld.y)

    val startX = floor(minX / worldStep) * worldStep
    val endX = ceil(maxX / worldStep) * worldStep
    val startY = floor(minY / worldStep) * worldStep
    val endY = ceil(maxY / worldStep) * worldStep

    var curX = startX
    var countX = 0
    while (curX <= endX && countX < 200) {
        val sx = vp.worldToScreen(Point2D(curX, 0f)).x
        val isMajor = (round(curX / (worldStep * 5)) * (worldStep * 5) == curX)
        drawLine(
            color = if (isMajor) majorGridColor else fineGridColor,
            start = Offset(sx, 0f),
            end = Offset(sx, size.height),
            strokeWidth = if (isMajor) 1.2f else 0.8f
        )
        curX += worldStep
        countX++
    }

    var curY = startY
    var countY = 0
    while (curY <= endY && countY < 200) {
        val sy = vp.worldToScreen(Point2D(0f, curY)).y
        val isMajor = (round(curY / (worldStep * 5)) * (worldStep * 5) == curY)
        drawLine(
            color = if (isMajor) majorGridColor else fineGridColor,
            start = Offset(0f, sy),
            end = Offset(size.width, sy),
            strokeWidth = if (isMajor) 1.2f else 0.8f
        )
        curY += worldStep
        countY++
    }
}

private fun DrawScope.drawCadAxes(vp: ViewportState) {
    val originScreen = vp.worldToScreen(Point2D(0f, 0f))

    // X Axis (Red)
    drawLine(
        color = Color(0x88FF5252),
        start = Offset(0f, originScreen.y),
        end = Offset(size.width, originScreen.y),
        strokeWidth = 1.5f
    )

    // Y Axis (Green)
    drawLine(
        color = Color(0x8869F0AE),
        start = Offset(originScreen.x, 0f),
        end = Offset(originScreen.x, size.height),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawMeasurementMarker(pos: Offset, color: Color = Color(0xFFFF4081), label: String? = null) {
    val crossSize = 12f
    drawLine(color, Offset(pos.x - crossSize, pos.y), Offset(pos.x + crossSize, pos.y), strokeWidth = 2.5f)
    drawLine(color, Offset(pos.x, pos.y - crossSize), Offset(pos.x, pos.y + crossSize), strokeWidth = 2.5f)
    drawCircle(color, radius = 5f, center = pos, style = Stroke(width = 2f))
}

private fun DrawScope.drawOriginWcsBadge(vp: ViewportState) {
    val origin = vp.worldToScreen(Point2D(0f, 0f))
    if (origin.x in 0f..size.width && origin.y in 0f..size.height) {
        drawLine(Color(0xFFFF5252), origin, Offset(origin.x + 35f, origin.y), strokeWidth = 2.5f)
        drawLine(Color(0xFF69F0AE), origin, Offset(origin.x, origin.y - 35f), strokeWidth = 2.5f)
    }
}
