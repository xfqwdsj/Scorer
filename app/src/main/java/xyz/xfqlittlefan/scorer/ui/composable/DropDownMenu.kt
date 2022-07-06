package xyz.xfqlittlefan.scorer.ui.composable

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

@Composable
fun DropDownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    expand: EnterTransition = fadeIn() + expandIn(expandFrom = Alignment.BottomStart),
    collapse: ExitTransition = shrinkOut(shrinkTowards = Alignment.BottomStart) + fadeOut(),
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(expanded) }

    LaunchedEffect(expanded) {
        if (expanded) visible = true
    }

    DropdownMenu(
        expanded = expanded || visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        properties = properties
    ) {
        AnimatedEnterExit(
            visible = expanded && visible,
            enter = expand,
            exit = collapse
        ) {
            Column(content = content)

            DisposableEffect(Unit) {
                onDispose {
                    visible = false
                }
            }
        }
    }
}