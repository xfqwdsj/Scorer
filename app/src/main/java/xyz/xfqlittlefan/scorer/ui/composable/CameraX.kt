package xyz.xfqlittlefan.scorer.ui.composable

import android.util.Rational
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

/**
 * CameraX Composable 函数。目前支持的用例：
 * - 预览
 * - 图片拍摄
 * - 图片分析
 *
 * @param modifier Modifier
 * @param cameraSelectorBuilder [CameraSelector.Builder] 的自定义配置。
 * @param previewBuilder [Preview.Builder] 的自定义配置。
 * @param attributes [PreviewView] 的属性。请参阅 [PreviewView]。
 * @param imageCaptureBuilder [ImageCapture.Builder] 的自定义配置。
 * @param aspectRatio 设置输出图像的目标裁剪纵横比。请参阅 [ImageCapture.setCropAspectRatio]。
 * @param onTakingPictureAvailable 当 [ImageCapture.takePicture] 可用时调用。
 * @param onSavingPictureAvailable 当 [ImageCapture.takePicture] 可用时调用。
 * @param imageAnalysisBuilder [ImageAnalysis.Builder] 的自定义配置。
 * @param imageAnalyzer 请参阅 [ImageAnalysis.Analyzer]。
 */
@Composable
fun CameraX(
    modifier: Modifier = Modifier,
    cameraSelectorBuilder: CameraSelector.Builder.() -> Unit = {},
    previewBuilder: Preview.Builder.() -> Unit = {},
    attributes: PreviewView.() -> Unit = {},
    imageCaptureBuilder: (ImageCapture.Builder.() -> Unit)? = null,
    aspectRatio: Rational? = null,
    onTakingPictureAvailable: (((executor: Executor, callback: ImageCapture.OnImageCapturedCallback) -> Unit) -> Unit)? = null,
    onSavingPictureAvailable: (((outputFileOptions: ImageCapture.OutputFileOptions, executor: Executor, callback: ImageCapture.OnImageSavedCallback) -> Unit) -> Unit)? = null,
    imageAnalysisBuilder: (ImageAnalysis.Builder.() -> Unit)? = null,
    imageAnalyzer: ImageAnalysis.Analyzer? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(localContext) }

    AndroidView(factory = { context ->
        val view = PreviewView(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.Builder().apply(cameraSelectorBuilder).build()

            val useCases = mutableListOf<UseCase>()

            useCases += Preview.Builder().apply(previewBuilder).build().apply {
                setSurfaceProvider(view.surfaceProvider)
            }
            if (imageCaptureBuilder != null) {
                val imageCapture = ImageCapture.Builder().apply(imageCaptureBuilder).build().apply {
                    if (aspectRatio != null) {
                        setCropAspectRatio(aspectRatio)
                    }
                }
                useCases += imageCapture

                if (onTakingPictureAvailable != null) {
                    onTakingPictureAvailable(imageCapture::takePicture)
                }
                if (onSavingPictureAvailable != null) {
                    onSavingPictureAvailable(imageCapture::takePicture)
                }
            }
            if (imageAnalyzer != null) {
                useCases += ImageAnalysis.Builder().apply {
                    if (imageAnalysisBuilder != null) {
                        apply(imageAnalysisBuilder)
                    }
                }.build().apply {
                    setAnalyzer(executor, imageAnalyzer)
                }
            }

            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, *useCases.toTypedArray()
            )
        }, executor)
        view
    }, modifier = modifier, update = { it.apply(attributes) })
}