@file:Suppress("UsePropertyAccessSyntax")
package com.example.docmanager.ui.dashboard

import android.net.Uri
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docmanager.repository.DocumentRepository
import com.google.api.services.drive.model.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.docmanager.util.PreferencesManager
import com.example.docmanager.util.NetworkMonitor

enum class SortOrder { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC }

@OptIn(FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val preferencesManager: PreferencesManager,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _documents = MutableStateFlow<List<File>>(emptyList())
    val documents: StateFlow<List<File>> = _documents.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _activeFilters = MutableStateFlow<Set<String>>(emptySet())
    val activeFilters: StateFlow<Set<String>> = _activeFilters.asStateFlow()

    val filteredDocuments: StateFlow<List<File>> = combine(_documents, _sortOrder, _activeFilters) { docs, sort, filters ->
        var result = docs
        if (filters.isNotEmpty()) {
            result = result.filter { file ->
                val mime = file.mimeType?.lowercase() ?: ""
                var matches = false
                if (filters.contains("PDF") && mime.contains("pdf")) matches = true
                if (filters.contains("IMG") && (mime.contains("image") || mime.contains("jpeg") || mime.contains("png") || mime.contains("webp"))) matches = true
                if (filters.contains("XLS") && (mime.contains("excel") || mime.contains("spreadsheet") || mime.contains("csv"))) matches = true
                if (filters.contains("DOC") && (mime.contains("word") || mime.contains("document") || mime.contains("text/plain") || mime.contains("txt") || mime.contains("rtf"))) matches = true
                if (filters.contains("PPT") && (mime.contains("powerpoint") || mime.contains("presentation"))) matches = true
                matches
            }
        }
        
        result = when (sort) {
            SortOrder.DATE_DESC -> result.sortedByDescending { it.createdTime?.value ?: 0 }
            SortOrder.DATE_ASC -> result.sortedBy { it.createdTime?.value ?: 0 }
            SortOrder.NAME_ASC -> result.sortedBy { it.name?.lowercase() ?: "" }
            SortOrder.NAME_DESC -> result.sortedByDescending { it.name?.lowercase() ?: "" }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<File>>(emptyList())
    val searchResults: StateFlow<List<File>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    var hasShownWelcome = false

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _isSelectionModeForced = MutableStateFlow(false)
    val isSelectionModeForced: StateFlow<Boolean> = _isSelectionModeForced.asStateFlow()

    val inSelectionMode: StateFlow<Boolean> = combine(_selectedFiles, _isSelectionModeForced) { selected, forced ->
        selected.isNotEmpty() || forced
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _searchDateFilter = MutableStateFlow("all") // "all", "today", "week", "month"
    val searchDateFilter: StateFlow<String> = _searchDateFilter.asStateFlow()

    private val _searchSizeFilter = MutableStateFlow("all") // "all", "small", "medium", "large"
    val searchSizeFilter: StateFlow<String> = _searchSizeFilter.asStateFlow()

    val filteredSearchResults: StateFlow<List<File>> = combine(
        _searchResults,
        _searchDateFilter,
        _searchSizeFilter
    ) { results, dateFilter, sizeFilter ->
        var filtered = results
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val sevenDaysMillis = 7 * oneDayMillis
        val thirtyDaysMillis = 30 * oneDayMillis

        if (dateFilter != "all") {
            filtered = filtered.filter { file ->
                val time = file.createdTime?.value ?: 0L
                if (time == 0L) true else {
                    when (dateFilter) {
                        "today" -> (now - time) <= oneDayMillis
                        "week" -> (now - time) <= sevenDaysMillis
                        "month" -> (now - time) <= thirtyDaysMillis
                        else -> true
                    }
                }
            }
        }

        if (sizeFilter != "all") {
            filtered = filtered.filter { file ->
                val size = file.getSize() ?: 0L
                when (sizeFilter) {
                    "small" -> size < 1 * 1024 * 1024L
                    "medium" -> size in (1 * 1024 * 1024L)..(10 * 1024 * 1024L)
                    "large" -> size > 10 * 1024 * 1024L
                    else -> true
                }
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _authIntent = MutableStateFlow<android.content.Intent?>(null)
    val authIntent: StateFlow<android.content.Intent?> = _authIntent.asStateFlow()

    private val _isDownloadingFile = MutableStateFlow<String?>(null)
    val isDownloadingFile: StateFlow<String?> = _isDownloadingFile.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(null)
    val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    private var currentUserEmail: String = ""

    // Storage quota
    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()
    private val _storageLimitBytes = MutableStateFlow(15L * 1024 * 1024 * 1024)
    val storageLimitBytes: StateFlow<Long> = _storageLimitBytes.asStateFlow()

    val recentSearches: StateFlow<List<String>> = preferencesManager.recentSearchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alwaysOpenWithDefault: StateFlow<Boolean> = preferencesManager.alwaysOpenWithDefaultFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAlwaysOpenWithDefault(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAlwaysOpenWithDefault(enabled)
        }
    }

    fun downloadAndOpenFile(
        context: android.content.Context,
        fileId: String,
        fileName: String,
        onComplete: (java.io.File) -> Unit
    ) {
        if (currentUserEmail.isEmpty()) return
        
        // 1. Look for existing copy in persistent local storage
        val localSavedFile = findLocalFile(context, currentUserEmail, fileName)
        if (localSavedFile != null && localSavedFile.exists() && localSavedFile.length() > 0) {
            onComplete(localSavedFile)
            return
        }
        
        // 2. Check if it is a local-only file (whose id starts with "local://")
        if (fileId.startsWith("local://")) {
            val path = fileId.removePrefix("local://")
            val localFile = java.io.File(path)
            if (localFile.exists() && localFile.length() > 0) {
                onComplete(localFile)
                return
            }
        }
        
        if (!isOnline.value) {
            android.widget.Toast.makeText(context, "No connection. Connect to the internet to download this file.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        
        viewModelScope.launch {
            _isDownloadingFile.value = fileName
            try {
                // Find document date to save in the right year/month folder
                val doc = _documents.value.find { it.id == fileId }
                val year: String
                val month: String
                if (doc?.createdTime != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = doc.createdTime.value
                    year = String.format("%04d", cal.get(java.util.Calendar.YEAR))
                    month = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
                } else {
                    val cal = java.util.Calendar.getInstance()
                    year = String.format("%04d", cal.get(java.util.Calendar.YEAR))
                    month = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
                }
                
                val destDir = getLocalStorageDir(context, currentUserEmail, year, month)
                val safeName = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                val localFile = java.io.File(destDir, safeName)
                
                repository.downloadFile(currentUserEmail, fileId, localFile)
                
                // Refresh list so we index the new local file
                loadRecentSuspend()
                
                onComplete(localFile)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error opening file: ${e.message}"
            } finally {
                _isDownloadingFile.value = null
            }
        }
    }

    fun saveProfilePicture(uriString: String) {
        viewModelScope.launch {
            preferencesManager.savePhotoUri(uriString)
            _profilePhotoUri.value = uriString
        }
    }

    fun deleteProfilePicture() {
        viewModelScope.launch {
            preferencesManager.savePhotoUri(null)
            _profilePhotoUri.value = null
        }
    }

    fun addRecentSearch(query: String) {
        viewModelScope.launch {
            preferencesManager.addRecentSearch(query)
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            preferencesManager.removeRecentSearch(query)
        }
    }

    init {
        _searchQuery
            .debounce(500)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    search(query)
                    addRecentSearch(query)
                } else {
                    _searchResults.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun initData(email: String) {
        if (currentUserEmail != email) {
            _documents.value = emptyList()
            _searchResults.value = emptyList()
            _selectedFiles.value = emptySet()
            _storageUsedBytes.value = 0L
            _storageLimitBytes.value = 15L * 1024 * 1024 * 1024
            _profilePhotoUri.value = null
            repository.clearSessionCache()
        }
        currentUserEmail = email
        viewModelScope.launch {
            loadRecentSuspend()
            fetchStorageQuotaSuspend()
            _profilePhotoUri.value = preferencesManager.savedPhotoUriFlow.first()
        }
    }

    fun clearAllData() {
        currentUserEmail = ""
        _documents.value = emptyList()
        _searchResults.value = emptyList()
        _selectedFiles.value = emptySet()
        _storageUsedBytes.value = 0L
        _profilePhotoUri.value = null
        viewModelScope.launch {
            repository.clearSessionCache()
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFilter(type: String) {
        _activeFilters.update { current ->
            if (current.contains(type)) current - type else current + type
        }
    }

    fun setSingleFilter(type: String?) {
        _activeFilters.value = if (type == null) emptySet() else setOf(type)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearAuthIntent() {
        _authIntent.value = null
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun toggleSelection(fileId: String) {
        _selectedFiles.update { current ->
            if (current.contains(fileId)) current - fileId else current + fileId
        }
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
        _isSelectionModeForced.value = false
    }

    fun deleteSelectedFiles() {
        if (currentUserEmail.isEmpty() || _selectedFiles.value.isEmpty()) return
        
        val filesToDelete = _selectedFiles.value
        viewModelScope.launch {
            _isLoading.value = true
            try {
                filesToDelete.forEach { fileId ->
                    repository.deleteFile(currentUserEmail, fileId)
                }
                clearSelection()
                _documents.update { list -> list.filterNot { it.id in filesToDelete } }
                _searchResults.update { list -> list.filterNot { it.id in filesToDelete } }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                _authIntent.value = e.intent
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error deleting files: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    private fun search(query: String) {
        if (currentUserEmail.isEmpty()) return
        
        viewModelScope.launch {
            _isSearching.value = true
            
            val initialResults = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val localFiles = getLocalFilesForUser(context, currentUserEmail)
                    .map { convertLocalFileToDriveFile(it) }
                    .filter { it.name?.contains(query, ignoreCase = true) == true }

                val localMatches = _documents.value.filter { file ->
                    val nameMatch = file.name?.contains(query, ignoreCase = true) == true
                    val descMatch = file.description?.contains(query, ignoreCase = true) == true
                    nameMatch || descMatch
                }
                
                mergeLocalAndDriveFiles(localFiles, localMatches)
            }
            _searchResults.value = initialResults

            try {
                if (isOnline.value) {
                    val apiResults = repository.searchDocuments(currentUserEmail, query)
                    val combined = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val localFiles = getLocalFilesForUser(context, currentUserEmail)
                            .map { convertLocalFileToDriveFile(it) }
                            .filter { it.name?.contains(query, ignoreCase = true) == true }

                        val localMatches = _documents.value.filter { file ->
                            val nameMatch = file.name?.contains(query, ignoreCase = true) == true
                            val descMatch = file.description?.contains(query, ignoreCase = true) == true
                            nameMatch || descMatch
                        }
                        mergeLocalAndDriveFiles(localFiles, localMatches + apiResults).distinctBy { it.id }
                    }
                    _searchResults.value = combined
                }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                _authIntent.value = e.intent
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun uploadFile(context: android.content.Context, uri: Uri, mimeType: String, title: String) {
        if (currentUserEmail.isNotEmpty()) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Get correct year and month for local storage placement
                    val year = java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(java.util.Date())
                    val month = java.text.SimpleDateFormat("MM", java.util.Locale.US).format(java.util.Date())
                    val localDir = getLocalStorageDir(context, currentUserEmail, year, month)
                    val localDestFile = java.io.File(localDir, title)
                    
                    val inputStream = context.contentResolver.openInputStream(uri)
                    localDestFile.outputStream().use { outputStream ->
                        inputStream?.copyTo(outputStream)
                    }
                    inputStream?.close()
                    
                    // Enqueue background upload using our saved persistent copy's Uri
                    val fileUri = android.net.Uri.fromFile(localDestFile)
                    repository.enqueueUpload(fileUri, mimeType, title, currentUserEmail)
                    
                    // Reload recent list so the offline local copy immediately appears!
                    loadRecentSuspend()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshSuspend()
        }
    }

    suspend fun refreshSuspend() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (_isRefreshing.value) return@withContext
        _isRefreshing.value = true
        val startTime = System.currentTimeMillis()
        try {
            kotlinx.coroutines.withTimeoutOrNull(10000) {
                loadRecentSuspend()
                fetchStorageQuotaSuspend()
                if (searchQuery.value.isNotBlank()) {
                    search(searchQuery.value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            val elapsedTime = System.currentTimeMillis() - startTime
            val remainingTime = 800L - elapsedTime
            if (remainingTime > 0) {
                kotlinx.coroutines.delay(remainingTime)
            }
            _isRefreshing.value = false
        }
    }

    private suspend fun loadRecentSuspend() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (currentUserEmail.isEmpty()) return@withContext
        
        // Load local storage files first for instant speed!
        val localFiles = getLocalFilesForUser(context, currentUserEmail)
            .map { convertLocalFileToDriveFile(it) }
            
        val cached = repository.getCachedRecentDocuments(currentUserEmail)
        
        // Merge local files and cached documents
        val initialMerged = mergeLocalAndDriveFiles(localFiles, cached)
        _documents.value = initialMerged
        
        if (cached.isEmpty() && localFiles.isEmpty()) {
            _isLoading.value = true
        }

        try {
            val freshDocs = repository.getRecentDocuments(currentUserEmail)
            val freshMerged = mergeLocalAndDriveFiles(localFiles, freshDocs)
            _documents.value = freshMerged
            repository.saveCachedRecentDocuments(currentUserEmail, freshDocs)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            _documents.value = localFiles
            _authIntent.value = e.intent
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            _documents.value = localFiles
        } catch (e: Exception) {
            e.printStackTrace()
            val message = e.message ?: ""
            if (message.contains("401") || message.contains("403") || message.contains("auth", ignoreCase = true)) {
                _documents.value = localFiles
            } else {
                val trace = e.stackTrace.take(3).joinToString("\n") { it.toString() }
                _errorMessage.value = "Error: ${e.javaClass.simpleName} - ${e.message}\n$trace"
                _documents.value = localFiles
            }
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun fetchStorageQuotaSuspend() {
        if (currentUserEmail.isEmpty()) return
        try {
            val quota = repository.getStorageQuota(currentUserEmail)
            _storageUsedBytes.value = quota.usageBytes
            _storageLimitBytes.value = quota.limitBytes
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            _storageUsedBytes.value = 0L
            _authIntent.value = e.intent
        } catch (e: Exception) {
            e.printStackTrace()
            val message = e.message ?: ""
            if (message.contains("401") || message.contains("403") || message.contains("auth", ignoreCase = true)) {
                _storageUsedBytes.value = 0L
            }
        }
    }

    fun downloadFileToDevice(
        context: android.content.Context,
        fileId: String,
        fileName: String,
        mimeType: String
    ) {
        if (currentUserEmail.isEmpty()) return
        if (!isOnline.value) {
            android.widget.Toast.makeText(context, "No connection. Connect to the internet to download.", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch {
            _isDownloadingFile.value = fileName
            try {
                val cacheSubDir = java.io.File(context.cacheDir, "Docman")
                if (!cacheSubDir.exists()) cacheSubDir.mkdirs()
                val safeName = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                val localFile = java.io.File(cacheSubDir, safeName)

                repository.downloadFile(currentUserEmail, fileId, localFile)

                // Copy to Downloads via MediaStore
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { outputStream ->
                        localFile.inputStream().use { it.copyTo(outputStream) }
                    }
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    android.widget.Toast.makeText(context, "Saved to Downloads", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error downloading: ${e.message}"
            } finally {
                _isDownloadingFile.value = null
            }
        }
    }

    fun setSelectionModeForced(forced: Boolean) {
        _isSelectionModeForced.value = forced
    }

    fun setSearchDateFilter(filter: String) {
        _searchDateFilter.value = filter
    }

    fun setSearchSizeFilter(filter: String) {
        _searchSizeFilter.value = filter
    }

    fun renameFile(fileId: String, newName: String) {
        if (currentUserEmail.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.renameFile(currentUserEmail, fileId, newName)
                _documents.update { list ->
                    list.map { file ->
                        if (file.id == fileId) {
                            file.clone().setName(newName)
                        } else file
                    }
                }
                _searchResults.update { list ->
                    list.map { file ->
                        if (file.id == fileId) {
                            file.clone().setName(newName)
                        } else file
                    }
                }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                _authIntent.value = e.intent
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error renaming file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSingleFile(fileId: String) {
        if (currentUserEmail.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteFile(currentUserEmail, fileId)
                _documents.update { list -> list.filterNot { it.id == fileId } }
                _searchResults.update { list -> list.filterNot { it.id == fileId } }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                _authIntent.value = e.intent
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error deleting file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getLocalStorageDir(context: android.content.Context, email: String, year: String, month: String): java.io.File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val docManagerDir = java.io.File(baseDir, "DocumentManager")
        val userFolderName = if (email.contains("rajprudvi", ignoreCase = true)) {
            "gmail"
        } else {
            email.substringBefore("@").replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "user" }
        }
        val userDir = java.io.File(docManagerDir, userFolderName)
        val yearDir = java.io.File(userDir, year)
        val monthDir = java.io.File(yearDir, month)
        if (!monthDir.exists()) {
            monthDir.mkdirs()
        }
        return monthDir
    }

    fun getLocalFilesForUser(context: android.content.Context, email: String): List<java.io.File> {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val docManagerDir = java.io.File(baseDir, "DocumentManager")
        val userFolderName = if (email.contains("rajprudvi", ignoreCase = true)) {
            "gmail"
        } else {
            email.substringBefore("@").replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "user" }
        }
        val userDir = java.io.File(docManagerDir, userFolderName)
        if (!userDir.exists()) return emptyList()
        
        val resultList = mutableListOf<java.io.File>()
        userDir.listFiles()?.forEach { yearDir ->
            if (yearDir.isDirectory) {
                yearDir.listFiles()?.forEach { monthDir ->
                    if (monthDir.isDirectory) {
                        monthDir.listFiles()?.forEach { file ->
                            if (file.isFile && file.length() > 0) {
                                resultList.add(file)
                            }
                        }
                    }
                }
            }
        }
        return resultList
    }

    fun findLocalFile(context: android.content.Context, email: String, fileName: String): java.io.File? {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val docManagerDir = java.io.File(baseDir, "DocumentManager")
        val userFolderName = if (email.contains("rajprudvi", ignoreCase = true)) {
            "gmail"
        } else {
            email.substringBefore("@").replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "user" }
        }
        val userDir = java.io.File(docManagerDir, userFolderName)
        if (!userDir.exists()) return null
        
        userDir.listFiles()?.forEach { yearDir ->
            if (yearDir.isDirectory) {
                yearDir.listFiles()?.forEach { monthDir ->
                    if (monthDir.isDirectory) {
                        val file = java.io.File(monthDir, fileName)
                        if (file.exists() && file.length() > 0) {
                            return file
                        }
                    }
                }
            }
        }
        return null
    }

    fun convertLocalFileToDriveFile(file: java.io.File): com.google.api.services.drive.model.File {
        val driveFile = com.google.api.services.drive.model.File()
        driveFile.id = "local://${file.absolutePath}"
        driveFile.name = file.name
        
        val ext = file.extension.lowercase()
        val mimeType = when (ext) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "doc", "docx" -> "application/msword"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            else -> "application/octet-stream"
        }
        driveFile.mimeType = mimeType
        driveFile.setSize(file.length())
        
        val dateTime = com.google.api.client.util.DateTime(file.lastModified())
        driveFile.createdTime = dateTime
        return driveFile
    }

    private fun mergeLocalAndDriveFiles(local: List<File>, drive: List<File>): List<File> {
        val driveNames = drive.mapNotNull { it.name?.lowercase() }.toSet()
        val uniqueLocal = local.filterNot { it.name?.lowercase() in driveNames }
        return drive + uniqueLocal
    }
}
