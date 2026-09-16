package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.measurement.MeasurementActiveState
import com.example.measurement.UnitConfig

@Composable
fun MeasurementResultPanel(
    activeState: MeasurementActiveState,
    unitConfig: UnitConfig,
    onSaveMeasurement: () -> Unit,
    onUndoLastPoint: () -> Unit,
    onFinishMultiPoint: () -> Unit,
    onClearCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("measurement_result_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2111827)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Tool type, Icon, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = activeState.toolType.iconEmoji,
                        fontSize = 16.sp
                    )
                    Text(
                        text = activeState.toolType.title.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFFFF4081),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    if (activeState.pickedPoints.isNotEmpty()) {
                        Surface(
                            color = Color(0xFF374151),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${activeState.pickedPoints.size} PTS",
                                color = Color(0xFF9CA3AF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Top right control buttons (Undo, Finish, Clear, Save)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (activeState.pickedPoints.size > 1) {
                        IconButton(
                            onClick = onUndoLastPoint,
                            modifier = Modifier.size(28.dp).testTag("btn_measure_undo_pt")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo point",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (activeState.pickedPoints.size >= 3 || (activeState.pickedPoints.size >= 2 && activeState.toolType.name.contains("CONTINUOUS"))) {
                        IconButton(
                            onClick = onFinishMultiPoint,
                            modifier = Modifier.size(28.dp).testTag("btn_measure_finish")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Finish measurement",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (activeState.isFinished || activeState.primaryValueDisplay.isNotBlank()) {
                        IconButton(
                            onClick = onSaveMeasurement,
                            modifier = Modifier.size(28.dp).testTag("btn_measure_save")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Save to measurement list",
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClearCurrent,
                        modifier = Modifier.size(28.dp).testTag("btn_measure_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear points",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Snap indicator badge if currently snapping
            val snap = activeState.activeSnap
            if (snap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF064E3B).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = snap.mode.symbol,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Snapped to ${snap.description} (%.2f, %.2f)".format(snap.point.x, snap.point.y),
                        color = Color(0xFF6EE7B7),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Main Primary Readout & Sub value
            if (activeState.primaryValueDisplay.isNotBlank()) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeState.primaryValueDisplay,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color(0xFFFF4081),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp
                                )
                            )

                            if (activeState.isFinished) {
                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "RECORDED",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (activeState.subValueDisplay.isNotBlank()) {
                            Text(
                                text = activeState.subValueDisplay,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            } else {
                // Interactive Guidance Hint based on tool type and points picked
                val hint = when {
                    activeState.pickedPoints.isEmpty() -> when (activeState.toolType) {
                        com.example.measurement.MeasureToolType.RADIUS,
                        com.example.measurement.MeasureToolType.DIAMETER,
                        com.example.measurement.MeasureToolType.CIRCLE,
                        com.example.measurement.MeasureToolType.ARC -> "👉 Tap on a Circle or Arc entity on the canvas"
                        com.example.measurement.MeasureToolType.AREA_BOUNDARY -> "👉 Tap inside an enclosed region or on a closed entity"
                        com.example.measurement.MeasureToolType.COORDINATE -> "👉 Tap any CAD point to read coordinates"
                        else -> "👉 Tap first point on the CAD drawing (P1)"
                    }
                    activeState.pickedPoints.size == 1 -> when (activeState.toolType) {
                        com.example.measurement.MeasureToolType.ANGLE -> "👉 Point 1 placed. Now tap the Vertex point"
                        com.example.measurement.MeasureToolType.CONTINUOUS,
                        com.example.measurement.MeasureToolType.AREA_POLYGON -> "👉 Point 1 placed. Tap next point (P2)"
                        else -> "👉 Point 1 placed. Tap second point (P2)"
                    }
                    activeState.pickedPoints.size == 2 && activeState.toolType == com.example.measurement.MeasureToolType.ANGLE ->
                        "👉 Vertex placed. Now tap third point (P3) to calculate angle"
                    else -> "👉 Tap next vertex point, or tap ✓ to finish"
                }

                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp
                    )
                )
            }

            // Detailed CAD Components Grid
            if (activeState.dynamicDetails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1F2937).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    activeState.dynamicDetails.entries.chunked(2).forEach { rowEntries ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEntries.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            if (rowEntries.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
