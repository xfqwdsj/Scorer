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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
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
            RoomInfoDialog(viewModel, mainViewModel)
        }
        if (viewModel.showQRDialog) {
            QRDialog(viewModel)
        }
        if (viewModel.showRequestPermissionRationaleDialog && cameraPermissionState.status is PermissionStatus.Denied) {
            RequestPermissionRationaleDialog(viewModel, cameraPermissionState)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextFieldHost(viewModel: ConnectingScreenViewModel) {
    TextFieldWithMessage(
        value = viewModel.host,
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

@OptIn(ExperimentalMaterial3Api::class)
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

@Composable
internal fun RoomInfoDialog(viewModel: ConnectingScreenViewModel, mainViewModel: MainViewModel) {
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
                    val addressToShow = stringResource(
                        R.string.template_room_address,
                        address.first,
                        address.second
                    )
                    Text(text = addressToShow, modifier = Modifier.clickable {
                        viewModel.showAddressMenu(index)
                    })
                    DropdownMenu(
                        expanded = viewModel.addressMenuShowingIndex == index,
                        onDismissRequest = viewModel::dismissAddressMenu,
                        expand = fadeIn() + expandIn(),
                        collapse = shrinkOut() + fadeOut()
                    ) {
                        val context = LocalContext.current
                        val sharingMessage = stringResource(
                            R.string.template_room_sharing_message,
                            "ScorerAddress:h${address.first}p${address.second}"
                        )
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_copy)) },
                            onClick = {
                                viewModel.copyAddress(
                                    context, addressToShow
                                )
                                viewModel.dismissAddressMenu()
                            })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_share)) },
                            onClick = {
                                viewModel.shareAddress(
                                    context, sharingMessage
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

@Composable
internal fun QRDialog(viewModel: ConnectingScreenViewModel) {
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun RequestPermissionRationaleDialog(
    viewModel: ConnectingScreenViewModel,
    cameraPermissionState: PermissionState
) {
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

internal class ConnectingScreenViewModel : ViewModel() {
    /**
     * 输入的房间地址。
     */
    var host by mutableStateOf("")

    /**
     * 地址输入框错误状态。
     */
    var hostError by mutableStateOf(false)

    /**
     * 输入的房间端口。
     */
    var port by mutableStateOf("")

    /**
     * 端口输入框错误状态。
     */
    var portError by mutableStateOf(false)

    /**
     * 更改输入的房间地址。
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
     * 更改输入的房间端口。
     */
    fun onPortChange(newValue: String) {
        port = newValue
        portError =
            newValue.isNotEmpty() && (newValue.toIntOrNull() == null || newValue.toInt() < 0 || newValue.toInt() > 65535)
    }

    /**
     * 用于控制动画。
     */
    var showSeats by mutableStateOf(false)

    /**
     * 座位列表。
     */
    var seats by mutableStateOf<Map<Int, Int>?>(null)

    /**
     * 选择的座位。
     */
    var selectedSeat by mutableStateOf<Int?>(null)

    /**
     * 获取座位的 Job。
     */
    var gettingSeatsJob: Job? = null

    /**
     * 获取座位。
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
     * 取消选择座位。
     */
    fun cancelSelectingSeats() {
        showSeats = false
        gettingSeatsJob?.cancel()
    }

    /**
     * 清除座位列表
     */
    fun clearSeats() {
        seats = null
        selectedSeat = null
    }

    /**
     * 显示房间信息对话框。
     */
    var showRoomInfoDialog by mutableStateOf(false)

    /**
     * 地址列表。
     */
    var dialogAddresses by mutableStateOf(listOf<Pair<String, Int>>())

    /**
     * 显示的地址菜单。
     */
    var addressMenuShowingIndex by mutableStateOf<Int?>(null)

    /**
     * 显示地址菜单。
     */
    fun showAddressMenu(index: Int) {
        addressMenuShowingIndex = index
    }

    /**
     * 关闭地址菜单。
     */
    fun dismissAddressMenu() {
        addressMenuShowingIndex = null
    }

    /**
     * 显示房间信息对话框。
     */
    fun showRoomInfoDialog() {
        showRoomInfoDialog = true
    }

    /**
     * 关闭房间信息对话框。
     */
    fun dismissRoomInfoDialog() {
        showRoomInfoDialog = false
    }

    /**
     * 复制地址。
     */
    fun copyAddress(context: Context, address: String) {
        val clipboard = getSystemService(context, ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Scorer Address", address))
    }

    /**
     * 分享地址。
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
     * 删除房间。
     */
    fun deleteRoom(viewModel: MainViewModel) {
        showRoomInfoDialog = false
        viewModelScope.launch(Dispatchers.IO) {
            viewModel.server?.server?.stop()
        }
    }

    /**
     * 显示二维码对话框。
     */
    var showQRDialog by mutableStateOf(false)

    /**
     * 二维码内容。
     */
    var qrContent by mutableStateOf<String?>(null)

    /**
     * 显示二维码
     */
    fun showQR(host: String, port: Int) {
        qrContent = RoomAddressQRCode(host, port).encodeToJson()
        showQRDialog = true
    }

    /**
     * 关闭二维码对话框。
     */
    fun dismissQRDialog() {
        showQRDialog = false
        qrContent = null
    }

    /**
     * 连接到房间。
     */
    fun connect(seat: Int, navController: NavController, viewModel: MainViewModel) {

    }

    /**
     * 创建房间。
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
                    addressesTemp += "0" to 0
                    if (!ipAddress.isLoopbackAddress && !ipAddress.isLinkLocalAddress) {
                        addressesTemp += (ipAddress.hostAddress ?: "") to port
                    }
                }
            }
            dialogAddresses = addressesTemp
            viewModel.server = launcher
            getSeats()
        }
    }

    /**
     * 扫描二维码。
     */
    fun startScanningActivity(context: Context, launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(Intent(context, ScannerActivity::class.java))
    }

    /**
     * 二维码被扫描后的回调。
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
     * 显示权限请求解释对话框。
     */
    var showRequestPermissionRationaleDialog by mutableStateOf(false)

    /**
     * 显示权限请求解释对话框。
     */
    fun showRequestPermissionRationaleDialog() {
        showRequestPermissionRationaleDialog = true
    }

    /**
     * 关闭权限请求解释对话框。
     */
    fun dismissRequestPermissionRationaleDialog() {
        showRequestPermissionRationaleDialog = false
    }

    /**
     * 请求权限。
     */
    @OptIn(ExperimentalPermissionsApi::class)
    fun requestPermission(permissionState: PermissionState) {
        showRequestPermissionRationaleDialog = false
        permissionState.launchPermissionRequest()
    }

    /**
     * 导航到主页。
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