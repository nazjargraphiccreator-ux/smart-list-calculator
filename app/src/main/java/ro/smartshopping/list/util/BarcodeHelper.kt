package ro.smartshopping.list.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import ro.smartshopping.list.domain.model.BarcodeType
import java.util.EnumMap

object BarcodeHelper {

    fun toZxingFormat(type: BarcodeType): BarcodeFormat {
        return when (type) {
            BarcodeType.CODE_128 -> BarcodeFormat.CODE_128
            BarcodeType.EAN_13 -> BarcodeFormat.EAN_13
            BarcodeType.QR_CODE -> BarcodeFormat.QR_CODE
            BarcodeType.EAN_8 -> BarcodeFormat.EAN_8
            BarcodeType.CODE_39 -> BarcodeFormat.CODE_39
        }
    }

    fun fromZxingFormat(format: BarcodeFormat): BarcodeType {
        return when (format) {
            BarcodeFormat.QR_CODE -> BarcodeType.QR_CODE
            BarcodeFormat.EAN_13 -> BarcodeType.EAN_13
            BarcodeFormat.EAN_8 -> BarcodeType.EAN_8
            BarcodeFormat.CODE_39 -> BarcodeType.CODE_39
            else -> BarcodeType.CODE_128
        }
    }

    /**
     * Generează o matrice de biți (BitMatrix) pură ZXing.
     * Poate fi testată direct pe JVM fără dependențe de framework Android.
     */
    fun generateBitMatrix(
        content: String,
        barcodeType: BarcodeType,
        width: Int = 800,
        height: Int = 320
    ): com.google.zxing.common.BitMatrix? {
        if (content.isBlank()) return null
        return try {
            val format = toZxingFormat(barcodeType)
            val finalHeight = if (barcodeType == BarcodeType.QR_CODE) width else height
            val writer = MultiFormatWriter()
            writer.encode(content.trim(), format, width, finalHeight)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generează o imagine Bitmap cu codul de bare sau QR code la rezoluție înaltă.
     */
    fun generateBarcodeBitmap(
        content: String,
        barcodeType: BarcodeType,
        width: Int = 800,
        height: Int = 320
    ): Bitmap? {
        val bitMatrix = generateBitMatrix(content, barcodeType, width, height) ?: return null
        return try {
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)

            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Scanează și decodează un cod de bare dintr-un Bitmap.
     */
    fun scanBarcodeFromBitmap(bitmap: Bitmap): Pair<String, BarcodeType>? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader().apply {
                val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
                hints[DecodeHintType.TRY_HARDER] = true
                hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.CODE_39
                )
                setHints(hints)
            }

            val result = reader.decodeWithState(binaryBitmap)
            val detectedType = fromZxingFormat(result.barcodeFormat)
            Pair(result.text, detectedType)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Scanează un frame YUV direct din camera Android (CameraX).
     */
    fun scanYuvFrame(
        yData: ByteArray,
        width: Int,
        height: Int
    ): Pair<String, BarcodeType>? {
        return try {
            val source = PlanarYUVLuminanceSource(
                yData, width, height, 0, 0, width, height, false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader().apply {
                val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
                hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.CODE_39
                )
                setHints(hints)
            }
            val result = reader.decodeWithState(binaryBitmap)
            Pair(result.text, fromZxingFormat(result.barcodeFormat))
        } catch (e: Exception) {
            null
        }
    }
}
