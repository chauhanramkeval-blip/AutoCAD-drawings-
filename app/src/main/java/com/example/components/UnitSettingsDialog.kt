package com.example.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.measurement.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSettingsDialog(
    unitConfig: UnitConfig,
    onUpdateUnitConfig: (UnitConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var unitSystem by remember { mutableStateOf(unitConfig.unitSystem) }
    var baseUnit by remember { mutableStateOf(unitConfig.baseDrawingUnit) }
    var distUnit by remember { mutableStateOf(unitConfig.displayDistanceUnit) }
    var areaUnit by remember { mutableStateOf(unitConfig.displayAreaUnit) }
    var precision by remember { mutableStateOf(unitConfig.precision) }
    var scaleFactorStr by remember { mutableStateOf(unitConfig.drawingScale.toString()) }

    // Live preview config based on current selections in dialog
    val previewConfig = remember(unitSystem, baseUnit, distUnit, areaUnit, precision, scaleFactorStr) {
        val scale = scaleFactorStr.toFloatOrNull() ?: 1.0f
        UnitConfig(
            unitSystem = unitSystem,
            baseDrawingUnit = baseUnit,
            displayDistanceUnit = distUnit,
            displayAreaUnit = areaUnit,
            precision = precision,
            drawingScale = scale.coerceAtLeast(0.0001f)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unit_settings_dialog"),
        containerColor = Color(0xFF0F172A), // Slate-900
        titleContentColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Global Units & Measurement",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Configure Metric / Imperial calculation system",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("btn_close_unit_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ============================================================
                // 1. PRIMARY SYSTEM SWITCHER (METRIC vs IMPERIAL)
                // ============================================================
                Text(
                    text = "MEASUREMENT SYSTEM",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // METRIC TAB
                    val isMetric = unitSystem == UnitSystem.METRIC
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                unitSystem = UnitSystem.METRIC
                                baseUnit = DistanceUnit.MILLIMETERS
                                distUnit = DistanceUnit.MILLIMETERS
                                areaUnit = AreaUnit.SQ_MILLIMETERS
                            }
                            .testTag("tab_metric_system"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMetric) Color(0xFF0284C7) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SquareFoot,
                                contentDescription = null,
                                tint = if (isMetric) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Metric (mm / m)",
                                    color = if (isMetric) Color.White else Color(0xFFCBD5E1),
                                    fontWeight = if (isMetric) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "mm, cm, m, km, m²",
                                    color = if (isMetric) Color(0xFFE0F2FE) else Color(0xFF64748B),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    // IMPERIAL TAB
                    val isImperial = unitSystem == UnitSystem.IMPERIAL
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                unitSystem = UnitSystem.IMPERIAL
                                baseUnit = DistanceUnit.INCHES
                                distUnit = DistanceUnit.INCHES
                                areaUnit = AreaUnit.SQ_INCHES
                            }
                            .testTag("tab_imperial_system"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isImperial) Color(0xFF0284C7) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = if (isImperial) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Imperial (in / ft)",
                                    color = if (isImperial) Color.White else Color(0xFFCBD5E1),
                                    fontWeight = if (isImperial) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "in, ft, yd, in², ft²",
                                    color = if (isImperial) Color(0xFFE0F2FE) else Color(0xFF64748B),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // ============================================================
                // 2. QUICK PRESET CHIPS
                // ============================================================
                Text(
                    text = "QUICK PRESETS",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UnitPreset.entries.forEach { preset ->
                        val isSelected = unitSystem == preset.system && distUnit == preset.distanceUnit && areaUnit == preset.areaUnit
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF0369A1) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    unitSystem = preset.system
                                    baseUnit = preset.baseUnit
                                    distUnit = preset.distanceUnit
                                    areaUnit = preset.areaUnit
                                }
                                .testTag("preset_${preset.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when(preset) {
                                        UnitPreset.METRIC_MM -> "Metric (mm)"
                                        UnitPreset.METRIC_M -> "Metric (m)"
                                        UnitPreset.IMPERIAL_IN -> "Imperial (in)"
                                        UnitPreset.IMPERIAL_FT -> "Imperial (ft)"
                                    },
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "${preset.distanceUnit.symbol} & ${preset.areaUnit.symbol}",
                                    color = if (isSelected) Color(0xFFBAE6FD) else Color(0xFF64748B),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                // ============================================================
                // 3. DISTANCE & AREA UNIT SELECTION
                // ============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Distance Unit
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DISTANCE UNIT",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        var distExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { distExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF38BDF8)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(distUnit.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = distExpanded,
                                onDismissRequest = { distExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                DistanceUnit.entries.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = unit.displayName,
                                                    color = if (distUnit == unit) Color(0xFF38BDF8) else Color.White,
                                                    fontWeight = if (distUnit == unit) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = if (unit.system == UnitSystem.METRIC) Color(0x330284C7) else Color(0x33F59E0B),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = if (unit.system == UnitSystem.METRIC) "M" else "IMP",
                                                        color = if (unit.system == UnitSystem.METRIC) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                                                        fontSize = 8.sp,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            distUnit = unit
                                            unitSystem = unit.system
                                            areaUnit = AreaUnit.fromDistanceUnit(unit)
                                            distExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Area Unit
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AREA UNIT",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        var areaExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { areaExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF4ADE80)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(areaUnit.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = areaExpanded,
                                onDismissRequest = { areaExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                AreaUnit.entries.forEach { u ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = u.displayName,
                                                    color = if (areaUnit == u) Color(0xFF4ADE80) else Color.White,
                                                    fontWeight = if (areaUnit == u) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = if (u.system == UnitSystem.METRIC) Color(0x330284C7) else Color(0x33F59E0B),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = if (u.system == UnitSystem.METRIC) "M" else "IMP",
                                                        color = if (u.system == UnitSystem.METRIC) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                                                        fontSize = 8.sp,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            areaUnit = u
                                            areaExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ============================================================
                // 4. DECIMAL PRECISION SELECTOR
                // ============================================================
                Text(
                    text = "DECIMAL PRECISION",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 1, 2, 3, 4).forEach { p ->
                        val isSelected = precision == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { precision = p },
                            label = {
                                Text(
                                    text = when(p) { 0 -> "0 (1)"; 1 -> "0.0"; 2 -> "0.00"; 3 -> "0.000"; else -> "0.0000" },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFF475569),
                                selectedBorderColor = Color(0xFF38BDF8)
                            ),
                            modifier = Modifier.weight(1f).height(34.dp).testTag("precision_$p")
                        )
                    }
                }

                // ============================================================
                // 5. BASE CAD UNIT & DRAWING SCALE FACTOR
                // ============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Base CAD unit
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BASE CAD UNIT",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        var baseExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { baseExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFFFCD34D)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("1 CAD = 1 ${baseUnit.symbol}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = baseExpanded,
                                onDismissRequest = { baseExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                DistanceUnit.entries.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text("1 CAD unit = 1 ${unit.displayName}", color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            baseUnit = unit
                                            baseExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Scale Multiplier
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DRAWING SCALE (1:X)",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = scaleFactorStr,
                            onValueChange = { scaleFactorStr = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        )
                    }
                }

                // ============================================================
                // 6. LIVE CONVERSION PREVIEW CARD
                // ============================================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF03162C)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("live_units_preview_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE SYSTEM CONVERSION PREVIEW",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = if (unitSystem == UnitSystem.METRIC) Color(0xFF0284C7) else Color(0xFFD97706),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = unitSystem.shortName,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sample Distance (1000 CAD units)
                        val sampleCadDist = 1000f
                        val distFormatted = UnitManager.formatDistance(sampleCadDist, previewConfig)
                        val dualDistFormatted = UnitManager.formatDualDistance(sampleCadDist, previewConfig)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sample Distance (1000 CAD):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(
                                text = dualDistFormatted,
                                color = Color(0xFFFFD600),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Sample Area (1,000,000 CAD units²)
                        val sampleCadArea = 1000000f
                        val dualAreaFormatted = UnitManager.formatDualArea(sampleCadArea, previewConfig)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sample Area (1,000,000 CAD²):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(
                                text = dualAreaFormatted,
                                color = Color(0xFF4ADE80),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scale = scaleFactorStr.toFloatOrNull() ?: 1.0f
                    val newConfig = UnitConfig(
                        unitSystem = unitSystem,
                        baseDrawingUnit = baseUnit,
                        displayDistanceUnit = distUnit,
                        displayAreaUnit = areaUnit,
                        precision = precision,
                        drawingScale = scale.coerceAtLeast(0.0001f)
                    )
                    onUpdateUnitConfig(newConfig)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("btn_save_unit_settings")
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply Globally (${if (unitSystem == UnitSystem.METRIC) "Metric" else "Imperial"})")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        // Reset to defaults
                        unitSystem = UnitSystem.METRIC
                        baseUnit = DistanceUnit.MILLIMETERS
                        distUnit = DistanceUnit.MILLIMETERS
                        areaUnit = AreaUnit.SQ_MILLIMETERS
                        precision = 2
                        scaleFactorStr = "1.0"
                    }
                ) {
                    Text("Reset", color = Color(0xFFF87171))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        }
    )
}
