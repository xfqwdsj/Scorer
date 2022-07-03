package xyz.xfqlittlefan.scorer.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun generateQR(text: String): Bitmap {
    val hints = HashMap<EncodeHintType, String>().apply {
        put(EncodeHintType.CHARACTER_SET, "utf-8")
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H.name)
        put(EncodeHintType.MARGIN, "2")
    }
    val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512, hints)
    val pixels = IntArray(512 * 512)
    for (y in 0 until 512) {
        for (x in 0 until 512) {
            if (bitMatrix.get(x, y)) {
                pixels[y * 512 + x] = Color.BLACK
            } else {
                pixels[y * 512 + x] = Color.WHITE
            }
        }
    }
    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, 512, 0, 0, 512, 512)
    return bitmap
}

class QRAnalyzer(private val onResult: (qr: com.google.zxing.Result) -> Unit): ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()

    override fun analyze(image: ImageProxy) {

    }
}