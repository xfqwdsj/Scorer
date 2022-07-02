package xyz.xfqlittlefan.scorer.ui.composable.screen

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import xyz.xfqlittlefan.scorer.R
import xyz.xfqlittlefan.scorer.ui.composable.ScorerScaffold
import java.net.URI

@Composable
fun Main(
    host: String,
    port: Int,
    password: Int,
    seat: Int,
    isServer: Boolean = false,
    navController: NavController,
    windowSize: WindowWidthSizeClass,
    viewModel: MainScreenViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            modelClass.getConstructor(URI::class.java).newInstance(run {
                URI("ws", "", host, port, "/$password/$seat", "", "")
            })
    })
) {
    /*
    val ips = mutableListOf<String>()
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (networkInterface in interfaces) {
        val ipAddresses = networkInterface.inetAddresses
        for (ipAddress in ipAddresses) {
            if (!ipAddress.isAnyLocalAddress && !ipAddress.isLoopbackAddress && !ipAddress.isLinkLocalAddress) {
                ips += if (ipAddress is Inet6Address) {
                    "[${ipAddress.hostAddress}]:$port"
                } else {
                    "${ipAddress.hostAddress}:$port"
                }
            }
        }
    }
     */

    DisposableEffect(Unit) {
        onDispose {

        }
    }

    ScorerScaffold(
        navController = navController,
        windowSize = windowSize,
        title = stringResource(R.string.page_title_main)
    ) {
        val mainNavController = rememberNavController()
        NavHost(navController = mainNavController, startDestination = "room") {
            composable("room") {
                Room()
            }
            if (isServer) {
                composable("management") {
                    Management()
                }
            }
        }
    }
}

@Composable
fun Room() {

}

@Composable
fun Management() {

}

class MainScreenViewModel(val address: URI) : ViewModel()