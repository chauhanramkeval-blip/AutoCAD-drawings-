package com.example.measurement

import com.example.parser.Point2D
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

enum class UnitSystem(val displayName: String, val shortName: String, val description: String) {
    METRIC("Metric System", "Metric (mm/m)", "Millimeters, Centimeters, Meters, Kilometers (mm, cm, m, km, m²)"),
    IMPERIAL("Imperial System", "Imperial (in/ft)", "Inches, Feet, Yards (in, ft, yd, in², ft²)")
}

enum class DistanceUnit(
    val symbol: String,
    val displayName: String,
    val toMeters: Double,
    val system: UnitSystem
) {
    MILLIMETERS("mm", "Millimeters (mm)", 0.001, UnitSystem.METRIC),
    CENTIMETERS("cm", "Centimeters (cm)", 0.01, UnitSystem.METRIC),
    METERS("m", "Meters (m)", 1.0, UnitSystem.METRIC),
    KILOMETERS("km", "Kilometers (km)", 1000.0, UnitSystem.METRIC),
    INCHES("in", "Inches (in)", 0.0254, UnitSystem.IMPERIAL),
    FEET("ft", "Feet (ft)", 0.3048, UnitSystem.IMPERIAL),
    YARDS("yd", "Yards (yd)", 0.9144, UnitSystem.IMPERIAL);

    companion object {
        fun fromSymbol(sym: String): DistanceUnit =
            entries.find { it.symbol.equals(sym, ignoreCase = true) } ?: METERS

        fun forSystem(system: UnitSystem): List<DistanceUnit> =
            entries.filter { it.system == system }
    }
}

enum class AreaUnit(
    val symbol: String,
    val displayName: String,
    val toSquareMeters: Double,
    val system: UnitSystem
) {
    SQ_MILLIMETERS("mm²", "Square Millimeters (mm²)", 0.000001, UnitSystem.METRIC),
    SQ_CENTIMETERS("cm²", "Square Centimeters (cm²)", 0.0001, UnitSystem.METRIC),
    SQ_METERS("m²", "Square Meters (m²)", 1.0, UnitSystem.METRIC),
    SQ_KILOMETERS("km²", "Square Kilometers (km²)", 1000000.0, UnitSystem.METRIC),
    SQ_INCHES("in²", "Square Inches (in²)", 0.00064516, UnitSystem.IMPERIAL),
    SQ_FEET("ft²", "Square Feet (ft²)", 0.09290304, UnitSystem.IMPERIAL),
    SQ_YARDS("yd²", "Square Yards (yd²)", 0.83612736, UnitSystem.IMPERIAL);

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

        fun forSystem(system: UnitSystem): List<AreaUnit> =
            entries.filter { it.system == system }
    }
}

enum class AngleUnit(val symbol: String, val displayName: String) {
    DEGREES("°", "Decimal Degrees (°)"),
    RADIANS("rad", "Radians (rad)"),
    GRADIANS("grad", "Gradians (grad)"),
    DMS("DMS", "Deg/Min/Sec (D°M'S\")")
}

enum class UnitPreset(
    val title: String,
    val system: UnitSystem,
    val baseUnit: DistanceUnit,
    val distanceUnit: DistanceUnit,
    val areaUnit: AreaUnit,
    val description: String
) {
    METRIC_MM("Metric (mm / mm²)", UnitSystem.METRIC, DistanceUnit.MILLIMETERS, DistanceUnit.MILLIMETERS, AreaUnit.SQ_MILLIMETERS, "Millimeters (Mechanical & Fabrication)"),
    METRIC_M("Metric (m / m²)", UnitSystem.METRIC, DistanceUnit.MILLIMETERS, DistanceUnit.METERS, AreaUnit.SQ_METERS, "Meters (Architectural & Civil)"),
    IMPERIAL_IN("Imperial (in / in²)", UnitSystem.IMPERIAL, DistanceUnit.INCHES, DistanceUnit.INCHES, AreaUnit.SQ_INCHES, "Inches (Mechanical & Manufacturing)"),
    IMPERIAL_FT("Imperial (ft / ft²)", UnitSystem.IMPERIAL, DistanceUnit.INCHES, DistanceUnit.FEET, AreaUnit.SQ_FEET, "Feet (Architectural & Construction)")
}

data class UnitConfig(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
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

    fun withSystem(system: UnitSystem): UnitConfig {
        return if (system == UnitSystem.METRIC) {
            copy(
                unitSystem = UnitSystem.METRIC,
                baseDrawingUnit = DistanceUnit.MILLIMETERS,
                displayDistanceUnit = DistanceUnit.MILLIMETERS,
                displayAreaUnit = AreaUnit.SQ_MILLIMETERS
            )
        } else {
            copy(
                unitSystem = UnitSystem.IMPERIAL,
                baseDrawingUnit = DistanceUnit.INCHES,
                displayDistanceUnit = DistanceUnit.INCHES,
                displayAreaUnit = AreaUnit.SQ_INCHES
            )
        }
    }

    fun withPreset(preset: UnitPreset): UnitConfig {
        return copy(
            unitSystem = preset.system,
            baseDrawingUnit = preset.baseUnit,
            displayDistanceUnit = preset.distanceUnit,
            displayAreaUnit = preset.areaUnit
        )
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

    fun parseToCadUnits(
        displayValue: Float,
        baseUnit: DistanceUnit,
        displayUnit: DistanceUnit,
        scale: Float = 1.0f
    ): Float {
        val inMeters = displayValue.toDouble() * displayUnit.toMeters
        val baseVal = inMeters / baseUnit.toMeters
        return (baseVal / scale).toFloat()
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

    /**
     * Dual unit display: shows primary measurement + equivalent in opposite system (Metric <-> Imperial)
     */
    fun formatDualDistance(cadValue: Float, config: UnitConfig): String {
        val primary = formatDistance(cadValue, config)
        val altUnit = if (config.unitSystem == UnitSystem.METRIC) DistanceUnit.INCHES else DistanceUnit.MILLIMETERS
        val altConverted = convertDistance(cadValue, config.baseDrawingUnit, altUnit, config.drawingScale)
        val altStr = String.format(Locale.US, "${config.precisionPattern} %s", altConverted, altUnit.symbol)
        return "$primary ($altStr)"
    }

    fun formatDualArea(cadArea: Float, config: UnitConfig): String {
        val primary = formatArea(cadArea, config)
        val altAreaUnit = if (config.unitSystem == UnitSystem.METRIC) AreaUnit.SQ_FEET else AreaUnit.SQ_METERS
        val altConverted = convertArea(cadArea, config.baseDrawingUnit, altAreaUnit, config.drawingScale)
        val altStr = String.format(Locale.US, "${config.precisionPattern} %s", altConverted, altAreaUnit.symbol)
        return "$primary ($altStr)"
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
            "%sX: ${config.precisionPattern} %s, Y: ${config.precisionPattern} %s, Z: ${config.precisionPattern} %s",
            prefix,
            xVal,
            config.displayDistanceUnit.symbol,
            yVal,
            config.displayDistanceUnit.symbol,
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
