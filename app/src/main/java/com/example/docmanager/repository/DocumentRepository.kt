package com.example.docmanager.repository

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.docmanager.drive.DriveServiceWrapper
import com.example.docmanager.worker.UploadWorker
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val driveServiceWrapper: DriveServiceWrapper
) {
    suspend fun getRecentDocuments(email: String): List<File> {
        return driveServiceWrapper.listRecentFiles(email)
    }

    suspend fun searchDocuments(email: String, query: String): List<File> {
        return driveServiceWrapper.searchFiles(email, query)
    }

    suspend fun deleteFile(email: String, fileId: String) {
        driveServiceWrapper.deleteFile(email, fileId)
    }

    suspend fun downloadFile(email: String, fileId: String, outputFile: java.io.File) {
        driveServiceWrapper.downloadFile(email, fileId, outputFile)
    }

    suspend fun renameFile(email: String, fileId: String, newName: String) {
        driveServiceWrapper.renameFile(email, fileId, newName)
    }

    suspend fun createFolder(email: String, parentId: String, name: String): File {
        return driveServiceWrapper.createFolder(email, parentId, name)
    }

    suspend fun moveFile(email: String, fileId: String, oldParentId: String, newParentId: String): File {
        return driveServiceWrapper.moveFile(email, fileId, oldParentId, newParentId)
    }

    suspend fun copyFile(email: String, fileId: String, newParentId: String): File {
        return driveServiceWrapper.copyFile(email, fileId, newParentId)
    }

    suspend fun listFilesInFolder(email: String, folderId: String): List<File> {
        return driveServiceWrapper.listFilesInFolder(email, folderId)
    }

    suspend fun getOrCreateAppFolder(email: String): String {
        return driveServiceWrapper.getOrCreateAppFolder(email)
    }

    fun enqueueUpload(uri: Uri, mimeType: String, title: String, email: String): java.util.UUID {
        val inputData = Data.Builder()
            .putString(UploadWorker.KEY_FILE_URI, uri.toString())
            .putString(UploadWorker.KEY_MIME_TYPE, mimeType)
            .putString(UploadWorker.KEY_TITLE, title)
            .putString(UploadWorker.KEY_EMAIL, email)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(UPLOAD_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueue(uploadWorkRequest)
        return uploadWorkRequest.id
    }

    suspend fun getStorageQuota(email: String): DriveServiceWrapper.StorageQuota {
        return driveServiceWrapper.getStorageQuota(email)
    }

    fun getCachedRecentDocuments(email: String): List<File> {
        val hash = email.hashCode()
        val cacheFile = java.io.File(context.cacheDir, "recent_docs_$hash.json")
        if (!cacheFile.exists() || cacheFile.length() == 0L) return emptyList()
        return try {
            val content = cacheFile.readText()
            val parser = com.google.api.client.json.gson.GsonFactory.getDefaultInstance().createJsonParser(content)
            val wrapper = parser.parseAndClose(FileListWrapper::class.java)
            wrapper.files ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveCachedRecentDocuments(email: String, files: List<File>) {
        val hash = email.hashCode()
        val cacheFile = java.io.File(context.cacheDir, "recent_docs_$hash.json")
        try {
            val wrapper = FileListWrapper().apply { this.files = files }
            val content = com.google.api.client.json.gson.GsonFactory.getDefaultInstance().toString(wrapper)
            cacheFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearSessionCache() {
        driveServiceWrapper.clearSessionCache()
        try {
            val cacheSubDir = java.io.File(context.cacheDir, "Docman")
            if (cacheSubDir.exists()) {
                cacheSubDir.deleteRecursively()
            }
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("recent_docs_") && file.name.endsWith(".json")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val UPLOAD_WORK_TAG = "UPLOAD_WORK"
    }
}

class FileListWrapper : com.google.api.client.json.GenericJson() {
    @com.google.api.client.util.Key
    var files: List<File>? = null
}
