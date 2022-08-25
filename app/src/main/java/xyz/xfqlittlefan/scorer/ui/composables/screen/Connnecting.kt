package xyz.xfqlittlefan.scorer.ui.composables.screen

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import xyz.xfqlittlefan.scorer.ui.activity.main.MainViewModel
import xyz.xfqlittlefan.scorer.ui.activity.scanner.ScannerActivity
import xyz.xfqlittlefan.scorer.ui.composables.*
import xyz.xfqlittlefan.scorer.util.*
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ConnectingScreenViewModel.Connecting() {
    val mainViewModel = LocalMainViewModel.current
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = this::onActivityResult
    )

    ScorerScaffold(
        title = stringResource(R.string.page_title_connecting),
        actions = {
            ActionButtonCreatingRoom(mainViewModel)
            ActionButtonFillAddress(cameraPermissionState, launcher)
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
            Title()
            Spacer(Modifier.height(20.dp))
            TextFieldHost()
            Spacer(Modifier.height(20.dp))
            TextFieldPort()
            Seats()
            Spacer(Modifier.height(20.dp))
            Buttons(mainViewModel)
        }
        if (shouldShowRoomInfoDialog) {
            RoomInfoDialog(mainViewModel)
        }
        if (shouldShowQRDialog) {
            QRDialog()
        }
        if (shouldShowPermissionRequestingRationaleDialog && cameraPermissionState.status is PermissionStatus.Denied) {
            RequestPermissionRationaleDialog(cameraPermissionState)
        }
    }
}

