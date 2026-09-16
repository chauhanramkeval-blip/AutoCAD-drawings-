package com.example.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadTopBar(
    fileName: String,
    entityCount: Int,
    isBlueprintTheme: Boolean,
    onOpenSamples: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onToggleLayers: () -> Unit,
    onFitExtents: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CAD Viewer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0x3300E5FF),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "DXF",
                            color = Color(0xFF00E5FF),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "$fileName • $entityCount entities",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF90CAF9),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            // Sample Drawings picker
            IconButton(
                onClick = onOpenSamples,
                modifier = Modifier.testTag("btn_sample_drawings")
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "Sample CAD Drawings",
                    tint = Color.White
                )
            }

            // Open Local DXF File
            IconButton(
                onClick = onOpenFilePicker,
                modifier = Modifier.testTag("btn_open_file")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Open Local DXF File",
                    tint = Color.White
                )
            }

            // Fit To Screen
            IconButton(
                onClick = onFitExtents,
                modifier = Modifier.testTag("btn_fit_extents")
            ) {
                Icon(
                    imageVector = Icons.Default.FitScreen,
                    contentDescription = "Fit Drawing to Screen",
                    tint = Color(0xFF00E5FF)
                )
            }

            // Layer Manager Sheet
            IconButton(
                onClick = onToggleLayers,
                modifier = Modifier.testTag("btn_layers_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "CAD Layers",
                    tint = Color(0xFFFFD54F)
                )
            }

            // Theme (Blueprint vs Neutral)
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("btn_toggle_theme")
            ) {
                Icon(
                    imageVector = if (isBlueprintTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = "Toggle Blueprint Theme",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0B192C)
        ),
        modifier = modifier
    )
}
