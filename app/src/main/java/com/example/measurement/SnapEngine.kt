package com.example.measurement

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.canvas.GridSettings
import com.example.canvas.ViewportState
import com.example.parser.*
import kotlin.math.*

enum class SnapMode(val displayName: String, val symbol: String) {
    ENDPOINT("Endpoint", "□"),
    MIDPOINT("Midpoint", "△"),
    CENTER("Center", "○"),
    INTERSECTION("Intersection", "✕"),
    PERPENDICULAR("Perpendicular", "⟂"),
    NEAREST("Nearest", "⧖"),
    QUADRANT("Quadrant", "◇"),
    TANGENT("Tangent", "⌒"),
    NODE("Node (Point)", "•"),
    GRID_NODE("Grid Snap", "⊞")
}

enum class OrthoMode(val displayName: String) {
    OFF("Ortho: Off"),
    HORIZONTAL("Horizontal Lock (ΔY=0)"),
    VERTICAL("Vertical Lock (ΔX=0)"),
    AUTO_ORTHO("Auto Ortho (0°/90° Snap)")
}

enum class PolarTrackingMode(val displayName: String, val stepDeg: Float) {
    OFF("Polar: Off", 0f),
    STEP_15("Polar 15°", 15f),
    STEP_30("Polar 30°", 30f),
    STEP_45("Polar 45°", 45f),
    STEP_90("Polar 90°", 90f)
}

data class SnapSettings(
    val isEnabled: Boolean = true,
    val activeModes: Set<SnapMode> = setOf(
        SnapMode.ENDPOINT,
        SnapMode.MIDPOINT,
        SnapMode.CENTER,
        SnapMode.INTERSECTION,
        SnapMode.QUADRANT,
        SnapMode.NEAREST,
        SnapMode.PERPENDICULAR
    ),
    val snapTolerancePx: Float = 28f,
    val orthoMode: OrthoMode = OrthoMode.OFF,
    val polarMode: PolarTrackingMode = PolarTrackingMode.OFF
)

data class SnapResult(
    val point: Point2D,
    val mode: SnapMode,
    val entity: DxfEntity? = null,
    val description: String = mode.displayName,
    val alignmentGuideline: Pair<Point2D, Point2D>? = null // for polar tracking or perpendicular projection
)

object SnapEngine {

