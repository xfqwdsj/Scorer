package xyz.xfqlittlefan.scorer.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.ktor.utils.io.charsets.*
import kotlin.text.Charsets

fun String.toQR(
    size: Int = 512,
    blackAreaColor: Int = Color.BLACK,
    whiteAreaColor: Int = Color.WHITE,
    bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
    hints: QREncodeHints.() -> Unit = {}
): Bitmap {
    val bitMatrix = QRCodeWriter().encode(
        this, BarcodeFormat.QR_CODE, size, size, QREncodeHints().apply(hints).toHints()
    )
    val pixels = IntArray(512 * 512)
    for (y in 0 until 512) {
        for (x in 0 until 512) {
            if (bitMatrix.get(x, y)) {
                pixels[y * 512 + x] = blackAreaColor
            } else {
                pixels[y * 512 + x] = whiteAreaColor
            }
        }
    }
    return Bitmap.createBitmap(size, size, bitmapConfig).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

class QREncodeHints {
    var errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.H
    var characterSet: Charset = Charsets.UTF_8
    var margin: Int = 4
    var version: QRVersion = QRVersion.VERSION_RECOMMENDED
    var maskPattern: QRMaskPattern = QRMaskPattern.MASK_PATTERN_AUTO
    var compact: Boolean = false

    internal fun toHints() = HashMap<EncodeHintType, Any>().apply {
        put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel)
        put(EncodeHintType.CHARACTER_SET, characterSet.name)
        put(EncodeHintType.MARGIN, margin)
        version.ordinal.let { if (it != 0) put(EncodeHintType.QR_VERSION, it) }
        put(EncodeHintType.QR_MASK_PATTERN, maskPattern.ordinal - 1)
        put(EncodeHintType.QR_COMPACT, compact)
    }
}

enum class QRVersion {
    VERSION_RECOMMENDED, VERSION_1, VERSION_2, VERSION_3, VERSION_4, VERSION_5, VERSION_6, VERSION_7, VERSION_8, VERSION_9, VERSION_10, VERSION_11, VERSION_12, VERSION_13, VERSION_14, VERSION_15, VERSION_16, VERSION_17, VERSION_18, VERSION_19, VERSION_20, VERSION_21, VERSION_22, VERSION_23, VERSION_24, VERSION_25, VERSION_26, VERSION_27, VERSION_28, VERSION_29, VERSION_30, VERSION_31, VERSION_32, VERSION_33, VERSION_34, VERSION_35, VERSION_36, VERSION_37, VERSION_38, VERSION_39, VERSION_40
}

enum class QRMaskPattern {
    MASK_PATTERN_AUTO, MASK_PATTERN_0, MASK_PATTERN_1, MASK_PATTERN_2, MASK_PATTERN_3, MASK_PATTERN_4, MASK_PATTERN_5, MASK_PATTERN_6, MASK_PATTERN_7
}