package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.measurement.*

@Composable
fun UnitSettingsDialog(
    unitConfig: UnitConfig,
    onUpdateUnitConfig: (UnitConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var baseUnit by remember { mutableStateOf(unitConfig.baseDrawingUnit) }
    var distUnit by remember { mutableStateOf(unitConfig.displayDistanceUnit) }
    var areaUnit by remember { mutableStateOf(unitConfig.displayAreaUnit) }
    var precision by remember { mutableStateOf(unitConfig.precision) }
    var scaleFactorStr by remember { mutableStateOf(unitConfig.drawingScale.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("unit_settings_dialog"),
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Units & Precision Settings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Precision Selector
                Text(
                    text = "DECIMAL PRECISION",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 1, 2, 3, 4).forEach { p ->
                        FilterChip(
                            selected = precision == p,
                            onClick = { precision = p },
                            label = { Text(when(p) { 0 -> "0"; 1 -> "0.0"; 2 -> "0.00"; 3 -> "0.000"; else -> "0.0000" }, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF334155),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f).height(32.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                // Distance Unit
                Text(
                    text = "DISTANCE UNIT",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                var distExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { distExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Text(distUnit.displayName, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = distExpanded,
                        onDismissRequest = { distExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DistanceUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    distUnit = unit
                                    areaUnit = AreaUnit.fromDistanceUnit(unit)
                                    distExpanded = false
                                }
                            )
                        }
                    }
                }

                // Area Unit
                Text(
                    text = "AREA UNIT",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                var areaExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { areaExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4ADE80))
                    ) {
                        Text(areaUnit.displayName, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = areaExpanded,
                        onDismissRequest = { areaExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        AreaUnit.entries.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    areaUnit = u
                                    areaExpanded = false
                                }
                            )
                        }
                    }
                }

                // Scale Multiplier
                Text(
                    text = "DRAWING SCALE FACTOR",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = scaleFactorStr,
                    onValueChange = { scaleFactorStr = it },
                    label = { Text("Scale (1 CAD unit = X units)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scale = scaleFactorStr.toFloatOrNull() ?: 1.0f
                    onUpdateUnitConfig(
                        unitConfig.copy(
                            baseDrawingUnit = baseUnit,
                            displayDistanceUnit = distUnit,
                            displayAreaUnit = areaUnit,
                            precision = precision,
                            drawingScale = scale.coerceAtLeast(0.0001f)
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}