@Composable
internal fun ConnectingScreenViewModel.ActionButtonCreatingRoom(
    mainViewModel: MainViewModel
) {
    IconButton(
        onClick = { createRoom(mainViewModel) },
        enabled = mainViewModel.server == null && actionsEnabled
    ) {
        Icon(
            imageVector = Icons.Default.Add, contentDescription = stringResource(
                when {
                    mainViewModel.server != null -> R.string.page_content_connecting_action_create_disabled_created
                    seats != null -> R.string.page_content_connecting_action_create_disabled_connected
                    else -> R.string.page_content_connecting_action_create
                }
            )
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun ConnectingScreenViewModel.ActionButtonFillAddress(
    cameraPermissionState: PermissionState,
    launcher: ActivityResultLauncher<Intent>
) {
    IconButton(onClick = this::showFillingOptionsMenu, enabled = actionsEnabled) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.page_content_connecting_action_fill)
        )
        DropdownMenu(
            expanded = shouldShowFillingOptionsMenu,
            onDismissRequest = this::dismissFillingOptionsMenu
        ) {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current

            DropdownMenuItem(
                text = { Text(stringResource(R.string.page_content_connecting_action_fill_way_scan_qr)) },
                onClick = { scanQR(cameraPermissionState, context, launcher) })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.page_content_connecting_action_fill_way_from_clipboard)) },
                onClick = { fillFromClipboard(clipboardManager) })

            LaunchedEffect(actionsEnabled) {
                if (!actionsEnabled) {
                    dismissFillingOptionsMenu()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun Title() {
    AnimatedContent(targetState = LocalWindowSize.current == WindowWidthSizeClass.Compact) {
        Text(
            text = stringResource(if (it) R.string.page_content_connecting_title_0 else R.string.page_content_connecting_title_1),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectingScreenViewModel.TextFieldHost() {
    TextFieldWithMessage(
        value = host,
        onValueChange = this::changeHost,
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        enabled = actionsEnabled,
        label = {
            Text(stringResource(R.string.page_content_connecting_text_field_host_label))
        },
        message = {
            AnimatedVisibility(
                visible = isHostError,
                enter = VerticalEnter,
                exit = VerticalExit
            )
            { Text(stringResource(R.string.page_content_connecting_text_field_host_error_message)) }
        },
        isError = isHostError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectingScreenViewModel.TextFieldPort() {
    TextFieldWithMessage(
        value = port,
        onValueChange = this::changePort,
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        enabled = actionsEnabled,
        label = {
            Text(stringResource(R.string.page_content_connecting_text_field_port_label))
        },
        message = {
            AnimatedVisibility(
                visible = isPortError,
                enter = VerticalEnter,
                exit = VerticalExit
            )
            { Text(stringResource(R.string.page_content_connecting_text_field_port_error_message)) }
        },
        isError = isPortError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectingScreenViewModel.Seats() {
    AnimatedVisibility(
        visible = shouldShowSeats, enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            Spacer(Modifier.height(20.dp))
            FlowRow(
                mainAxisAlignment = FlowMainAxisAlignment.Center,
                crossAxisAlignment = FlowCrossAxisAlignment.Center
            ) {
                seats?.forEach { seat, name, _, _ ->
                    InputChip(
                        selected = selectedSeat == seat,
                        onClick = { selectSeat(seat) },
                        label = {
                            Text(name)
                        },
                        enabled = shouldShowSeats
                    )
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    clearSeats()
                }
            }
        }
    }
}

@Composable
internal fun ConnectingScreenViewModel.Buttons(mainViewModel: MainViewModel) {
    @Composable
    fun MyButton(enabled: Boolean, onClick: () -> Unit, text: String) {
        AnimatedVisibility(
            visible = enabled, enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
        ) {
            Button(
                onClick = onClick, modifier = Modifier.padding(horizontal = 5.dp),
                enabled = enabled
            ) {
                Text(text)
            }
        }
    }

    FlowRow(
        mainAxisAlignment = FlowMainAxisAlignment.Center,
        crossAxisAlignment = FlowCrossAxisAlignment.Center
    ) {
        MyButton(
            enabled = actionsEnabled,
            onClick = this@Buttons::getSeats,
            text = stringResource(R.string.page_content_connecting_button_get_seats)
        )
        MyButton(
            enabled = gettingSeatsJob != null || shouldShowSeats,
            onClick = this@Buttons::cancelSelectingSeats,
            text = stringResource(R.string.page_content_connecting_button_cancel_connection)
        )
        MyButton(
            enabled = mainViewModel.server != null,
            onClick = this@Buttons::showRoomInfoDialog,
            text = stringResource(R.string.page_content_connecting_button_room_information)
        )
    }
}

@Composable
internal fun ConnectingScreenViewModel.RoomInfoDialog(mainViewModel: MainViewModel) {
    AlertDialog(onDismissRequest = this::dismissRoomInfoDialog, confirmButton = {
        Button(
            onClick = { this.deleteRoom(mainViewModel) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(stringResource(R.string.page_content_connecting_dialog_button_room_information_0))
        }
    }, dismissButton = {
        Button(onClick = this::dismissRoomInfoDialog) {
            Text(stringResource(android.R.string.cancel))
        }
    }, title = {
        Text(stringResource(R.string.page_content_connecting_dialog_title_room_information))
    }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.page_content_connecting_dialog_subtitle_room_information))
            Spacer(Modifier.height(10.dp))
            roomInfoAddresses.forEachIndexed { index, address ->
                Box {
                    Text(text = stringResource(
                        R.string.template_room_address,
                        address.first,
                        address.second.toString()
                    ), modifier = Modifier.clickable {
                        showAddressMenu(index)
                    })
                    DropdownMenu(
                        expanded = addressMenuShowingIndex == index,
                        onDismissRequest = this@RoomInfoDialog::dismissAddressMenu
                    ) {
                        val context = LocalContext.current
                        val addressToShare = "ScorerAddress:h${address.first}p${address.second}"
                        val sharingMessage = stringResource(
                            R.string.template_room_sharing_message
                        )
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_copy)) },
                            onClick = {
                                copyAddress(
                                    context,
                                    addressToShare
                                )
                                dismissAddressMenu()
                            })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = {
                                shareAddress(
                                    context, "$sharingMessage\n$addressToShare"
                                )
                                dismissAddressMenu()
                            })
                        DropdownMenuItem(text = { Text(stringResource(R.string.page_content_connecting_dialog_content_room_information_menu_show_qr)) },
                            onClick = {
                                showQR(
                                    address.first, address.second
                                )
                                dismissAddressMenu()
                            })
                    }
                }
                if (roomInfoAddresses.lastIndex > index) {
                    Spacer(Modifier.height(5.dp))
                }
            }
        }
    })
}

