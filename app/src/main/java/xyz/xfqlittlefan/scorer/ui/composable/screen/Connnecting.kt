@file:OptIn(ExperimentalAnimationApi::class)

package xyz.xfqlittlefan.scorer.ui.composable.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.xfqlittlefan.scorer.R
import xyz.xfqlittlefan.scorer.communication.CLIENT_VERSION
import xyz.xfqlittlefan.scorer.communication.RoomServerLauncher
import xyz.xfqlittlefan.scorer.communication.WebSocketServerInfo
import xyz.xfqlittlefan.scorer.communication.client
import xyz.xfqlittlefan.scorer.ui.activity.main.LocalMainViewModel
import xyz.xfqlittlefan.scorer.ui.activity.main.MainViewModel
import xyz.xfqlittlefan.scorer.ui.composable.ScorerScaffold
import xyz.xfqlittlefan.scorer.util.decodeFromJson
import java.net.InetAddress

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Connecting(
    navController: NavController,
    windowSize: WindowWidthSizeClass,
    viewModel: ConnectingScreenViewModel = viewModel()
) {
    val mainViewModel = LocalMainViewModel.current
    ScorerScaffold(navController = navController,
        windowSize = windowSize,
        title = stringResource(R.string.page_title_connecting),
        actions = {
            IconButton(
                onClick = { viewModel.onCreatingServerButtonClick(mainViewModel) },
                enabled = mainViewModel.server == null
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.page_content_connecting_button_create)
                )
            }
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(targetState = windowSize) {
                Text(
                    text = stringResource(if (it == WindowWidthSizeClass.Compact) R.string.page_content_connecting_title_0 else R.string.page_content_connecting_title_1),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(20.dp))
            TextField(value = viewModel.host,
                onValueChange = viewModel::onHostChange,
                enabled = viewModel.seats == null,
                label = {
                    Text(stringResource(R.string.page_content_connecting_text_field_host_label))
                })
            Spacer(Modifier.height(20.dp))
            TextField(value = viewModel.port,
                onValueChange = viewModel::onPortChange,
                enabled = viewModel.seats == null,
                label = {
                    Text(stringResource(R.string.page_content_connecting_text_field_port_label))
                })
            AnimatedVisibility(visible = viewModel.seats != null) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    FlowRow(
                        mainAxisAlignment = FlowMainAxisAlignment.Center,
                        crossAxisAlignment = FlowCrossAxisAlignment.Center
                    ) {
                        viewModel.seats?.forEach { (seat, res) ->
                            InputChip(selected = viewModel.selectedSeat == seat,
                                onClick = { viewModel.selectedSeat = seat },
                                label = {
                                    Text(stringResource(res))
                                })
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = viewModel::onGettingSeatsButtonClick) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.page_content_connecting_button_get_seats)
                )
            }
        }
    }
}

class ConnectingScreenViewModel : ViewModel() {
    var host by mutableStateOf("")
    fun onHostChange(newValue: String) {
        host = newValue
    }

    var port by mutableStateOf("")
    fun onPortChange(newValue: String) {
        port = newValue
    }

    var seats by mutableStateOf<Map<Int, Int>?>(null)
    var selectedSeat by mutableStateOf<Int?>(null)

    fun onGettingSeatsButtonClick() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = client.get {
                url {
                    protocol = URLProtocol.HTTP
                    host = this@ConnectingScreenViewModel.host
                    port = this@ConnectingScreenViewModel.port.toInt()
                    appendPathSegments("join", CLIENT_VERSION.toString())
                }
            }.body<String>().decodeFromJson<WebSocketServerInfo>()
            seats = info.seats
        }
    }

    fun onConnectingButtonClick(seat: Int, navController: NavController, viewModel: MainViewModel) {

    }

    fun onCreatingServerButtonClick(viewModel: MainViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val launcher = RoomServerLauncher()
            launcher.server.start()
            viewModel.server = launcher
            host = InetAddress.getLocalHost().hostAddress!!
            port = launcher.server.resolvedConnectors().first().port.toString()
        }
    }

    private fun NavController.navigateToMain(
        host: String, port: Int, password: Int, seat: Int, isServer: Boolean = false
    ) {
        navigate(
            "main/$host/$port/$password/$seat?isServer=$isServer"
        ) {
            popUpTo(graph.startDestinationId) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}