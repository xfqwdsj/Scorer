package xyz.xfqlittlefan.scorer.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = containerColor,
        tonalElevation = tonalElevation
    ) {
        androidx.compose.material3.NavigationBar(
            modifier = modifier,
            containerColor = Color.Transparent,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            content = content
        )
    }
}

@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.background(containerColor)) {
        androidx.compose.material3.NavigationRail(
            modifier = modifier,
            containerColor = Color.Transparent,
            contentColor = contentColor,
            header = header,
            content = content
        )
    }
}

class NavigationBarScope(private val scope: Any) {
    @Composable
    fun NavigationBarItem(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        label: @Composable (() -> Unit)? = null
    ) {
        when (scope) {
            is ColumnScope -> {
                NavigationRailItem(
                    selected = selected,
                    onClick = onClick,
                    icon = icon,
                    modifier = modifier,
                    enabled = enabled,
                    label = label
                )
            }
            is RowScope -> {
                scope.NavigationBarItem(
                    selected = selected,
                    onClick = onClick,
                    icon = icon,
                    modifier = modifier,
                    enabled = enabled,
                    label = label
                )
            }
        }
    }
}