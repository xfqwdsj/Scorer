package xyz.xfqlittlefan.scorer.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.xfqlittlefan.scorer.ui.composables.LocalMainViewModel
import xyz.xfqlittlefan.scorer.ui.composables.LocalNavController
import xyz.xfqlittlefan.scorer.ui.composables.LocalWindowSize
import xyz.xfqlittlefan.scorer.ui.composables.screen.Composable
import xyz.xfqlittlefan.scorer.ui.composables.screen.ConnectingScreenViewModel
import xyz.xfqlittlefan.scorer.ui.composables.screen.Main
import xyz.xfqlittlefan.scorer.ui.theme.ScorerTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ScorerTheme {
                val navController = rememberNavController()

                CompositionLocalProvider(
                    LocalMainViewModel provides viewModel(),
                    LocalNavController provides navController,
                    LocalWindowSize provides calculateWindowSizeClass(activity = this).widthSizeClass
                ) {
                    NavHost(navController = navController, startDestination = "connecting") {
                        composable("connecting") {
                            viewModel<ConnectingScreenViewModel>().Composable()
                        }
                        composable(
                            "main/{host}/{port}/{password}/{seat}?isServer={isServer}",
                            listOf(navArgument("host") {
                                type = NavType.StringType
                            }, navArgument("port") {
                                type = NavType.IntType
                            }, navArgument("password") {
                                type = NavType.IntType
                            }, navArgument("seat") {
                                type = NavType.IntType
                            }, navArgument("isServer") {
                                type = NavType.BoolType
                                defaultValue = false
                            })
                        ) {
                            Main(
                                host = it.arguments!!.getString("address")!!,
                                port = it.arguments!!.getInt("port"),
                                password = it.arguments!!.getInt("password"),
                                seat = it.arguments!!.getInt("seat"),
                                isServer = it.arguments!!.getBoolean("isServer", false)
                            )
                        }

                    }
                }
            }
        }
    }
}