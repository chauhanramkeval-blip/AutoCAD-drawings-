package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.clickable
import com.example.measurement.UnitConfig
import com.example.measurement.UnitSystem

/**
 * Android Jetpack Compose implementation of TopNavbar component.
 * Mirrors the React specification with Top Header row (Title, DXF badge, Subtitle, Quick Actions)
 * and Menu Bar row (File, Edit, View, Tools, Layers).
 */
@Composable
fun TopNavbar(
    fileName: String,
    entityCount: Int,
    isDarkMode: Boolean,
    unitConfig: UnitConfig = UnitConfig(),
    onToggleTheme: () -> Unit,
    onSelectAction: (action: String, payload: Any?) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeMenu by remember { mutableStateOf<String?>(null) }
    val menuScrollState = rememberScrollState()

    Surface(
        color = Color(0xFF111827), // bg-gray-900
        contentColor = Color(0xFFE5E7EB), // text-gray-200
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_navbar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFF1F2937)) // border-gray-800
        ) {
            // ============================================================
            // 1. TOP HEADER ROW (Title, DXF Badge, Entity count, Quick Actions)
            // ============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Title & File Info
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CAD Viewer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                        )
                        Surface(
                            color = Color(0xFF2563EB).copy(alpha = 0.3f), // bg-blue-600/30
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DXF",
                                color = Color(0xFF60A5FA), // text-blue-400
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${if (fileName.isNotBlank()) fileName else "No file loaded"} • $entityCount entities",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9CA3AF), // text-gray-400
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Quick Action Icons from Screenshot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Global Units Badge Quick Button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (unitConfig.unitSystem == UnitSystem.METRIC) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFFD97706).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (unitConfig.unitSystem == UnitSystem.METRIC) Color(0xFF38BDF8) else Color(0xFFFBBF24)
                        ),
                        modifier = Modifier
                            .clickable { onSelectAction("measure-units-settings", null) }
                            .testTag("btn_quick_unit_settings")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = "Units Settings",
                                tint = if (unitConfig.unitSystem == UnitSystem.METRIC) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (unitConfig.unitSystem == UnitSystem.METRIC) "METRIC" else "IMP"} (${unitConfig.displayDistanceUnit.symbol})",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Import Image / Background
                    IconButton(
                        onClick = { onSelectAction("import-image", null) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_quick_import_image")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Import Image / Background",
                            tint = Color(0xFFD1D5DB), // text-gray-300
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Open DXF File
                    IconButton(
                        onClick = { onSelectAction("open-file", null) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_quick_open_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Open DXF File",
                            tint = Color(0xFFD1D5DB), // text-gray-300
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Fit to View (Maximize, blue-400)
                    IconButton(
                        onClick = { onSelectAction("fit-view", null) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_quick_fit_view")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen, // Maximize equivalent
                            contentDescription = "Fit to View",
                            tint = Color(0xFF60A5FA), // text-blue-400
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Layers Manager (yellow-400)
                    IconButton(
                        onClick = { onSelectAction("toggle-layers-panel", null) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_quick_layers")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Layers Manager",
                            tint = Color(0xFFFACC15), // text-yellow-400
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Toggle Theme (Sun / Moon)
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_quick_theme")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x33374151), thickness = 1.dp)

            // ============================================================
            // 2. MENU BAR ROW (File, Edit, View, Tools, Layers)
            // ============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(menuScrollState)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // FILE MENU
                Box {
                    NavMenuTab(
                        title = "File",
                        isActive = activeMenu == "file",
                        onClick = { activeMenu = if (activeMenu == "file") null else "file" }
                    )
                    DropdownMenu(
                        expanded = activeMenu == "file",
                        onDismissRequest = { activeMenu = null },
                        modifier = Modifier
                            .background(Color(0xFF111827)) // bg-gray-900
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open DXF...", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("open-file", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save / Export DXF", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("export-dxf", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as PNG", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("export-png", null)
                            }
                        )
                    }
                }

                // EDIT & MODIFY MENU
                Box {
                    NavMenuTab(
                        title = "Edit",
                        isActive = activeMenu == "edit",
                        onClick = { activeMenu = if (activeMenu == "edit") null else "edit" }
                    )
                    DropdownMenu(
                        expanded = activeMenu == "edit",
                        onDismissRequest = { activeMenu = null },
                        modifier = Modifier
                            .width(210.dp)
                            .background(Color(0xFF111827))
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "MODIFY TOOLS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280), // text-gray-500
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        DropdownMenuItem(
                            text = { Text("Move", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.OpenWith, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("tool-move", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rotate", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("tool-rotate", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Scale", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("tool-scale", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy / Offset", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("tool-copy", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Trim & Extend", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("tool-trim", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mirror", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Flip, contentDescription = null, tint = Color(0xFF22D3EE), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("tool-mirror", null)
                            }
                        )
                    }
                }

                // VIEW MENU
                Box {
                    NavMenuTab(
                        title = "View",
                        isActive = activeMenu == "view",
                        onClick = { activeMenu = if (activeMenu == "view") null else "view" }
                    )
                    DropdownMenu(
                        expanded = activeMenu == "view",
                        onDismissRequest = { activeMenu = null },
                        modifier = Modifier
                            .background(Color(0xFF111827))
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Grid Settings & Opacity...", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.GridOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("grid-settings", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Toggle Grid", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("toggle-grid", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Toggle Axes", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFF69F0AE), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("toggle-axes", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fit to Screen", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("fit-view", null)
                            }
                        )
                    }
                }

                // GRID MENU TAB
                Box {
                    NavMenuTab(
                        title = "Grid",
                        isActive = activeMenu == "grid",
                        onClick = { activeMenu = if (activeMenu == "grid") null else "grid" }
                    )
                    DropdownMenu(
                        expanded = activeMenu == "grid",
                        onDismissRequest = { activeMenu = null },
                        modifier = Modifier
                            .background(Color(0xFF111827))
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚙️ Grid & Snap Settings...", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("grid-settings", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Toggle Grid On/Off", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.GridOn, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("toggle-grid", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Toggle Grid Snap Magnet", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.TrackChanges, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("toggle-grid-snap", null)
                            }
                        )
                    }
                }

                // TOOLS MENU
                Box {
                    NavMenuTab(
                        title = "Tools",
                        isActive = activeMenu == "tools",
                        onClick = { activeMenu = if (activeMenu == "tools") null else "tools" }
                    )
                    DropdownMenu(
                        expanded = activeMenu == "tools",
                        onDismissRequest = { activeMenu = null },
                        modifier = Modifier
                            .background(Color(0xFF111827))
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("📏 Measure Distance", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                activeMenu = null
                                onSelectAction("measure-dist", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📐 Measure Area", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                activeMenu = null
                                onSelectAction("measure-area", null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⚙️ Global Units & Scale...", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.Straighten, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("measure-units-settings", null)
                            }
                        )
                    }
                }

                // UNITS & SETTINGS MENU TAB
                Box {
                    NavMenuTab(
                        title = "Units",
                        isActive = activeMenu == "units",
                        onClick = {
                            activeMenu = null
                            onSelectAction("measure-units-settings", null)
                        }
                    )
                }

                // LAYERS MENU
                Box {
                    NavMenuTab(
                        title = "Layers",
                        isActive = activeMenu == "layers",
                        onClick = { activeMenu = if (activeMenu == "layers") null else "layers" }
                    )
                    DropdownMenu(
                        expanded = activeMenu == "layers",
                        onDismissRequest = { activeMenu = null },
                        modifier = Modifier
                            .background(Color(0xFF111827))
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Manage Layers", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                activeMenu = null
                                onSelectAction("toggle-layers-panel", null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavMenuTab(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isActive) Color(0xFF1F2937) else Color.Transparent, // bg-gray-800
            contentColor = if (isActive) Color(0xFF60A5FA) else Color(0xFFD1D5DB) // text-blue-400 / text-gray-300
        ),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
