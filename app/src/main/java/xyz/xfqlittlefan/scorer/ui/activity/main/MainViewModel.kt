package xyz.xfqlittlefan.scorer.ui.activity.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import xyz.xfqlittlefan.scorer.communication.RoomServerLauncher

class MainViewModel : ViewModel() {
    var server by mutableStateOf<RoomServerLauncher?>(null)
    //var client by mutableStateOf<RoomClient?>(null)
}