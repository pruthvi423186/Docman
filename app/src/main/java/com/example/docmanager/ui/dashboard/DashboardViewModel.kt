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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.time.Duration.Companion.milliseconds
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
    @param:ApplicationContext private val context: Context
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<File>>(emptyList())

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _copiedFiles = MutableStateFlow<Set<String>>(emptySet())
    val copiedFiles: StateFlow<Set<String>> = _copiedFiles.asStateFlow()

    private val _isSelectionModeForced = MutableStateFlow(false)

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

    data class FolderNode(val id: String, val name: String)
    private val _folderStack = MutableStateFlow<List<FolderNode>>(emptyList())
    val folderStack: StateFlow<List<FolderNode>> = _folderStack.asStateFlow()

    private val _currentFolderFiles = MutableStateFlow<List<File>>(emptyList())
    val currentFolderFiles: StateFlow<List<File>> = _currentFolderFiles.asStateFlow()

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
                    ?: _currentFolderFiles.value.find { it.id == fileId }
                    ?: _searchResults.value.find { it.id == fileId }
                val year: String
                val month: String
                if (doc?.createdTime != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = doc.createdTime.value
                    year = String.format(java.util.Locale.US, "%04d", cal.get(java.util.Calendar.YEAR))
                    month = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MONTH) + 1)
                } else {
                    val cal = java.util.Calendar.getInstance()
                    year = String.format(java.util.Locale.US, "%04d", cal.get(java.util.Calendar.YEAR))
                    month = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MONTH) + 1)
                }
                
                val destDir = getLocalStorageDir(context, currentUserEmail, year, month)
                val ext = getExtensionForMimeType(doc?.mimeType)
                val nameWithExt = if (ext.isNotEmpty() && !fileName.lowercase().endsWith(".$ext")) {
                    "$fileName.$ext"
                } else {
                    fileName
                }
                val safeName = nameWithExt.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
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
            _folderStack.value = emptyList()
            _currentFolderFiles.value = emptyList()
            repository.clearSessionCache()
        }
        currentUserEmail = email
        viewModelScope.launch {
            loadRecentSuspend()
            fetchStorageQuotaSuspend()
            _profilePhotoUri.value = preferencesManager.savedPhotoUriFlow.first()
            try {
                repository.getOrCreateAppFolder(currentUserEmail)
                loadFolderContents("root_app_folder", "Folders")
            } catch (e: Exception) { e.printStackTrace() }
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
        
        val filesToDelete = _selectedFiles.value.toList()
        viewModelScope.launch {
            // Optimistic UI updates
            clearSelection()
            _documents.update { list -> list.filterNot { it.id in filesToDelete } }
            _searchResults.update { list -> list.filterNot { it.id in filesToDelete } }
            
            _isLoading.value = true
            try {
                // Delete in parallel
                kotlinx.coroutines.coroutineScope {
                    filesToDelete.map { fileId ->
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            deleteSingleFileInternal(fileId)
                        }
                    }.awaitAll()
                }
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
                    val ext = getExtensionForMimeType(mimeType)
                    val safeTitle = if (ext.isNotEmpty() && !title.lowercase().endsWith(".$ext")) {
                        "$title.$ext"
                    } else {
                        title
                    }
                    val localDestFile = java.io.File(localDir, safeTitle)
                    
                    val inputStream = context.contentResolver.openInputStream(uri)
                    localDestFile.outputStream().use { outputStream ->
                        inputStream?.copyTo(outputStream)
                    }
                    inputStream?.close()
                    
                    // Enqueue background upload using our saved persistent copy's Uri
                    val fileUri = android.net.Uri.fromFile(localDestFile)
                    repository.enqueueUpload(fileUri, mimeType, safeTitle, currentUserEmail)
                    
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
            kotlinx.coroutines.withTimeoutOrNull(10000.milliseconds) {
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
                kotlinx.coroutines.delay(remainingTime.milliseconds)
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

    fun loadFolderContents(folderId: String, folderName: String) {
        if (currentUserEmail.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            // Load local/optimistic files synchronously to avoid screen lag or full screen awaiting loader
            if (folderId == "root_app_folder") {
                _currentFolderFiles.value = getLocalCustomFolders(context, currentUserEmail)
            } else {
                val folderDir = getLocalCustomFolderDir(context, currentUserEmail, folderName)
                val localFiles = folderDir.listFiles()?.filter { it.isFile && it.length() > 0 }
                    ?.map { convertLocalFileToDriveFile(it) } ?: emptyList()
                _currentFolderFiles.value = localFiles
            }

            _isLoading.value = true
            try {
                // Navigate into folder
                val currentStack = _folderStack.value.toMutableList()
                if (folderId == "root_app_folder") { // Dummy ID for root
                    _folderStack.value = emptyList()
                    try {
                        val appFolderId = repository.getOrCreateAppFolder(currentUserEmail)
                        val driveFiles = repository.listFilesInFolder(currentUserEmail, appFolderId)
                        val localFolders = getLocalCustomFolders(context, currentUserEmail)
                        val combined = mergeFolders(localFolders, driveFiles)
                        _currentFolderFiles.value = combined
                    } catch (e: Exception) {
                        // Keep optimistic folders list
                    }
                } else {
                    if (!currentStack.any { it.id == folderId }) {
                        currentStack.add(FolderNode(folderId, folderName))
                        _folderStack.value = currentStack
                    } else {
                        // Navigating back to an existing folder in stack
                        val index = currentStack.indexOfFirst { it.id == folderId }
                        if (index != -1) {
                            _folderStack.value = currentStack.subList(0, index + 1)
                        }
                    }
                    try {
                        _currentFolderFiles.value = repository.listFilesInFolder(currentUserEmail, folderId)
                    } catch (e: Exception) {
                        // Keep optimistic files list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error loading folder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateUpFolder() {
        clearSelection()
        if (_folderStack.value.isNotEmpty()) {
            val currentStack = _folderStack.value.toMutableList()
            currentStack.removeAt(currentStack.lastIndex)
            _folderStack.value = currentStack
            
            if (currentStack.isEmpty()) {
                loadFolderContents("root_app_folder", "Folders")
            } else {
                val parent = currentStack.last()
                loadFolderContents(parent.id, parent.name)
            }
        }
    }

    fun createCustomFolder(name: String) {
        if (currentUserEmail.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ensure local folder is created
                getLocalCustomFolderDir(context, currentUserEmail, name)
                
                if (isOnline.value) {
                    val parentId = if (_folderStack.value.isEmpty()) {
                        repository.getOrCreateAppFolder(currentUserEmail)
                    } else {
                        _folderStack.value.last().id
                    }
                    repository.createFolder(currentUserEmail, parentId, name)
                }
                
                // Reload current folder contents
                if (_folderStack.value.isEmpty()) {
                    loadFolderContents("root_app_folder", "Folders")
                } else {
                    loadFolderContents(_folderStack.value.last().id, _folderStack.value.last().name)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error creating folder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun moveSelectedFilesToFolder(targetFolderId: String) {
        if (currentUserEmail.isEmpty() || _selectedFiles.value.isEmpty()) return
        val filesToMove = _selectedFiles.value.toList()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sourceFolderId = if (_folderStack.value.isEmpty()) {
                    repository.getOrCreateAppFolder(currentUserEmail)
                } else {
                    _folderStack.value.last().id
                }
                
                val targetFolderName = _currentFolderFiles.value.find { it.id == targetFolderId }?.name
                    ?: if (targetFolderId.startsWith("local_folder://")) targetFolderId.removePrefix("local_folder://") else "Folders"
                
                kotlinx.coroutines.coroutineScope {
                    filesToMove.map { fileId ->
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            if (!fileId.startsWith("local://")) {
                                try {
                                    if (isOnline.value) {
                                        repository.moveFile(currentUserEmail, fileId, sourceFolderId, targetFolderId)
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            
                            val localFile = if (fileId.startsWith("local://")) {
                                java.io.File(fileId.removePrefix("local://"))
                            } else {
                                val docName = _documents.value.find { it.id == fileId }?.name
                                if (docName != null) findLocalFile(context, currentUserEmail, docName) else null
                            }
                            
                            if (localFile != null && localFile.exists()) {
                                val destDir = getLocalCustomFolderDir(context, currentUserEmail, targetFolderName)
                                val destFile = java.io.File(destDir, localFile.name)
                                if (localFile.absolutePath != destFile.absolutePath) {
                                    localFile.renameTo(destFile)
                                }
                            }
                        }
                    }.awaitAll()
                }
                clearSelection()
                // Reload current folder contents
                if (_folderStack.value.isEmpty()) {
                    loadFolderContents("root_app_folder", "Folders")
                } else {
                    loadFolderContents(_folderStack.value.last().id, _folderStack.value.last().name)
                }
                refreshSuspend()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun copySelectedFiles() {
        if (currentUserEmail.isEmpty() || _selectedFiles.value.isEmpty()) return
        _copiedFiles.value = _selectedFiles.value
        val count = _selectedFiles.value.size
        clearSelection()
        android.widget.Toast.makeText(context, "$count file(s) copied to clipboard.", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun clearCopiedFiles() {
        _copiedFiles.value = emptySet()
    }

    fun pasteCopiedFiles(targetFolderId: String) {
        if (currentUserEmail.isEmpty() || _copiedFiles.value.isEmpty()) return
        val filesToCopy = _copiedFiles.value.toList()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resolvedTargetId = if (targetFolderId == "root_app_folder") {
                    repository.getOrCreateAppFolder(currentUserEmail)
                } else {
                    targetFolderId
                }

                val targetFolderName = _currentFolderFiles.value.find { it.id == targetFolderId }?.name
                    ?: if (targetFolderId.startsWith("local_folder://")) targetFolderId.removePrefix("local_folder://") else "Folders"

                kotlinx.coroutines.coroutineScope {
                    filesToCopy.map { fileId ->
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            // 1. Cloud copy
                            if (!fileId.startsWith("local://")) {
                                try {
                                    if (isOnline.value) {
                                        repository.copyFile(currentUserEmail, fileId, resolvedTargetId)
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }

                            // 2. Physical local copy
                            val doc = if (fileId.startsWith("local://")) null else {
                                _documents.value.find { it.id == fileId }
                                    ?: _currentFolderFiles.value.find { it.id == fileId }
                                    ?: _searchResults.value.find { it.id == fileId }
                            }
                            val localFile = if (fileId.startsWith("local://")) {
                                java.io.File(fileId.removePrefix("local://"))
                            } else {
                                val docName = doc?.name
                                if (docName != null) findLocalFile(context, currentUserEmail, docName) else null
                            }

                            if (localFile != null && localFile.exists()) {
                                val destDir = if (targetFolderId == "root_app_folder") {
                                    val year = java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(java.util.Date())
                                    val month = java.text.SimpleDateFormat("MM", java.util.Locale.US).format(java.util.Date())
                                    getLocalStorageDir(context, currentUserEmail, year, month)
                                } else {
                                    getLocalCustomFolderDir(context, currentUserEmail, targetFolderName)
                                }
                                
                                val mimeType = doc?.mimeType ?: run {
                                    val ext = localFile.extension.lowercase()
                                    when (ext) {
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
                                }
                                val ext = getExtensionForMimeType(mimeType)
                                val originalName = localFile.name
                                val destFileName = if (ext.isNotEmpty() && !originalName.lowercase().endsWith(".$ext")) {
                                    "$originalName.$ext"
                                } else {
                                    originalName
                                }
                                
                                val destFile = java.io.File(destDir, destFileName)
                                if (localFile.absolutePath != destFile.absolutePath) {
                                    localFile.copyTo(destFile, overwrite = true)
                                }
                            }
                        }
                    }.awaitAll()
                }
                clearCopiedFiles()
                // Reload current folder contents
                if (_folderStack.value.isEmpty()) {
                    loadFolderContents("root_app_folder", "Folders")
                } else {
                    loadFolderContents(_folderStack.value.last().id, _folderStack.value.last().name)
                }
                refreshSuspend()
                android.widget.Toast.makeText(context, "Pasted successfully.", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error pasting files: ${e.message}"
            } finally {
                _isLoading.value = false
            }
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

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Copy to Downloads via MediaStore for API >= 29
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
                } else {
                    // Fallback for API < 29 (Pre-Android 10)
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val destFile = java.io.File(downloadsDir, safeName)
                    localFile.inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
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
                val doc = _documents.value.find { it.id == fileId }
                    ?: _currentFolderFiles.value.find { it.id == fileId }
                val oldName = doc?.name

                if (fileId.startsWith("local_folder://")) {
                    val oldFolderName = fileId.removePrefix("local_folder://")
                    val oldFolderDir = getLocalCustomFolderDir(context, currentUserEmail, oldFolderName)
                    val newFolderDir = getLocalCustomFolderDir(context, currentUserEmail, newName)
                    if (oldFolderDir.exists() && oldFolderDir.absolutePath != newFolderDir.absolutePath) {
                        oldFolderDir.renameTo(newFolderDir)
                    }
                } else {
                    if (isOnline.value) {
                        repository.renameFile(currentUserEmail, fileId, newName)
                    }
                    if (doc?.mimeType == "application/vnd.google-apps.folder" && oldName != null) {
                        val oldFolderDir = getLocalCustomFolderDir(context, currentUserEmail, oldName)
                        val newFolderDir = getLocalCustomFolderDir(context, currentUserEmail, newName)
                        if (oldFolderDir.exists() && oldFolderDir.absolutePath != newFolderDir.absolutePath) {
                            oldFolderDir.renameTo(newFolderDir)
                        }
                    }
                }

                _documents.update { list ->
                    list.map { file ->
                        if (file.id == fileId) {
                            file.clone().setName(newName)
                        } else file
                    }
                }
                _currentFolderFiles.update { list ->
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
            // Optimistic UI updates
            _documents.update { list -> list.filterNot { it.id == fileId } }
            _searchResults.update { list -> list.filterNot { it.id == fileId } }
            _currentFolderFiles.update { list -> list.filterNot { it.id == fileId } }
            
            _isLoading.value = true
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    deleteSingleFileInternal(fileId)
                }
                // Reload current folder contents to reflect deletion
                if (_folderStack.value.isEmpty()) {
                    loadFolderContents("root_app_folder", "Folders")
                } else {
                    loadFolderContents(_folderStack.value.last().id, _folderStack.value.last().name)
                }
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

    private suspend fun deleteSingleFileInternal(fileId: String) {
        if (fileId.startsWith("local_folder://")) {
            val folderName = fileId.removePrefix("local_folder://")
            val localFolderDir = getLocalCustomFolderDir(context, currentUserEmail, folderName)
            if (localFolderDir.exists()) {
                localFolderDir.deleteRecursively()
            }
            try {
                if (isOnline.value) {
                    val appFolderId = repository.getOrCreateAppFolder(currentUserEmail)
                    val cloudFolders = repository.listFilesInFolder(currentUserEmail, appFolderId)
                        .filter { it.mimeType == "application/vnd.google-apps.folder" && it.name == folderName }
                    cloudFolders.forEach { cloudFolder ->
                        cloudFolder.id?.let { repository.deleteFile(currentUserEmail, it) }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else if (fileId.startsWith("local://")) {
            val path = fileId.removePrefix("local://")
            val localFile = java.io.File(path)
            val name = localFile.name
            if (localFile.exists()) {
                localFile.delete()
            }
            // Now attempt to find cloud file with same name and delete it
            try {
                val cloudFiles = repository.searchDocuments(currentUserEmail, "name='$name'")
                cloudFiles.forEach { cloudFile ->
                    cloudFile.id?.let { repository.deleteFile(currentUserEmail, it) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // It's a cloud file or folder
            val doc = _documents.value.find { it.id == fileId }
                ?: _currentFolderFiles.value.find { it.id == fileId }

            if (isOnline.value) {
                repository.deleteFile(currentUserEmail, fileId)
            }

            if (doc != null) {
                if (doc.mimeType == "application/vnd.google-apps.folder") {
                    val folderName = doc.name
                    if (folderName != null) {
                        val localFolderDir = getLocalCustomFolderDir(context, currentUserEmail, folderName)
                        if (localFolderDir.exists()) {
                            localFolderDir.deleteRecursively()
                        }
                    }
                } else {
                    val name = doc.name
                    if (name != null) {
                        val currentFolder = _folderStack.value.lastOrNull()
                        val localFile = if (currentFolder != null) {
                            val folderDir = getLocalCustomFolderDir(context, currentUserEmail, currentFolder.name)
                            val fileInFolder = java.io.File(folderDir, name)
                            if (fileInFolder.exists()) fileInFolder else {
                                folderDir.listFiles()?.find { it.nameWithoutExtension.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                            }
                        } else {
                            val year: String
                            val month: String
                            if (doc.createdTime != null) {
                                val cal = java.util.Calendar.getInstance()
                                cal.timeInMillis = doc.createdTime.value
                                year = String.format(java.util.Locale.US, "%04d", cal.get(java.util.Calendar.YEAR))
                                month = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MONTH) + 1)
                            } else {
                                val cal = java.util.Calendar.getInstance()
                                year = String.format(java.util.Locale.US, "%04d", cal.get(java.util.Calendar.YEAR))
                                month = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MONTH) + 1)
                            }
                            val localDir = getLocalStorageDir(context, currentUserEmail, year, month)
                            val fileInDir = java.io.File(localDir, name)
                            if (fileInDir.exists()) fileInDir else {
                                localDir.listFiles()?.find { it.nameWithoutExtension.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                            }
                        } ?: findLocalFile(context, currentUserEmail, name)
                        
                        if (localFile != null && localFile.exists()) {
                            localFile.delete()
                        }
                    }
                }
            }
        }
    }

    suspend fun getAllCustomFoldersSuspend(): List<File> {
        if (currentUserEmail.isEmpty()) return emptyList()
        val localFolders = getLocalCustomFolders(context, currentUserEmail)
        return try {
            if (isOnline.value) {
                val appFolderId = repository.getOrCreateAppFolder(currentUserEmail)
                val driveFiles = repository.listFilesInFolder(currentUserEmail, appFolderId)
                val driveFolders = driveFiles.filter { it.mimeType == "application/vnd.google-apps.folder" }
                mergeFolders(localFolders, driveFolders)
            } else {
                localFolders
            }
        } catch (e: Exception) {
            localFolders
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

    fun getLocalCustomFolderDir(context: android.content.Context, email: String, folderName: String): java.io.File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val docManagerDir = java.io.File(baseDir, "DocumentManager")
        val userFolderName = if (email.contains("rajprudvi", ignoreCase = true)) {
            "gmail"
        } else {
            email.substringBefore("@").replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "user" }
        }
        val userDir = java.io.File(docManagerDir, userFolderName)
        val customFolderDir = java.io.File(userDir, folderName)
        if (!customFolderDir.exists()) {
            customFolderDir.mkdirs()
        }
        return customFolderDir
    }

    fun getLocalCustomFolders(context: android.content.Context, email: String): List<File> {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val docManagerDir = java.io.File(baseDir, "DocumentManager")
        val userFolderName = if (email.contains("rajprudvi", ignoreCase = true)) {
            "gmail"
        } else {
            email.substringBefore("@").replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "user" }
        }
        val userDir = java.io.File(docManagerDir, userFolderName)
        if (!userDir.exists()) return emptyList()
        
        return userDir.listFiles()?.filter { child ->
            child.isDirectory && !child.name.matches("\\d{4}".toRegex())
        }?.map { dir ->
            val driveFolder = File()
            driveFolder.id = "local_folder://${dir.name}"
            driveFolder.name = dir.name
            driveFolder.mimeType = "application/vnd.google-apps.folder"
            driveFolder.createdTime = com.google.api.client.util.DateTime(dir.lastModified())
            driveFolder
        } ?: emptyList()
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
        val queue = java.util.LinkedList<java.io.File>()
        queue.add(userDir)
        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    queue.add(child)
                } else if (child.isFile && child.length() > 0) {
                    resultList.add(child)
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
        
        val queue = java.util.LinkedList<java.io.File>()
        queue.add(userDir)
        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    queue.add(child)
                } else if (child.isFile && child.length() > 0) {
                    val nameWithoutExt = child.nameWithoutExtension
                    if (child.name.equals(fileName, ignoreCase = true) || nameWithoutExt.equals(fileName, ignoreCase = true)) {
                        return child
                    }
                }
            }
        }
        return null
    }

    fun convertLocalFileToDriveFile(file: java.io.File): File {
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

    private fun mergeFolders(local: List<File>, drive: List<File>): List<File> {
        val driveFolderNames = drive.filter { it.mimeType == "application/vnd.google-apps.folder" }
            .mapNotNull { it.name?.lowercase() }.toSet()
        val uniqueLocal = local.filterNot { it.name?.lowercase() in driveFolderNames }
        return drive + uniqueLocal
    }

    private fun getExtensionForMimeType(mimeType: String?): String {
        val mime = mimeType?.lowercase() ?: return ""
        return when {
            mime.contains("pdf") -> "pdf"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("excel") || mime.contains("spreadsheet") || mime.contains("csv") -> "xlsx"
            mime.contains("word") || mime.contains("document") -> "docx"
            mime.contains("powerpoint") || mime.contains("presentation") -> "pptx"
            mime.contains("text/plain") || mime.contains("txt") -> "txt"
            else -> ""
        }
    }
}
