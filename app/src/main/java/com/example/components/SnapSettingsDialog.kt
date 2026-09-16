package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.measurement.OrthoMode
import com.example.measurement.PolarTrackingMode
import com.example.measurement.SnapMode
import com.example.measurement.SnapSettings

@Composable
fun SnapSettingsDialog(
    snapSettings: SnapSettings,
    onUpdateSnapSettings: (SnapSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(snapSettings.isEnabled) }
    var activeModes by remember { mutableStateOf(snapSettings.activeModes.toMutableSet()) }
    var orthoMode by remember { mutableStateOf(snapSettings.orthoMode) }
    var polarMode by remember { mutableStateOf(snapSettings.polarMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("snap_settings_dialog"),
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CAD Object Snap (OSNAP)",
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
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Master OSNAP Switch
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Object Snapping (OSNAP)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }

                // Snap Modes Grid
                Text(
                    text = "SNAP MODES",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(SnapMode.entries) { mode ->
                        val isSelected = activeModes.contains(mode)
                        FilterChip(
                            selected = isSelected,
                            enabled = isEnabled,
                            onClick = {
                                if (isSelected) activeModes.remove(mode) else activeModes.add(mode)
                                activeModes = activeModes.toMutableSet()
                            },
                            leadingIcon = {
                                Text(mode.symbol, color = if (isSelected) Color(0xFF34D399) else Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                            },
                            label = {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF064E3B),
                                selectedLabelColor = Color(0xFF6EE7B7),
                                containerColor = Color(0xFF334155),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                // Ortho Mode & Polar Tracking
                Text(
                    text = "CONSTRAINTS & TRACKING",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ortho Mode Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ortho Mode", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA))
                            ) {
                                Text(orthoMode.displayName.take(12), fontSize = 11.sp, maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                OrthoMode.entries.forEach { om ->
                                    DropdownMenuItem(
                                        text = { Text(om.displayName, color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            orthoMode = om
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Polar Tracking Mode Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Polar Tracking", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFACC15))
                            ) {
                                Text(polarMode.displayName, fontSize = 11.sp, maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                PolarTrackingMode.entries.forEach { pm ->
                                    DropdownMenuItem(
                                        text = { Text(pm.displayName, color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            polarMode = pm
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateSnapSettings(
                        snapSettings.copy(
                            isEnabled = isEnabled,
                            activeModes = activeModes,
                            orthoMode = orthoMode,
                            polarMode = polarMode
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}
