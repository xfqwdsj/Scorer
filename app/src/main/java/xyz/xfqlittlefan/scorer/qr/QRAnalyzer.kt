package xyz.xfqlittlefan.scorer.qr

import android.graphics.ImageFormat
import android.os.Build
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class QRAnalyzer(
    private val onResult: (qr: Result) -> Unit,
    private val onFormatNotSupported: (format: Int) -> Unit = {},
    private val onNotFoundException: (e: NotFoundException) -> Unit = {},
    private val onFormatException: (e: FormatException) -> Unit = {},
    private val onChecksumException: (e: ChecksumException) -> Unit = {},
    private val onOtherException: (e: Throwable) -> Unit = {}
) : ImageAnalysis.Analyzer {
    private val yuvFormat = mutableListOf(ImageFormat.YUV_420_888)
    private val reader = MultiFormatReader().apply {
        val map = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE)
        )
        setHints(map)
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            yuvFormat.addAll(listOf(ImageFormat.YUV_422_888, ImageFormat.YUV_444_888))
        }
    }

    override fun analyze(image: ImageProxy) {
        if (image.format !in yuvFormat) {
            image.close()
            onFormatNotSupported(image.format)
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
            onNotFoundException(e)
        } catch (e: FormatException) {
            onFormatException(e)
        } catch (e: ChecksumException) {
            onChecksumException(e)
        } catch (e: Throwable) {
            onOtherException(e)
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