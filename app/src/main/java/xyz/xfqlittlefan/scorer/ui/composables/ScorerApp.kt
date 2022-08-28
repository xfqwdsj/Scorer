package xyz.xfqlittlefan.scorer.ui.composables

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import xyz.xfqlittlefan.scorer.R
import xyz.xfqlittlefan.scorer.utils.allBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <ACTIONS_KEY> ScorerApp(
    title: String,
    requiredActionsGroup: ACTIONS_KEY? = null,
    actions: ScorerAppActionsScope<ACTIONS_KEY>.() -> Unit = {},
    navigationIcon: @Composable () -> Unit = {
        val navController = LocalNavController.current
        AnimatedVisibility(
            visible = navController.graph.arguments.isNotEmpty(),
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(
                        R.string.back
                    )
                )
            }
        }
    },
    showNavigationItems: Boolean = true,
    navigationItems: @Composable NavigationBarScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val windowSize = LocalWindowSize.current
    val actionsScope = ScorerAppActionsScope<ACTIONS_KEY>().apply(actions)

    Surface {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = windowSize == WindowWidthSizeClass.Compact,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SmallTopAppBar(
                    title = {
                        Text(title)
                    },
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.allBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    ),
                    navigationIcon = navigationIcon,
                    actions = {
                        actionsScope[requiredActionsGroup, Direction.Horizontal]()
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedVisibility(
                    visible = windowSize != WindowWidthSizeClass.Compact,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally()
                ) {
                    NavigationRail(
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.allBars.only(
                                WindowInsetsSides.Start + WindowInsetsSides.Vertical
                            )
                        ),
                        header = {
                            navigationIcon()
                            Text(text = title, textAlign = TextAlign.Center)
                            actionsScope[requiredActionsGroup, Direction.Vertical]()
                        }
                    ) {
                        Crossfade(targetState = showNavigationItems) {
                            if (it) {
                                Column {
                                    NavigationBarScope(this).navigationItems()
                                }
                            }
                        }
                    }
                }
                content()
            }
            AnimatedVisibility(
                visible = windowSize == WindowWidthSizeClass.Compact && showNavigationItems,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                NavigationBar(
                    Modifier.windowInsetsPadding(
                        WindowInsets.allBars.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
                ) {
                    NavigationBarScope(this).navigationItems()
                }
            }
        }
    }
}

@Composable
fun ScorerApp(
    title: String,
    navigationIcon: @Composable () -> Unit = {
        val navController = LocalNavController.current
        AnimatedVisibility(
            visible = navController.graph.arguments.isNotEmpty(),
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(
                        R.string.back
                    )
                )
            }
        }
    },
    showNavigationItems: Boolean = true,
    navigationItems: @Composable NavigationBarScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    ScorerApp<Nothing>(
        title = title,
        navigationIcon = navigationIcon,
        showNavigationItems = showNavigationItems,
        navigationItems = navigationItems,
        content = content
    )
}

class ScorerAppActionsScope<K> {
    private val groups = mutableMapOf<K, @Composable () -> Unit>()

    /**
     * 添加一组操作按钮。注意每一组之间存在顺序。
     *
     * @param key 操作按钮组的 Key。
     * @param content 操作按钮组的内容。
     */
    fun group(key: K, content: @Composable () -> Unit) {
        groups[key] = content
    }

    @OptIn(ExperimentalAnimationApi::class)
    internal operator fun get(key: K?, direction: Direction): @Composable () -> Unit = {
        (key ?: groups.keys.firstOrNull())?.let { safeKey ->
            AnimatedContent(targetState = safeKey, transitionSpec = {
                val factor =
                    if (groups.keys.indexOf(initialState) < groups.keys.indexOf(targetState)) 1 else -1
                when (direction) {
                    Direction.Vertical -> fadeIn() + slideInVertically { -it * factor / 2 } with slideOutVertically { it * factor / 2 } + fadeOut()
                    Direction.Horizontal -> fadeIn() + slideInHorizontally { it * factor / 2 } with slideOutHorizontally { -it * factor / 2 } + fadeOut()
                }.using(SizeTransform(clip = false))
            }) {
                when (direction) {
                    Direction.Vertical -> Column {
                        groups.getOrElse(it) { {} }()
                    }
                    Direction.Horizontal -> Row {
                        groups.getOrElse(it) { {} }()
                    }
                }
            }
        }
    }
}

internal enum class Direction {
    Vertical,
    Horizontal
}