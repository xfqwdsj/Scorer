package xyz.xfqlittlefan.scorer.ui.activity.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import io.ktor.server.engine.*
import xyz.xfqlittlefan.scorer.communication.RoomServerLauncher

val LocalMainViewModel = staticCompositionLocalOf { MainViewModel() }

class MainViewModel : ViewModel() {
    var server by mutableStateOf<RoomServerLauncher?>(null)
    //var client by mutableStateOf<RoomClient?>(null)
}