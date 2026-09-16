package com.example.measurement

import androidx.compose.ui.graphics.Color
import com.example.parser.Point2D
import java.util.UUID

enum class MeasureToolType(
    val title: String,
    val iconEmoji: String,
    val description: String,
    val requiresPointsCount: Int // -1 for dynamic/poly, 1 for coord/entity, 2 for dist, 3 for angle
) {
    DISTANCE("Distance", "📏", "Measure point-to-point direct, horizontal & vertical distance", 2),
    CONTINUOUS("Continuous", "↔", "Measure multi-point cumulative polyline distance", -1),
    HORIZONTAL("Horizontal", "↔", "Measure horizontal X-axis distance (ΔX)", 2),
    VERTICAL("Vertical", "↕", "Measure vertical Y-axis distance (ΔY)", 2),
    ANGLE("Angle", "📐", "Measure vertex angle between two vectors", 3),
    RADIUS("Radius", "⭕", "Select circle or arc to measure radius & circumference", 1),
    DIAMETER("Diameter", "⌀", "Select circle or arc to measure diameter (2R)", 1),
    CIRCLE("Circle", "🔵", "Measure circle center, radius, diameter, circumference & area", 1),
    ARC("Arc", "〰", "Measure arc radius, arc length, angles & center", 1),
    AREA_POLYGON("Polygon Area", "⬡", "Measure enclosed area and perimeter by tapping vertices", -1),
    AREA_RECTANGLE("Rectangle Area", "▭", "Measure rectangular area & perimeter from 2 corners", 2),
    AREA_BOUNDARY("Boundary Area", "🔲", "Auto-detect enclosed CAD region & calculate area", 1),
    COORDINATE("Coordinates", "📍", "Query absolute and relative X, Y, Z point coordinates", 1),
    COORD_DIFFERENCE("Coord Delta", "📏", "Measure ΔX, ΔY, ΔZ, distance & bearing angle", 2),
    ELEVATION("Elevation / Z", "📐", "Measure 3D elevation and height differences", 2),
    TOTAL_AREA("Total Area", "➕", "Sum multiple regions or subtract inner holes (Net Area)", -1)
}

data class MeasurementActiveState(
    val toolType: MeasureToolType = MeasureToolType.DISTANCE,
    val pickedPoints: List<Point2D> = emptyList(),
    val livePreviewPoint: Point2D? = null,
    val activeSnap: SnapResult? = null,
    val selectedEntityForMeasure: com.example.parser.DxfEntity? = null,
    val isFinished: Boolean = false,
    val dynamicDetails: Map<String, String> = emptyMap(),
    val primaryValueDisplay: String = "",
    val subValueDisplay: String = ""
)

data class MeasurementRecord(
    val id: String = "M-${UUID.randomUUID().toString().take(4).uppercase()}",
    val title: String,
    val toolType: MeasureToolType,
    val points: List<Point2D>,
    val primaryValue: Double,
    val primaryFormatted: String,
    val secondaryDetails: Map<String, String> = emptyMap(),
    val labelPosition: Point2D = points.firstOrNull() ?: Point2D(0f, 0f),
    val isVisible: Boolean = true,
    val isHoleSubtraction: Boolean = false,
    val color: Color = Color(0xFFFF4081),
    val timestamp: Long = System.currentTimeMillis()
)

data class NetAreaCalculation(
    val grossArea: Double = 0.0,
    val grossAreaFormatted: String = "",
    val holesArea: Double = 0.0,
    val holesAreaFormatted: String = "",
    val netArea: Double = 0.0,
    val netAreaFormatted: String = "",
    val totalPerimeter: Double = 0.0,
    val totalPerimeterFormatted: String = "",
    val regionsCount: Int = 0,
    val holesCount: Int = 0
)
