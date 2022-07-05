package xyz.xfqlittlefan.scorer.ui.composable.screen

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import xyz.xfqlittlefan.scorer.qr.QRAnalyzer
import xyz.xfqlittlefan.scorer.ui.composable.CameraX
import xyz.xfqlittlefan.scorer.util.sendResult

@Composable
fun Scanning(
    navController: NavController,
    windowSize: WindowWidthSizeClass,
    viewModel: ScanningScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    CameraX(modifier = Modifier.fillMaxSize(), cameraSelectorBuilder = {
        requireLensFacing(CameraSelector.LENS_FACING_BACK)
    }, imageAnalyzer = QRAnalyzer(onResult = {
        navController.sendResult("qr_result", it.text)
        navController.popBackStack()
    }, onFormatNotSupported = {
        navController.popBackStack()
    }, onOtherException = {
        it.printStackTrace()
        navController.popBackStack()
    }))
}

class ScanningScreenViewModel : ViewModel() {

}
