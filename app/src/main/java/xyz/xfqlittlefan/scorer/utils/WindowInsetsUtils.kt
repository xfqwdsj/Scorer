package xyz.xfqlittlefan.scorer.utils

import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import xyz.xfqlittlefan.scorer.ui.composables.LocalWindowSize

operator fun WindowInsets.plus(other: WindowInsets): WindowInsets = add(other)

val WindowInsets.Companion.allBars
    @Composable get() = WindowInsets.systemBars + WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)

val WindowInsetsSides.Companion.Content
    @Composable get() = if (LocalWindowSize.current == WindowWidthSizeClass.Compact) {
        Horizontal + Bottom
    } else {
        End + Vertical
    }