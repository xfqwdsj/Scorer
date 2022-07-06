package xyz.xfqlittlefan.scorer.util

import androidx.compose.ui.graphics.ColorMatrix

val InvertedColorMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
)

val InvertedDarkerColorMatrix = InvertedColorMatrix.apply {
    setToScale(0.5f, 0.5f, 0.5f, 1f)
}