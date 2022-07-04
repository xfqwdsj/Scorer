package xyz.xfqlittlefan.scorer.qr

import android.graphics.ImageFormat
import android.os.Build
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer

class QRAnalyzer(private val onResult: (qr: com.google.zxing.Result) -> Unit) :
    ImageAnalysis.Analyzer {
    private val yuvFormat = mutableListOf(ImageFormat.YUV_420_888)
    private val reader = QRCodeReader()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            yuvFormat.addAll(listOf(ImageFormat.YUV_422_888, ImageFormat.YUV_444_888))
        }
    }

    override fun analyze(image: ImageProxy) {
        if (image.format !in yuvFormat) {
            image.close()
            return
        }

        val data = image.planes[0].buffer.toByteArray()
        val source = PlanarYUVLuminanceSource(
            data, image.width, image.height, 0, 0, image.width, image.height, false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            onResult(reader.decode(binaryBitmap))
        } catch (e: NotFoundException) {
            e.printStackTrace()
        }
        image.close()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val array = ByteArray(remaining())
        get(array)
        return array
    }
}