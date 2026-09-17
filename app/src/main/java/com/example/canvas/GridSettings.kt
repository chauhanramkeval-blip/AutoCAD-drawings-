package com.example.canvas

import com.example.measurement.DistanceUnit
import com.example.measurement.UnitConfig
import com.example.measurement.UnitManager
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

enum class GridSpacingMode(val displayName: String, val description: String) {
    ADAPTIVE("Adaptive (Dynamic Zoom)", "Automatically scales grid divisions smoothly as you zoom in/out"),
    FIXED("Fixed Spacing", "Locks grid lines to a precise user-defined measurement interval")
}

enum class GridStyle(val displayName: String, val iconLabel: String) {
    LINES("Solid Lines", "―"),
    DOTTED("Dotted / Dashed", "┄"),
    DOT_GRID("Point / Dot Matrix", "•••")
}

enum class GridSnapFrequency(val displayName: String, val shortLabel: String, val factor: Float) {
    OFF("Snap Off", "Off", 0f),
    EVERY_GRID("1x (Every Grid Line)", "1x", 1.0f),
    HALF_GRID("0.5x (Half-Grid Subdivisions)", "0.5x", 0.5f),
    QUARTER_GRID("0.25x (Quarter-Grid)", "0.25x", 0.25f),
    TENTH_GRID("0.1x (Tenth-Grid)", "0.1x", 0.1f),
    MAJOR_ONLY("Major Lines (Every N Lines)", "Major", 5.0f)
}

data class GridSettings(
    val isVisible: Boolean = true,
    val spacingMode: GridSpacingMode = GridSpacingMode.ADAPTIVE,
    val customSpacingCad: Float = 10f, // in base CAD units
    val majorInterval: Int = 5, // Every N grid lines is major
    val lineOpacity: Float = 0.25f, // 0.05f to 0.90f
    val majorOpacityMultiplier: Float = 2.2f, // Multiplier for major line opacity
    val style: GridStyle = GridStyle.LINES,
    val isSnapToGrid: Boolean = false,
    val snapFrequency: GridSnapFrequency = GridSnapFrequency.EVERY_GRID,
    val showCoordinateLabels: Boolean = false
) {
    /**
     * Calculates the active world step (distance between minor grid lines in CAD world units)
     */
    fun computeWorldGridStep(scale: Float): Float {
        return when (spacingMode) {
            GridSpacingMode.FIXED -> customSpacingCad.coerceAtLeast(0.0001f)
            GridSpacingMode.ADAPTIVE -> {
                val screenStep = 80f
                val rawWorldStep = screenStep / scale.coerceAtLeast(0.000001f)
                val magnitude = 10f.pow(floor(log10(rawWorldStep)))
                val normalized = rawWorldStep / magnitude
                when {
                    normalized < 2f -> 1f * magnitude
                    normalized < 5f -> 2f * magnitude
                    else -> 5f * magnitude
                }
            }
        }
    }

    /**
     * Calculates the world step for snap-to-grid.
     */
    fun computeSnapStep(scale: Float): Float {
        val baseGridStep = computeWorldGridStep(scale)
        val multiplier = if (snapFrequency == GridSnapFrequency.MAJOR_ONLY) {
            majorInterval.toFloat()
        } else if (snapFrequency.factor > 0f) {
            snapFrequency.factor
        } else {
            1.0f
        }
        return (baseGridStep * multiplier).coerceAtLeast(0.00001f)
    }

    /**
     * Formats current grid spacing for UI display with active UnitConfig.
     */
    fun formatCurrentSpacing(scale: Float, unitConfig: UnitConfig): String {
        val step = computeWorldGridStep(scale)
        return UnitManager.formatDistance(step, unitConfig)
    }

    /**
     * Formats current snap step for UI display with active UnitConfig.
     */
    fun formatCurrentSnapStep(scale: Float, unitConfig: UnitConfig): String {
        val snapStep = computeSnapStep(scale)
        return UnitManager.formatDistance(snapStep, unitConfig)
    }
}
