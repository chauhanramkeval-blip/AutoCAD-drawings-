package com.example.measurement

import com.example.parser.Point2D
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

enum class DistanceUnit(val symbol: String, val displayName: String, val toMeters: Double) {
    MILLIMETERS("mm", "Millimeters (mm)", 0.001),
    CENTIMETERS("cm", "Centimeters (cm)", 0.01),
    METERS("m", "Meters (m)", 1.0),
    KILOMETERS("km", "Kilometers (km)", 1000.0),
    INCHES("in", "Inches (in)", 0.0254),
    FEET("ft", "Feet (ft)", 0.3048),
    YARDS("yd", "Yards (yd)", 0.9144);

    companion object {
        fun fromSymbol(sym: String): DistanceUnit =
            entries.find { it.symbol.equals(sym, ignoreCase = true) } ?: METERS
    }
}

enum class AreaUnit(val symbol: String, val displayName: String, val toSquareMeters: Double) {
    SQ_MILLIMETERS("mm²", "Square Millimeters (mm²)", 0.000001),
    SQ_CENTIMETERS("cm²", "Square Centimeters (cm²)", 0.0001),
    SQ_METERS("m²", "Square Meters (m²)", 1.0),
    SQ_KILOMETERS("km²", "Square Kilometers (km²)", 1000000.0),
    SQ_INCHES("in²", "Square Inches (in²)", 0.00064516),
    SQ_FEET("ft²", "Square Feet (ft²)", 0.09290304),
    SQ_YARDS("yd²", "Square Yards (yd²)", 0.83612736);

    companion object {
        fun fromDistanceUnit(dist: DistanceUnit): AreaUnit {
            return when (dist) {
                DistanceUnit.MILLIMETERS -> SQ_MILLIMETERS
                DistanceUnit.CENTIMETERS -> SQ_CENTIMETERS
                DistanceUnit.METERS -> SQ_METERS
                DistanceUnit.KILOMETERS -> SQ_KILOMETERS
                DistanceUnit.INCHES -> SQ_INCHES
                DistanceUnit.FEET -> SQ_FEET
                DistanceUnit.YARDS -> SQ_YARDS
            }
        }
    }
}

enum class AngleUnit(val symbol: String, val displayName: String) {
    DEGREES("°", "Decimal Degrees (°)"),
    RADIANS("rad", "Radians (rad)"),
    GRADIANS("grad", "Gradians (grad)"),
    DMS("DMS", "Deg/Min/Sec (D°M'S\")")
}

data class UnitConfig(
    val baseDrawingUnit: DistanceUnit = DistanceUnit.MILLIMETERS,
    val displayDistanceUnit: DistanceUnit = DistanceUnit.MILLIMETERS,
    val displayAreaUnit: AreaUnit = AreaUnit.SQ_MILLIMETERS,
    val displayAngleUnit: AngleUnit = AngleUnit.DEGREES,
    val precision: Int = 2,
    val drawingScale: Float = 1.0f // 1 CAD unit = 1 base drawing unit * scale
) {
    val precisionPattern: String
        get() = when (precision) {
            0 -> "%.0f"
            1 -> "%.1f"
            2 -> "%.2f"
            3 -> "%.3f"
            4 -> "%.4f"
            else -> "%.2f"
        }
}

object UnitManager {

    fun convertDistance(
        cadValue: Float,
        fromBase: DistanceUnit,
        toTarget: DistanceUnit,
        scale: Float = 1.0f
    ): Double {
        val inBaseMeters = (cadValue.toDouble() * scale) * fromBase.toMeters
        return inBaseMeters / toTarget.toMeters
    }

    fun convertArea(
        cadAreaValue: Float,
        fromBase: DistanceUnit,
        toTarget: AreaUnit,
        scale: Float = 1.0f
    ): Double {
        val baseAreaUnit = AreaUnit.fromDistanceUnit(fromBase)
        val inSquareMeters = (cadAreaValue.toDouble() * scale * scale) * baseAreaUnit.toSquareMeters
        return inSquareMeters / toTarget.toSquareMeters
    }

    fun formatDistance(cadValue: Float, config: UnitConfig): String {
        val converted = convertDistance(cadValue, config.baseDrawingUnit, config.displayDistanceUnit, config.drawingScale)
        return String.format(Locale.US, "${config.precisionPattern} %s", converted, config.displayDistanceUnit.symbol)
    }

    fun formatArea(cadArea: Float, config: UnitConfig): String {
        val converted = convertArea(cadArea, config.baseDrawingUnit, config.displayAreaUnit, config.drawingScale)
        return String.format(Locale.US, "${config.precisionPattern} %s", converted, config.displayAreaUnit.symbol)
    }

    fun formatAngle(angleDeg: Float, config: UnitConfig): String {
        val norm = if (angleDeg < 0) angleDeg + 360f else angleDeg
        return when (config.displayAngleUnit) {
            AngleUnit.DEGREES -> String.format(Locale.US, "${config.precisionPattern}°", norm)
            AngleUnit.RADIANS -> {
                val rad = Math.toRadians(norm.toDouble())
                String.format(Locale.US, "${config.precisionPattern} rad", rad)
            }
            AngleUnit.GRADIANS -> {
                val grad = norm * (400.0 / 360.0)
                String.format(Locale.US, "${config.precisionPattern} grad", grad)
            }
            AngleUnit.DMS -> {
                val totalSec = (norm * 3600.0).roundToInt()
                val d = totalSec / 3600
                val m = (totalSec % 3600) / 60
                val s = totalSec % 60
                "$d°$m'$s\""
            }
        }
    }

    fun formatCoordinate(
        p: Point2D,
        z: Float = 0f,
        config: UnitConfig,
        relativeTo: Point2D? = null
    ): String {
        val targetP = if (relativeTo != null) Point2D(p.x - relativeTo.x, p.y - relativeTo.y) else p
        val xVal = convertDistance(targetP.x, config.baseDrawingUnit, config.displayDistanceUnit, config.drawingScale)
        val yVal = convertDistance(targetP.y, config.baseDrawingUnit, config.displayDistanceUnit, config.drawingScale)
        val zVal = convertDistance(z, config.baseDrawingUnit, config.displayDistanceUnit, config.drawingScale)

        val prefix = if (relativeTo != null) "@" else ""
        return String.format(
            Locale.US,
            "%sX: ${config.precisionPattern}  Y: ${config.precisionPattern}  Z: ${config.precisionPattern} %s",
            prefix,
            xVal,
            yVal,
            zVal,
            config.displayDistanceUnit.symbol
        )
    }

    fun formatRawNumber(value: Double, precision: Int): String {
        val pattern = when (precision) {
            0 -> "%.0f"
            1 -> "%.1f"
            2 -> "%.2f"
            3 -> "%.3f"
            4 -> "%.4f"
            else -> "%.2f"
        }
        return String.format(Locale.US, pattern, value)
    }
}
