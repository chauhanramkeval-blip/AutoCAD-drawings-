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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parser.Point2D
import com.example.store.CadTool

@Composable
fun CadBottomBar(
    activeTool: CadTool,
    cursorCoords: Point2D,
    zoomScale: Float,
    showGrid: Boolean,
    showAxes: Boolean,
    visibleLayerCount: Int,
    totalLayerCount: Int,
    onSetTool: (CadTool) -> Unit,
    onOpenLayers: () -> Unit,
    onFitExtents: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleAxes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0B192C),
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("cad_bottom_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Status & Quick Controls strip above navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF071220))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Live CAD Coordinates
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "X: %.1f  Y: %.1f".format(cursorCoords.x, cursorCoords.y),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF90A4AE),
                            fontSize = 11.sp
                        )
                    )
                }

                // Quick Viewport Actions (Grid, Axes, Zoom -, Zoom +, Fit)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Zoom Percentage
                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF78909C),
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Zoom Out
                    IconButton(
                        onClick = onZoomOut,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_bottom_zoom_out")
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Zoom In
                    IconButton(
                        onClick = onZoomIn,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_bottom_zoom_in")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Fit Button
                    IconButton(
                        onClick = onFitExtents,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_bottom_fit")
                    ) {
                        Icon(
                            Icons.Default.FitScreen,
                            contentDescription = "Fit Extents",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Grid Toggle Icon
                    IconButton(
                        onClick = onToggleGrid,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_bottom_grid")
                    ) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = "Toggle Grid",
                            tint = if (showGrid) Color(0xFF00E5FF) else Color(0x44FFFFFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Axes Toggle Icon
                    IconButton(
                        onClick = onToggleAxes,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_bottom_axes")
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = "Toggle Axes",
                            tint = if (showAxes) Color(0xFF69F0AE) else Color(0x44FFFFFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)

            // Primary M3 Navigation Bar
            NavigationBar(
                containerColor = Color(0xFF0B192C),
                contentColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
            ) {
                // 1. PAN / ZOOM
                NavigationBarItem(
                    selected = activeTool == CadTool.PAN_ZOOM,
                    onClick = { onSetTool(CadTool.PAN_ZOOM) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = "Pan Tool"
                        )
                    },
                    label = { Text("Pan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B192C),
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00E5FF),
                        unselectedIconColor = Color(0xFF90CAF9),
                        unselectedTextColor = Color(0xFF90CAF9)
                    ),
                    modifier = Modifier.testTag("nav_pan")
                )

                // 2. MARQUEE BOX SELECT
                NavigationBarItem(
                    selected = activeTool == CadTool.MARQUEE_SELECT,
                    onClick = { onSetTool(CadTool.MARQUEE_SELECT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CropSquare,
                            contentDescription = "Marquee Select Tool"
                        )
                    },
                    label = { Text("Marquee", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B192C),
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00E5FF),
                        unselectedIconColor = Color(0xFF90CAF9),
                        unselectedTextColor = Color(0xFF90CAF9)
                    ),
                    modifier = Modifier.testTag("nav_marquee")
                )

                // 3. INSPECT SINGLE
                NavigationBarItem(
                    selected = activeTool == CadTool.SELECT,
                    onClick = { onSetTool(CadTool.SELECT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Inspect Tool"
                        )
                    },
                    label = { Text("Inspect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B192C),
                        selectedTextColor = Color(0xFFFFD600),
                        indicatorColor = Color(0xFFFFD600),
                        unselectedIconColor = Color(0xFF90CAF9),
                        unselectedTextColor = Color(0xFF90CAF9)
                    ),
                    modifier = Modifier.testTag("nav_inspect")
                )

                // 4. MEASURE
                NavigationBarItem(
                    selected = activeTool == CadTool.MEASURE,
                    onClick = { onSetTool(CadTool.MEASURE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = "Measure Tool"
                        )
                    },
                    label = { Text("Measure", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFFFF4081),
                        indicatorColor = Color(0xFFFF4081),
                        unselectedIconColor = Color(0xFF90CAF9),
                        unselectedTextColor = Color(0xFF90CAF9)
                    ),
                    modifier = Modifier.testTag("nav_measure")
                )

                // 5. LAYERS
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenLayers,
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(0xFFFFD54F),
                                    contentColor = Color(0xFF0B192C)
                                ) {
                                    Text("$visibleLayerCount/$totalLayerCount", fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Layers Manager"
                            )
                        }
                    },
                    label = { Text("Layers", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color(0xFF90CAF9),
                        unselectedTextColor = Color(0xFF90CAF9)
                    ),
                    modifier = Modifier.testTag("nav_layers")
                )
            }
        }
    }
}
