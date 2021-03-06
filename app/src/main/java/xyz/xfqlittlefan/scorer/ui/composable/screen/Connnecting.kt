package xyz.xfqlittlefan.scorer.ui.composable.screen

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.ColorFilter
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
import kotlinx.serialization.SerializationException
import xyz.xfqlittlefan.scorer.BuildConfig
import xyz.xfqlittlefan.scorer.R
import xyz.xfqlittlefan.scorer.communication.Client
import xyz.xfqlittlefan.scorer.communication.RoomAddressQRCode
import xyz.xfqlittlefan.scorer.communication.RoomServerLauncher
import xyz.xfqlittlefan.scorer.communication.WebSocketServerInfo
import xyz.xfqlittlefan.scorer.ui.activity.main.LocalMainViewModel
import xyz.xfqlittlefan.scorer.ui.activity.main.MainViewModel
import xyz.xfqlittlefan.scorer.ui.activity.scanner.ScannerActivity
import xyz.xfqlittlefan.scorer.ui.composable.DropdownMenu
import xyz.xfqlittlefan.scorer.ui.composable.QRCode
import xyz.xfqlittlefan.scorer.ui.composable.ScorerScaffold
import xyz.xfqlittlefan.scorer.ui.composable.TextFieldWithMessage
import xyz.xfqlittlefan.scorer.util.*
import java.net.InetAddress
import java.net.NetworkInterface

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Connecting(
    navController: NavController,
    windowSize: WindowWidthSizeClass
) {
    val viewModel = viewModel<ConnectingScreenViewModel>()
    val mainViewModel = LocalMainViewModel.current
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("result")?.let { viewModel.onQRScanned(it) }
        }
    }

    ScorerScaffold(navController = navController,
        windowSize = windowSize,
        title = stringResource(R.string.page_title_connecting),
        actions = {
            ActionButtonCreatingRoom(viewModel, mainViewModel)
            ActionButtonScanning(viewModel, cameraPermissionState, launcher)
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
            Title(windowSize)
            Spacer(Modifier.height(20.dp))
            TextFieldHost(viewModel)
            Spacer(Modifier.height(20.dp))
            TextFieldPort(viewModel)
            Seats(viewModel)
            Spacer(Modifier.height(20.dp))
            Buttons(viewModel, mainViewModel)
        }
        if (viewModel.showRoomInfoDialog) {
            AlertDialog(onDismissRequest = viewModel::dismissRoomInfoDialog, confirmButton = {
                Button(
                    onClick = { viewModel.deleteRoom(mainViewModel) },
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
                                onDismissRequest = viewModel::dismissAddressMenu,
                                expand = fadeIn() + expandIn(),
                                collapse = shrinkOut() + fadeOut()
                            ) {
                                val context = LocalContext.current
                                DropdownMenuItem(text = { Text(stringResource(R.string.page_content_connecting_dialog_content_room_information_menu_copy)) },
                                    onClick = {
                                        viewModel.copyAddress(
                                            context, addressString
                                        )
                                        viewModel.dismissAddressMenu()
                                    })
                                DropdownMenuItem(text = { Text(stringResource(R.string.page_content_connecting_dialog_content_room_information_menu_share)) },
                                    onClick = {
                                        viewModel.shareAddress(
                                            context, addressString
                                        )
                                        viewModel.dismissAddressMenu()
                                    })
                                DropdownMenuItem(text = { Text(stringResource(R.string.page_content_connecting_dialog_content_room_information_menu_show_qr)) },
                                    onClick = {
                                        viewModel.showQR(
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
                viewModel.qrContent?.let { content ->
                    Box(Modifier.fillMaxWidth()) {
                        QRCode(
                            text = content,
                            contentDescription = stringResource(R.string.page_content_connecting_dialog_content_address_qr_description),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxSize(),
                            colorFilter = ColorFilter.colorMatrix(
                                filteredWhiteColorMatrixWithTint(
                                    MaterialTheme.colorScheme.onSurface
                                )
                            )
                        ) {
                            margin = 2
                        }
                    }
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

@Composable
internal fun ActionButtonCreatingRoom(
    viewModel: ConnectingScreenViewModel,
    mainViewModel: MainViewModel
) {
    IconButton(
        onClick = { viewModel.createRoom(mainViewModel) },
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
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun ActionButtonScanning(
    viewModel: ConnectingScreenViewModel,
    cameraPermissionState: PermissionState,
    launcher: ActivityResultLauncher<Intent>
) {
    if (cameraPermissionState.status == PermissionStatus.Granted) {
        val context = LocalContext.current
        IconButton(onClick = { viewModel.startScanningActivity(context, launcher) }) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = stringResource(R.string.page_content_connecting_action_scan_qr)
            )
        }
    } else {
        IconButton(onClick = viewModel::showRequestPermissionRationaleDialog) {
            Icon(
                imageVector = Icons.Default.Warning, contentDescription = stringResource(
                    R.string.page_content_connecting_action_request_permission
                )
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun Title(windowSize: WindowWidthSizeClass) {
    AnimatedContent(targetState = windowSize == WindowWidthSizeClass.Compact) {
        Text(
            text = stringResource(if (it) R.string.page_content_connecting_title_0 else R.string.page_content_connecting_title_1),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
internal fun TextFieldHost(viewModel: ConnectingScreenViewModel) {
    TextFieldWithMessage(value = viewModel.host,
        onValueChange = viewModel::changeHost,
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        enabled = !viewModel.showSeats,
        label = {
            Text(stringResource(R.string.page_content_connecting_text_field_host_label))
        },
        message = {
            AnimatedVisibility(
                visible = viewModel.hostError,
                enter = VerticalEnter,
                exit = VerticalExit
            )
            { Text(stringResource(R.string.page_content_connecting_text_field_host_error_message)) }
        },
        isError = viewModel.hostError
    )
}

@Composable
internal fun TextFieldPort(viewModel: ConnectingScreenViewModel) {
    TextFieldWithMessage(
        value = viewModel.port,
        onValueChange = viewModel::onPortChange,
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        enabled = !viewModel.showSeats,
        label = {
            Text(stringResource(R.string.page_content_connecting_text_field_port_label))
        },
        message = {
            AnimatedVisibility(
                visible = viewModel.portError,
                enter = VerticalEnter,
                exit = VerticalExit
            )
            { Text(stringResource(R.string.page_content_connecting_text_field_port_error_message)) }
        },
        isError = viewModel.portError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColumnScope.Seats(viewModel: ConnectingScreenViewModel) {
    AnimatedVisibility(visible = viewModel.showSeats) {
        Column {
            Spacer(Modifier.height(20.dp))
            FlowRow(
                mainAxisAlignment = FlowMainAxisAlignment.Center,
                crossAxisAlignment = FlowCrossAxisAlignment.Center
            ) {
                viewModel.seats?.forEach { seat, res, _, _ ->
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
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun Buttons(viewModel: ConnectingScreenViewModel, mainViewModel: MainViewModel) {
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
                    onClick = viewModel::getSeats,
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
                    onClick = viewModel::cancelSelectingSeats,
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

internal class ConnectingScreenViewModel : ViewModel() {
    /**
     * ????????????????????????
     */
    var host by mutableStateOf("")

    /**
     * ??????????????????????????????
     */
    var hostError by mutableStateOf(false)

    /**
     * ????????????????????????
     */
    var port by mutableStateOf("")

    /**
     * ??????????????????????????????
     */
    var portError by mutableStateOf(false)

    /**
     * ??????????????????????????????
     */
    fun changeHost(newValue: String) {
        host = newValue
        hostError = newValue.isNotEmpty() && try {
            InetAddress.getByName(newValue)
            false
        } catch (e: Throwable) {
            true
        }
    }

    /**
     * ??????????????????????????????
     */
    fun onPortChange(newValue: String) {
        port = newValue
        portError = newValue.isNotEmpty() && (newValue.toIntOrNull() == null || newValue.toInt() < 0 || newValue.toInt() > 65535)
    }

    /**
     * ?????????????????????
     */
    var showSeats by mutableStateOf(false)

    /**
     * ???????????????
     */
    var seats by mutableStateOf<Map<Int, Int>?>(null)

    /**
     * ??????????????????
     */
    var selectedSeat by mutableStateOf<Int?>(null)

    /**
     * ??????????????? Job???
     */
    var gettingSeatsJob: Job? = null

    /**
     * ???????????????
     */
    fun getSeats() {
        if (gettingSeatsJob != null) {
            LogUtil.d("Already getting seats.", "Scorer.GettingSeats")
            return
        }

        gettingSeatsJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = Client.get {
                    url {
                        protocol = URLProtocol.HTTP
                        host = this@ConnectingScreenViewModel.host
                        port = this@ConnectingScreenViewModel.port.toInt()
                        appendPathSegments("join", BuildConfig.VERSION_CODE.toString())
                    }
                }.body<String>().decodeFromJson<WebSocketServerInfo>()
                showSeats = true
                seats = info.seats
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        gettingSeatsJob?.invokeOnCompletion {
            gettingSeatsJob = null
        }
    }

    /**
     * ?????????????????????
     */
    fun cancelSelectingSeats() {
        showSeats = false
        gettingSeatsJob?.cancel()
    }

    /**
     * ??????????????????
     */
    fun clearSeats() {
        seats = null
        selectedSeat = null
    }

    /**
     * ??????????????????????????????
     */
    var showRoomInfoDialog by mutableStateOf(false)

    /**
     * ???????????????
     */
    var dialogAddresses by mutableStateOf(listOf<Pair<String, Int>>())

    /**
     * ????????????????????????
     */
    var addressMenuShowingIndex by mutableStateOf<Int?>(null)

    /**
     * ?????????????????????
     */
    fun showAddressMenu(index: Int) {
        addressMenuShowingIndex = index
    }

    /**
     * ?????????????????????
     */
    fun dismissAddressMenu() {
        addressMenuShowingIndex = null
    }

    /**
     * ??????????????????????????????
     */
    fun showRoomInfoDialog() {
        showRoomInfoDialog = true
    }

    /**
     * ??????????????????????????????
     */
    fun dismissRoomInfoDialog() {
        showRoomInfoDialog = false
    }

    /**
     * ???????????????
     */
    fun copyAddress(context: Context, address: String) {
        val clipboard = getSystemService(context, ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Scorer Address", address))
    }

    /**
     * ???????????????
     */
    fun shareAddress(context: Context, address: String) {
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

    /**
     * ???????????????
     */
    fun deleteRoom(viewModel: MainViewModel) {
        showRoomInfoDialog = false
        viewModelScope.launch(Dispatchers.IO) {
            viewModel.server?.server?.stop()
        }
    }

    /**
     * ???????????????????????????
     */
    var showQRDialog by mutableStateOf(false)

    /**
     * ??????????????????
     */
    var qrContent by mutableStateOf<String?>(null)

    /**
     * ???????????????
     */
    fun showQR(host: String, port: Int) {
        qrContent = RoomAddressQRCode(host, port).encodeToJson()
        showQRDialog = true
    }

    /**
     * ???????????????????????????
     */
    fun dismissQRDialog() {
        showQRDialog = false
        qrContent = null
    }

    /**
     * ??????????????????
     */
    fun connect(seat: Int, navController: NavController, viewModel: MainViewModel) {

    }

    /**
     * ???????????????
     */
    fun createRoom(viewModel: MainViewModel) {
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
                cancelSelectingSeats()
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
            getSeats()
        }
    }

    /**
     * ??????????????????
     */
    fun startScanningActivity(context: Context, launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(Intent(context, ScannerActivity::class.java))
    }

    /**
     * ?????????????????????????????????
     */
    fun onQRScanned(text: String) {
        try {
            val info = text.decodeFromJson<RoomAddressQRCode>()
            host = info.host
            port = info.port.toString()
            getSeats()
        } catch (e: SerializationException) {
            e.printStackTrace()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * ????????????????????????????????????
     */
    var showRequestPermissionRationaleDialog by mutableStateOf(false)

    /**
     * ????????????????????????????????????
     */
    fun showRequestPermissionRationaleDialog() {
        showRequestPermissionRationaleDialog = true
    }

    /**
     * ????????????????????????????????????
     */
    fun dismissRequestPermissionRationaleDialog() {
        showRequestPermissionRationaleDialog = false
    }

    /**
     * ???????????????
     */
    @OptIn(ExperimentalPermissionsApi::class)
    fun requestPermission(permissionState: PermissionState) {
        showRequestPermissionRationaleDialog = false
        permissionState.launchPermissionRequest()
    }

    /**
     * ??????????????????
     */
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

internal enum class ButtonType {
    GettingSeats, CancelingConnection, Connecting, ShowingRoomInfo
}