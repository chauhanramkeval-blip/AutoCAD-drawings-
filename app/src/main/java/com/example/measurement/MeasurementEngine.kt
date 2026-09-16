package com.example.measurement

import com.example.parser.*
import java.util.Locale
import kotlin.math.*

data class MeasurementComputationResult(
    val primaryValue: Double,
    val primaryFormatted: String,
    val subValueFormatted: String = "",
    val details: Map<String, String> = emptyMap(),
    val labelPosition: Point2D = Point2D(0f, 0f),
    val isComplete: Boolean = true
)

object MeasurementEngine {

    /**
     * 1. Direct Distance & Components
     */
    fun computeDistance(p1: Point2D, p2: Point2D, config: UnitConfig): MeasurementComputationResult {
        val distCad = p1.distanceTo(p2)
        val dxCad = abs(p2.x - p1.x)
        val dyCad = abs(p2.y - p1.y)
        val signedDx = p2.x - p1.x
        val signedDy = p2.y - p1.y
        val angleDeg = Math.toDegrees(atan2(signedDy.toDouble(), signedDx.toDouble())).toFloat()
        val normAngle = if (angleDeg < 0) angleDeg + 360f else angleDeg

        val distStr = UnitManager.formatDistance(distCad, config)
        val dxStr = UnitManager.formatDistance(dxCad, config)
        val dyStr = UnitManager.formatDistance(dyCad, config)
        val angleStr = UnitManager.formatAngle(normAngle, config)

        val details = linkedMapOf(
            "Direct Distance" to distStr,
            "Horizontal (ΔX)" to dxStr,
            "Vertical (ΔY)" to dyStr,
            "Bearing / Angle" to angleStr,
            "P1" to "(%.2f, %.2f)".format(p1.x, p1.y),
            "P2" to "(%.2f, %.2f)".format(p2.x, p2.y)
        )

        val mid = Point2D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

        return MeasurementComputationResult(
            primaryValue = distCad.toDouble(),
            primaryFormatted = distStr,
            subValueFormatted = "ΔX: $dxStr • ΔY: $dyStr • $angleStr",
            details = details,
            labelPosition = mid,
            isComplete = true
        )
    }

    /**
     * 2. Continuous / Polyline Distance
     */
    fun computeContinuous(points: List<Point2D>, config: UnitConfig): MeasurementComputationResult {
        if (points.size < 2) {
            return MeasurementComputationResult(
                primaryValue = 0.0,
                primaryFormatted = "0.00 ${config.displayDistanceUnit.symbol}",
                subValueFormatted = "Tap next point to accumulate distance",
                isComplete = false
            )
        }

        var totalCad = 0f
        val details = linkedMapOf<String, String>()

        for (i in 0 until points.size - 1) {
            val segDist = points[i].distanceTo(points[i + 1])
            totalCad += segDist
            details["Segment ${i + 1} (P${i + 1}→P${i + 2})"] = UnitManager.formatDistance(segDist, config)
        }

        val totalStr = UnitManager.formatDistance(totalCad, config)
        details["Total Cumulative Distance"] = totalStr
        details["Total Points"] = "${points.size}"

        val lastP = points.last()

        return MeasurementComputationResult(
            primaryValue = totalCad.toDouble(),
            primaryFormatted = totalStr,
            subValueFormatted = "${points.size} pts • ${points.size - 1} segments",
            details = details,
            labelPosition = lastP,
            isComplete = true
        )
    }

    /**
     * 3. Horizontal Distance
     */
    fun computeHorizontal(p1: Point2D, p2: Point2D, config: UnitConfig): MeasurementComputationResult {
        val dxCad = abs(p2.x - p1.x)
        val dxStr = UnitManager.formatDistance(dxCad, config)
        val details = linkedMapOf(
            "Horizontal Distance (ΔX)" to dxStr,
            "X1" to UnitManager.formatRawNumber(p1.x.toDouble(), config.precision),
            "X2" to UnitManager.formatRawNumber(p2.x.toDouble(), config.precision)
        )
        val mid = Point2D((p1.x + p2.x) / 2f, p1.y)

        return MeasurementComputationResult(
            primaryValue = dxCad.toDouble(),
            primaryFormatted = dxStr,
            subValueFormatted = "ΔX = |%.2f - %.2f|".format(p2.x, p1.x),
            details = details,
            labelPosition = mid,
            isComplete = true
        )
    }

