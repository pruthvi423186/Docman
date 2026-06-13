package com.example.docmanager.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    suspend fun compressImage(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return@withContext null

            var quality = 80
            var outputBytes: ByteArray
            val outputStream = ByteArrayOutputStream()

            do {
                outputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputBytes = outputStream.toByteArray()
                quality -= 10
            } while (outputBytes.size > 1_000_000 && quality > 10)

            val tempFile = File(context.cacheDir, "compressed_upload_${System.currentTimeMillis()}.jpg")
            val fileOutputStream = FileOutputStream(tempFile)
            fileOutputStream.write(outputBytes)
            fileOutputStream.flush()
            fileOutputStream.close()

            return@withContext tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