    /**
     * Finds the best snap point near cursorPoint in world coordinates.
     */
    fun findSnap(
        cursorWorld: Point2D,
        entities: List<DxfEntity>,
        layerVisibility: Map<String, Boolean>,
        viewport: ViewportState,
        settings: SnapSettings,
        referencePoint: Point2D? = null,
        gridSettings: GridSettings? = null
    ): SnapResult? {
        if (!settings.isEnabled && gridSettings?.isSnapToGrid != true) return null

        val toleranceWorld = settings.snapTolerancePx / viewport.scale
        var bestSnap: SnapResult? = null
        var minDistance = toleranceWorld

        // Helper to check and record a candidate snap point
        fun checkCandidate(point: Point2D, mode: SnapMode, entity: DxfEntity?, desc: String) {
            if (!settings.activeModes.contains(mode) && !(mode == SnapMode.GRID_NODE && gridSettings?.isSnapToGrid == true)) return
            val d = cursorWorld.distanceTo(point)
            if (d < minDistance) {
                minDistance = d
                bestSnap = SnapResult(point, mode, entity, desc)
            }
        }

        // 0. Grid Snap candidate
        if (gridSettings != null && gridSettings.isVisible && (gridSettings.isSnapToGrid || settings.activeModes.contains(SnapMode.GRID_NODE))) {
            val snapStep = gridSettings.computeSnapStep(viewport.scale)
            if (snapStep > 0.00001f) {
                val gx = round(cursorWorld.x / snapStep) * snapStep
                val gy = round(cursorWorld.y / snapStep) * snapStep
                val gPoint = Point2D(gx, gy)
                val d = cursorWorld.distanceTo(gPoint)
                val gridTolerance = if (gridSettings.isSnapToGrid) (settings.snapTolerancePx * 1.5f) / viewport.scale else toleranceWorld
                if (d < gridTolerance) {
                    minDistance = d
                    bestSnap = SnapResult(gPoint, SnapMode.GRID_NODE, null, "Grid Snap (%.2f, %.2f)".format(gx, gy))
                }
            }
        }

        // 1. Gather all geometry key points from visible entities
        val visibleEntities = entities.filter { layerVisibility[it.layer] != false }

        // Intersections between lines/segments
        if (settings.activeModes.contains(SnapMode.INTERSECTION)) {
            val segments = extractSegments(visibleEntities)
            for (i in 0 until segments.size) {
                for (j in i + 1 until segments.size) {
                    val inter = findSegmentIntersection(segments[i].first, segments[i].second, segments[j].first, segments[j].second)
                    if (inter != null) {
                        checkCandidate(inter, SnapMode.INTERSECTION, null, "Intersection")
                    }
                }
            }
        }

        for (entity in visibleEntities) {
            when (entity) {
                is DxfLine -> {
                    // Endpoint
                    checkCandidate(entity.start, SnapMode.ENDPOINT, entity, "Line Endpoint")
                    checkCandidate(entity.end, SnapMode.ENDPOINT, entity, "Line Endpoint")
                    // Midpoint
                    val mid = Point2D((entity.start.x + entity.end.x) / 2f, (entity.start.y + entity.end.y) / 2f)
                    checkCandidate(mid, SnapMode.MIDPOINT, entity, "Line Midpoint")

                    // Perpendicular from reference point
                    if (referencePoint != null && settings.activeModes.contains(SnapMode.PERPENDICULAR)) {
                        val perp = projectPointOnSegment(referencePoint, entity.start, entity.end)
                        if (perp != null) {
                            checkCandidate(perp, SnapMode.PERPENDICULAR, entity, "Perpendicular")
                        }
                    }

                    // Nearest
                    if (settings.activeModes.contains(SnapMode.NEAREST)) {
                        val nearest = projectPointOnSegment(cursorWorld, entity.start, entity.end)
                        if (nearest != null) {
                            checkCandidate(nearest, SnapMode.NEAREST, entity, "Nearest on Line")
                        }
                    }
                }
                is DxfCircle -> {
                    // Center
                    checkCandidate(entity.center, SnapMode.CENTER, entity, "Circle Center")

                    // Quadrants (0°, 90°, 180°, 270°)
                    if (settings.activeModes.contains(SnapMode.QUADRANT)) {
                        checkCandidate(Point2D(entity.center.x + entity.radius, entity.center.y), SnapMode.QUADRANT, entity, "Quadrant 0°")
                        checkCandidate(Point2D(entity.center.x, entity.center.y + entity.radius), SnapMode.QUADRANT, entity, "Quadrant 90°")
                        checkCandidate(Point2D(entity.center.x - entity.radius, entity.center.y), SnapMode.QUADRANT, entity, "Quadrant 180°")
                        checkCandidate(Point2D(entity.center.x, entity.center.y - entity.radius), SnapMode.QUADRANT, entity, "Quadrant 270°")
                    }

                    // Nearest on circle perimeter
                    if (settings.activeModes.contains(SnapMode.NEAREST)) {
                        val angle = atan2((cursorWorld.y - entity.center.y).toDouble(), (cursorWorld.x - entity.center.x).toDouble())
                        val onCircle = Point2D(
                            (entity.center.x + entity.radius * cos(angle)).toFloat(),
                            (entity.center.y + entity.radius * sin(angle)).toFloat()
                        )
                        checkCandidate(onCircle, SnapMode.NEAREST, entity, "Nearest on Circle")
                    }

                    // Tangent from referencePoint
                    if (referencePoint != null && settings.activeModes.contains(SnapMode.TANGENT)) {
                        val dCenter = referencePoint.distanceTo(entity.center)
                        if (dCenter > entity.radius) {
                            val alpha = asin((entity.radius / dCenter).toDouble())
                            val baseAngle = atan2((entity.center.y - referencePoint.y).toDouble(), (entity.center.x - referencePoint.x).toDouble())
                            val t1 = Point2D(
                                (entity.center.x + entity.radius * cos(baseAngle + Math.PI / 2 + alpha)).toFloat(),
                                (entity.center.y + entity.radius * sin(baseAngle + Math.PI / 2 + alpha)).toFloat()
                            )
                            val t2 = Point2D(
                                (entity.center.x + entity.radius * cos(baseAngle - Math.PI / 2 - alpha)).toFloat(),
                                (entity.center.y + entity.radius * sin(baseAngle - Math.PI / 2 - alpha)).toFloat()
                            )
                            checkCandidate(t1, SnapMode.TANGENT, entity, "Tangent Point")
                            checkCandidate(t2, SnapMode.TANGENT, entity, "Tangent Point")
                        }
                    }
                }
                is DxfArc -> {
                    // Center
                    checkCandidate(entity.center, SnapMode.CENTER, entity, "Arc Center")

                    // Endpoints
                    val radStart = Math.toRadians(entity.startAngleDeg.toDouble())
                    val radEnd = Math.toRadians(entity.endAngleDeg.toDouble())
                    val pStart = Point2D(
                        (entity.center.x + entity.radius * cos(radStart)).toFloat(),
                        (entity.center.y + entity.radius * sin(radStart)).toFloat()
                    )
                    val pEnd = Point2D(
                        (entity.center.x + entity.radius * cos(radEnd)).toFloat(),
                        (entity.center.y + entity.radius * sin(radEnd)).toFloat()
                    )
                    checkCandidate(pStart, SnapMode.ENDPOINT, entity, "Arc Start")
                    checkCandidate(pEnd, SnapMode.ENDPOINT, entity, "Arc End")

                    // Midpoint along arc
                    val midAngleDeg = if (entity.endAngleDeg >= entity.startAngleDeg) {
                        (entity.startAngleDeg + entity.endAngleDeg) / 2f
                    } else {
                        ((entity.startAngleDeg + entity.endAngleDeg + 360f) / 2f) % 360f
                    }
                    val radMid = Math.toRadians(midAngleDeg.toDouble())
                    val pMid = Point2D(
                        (entity.center.x + entity.radius * cos(radMid)).toFloat(),
                        (entity.center.y + entity.radius * sin(radMid)).toFloat()
                    )
                    checkCandidate(pMid, SnapMode.MIDPOINT, entity, "Arc Midpoint")

                    // Quadrants within arc span
                    if (settings.activeModes.contains(SnapMode.QUADRANT)) {
                        val quads = listOf(0f, 90f, 180f, 270f)
                        for (q in quads) {
                            if (isAngleInArcSpan(q, entity.startAngleDeg, entity.endAngleDeg)) {
                                val radQ = Math.toRadians(q.toDouble())
                                val qPoint = Point2D(
                                    (entity.center.x + entity.radius * cos(radQ)).toFloat(),
                                    (entity.center.y + entity.radius * sin(radQ)).toFloat()
                                )
                                checkCandidate(qPoint, SnapMode.QUADRANT, entity, "Arc Quadrant ${q.toInt()}°")
                            }
                        }
                    }
                }
                is DxfLwPolyline -> {
                    val v = entity.vertices
                    for (i in 0 until v.size) {
                        checkCandidate(v[i], SnapMode.ENDPOINT, entity, "Polyline Vertex")
                    }
                    for (i in 0 until v.size - 1) {
                        val mid = Point2D((v[i].x + v[i + 1].x) / 2f, (v[i].y + v[i + 1].y) / 2f)
                        checkCandidate(mid, SnapMode.MIDPOINT, entity, "Polyline Segment Midpoint")
                        if (settings.activeModes.contains(SnapMode.NEAREST)) {
                            val near = projectPointOnSegment(cursorWorld, v[i], v[i + 1])
                            if (near != null) checkCandidate(near, SnapMode.NEAREST, entity, "Nearest on Polyline")
                        }
                    }
                    if (entity.isClosed && v.size > 2) {
                        val mid = Point2D((v.last().x + v.first().x) / 2f, (v.last().y + v.first().y) / 2f)
                        checkCandidate(mid, SnapMode.MIDPOINT, entity, "Polyline Closing Midpoint")
                    }
                }
                is DxfPolyline -> {
                    val v = entity.vertices
                    for (i in 0 until v.size) {
                        checkCandidate(v[i], SnapMode.ENDPOINT, entity, "Polyline Vertex")
                    }
                    for (i in 0 until v.size - 1) {
                        val mid = Point2D((v[i].x + v[i + 1].x) / 2f, (v[i].y + v[i + 1].y) / 2f)
                        checkCandidate(mid, SnapMode.MIDPOINT, entity, "Polyline Segment Midpoint")
                    }
                }
                is DxfPoint -> {
                    checkCandidate(entity.position, SnapMode.NODE, entity, "Node Point")
                }
                is DxfDimension -> {
                    checkCandidate(entity.defPoint, SnapMode.ENDPOINT, entity, "Dimension Def Point")
                    checkCandidate(entity.textPoint, SnapMode.ENDPOINT, entity, "Dimension Text Point")
                }
                else -> {}
            }
        }

        // 2. If no object snap matched, apply Ortho or Polar tracking if a reference point exists
        if (bestSnap == null && referencePoint != null) {
            val orthoPoint = applyOrthoOrPolar(referencePoint, cursorWorld, settings)
            if (orthoPoint != cursorWorld) {
                return SnapResult(
                    point = orthoPoint,
                    mode = SnapMode.NEAREST,
                    description = if (settings.orthoMode != OrthoMode.OFF) "Ortho Alignment" else "Polar Alignment",
                    alignmentGuideline = Pair(referencePoint, orthoPoint)
                )
            }
        }

        return bestSnap
    }

