package xyz.xfqlittlefan.scorer.ui.composable.screen

import androidx.camera.view.PreviewView
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

@Composable
fun Scanning(
    navController: NavController,
    windowSize: WindowWidthSizeClass,
    viewModel: ScanningScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    AndroidView(factory = {
        PreviewView(it)
    })
}

class ScanningScreenViewModel : ViewModel() {

}
