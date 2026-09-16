package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.measurement.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementListSheet(
    records: List<MeasurementRecord>,
    unitConfig: UnitConfig,
    fileName: String,
    onDismiss: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleHole: (String) -> Unit,
    onZoomTo: (MeasurementRecord) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val netAreaCalc = remember(records, unitConfig) {
        MeasurementEngine.computeTotalAndNetArea(records, unitConfig)
    }
    var exportMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF4B5563)) },
        modifier = modifier.testTag("measurement_list_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Measurements (${records.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Export Menu
                    Box {
                        FilledTonalButton(
                            onClick = { exportMenuExpanded = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF60A5FA)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("btn_export_measurements")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF1F2937))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export CSV (.csv)", color = Color.White, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Description, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    exportMenuExpanded = false
                                    val csv = MeasurementExporter.exportToCsv(records, unitConfig)
                                    MeasurementExporter.shareExport(context, csv, "Export Measurements CSV", "text/csv")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export JSON (.json)", color = Color.White, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Code, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    exportMenuExpanded = false
                                    val json = MeasurementExporter.exportToJson(records, unitConfig)
                                    MeasurementExporter.shareExport(context, json, "Export Measurements JSON", "application/json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Report (.txt)", color = Color.White, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Article, null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    exportMenuExpanded = false
                                    val report = MeasurementExporter.exportToFormattedReport(records, unitConfig, fileName)
                                    MeasurementExporter.shareExport(context, report, "Export CAD Measurement Report", "text/plain")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (records.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.size(32.dp).testTag("btn_clear_all_measurements")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total & Net Area summary box (if area measurements exist)
            if (netAreaCalc.regionsCount > 0) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL / NET AREA AGGREGATOR",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                color = Color(0xFF0369A1),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${netAreaCalc.regionsCount} regions" + (if (netAreaCalc.holesCount > 0) " (${netAreaCalc.holesCount} holes)" else ""),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Gross Area", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Text(
                                    text = netAreaCalc.grossAreaFormatted,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (netAreaCalc.holesCount > 0) {
                                Column {
                                    Text("Holes Subtracted", color = Color(0xFFF87171), fontSize = 10.sp)
                                    Text(
                                        text = "-${netAreaCalc.holesAreaFormatted}",
                                        color = Color(0xFFF87171),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Column {
                                Text("NET AREA", color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = netAreaCalc.netAreaFormatted,
                                    color = Color(0xFF4ADE80),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Records List
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved measurements yet.\nUse any measurement tool on canvas and tap 'Save'.",
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        MeasurementItemCard(
                            record = record,
                            onToggleVisibility = { onToggleVisibility(record.id) },
                            onToggleHole = { onToggleHole(record.id) },
                            onZoomTo = { onZoomTo(record) },
                            onDelete = { onDelete(record.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MeasurementItemCard(
    record: MeasurementRecord,
    onToggleVisibility: () -> Unit,
    onToggleHole: () -> Unit,
    onZoomTo: () -> Unit,
    onDelete: () -> Unit
) {
    val isArea = record.toolType in listOf(
        MeasureToolType.AREA_POLYGON,
        MeasureToolType.AREA_RECTANGLE,
        MeasureToolType.AREA_BOUNDARY,
        MeasureToolType.CIRCLE
    )

    Surface(
        color = if (record.isHoleSubtraction) Color(0xFF2A1B24) else Color(0xFF1F2937),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (record.isHoleSubtraction) Color(0xFF831843) else Color(0xFF374151)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(record.toolType.iconEmoji, fontSize = 14.sp)
                    Text(
                        text = record.id,
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Value and Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Zoom To
                    IconButton(
                        onClick = onZoomTo,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Zoom to",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Eye visibility
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (record.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle visibility",
                            tint = if (record.isVisible) Color(0xFF4ADE80) else Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.primaryFormatted,
                    color = if (record.isHoleSubtraction) Color(0xFFF472B6) else Color(0xFFFF4081),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                if (isArea) {
                    FilterChip(
                        selected = record.isHoleSubtraction,
                        onClick = onToggleHole,
                        label = {
                            Text(
                                text = if (record.isHoleSubtraction) "Inner Void / Hole" else "+ Add Area",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF831843),
                            selectedLabelColor = Color(0xFFFBCFE8),
                            containerColor = Color(0xFF374151),
                            labelColor = Color(0xFF9CA3AF)
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            if (record.secondaryDetails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val detailStr = record.secondaryDetails.entries.take(3).joinToString(" • ") { "${it.key}: ${it.value}" }
                Text(
                    text = detailStr,
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
