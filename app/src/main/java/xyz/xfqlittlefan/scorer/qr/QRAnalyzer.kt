package xyz.xfqlittlefan.scorer.qr

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

class QRAnalyzer(
    private val onSuccessListener: OnSuccessListener<List<Barcode>>? = null,
    private val onFailureListener: OnFailureListener? = null
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            scanner.process(inputImage).apply {
                if (onSuccessListener != null) {
                    addOnSuccessListener(onSuccessListener)
                }
                if (onFailureListener != null) {
                    addOnFailureListener(onFailureListener)
                }
            }.addOnCompleteListener {
                image.close()
            }
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val array = ByteArray(remaining())
        get(array)
        return array
    }
}