package xyz.xfqlittlefan.scorer.ui.activity.scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import xyz.xfqlittlefan.scorer.R
import xyz.xfqlittlefan.scorer.qr.QRAnalyzer
import xyz.xfqlittlefan.scorer.ui.composables.CameraX
import xyz.xfqlittlefan.scorer.ui.composables.LocalWindowSize
import xyz.xfqlittlefan.scorer.ui.composables.ScorerApp
import xyz.xfqlittlefan.scorer.ui.theme.ScorerTheme

class ScannerActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ScorerTheme {
                CompositionLocalProvider(LocalWindowSize provides calculateWindowSizeClass(activity = this).widthSizeClass) {
                    ScorerApp(
                        title = stringResource(R.string.scan_qr),
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close)
                                )
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .aspectRatio(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(70.dp))
                            ) {
                                CameraX(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraSelectorBuilder = {
                                        requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    },
                                    imageAnalyzer = QRAnalyzer(
                                        onSuccessListener = { barcodes ->
                                            barcodes.firstOrNull()?.let {
                                                setResult(RESULT_OK, Intent().apply {
                                                    putExtra("result", it.rawValue)
                                                })
                                                finish()
                                            }
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}