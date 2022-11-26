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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import xyz.xfqlittlefan.scorer.utils.*
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ConnectingScreenViewModel.Connecting() {
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )
    val pageController = rememberNavController()

    ScorerApp(
        title = stringResource(R.string.connecting),
        requiredActionsGroup = pageController.currentRoute,
        actions = {
            group("main") {
                ActionButtonCreatingRoom()
                ActionButtonFillingAddress(
                    cameraPermissionState, rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = this@Connecting::onActivityResult
                    )
                )
            }
            group("room_info") {
                ActionButtonDeletingRoom()
            }
        },
        showNavigationItems = LocalMainViewModel.current.server != null,
        navigationItems = {
            NavigationItemMain(this@Connecting, pageController)
            NavigationItemRoomInfo(this@Connecting, pageController)
        }
    ) {
        NavHost(navController = pageController, startDestination = "main") {
            composable("main") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.allBars.only(WindowInsetsSides.Content))
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
                    Buttons()
                }
            }
            composable("room_info") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.allBars.only(WindowInsetsSides.Content))
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ListItemRoomAddresses()
                }
            }
        }

        if (shouldShowRoomAddressesDialog) {
            DialogRoomAddresses()
        }
        if (shouldShowQRDialog) {
            DialogQR()
        }
        if (shouldShowPermissionRequestingRationaleDialog && cameraPermissionState.status is PermissionStatus.Denied) {
            DialogPermissionRequestingRationale(cameraPermissionState)
        }
    }

    val server = LocalMainViewModel.current.server
    LaunchedEffect(server) {
        if (server == null) {
            navigatePage(pageController, "main")
        }
    }
}

@Composable
internal fun ConnectingScreenViewModel.ActionButtonCreatingRoom() {
    val mainViewModel = LocalMainViewModel.current
    IconButton(
        onClick = { createRoom(mainViewModel) },
        enabled = mainViewModel.server == null && actionsEnabled
    ) {
        Icon(
            imageVector = Icons.Default.Add, contentDescription = stringResource(
                when {
                    mainViewModel.server != null -> R.string.creating_room_failed_created
                    seats != null -> R.string.creating_room_failed_connected
                    else -> R.string.create_room
                }
            )
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun ConnectingScreenViewModel.ActionButtonFillingAddress(
    cameraPermissionState: PermissionState,
    launcher: ActivityResultLauncher<Intent>
) {
    IconButton(onClick = this::showFillingOptionsMenu, enabled = actionsEnabled) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.show_filling_address_menu)
        )
        DropdownMenu(
            expanded = shouldShowFillingOptionsMenu,
            onDismissRequest = this::dismissFillingOptionsMenu
        ) {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current

            DropdownMenuItem(
                text = { Text(stringResource(R.string.scan_qr_to_fill)) },
                onClick = { scanQR(cameraPermissionState, context, launcher) })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.get_address_from_clipboard)) },
                onClick = { fillFromClipboard(clipboardManager) })

            LaunchedEffect(actionsEnabled) {
                if (!actionsEnabled) {
                    dismissFillingOptionsMenu()
                }
            }
        }
    }
}

@Composable
internal fun ConnectingScreenViewModel.ActionButtonDeletingRoom() {
    val mainViewModel = LocalMainViewModel.current
    IconButton(onClick = { deleteRoom(mainViewModel) }) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.delete_room)
        )
    }
}

@Composable
internal fun NavigationBarScope.NavigationItemMain(
    viewModel: ConnectingScreenViewModel,
    pageController: NavController
) {
    NavigationBarItem(
        selected = pageController.currentRoute == "main",
        onClick = { viewModel.navigatePage(pageController, "main") },
        icon = {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = stringResource(R.string.connecting)
            )
        },
        enabled = LocalMainViewModel.current.server != null,
        label = { Text(stringResource(R.string.connecting)) }
    )
}

