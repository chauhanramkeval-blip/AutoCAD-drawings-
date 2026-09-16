package com.example.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parser.SampleDxfProvider

@Composable
fun SampleSelectorDialog(
    samples: List<SampleDxfProvider.SampleDrawing>,
    currentFileName: String,
    onSelectSample: (SampleDxfProvider.SampleDrawing) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose CAD Drawing",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(samples) { sample ->
                    val isSelected = sample.document.fileName == currentFileName
                    val icon: ImageVector = when (sample.category) {
                        "Architecture" -> Icons.Default.Apartment
                        "Mechanical" -> Icons.Default.Build
                        else -> Icons.Default.Terrain
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelectSample(sample) }
                            .testTag("sample_item_${sample.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF132238)
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00E5FF)),
                            width = 1.5.dp
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF0B192C) else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sample.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = sample.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${sample.document.entities.size} entities • ${sample.document.layers.size} layers",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF00E5FF)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_samples")
            ) {
                Text("Close", color = Color(0xFF90CAF9))
            }
        },
        containerColor = Color(0xFF0B192C)
    )
}
