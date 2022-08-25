package xyz.xfqlittlefan.scorer.ui.composables

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import xyz.xfqlittlefan.scorer.ui.activity.main.MainViewModel

val LocalMainViewModel =
    staticCompositionLocalOf<MainViewModel> { error("No LocalMainViewModel provided.") }

val LocalNavController =
    staticCompositionLocalOf<NavController> { error("No LocalNavController provided.") }

val LocalWindowSize =
    compositionLocalOf<WindowWidthSizeClass> { error("No LocalWindowSize provided.") }