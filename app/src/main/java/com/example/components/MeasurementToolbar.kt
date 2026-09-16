package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.measurement.MeasureToolType
import com.example.measurement.OrthoMode
import com.example.measurement.SnapSettings
import com.example.measurement.UnitConfig

@Composable
fun MeasurementToolbar(
    activeToolType: MeasureToolType,
    snapSettings: SnapSettings,
    unitConfig: UnitConfig,
    savedCount: Int,
    onSelectTool: (MeasureToolType) -> Unit,
    onToggleSnap: () -> Unit,
    onCycleOrtho: () -> Unit,
    onOpenSnapSettings: () -> Unit,
    onOpenUnitSettings: () -> Unit,
    onOpenMeasurementManager: () -> Unit,
    onCloseMeasureMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("measurement_toolbar"),
        color = Color(0xFF111827),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFF374151))
        ) {
            // Top Row: Category controls & quick utility toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFFF4081).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "CAD MEASURE",
                            color = Color(0xFFFF4081),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Ortho Quick Toggle
                    FilterChip(
                        selected = snapSettings.orthoMode != OrthoMode.OFF,
                        onClick = onCycleOrtho,
                        label = {
                            Text(
                                text = when (snapSettings.orthoMode) {
                                    OrthoMode.OFF -> "ORTHO: OFF"
                                    OrthoMode.HORIZONTAL -> "ORTHO: H"
                                    OrthoMode.VERTICAL -> "ORTHO: V"
                                    OrthoMode.AUTO_ORTHO -> "ORTHO: AUTO"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1F2937),
                            labelColor = Color(0xFF9CA3AF)
                        ),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("btn_toggle_ortho")
                    )

                    // Snap Settings Button
                    FilterChip(
                        selected = snapSettings.isEnabled,
                        onClick = onToggleSnap,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = if (snapSettings.isEnabled) Color(0xFF4ADE80) else Color(0xFF6B7280),
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (snapSettings.isEnabled) "OSNAP ON" else "OSNAP OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF065F46),
                            selectedLabelColor = Color(0xFF6EE7B7),
                            containerColor = Color(0xFF1F2937),
                            labelColor = Color(0xFF9CA3AF)
                        ),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("btn_toggle_osnap")
                    )
                }

                // Right Utility actions: Snap Config, Units, Saved List, Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Snap modes configure
                    IconButton(
                        onClick = onOpenSnapSettings,
                        modifier = Modifier.size(32.dp).testTag("btn_snap_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Snap Settings",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Unit settings
                    IconButton(
                        onClick = onOpenUnitSettings,
                        modifier = Modifier.size(32.dp).testTag("btn_unit_settings")
                    ) {
                        Surface(
                            color = Color(0xFF374151),
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = unitConfig.displayDistanceUnit.symbol,
                                    color = Color(0xFF60A5FA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Saved Measurements List
                    IconButton(
                        onClick = onOpenMeasurementManager,
                        modifier = Modifier.size(32.dp).testTag("btn_measurement_manager")
                    ) {
                        BadgedBox(
                            badge = {
                                if (savedCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFFF4081),
                                        contentColor = Color.White
                                    ) {
                                        Text("$savedCount", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "Saved Measurements",
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Exit Measurement Mode
                    IconButton(
                        onClick = onCloseMeasureMode,
                        modifier = Modifier.size(32.dp).testTag("btn_close_measure_toolbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Measure Mode",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x33374151), thickness = 1.dp)

            // Bottom Row: Scrollable Tool Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MeasureToolType.entries.forEach { tool ->
                    val isSelected = activeToolType == tool
                    Surface(
                        onClick = { onSelectTool(tool) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFFFF4081) else Color(0xFF1F2937),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("tool_measure_${tool.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = tool.iconEmoji,
                                fontSize = 13.sp
                            )
                            Text(
                                text = tool.title,
                                color = if (isSelected) Color.White else Color(0xFFE5E7EB),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
