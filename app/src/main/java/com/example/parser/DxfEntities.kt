package com.example.parser

import androidx.compose.ui.graphics.Color
import kotlin.math.*

sealed class DxfEntity {
    abstract val layer: String
    abstract val color: Color?
    abstract fun getBoundingBox(): BoundingBox
    abstract fun getSummary(): String
    abstract fun getCenterPoint(): Point2D

    abstract fun translate(dx: Float, dy: Float): DxfEntity
    abstract fun rotate(origin: Point2D, angleDeg: Float): DxfEntity
    abstract fun scale(origin: Point2D, factor: Float): DxfEntity
    abstract fun mirror(axisP1: Point2D, axisP2: Point2D): DxfEntity
    abstract fun trimOrExtend(factor: Float): DxfEntity
}

data class DxfLine(
    val start: Point2D,
    val end: Point2D,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    val length: Float get() = start.distanceTo(end)

    override fun getBoundingBox(): BoundingBox =
        BoundingBox.fromPoints(listOf(start, end))

    override fun getCenterPoint(): Point2D =
        Point2D((start.x + end.x) / 2f, (start.y + end.y) / 2f)

    override fun getSummary(): String =
        "Line: L=%.2f, (%.1f, %.1f) -> (%.1f, %.1f)".format(length, start.x, start.y, end.x, end.y)

    override fun translate(dx: Float, dy: Float): DxfLine =
        copy(start = Point2D(start.x + dx, start.y + dy), end = Point2D(end.x + dx, end.y + dy))

    override fun rotate(origin: Point2D, angleDeg: Float): DxfLine =
        copy(start = start.rotateAround(origin, angleDeg), end = end.rotateAround(origin, angleDeg))

    override fun scale(origin: Point2D, factor: Float): DxfLine =
        copy(start = start.scaleFrom(origin, factor), end = end.scaleFrom(origin, factor))

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfLine =
        copy(start = start.mirrorAcross(axisP1, axisP2), end = end.mirrorAcross(axisP1, axisP2))

    override fun trimOrExtend(factor: Float): DxfLine {
        val mid = getCenterPoint()
        val newStart = Point2D(mid.x + (start.x - mid.x) * factor, mid.y + (start.y - mid.y) * factor)
        val newEnd = Point2D(mid.x + (end.x - mid.x) * factor, mid.y + (end.y - mid.y) * factor)
        return copy(start = newStart, end = newEnd)
    }
}

data class DxfCircle(
    val center: Point2D,
    val radius: Float,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    val circumference: Float get() = (2 * Math.PI * radius).toFloat()
    val area: Float get() = (Math.PI * radius * radius).toFloat()

    override fun getBoundingBox(): BoundingBox =
        BoundingBox(center.x - radius, center.y - radius, center.x + radius, center.y + radius)

    override fun getCenterPoint(): Point2D = center

    override fun getSummary(): String =
        "Circle: R=%.2f, Center=(%.1f, %.1f)".format(radius, center.x, center.y)

    override fun translate(dx: Float, dy: Float): DxfCircle =
        copy(center = Point2D(center.x + dx, center.y + dy))

    override fun rotate(origin: Point2D, angleDeg: Float): DxfCircle =
        copy(center = center.rotateAround(origin, angleDeg))

    override fun scale(origin: Point2D, factor: Float): DxfCircle =
        copy(center = center.scaleFrom(origin, factor), radius = (radius * factor).coerceAtLeast(0.01f))

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfCircle =
        copy(center = center.mirrorAcross(axisP1, axisP2))

    override fun trimOrExtend(factor: Float): DxfCircle =
        copy(radius = (radius * factor).coerceAtLeast(0.01f))
}

