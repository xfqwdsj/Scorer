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
import xyz.xfqlittlefan.scorer.util.allBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorerScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
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
                        R.string.action_back
                    )
                )
            }
        }
    },
    navigationItems: (@Composable NavigationBarScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val windowSize = LocalWindowSize.current
    Scaffold(topBar = {
        AnimatedEnterExit(
            visible = windowSize == WindowWidthSizeClass.Compact,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SmallTopAppBar(title = {
                Text(title)
            }, modifier = Modifier.windowInsetsPadding(
                WindowInsets.allBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            ), navigationIcon = navigationIcon, actions = { actions() })
        }
    }, bottomBar = {
        if (navigationItems != null) {
            AnimatedEnterExit(
                visible = windowSize == WindowWidthSizeClass.Compact,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                NavigationBar {
                    NavigationBarScope(this).navigationItems()
                }
            }
        }
    }) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedEnterExit(
                visible = windowSize != WindowWidthSizeClass.Compact,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                NavigationRail(modifier = Modifier.windowInsetsPadding(
                    WindowInsets.allBars.only(
                        WindowInsetsSides.Start + WindowInsetsSides.Vertical
                    )
                ), header = {
                    navigationIcon()
                    Text(text = title, textAlign = TextAlign.Center)
                    actions()
                }) {
                    if (navigationItems != null) {
                        NavigationBarScope(this).navigationItems()
                    }
                }
            }
            content()
        }
    }
}