package xyz.xfqlittlefan.scorer.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable

operator fun WindowInsets.plus(other: WindowInsets): WindowInsets = add(other)

val WindowInsets.Companion.allBars: WindowInsets
    @Composable get() = WindowInsets.systemBars + WindowInsets.displayCutout