    /**
     * 4. Vertical Distance
     */
    fun computeVertical(p1: Point2D, p2: Point2D, config: UnitConfig): MeasurementComputationResult {
        val dyCad = abs(p2.y - p1.y)
        val dyStr = UnitManager.formatDistance(dyCad, config)
        val details = linkedMapOf(
            "Vertical Distance (ΔY)" to dyStr,
            "Y1" to UnitManager.formatRawNumber(p1.y.toDouble(), config.precision),
            "Y2" to UnitManager.formatRawNumber(p2.y.toDouble(), config.precision)
        )
        val mid = Point2D(p1.x, (p1.y + p2.y) / 2f)

        return MeasurementComputationResult(
            primaryValue = dyCad.toDouble(),
            primaryFormatted = dyStr,
            subValueFormatted = "ΔY = |%.2f - %.2f|".format(p2.y, p1.y),
            details = details,
            labelPosition = mid,
            isComplete = true
        )
    }

    /**
     * 5. Angle Measurement (P1 -> Vertex -> P3)
     */
    fun computeAngle(p1: Point2D, vertex: Point2D, p3: Point2D, config: UnitConfig): MeasurementComputationResult {
        val v1x = (p1.x - vertex.x).toDouble()
        val v1y = (p1.y - vertex.y).toDouble()
        val v2x = (p3.x - vertex.x).toDouble()
        val v2y = (p3.y - vertex.y).toDouble()

        val len1 = hypot(v1x, v1y)
        val len2 = hypot(v2x, v2y)

        val angleDeg = if (len1 > 0.0001 && len2 > 0.0001) {
            val dot = v1x * v2x + v1y * v2y
            val cosTheta = (dot / (len1 * len2)).coerceIn(-1.0, 1.0)
            Math.toDegrees(acos(cosTheta)).toFloat()
        } else 0f

        val angleStr = UnitManager.formatAngle(angleDeg, config)
        val radStr = String.format(Locale.US, "%.4f rad", Math.toRadians(angleDeg.toDouble()))

        val details = linkedMapOf(
            "Included Angle" to angleStr,
            "Angle in Radians" to radStr,
            "Vertex" to "(%.2f, %.2f)".format(vertex.x, vertex.y),
            "Arm 1 Length" to UnitManager.formatDistance(len1.toFloat(), config),
            "Arm 2 Length" to UnitManager.formatDistance(len2.toFloat(), config)
        )

        return MeasurementComputationResult(
            primaryValue = angleDeg.toDouble(),
            primaryFormatted = angleStr,
            subValueFormatted = "$radStr • Vertex at (%.1f, %.1f)".format(vertex.x, vertex.y),
            details = details,
            labelPosition = vertex,
            isComplete = true
        )
    }

    /**
     * 6, 7, 8. Circle Measurement (Radius, Diameter, Area, Circumference)
     */
    fun computeCircle(center: Point2D, radius: Float, config: UnitConfig): MeasurementComputationResult {
        val diameter = radius * 2f
        val circumference = (2 * Math.PI * radius).toFloat()
        val area = (Math.PI * radius * radius).toFloat()

        val rStr = UnitManager.formatDistance(radius, config)
        val dStr = UnitManager.formatDistance(diameter, config)
        val cStr = UnitManager.formatDistance(circumference, config)
        val aStr = UnitManager.formatArea(area, config)

        val details = linkedMapOf(
            "Radius (R)" to rStr,
            "Diameter (⌀)" to dStr,
            "Circumference (C)" to cStr,
            "Area (A)" to aStr,
            "Center X" to UnitManager.formatRawNumber(center.x.toDouble(), config.precision),
            "Center Y" to UnitManager.formatRawNumber(center.y.toDouble(), config.precision)
        )

        return MeasurementComputationResult(
            primaryValue = radius.toDouble(),
            primaryFormatted = "R: $rStr (⌀ $dStr)",
            subValueFormatted = "Area: $aStr • Perim: $cStr",
            details = details,
            labelPosition = center,
            isComplete = true
        )
    }

