package com.example.quickqr.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUtils @Inject constructor(private val context: Context) {

    suspend fun createQRCodeBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512,
        backgroundColor: Int = Color.WHITE,
        codeColor: Int = Color.BLACK,
        margin: Int = 1
    ): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = margin
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )

            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) codeColor else backgroundColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: WriterException) {
            Log.e("ImageUtils", "Error generating QR code", e)
            null
        }
    }

    suspend fun saveBitmapToGallery(
        bitmap: Bitmap,
        displayName: String = "QR_${System.currentTimeMillis()}",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Uri? = withContext(Dispatchers.IO) {
        val fileName = "${displayName}.${format.name.lowercase()}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/${format.name.lowercase()}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        return@withContext try {
            val resolver = context.contentResolver
            var uri: Uri? = null
            var outputStream: OutputStream? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentUri = MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
                uri = resolver.insert(contentUri, contentValues)
                outputStream = resolver.openOutputStream(uri ?: throw Exception("Failed to create new MediaStore record."))
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val imageFile = File(imagesDir, fileName)
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                outputStream = FileOutputStream(imageFile)
            }

            outputStream?.use { stream ->
                if (!bitmap.compress(format, quality, stream)) {
                    throw Exception("Failed to save bitmap")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                uri?.let { resolver.update(it, contentValues, null, null) }
            }

            uri
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error saving image to gallery", e)
            null
        }
    }

    fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = pixels.toFloat()

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = Color.WHITE
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    fun getResizedBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    fun bitmapToBase64(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error converting base64 to bitmap", e)
            null
        }
    }

    fun createUniqueImageFile(context: Context, prefix: String = "IMG_", extension: String = ".jpg"): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "${prefix}${timeStamp}_",
            extension,
            storageDir
        )
    }

    fun getImageContentUri(context: Context, imageFile: File): Uri? {
        val filePath = imageFile.absolutePath
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.DATA} = ?",
            arrayOf(filePath),
            null
        )

        return if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            cursor.close()
            Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "$id")
        } else {
            if (imageFile.exists()) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, filePath)
                }
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
            } else {
                null
            }
        }
    }
}
