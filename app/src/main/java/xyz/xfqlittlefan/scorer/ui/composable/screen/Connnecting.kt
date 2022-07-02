package xyz.xfqlittlefan.scorer.ui.composable.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
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
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.net.NetworkInterface

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
                onClick = { viewModel.onCreatingRoomButtonClick(mainViewModel) },
                enabled = mainViewModel.server == null && !viewModel.showSeats
            ) {
                Icon(
                    imageVector = Icons.Default.Add, contentDescription = stringResource(
                        when {
                            mainViewModel.server != null -> R.string.page_content_connecting_button_create_disabled_created
                            viewModel.seats != null -> R.string.page_content_connecting_button_create_disabled_connected
                            else -> R.string.page_content_connecting_button_create
                        }
                    )
                )
            }
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
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
                enabled = !viewModel.showSeats,
                label = {
                    Text(stringResource(R.string.page_content_connecting_text_field_host_label))
                })
            Spacer(Modifier.height(20.dp))
            TextField(value = viewModel.port,
                onValueChange = viewModel::onPortChange,
                enabled = !viewModel.showSeats,
                label = {
                    Text(stringResource(R.string.page_content_connecting_text_field_port_label))
                })
            AnimatedVisibility(visible = viewModel.showSeats) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    FlowRow(
                        mainAxisAlignment = FlowMainAxisAlignment.Center,
                        crossAxisAlignment = FlowCrossAxisAlignment.Center
                    ) {
                        viewModel.seats?.forEach { (seat, res) ->
                            InputChip(
                                selected = viewModel.selectedSeat == seat,
                                onClick = { viewModel.selectedSeat = seat },
                                label = {
                                    Text(stringResource(res))
                                },
                                enabled = viewModel.showSeats
                            )
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            viewModel.clearSeats()
                        }
                    }
                }
            }
            AnimatedVisibility(visible = !viewModel.showSeats) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = viewModel::onGettingSeatsButtonClick,
                        enabled = !viewModel.showSeats
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = stringResource(R.string.page_content_connecting_button_get_seats)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = viewModel.gettingSeatsJob != null || viewModel.showSeats) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = viewModel::onCancelingConnectionButtonClick,
                        enabled = viewModel.gettingSeatsJob != null || viewModel.showSeats
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.page_content_connecting_button_cancel_connection)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = mainViewModel.server != null) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = viewModel::onRoomInfoButtonClick,
                        enabled = mainViewModel.server != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.page_content_connecting_button_room_information)
                        )
                    }
                }
            }
        }
        if (viewModel.showRoomInfoDialog) {
            AlertDialog(onDismissRequest = viewModel::dismissRoomInfoDialog, confirmButton = {
                Button(onClick = { viewModel.onDeletingRoomButtonClick(mainViewModel) }) {
                    Text(stringResource(R.string.page_content_connecting_dialog_button_room_information_0))
                }
            }, dismissButton = {
                Button(onClick = viewModel::dismissRoomInfoDialog) {
                    Text(stringResource(android.R.string.cancel))
                }
            }, title = {
                Text(stringResource(R.string.page_content_connecting_dialog_title_room_information))
            }, text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.page_content_connecting_dialog_subtitle_room_information))
                    viewModel.dialogAddresses.forEachIndexed { index, address ->
                        Box {
                            Text(text = stringResource(
                                R.string.page_content_connecting_dialog_content_room_information,
                                address.first,
                                address.second
                            ), modifier = Modifier.clickable {
                                viewModel.showAddressMenu(index)
                            })
                            DropdownMenu(
                                expanded = viewModel.addressMenuShowingIndex == index,
                                onDismissRequest = viewModel::dismissAddressMenu
                            ) {
                                DropdownMenuItem(
                                    text = { Text("abc") }, onClick = viewModel::dismissAddressMenu
                                )
                            }
                        }

                    }
                }
            })
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

    var showSeats by mutableStateOf(false)
    var seats by mutableStateOf<Map<Int, Int>?>(null)
    var selectedSeat by mutableStateOf<Int?>(null)

    var gettingSeatsJob: Job? = null

    fun onGettingSeatsButtonClick() {
        gettingSeatsJob = viewModelScope.launch(Dispatchers.IO) {
            val info = client.get {
                url {
                    protocol = URLProtocol.HTTP
                    host = this@ConnectingScreenViewModel.host
                    port = this@ConnectingScreenViewModel.port.toInt()
                    appendPathSegments("join", CLIENT_VERSION.toString())
                }
            }.body<String>().decodeFromJson<WebSocketServerInfo>()
            showSeats = true
            seats = info.seats
        }
        gettingSeatsJob?.invokeOnCompletion {
            gettingSeatsJob = null
        }
    }

    fun onCancelingConnectionButtonClick() {
        showSeats = false
        gettingSeatsJob?.cancel()
    }

    fun clearSeats() {
        seats = null
        selectedSeat = null
    }

    var showRoomInfoDialog by mutableStateOf(false)
    var dialogAddresses by mutableStateOf(listOf<Pair<String, String>>())
    var addressMenuShowingIndex by mutableStateOf<Int?>(null)

    fun onRoomInfoButtonClick() {
        showRoomInfoDialog = true
    }

    fun dismissRoomInfoDialog() {
        showRoomInfoDialog = false
    }

    fun showAddressMenu(index: Int) {
        addressMenuShowingIndex = index
    }

    fun dismissAddressMenu() {
        addressMenuShowingIndex = null
    }

    fun onConnectingButtonClick(seat: Int, navController: NavController, viewModel: MainViewModel) {

    }

    fun onCreatingRoomButtonClick(viewModel: MainViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val launcher = RoomServerLauncher()
            launcher.server.start()
            val host = InetAddress.getLocalHost().hostAddress!!
            val port = launcher.server.resolvedConnectors().first().port.toString()
            launcher.server.environment.monitor.subscribe(ApplicationStopped) {
                viewModel.server = null
                if (this@ConnectingScreenViewModel.host == host && this@ConnectingScreenViewModel.port == port) {
                    this@ConnectingScreenViewModel.host = ""
                    this@ConnectingScreenViewModel.port = ""
                }
                showRoomInfoDialog = false
                dialogAddresses = emptyList()
                addressMenuShowingIndex = null
                onCancelingConnectionButtonClick()
            }
            this@ConnectingScreenViewModel.host = host
            this@ConnectingScreenViewModel.port = port
            val addressesTemp = mutableListOf<Pair<String, String>>()
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val ipAddresses = networkInterface.inetAddresses
                for (ipAddress in ipAddresses) {
                    if (!ipAddress.isAnyLocalAddress && !ipAddress.isLoopbackAddress && !ipAddress.isLinkLocalAddress) {
                        addressesTemp += ipAddress.hostAddress to port
                    }
                }
            }
            dialogAddresses = addressesTemp
            viewModel.server = launcher
            onGettingSeatsButtonClick()
        }
    }

    fun onDeletingRoomButtonClick(viewModel: MainViewModel) {
        showRoomInfoDialog = false
        viewModel.server?.server?.stop()
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