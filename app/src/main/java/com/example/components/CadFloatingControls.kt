package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parser.Point2D
import com.example.store.CadTool

@Composable
fun CadFloatingControls(
    activeTool: CadTool,
    showGrid: Boolean,
    showAxes: Boolean,
    cursorCoords: Point2D,
    onSetTool: (CadTool) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitExtents: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleAxes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Top Center: Tool Mode Switcher Bar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("tool_mode_switcher"),
            shape = MaterialTheme.shapes.extraLarge,
            color = Color(0xEB132238),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pan / Navigate
                FilterChip(
                    selected = activeTool == CadTool.PAN_ZOOM,
                    onClick = { onSetTool(CadTool.PAN_ZOOM) },
                    label = { Text("Pan", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = "Pan/Zoom tool",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color(0xFF0B192C),
                        selectedLeadingIconColor = Color(0xFF0B192C)
                    ),
                    modifier = Modifier.testTag("tool_pan")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Select / Inspect
                FilterChip(
                    selected = activeTool == CadTool.SELECT,
                    onClick = { onSetTool(CadTool.SELECT) },
                    label = { Text("Inspect", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Select tool",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFD600),
                        selectedLabelColor = Color(0xFF0B192C),
                        selectedLeadingIconColor = Color(0xFF0B192C)
                    ),
                    modifier = Modifier.testTag("tool_select")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Measure
                FilterChip(
                    selected = activeTool == CadTool.MEASURE,
                    onClick = { onSetTool(CadTool.MEASURE) },
                    label = { Text("Measure", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = "Measure tool",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF4081),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    modifier = Modifier.testTag("tool_measure")
                )
            }
        }

        // Right side: Vertical Zoom & Display Controls
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("viewport_controls_stack"),
            shape = MaterialTheme.shapes.medium,
            color = Color(0xEB132238),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom In
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(42.dp).testTag("btn_zoom_in")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                }

                HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.width(28.dp))

                // Zoom Out
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(42.dp).testTag("btn_zoom_out")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                }

                HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.width(28.dp))

                // Fit Extents
                IconButton(
                    onClick = onFitExtents,
                    modifier = Modifier.size(42.dp).testTag("btn_fit_center")
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "Fit Extents", tint = Color(0xFF00E5FF))
                }

                HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.width(28.dp))

                // Toggle Grid
                IconButton(
                    onClick = onToggleGrid,
                    modifier = Modifier.size(42.dp).testTag("btn_toggle_grid")
                ) {
                    Icon(
                        Icons.Default.GridOn,
                        contentDescription = "Toggle Grid",
                        tint = if (showGrid) Color(0xFF00E5FF) else Color(0x55FFFFFF)
                    )
                }

                HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.width(28.dp))

                // Toggle Axes
                IconButton(
                    onClick = onToggleAxes,
                    modifier = Modifier.size(42.dp).testTag("btn_toggle_axes")
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = "Toggle Axes",
                        tint = if (showAxes) Color(0xFF69F0AE) else Color(0x55FFFFFF)
                    )
                }
            }
        }

        // Bottom Left: Live CAD Coordinates HUD
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .testTag("cad_coordinates_badge"),
            shape = MaterialTheme.shapes.small,
            color = Color(0xCC0B192C)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "X: %.1f   Y: %.1f".format(cursorCoords.x, cursorCoords.y),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
