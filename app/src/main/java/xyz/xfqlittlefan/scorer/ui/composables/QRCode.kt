package xyz.xfqlittlefan.scorer.ui.composables

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import xyz.xfqlittlefan.scorer.util.QREncodeHints
import xyz.xfqlittlefan.scorer.util.toQR

@Composable
fun QRCode(
    text: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    qrCodeSize: Int = 512,
    blackAreaColor: Int = Color.BLACK,
    whiteAreaColor: Int = Color.WHITE,
    bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    hints: QREncodeHints.() -> Unit = {}
) {
    Image(
        bitmap = text.toQR(qrCodeSize, blackAreaColor, whiteAreaColor, bitmapConfig, hints).asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality
    )
}