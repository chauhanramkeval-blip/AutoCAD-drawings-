package com.example.parser

import androidx.compose.ui.graphics.Color
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

/**
 * Standard ASCII DXF parser.
 * Reads group code / value pairs and constructs structured CAD models.
 */
class DxfParser {

    companion object {
        fun parse(input: String, fileName: String = "drawing.dxf"): DxfDocument {
            return DxfParser().parse(input, fileName)
        }
        fun parse(inputStream: InputStream, fileName: String = "drawing.dxf"): DxfDocument {
            return DxfParser().parse(inputStream, fileName)
        }
    }

    data class GroupCodePair(val code: Int, val value: String)

    fun parse(input: String, fileName: String = "drawing.dxf"): DxfDocument {
        return parse(BufferedReader(StringReader(input)), fileName)
    }

    fun parse(inputStream: InputStream, fileName: String = "drawing.dxf"): DxfDocument {
        return parse(BufferedReader(InputStreamReader(inputStream)), fileName)
    }

    fun parse(reader: BufferedReader, fileName: String = "drawing.dxf"): DxfDocument {
        val pairs = mutableListOf<GroupCodePair>()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val codeStr = line!!.trim()
            if (codeStr.isEmpty()) continue
            val code = codeStr.toIntOrNull() ?: continue
            val value = reader.readLine()?.trim() ?: ""
            pairs.add(GroupCodePair(code, value))
        }

        val layers = mutableMapOf<String, DxfLayer>()
        // Default standard layer
        layers["0"] = DxfLayer("0", Color(0xFFE0E0E0))

        val entities = mutableListOf<DxfEntity>()

        var i = 0
        var currentSection = ""

        while (i < pairs.size) {
            val pair = pairs[i]

            if (pair.code == 0 && pair.value == "SECTION") {
                if (i + 1 < pairs.size && pairs[i + 1].code == 2) {
                    currentSection = pairs[i + 1].value.uppercase()
                    i += 2
                    continue
                }
            } else if (pair.code == 0 && pair.value == "ENDSEC") {
                currentSection = ""
                i++
                continue
            }

            when (currentSection) {
                "TABLES" -> {
                    // Extract LAYER definitions
                    if (pair.code == 0 && pair.value == "LAYER") {
                        i = parseLayer(pairs, i, layers)
                        continue
                    }
                }
                "ENTITIES" -> {
                    if (pair.code == 0 && pair.value != "ENDSEC") {
                        val entityType = pair.value.uppercase()
                        i = parseEntity(pairs, i, entityType, entities)
                        continue
                    }
                }
                else -> {
                    // Also parse entities if DXF has no explicit SECTION wrappers
                    if (pair.code == 0 && isKnownEntityType(pair.value)) {
                        i = parseEntity(pairs, i, pair.value.uppercase(), entities)
                        continue
                    }
                }
            }
            i++
        }

        // Ensure all referenced layers exist
        for (entity in entities) {
            if (!layers.containsKey(entity.layer)) {
                val color = entity.color ?: AciColors.Cyan
                layers[entity.layer] = DxfLayer(entity.layer, color)
            }
        }

        val points = mutableListOf<Point2D>()
        for (e in entities) {
            val bb = e.getBoundingBox()
            points.add(Point2D(bb.minX, bb.minY))
            points.add(Point2D(bb.maxX, bb.maxY))
        }

        val extents = BoundingBox.fromPoints(points)

