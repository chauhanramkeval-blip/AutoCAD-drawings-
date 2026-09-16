package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parser.DxfLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerManagerSheet(
    layers: Map<String, DxfLayer>,
    visibilityMap: Map<String, Boolean>,
    layerEntityCounts: Map<String, Int>,
    onToggleLayer: (String, Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF132238),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Layer Manager",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${layers.size} CAD Layers in current drawing",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF90CAF9))
                    )
                }

                Row {
                    TextButton(
                        onClick = { onToggleAll(true) },
                        modifier = Modifier.testTag("btn_show_all_layers")
                    ) {
                        Text("Show All", color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    TextButton(
                        onClick = { onToggleAll(false) },
                        modifier = Modifier.testTag("btn_hide_all_layers")
                    ) {
                        Text("Hide All", color = Color(0xFFFF8A80), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0x33FFFFFF))
            Spacer(modifier = Modifier.height(8.dp))

            // Layers List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(layers.values.toList()) { layer ->
                    val isVisible = visibilityMap[layer.name] != false
                    val count = layerEntityCounts[layer.name] ?: 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onToggleLayer(layer.name, !isVisible) }
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                            .testTag("layer_item_${layer.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color Swatch
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(layer.color)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Layer Name & Count
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = layer.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isVisible) Color.White else Color(0x66FFFFFF)
                                )
                            )
                            Text(
                                text = "$count entities",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isVisible) Color(0xFF81D4FA) else Color(0x44FFFFFF)
                                )
                            )
                        }

                        // Eye Visibility Toggle Icon
                        IconButton(
                            onClick = { onToggleLayer(layer.name, !isVisible) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isVisible) "Hide layer" else "Show layer",
                                tint = if (isVisible) Color(0xFF00E5FF) else Color(0x55FFFFFF)
                            )
                        }
                    }
                }
            }
        }
    }
}
