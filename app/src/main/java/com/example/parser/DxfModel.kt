package com.example.parser

import androidx.compose.ui.graphics.Color
import kotlin.math.*

data class Point2D(val x: Float, val y: Float) {
    fun distanceTo(other: Point2D): Float {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    fun rotateAround(origin: Point2D, angleDeg: Float): Point2D {
        val rad = Math.toRadians(angleDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val dx = x - origin.x
        val dy = y - origin.y
        return Point2D(
            x = origin.x + (dx * cosA - dy * sinA),
            y = origin.y + (dx * sinA + dy * cosA)
        )
    }

    fun scaleFrom(origin: Point2D, factor: Float): Point2D {
        return Point2D(
            x = origin.x + (x - origin.x) * factor,
            y = origin.y + (y - origin.y) * factor
        )
    }

    fun mirrorAcross(axisP1: Point2D, axisP2: Point2D): Point2D {
        val dx = axisP2.x - axisP1.x
        val dy = axisP2.y - axisP1.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 0.0001f) return this
        val u = ((x - axisP1.x) * dx + (y - axisP1.y) * dy) / lenSq
        val projX = axisP1.x + u * dx
        val projY = axisP1.y + u * dy
        return Point2D(2 * projX - x, 2 * projY - y)
    }

    operator fun plus(other: Point2D) = Point2D(x + other.x, y + other.y)
    operator fun minus(other: Point2D) = Point2D(x - other.x, y - other.y)
    operator fun times(factor: Float) = Point2D(x * factor, y * factor)
}

data class Point3D(val x: Float, val y: Float, val z: Float = 0f) {
    fun toPoint2D(): Point2D = Point2D(x, y)
}

data class BoundingBox(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(0.001f)
    val height: Float get() = (maxY - minY).coerceAtLeast(0.001f)
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f

    fun expandedBy(point: Point2D): BoundingBox {
        return BoundingBox(
            minX = min(minX, point.x),
            minY = min(minY, point.y),
            maxX = max(maxX, point.x),
            maxY = max(maxY, point.y)
        )
    }

    companion object {
        val EMPTY = BoundingBox(0f, 0f, 100f, 100f)

        fun fromPoints(points: List<Point2D>): BoundingBox {
            if (points.isEmpty()) return EMPTY
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE

            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
            if (minX == maxX) { minX -= 5f; maxX += 5f }
            if (minY == maxY) { minY -= 5f; maxY += 5f }
            return BoundingBox(minX, minY, maxX, maxY)
        }
    }
}

data class DxfLayer(
    val name: String,
    val color: Color,
    val isVisible: Boolean = true,
    val lineType: String = "CONTINUOUS"
)

data class DxfDocument(
    val fileName: String,
    val layers: Map<String, DxfLayer>,
    val entities: List<DxfEntity>,
    val extents: BoundingBox
)
