package com.example.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.store.CadModifyDialogType

@Composable
fun CadModifyControls(
    dialogType: CadModifyDialogType,
    selectedCount: Int,
    canUndo: Boolean,
    onMove: (dx: Float, dy: Float) -> Unit,
    onRotate: (angleDeg: Float) -> Unit,
    onScale: (factor: Float) -> Unit,
    onCopyOffset: (dx: Float, dy: Float) -> Unit,
    onTrimExtend: (factor: Float) -> Unit,
    onMirror: (horizontal: Boolean) -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = dialogType != CadModifyDialogType.NONE,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C192E)),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(16.dp))
                .testTag("cad_modify_controls_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header with Tool Title, Entity Count Badge, and Close Button
                val (title, icon, tintColor) = when (dialogType) {
                    CadModifyDialogType.MOVE -> Triple("Move Entity", Icons.Default.OpenWith, Color(0xFF60A5FA))
                    CadModifyDialogType.ROTATE -> Triple("Rotate", Icons.Default.RotateRight, Color(0xFF4ADE80))
                    CadModifyDialogType.SCALE -> Triple("Scale", Icons.Default.AspectRatio, Color(0xFFFACC15))
                    CadModifyDialogType.COPY -> Triple("Copy / Offset", Icons.Default.ContentCopy, Color(0xFFC084FC))
                    CadModifyDialogType.TRIM -> Triple("Trim & Extend", Icons.Default.ContentCut, Color(0xFFF87171))
                    CadModifyDialogType.MIRROR -> Triple("Mirror", Icons.Default.Flip, Color(0xFF22D3EE))
                    CadModifyDialogType.NONE -> Triple("", Icons.Default.Edit, Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = tintColor.copy(alpha = 0.18f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (selectedCount > 0) "$selectedCount entities selected" else "All visible entities",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF90A4AE))
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canUndo) {
                            IconButton(
                                onClick = onUndo,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_modify_undo")
                            ) {
                                Icon(
                                    Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_modify_close")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF90A4AE),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x22FFFFFF))
                Spacer(modifier = Modifier.height(12.dp))

                // Interactive modification body according to the active tool
                when (dialogType) {
                    CadModifyDialogType.MOVE -> {
                        MoveControls(onMove = onMove)
                    }
                    CadModifyDialogType.ROTATE -> {
                        RotateControls(onRotate = onRotate)
                    }
                    CadModifyDialogType.SCALE -> {
                        ScaleControls(onScale = onScale)
                    }
                    CadModifyDialogType.COPY -> {
                        CopyControls(onCopy = onCopyOffset)
                    }
                    CadModifyDialogType.TRIM -> {
                        TrimControls(onTrim = onTrimExtend)
                    }
                    CadModifyDialogType.MIRROR -> {
                        MirrorControls(onMirror = onMirror)
                    }
                    CadModifyDialogType.NONE -> {}
                }
            }
        }
    }
}

@Composable
private fun MoveControls(onMove: (dx: Float, dy: Float) -> Unit) {
    var stepSize by remember { mutableStateOf(10f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step Distance:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1f, 5f, 10f, 25f, 50f).forEach { step ->
                    FilterChip(
                        selected = stepSize == step,
                        onClick = { stepSize = step },
                        label = { Text("${step.toInt()}u", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color(0xFF60A5FA),
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direction D-Pad Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onMove(-stepSize, 0f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Left (-X)", fontSize = 12.sp)
            }

            Button(
                onClick = { onMove(stepSize, 0f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Right (+X)", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
            }

            Button(
                onClick = { onMove(0f, stepSize) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Up (+Y)", fontSize = 12.sp)
            }

            Button(
                onClick = { onMove(0f, -stepSize) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Down (-Y)", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RotateControls(onRotate: (angleDeg: Float) -> Unit) {
    var angleSlider by remember { mutableStateOf(45f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preset Angles:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15f, 45f, 90f, 180f, -90f).forEach { ang ->
                    FilledTonalButton(
                        onClick = { onRotate(ang) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF064E3B),
                            contentColor = Color(0xFF4ADE80)
                        )
                    ) {
                        Text("${ang.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Angle: ${angleSlider.toInt()}°",
                color = Color(0xFF4ADE80),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(80.dp)
            )
            Slider(
                value = angleSlider,
                onValueChange = { angleSlider = it },
                valueRange = -180f..180f,
                steps = 71,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF4ADE80),
                    activeTrackColor = Color(0xFF4ADE80)
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onRotate(angleSlider) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Apply", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ScaleControls(onScale: (factor: Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Quick Scale Multiplier:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                0.5f to "0.5x (Half)",
                0.8f to "0.8x",
                1.25f to "1.25x",
                1.5f to "1.5x",
                2.0f to "2.0x (Double)"
            ).forEach { (factor, label) ->
                FilledTonalButton(
                    onClick = { onScale(factor) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF713F12),
                        contentColor = Color(0xFFFACC15)
                    )
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CopyControls(onCopy: (dx: Float, dy: Float) -> Unit) {
    var offsetDist by remember { mutableStateOf(20f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Offset Distance:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10f, 20f, 50f, 100f).forEach { dist ->
                    FilterChip(
                        selected = offsetDist == dist,
                        onClick = { offsetDist = dist },
                        label = { Text("${dist.toInt()}u", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF581C87),
                            selectedLabelColor = Color(0xFFC084FC),
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onCopy(offsetDist, 0f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("+X Copy", fontSize = 12.sp)
            }
            Button(
                onClick = { onCopy(0f, offsetDist) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("+Y Copy", fontSize = 12.sp)
            }
            Button(
                onClick = { onCopy(offsetDist, offsetDist) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Diagonal Copy", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TrimControls(onTrim: (factor: Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Trim / Extend Length Ratio:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                0.75f to "Trim -25%",
                0.9f to "Trim -10%",
                1.1f to "Extend +10%",
                1.25f to "Extend +25%"
            ).forEach { (factor, label) ->
                FilledTonalButton(
                    onClick = { onTrim(factor) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF7F1D1D),
                        contentColor = Color(0xFFF87171)
                    )
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MirrorControls(onMirror: (horizontal: Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Select Mirror Axis:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onMirror(true) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF155E75)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color(0xFF22D3EE), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mirror Horizontally", fontSize = 12.sp)
            }

            Button(
                onClick = { onMirror(false) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF155E75)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF22D3EE), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mirror Vertically", fontSize = 12.sp)
            }
        }
    }
}
