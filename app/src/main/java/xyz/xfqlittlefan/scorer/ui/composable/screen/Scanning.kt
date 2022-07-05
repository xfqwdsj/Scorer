package xyz.xfqlittlefan.scorer.ui.composable.screen

import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import xyz.xfqlittlefan.scorer.qr.QRAnalyzer
import xyz.xfqlittlefan.scorer.ui.composable.CameraX

@Composable
fun Scanning(
    navController: NavController,
    windowSize: WindowWidthSizeClass,
    viewModel: ScanningScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    CameraX(modifier = Modifier.fillMaxSize(), cameraSelectorBuilder = {
        requireLensFacing(CameraSelector.LENS_FACING_BACK)
    }, imageAnalyzer = QRAnalyzer {
        Toast.makeText(context, it.text, Toast.LENGTH_LONG).show()
    })
}

class ScanningScreenViewModel : ViewModel() {

}
