package nz.ac.massey.circle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageUtils {

    // Runs on a background thread so picking a photo doesn't freeze the UI
    suspend fun uriToBase64(context: Context, uri: Uri, maxSize: Int = 800): String =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver

            // Pass 1: read only the image size, not the pixels
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            // Pass 2: decode at a reduced size to save memory
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext ""

            // Scale exactly to maxSize on the longest side
            val scale = maxSize.toFloat() / maxOf(decoded.width, decoded.height)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt(),
                    (decoded.height * scale).toInt(),
                    true
                )
            } else decoded

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)

            // Free the bitmap memory now rather than waiting for the GC
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()

            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

    // Encodes a photo from the camera the same way as a picked photo
    suspend fun bitmapToBase64(bitmap: Bitmap): String =
        withContext(Dispatchers.Default) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

    fun base64ToBitmap(data: String): ImageBitmap? {
        if (data.isEmpty()) return null
        val bytes = Base64.decode(data, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
}