    /**
     * Applies Ortho / Polar angle constraints relative to referencePoint.
     */
    fun applyOrthoOrPolar(
        reference: Point2D,
        cursor: Point2D,
        settings: SnapSettings
    ): Point2D {
        val dx = cursor.x - reference.x
        val dy = cursor.y - reference.y
        val dist = cursor.distanceTo(reference)
        if (dist < 0.001f) return cursor

        // 1. Direct Ortho
        when (settings.orthoMode) {
            OrthoMode.HORIZONTAL -> return Point2D(cursor.x, reference.y)
            OrthoMode.VERTICAL -> return Point2D(reference.x, cursor.y)
            OrthoMode.AUTO_ORTHO -> {
                return if (abs(dx) >= abs(dy)) Point2D(cursor.x, reference.y) else Point2D(reference.x, cursor.y)
            }
            OrthoMode.OFF -> {}
        }

        // 2. Polar tracking
        if (settings.polarMode != PolarTrackingMode.OFF && settings.polarMode.stepDeg > 0f) {
            val rawAngleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val normAngle = if (rawAngleDeg < 0) rawAngleDeg + 360f else rawAngleDeg
            val step = settings.polarMode.stepDeg
            val snappedAngleDeg = (normAngle / step).roundToInt() * step
            val angleDiff = abs(normAngle - snappedAngleDeg)

            // If within 6 degrees of a polar ray, snap to that ray
            if (angleDiff <= 6.0f) {
                val rad = Math.toRadians(snappedAngleDeg.toDouble())
                return Point2D(
                    (reference.x + dist * cos(rad)).toFloat(),
                    (reference.y + dist * sin(rad)).toFloat()
                )
            }
        }

        return cursor
    }

