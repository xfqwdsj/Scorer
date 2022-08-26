package xyz.xfqlittlefan.scorer.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix

fun filteredWhiteColorMatrixWithTint(tint: Color) = ColorMatrix(
    floatArrayOf(
        0f, 0f, 0f, 0f, tint.red * 255,
        0f, 0f, 0f, 0f, tint.green * 255,
        0f, 0f, 0f, 0f, tint.blue * 255,
        -1f, -1f, -1f, 0f, 255f * 3
    )
)