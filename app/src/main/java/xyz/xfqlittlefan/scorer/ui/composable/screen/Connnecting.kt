package xyz.xfqlittlefan.scorer.ui.composable.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.xfqlittlefan.scorer.R
import xyz.xfqlittlefan.scorer.communication.*
import xyz.xfqlittlefan.scorer.ui.activity.main.LocalMainViewModel
import xyz.xfqlittlefan.scorer.ui.activity.main.MainViewModel
import xyz.xfqlittlefan.scorer.ui.composable.ScorerScaffold
import xyz.xfqlittlefan.scorer.util.allBars
import xyz.xfqlittlefan.scorer.util.decodeFromJson
import xyz.xfqlittlefan.scorer.util.encodeToJson
import xyz.xfqlittlefan.scorer.util.generateQR
import java.net.InetAddress
import java.net.NetworkInterface

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class
)
@Composable
fun Connecting(
    navController: NavController,
    windowSize: WindowWidthSizeClass,
    viewModel: ConnectingScreenViewModel = viewModel()
) {
    val mainViewModel = LocalMainViewModel.current
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

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
                            mainViewModel.server != null -> R.string.page_content_connecting_action_create_disabled_created
                            viewModel.seats != null -> R.string.page_content_connecting_action_create_disabled_connected
                            else -> R.string.page_content_connecting_action_create
                        }
                    )
                )
            }
            if (cameraPermissionState.status == PermissionStatus.Granted) {
                IconButton(onClick = { viewModel.onScanningQRButtonClick(navController) }) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.page_content_connecting_action_scan_qr)
                    )
                }
            } else {
                IconButton(onClick = viewModel::onRequestingPermissionButtonClick) {
                    Icon(
                        imageVector = Icons.Default.Warning, contentDescription = stringResource(
                            R.string.page_content_connecting_action_request_permission
                        )
                    )
                }
            }
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.allBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(targetState = windowSize) {
                Text(
                    text = stringResource(if (it == WindowWidthSizeClass.Compact) R.string.page_content_connecting_title_0 else R.string.page_content_connecting_title_1),
                    textAlign = TextAlign.Center,
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
            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = mapOf(
                    ButtonType.GettingSeats to !viewModel.showSeats,
                    ButtonType.CancelingConnection to (viewModel.gettingSeatsJob != null || viewModel.showSeats),
                    ButtonType.ShowingRoomInfo to (mainViewModel.server != null)
                )
            ) { buttonVisibility ->
                FlowRow(
                    mainAxisAlignment = FlowMainAxisAlignment.Center,
                    crossAxisAlignment = FlowCrossAxisAlignment.Center
                ) {
                    if (buttonVisibility[ButtonType.GettingSeats] == true) {
                        Button(
                            onClick = viewModel::onGettingSeatsButtonClick,
                            modifier = Modifier.padding(horizontal = 5.dp),
                            enabled = buttonVisibility[ButtonType.GettingSeats] == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = stringResource(R.string.page_content_connecting_button_get_seats)
                            )
                        }
                    }
                    if (buttonVisibility[ButtonType.CancelingConnection] == true) {
                        Button(
                            onClick = viewModel::onCancelingConnectionButtonClick,
                            modifier = Modifier.padding(horizontal = 5.dp),
                            enabled = buttonVisibility[ButtonType.CancelingConnection] == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = stringResource(R.string.page_content_connecting_button_cancel_connection)
                            )
                        }
                    }
                    if (buttonVisibility[ButtonType.ShowingRoomInfo] == true) {
                        Button(
                            onClick = viewModel::showRoomInfoDialog,
                            modifier = Modifier.padding(horizontal = 5.dp),
                            enabled = buttonVisibility[ButtonType.ShowingRoomInfo] == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.page_content_connecting_button_room_information)
                            )
                        }
                    }
                }
            }
        }
        if (viewModel.showRoomInfoDialog) {
            AlertDialog(onDismissRequest = viewModel::dismissRoomInfoDialog, confirmButton = {
                Button(
                    onClick = { viewModel.onDeletingRoomButtonClick(mainViewModel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
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
                    Spacer(Modifier.height(10.dp))
                    viewModel.dialogAddresses.forEachIndexed { index, address ->
                        Box {
                            val addressString = stringResource(
                                R.string.page_content_connecting_dialog_content_room_information,
                                address.first,
                                address.second
                            )
                            Text(text = addressString, modifier = Modifier.clickable {
                                viewModel.showAddressMenu(index)
                            })
                            DropdownMenu(
                                expanded = viewModel.addressMenuShowingIndex == index,
                                onDismissRequest = viewModel::dismissAddressMenu
                            ) {
                                val context = LocalContext.current
                                DropdownMenuItem(text = { Text(stringResource(R.string.page_content_connecting_dialog_content_room_information_menu_copy)) },
                                    onClick = {
                                        viewModel.onAddressMenuItemCopingClick(
                                            context, addressString
                                        )
                                        viewModel.dismissAddressMenu()
                                    })
                                DropdownMenuItem(text = { Text(stringResource(R.string.page_content_connecting_dialog_content_room_information_menu_share)) },
                                    onClick = {
                                        viewModel.onAddressMenuItemSharingClick(
                                            context, addressString
                                        )
                                        viewModel.dismissAddressMenu()
                                    })
                                DropdownMenuItem(text = { Text(stringResource(R.string.page_content_connecting_dialog_content_room_information_menu_show_qr)) },
                                    onClick = {
                                        viewModel.showQRDialog(
                                            address.first, address.second
                                        )
                                        viewModel.dismissAddressMenu()
                                    })
                            }
                        }
                        if (viewModel.dialogAddresses.lastIndex > index) {
                            Spacer(Modifier.height(5.dp))
                        }
                    }
                }
            })
        }
        if (viewModel.showQRDialog) {
            AlertDialog(onDismissRequest = viewModel::dismissQRDialog, confirmButton = {
                Button(onClick = viewModel::dismissQRDialog) {
                    Text(stringResource(android.R.string.ok))
                }
            }, title = {
                Text(stringResource(R.string.page_content_connecting_dialog_title_address_qr))
            }, text = {
                if (viewModel.qr != null) {
                    Image(
                        bitmap = viewModel.qr!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.page_content_connecting_dialog_content_address_qr_description),
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
            })
        }
        if (viewModel.showRequestPermissionRationaleDialog && cameraPermissionState.status is PermissionStatus.Denied) {
            AlertDialog(onDismissRequest = viewModel::dismissRequestPermissionRationaleDialog,
                confirmButton = {
                    Button(onClick = { viewModel.requestPermission(cameraPermissionState) }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    Button(onClick = viewModel::dismissRequestPermissionRationaleDialog) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
                title = {
                    Text(stringResource(R.string.page_content_connecting_dialog_title_request_permission_rationale))
                },
                text = {
                    Text(
                        stringResource(R.string.page_content_connecting_dialog_content_request_permission_rationale)
                    )
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
    var dialogAddresses by mutableStateOf(listOf<Pair<String, Int>>())
    var addressMenuShowingIndex by mutableStateOf<Int?>(null)

    fun showRoomInfoDialog() {
        showRoomInfoDialog = true
    }

    fun dismissRoomInfoDialog() {
        showRoomInfoDialog = false
    }

    fun showAddressMenu(index: Int) {
        addressMenuShowingIndex = index
    }

    fun onAddressMenuItemCopingClick(context: Context, address: String) {
        val clipboard = getSystemService(context, ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Scorer Address", address))
    }

    fun onAddressMenuItemSharingClick(context: Context, address: String) {
        context.startActivity(
            Intent.createChooser(
                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, address)
                }, null
            )
        )
    }

    fun dismissAddressMenu() {
        addressMenuShowingIndex = null
    }

    var showQRDialog by mutableStateOf(false)
    var qr by mutableStateOf<Bitmap?>(null)

    fun showQRDialog(address: String, port: Int) {
        qr = generateQR(RoomAddressQRCode(address, port).encodeToJson())
        showQRDialog = true
    }

    fun dismissQRDialog() {
        showQRDialog = false
        qr = null
    }

    fun onConnectingButtonClick(seat: Int, navController: NavController, viewModel: MainViewModel) {

    }

    fun onCreatingRoomButtonClick(viewModel: MainViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val launcher = RoomServerLauncher()
            launcher.server.start()
            val host = InetAddress.getLocalHost().hostAddress!!
            val port = launcher.server.resolvedConnectors().first().port
            launcher.server.environment.monitor.subscribe(ApplicationStopped) {
                viewModel.server = null
                if (this@ConnectingScreenViewModel.host == host && this@ConnectingScreenViewModel.port == port.toString()) {
                    this@ConnectingScreenViewModel.host = ""
                    this@ConnectingScreenViewModel.port = ""
                }
                showRoomInfoDialog = false
                dialogAddresses = emptyList()
                addressMenuShowingIndex = null
                onCancelingConnectionButtonClick()
            }
            this@ConnectingScreenViewModel.host = host
            this@ConnectingScreenViewModel.port = port.toString()
            val addressesTemp = mutableListOf<Pair<String, Int>>()
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val ipAddresses = networkInterface.inetAddresses
                for (ipAddress in ipAddresses) {
                    if (!ipAddress.isLoopbackAddress && !ipAddress.isLinkLocalAddress) {
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
        viewModelScope.launch(Dispatchers.IO) {
            viewModel.server?.server?.stop()
        }
    }

    var showRequestPermissionRationaleDialog by mutableStateOf(false)

    fun onRequestingPermissionButtonClick() {
        showRequestPermissionRationaleDialog = true
    }

    fun dismissRequestPermissionRationaleDialog() {
        showRequestPermissionRationaleDialog = false
    }

    fun onScanningQRButtonClick(navController: NavController) {
        navController.navigate("scanning")
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun requestPermission(permissionState: PermissionState) {
        showRequestPermissionRationaleDialog = false
        permissionState.launchPermissionRequest()
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

enum class ButtonType {
    GettingSeats, CancelingConnection, Connecting, ShowingRoomInfo
}