@Composable
internal fun ConnectingScreenViewModel.QRDialog() {
    AlertDialog(onDismissRequest = this::dismissQRDialog, confirmButton = {
        Button(onClick = this::dismissQRDialog) {
            Text(stringResource(android.R.string.ok))
        }
    }, title = {
        Text(stringResource(R.string.page_content_connecting_dialog_title_address_qr))
    }, text = {
        qrContent?.let { content ->
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
internal fun ConnectingScreenViewModel.RequestPermissionRationaleDialog(
    cameraPermissionState: PermissionState
) {
    AlertDialog(
        onDismissRequest = this::dismissPermissionRequestingRationaleDialog,
        confirmButton = {
            Button(onClick = { requestPermission(cameraPermissionState) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Button(onClick = this::dismissPermissionRequestingRationaleDialog) {
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

class ConnectingScreenViewModel : ViewModel() {
    var host by mutableStateOf("")
        private set

    var isHostError by mutableStateOf(false)
        private set

    var port by mutableStateOf("")
        private set

    var isPortError by mutableStateOf(false)
        private set

    /**
     * 是否显示座位列表，用于控制动画。
     */
    var shouldShowSeats by mutableStateOf(false)
        private set

    var seats by mutableStateOf<Map<Int, String>?>(null)
        private set

    var selectedSeat by mutableStateOf<Int?>(null)
        private set

    var gettingSeatsJob: Job? = null
        private set

    var shouldShowRoomInfoDialog by mutableStateOf(false)
        private set

    var actionsEnabled by mutableStateOf(!shouldShowSeats && gettingSeatsJob == null)
        private set

    /**
     * 在房间信息对话框中显示的地址列表。
     */
    var roomInfoAddresses by mutableStateOf(listOf<Pair<String, Int>>())
        private set

    /**
     * 显示的地址菜单。
     */
    var addressMenuShowingIndex by mutableStateOf<Int?>(null)
        private set

    /**
     * 是否显示二维码对话框。
     */
    var shouldShowQRDialog by mutableStateOf(false)
        private set

    var qrContent by mutableStateOf<String?>(null)
        private set

    /**
     * 是否显示填充地址选项菜单。
     */
    var shouldShowFillingOptionsMenu by mutableStateOf(false)
        private set

    /**
     * 是否显示权限请求解释对话框。
     */
    var shouldShowPermissionRequestingRationaleDialog by mutableStateOf(false)
        private set

    fun changeHost(newValue: String) {
        if (actionsEnabled) {
            host = newValue
        }
        isHostError = newValue.isNotEmpty() && try {
            InetAddress.getByName(newValue)
            false
        } catch (e: Throwable) {
            true
        }
    }

    fun changePort(newValue: String) {
        if (actionsEnabled) {
            port = newValue
        }
        isPortError =
            newValue.isNotEmpty() && (newValue.toIntOrNull() == null || newValue.toInt() < 0 || newValue.toInt() > 65535)
    }

    private fun changeShouldShowSeats(newValue: Boolean) {
        shouldShowSeats = newValue
        actionsEnabled = !shouldShowSeats && gettingSeatsJob == null
    }

    private fun changeGettingSeatsJob(newValue: Job?) {
        gettingSeatsJob = newValue
        actionsEnabled = !shouldShowSeats && gettingSeatsJob == null
    }

    fun getSeats() {
        if (gettingSeatsJob != null) {
            LogUtil.d("Already getting seats.", "Scorer.GettingSeats")
            return
        }

        val finalHost = host.let {
            if (InetAddress.getByName(it) is Inet6Address) {
                "[$it]"
            } else {
                it
            }
        }

        changeGettingSeatsJob(viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = Client.get {
                    url {
                        protocol = URLProtocol.HTTP
                        host = finalHost
                        port = this@ConnectingScreenViewModel.port.toInt()
                        appendPathSegments("join", BuildConfig.VERSION_CODE.toString())
                    }
                }.body<String>().decodeFromJson<WebSocketServerInfo>()
                changeShouldShowSeats(true)
                seats = info.seats
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }.apply {
            invokeOnCompletion {
                changeGettingSeatsJob(null)
            }
        })
    }

    fun selectSeat(seat: Int) {
        selectedSeat = seat
    }

    fun cancelSelectingSeats() {
        changeShouldShowSeats(false)
        gettingSeatsJob?.cancel()
    }

    fun clearSeats() {
        seats = null
        selectedSeat = null
    }

    fun showRoomInfoDialog() {
        shouldShowRoomInfoDialog = true
    }

    fun dismissRoomInfoDialog() {
        shouldShowRoomInfoDialog = false
    }

    fun showAddressMenu(index: Int) {
        addressMenuShowingIndex = index
    }

    fun dismissAddressMenu() {
        addressMenuShowingIndex = null
    }

    fun copyAddress(context: Context, address: String) {
        val clipboard = getSystemService(context, ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Scorer Address", address))
    }

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

    fun deleteRoom(viewModel: MainViewModel) {
        shouldShowRoomInfoDialog = false
        viewModelScope.launch(Dispatchers.IO) {
            viewModel.server?.server?.stop()
        }
    }

    fun showQR(host: String, port: Int) {
        qrContent = RoomAddressQRCode(host, port).encodeToJson()
        shouldShowQRDialog = true
    }

    fun dismissQRDialog() {
        shouldShowQRDialog = false
        qrContent = null
    }

    fun connect(seat: Int, navController: NavController, viewModel: MainViewModel) {

    }

    fun createRoom(viewModel: MainViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val launcher = RoomServerLauncher(mapOf())
            launcher.server.start()
            val host = InetAddress.getLocalHost().hostAddress!!
            val port = launcher.server.resolvedConnectors().first().port
            launcher.server.environment.monitor.subscribe(ApplicationStopped) {
                viewModel.server = null
                if (this@ConnectingScreenViewModel.host == host && this@ConnectingScreenViewModel.port == port.toString()) {
                    this@ConnectingScreenViewModel.host = ""
                    this@ConnectingScreenViewModel.port = ""
                }
                shouldShowRoomInfoDialog = false
                roomInfoAddresses = emptyList()
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
                        addressesTemp += (ipAddress.hostAddress ?: "") to port
                    }
                }
            }
            roomInfoAddresses = addressesTemp
            viewModel.server = launcher
            getSeats()
        }
    }

    fun showFillingOptionsMenu() {
        if (!actionsEnabled) {
            shouldShowFillingOptionsMenu = false
            return
        }
        shouldShowFillingOptionsMenu = true
    }

    fun dismissFillingOptionsMenu() {
        shouldShowFillingOptionsMenu = false
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun scanQR(
        cameraPermissionState: PermissionState,
        context: Context,
        launcher: ActivityResultLauncher<Intent>
    ) {
        shouldShowFillingOptionsMenu = false
        if (cameraPermissionState.status is PermissionStatus.Granted) {
            launcher.launch(Intent(context, ScannerActivity::class.java))
        } else {
            shouldShowPermissionRequestingRationaleDialog = true
        }
    }

    fun onActivityResult(result: ActivityResult) {
        if (!actionsEnabled) return
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("result")?.let { text ->
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
        }
    }

    fun dismissPermissionRequestingRationaleDialog() {
        shouldShowPermissionRequestingRationaleDialog = false
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun requestPermission(permissionState: PermissionState) {
        shouldShowPermissionRequestingRationaleDialog = false
        permissionState.launchPermissionRequest()
    }

    fun fillFromClipboard(
        clipboardManager: androidx.compose.ui.platform.ClipboardManager
    ) {
        shouldShowFillingOptionsMenu = false
        if (!actionsEnabled) return
        clipboardManager.getText()?.text?.let { text ->
            Regex("ScorerAddress:h(.+)p(.+)").find(text)?.groupValues?.let {
                host = it[1]
                port = it[2]
                getSeats()
            }
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