    private fun extractSegments(entities: List<DxfEntity>): List<Pair<Point2D, Point2D>> {
        val segments = mutableListOf<Pair<Point2D, Point2D>>()
        for (e in entities) {
            when (e) {
                is DxfLine -> segments.add(Pair(e.start, e.end))
                is DxfLwPolyline -> {
                    for (i in 0 until e.vertices.size - 1) {
                        segments.add(Pair(e.vertices[i], e.vertices[i + 1]))
                    }
                    if (e.isClosed && e.vertices.size > 2) {
                        segments.add(Pair(e.vertices.last(), e.vertices.first()))
                    }
                }
                is DxfPolyline -> {
                    for (i in 0 until e.vertices.size - 1) {
                        segments.add(Pair(e.vertices[i], e.vertices[i + 1]))
                    }
                }
                else -> {}
            }
        }
        return segments
    }

    private fun findSegmentIntersection(p1: Point2D, p2: Point2D, p3: Point2D, p4: Point2D): Point2D? {
        val d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
        if (abs(d) < 0.0001f) return null

        val t = ((p1.x - p3.x) * (p3.y - p4.y) - (p1.y - p3.y) * (p3.x - p4.x)) / d
        val u = -((p1.x - p2.x) * (p1.y - p3.y) - (p1.y - p2.y) * (p1.x - p3.x)) / d

        if (t in 0.001f..0.999f && u in 0.001f..0.999f) {
            return Point2D(
                p1.x + t * (p2.x - p1.x),
                p1.y + t * (p2.y - p1.y)
            )
        }
        return null
    }

    private fun projectPointOnSegment(p: Point2D, a: Point2D, b: Point2D): Point2D? {
        val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        if (l2 < 0.00001f) return a
        val t = (((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2).coerceIn(0f, 1f)
        return Point2D(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
    }

    private fun isAngleInArcSpan(angle: Float, startDeg: Float, endDeg: Float): Boolean {
        val norm = (angle % 360f + 360f) % 360f
        return if (endDeg >= startDeg) {
            norm in startDeg..endDeg
        } else {
            norm >= startDeg || norm <= endDeg
        }
    }
}
