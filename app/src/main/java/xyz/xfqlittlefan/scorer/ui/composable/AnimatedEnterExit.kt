package xyz.xfqlittlefan.scorer.ui.composable

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
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