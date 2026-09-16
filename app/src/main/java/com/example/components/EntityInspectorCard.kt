package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Info
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
import com.example.parser.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun EntitySelectionInspector(
    selectedEntities: List<DxfEntity>,
    layersMap: Map<String, DxfLayer>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedEntities.isEmpty()) return

    if (selectedEntities.size == 1) {
        val entity = selectedEntities.first()
        val layerColor = entity.color
            ?: layersMap[entity.layer]?.color
            ?: Color.White

        SingleEntityInspectorCard(
            entity = entity,
            layerColor = layerColor,
            onDismiss = onDismiss,
            modifier = modifier
        )
    } else {
        MultiEntityInspectorCard(
            entities = selectedEntities,
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}

@Composable
fun MultiEntityInspectorCard(
    entities: List<DxfEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeCounts = entities.groupingBy { getEntityTypeName(it) }.eachCount()
    val layerCounts = entities.groupingBy { it.layer }.eachCount()

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    for (e in entities) {
        val b = e.getBoundingBox()
        minX = min(minX, b.minX)
        minY = min(minY, b.minY)
        maxX = max(maxX, b.maxX)
        maxY = max(maxY, b.maxY)
    }

    val width = if (minX.isFinite() && maxX.isFinite()) maxX - minX else 0f
    val height = if (minY.isFinite() && maxY.isFinite()) maxY - minY else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_entity_inspector_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xF0102A43)
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CropSquare,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Marquee Selection (${entities.size} Entities)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("btn_close_multi_inspector")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Selection",
                        tint = Color(0x99FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bounding Extents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33000000), shape = MaterialTheme.shapes.small)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Area: %.1f × %.1f units".format(width, height),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD600),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "Layers: ${layerCounts.size}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF90CAF9)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Entity Type Chips Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for ((typeName, count) in typeCounts) {
                    Surface(
                        color = Color(0x3300E5FF),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "$typeName: $count",
                            color = Color(0xFF00E5FF),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SingleEntityInspectorCard(
    entity: DxfEntity,
    layerColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("entity_inspector_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xF0102A43)
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Entity Inspector",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("btn_close_inspector")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Inspector",
                        tint = Color(0x99FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Entity Type & Layer Badges
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0x3300E5FF),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = getEntityTypeName(entity),
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = Color(0x33FFFFFF),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(layerColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Layer: ${entity.layer}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detailed Geometry Attributes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x22000000), shape = MaterialTheme.shapes.small)
                    .padding(10.dp)
            ) {
                renderEntityGeometry(entity)
            }
        }
    }
}

// Backward compatibility alias
@Composable
fun EntityInspectorCard(
    entity: DxfEntity,
    layerColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SingleEntityInspectorCard(
        entity = entity,
        layerColor = layerColor,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
private fun renderEntityGeometry(entity: DxfEntity) {
    val monoStyle = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        color = Color(0xFFE0E0E0),
        fontSize = 12.sp
    )

    when (entity) {
        is DxfLine -> {
            Text("Start: (%.2f, %.2f)".format(entity.start.x, entity.start.y), style = monoStyle)
            Text("End:   (%.2f, %.2f)".format(entity.end.x, entity.end.y), style = monoStyle)
            Text("Length: %.3f units".format(entity.length), style = monoStyle.copy(color = Color(0xFFFFD600), fontWeight = FontWeight.Bold))
        }
        is DxfCircle -> {
            Text("Center: (%.2f, %.2f)".format(entity.center.x, entity.center.y), style = monoStyle)
            Text("Radius: %.3f units".format(entity.radius), style = monoStyle.copy(color = Color(0xFFFFD600), fontWeight = FontWeight.Bold))
            Text("Circumference: %.3f".format(entity.circumference), style = monoStyle)
            Text("Area:   %.3f sq units".format(entity.area), style = monoStyle)
        }
        is DxfArc -> {
            Text("Center: (%.2f, %.2f)".format(entity.center.x, entity.center.y), style = monoStyle)
            Text("Radius: %.3f".format(entity.radius), style = monoStyle)
            Text("Angles: %.1f° to %.1f°".format(entity.startAngleDeg, entity.endAngleDeg), style = monoStyle.copy(color = Color(0xFFFFD600)))
        }
        is DxfLwPolyline -> {
            Text("Vertices: ${entity.vertices.size} points", style = monoStyle)
            Text("Closed:   ${if (entity.isClosed) "Yes" else "No"}", style = monoStyle)
            Text("Length:   %.3f units".format(entity.totalLength), style = monoStyle.copy(color = Color(0xFFFFD600), fontWeight = FontWeight.Bold))
        }
        is DxfPolyline -> {
            Text("Vertices: ${entity.vertices.size} points", style = monoStyle)
            Text("Closed:   ${if (entity.isClosed) "Yes" else "No"}", style = monoStyle)
        }
        is DxfText -> {
            Text("Text Content: \"${entity.text}\"", style = monoStyle.copy(fontWeight = FontWeight.Bold, color = Color.White))
            Text("Position: (%.2f, %.2f)".format(entity.position.x, entity.position.y), style = monoStyle)
            Text("Height:   %.2f units".format(entity.height), style = monoStyle)
        }
        is DxfDimension -> {
            Text("Dim Text: ${entity.text.ifBlank { "Auto" }}", style = monoStyle.copy(fontWeight = FontWeight.Bold))
            Text("Def Point: (%.2f, %.2f)".format(entity.defPoint.x, entity.defPoint.y), style = monoStyle)
        }
        is DxfEllipse -> {
            Text("Center: (%.2f, %.2f)".format(entity.center.x, entity.center.y), style = monoStyle)
            Text("Major Radius: %.2f".format(entity.majorRadius), style = monoStyle)
            Text("Minor Ratio:  %.2f".format(entity.minorRatio), style = monoStyle)
        }
        else -> {
            Text(entity.getSummary(), style = monoStyle)
        }
    }
}

private fun getEntityTypeName(entity: DxfEntity): String {
    return when (entity) {
        is DxfLine -> "LINE"
        is DxfCircle -> "CIRCLE"
        is DxfArc -> "ARC"
        is DxfLwPolyline -> "LWPOLYLINE"
        is DxfPolyline -> "POLYLINE"
        is DxfText -> "TEXT"
        is DxfDimension -> "DIMENSION"
        is DxfEllipse -> "ELLIPSE"
        is DxfSpline -> "SPLINE"
        is DxfPoint -> "POINT"
    }
}