    /**
     * 9. Arc Measurement (Radius, Diameter, Arc Length, Center, Angles)
     */
    fun computeArc(
        center: Point2D,
        radius: Float,
        startDeg: Float,
        endDeg: Float,
        config: UnitConfig
    ): MeasurementComputationResult {
        val sweepDeg = if (endDeg >= startDeg) endDeg - startDeg else (360f - startDeg + endDeg)
        val sweepRad = Math.toRadians(sweepDeg.toDouble())
        val arcLength = (radius * sweepRad).toFloat()
        val diameter = radius * 2f

        val rStr = UnitManager.formatDistance(radius, config)
        val dStr = UnitManager.formatDistance(diameter, config)
        val lStr = UnitManager.formatDistance(arcLength, config)
        val sweepStr = UnitManager.formatAngle(sweepDeg, config)

        val details = linkedMapOf(
            "Arc Length" to lStr,
            "Radius (R)" to rStr,
            "Diameter (⌀)" to dStr,
            "Included Angle" to sweepStr,
            "Start Angle" to "%.2f°".format(startDeg),
            "End Angle" to "%.2f°".format(endDeg),
            "Center" to "(%.2f, %.2f)".format(center.x, center.y)
        )

        val midAngle = Math.toRadians(((startDeg + sweepDeg / 2f) % 360f).toDouble())
        val arcMid = Point2D(
            (center.x + radius * cos(midAngle)).toFloat(),
            (center.y + radius * sin(midAngle)).toFloat()
        )

        return MeasurementComputationResult(
            primaryValue = arcLength.toDouble(),
            primaryFormatted = "Length: $lStr",
            subValueFormatted = "R: $rStr • θ: $sweepStr",
            details = details,
            labelPosition = arcMid,
            isComplete = true
        )
    }

    /**
     * 10, 12. Polygon Area & Perimeter
     */
    fun computePolygonArea(vertices: List<Point2D>, config: UnitConfig): MeasurementComputationResult {
        if (vertices.size < 3) {
            val perim = BoundaryDetector.computePerimeter(vertices, isClosed = false)
            return MeasurementComputationResult(
                primaryValue = 0.0,
                primaryFormatted = "0.00 ${config.displayAreaUnit.symbol}",
                subValueFormatted = "${vertices.size} pts • Tap at least 3 points",
                isComplete = false
            )
        }

        val areaCad = BoundaryDetector.computePolygonArea(vertices)
        val perimCad = BoundaryDetector.computePerimeter(vertices, isClosed = true)

        val areaStr = UnitManager.formatArea(areaCad, config)
        val perimStr = UnitManager.formatDistance(perimCad, config)

        val details = linkedMapOf(
            "Enclosed Area" to areaStr,
            "Perimeter" to perimStr,
            "Vertices Count" to "${vertices.size}"
        )

        // Centroid approximation
        var cx = 0f
        var cy = 0f
        for (v in vertices) {
            cx += v.x
            cy += v.y
        }
        val center = Point2D(cx / vertices.size, cy / vertices.size)

        return MeasurementComputationResult(
            primaryValue = areaCad.toDouble(),
            primaryFormatted = "Area: $areaStr",
            subValueFormatted = "Perimeter: $perimStr • ${vertices.size} vertices",
            details = details,
            labelPosition = center,
            isComplete = true
        )
    }

    /**
     * 11. Rectangle Area (Opposite corners)
     */
    fun computeRectangleArea(p1: Point2D, p2: Point2D, config: UnitConfig): MeasurementComputationResult {
        val width = abs(p2.x - p1.x)
        val height = abs(p2.y - p1.y)
        val areaCad = width * height
        val perimCad = 2 * (width + height)

        val wStr = UnitManager.formatDistance(width, config)
        val hStr = UnitManager.formatDistance(height, config)
        val aStr = UnitManager.formatArea(areaCad, config)
        val pStr = UnitManager.formatDistance(perimCad, config)

        val details = linkedMapOf(
            "Rectangular Area" to aStr,
            "Perimeter" to pStr,
            "Width (ΔX)" to wStr,
            "Height (ΔY)" to hStr,
            "Corner 1" to "(%.2f, %.2f)".format(p1.x, p1.y),
            "Corner 2" to "(%.2f, %.2f)".format(p2.x, p2.y)
        )

        val center = Point2D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

        return MeasurementComputationResult(
            primaryValue = areaCad.toDouble(),
            primaryFormatted = "Area: $aStr",
            subValueFormatted = "W: $wStr • H: $hStr • Perim: $pStr",
            details = details,
            labelPosition = center,
            isComplete = true
        )
    }

