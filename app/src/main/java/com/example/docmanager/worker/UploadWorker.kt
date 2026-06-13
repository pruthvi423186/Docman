package com.example.docmanager.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.docmanager.drive.DriveServiceWrapper
import com.example.docmanager.util.ImageUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.io.File

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val driveServiceWrapper: DriveServiceWrapper
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_FILE_URI = "KEY_FILE_URI"
        const val KEY_MIME_TYPE = "KEY_MIME_TYPE"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_EMAIL = "KEY_EMAIL"
    }

    override suspend fun doWork(): Result {
        val fileUriString = inputData.getString(KEY_FILE_URI) ?: return Result.failure()
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "application/octet-stream"
        var title = inputData.getString(KEY_TITLE) ?: "Document_${System.currentTimeMillis()}"
        val email = inputData.getString(KEY_EMAIL) ?: return Result.failure()

        try {
            val uri = Uri.parse(fileUriString)
            val fileToUpload: File
            
            if (mimeType.startsWith("image/")) {
                val compressedFile = ImageUtils.compressImage(context, uri)
                if (compressedFile != null) {
                    fileToUpload = compressedFile
                    if (!title.endsWith(".jpg")) title += ".jpg"
                } else {
                    fileToUpload = copyUriToTempFile(context, uri)
                }
            } else {
                fileToUpload = copyUriToTempFile(context, uri)
            }

            var extractedText = ""
            if (mimeType.contains("pdf")) {
                try {
                    val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(fileToUpload)
                    val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                    extractedText = stripper.getText(document) ?: ""
                    document.close()
                    
                    // If PDFBox found no text, it's a scanned PDF (image). Use ML Kit OCR!
                    if (extractedText.trim().length < 50) {
                        val pfd = android.os.ParcelFileDescriptor.open(fileToUpload, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = android.graphics.pdf.PdfRenderer(pfd)
                        val pageCount = Math.min(renderer.pageCount, 2) // Max 2 pages for OCR speed
                        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                        val stringBuilder = java.lang.StringBuilder()
                        
                        for (i in 0 until pageCount) {
                            val page = renderer.openPage(i)
                            val bitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                            
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            
                            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                            try {
                                val result = recognizer.process(image).await()
                                stringBuilder.append(result.text).append("\n")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            page.close()
                        }
                        renderer.close()
                        pfd.close()
                        
                        if (stringBuilder.isNotEmpty()) {
                            extractedText = stringBuilder.toString()
                        }
                    }

                    // Truncate to prevent exceeding Google Drive's description size limit
                    if (extractedText.length > 8000) {
                        extractedText = extractedText.substring(0, 8000)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (mimeType.startsWith("image/")) {
                try {
                    val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                    val bitmap = android.graphics.BitmapFactory.decodeFile(fileToUpload.absolutePath)
                    if (bitmap != null) {
                        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                        val result = recognizer.process(image).await()
                        extractedText = result.text
                        if (extractedText.length > 8000) {
                            extractedText = extractedText.substring(0, 8000)
                        }
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            driveServiceWrapper.uploadFile(email, fileToUpload, mimeType, title, extractedText)
            fileToUpload.delete()

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}")
        val outputStream = tempFile.outputStream()
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return tempFile
    }
}
