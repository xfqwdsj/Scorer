package xyz.xfqlittlefan.scorer.utils

import androidx.compose.animation.*

val HorizontalEnter = fadeIn() + expandHorizontally()
val HorizontalExit = shrinkHorizontally() + fadeOut()

val VerticalEnter = fadeIn() + expandVertically()
val VerticalExit = shrinkVertically() + fadeOut()