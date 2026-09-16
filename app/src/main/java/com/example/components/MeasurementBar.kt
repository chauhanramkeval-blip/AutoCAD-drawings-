package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.store.MeasurementInfo

@Composable
fun MeasurementBar(
    measurement: MeasurementInfo,
    onClearPoints: () -> Unit,
    onExitMeasure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("measurement_bar"),
        colors = CardDefaults.cardColors(containerColor = Color(0xF21A1A2E)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Measurement Tool",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Row {
                    if (measurement.p1 != null) {
                        IconButton(
                            onClick = onClearPoints,
                            modifier = Modifier.size(32.dp).testTag("btn_clear_measure")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear points",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                    IconButton(
                        onClick = onExitMeasure,
                        modifier = Modifier.size(32.dp).testTag("btn_exit_measure")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit measure mode",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when {
                measurement.p1 == null -> {
                    Text(
                        text = "👉 Tap anywhere on the canvas to place point 1 (P1)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB0BEC5))
                    )
                }
                measurement.p2 == null -> {
                    Text(
                        text = "👉 Point 1 set at (%.1f, %.1f). Now tap point 2 (P2)".format(
                            measurement.p1.x, measurement.p1.y
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFD54F))
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33000000), shape = MaterialTheme.shapes.extraSmall)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DISTANCE", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF90CAF9), fontSize = 10.sp))
                            Text(
                                "%.2f".format(measurement.distance),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFFFF4081),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ΔX", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF90CAF9), fontSize = 10.sp))
                            Text(
                                "%.2f".format(measurement.deltaX),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ΔY", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF90CAF9), fontSize = 10.sp))
                            Text(
                                "%.2f".format(measurement.deltaY),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ANGLE", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF90CAF9), fontSize = 10.sp))
                            Text(
                                "%.1f°".format(measurement.angleDeg),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFFFD600),
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