data class DxfArc(
    val center: Point2D,
    val radius: Float,
    val startAngleDeg: Float,
    val endAngleDeg: Float,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    override fun getBoundingBox(): BoundingBox {
        val points = mutableListOf<Point2D>()
        val rad1 = Math.toRadians(startAngleDeg.toDouble())
        val rad2 = Math.toRadians(endAngleDeg.toDouble())
        points.add(Point2D((center.x + radius * cos(rad1)).toFloat(), (center.y + radius * sin(rad1)).toFloat()))
        points.add(Point2D((center.x + radius * cos(rad2)).toFloat(), (center.y + radius * sin(rad2)).toFloat()))

        val sweep = if (endAngleDeg >= startAngleDeg) endAngleDeg - startAngleDeg else (360f - startAngleDeg + endAngleDeg)
        val steps = 8
        for (i in 1 until steps) {
            val a = Math.toRadians((startAngleDeg + sweep * (i.toDouble() / steps)))
            points.add(Point2D((center.x + radius * cos(a)).toFloat(), (center.y + radius * sin(a)).toFloat()))
        }
        return BoundingBox.fromPoints(points)
    }

    override fun getCenterPoint(): Point2D = center

    override fun getSummary(): String =
        "Arc: R=%.2f, %.0f° to %.0f°".format(radius, startAngleDeg, endAngleDeg)

    override fun translate(dx: Float, dy: Float): DxfArc =
        copy(center = Point2D(center.x + dx, center.y + dy))

    override fun rotate(origin: Point2D, angleDeg: Float): DxfArc =
        copy(
            center = center.rotateAround(origin, angleDeg),
            startAngleDeg = (startAngleDeg + angleDeg) % 360f,
            endAngleDeg = (endAngleDeg + angleDeg) % 360f
        )

    override fun scale(origin: Point2D, factor: Float): DxfArc =
        copy(center = center.scaleFrom(origin, factor), radius = (radius * factor).coerceAtLeast(0.01f))

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfArc =
        copy(center = center.mirrorAcross(axisP1, axisP2))

    override fun trimOrExtend(factor: Float): DxfArc {
        val sweep = if (endAngleDeg >= startAngleDeg) endAngleDeg - startAngleDeg else (360f - startAngleDeg + endAngleDeg)
        val newSweep = (sweep * factor).coerceIn(5f, 360f)
        return copy(endAngleDeg = (startAngleDeg + newSweep) % 360f)
    }
}

data class DxfLwPolyline(
    val vertices: List<Point2D>,
    val isClosed: Boolean = false,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    val totalLength: Float
        get() {
            var len = 0f
            for (i in 0 until vertices.size - 1) {
                len += vertices[i].distanceTo(vertices[i + 1])
            }
            if (isClosed && vertices.size > 2) {
                len += vertices.last().distanceTo(vertices.first())
            }
            return len
        }

    override fun getBoundingBox(): BoundingBox = BoundingBox.fromPoints(vertices)

    override fun getCenterPoint(): Point2D {
        val bb = getBoundingBox()
        return Point2D(bb.centerX, bb.centerY)
    }

    override fun getSummary(): String =
        "LWPolyline: %d pts, Len=%.2f, %s".format(
            vertices.size,
            totalLength,
            if (isClosed) "Closed" else "Open"
        )

    override fun translate(dx: Float, dy: Float): DxfLwPolyline =
        copy(vertices = vertices.map { Point2D(it.x + dx, it.y + dy) })

    override fun rotate(origin: Point2D, angleDeg: Float): DxfLwPolyline =
        copy(vertices = vertices.map { it.rotateAround(origin, angleDeg) })

    override fun scale(origin: Point2D, factor: Float): DxfLwPolyline =
        copy(vertices = vertices.map { it.scaleFrom(origin, factor) })

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfLwPolyline =
        copy(vertices = vertices.map { it.mirrorAcross(axisP1, axisP2) })

    override fun trimOrExtend(factor: Float): DxfLwPolyline {
        val center = getCenterPoint()
        return copy(vertices = vertices.map { it.scaleFrom(center, factor) })
    }
}

