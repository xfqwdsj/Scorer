package xyz.xfqlittlefan.scorer.ui.composables

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedEnterExit(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedContent(
        targetState = visible,
        modifier = modifier,
        transitionSpec = { enter with exit }
    ) {
        if (it) {
            content()
        }
    }
}

@Composable
fun AnimatedEnterExit(
    modifier: Modifier = Modifier,
    enterFrom: Alignment = Alignment.TopStart,
    exitTo: Alignment = Alignment.TopStart,
    content: (@Composable () -> Unit)?
) {
    var rememberedContent by remember { mutableStateOf(content) }


}