@Composable
internal fun NavigationBarScope.NavigationItemRoomInfo(
    viewModel: ConnectingScreenViewModel,
    pageController: NavController
) {
    NavigationBarItem(
        selected = pageController.currentRoute == "room_info",
        onClick = { viewModel.navigatePage(pageController, "room_info") },
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.room_information)
            )
        },
        enabled = LocalMainViewModel.current.server != null,
        label = { Text(stringResource(R.string.room_information)) }
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun Title() {
    AnimatedContent(targetState = LocalWindowSize.current == WindowWidthSizeClass.Compact) {
        Text(
            text = stringResource(if (it) R.string.connect_or_create_0 else R.string.connect_or_create_1),
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
            Text(stringResource(R.string.room_host))
        },
        message = {
            AnimatedVisibility(
                visible = isHostError,
                enter = VerticalEnter,
                exit = VerticalExit
            )
            { Text(stringResource(R.string.host_invalid)) }
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
            Text(stringResource(R.string.room_port))
        },
        message = {
            AnimatedVisibility(
                visible = isPortError,
                enter = VerticalEnter,
                exit = VerticalExit
            )
            { Text(stringResource(R.string.port_invalid)) }
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
internal fun ConnectingScreenViewModel.Buttons() {
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
            text = stringResource(R.string.get_seats)
        )
        MyButton(
            enabled = gettingSeatsJob != null || shouldShowSeats,
            onClick = this@Buttons::cancelSelectingSeats,
            text = stringResource(R.string.cancel_connection)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectingScreenViewModel.ListItemRoomAddresses() {
    ListItem(
        headlineText = {
            Text(stringResource(R.string.room_addresses))
        },
        modifier = Modifier.clickable(onClick = this::showRoomAddressesDialog),
        supportingText = {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                AssistChip(
                    onClick = { },
                    label = { Text(stringResource(R.string.show_qr)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = stringResource(R.string.show_qr)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.expand)
                        )
                    }
                )
                Spacer(Modifier.width(10.dp))
                AssistChip(
                    onClick = { },
                    label = { Text(stringResource(android.R.string.copy)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(android.R.string.copy)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.expand)
                        )
                    }
                )
                Spacer(Modifier.width(10.dp))
                AssistChip(
                    onClick = { },
                    label = { Text(stringResource(R.string.share)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.expand)
                        )
                    }
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.NavigateNext,
                contentDescription = stringResource(R.string.view)
            )
        }
    )
}

@Composable
internal fun ConnectingScreenViewModel.DialogRoomAddresses() {
    fun getRecognizableText(port: String, vararg hosts: String): String {
        val recognizableHost = hosts.joinToString { "h$it" }
        return "ScorerAddresses:${recognizableHost}p$port"
    }

    val context = LocalContext.current
    val sharingMessage = stringResource(
        R.string.template_room_sharing_message
    )

    AlertDialog(
        onDismissRequest = this::dismissRoomAddressesDialog,
        confirmButton = {
            Button(onClick = {
                copyAddress(
                    context,
                    getRecognizableText(port, *roomHosts.toTypedArray())
                )
            }) {
                Text(stringResource(R.string.copy))
            }
        },
        dismissButton = {
            Button(onClick = this::dismissRoomAddressesDialog) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        title = {
            Text(stringResource(R.string.room_addresses))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                roomHosts.forEachIndexed { index, host ->
                    Box {
                        Text(
                            text = stringResource(
                                R.string.template_room_address,
                                host,
                                roomPort
                            ),
                            modifier = Modifier.clickable {
                                showAddressMenu(index)
                            }
                        )
                        DropdownMenu(
                            expanded = addressMenuShowingIndex == index,
                            onDismissRequest = this@DialogRoomAddresses::dismissAddressMenu
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.copy)) },
                                onClick = {
                                    copyAddress(
                                        context,
                                        getRecognizableText(roomPort, host)
                                    )
                                    dismissAddressMenu()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share)) },
                                onClick = {
                                    shareAddress(
                                        context,
                                        "$sharingMessage\n${getRecognizableText(roomPort, host)}"
                                    )
                                    dismissAddressMenu()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.show_qr)) },
                                onClick = {
                                    showQR(
                                        host, roomPort
                                    )
                                    dismissAddressMenu()
                                }
                            )
                        }
                    }
                    if (roomHosts.lastIndex > index) {
                        Spacer(Modifier.height(5.dp))
                    }
                }
            }
        }
    )
}

@Composable
internal fun ConnectingScreenViewModel.DialogQR() {
    AlertDialog(
        onDismissRequest = this::dismissQRDialog,
        confirmButton = {
            Button(onClick = this::dismissQRDialog) {
                Text(stringResource(android.R.string.ok))
            }
        },
        title = {
            Text(stringResource(R.string.room_addresses_qr))
        },
        text = {
            qrContent?.let { content ->
                Box(Modifier.fillMaxWidth()) {
                    QRCode(
                        text = content,
                        contentDescription = stringResource(R.string.room_addresses_qr),
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
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun ConnectingScreenViewModel.DialogPermissionRequestingRationale(
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
            Text(stringResource(R.string.request_camera_permission))
        },
        text = {
            Text(
                stringResource(R.string.request_camera_permission_summary)
            )
        }
    )
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

    var shouldShowRoomAddressesDialog by mutableStateOf(false)
        private set

    var actionsEnabled by mutableStateOf(!shouldShowSeats && gettingSeatsJob == null)
        private set

    /**
     * 在房间信息对话框中显示的主机列表。
     */
    var roomHosts = mutableStateListOf<String>()
        private set

    var roomPort by mutableStateOf("")
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

    fun showRoomAddressesDialog() {
        shouldShowRoomAddressesDialog = true
    }

    fun dismissRoomAddressesDialog() {
        shouldShowRoomAddressesDialog = false
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

    fun deleteRoom(mainViewModel: MainViewModel) {
        shouldShowRoomAddressesDialog = false
        viewModelScope.launch(Dispatchers.IO) {
            mainViewModel.server?.server?.stop()
        }
    }

    fun showQR(host: String, port: String) {
        qrContent = RoomAddressQRCode(host, port.toInt()).encodeToJson()
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
                shouldShowRoomAddressesDialog = false
                roomHosts.clear()
                addressMenuShowingIndex = null
                cancelSelectingSeats()
            }
            this@ConnectingScreenViewModel.host = host
            this@ConnectingScreenViewModel.port = port.toString()
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val ipAddresses = networkInterface.inetAddresses
                for (ipAddress in ipAddresses) {
                    if (!ipAddress.isLoopbackAddress && !ipAddress.isLinkLocalAddress) {
                        roomHosts += ipAddress.hostAddress ?: ""
                    }
                }
            }
            roomPort = port.toString()
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

    fun navigatePage(pageController: NavController, route: String) {
        pageController.navigate(route) {
            popUpTo("main") {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
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