data class DxfPolyline(
    val vertices: List<Point2D>,
    val isClosed: Boolean = false,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    override fun getBoundingBox(): BoundingBox = BoundingBox.fromPoints(vertices)

    override fun getCenterPoint(): Point2D {
        val bb = getBoundingBox()
        return Point2D(bb.centerX, bb.centerY)
    }

    override fun getSummary(): String =
        "Polyline: %d pts, %s".format(vertices.size, if (isClosed) "Closed" else "Open")

    override fun translate(dx: Float, dy: Float): DxfPolyline =
        copy(vertices = vertices.map { Point2D(it.x + dx, it.y + dy) })

    override fun rotate(origin: Point2D, angleDeg: Float): DxfPolyline =
        copy(vertices = vertices.map { it.rotateAround(origin, angleDeg) })

    override fun scale(origin: Point2D, factor: Float): DxfPolyline =
        copy(vertices = vertices.map { it.scaleFrom(origin, factor) })

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfPolyline =
        copy(vertices = vertices.map { it.mirrorAcross(axisP1, axisP2) })

    override fun trimOrExtend(factor: Float): DxfPolyline {
        val center = getCenterPoint()
        return copy(vertices = vertices.map { it.scaleFrom(center, factor) })
    }
}

data class DxfEllipse(
    val center: Point2D,
    val majorAxisEndpoint: Point2D,
    val minorRatio: Float,
    val startParam: Float = 0f,
    val endParam: Float = (2 * Math.PI).toFloat(),
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    val majorRadius: Float get() = sqrt(majorAxisEndpoint.x * majorAxisEndpoint.x + majorAxisEndpoint.y * majorAxisEndpoint.y)

    override fun getBoundingBox(): BoundingBox {
        val r = majorRadius
        return BoundingBox(center.x - r, center.y - r, center.x + r, center.y + r)
    }

    override fun getCenterPoint(): Point2D = center

    override fun getSummary(): String =
        "Ellipse: Center=(%.1f, %.1f), Major=%.2f, Ratio=%.2f".format(center.x, center.y, majorRadius, minorRatio)

    override fun translate(dx: Float, dy: Float): DxfEllipse =
        copy(center = Point2D(center.x + dx, center.y + dy))

    override fun rotate(origin: Point2D, angleDeg: Float): DxfEllipse =
        copy(
            center = center.rotateAround(origin, angleDeg),
            majorAxisEndpoint = majorAxisEndpoint.rotateAround(Point2D(0f, 0f), angleDeg)
        )

    override fun scale(origin: Point2D, factor: Float): DxfEllipse =
        copy(
            center = center.scaleFrom(origin, factor),
            majorAxisEndpoint = majorAxisEndpoint * factor
        )

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfEllipse =
        copy(center = center.mirrorAcross(axisP1, axisP2))

    override fun trimOrExtend(factor: Float): DxfEllipse =
        copy(majorAxisEndpoint = majorAxisEndpoint * factor)
}

data class DxfSpline(
    val points: List<Point2D>,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    override fun getBoundingBox(): BoundingBox = BoundingBox.fromPoints(points)

    override fun getCenterPoint(): Point2D {
        val bb = getBoundingBox()
        return Point2D(bb.centerX, bb.centerY)
    }

    override fun getSummary(): String = "Spline: %d control points".format(points.size)

    override fun translate(dx: Float, dy: Float): DxfSpline =
        copy(points = points.map { Point2D(it.x + dx, it.y + dy) })

    override fun rotate(origin: Point2D, angleDeg: Float): DxfSpline =
        copy(points = points.map { it.rotateAround(origin, angleDeg) })

    override fun scale(origin: Point2D, factor: Float): DxfSpline =
        copy(points = points.map { it.scaleFrom(origin, factor) })

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfSpline =
        copy(points = points.map { it.mirrorAcross(axisP1, axisP2) })

    override fun trimOrExtend(factor: Float): DxfSpline {
        val center = getCenterPoint()
        return copy(points = points.map { it.scaleFrom(center, factor) })
    }
}

