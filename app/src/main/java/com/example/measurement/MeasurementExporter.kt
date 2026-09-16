package com.example.measurement

import android.content.Context
import android.content.Intent
import com.example.parser.Point2D
import java.text.SimpleDateFormat
import java.util.*

object MeasurementExporter {

    fun exportToCsv(records: List<MeasurementRecord>, unitConfig: UnitConfig): String {
        val sb = StringBuilder()
        sb.append("ID,Type,Primary Value,Unit,Details,X1,Y1,X2,Y2,Is Subtraction Hole,Timestamp\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        for (r in records) {
            val p1 = r.points.getOrNull(0) ?: Point2D(0f, 0f)
            val p2 = r.points.getOrNull(1) ?: p1
            val detailsEscaped = r.secondaryDetails.entries.joinToString(";") { "${it.key}: ${it.value}" }.replace("\"", "\"\"")
            val dateStr = sdf.format(Date(r.timestamp))

            sb.append("\"${r.id}\",")
            sb.append("\"${r.toolType.name}\",")
            sb.append("\"${r.primaryFormatted}\",")
            sb.append("\"${unitConfig.displayDistanceUnit.symbol}\",")
            sb.append("\"$detailsEscaped\",")
            sb.append("%.4f,%.4f,".format(p1.x, p1.y))
            sb.append("%.4f,%.4f,".format(p2.x, p2.y))
            sb.append("${r.isHoleSubtraction},")
            sb.append("\"$dateStr\"\n")
        }

        return sb.toString()
    }

    fun exportToJson(records: List<MeasurementRecord>, unitConfig: UnitConfig): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"cadViewerMeasurementExport\": {\n")
        sb.append("    \"units\": {\n")
        sb.append("      \"distance\": \"${unitConfig.displayDistanceUnit.symbol}\",\n")
        sb.append("      \"area\": \"${unitConfig.displayAreaUnit.symbol}\",\n")
        sb.append("      \"precision\": ${unitConfig.precision}\n")
        sb.append("    },\n")
        sb.append("    \"measurementsCount\": ${records.size},\n")
        sb.append("    \"records\": [\n")

        records.forEachIndexed { index, r ->
            sb.append("      {\n")
            sb.append("        \"id\": \"${r.id}\",\n")
            sb.append("        \"title\": \"${r.title}\",\n")
            sb.append("        \"type\": \"${r.toolType.name}\",\n")
            sb.append("        \"primaryFormatted\": \"${r.primaryFormatted}\",\n")
            sb.append("        \"isHoleSubtraction\": ${r.isHoleSubtraction},\n")
            sb.append("        \"points\": [")
            sb.append(r.points.joinToString(", ") { "{\"x\": %.4f, \"y\": %.4f}".format(it.x, it.y) })
            sb.append("],\n")
            sb.append("        \"details\": {\n")
            val detailEntries = r.secondaryDetails.entries.toList()
            detailEntries.forEachIndexed { dIdx, (k, v) ->
                sb.append("          \"$k\": \"$v\"${if (dIdx < detailEntries.size - 1) "," else ""}\n")
            }
            sb.append("        }\n")
            sb.append("      }${if (index < records.size - 1) "," else ""}\n")
        }

        sb.append("    ]\n")
        sb.append("  }\n")
        sb.append("}\n")
        return sb.toString()
    }

    fun exportToFormattedReport(records: List<MeasurementRecord>, unitConfig: UnitConfig, fileName: String): String {
        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append("  CAD DRAWING MEASUREMENT REPORT\n")
        sb.append("  Drawing: $fileName\n")
        sb.append("  Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}\n")
        sb.append("  Base Unit: ${unitConfig.baseDrawingUnit.displayName}\n")
        sb.append("  Display Distance: ${unitConfig.displayDistanceUnit.displayName}\n")
        sb.append("  Display Area: ${unitConfig.displayAreaUnit.displayName}\n")
        sb.append("==================================================\n\n")

        val netArea = MeasurementEngine.computeTotalAndNetArea(records, unitConfig)
        if (netArea.regionsCount > 0) {
            sb.append("--- TOTAL / NET AREA SUMMARY ---\n")
            sb.append("Gross Area (${netArea.regionsCount} regions): ${netArea.grossAreaFormatted}\n")
            if (netArea.holesCount > 0) {
                sb.append("Holes Subtracted (${netArea.holesCount} holes): ${netArea.holesAreaFormatted}\n")
            }
            sb.append("NET AREA: ${netArea.netAreaFormatted}\n")
            sb.append("Total Perimeter: ${netArea.totalPerimeterFormatted}\n\n")
        }

        sb.append("--- RECORDED MEASUREMENTS (${records.size}) ---\n")
        records.forEachIndexed { index, r ->
            sb.append("[${index + 1}] ${r.id} - ${r.title} (${r.toolType.displayName()})\n")
            sb.append("    Value: ${r.primaryFormatted}\n")
            if (r.isHoleSubtraction) {
                sb.append("    * Subtracted as inner void/hole\n")
            }
            r.secondaryDetails.forEach { (k, v) ->
                sb.append("    • $k: $v\n")
            }
            sb.append("\n")
        }

        sb.append("==================================================\n")
        return sb.toString()
    }

    private fun MeasureToolType.displayName(): String = this.title

    fun shareExport(context: Context, text: String, title: String, mimeType: String = "text/plain") {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = mimeType
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
