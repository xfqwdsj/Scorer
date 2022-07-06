package xyz.xfqlittlefan.scorer.ui.composable

import androidx.compose.animation.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RowScope.AnimatedEnterExit(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandHorizontally(),
    exit: ExitTransition = shrinkHorizontally() + fadeOut(),
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ColumnScope.AnimatedEnterExit(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = shrinkVertically() + fadeOut(),
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
