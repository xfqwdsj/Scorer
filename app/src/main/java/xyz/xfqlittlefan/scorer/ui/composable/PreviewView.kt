package xyz.xfqlittlefan.scorer.ui.composable

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * CameraX PreviewView 的 Composable 函数。
 *
 * @param modifier Modifier
 * @param previewBuilder [Preview.Builder] 的自定义配置。
 * @param cameraSelectorBuilder [CameraSelector.Builder] 的自定义配置。
 *
 * @see androidx.camera.view.PreviewView
 */
@Composable
fun PreviewView(
    modifier: Modifier = Modifier,
    previewBuilder: Preview.Builder.() -> Unit = {},
    cameraSelectorBuilder: CameraSelector.Builder.() -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(localContext) }

    AndroidView(factory = { context ->
        val view = androidx.camera.view.PreviewView(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().apply(previewBuilder).build().apply {
                setSurfaceProvider(view.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder().apply(cameraSelectorBuilder).build()

            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview
            )
        }, ContextCompat.getMainExecutor(context))
        view
    }, modifier = modifier)
}