package xyz.xfqlittlefan.scorer.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix

fun filteredWhiteColorMatrixWithTint(tint: Color) = ColorMatrix(
    floatArrayOf(
        0f, 0f, 0f, 0f, tint.red,
        0f, 0f, 0f, 0f, tint.blue,
        0f, 0f, 0f, 0f, tint.blue,
        -1f, -1f, -1f, 0f, 255f * 3
    )
)