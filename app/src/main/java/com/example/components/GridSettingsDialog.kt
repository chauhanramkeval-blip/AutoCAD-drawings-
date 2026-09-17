package com.example.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.canvas.*
import com.example.measurement.UnitConfig
import com.example.measurement.UnitManager
import com.example.measurement.UnitSystem
import kotlin.math.roundToInt

@Composable
fun GridSettingsDialog(
    gridSettings: GridSettings,
    unitConfig: UnitConfig,
    viewportScale: Float,
    isBlueprintTheme: Boolean,
    onSaveSettings: (GridSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSettings by remember { mutableStateOf(gridSettings) }
    var customSpacingText by remember {
        val converted = UnitManager.convertDistance(
            tempSettings.customSpacingCad,
            unitConfig.baseDrawingUnit,
            unitConfig.displayDistanceUnit,
            unitConfig.drawingScale
        )
        mutableStateOf(unitConfig.precisionPattern.format(converted))
    }

    val isMetric = unitConfig.unitSystem == UnitSystem.METRIC
    val unitSymbol = unitConfig.displayDistanceUnit.symbol

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("grid_settings_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Grid & Viewport Settings",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "Spacing, snap frequency & visual opacity",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_close_grid_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Grid Visibility Master Card & Live Preview
                    GridLivePreviewCard(
                        settings = tempSettings,
                        unitConfig = unitConfig,
                        viewportScale = viewportScale,
                        isBlueprintTheme = isBlueprintTheme,
                        onToggleVisibility = {
                            tempSettings = tempSettings.copy(isVisible = !tempSettings.isVisible)
                        }
                    )

                    // 2. Grid Style Selection
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Grid Display Style",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GridStyle.values().forEach { style ->
                                    val isSelected = tempSettings.style == style
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { tempSettings = tempSettings.copy(style = style) }
                                            .testTag("style_chip_${style.name.lowercase()}"),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = style.iconLabel,
                                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = style.displayName,
                                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Spacing Mode & Values
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grid Spacing Mode",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Active: ${tempSettings.formatCurrentSpacing(viewportScale, unitConfig)}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Adaptive vs Fixed Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GridSpacingMode.values().forEach { mode ->
                                    val isSelected = tempSettings.spacingMode == mode
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { tempSettings = tempSettings.copy(spacingMode = mode) }
                                            .testTag("mode_chip_${mode.name.lowercase()}"),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = mode.displayName,
                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = mode.description,
                                                color = Color(0xFF64748B),
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Fixed spacing controls when FIXED is selected
                            if (tempSettings.spacingMode == GridSpacingMode.FIXED) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Custom Spacing Interval ($unitSymbol)",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Presets horizontal scroll
                                val presets = if (isMetric) {
                                    listOf(1f, 5f, 10f, 25f, 50f, 100f, 500f, 1000f)
                                } else {
                                    listOf(0.125f, 0.25f, 0.5f, 1f, 6f, 12f, 36f)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    presets.forEach { presetVal ->
                                        val displayStr = if (presetVal < 1f) "%.3f".format(presetVal).trimEnd('0') else presetVal.toInt().toString()
                                        Surface(
                                            modifier = Modifier
                                                .clickable {
                                                    val cadVal = UnitManager.parseToCadUnits(
                                                        presetVal,
                                                        unitConfig.baseDrawingUnit,
                                                        unitConfig.displayDistanceUnit,
                                                        unitConfig.drawingScale
                                                    )
                                                    tempSettings = tempSettings.copy(customSpacingCad = cadVal)
                                                    customSpacingText = displayStr
                                                }
                                                .testTag("preset_$displayStr"),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF334155),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                                        ) {
                                            Text(
                                                text = "$displayStr $unitSymbol",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Custom text input
                                OutlinedTextField(
                                    value = customSpacingText,
                                    onValueChange = { str ->
                                        customSpacingText = str
                                        val num = str.toFloatOrNull()
                                        if (num != null && num > 0f) {
                                            val cadVal = UnitManager.parseToCadUnits(
                                                num,
                                                unitConfig.baseDrawingUnit,
                                                unitConfig.displayDistanceUnit,
                                                unitConfig.drawingScale
                                            )
                                            tempSettings = tempSettings.copy(customSpacingCad = cadVal)
                                        }
                                    },
                                    label = { Text("Custom Grid Spacing", fontSize = 12.sp) },
                                    suffix = { Text(unitSymbol, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF475569),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_custom_grid_spacing")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Major line interval
                            Text(
                                text = "Major Grid Line Frequency (Every N Lines)",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 5, 10, 20).forEach { interval ->
                                    val isSelected = tempSettings.majorInterval == interval
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { tempSettings = tempSettings.copy(majorInterval = interval) }
                                            .testTag("major_interval_$interval"),
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                                        )
                                    ) {
                                        Text(
                                            text = "${interval}x",
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Snap Frequency & Grid Snapping
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Magnet Snap to Grid",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Snap cursor & tool points to grid intersections",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }

                                Switch(
                                    checked = tempSettings.isSnapToGrid,
                                    onCheckedChange = { tempSettings = tempSettings.copy(isSnapToGrid = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0284C7),
                                        uncheckedTrackColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.testTag("switch_snap_to_grid")
                                )
                            }

                            if (tempSettings.isSnapToGrid) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Snap Frequency Subdivisions",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Step: ${tempSettings.formatCurrentSnapStep(viewportScale, unitConfig)}",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        GridSnapFrequency.EVERY_GRID,
                                        GridSnapFrequency.HALF_GRID,
                                        GridSnapFrequency.QUARTER_GRID,
                                        GridSnapFrequency.TENTH_GRID,
                                        GridSnapFrequency.MAJOR_ONLY
                                    ).forEach { freq ->
                                        val isSelected = tempSettings.snapFrequency == freq
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { tempSettings = tempSettings.copy(snapFrequency = freq) }
                                                .testTag("snap_freq_${freq.name.lowercase()}"),
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Color(0xFFD97706).copy(alpha = 0.3f) else Color(0xFF0F172A),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0xFFFBBF24) else Color(0xFF334155)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = freq.shortLabel,
                                                    color = if (isSelected) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Line Opacity & Major Line Emphasis
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Opacity Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Line Opacity",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(tempSettings.lineOpacity * 100).roundToInt()}%",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = tempSettings.lineOpacity,
                                onValueChange = { tempSettings = tempSettings.copy(lineOpacity = it) },
                                valueRange = 0.05f..0.85f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF0284C7),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("slider_line_opacity")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Major Line Emphasis Multiplier
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Major Line Contrast Emphasis",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "%.1fx".format(tempSettings.majorOpacityMultiplier),
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = tempSettings.majorOpacityMultiplier,
                                onValueChange = { tempSettings = tempSettings.copy(majorOpacityMultiplier = it) },
                                valueRange = 1.0f..3.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF0284C7),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("slider_major_emphasis")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempSettings = GridSettings()
                            val converted = UnitManager.convertDistance(
                                10f,
                                unitConfig.baseDrawingUnit,
                                unitConfig.displayDistanceUnit,
                                unitConfig.drawingScale
                            )
                            customSpacingText = unitConfig.precisionPattern.format(converted)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF94A3B8)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_reset_grid_settings")
                    ) {
                        Text("Reset Default", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onSaveSettings(tempSettings)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_apply_grid_settings")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Grid Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GridLivePreviewCard(
    settings: GridSettings,
    unitConfig: UnitConfig,
    viewportScale: Float,
    isBlueprintTheme: Boolean,
    onToggleVisibility: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Live Viewport Grid Preview",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (settings.isVisible) Color(0x3310B981) else Color(0x33EF4444),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (settings.isVisible) "GRID ON" else "GRID OFF",
                            color = if (settings.isVisible) Color(0xFF34D399) else Color(0xFFF87171),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Switch(
                    checked = settings.isVisible,
                    onCheckedChange = { onToggleVisibility() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7),
                        uncheckedTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("switch_grid_visible")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Box Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        if (isBlueprintTheme) Color(0xFF0A192F) else Color(0xFF111827),
                        RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            ) {
                if (settings.isVisible) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseColor = if (isBlueprintTheme) Color(0xFF00E5FF) else Color(0xFFFFFFFF)
                        val opacity = settings.lineOpacity.coerceIn(0.01f, 1.0f)
                        val majorOpacity = (opacity * settings.majorOpacityMultiplier).coerceIn(0.01f, 1.0f)

                        val fineColor = baseColor.copy(alpha = opacity)
                        val majorColor = baseColor.copy(alpha = majorOpacity)

                        val stepPx = 28f
                        val majorStepPx = stepPx * settings.majorInterval.coerceAtLeast(2)

                        val dottedEffect = if (settings.style == GridStyle.DOTTED) {
                            PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        } else null

                        if (settings.style == GridStyle.DOT_GRID) {
                            var x = 0f
                            while (x <= size.width) {
                                var y = 0f
                                val isMajorX = (x % majorStepPx) < 1f || (majorStepPx - (x % majorStepPx)) < 1f
                                while (y <= size.height) {
                                    val isMajorY = (y % majorStepPx) < 1f || (majorStepPx - (y % majorStepPx)) < 1f
                                    val isMajor = isMajorX && isMajorY
                                    drawCircle(
                                        color = if (isMajor) majorColor else fineColor,
                                        radius = if (isMajor) 2.5f else 1.2f,
                                        center = Offset(x, y)
                                    )
                                    y += stepPx
                                }
                                x += stepPx
                            }
                        } else {
                            var x = 0f
                            while (x <= size.width) {
                                val isMajor = (x % majorStepPx) < 1f || (majorStepPx - (x % majorStepPx)) < 1f
                                drawLine(
                                    color = if (isMajor) majorColor else fineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = if (isMajor) 1.5f else 0.8f,
                                    pathEffect = dottedEffect
                                )
                                x += stepPx
                            }
                            var y = 0f
                            while (y <= size.height) {
                                val isMajor = (y % majorStepPx) < 1f || (majorStepPx - (y % majorStepPx)) < 1f
                                drawLine(
                                    color = if (isMajor) majorColor else fineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = if (isMajor) 1.5f else 0.8f,
                                    pathEffect = dottedEffect
                                )
                                y += stepPx
                            }
                        }

                        // Draw demo snap point if snap enabled
                        if (settings.isSnapToGrid) {
                            val snapCenter = Offset(stepPx * 4, stepPx * 2)
                            val snapColor = Color(0xFFFBBF24)
                            drawRect(
                                color = snapColor,
                                topLeft = Offset(snapCenter.x - 7f, snapCenter.y - 7f),
                                size = androidx.compose.ui.geometry.Size(14f, 14f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                            )
                            drawLine(snapColor, Offset(snapCenter.x - 7f, snapCenter.y), Offset(snapCenter.x + 7f, snapCenter.y), strokeWidth = 1.2f)
                            drawLine(snapColor, Offset(snapCenter.x, snapCenter.y - 7f), Offset(snapCenter.x, snapCenter.y + 7f), strokeWidth = 1.2f)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Grid Disabled", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }

                // Mini HUD Overlay
                Surface(
                    color = Color(0xDD0F172A),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "Spacing: ${settings.formatCurrentSpacing(viewportScale, unitConfig)} | Snap: ${if (settings.isSnapToGrid) settings.snapFrequency.shortLabel else "Off"} | Opacity: ${(settings.lineOpacity * 100).roundToInt()}%",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
