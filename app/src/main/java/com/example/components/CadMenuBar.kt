package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.store.CadModifyDialogType
import com.example.store.CadTool

@Composable
fun CadMenuBar(
    activeTool: CadTool,
    showGrid: Boolean,
    showAxes: Boolean,
    isBlueprintTheme: Boolean,
    hasSelection: Boolean,
    hasMeasurement: Boolean,
    canUndo: Boolean,
    onOpenFilePicker: () -> Unit,
    onOpenSamples: () -> Unit,
    onSetTool: (CadTool) -> Unit,
    onOpenModifyDialog: (CadModifyDialogType) -> Unit,
    onUndo: () -> Unit,
    onSelectAllInView: () -> Unit,
    onClearSelection: () -> Unit,
    onClearMeasurement: () -> Unit,
    onFitExtents: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleAxes: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenLayers: () -> Unit,
    onToggleAllLayers: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedMenu by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Surface(
        color = Color(0xFF091424),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cad_menu_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. FILE MENU
            Box {
                MenuButton(
                    title = "File",
                    isOpen = expandedMenu == "File",
                    onClick = { expandedMenu = if (expandedMenu == "File") null else "File" }
                )
                DropdownMenu(
                    expanded = expandedMenu == "File",
                    onDismissRequest = { expandedMenu = null },
                    modifier = Modifier.background(Color(0xFF0C1D36))
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Local DXF...", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        onClick = {
                            expandedMenu = null
                            onOpenFilePicker()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sample Blueprints...", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null, tint = Color(0xFFFFD54F)) },
                        onClick = {
                            expandedMenu = null
                            onOpenSamples()
                        }
                    )
                }
            }

            // 2. EDIT & MODIFY MENU (Matches React/Lucide specification)
            Box {
                MenuButton(
                    title = "Edit & Modify ▾",
                    isOpen = expandedMenu == "Edit",
                    isHighlighted = true,
                    onClick = { expandedMenu = if (expandedMenu == "Edit") null else "Edit" }
                )
                DropdownMenu(
                    expanded = expandedMenu == "Edit",
                    onDismissRequest = { expandedMenu = null },
                    modifier = Modifier
                        .width(240.dp)
                        .background(Color(0xFF0C1D36))
                ) {
                    // Selection Section Header
                    Text(
                        text = "SELECTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )

                    DropdownMenuItem(
                        text = { Text("Marquee Box Select", color = Color.White, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        onClick = {
                            expandedMenu = null
                            onSetTool(CadTool.MARQUEE_SELECT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Select All in View", color = Color.White, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null, tint = Color(0xFF69F0AE)) },
                        onClick = {
                            expandedMenu = null
                            onSelectAllInView()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear Selection", color = if (hasSelection) Color.White else Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF5252)) },
                        enabled = hasSelection,
                        onClick = {
                            expandedMenu = null
                            onClearSelection()
                        }
                    )

                    HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                    // Modify & Edit Tools Section Header
                    Text(
                        text = "MODIFY & EDIT TOOLS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )

                    DropdownMenuItem(
                        text = { Text("Move Entity", color = Color.White, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.OpenWith, contentDescription = null, tint = Color(0xFF60A5FA)) },
                        onClick = {
                            expandedMenu = null
                            onOpenModifyDialog(CadModifyDialogType.MOVE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rotate", color = Color.White, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color(0xFF4ADE80)) },
                        onClick = {
                            expandedMenu = null
                            onOpenModifyDialog(CadModifyDialogType.ROTATE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Scale", color = Color.White, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color(0xFFFACC15)) },
                        onClick = {
                            expandedMenu = null
                            onOpenModifyDialog(CadModifyDialogType.SCALE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy / Offset", color = Color.White, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFC084FC)) },
                        onClick = {
                            expandedMenu = null
                            onOpenModifyDialog(CadModifyDialogType.COPY)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Trim & Extend", color = Color.White, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color(0xFFF87171)) },
                        onClick = {
                            expandedMenu = null
                            onOpenModifyDialog(CadModifyDialogType.TRIM)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mirror", color = Color.White, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Flip, contentDescription = null, tint = Color(0xFF22D3EE)) },
                        onClick = {
                            expandedMenu = null
                            onOpenModifyDialog(CadModifyDialogType.MIRROR)
                        }
                    )

                    if (canUndo) {
                        HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Undo Last Modification", color = Color(0xFFFFD54F), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Undo, contentDescription = null, tint = Color(0xFFFFD54F)) },
                            onClick = {
                                expandedMenu = null
                                onUndo()
                            }
                        )
                    }
                }
            }

            // 3. VIEW MENU
            Box {
                MenuButton(
                    title = "View",
                    isOpen = expandedMenu == "View",
                    onClick = { expandedMenu = if (expandedMenu == "View") null else "View" }
                )
                DropdownMenu(
                    expanded = expandedMenu == "View",
                    onDismissRequest = { expandedMenu = null },
                    modifier = Modifier.background(Color(0xFF0C1D36))
                ) {
                    DropdownMenuItem(
                        text = { Text("Fit to Extents", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.FitScreen, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        onClick = {
                            expandedMenu = null
                            onFitExtents()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Zoom In (+)", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White) },
                        onClick = {
                            expandedMenu = null
                            onZoomIn()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Zoom Out (-)", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ZoomOut, contentDescription = null, tint = Color.White) },
                        onClick = {
                            expandedMenu = null
                            onZoomOut()
                        }
                    )
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    DropdownMenuItem(
                        text = { Text(if (showGrid) "Hide Grid" else "Show Grid", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                                contentDescription = null,
                                tint = if (showGrid) Color(0xFF00E5FF) else Color.Gray
                            )
                        },
                        onClick = {
                            expandedMenu = null
                            onToggleGrid()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (showAxes) "Hide Origin Axes" else "Show Origin Axes", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Explore,
                                contentDescription = null,
                                tint = if (showAxes) Color(0xFF69F0AE) else Color.Gray
                            )
                        },
                        onClick = {
                            expandedMenu = null
                            onToggleAxes()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isBlueprintTheme) "Theme: Blueprint (Dark)" else "Theme: Carbon Dark", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                if (isBlueprintTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F)
                            )
                        },
                        onClick = {
                            expandedMenu = null
                            onToggleTheme()
                        }
                    )
                }
            }

            // 4. TOOLS MENU
            Box {
                MenuButton(
                    title = "Tools",
                    isOpen = expandedMenu == "Tools",
                    onClick = { expandedMenu = if (expandedMenu == "Tools") null else "Tools" }
                )
                DropdownMenu(
                    expanded = expandedMenu == "Tools",
                    onDismissRequest = { expandedMenu = null },
                    modifier = Modifier.background(Color(0xFF0C1D36))
                ) {
                    DropdownMenuItem(
                        text = { Text("Pan & Orbit (P)", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PanTool,
                                contentDescription = null,
                                tint = if (activeTool == CadTool.PAN_ZOOM) Color(0xFF00E5FF) else Color.White
                            )
                        },
                        trailingIcon = {
                            if (activeTool == CadTool.PAN_ZOOM) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            expandedMenu = null
                            onSetTool(CadTool.PAN_ZOOM)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Marquee Box Select (B)", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CropSquare,
                                contentDescription = null,
                                tint = if (activeTool == CadTool.MARQUEE_SELECT) Color(0xFF00E5FF) else Color.White
                            )
                        },
                        trailingIcon = {
                            if (activeTool == CadTool.MARQUEE_SELECT) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            expandedMenu = null
                            onSetTool(CadTool.MARQUEE_SELECT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Inspect Single (I)", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (activeTool == CadTool.SELECT) Color(0xFFFFD600) else Color.White
                            )
                        },
                        trailingIcon = {
                            if (activeTool == CadTool.SELECT) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            expandedMenu = null
                            onSetTool(CadTool.SELECT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Measure Distance (M)", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Straighten,
                                contentDescription = null,
                                tint = if (activeTool == CadTool.MEASURE) Color(0xFFFF4081) else Color.White
                            )
                        },
                        trailingIcon = {
                            if (activeTool == CadTool.MEASURE) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFFF4081), modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            expandedMenu = null
                            onSetTool(CadTool.MEASURE)
                        }
                    )
                }
            }

            // 5. LAYERS MENU
            Box {
                MenuButton(
                    title = "Layers",
                    isOpen = expandedMenu == "Layers",
                    onClick = { expandedMenu = if (expandedMenu == "Layers") null else "Layers" }
                )
                DropdownMenu(
                    expanded = expandedMenu == "Layers",
                    onDismissRequest = { expandedMenu = null },
                    modifier = Modifier.background(Color(0xFF0C1D36))
                ) {
                    DropdownMenuItem(
                        text = { Text("Manage CAD Layers...", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFFFFD54F)) },
                        onClick = {
                            expandedMenu = null
                            onOpenLayers()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show All Layers", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF69F0AE)) },
                        onClick = {
                            expandedMenu = null
                            onToggleAllLayers(true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hide All Layers", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFFF5252)) },
                        onClick = {
                            expandedMenu = null
                            onToggleAllLayers(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuButton(
    title: String,
    isOpen: Boolean,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isOpen) Color(0x3300E5FF) else if (isHighlighted) Color(0x1A3B82F6) else Color.Transparent,
            contentColor = if (isOpen) Color(0xFF00E5FF) else if (isHighlighted) Color(0xFF60A5FA) else Color(0xFFCFD8DC)
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.height(30.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isOpen || isHighlighted) FontWeight.Bold else FontWeight.Medium
        )
    }
}