        return DxfDocument(
            fileName = fileName,
            layers = layers,
            entities = entities,
            extents = extents
        )
    }

    private fun isKnownEntityType(type: String): Boolean {
        return when (type.uppercase()) {
            "LINE", "CIRCLE", "ARC", "LWPOLYLINE", "POLYLINE", "ELLIPSE",
            "SPLINE", "TEXT", "MTEXT", "DIMENSION", "POINT" -> true
            else -> false
        }
    }

    private fun parseLayer(
        pairs: List<GroupCodePair>,
        startIndex: Int,
        layers: MutableMap<String, DxfLayer>
    ): Int {
        var i = startIndex + 1
        var layerName = "0"
        var colorIndex = 7

        while (i < pairs.size) {
            val p = pairs[i]
            if (p.code == 0) break // next element
            when (p.code) {
                2 -> layerName = p.value
                62 -> colorIndex = p.value.toIntOrNull() ?: 7
            }
            i++
        }

        val color = AciColors.getColor(colorIndex)
        layers[layerName] = DxfLayer(layerName, color)
        return i
    }

    private fun parseEntity(
        pairs: List<GroupCodePair>,
        startIndex: Int,
        entityType: String,
        entities: MutableList<DxfEntity>
    ): Int {
        var i = startIndex + 1
        var layer = "0"
        var color: Color? = null

        var x1 = 0f; var y1 = 0f
        var x2 = 0f; var y2 = 0f
        var radius = 0f
        var startAngle = 0f
        var endAngle = 360f
        var textContent = ""
        var textHeight = 2.5f
        var rotation = 0f
        var isClosed = false
        val vertices = mutableListOf<Point2D>()
        var currentVx = 0f; var currentVy = 0f
        var hasVx = false; var hasVy = false
        var minorRatio = 1f

        while (i < pairs.size) {
            val p = pairs[i]
            if (p.code == 0) break // Next entity begins

            when (p.code) {
                8 -> layer = p.value
                62 -> {
                    val c = p.value.toIntOrNull()
                    if (c != null) color = AciColors.getColor(c)
                }
                10 -> {
                    x1 = p.value.toFloatOrNull() ?: 0f
                    if (entityType == "LWPOLYLINE" || entityType == "POLYLINE" || entityType == "SPLINE") {
                        if (hasVx && hasVy) {
                            vertices.add(Point2D(currentVx, currentVy))
                            hasVy = false
                        }
                        currentVx = x1
                        hasVx = true
                    }
                }
                20 -> {
                    y1 = p.value.toFloatOrNull() ?: 0f
                    if (entityType == "LWPOLYLINE" || entityType == "POLYLINE" || entityType == "SPLINE") {
                        currentVy = y1
                        hasVy = true
                    }
                }
                11 -> x2 = p.value.toFloatOrNull() ?: 0f
                21 -> y2 = p.value.toFloatOrNull() ?: 0f
                40 -> {
                    val v = p.value.toFloatOrNull() ?: 0f
                    if (entityType == "TEXT" || entityType == "MTEXT") {
                        textHeight = v
                    } else if (entityType == "ELLIPSE") {
                        minorRatio = v
                    } else {
                        radius = v
                    }
                }
                50 -> startAngle = p.value.toFloatOrNull() ?: 0f
                51 -> endAngle = p.value.toFloatOrNull() ?: 0f
                1 -> textContent = p.value
                70 -> {
                    val flags = p.value.toIntOrNull() ?: 0
                    isClosed = (flags and 1) != 0
                }
            }
            i++
        }

        if (hasVx && hasVy) {
            vertices.add(Point2D(currentVx, currentVy))
        }

        when (entityType) {
            "LINE" -> entities.add(DxfLine(Point2D(x1, y1), Point2D(x2, y2), layer, color))
            "CIRCLE" -> entities.add(DxfCircle(Point2D(x1, y1), radius, layer, color))
            "ARC" -> entities.add(DxfArc(Point2D(x1, y1), radius, startAngle, endAngle, layer, color))
            "LWPOLYLINE" -> {
                if (vertices.isNotEmpty()) {
                    entities.add(DxfLwPolyline(vertices, isClosed, layer, color))
                }
            }
            "POLYLINE" -> {
                if (vertices.isNotEmpty()) {
                    entities.add(DxfPolyline(vertices, isClosed, layer, color))
                }
            }
            "ELLIPSE" -> entities.add(DxfEllipse(Point2D(x1, y1), Point2D(x2, y2), minorRatio, layer = layer, color = color))
            "SPLINE" -> {
                if (vertices.isNotEmpty()) {
                    entities.add(DxfSpline(vertices, layer, color))
                }
            }
            "TEXT", "MTEXT" -> entities.add(DxfText(textContent, Point2D(x1, y1), textHeight, rotation, layer, color))
            "DIMENSION" -> entities.add(DxfDimension(textContent, Point2D(x1, y1), Point2D(x2, y2), layer, color))
            "POINT" -> entities.add(DxfPoint(Point2D(x1, y1), layer, color))
        }

        return i
    }

    fun exportToDxf(doc: DxfDocument): String {
        val sb = StringBuilder()
        sb.append("0\nSECTION\n2\nHEADER\n0\nENDSEC\n")
        sb.append("0\nSECTION\n2\nTABLES\n0\nTABLE\n2\nLAYER\n")
        for (layer in doc.layers.values) {
            sb.append("0\nLAYER\n2\n${layer.name}\n70\n0\n62\n7\n")
        }
        sb.append("0\nENDTAB\n0\nENDSEC\n")
        sb.append("0\nSECTION\n2\nENTITIES\n")

        for (entity in doc.entities) {
            when (entity) {
                is DxfLine -> {
                    sb.append("0\nLINE\n8\n${entity.layer}\n")
                    sb.append("10\n${entity.start.x}\n20\n${entity.start.y}\n")
                    sb.append("11\n${entity.end.x}\n21\n${entity.end.y}\n")
                }
                is DxfCircle -> {
                    sb.append("0\nCIRCLE\n8\n${entity.layer}\n")
                    sb.append("10\n${entity.center.x}\n20\n${entity.center.y}\n")
                    sb.append("40\n${entity.radius}\n")
                }
                is DxfArc -> {
                    sb.append("0\nARC\n8\n${entity.layer}\n")
                    sb.append("10\n${entity.center.x}\n20\n${entity.center.y}\n")
                    sb.append("40\n${entity.radius}\n")
                    sb.append("50\n${entity.startAngleDeg}\n51\n${entity.endAngleDeg}\n")
                }
                is DxfLwPolyline -> {
                    sb.append("0\nLWPOLYLINE\n8\n${entity.layer}\n")
                    sb.append("90\n${entity.vertices.size}\n")
                    sb.append("70\n${if (entity.isClosed) 1 else 0}\n")
                    for (v in entity.vertices) {
                        sb.append("10\n${v.x}\n20\n${v.y}\n")
                    }
                }
                is DxfPolyline -> {
                    sb.append("0\nPOLYLINE\n8\n${entity.layer}\n")
                    sb.append("70\n${if (entity.isClosed) 1 else 0}\n")
                    for (v in entity.vertices) {
                        sb.append("0\nVERTEX\n8\n${entity.layer}\n")
                        sb.append("10\n${v.x}\n20\n${v.y}\n")
                    }
                    sb.append("0\nSEQEND\n")
                }
                is DxfText -> {
                    sb.append("0\nTEXT\n8\n${entity.layer}\n")
                    sb.append("1\n${entity.text}\n")
                    sb.append("10\n${entity.position.x}\n20\n${entity.position.y}\n")
                    sb.append("40\n${entity.height}\n")
                    sb.append("50\n${entity.rotationDeg}\n")
                }
                is DxfDimension -> {
                    sb.append("0\nDIMENSION\n8\n${entity.layer}\n")
                    sb.append("1\n${entity.text}\n")
                    sb.append("10\n${entity.defPoint.x}\n20\n${entity.defPoint.y}\n")
                    sb.append("11\n${entity.textPoint.x}\n21\n${entity.textPoint.y}\n")
                }
                is DxfPoint -> {
                    sb.append("0\nPOINT\n8\n${entity.layer}\n")
                    sb.append("10\n${entity.position.x}\n20\n${entity.position.y}\n")
                }
                else -> {}
            }
        }

        sb.append("0\nENDSEC\n0\nEOF\n")
        return sb.toString()
    }
}
