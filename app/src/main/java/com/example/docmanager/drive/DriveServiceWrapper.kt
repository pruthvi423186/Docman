package com.example.docmanager.drive

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File as JavaFile

@Singleton
class DriveServiceWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val folderName = "DocumentManager"
    private var folderId: String? = null

    fun clearSessionCache() {
        folderId = null
    }

    suspend fun getDriveService(email: String): Drive = withContext(Dispatchers.IO) {
        val account = Account(email, "com.google")
        val scope = "oauth2:${DriveScopes.DRIVE_FILE}"

        // Fetch token natively using GoogleAuthUtil to trigger UserRecoverableAuthException if consent is needed
        val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account, scope)

        return@withContext Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            com.google.api.client.http.HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $token"
                request.unsuccessfulResponseHandler = com.google.api.client.http.HttpUnsuccessfulResponseHandler { _, response, _ ->
                    if (response.statusCode == 401) {
                        try {
                            com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, token)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    false
                }
            }
        )
            .setApplicationName("DocumentManager")
            .build()
    }

    suspend fun getOrCreateAppFolder(email: String): String = withContext(Dispatchers.IO) {
        val service = getDriveService(email)
        
        folderId?.let { return@withContext it }

        val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false"
        val result = service.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val files = result.files
        if (!files.isNullOrEmpty()) {
            folderId = files[0].id
            return@withContext folderId!!
        }

        val folderMetadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }

        val folder = service.files().create(folderMetadata)
            .setFields("id")
            .execute()

        folderId = folder.id
        return@withContext folder.id
    }

    suspend fun uploadFile(email: String, localFile: JavaFile, mimeType: String, title: String, extractedText: String = ""): File = withContext(Dispatchers.IO) {
        val service = getDriveService(email)
        val targetFolderId = getOrCreateAppFolder(email)

        val fileMetadata = File().apply {
            name = title
            parents = listOf(targetFolderId)
            if (extractedText.isNotBlank()) {
                description = extractedText
            }
        }

        val mediaContent = FileContent(mimeType, localFile)
        
        return@withContext service.files().create(fileMetadata, mediaContent)
            .setFields("id, name, createdTime, webViewLink, thumbnailLink, size")
            .execute()
    }

    suspend fun searchFiles(email: String, searchQuery: String): List<File> = withContext(Dispatchers.IO) {
        val service = getDriveService(email)
        val targetFolderId = getOrCreateAppFolder(email)

        // Split query into individual words to avoid exact-phrase matching issues
        val terms = searchQuery.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (terms.isEmpty()) return@withContext emptyList()

        // name contains allows substring matches
        val nameConditions = terms.joinToString(" and ") { "name contains '${it.replace("'", "\\'")}'" }
        // fullText contains allows deep content search, but only on whole words
        val textConditions = terms.joinToString(" and ") { "fullText contains '${it.replace("'", "\\'")}'" }
        // description contains searches our instantly injected local text extraction!
        val descConditions = terms.joinToString(" and ") { "description contains '${it.replace("'", "\\'")}'" }

        // Combine strategies: search name OR text OR description
        val query = "'$targetFolderId' in parents and (($nameConditions) or ($textConditions) or ($descConditions)) and trashed=false"
        
        val result = service.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name, createdTime, webViewLink, thumbnailLink, mimeType, description, size)")
            .setOrderBy("createdTime desc")
            .execute()

        return@withContext result.files ?: emptyList()
    }

    suspend fun listRecentFiles(email: String): List<File> = withContext(Dispatchers.IO) {
        val service = getDriveService(email)
        val targetFolderId = getOrCreateAppFolder(email)

        val query = "'$targetFolderId' in parents and trashed=false"
        
        val result = service.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name, createdTime, webViewLink, thumbnailLink, mimeType, description, size)")
            .setOrderBy("createdTime desc")
            .setPageSize(100)
            .execute()

        return@withContext result.files ?: emptyList()
    }

    suspend fun deleteFile(email: String, fileId: String) = withContext(Dispatchers.IO) {
        val service = getDriveService(email)
        service.files().delete(fileId).execute()
    }

    suspend fun downloadFile(email: String, fileId: String, outputFile: JavaFile) = withContext(Dispatchers.IO) {
        val service = getDriveService(email)
        outputFile.parentFile?.mkdirs()
        outputFile.outputStream().use { output ->
            service.files().get(fileId).executeMediaAndDownloadTo(output)
        }
    }

    suspend fun renameFile(email: String, fileId: String, newName: String) = withContext(Dispatchers.IO) {
        val service = getDriveService(email)
        val fileMetadata = File().apply {
            name = newName
        }
        service.files().update(fileId, fileMetadata).execute()
    }

    data class StorageQuota(val usageBytes: Long, val limitBytes: Long)

    suspend fun getStorageQuota(email: String): StorageQuota = withContext(Dispatchers.IO) {
        val service = getDriveService(email)
        val about = service.about().get()
            .setFields("storageQuota")
            .execute()
        val quota = about.storageQuota
        StorageQuota(
            usageBytes = quota?.usage ?: 0L,
            limitBytes = quota?.limit ?: (15L * 1024 * 1024 * 1024) // Default 15GB
        )
    }
}
