package com.example.parser

import androidx.compose.ui.graphics.Color

/**
 * AutoCAD Color Index (ACI) lookup table for CAD standard colors.
 * Standard basic colors 1-7:
 * 1: Red, 2: Yellow, 3: Green, 4: Cyan, 5: Blue, 6: Magenta, 7: White/Black (by theme)
 */
object AciColors {
    val Red = Color(0xFFFF3333)
    val Yellow = Color(0xFFFFD700)
    val Green = Color(0xFF00E676)
    val Cyan = Color(0xFF00E5FF)
    val Blue = Color(0xFF2979FF)
    val Magenta = Color(0xFFFF4081)
    val White = Color(0xFFF0F4F8)
    val DarkGrey = Color(0xFF78909C)
    val LightGrey = Color(0xFFB0BEC5)
    val Orange = Color(0xFFFF9100)
    val Brown = Color(0xFF8D6E63)

    fun getColor(colorIndex: Int, defaultColor: Color = Cyan): Color {
        return when (colorIndex) {
            1 -> Red
            2 -> Yellow
            3 -> Green
            4 -> Cyan
            5 -> Blue
            6 -> Magenta
            7 -> White
            8 -> DarkGrey
            9 -> LightGrey
            10 -> Color(0xFFFF0000)
            20 -> Color(0xFFFF7F00)
            30 -> Color(0xFFFFAA00)
            40 -> Color(0xFFFFD400)
            50 -> Color(0xFFFFFF00)
            60 -> Color(0xFFAAFF00)
            70 -> Color(0xFF55FF00)
            80 -> Color(0xFF00FF00)
            90 -> Color(0xFF00FFAA)
            100 -> Color(0xFF00FFD4)
            110 -> Color(0xFF00FFFF)
            120 -> Color(0xFF00AAFF)
            130 -> Color(0xFF0055FF)
            140 -> Color(0xFF0000FF)
            150 -> Color(0xFF5500FF)
            160 -> Color(0xFFAA00FF)
            170 -> Color(0xFFFF00FF)
            180 -> Color(0xFFFF00AA)
            190 -> Color(0xFFFF0055)
            200 -> Color(0xFF7F003F)
            210 -> Color(0xFFBF005F)
            220 -> Color(0xFFFF3F7F)
            230 -> Color(0xFFFF7FAF)
            240 -> Color(0xFFFFBFDF)
            250 -> Color(0xFF333333)
            251 -> Color(0xFF505050)
            252 -> Color(0xFF696969)
            253 -> Color(0xFF828282)
            254 -> Color(0xFFBEBEBE)
            255 -> Color(0xFFFFFFFF)
            else -> defaultColor
        }
    }
}