    /**
     * 14. Coordinate Tool (Single Point Absolute / Relative)
     */
    fun computeCoordinate(p: Point2D, z: Float = 0f, config: UnitConfig, relativeBase: Point2D? = null): MeasurementComputationResult {
        val absX = UnitManager.formatDistance(p.x, config)
        val absY = UnitManager.formatDistance(p.y, config)
        val absZ = UnitManager.formatDistance(z, config)

        val details = linkedMapOf(
            "Absolute X" to absX,
            "Absolute Y" to absY,
            "Absolute Z (Elevation)" to absZ
        )

        if (relativeBase != null) {
            val relX = UnitManager.formatDistance(p.x - relativeBase.x, config)
            val relY = UnitManager.formatDistance(p.y - relativeBase.y, config)
            val relDist = UnitManager.formatDistance(p.distanceTo(relativeBase), config)
            details["Relative @X"] = relX
            details["Relative @Y"] = relY
            details["Distance from Base"] = relDist
        }

        val summary = UnitManager.formatCoordinate(p, z, config, relativeBase)

        return MeasurementComputationResult(
            primaryValue = hypot(p.x.toDouble(), p.y.toDouble()),
            primaryFormatted = summary,
            subValueFormatted = "X: $absX, Y: $absY, Z: $absZ",
            details = details,
            labelPosition = p,
            isComplete = true
        )
    }

    /**
     * 15. Point-to-Point Coordinate Difference
     */
    fun computeCoordinateDifference(p1: Point2D, p2: Point2D, z1: Float = 0f, z2: Float = 0f, config: UnitConfig): MeasurementComputationResult {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dz = z2 - z1
        val dist2D = p1.distanceTo(p2)
        val dist3D = sqrt(dist2D * dist2D + dz * dz)
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val normAngle = if (angleDeg < 0) angleDeg + 360f else angleDeg

        val dxStr = UnitManager.formatDistance(dx, config)
        val dyStr = UnitManager.formatDistance(dy, config)
        val dzStr = UnitManager.formatDistance(dz, config)
        val distStr = UnitManager.formatDistance(dist2D, config)
        val dist3DStr = UnitManager.formatDistance(dist3D, config)
        val angleStr = UnitManager.formatAngle(normAngle, config)

        val details = linkedMapOf(
            "ΔX (X2 - X1)" to dxStr,
            "ΔY (Y2 - Y1)" to dyStr,
            "ΔZ (Elevation Diff)" to dzStr,
            "2D Plan Distance" to distStr,
            "3D Spatial Distance" to dist3DStr,
            "Bearing Angle" to angleStr
        )

        val mid = Point2D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

        return MeasurementComputationResult(
            primaryValue = dist2D.toDouble(),
            primaryFormatted = "ΔX: $dxStr | ΔY: $dyStr",
            subValueFormatted = "Dist: $distStr • $angleStr",
            details = details,
            labelPosition = mid,
            isComplete = true
        )
    }

    /**
     * 26. Total Area & Net Area Aggregator (Sum multiple regions, subtract inner holes)
     */
    fun computeTotalAndNetArea(records: List<MeasurementRecord>, config: UnitConfig): NetAreaCalculation {
        var grossArea = 0.0
        var holesArea = 0.0
        var totalPerimeter = 0.0
        var regionsCount = 0
        var holesCount = 0

        for (rec in records) {
            val isAreaTool = rec.toolType in listOf(
                MeasureToolType.AREA_POLYGON,
                MeasureToolType.AREA_RECTANGLE,
                MeasureToolType.AREA_BOUNDARY,
                MeasureToolType.CIRCLE
            )
            if (!isAreaTool) continue

            val areaVal = rec.primaryValue.toFloat()
            val perimStr = rec.secondaryDetails["Perimeter"] ?: rec.secondaryDetails["Circumference (C)"]
            val perimVal = perimStr?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0

            if (rec.isHoleSubtraction) {
                holesArea += areaVal
                holesCount++
            } else {
                grossArea += areaVal
                regionsCount++
                totalPerimeter += perimVal
            }
        }

        val netArea = (grossArea - holesArea).coerceAtLeast(0.0)

        return NetAreaCalculation(
            grossArea = grossArea,
            grossAreaFormatted = UnitManager.formatArea(grossArea.toFloat(), config),
            holesArea = holesArea,
            holesAreaFormatted = UnitManager.formatArea(holesArea.toFloat(), config),
            netArea = netArea,
            netAreaFormatted = UnitManager.formatArea(netArea.toFloat(), config),
            totalPerimeter = totalPerimeter,
            totalPerimeterFormatted = String.format(Locale.US, "%.2f %s", totalPerimeter, config.displayDistanceUnit.symbol),
            regionsCount = regionsCount,
            holesCount = holesCount
        )
    }
}