data class DxfText(
    val text: String,
    val position: Point2D,
    val height: Float = 2.5f,
    val rotationDeg: Float = 0f,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    override fun getBoundingBox(): BoundingBox {
        val estimatedWidth = text.length * height * 0.7f
        return BoundingBox(position.x, position.y, position.x + estimatedWidth, position.y + height)
    }

    override fun getCenterPoint(): Point2D = position

    override fun getSummary(): String = "Text: \"$text\" at (%.1f, %.1f)".format(position.x, position.y)

    override fun translate(dx: Float, dy: Float): DxfText =
        copy(position = Point2D(position.x + dx, position.y + dy))

    override fun rotate(origin: Point2D, angleDeg: Float): DxfText =
        copy(
            position = position.rotateAround(origin, angleDeg),
            rotationDeg = (rotationDeg + angleDeg) % 360f
        )

    override fun scale(origin: Point2D, factor: Float): DxfText =
        copy(position = position.scaleFrom(origin, factor), height = (height * factor).coerceAtLeast(0.5f))

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfText =
        copy(position = position.mirrorAcross(axisP1, axisP2))

    override fun trimOrExtend(factor: Float): DxfText =
        copy(height = (height * factor).coerceAtLeast(0.5f))
}

data class DxfDimension(
    val text: String,
    val defPoint: Point2D,
    val textPoint: Point2D,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    override fun getBoundingBox(): BoundingBox =
        BoundingBox.fromPoints(listOf(defPoint, textPoint))

    override fun getCenterPoint(): Point2D =
        Point2D((defPoint.x + textPoint.x) / 2f, (defPoint.y + textPoint.y) / 2f)

    override fun getSummary(): String = "Dimension: \"$text\"".ifBlank { "Dimension at (%.1f, %.1f)".format(defPoint.x, defPoint.y) }

    override fun translate(dx: Float, dy: Float): DxfDimension =
        copy(
            defPoint = Point2D(defPoint.x + dx, defPoint.y + dy),
            textPoint = Point2D(textPoint.x + dx, textPoint.y + dy)
        )

    override fun rotate(origin: Point2D, angleDeg: Float): DxfDimension =
        copy(
            defPoint = defPoint.rotateAround(origin, angleDeg),
            textPoint = textPoint.rotateAround(origin, angleDeg)
        )

    override fun scale(origin: Point2D, factor: Float): DxfDimension =
        copy(
            defPoint = defPoint.scaleFrom(origin, factor),
            textPoint = textPoint.scaleFrom(origin, factor)
        )

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfDimension =
        copy(
            defPoint = defPoint.mirrorAcross(axisP1, axisP2),
            textPoint = textPoint.mirrorAcross(axisP1, axisP2)
        )

    override fun trimOrExtend(factor: Float): DxfDimension {
        val mid = getCenterPoint()
        return copy(
            defPoint = defPoint.scaleFrom(mid, factor),
            textPoint = textPoint.scaleFrom(mid, factor)
        )
    }
}

data class DxfPoint(
    val position: Point2D,
    override val layer: String = "0",
    override val color: Color? = null
) : DxfEntity() {
    override fun getBoundingBox(): BoundingBox =
        BoundingBox(position.x - 1f, position.y - 1f, position.x + 1f, position.y + 1f)

    override fun getCenterPoint(): Point2D = position

    override fun getSummary(): String = "Point: (%.2f, %.2f)".format(position.x, position.y)

    override fun translate(dx: Float, dy: Float): DxfPoint =
        copy(position = Point2D(position.x + dx, position.y + dy))

    override fun rotate(origin: Point2D, angleDeg: Float): DxfPoint =
        copy(position = position.rotateAround(origin, angleDeg))

    override fun scale(origin: Point2D, factor: Float): DxfPoint =
        copy(position = position.scaleFrom(origin, factor))

    override fun mirror(axisP1: Point2D, axisP2: Point2D): DxfPoint =
        copy(position = position.mirrorAcross(axisP1, axisP2))

    override fun trimOrExtend(factor: Float): DxfPoint = this
}
