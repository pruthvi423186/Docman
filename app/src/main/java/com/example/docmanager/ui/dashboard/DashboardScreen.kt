@file:Suppress("UsePropertyAccessSyntax", "SpellCheckingInspection")
package com.example.docmanager.ui.dashboard

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ExperimentalFoundationApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import coil.compose.AsyncImage
import com.example.docmanager.repository.DocumentRepository
import com.example.docmanager.ui.theme.OutfitFontFamily
import com.example.docmanager.util.HapticUtils
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import java.text.SimpleDateFormat
import java.util.*

enum class DashboardTab(val title: String, val icon: ImageVector) {
    RECENT("Recent", Icons.Rounded.History),
    HIERARCHY("Folders", Icons.Rounded.FolderCopy),
    SEARCH("Search", Icons.Rounded.Search),
    SETTINGS("Profile", Icons.Rounded.AccountCircle)
}

data class ThemeColors(
    val isDark: Boolean,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val cardBorderSecondary: Color,
    val iconTint: Color,
    val iconTintSecondary: Color,
    val chipBgSelected: Color,
    val chipTextSelected: Color,
    val glassRefractionAlpha: Float
)

@Composable
fun getThemeColors(): ThemeColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(isDark) {
        ThemeColors(
            isDark = isDark,
            textPrimary = if (isDark) Color.White else Color(0xFF1E1B4B),
            textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF1E1B4B).copy(alpha = 0.65f),
            textTertiary = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF1E1B4B).copy(alpha = 0.45f),
            cardBackground = if (isDark) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.35f),
            cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.60f),
            cardBorderSecondary = if (isDark) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.25f),
            iconTint = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF1E1B4B).copy(alpha = 0.8f),
            iconTintSecondary = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF1E1B4B).copy(alpha = 0.5f),
            chipBgSelected = if (isDark) Color(0xFFE8DDF4) else Color.White,
            chipTextSelected = if (isDark) Color(0xFF1B162E) else Color(0xFF6366F1),
            glassRefractionAlpha = if (isDark) 0.15f else 0.40f
        )
    }
}

@Composable
fun PremiumBackground() {
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    
    // Premium slate-indigo base colors for light mode reduced to 50% brightness to prevent glare
    val baseColor1 = if (isDarkTheme) Color(0xFF0D0B18) else Color(0xFF7A8D9E) 
    val baseColor2 = if (isDarkTheme) Color(0xFF140E24) else Color(0xFF8EA0B0) 
    val baseColor3 = if (isDarkTheme) Color(0xFF0A0814) else Color(0xFF6E8091)
    
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundOrbs")
    
    // Slow drifting values (durations 15-25 seconds for ultra-smooth premium organic feel)
    val drift1X by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1X"
    )
    val drift1Y by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1Y"
    )
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )

    val drift2X by infiniteTransition.animateFloat(
        initialValue = 140f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2X"
    )
    val drift2Y by infiniteTransition.animateFloat(
        initialValue = -140f,
        targetValue = -70f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2Y"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(17000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )

    val drift3X by infiniteTransition.animateFloat(
        initialValue = -90f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift3X"
    )
    val drift3Y by infiniteTransition.animateFloat(
        initialValue = 110f,
        targetValue = 170f,
        animationSpec = infiniteRepeatable(
            animation = tween(21000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift3Y"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(baseColor1, baseColor2, baseColor3)))
    ) {
        // Glowing Orb 1 (Top Left) - Indigo / Violet (Lowered alphas in light mode)
        Box(
            modifier = Modifier
                .offset(x = drift1X.dp, y = drift1Y.dp)
                .scale(scale1)
                .size(350.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) Color(0xFF8B5CF6) else Color(0xFF818CF8)).copy(alpha = if (isDarkTheme) 0.18f else 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Glowing Orb 2 (Middle Right) - Pink / Rose (Lowered alphas in light mode)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = drift2X.dp, y = drift2Y.dp)
                .scale(scale2)
                .size(400.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) Color(0xFFEC4899) else Color(0xFFF472B6)).copy(alpha = if (isDarkTheme) 0.15f else 0.04f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Glowing Orb 3 (Bottom Left) - Sky Blue / Cyan (Lowered alphas in light mode)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = drift3X.dp, y = drift3Y.dp)
                .scale(scale3)
                .size(380.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF38BDF8)).copy(alpha = if (isDarkTheme) 0.18f else 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Glowing Orb 4 (Bottom Right) - Purple / Orchid (Lowered alphas in light mode)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 80.dp)
                .size(360.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) Color(0xFF7C3AED) else Color(0xFFC084FC)).copy(alpha = if (isDarkTheme) 0.12f else 0.03f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

// --- Reusable light refraction overlay with chromatic dispersion & dynamic reflection ---
@Composable
fun BoxScope.GlassRefraction(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(modifier)
            .clip(shape)
    ) {
        // 1. Static base refraction gradient with chromatic dispersion (rainbow spectrals)
        val colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent,
                Color.White.copy(alpha = 0.03f),
                Color.Transparent
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.04f),
                Color.Transparent,
                Color.White.copy(alpha = 0.06f),
                Color.Transparent
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )
        
        // 3. Crisp Top-Left Highlight Stroke (Direct Reflection) - sharp gloss edge & Bottom-Right Bevel Shadow
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw top border edge line
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isDark) 0.20f else 0.45f),
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.18f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, 0.5f),
                end = Offset(size.width, 0.5f),
                strokeWidth = 1.2f
            )
            
            // Draw left border edge line
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.20f else 0.45f),
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.12f),
                        Color.Transparent
                    )
                ),
                start = Offset(0.5f, 0f),
                end = Offset(0.5f, size.height),
                strokeWidth = 1.2f
            )

            // Draw bottom border edge line (bevel shadow)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = if (isDark) 0.22f else 0.18f),
                        Color.Black.copy(alpha = if (isDark) 0.10f else 0.08f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, size.height - 0.5f),
                end = Offset(size.width, size.height - 0.5f),
                strokeWidth = 1.2f
            )
            
            // Draw right border edge line (bevel shadow)
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = if (isDark) 0.22f else 0.18f),
                        Color.Transparent
                    )
                ),
                start = Offset(size.width - 0.5f, 0f),
                end = Offset(size.width - 0.5f, size.height),
                strokeWidth = 1.2f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    themeViewModel: com.example.docmanager.ui.theme.ThemeViewModel,
    userDisplayName: String,
    userEmail: String,
    userPhotoUrl: String?,
    onLogout: () -> Unit
) {
    val colors = getThemeColors()
    val isOnline by viewModel.isOnline.collectAsState()
    val authIntent by viewModel.authIntent.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    
    val filteredDocuments by viewModel.filteredDocuments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val alwaysOpenWithDefault by viewModel.alwaysOpenWithDefault.collectAsState()
    
    var currentTab by remember { mutableStateOf(DashboardTab.RECENT) }
    var showBottomSheet by remember { mutableStateOf(false) }

    var pendingOpenFileId by remember { mutableStateOf<String?>(null) }
    var pendingOpenFileName by remember { mutableStateOf("") }
    var pendingOpenFileMimeType by remember { mutableStateOf("") }
    var showOpenChoiceDialog by remember { mutableStateOf(false) }

    @Suppress("MutableCollectionMutableState")
    var selectedDocumentForOptions by remember { mutableStateOf<com.google.api.services.drive.model.File?>(null) }
    var showDocOptionsSheet by remember { mutableStateOf(false) }
    var showSearchFiltersSheet by remember { mutableStateOf(false) }

    @Suppress("MutableCollectionMutableState")
    var showRenameDialogForDoc by remember { mutableStateOf<com.google.api.services.drive.model.File?>(null) }

    var showWelcome by remember { mutableStateOf(true) }
    var selectedYear by remember { mutableStateOf<String?>(null) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUploadDefaultName by remember { mutableStateOf("") }
    var pendingUploadMimeType by remember { mutableStateOf("") }
    var showLocalStorageConfirmDialog by remember { mutableStateOf(false) }
    var pendingUploadAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var totalRowWidthPx by remember { mutableIntStateOf(0) }

    val performOpenFile: (String, String, String, Boolean) -> Unit = { fileId, fileName, mimeType, forceChooser ->
        viewModel.downloadAndOpenFile(context, fileId, fileName) { localFile ->
            try {
                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, localFile)
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("authAccount", userEmail)
                    putExtra("active_account", userEmail)
                    putExtra("com.google.android.apps.docs.drive.auth.active_account", userEmail)
                    putExtra("com.google.android.apps.docs.auth.active_account", userEmail)
                    putExtra("account", userEmail)
                }
                if (forceChooser) {
                    val chooser = android.content.Intent.createChooser(intent, "Open File").apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } else {
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "No app found to open this file", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val onOpenFile: (String, String, String) -> Unit = { fileId, fileName, mimeType ->
        if (alwaysOpenWithDefault) {
            performOpenFile(fileId, fileName, mimeType, false)
        } else {
            pendingOpenFileId = fileId
            pendingOpenFileName = fileName
            pendingOpenFileMimeType = mimeType
            showOpenChoiceDialog = true
        }
    }

    LaunchedEffect(Unit) {
        delay(500.milliseconds)
        showWelcome = false
    }

    LaunchedEffect(currentTab) {
        selectedYear = null
        selectedMonth = null
    }

    // WorkManager Observation
    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosByTagFlow(DocumentRepository.UPLOAD_WORK_TAG)
        .collectAsState(initial = emptyList())
        
    val isUploading = workInfos.any { it.state == WorkInfo.State.RUNNING }
    val hazeState = remember(currentTab) { HazeState() }
    
    LaunchedEffect(workInfos) {
        if (workInfos.any { it.state == WorkInfo.State.SUCCEEDED }) {
            viewModel.refresh()
            WorkManager.getInstance(context).pruneWork()
        }
    }

    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.clearAuthIntent()
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refresh()
        } else {
            onLogout()
        }
    }
    LaunchedEffect(authIntent) {
        authIntent?.let { intent -> authLauncher.launch(intent) }
    }

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                pendingUploadUri = pdf.uri
                pendingUploadDefaultName = "Scanned_${System.currentTimeMillis()}"
                pendingUploadMimeType = "application/pdf"
                HapticUtils.vibrateSuccess(context)
            }
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scannerClient = remember { GmsDocumentScanning.getClient(scannerOptions) }

    fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun launchScanner() {
        showBottomSheet = false
        val activity = context.findActivity()
        if (activity != null) {
            scannerClient.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
        }
    }

    val multipleFilesPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val forbiddenExtensions = setOf("kt", "java", "cpp", "h", "py", "js", "ts", "json", "xml", "html", "css", "sh", "bat", "gradle", "c", "cs", "go", "rs", "swift", "rb", "pl")
            var blockedCount = 0
            val allowedUris = uris.filter { uri ->
                val extension = getFileExtensionFromUri(context, uri).lowercase()
                val isBlocked = forbiddenExtensions.contains(extension)
                if (isBlocked) blockedCount++
                !isBlocked
            }
            
            if (blockedCount > 0) {
                android.widget.Toast.makeText(context, "$blockedCount file(s) blocked: Coding/engineering files are not permitted.", android.widget.Toast.LENGTH_LONG).show()
            }
            
            if (allowedUris.isNotEmpty()) {
                if (allowedUris.size == 1) {
                    val singleUri = allowedUris.first()
                    pendingUploadUri = singleUri
                    pendingUploadDefaultName = getFileNameFromUri(context, singleUri)
                    pendingUploadMimeType = context.contentResolver.getType(singleUri) ?: "application/octet-stream"
                } else {
                    pendingUploadAction = {
                        allowedUris.forEach { uri ->
                            val fileName = getFileNameFromUri(context, uri)
                            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                            viewModel.uploadFile(context, uri, mime, fileName)
                        }
                        android.widget.Toast.makeText(context, "Uploading ${allowedUris.size} files in the background...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    showLocalStorageConfirmDialog = true
                }
                HapticUtils.vibrateSuccess(context)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().haze(state = hazeState)) {
        PremiumBackground()

        // Splash Screen Overlay (Solid Background with Logo)
        AnimatedVisibility(
            visible = showWelcome,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.zIndex(1000f).fillMaxSize()
        ) {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF0D0C15) else Color(0xFFF9FAFB)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Premium Docman logo
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = if (isDark) Color(0xFF0D0C15) else Color.White,
                                    shape = RoundedCornerShape(22.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DocumentScanner,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = Color(0xFF8B5CF6)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "DOCMAN",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily,
                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                        letterSpacing = 4.sp
                    )
                }
            }
        }

        // Main Scaffold
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val inSelectionMode by viewModel.inSelectionMode.collectAsState()
                val selectedFiles by viewModel.selectedFiles.collectAsState()
                AnimatedVisibility(
                    visible = inSelectionMode,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = { Text("${selectedFiles.size} Selected", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear Selection")
                            }
                        },
                        actions = {
                            val selectedIds by viewModel.selectedFiles.collectAsState()
                            val allDocs by viewModel.documents.collectAsState()
                            IconButton(onClick = {
                                val docsToShare = allDocs.filter { it.id in selectedIds }
                                if (docsToShare.isNotEmpty()) {
                                    val shareText = "Shared via Docman:\n\n" + docsToShare.joinToString("\n\n") { doc ->
                                        val link = doc.webViewLink ?: ""
                                        "${doc.name}: $link"
                                    }
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Documents"))
                                }
                            }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share Selected")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                val docsToDownload = allDocs.filter { it.id in selectedIds }
                                if (docsToDownload.isNotEmpty()) {
                                    docsToDownload.forEach { doc ->
                                        doc.id?.let { fileId ->
                                            viewModel.downloadFileToDevice(
                                                context,
                                                fileId,
                                                doc.name ?: "Document",
                                                doc.mimeType ?: "application/octet-stream"
                                            )
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Rounded.Download, contentDescription = "Download Selected")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                if (isOnline) {
                                    viewModel.deleteSelectedFiles()
                                } else {
                                    android.widget.Toast.makeText(context, "Internet connection is required to delete files.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
        bottomBar = {
            val inSelectionMode by viewModel.inSelectionMode.collectAsState()
            if (!inSelectionMode) {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val density = androidx.compose.ui.platform.LocalDensity.current
                
                var targetIndex by remember { mutableIntStateOf(currentTab.ordinal) }
                var previousIndex by remember { mutableIntStateOf(currentTab.ordinal) }
                LaunchedEffect(currentTab) {
                    if (currentTab.ordinal != targetIndex) {
                        previousIndex = targetIndex
                        targetIndex = currentTab.ordinal
                    }
                }

                val maskColor = if (isDark) Color(0xFF0A0814) else Color(0xFF6E8091)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    maskColor.copy(alpha = 0.95f),
                                    maskColor,
                                    maskColor
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Soft drop shadow Box behind the tab bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .offset(y = 4.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = if (isDark) 0.28f else 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(36.dp)
                            )
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        shape = RoundedCornerShape(36.dp),
                        color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            1.2.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                                    Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                                )
                            )
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            GlassRefraction(shape = RoundedCornerShape(36.dp), modifier = Modifier.matchParentSize())
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .onGloballyPositioned { coordinates ->
                                        totalRowWidthPx = coordinates.size.width
                                    }
                            ) {
                                // Dynamic Liquid Droplet morphing indicator
                                if (totalRowWidthPx > 0) {
                                    val tabWidthPx = totalRowWidthPx / 4f
                                    
                                    val stationaryWidthPx = tabWidthPx - with(density) { 4.dp.toPx() }
                                    val heightPx = with(density) { 54.dp.toPx() }
                                    
                                    val targetCenterX = tabWidthPx * (targetIndex + 0.5f)
                                    val targetLeft = targetCenterX - stationaryWidthPx / 2f
                                    val targetRight = targetCenterX + stationaryWidthPx / 2f
                                    
                                    val movingRight = targetIndex > previousIndex
                                    
                                    val fastSpring = spring<Float>(stiffness = Spring.StiffnessMedium, dampingRatio = 0.65f)
                                    val slowSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f)
                                    
                                    val animatedLeft by animateFloatAsState(
                                        targetValue = targetLeft,
                                        animationSpec = if (movingRight) slowSpring else fastSpring,
                                        label = "dropletLeft"
                                    )
                                    val animatedRight by animateFloatAsState(
                                        targetValue = targetRight,
                                        animationSpec = if (movingRight) fastSpring else slowSpring,
                                        label = "dropletRight"
                                    )
                                    
                                    val currentWidth = animatedRight - animatedLeft
                                    val widthScale = currentWidth / stationaryWidthPx
                                    val heightScale = (1f - 0.25f * (widthScale - 1f).coerceIn(0f, 2f)).coerceIn(0.5f, 1f)
                                    val height = heightPx * heightScale
                                    
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val left = animatedLeft
                                        val right = animatedRight
                                        val width = right - left
                                        val top = (size.height - height) / 2f
                                        val bottom = top + height
                                        
                                        // Draw the droplet background (frosted translucent signature color)
                                        val dropletColor = when (DashboardTab.entries[targetIndex]) {
                                            DashboardTab.RECENT -> Color(0xFF3B82F6)
                                            DashboardTab.HIERARCHY -> Color(0xFFF59E0B)
                                            DashboardTab.SEARCH -> Color(0xFF10B981)
                                            DashboardTab.SETTINGS -> Color(0xFFEC4899)
                                        }
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(
                                                colors = if (isDark) {
                                                    listOf(dropletColor.copy(alpha = 0.35f), dropletColor.copy(alpha = 0.20f))
                                                } else {
                                                    listOf(dropletColor.copy(alpha = 0.22f), dropletColor.copy(alpha = 0.12f))
                                                }
                                            ),
                                            topLeft = Offset(left, top),
                                            size = androidx.compose.ui.geometry.Size(width, height),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f, height / 2f)
                                        )
                                        
                                        // Draw specularity outline around the droplet itself
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            addRoundRect(
                                                androidx.compose.ui.geometry.RoundRect(
                                                    left = left,
                                                    top = top,
                                                    right = right,
                                                    bottom = bottom,
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f, height / 2f)
                                                )
                                            )
                                        }
                                        drawPath(
                                            path = path,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = if (isDark) 0.50f else 0.70f),
                                                    Color.White.copy(alpha = 0.05f)
                                                )
                                            ),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DashboardTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        val activeTabColor = when (tab) {
                                            DashboardTab.RECENT -> Color(0xFF3B82F6) // Vibrant Blue
                                            DashboardTab.HIERARCHY -> Color(0xFFF59E0B) // Vibrant Amber/Orange
                                            DashboardTab.SEARCH -> Color(0xFF10B981) // Vibrant Emerald/Green
                                            DashboardTab.SETTINGS -> Color(0xFFEC4899) // Vibrant Pink/Rose
                                        }
                                        val animatedColor by animateColorAsState(
                                            if (selected) {
                                                activeTabColor
                                            } else {
                                                if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF1E1B4B).copy(alpha = 0.5f)
                                            },
                                            label = "color"
                                        )
                                        val iconSize by animateDpAsState(
                                            targetValue = if (selected) 28.dp else 24.dp,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "iconSize"
                                        )
 
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = LocalIndication.current
                                                ) {
                                                    HapticUtils.vibrateShort(context)
                                                    currentTab = tab
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = animatedColor,
                                                modifier = Modifier.size(iconSize)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = tab.title,
                                                fontSize = 10.sp,
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = animatedColor,
                                                fontFamily = OutfitFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == DashboardTab.RECENT || currentTab == DashboardTab.HIERARCHY) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 88.dp)
                        .navigationBarsPadding()
                        .size(56.dp)
                        .shadow(12.dp, CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.85f),
                                    Color(0xFF6366F1).copy(alpha = 0.85f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            1.2.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.50f),
                                    Color.White.copy(alpha = 0.10f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable {
                            HapticUtils.vibrateShort(context)
                            showBottomSheet = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    GlassRefraction(shape = CircleShape, modifier = Modifier.matchParentSize())
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Document",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = !isOnline,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFFEE2E2),
                                contentColor = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                0.5.dp,
                                if (isDarkTheme) Color(0xFFEF4444).copy(alpha = 0.3f) else Color(0xFFFCA5A5)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudOff,
                                    contentDescription = "Offline",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Offline Mode — Viewing cached files",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Breadcrumbs if in Hierarchy tab (shown above Folders header)
                    AnimatedVisibility(
                        visible = currentTab == DashboardTab.HIERARCHY,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 0.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = "Home",
                                tint = colors.textPrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedYear = null
                                        selectedMonth = null
                                    }
                            )
                            if (selectedYear != null) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selectedYear!!,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedMonth = null },
                                    fontFamily = OutfitFontFamily
                                )
                            }
                            if (selectedMonth != null) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selectedMonth!!,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontFamily = OutfitFontFamily
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        }, label = "Header Transition"
                    ) { tab ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = if (tab == DashboardTab.HIERARCHY) 12.dp else 48.dp, end = 24.dp, bottom = 16.dp),
                            horizontalArrangement = if (tab == DashboardTab.SETTINGS) Arrangement.Center else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 41.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = OutfitFontFamily,
                                color = colors.textPrimary
                            )
                            if (tab == DashboardTab.SEARCH) {
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { showSearchFiltersSheet = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreHoriz,
                                        contentDescription = "More Options",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Error Message
                    AnimatedVisibility(visible = errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clickable { viewModel.clearErrorMessage() },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(errorMessage ?: "", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    // Uploading Indicator
                    AnimatedVisibility(visible = isUploading) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Uploading safely to Google Drive...", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }



                    // Content Switcher
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()
                    ) {
                        when (currentTab) {
                            DashboardTab.RECENT -> RecentTabContent(
                                viewModel = viewModel,
                                documents = filteredDocuments,
                                isLoading = isLoading,
                                onOpenFile = onOpenFile,
                                onOptionsClick = { doc ->
                                    selectedDocumentForOptions = doc
                                    showDocOptionsSheet = true
                                }
                            )
                            DashboardTab.HIERARCHY -> HierarchyTabContent(
                                viewModel = viewModel,
                                allDocuments = viewModel.documents.collectAsState().value,
                                isLoading = isLoading,
                                onOpenFile = onOpenFile,
                                onOptionsClick = { doc ->
                                    selectedDocumentForOptions = doc
                                    showDocOptionsSheet = true
                                },
                                selectedYear = selectedYear,
                                selectedMonth = selectedMonth,
                                onYearChange = { selectedYear = it },
                                onMonthChange = { selectedMonth = it }
                            )
                            DashboardTab.SEARCH -> SearchTabContent(
                                viewModel = viewModel,
                                onOpenFile = onOpenFile,
                                onOptionsClick = { doc ->
                                    selectedDocumentForOptions = doc
                                    showDocOptionsSheet = true
                                }
                            )
                            DashboardTab.SETTINGS -> SettingsTabContent(userDisplayName, userEmail, userPhotoUrl, onLogout, themeViewModel, viewModel, hazeState)
                        }
                    }
                    }
                }
            }

            // Custom Glass Bottom Sheet for Create Options
            CustomGlassBottomSheet(
                visible = showBottomSheet,
                onDismiss = { showBottomSheet = false },
                hazeState = hazeState
            ) {
                Text("Create New", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OutfitFontFamily, color = colors.textPrimary, modifier = Modifier.padding(bottom = 24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UploadOptionCard(
                        title = "Scan",
                        subtitle = "Camera",
                        icon = Icons.Rounded.DocumentScanner,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isOnline) {
                                launchScanner()
                            } else {
                                showBottomSheet = false
                                android.widget.Toast.makeText(context, "Internet connection is required to scan documents.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                    UploadOptionCard(
                        title = "Upload",
                        subtitle = "Files",
                        icon = Icons.Rounded.FolderOpen,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isOnline) {
                                showBottomSheet = false
                                multipleFilesPickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.oasis.opendocument.text",
                                        "application/vnd.ms-excel",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.oasis.opendocument.spreadsheet",
                                        "application/vnd.ms-powerpoint",
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                        "application/vnd.oasis.opendocument.presentation",
                                        "text/plain",
                                        "text/csv",
                                        "application/rtf",
                                        "image/jpeg",
                                        "image/png",
                                        "image/webp"
                                    )
                                )
                            } else {
                                showBottomSheet = false
                                android.widget.Toast.makeText(context, "Internet connection is required to upload files.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }

        val isDownloadingFile by viewModel.isDownloadingFile.collectAsState()
        AnimatedVisibility(
            visible = isDownloadingFile != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .hazeChild(
                        state = hazeState,
                        style = HazeStyle(tint = Color.Transparent, blurRadius = 16.dp)
                    )
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1B162E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Opening document...",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF1E1B4B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = isDownloadingFile ?: "",
                            fontSize = 13.sp,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Custom Glass Dialog for Open Choice
        CustomGlassDialog(
            visible = showOpenChoiceDialog,
            onDismiss = { showOpenChoiceDialog = false },
            hazeState = hazeState
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Open Document",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "How would you like to open '$pendingOpenFileName'?",
                    fontFamily = OutfitFontFamily,
                    color = colors.textSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            showOpenChoiceDialog = false
                            performOpenFile(pendingOpenFileId!!, pendingOpenFileName, pendingOpenFileMimeType, true)
                        }
                    ) {
                        Text("Just Once", color = if (colors.isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF1E1B4B).copy(alpha = 0.8f), fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.setAlwaysOpenWithDefault(true)
                            showOpenChoiceDialog = false
                            performOpenFile(pendingOpenFileId!!, pendingOpenFileName, pendingOpenFileMimeType, false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Always", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Custom Glass Bottom Sheet for Document Options
        if (selectedDocumentForOptions != null) {
            val doc = selectedDocumentForOptions!!
            val mime = doc.mimeType?.lowercase() ?: ""
            val (typeLabel, badgeColor) = when {
                mime.contains("pdf") -> "PDF" to Color(0xFFEF4444)
                mime.contains("image") || mime.contains("jpeg") || mime.contains("png") || mime.contains("webp") -> "IMG" to Color(0xFFEC4899)
                mime.contains("excel") || mime.contains("spreadsheet") || mime.contains("csv") -> "XLS" to Color(0xFF10B981)
                mime.contains("powerpoint") || mime.contains("presentation") -> "PPT" to Color(0xFFF59E0B)
                mime.contains("text/plain") || mime.contains("txt") || mime.contains("rtf") -> "TXT" to Color(0xFF6B7280)
                else -> "DOC" to Color(0xFF3B82F6)
            }
            val isDark = colors.isDark
            val typeBg = if (isDark) badgeColor.copy(alpha = 0.15f) else badgeColor.copy(alpha = 0.12f)
            val typeColor = if (isDark) {
                when (typeLabel) {
                    "PDF" -> Color(0xFFFCA5A5)
                    "IMG" -> Color(0xFFFBCFE8)
                    "DOC" -> Color(0xFF93C5FD)
                    "XLS" -> Color(0xFF6EE7B7)
                    "PPT" -> Color(0xFFFDBA74)
                    else -> Color(0xFFD1D5DB)
                }
            } else {
                when (typeLabel) {
                    "PDF" -> Color(0xFFB91C1C)
                    "IMG" -> Color(0xFFBE185D)
                    "DOC" -> Color(0xFF1D4ED8)
                    "XLS" -> Color(0xFF047857)
                    "PPT" -> Color(0xFFB45309)
                    else -> Color(0xFF4B5563)
                }
            }

            CustomGlassBottomSheet(
                visible = showDocOptionsSheet,
                onDismiss = { showDocOptionsSheet = false },
                hazeState = hazeState
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(typeBg)
                            .border(1.dp, if (isDark) badgeColor.copy(alpha = 0.3f) else badgeColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = typeLabel,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = typeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.name ?: "Unknown Document",
                            fontFamily = OutfitFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E1B4B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatDriveDate(doc.createdTime?.toString())} • ${formatFileSize(doc.getSize())}",
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f)
                        )
                    }
                }

                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF1E1B4B).copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                DocOptionItem(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    label = "Open Document",
                    isDark = isDark,
                    onClick = {
                        showDocOptionsSheet = false
                        doc.id?.let { fileId ->
                            onOpenFile(fileId, doc.name ?: "Document", doc.mimeType ?: "application/octet-stream")
                        }
                    }
                )

                DocOptionItem(
                    icon = Icons.Rounded.Download,
                    label = "Download to Device",
                    isDark = isDark,
                    onClick = {
                        showDocOptionsSheet = false
                        doc.id?.let { fileId ->
                            viewModel.downloadFileToDevice(context, fileId, doc.name ?: "Document", doc.mimeType ?: "application/octet-stream")
                        }
                    }
                )

                DocOptionItem(
                    icon = Icons.Rounded.Share,
                    label = "Share Document",
                    isDark = isDark,
                    onClick = {
                        showDocOptionsSheet = false
                        doc.id?.let { fileId ->
                            viewModel.downloadAndOpenFile(context, fileId, doc.name ?: "Document") { localFile ->
                                try {
                                    val authority = "${context.packageName}.fileprovider"
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, localFile)
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = doc.mimeType ?: "application/octet-stream"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, doc.name ?: "Document")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = android.content.Intent.createChooser(shareIntent, "Share Document").apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Error sharing document: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )

                DocOptionItem(
                    icon = Icons.Rounded.Edit,
                    label = "Rename Document",
                    isDark = isDark,
                    onClick = {
                        showDocOptionsSheet = false
                        if (isOnline) {
                            showRenameDialogForDoc = doc
                        } else {
                            android.widget.Toast.makeText(context, "Internet connection is required to rename files.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                )

                DocOptionItem(
                    icon = Icons.Rounded.Delete,
                    label = "Delete Document",
                    isDark = isDark,
                    isDestructive = true,
                    onClick = {
                        showDocOptionsSheet = false
                        if (isOnline) {
                            doc.id?.let { viewModel.deleteSingleFile(it) }
                        } else {
                            android.widget.Toast.makeText(context, "Internet connection is required to delete files.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }

        // Custom Glass Bottom Sheet for Search Filters
        CustomGlassBottomSheet(
            visible = showSearchFiltersSheet,
            onDismiss = { showSearchFiltersSheet = false },
            hazeState = hazeState
        ) {
            val searchDateFilter by viewModel.searchDateFilter.collectAsState()
            val searchSizeFilter by viewModel.searchSizeFilter.collectAsState()
            val isDark = colors.isDark

            Text(
                text = "Search Filters",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = OutfitFontFamily,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Filter by Date Uploaded",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateOptions = listOf(
                    "all" to "All Time",
                    "today" to "Today",
                    "week" to "Last 7 Days",
                    "month" to "Last 30 Days"
                )
                dateOptions.forEach { (value, label) ->
                    val selected = searchDateFilter == value
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setSearchDateFilter(value) },
                        label = { Text(label, fontFamily = OutfitFontFamily) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF8B5CF6),
                            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f),
                            labelColor = colors.textPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Filter by File Size",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sizeOptions = listOf(
                    "all" to "Any Size",
                    "small" to "Small (< 1MB)",
                    "medium" to "Medium (1-10MB)",
                    "large" to "Large (> 10MB)"
                )
                sizeOptions.forEach { (value, label) ->
                    val selected = searchSizeFilter == value
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setSearchSizeFilter(value) },
                        label = { Text(label, fontFamily = OutfitFontFamily) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF8B5CF6),
                            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f),
                            labelColor = colors.textPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showSearchFiltersSheet = false },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Text("Apply Filters", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
            }
        }

        // Custom Glass Dialog for Rename
        CustomGlassDialog(
            visible = showRenameDialogForDoc != null,
            onDismiss = { showRenameDialogForDoc = null },
            hazeState = hazeState
        ) {
            val doc = showRenameDialogForDoc
            if (doc != null) {
                var newName by remember(doc) { mutableStateOf(doc.name ?: "") }
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Rename Document", fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily, fontSize = 20.sp, color = colors.textPrimary, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Document Name", fontFamily = OutfitFontFamily) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            focusedLabelColor = Color(0xFF8B5CF6)
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRenameDialogForDoc = null }) {
                            Text("Cancel", color = colors.textSecondary, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    doc.id?.let { viewModel.renameFile(it, newName) }
                                }
                                showRenameDialogForDoc = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Text("Save", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Custom Glass Dialog for upload naming
        CustomGlassDialog(
            visible = pendingUploadUri != null,
            onDismiss = {
                pendingUploadUri = null
                pendingUploadMimeType = ""
            },
            hazeState = hazeState
        ) {
            val uri = pendingUploadUri
            if (uri != null) {
                var customName by remember(uri) { mutableStateOf(pendingUploadDefaultName) }
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Name Your Document", fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily, fontSize = 20.sp, color = colors.textPrimary, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Document Name", fontFamily = OutfitFontFamily) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            focusedLabelColor = Color(0xFF8B5CF6)
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                pendingUploadUri = null
                                pendingUploadMimeType = ""
                            }
                        ) {
                            Text("Cancel", color = colors.textSecondary, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val ext = getFileExtensionFromUri(context, uri)
                                val finalName = if (ext.isNotEmpty() && !customName.endsWith(".$ext", ignoreCase = true)) {
                                    "$customName.$ext"
                                } else {
                                    customName
                                }
                                val mime = pendingUploadMimeType.ifEmpty { context.contentResolver.getType(uri) ?: "application/octet-stream" }
                                
                                pendingUploadAction = {
                                    viewModel.uploadFile(context, uri, mime, finalName)
                                }
                                showLocalStorageConfirmDialog = true
                                
                                pendingUploadUri = null
                                pendingUploadMimeType = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Text("Upload", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Custom Glass Dialog for Local Storage Save warning
        CustomGlassDialog(
            visible = showLocalStorageConfirmDialog,
            onDismiss = {
                showLocalStorageConfirmDialog = false
                pendingUploadAction = null
            },
            hazeState = hazeState
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Save Offline Copy?", fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily, fontSize = 20.sp, color = colors.textPrimary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "This document will be uploaded to Google Drive and also saved in your local storage for easy offline access.",
                    fontFamily = OutfitFontFamily,
                    color = colors.textSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            showLocalStorageConfirmDialog = false
                            pendingUploadAction = null
                        }
                    ) {
                        Text("Cancel", color = colors.textSecondary, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            pendingUploadAction?.invoke()
                            pendingUploadAction = null
                            showLocalStorageConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Proceed", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecentTabContent(
    viewModel: DashboardViewModel,
    documents: List<com.google.api.services.drive.model.File>,
    isLoading: Boolean,
    onOpenFile: (String, String, String) -> Unit,
    onOptionsClick: (com.google.api.services.drive.model.File) -> Unit
) {
    val sortOrder by viewModel.sortOrder.collectAsState()
    val activeFilters by viewModel.activeFilters.collectAsState()
    val inSelectionMode by viewModel.inSelectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumSortToggle(
                text = if (sortOrder == SortOrder.DATE_DESC) "Newest First" else "Oldest First",
                onClick = { viewModel.setSortOrder(if (sortOrder == SortOrder.DATE_DESC) SortOrder.DATE_ASC else SortOrder.DATE_DESC) }
            )
        }

        FilterTabBar(
            activeFilters = activeFilters,
            onFilterSelected = { filter ->
                viewModel.setSingleFilter(filter)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (isLoading && documents.isEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(5) { ShimmerCompactDocumentItem() }
                }
            } else if (!isLoading && documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No documents match filters", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id ?: it.hashCode() }) { doc ->
                        CompactDocumentItem(
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = 0.8f
                                )
                            ),
                            document = doc,
                            onOpenFile = onOpenFile,
                            isSelected = selectedFiles.contains(doc.id),
                            inSelectionMode = inSelectionMode,
                            onLongPress = { doc.id?.let { viewModel.toggleSelection(it) } },
                            onClick = { doc.id?.let { viewModel.toggleSelection(it) } },
                            onOptionsClick = {
                                onOptionsClick(doc)
                            }
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun HierarchyTabContent(
    viewModel: DashboardViewModel,
    allDocuments: List<com.google.api.services.drive.model.File>,
    isLoading: Boolean,
    onOpenFile: (String, String, String) -> Unit,
    onOptionsClick: (com.google.api.services.drive.model.File) -> Unit,
    selectedYear: String?,
    selectedMonth: String?,
    onYearChange: (String?) -> Unit,
    onMonthChange: (String?) -> Unit
) {
    val inSelectionMode by viewModel.inSelectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    if (isLoading && allDocuments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val groupedByYearMonth = remember(allDocuments) {
        val formatYear = SimpleDateFormat("yyyy", Locale.getDefault())
        val formatMonth = SimpleDateFormat("MMMM", Locale.getDefault())
        val map = mutableMapOf<String, MutableMap<String, MutableList<com.google.api.services.drive.model.File>>>()
        allDocuments.forEach { doc ->
            if (doc.createdTime != null) {
                val date = Date(doc.createdTime.value)
                val year = formatYear.format(date)
                val month = formatMonth.format(date)
                if (!map.containsKey(year)) map[year] = mutableMapOf()
                if (!map[year]!!.containsKey(month)) map[year]!![month] = mutableListOf()
                map[year]!![month]!!.add(doc)
            }
        }
        map.toSortedMap(reverseOrder())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Create a data class for state to trigger AnimatedContent
        data class HierarchyState(val year: String?, val month: String?)
        val currentState = HierarchyState(selectedYear, selectedMonth)

        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                val isForward = if (initialState.year == null && targetState.year != null) true 
                                else if (initialState.month == null && targetState.month != null) true
                                else false
                if (isForward) {
                    (slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
                    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
                        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
                    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
                        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                }.using(SizeTransform(clip = false))
            },
            label = "hierarchy_animation",
            modifier = Modifier.fillMaxSize()
        ) { state ->
            if (state.year == null) {
                // View Years (Level 1)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(groupedByYearMonth.keys.toList()) { year ->
                        val fileCount = groupedByYearMonth[year]?.values?.sumOf { it.size } ?: 0
                        FolderCard(title = year, subtitle = "$fileCount files", onClick = { onYearChange(year) })
                    }
                }
            } else if (state.month == null) {
                // View Months in Year (Level 2)
                val months = groupedByYearMonth[state.year] ?: emptyMap()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(months.keys.toList().sortedDescending()) { month ->
                        val fileCount = months[month]?.size ?: 0
                        FolderCard(title = month, subtitle = "$fileCount files", onClick = { onMonthChange(month) })
                    }
                }
            } else {
                // View Documents in Month (Level 3)
                val documents = groupedByYearMonth[state.year]?.get(state.month) ?: emptyList()
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id ?: it.hashCode() }) { file ->
                        CompactDocumentItem(
                            document = file,
                            onOpenFile = onOpenFile,
                            isSelected = selectedFiles.contains(file.id),
                            inSelectionMode = inSelectionMode,
                            onLongPress = { file.id?.let { viewModel.toggleSelection(it) } },
                            onClick = { file.id?.let { viewModel.toggleSelection(it) } },
                            onOptionsClick = {
                                onOptionsClick(file)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderCard(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = getThemeColors()
    val isDark = colors.isDark
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
        label = "folder_press_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = if (isDark) 0.28f else 0.12f),
                            Color.Black.copy(alpha = if (isDark) 0.08f else 0.03f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        )
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.2.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                        Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                    )
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                GlassRefraction(modifier = Modifier.matchParentSize(), shape = RoundedCornerShape(24.dp))
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = "Folder",
                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                        modifier = Modifier.size(44.dp)
                    )
                    Column {
                        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily, color = if (isDark) Color.White else Color(0xFF1E1B4B))
                        Text(subtitle, fontSize = 12.sp, color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1E1B4B).copy(alpha = 0.7f), fontWeight = FontWeight.Medium, fontFamily = OutfitFontFamily)
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTabContent(
    viewModel: DashboardViewModel,
    onOpenFile: (String, String, String) -> Unit,
    onOptionsClick: (com.google.api.services.drive.model.File) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.filteredSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val inSelectionMode by viewModel.inSelectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val colors = getThemeColors()
    val isDark = colors.isDark

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 2.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = if (isDark) 0.15f else 0.06f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            )
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
                color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.30f else 0.40f),
                            Color.White.copy(alpha = 0.0f)
                        )
                    )
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GlassRefraction(shape = RoundedCornerShape(28.dp), modifier = Modifier.matchParentSize())
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Search all files...", fontSize = 15.sp, color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1E1B4B).copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedTextColor = if (isDark) Color.White else Color(0xFF1E1B4B),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF1E1B4B),
                            cursorColor = if (isDark) Color.White else Color(0xFF1E1B4B)
                        ),
                        singleLine = true
                    )
                }
            }
        }
        
        val matchingRecent = recentSearches.filter { it.contains(searchQuery, ignoreCase = true) && it != searchQuery }
        if (searchQuery.isNotBlank() && matchingRecent.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(matchingRecent) { term ->
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.onSearchQueryChanged(term) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.30f else 0.40f),
                                    Color.White.copy(alpha = 0.0f)
                                )
                            )
                        )
                    ) {
                        Box {
                            GlassRefraction(shape = RoundedCornerShape(12.dp), modifier = Modifier.matchParentSize())
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF4F46E5).copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = term,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color(0xFF4F46E5)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = if (isDark) Color.White else Color(0xFF6366F1)) }
        } else if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                Text("No results found for '$searchQuery'", color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f), fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section header
                item {
                    Text(
                        text = if (searchQuery.isBlank()) "Recent searches" else "All files",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1E1B4B).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (searchQuery.isBlank()) {
                    if (recentSearches.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent searches",
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF1E1B4B).copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        items(recentSearches) { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onSearchQueryChanged(term) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF1E1B4B).copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = term,
                                    fontSize = 16.sp,
                                    color = if (isDark) Color.White else Color(0xFF1E1B4B),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeRecentSearch(term) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Delete search history item",
                                        tint = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF1E1B4B).copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(searchResults, key = { it.id ?: it.hashCode() }) { doc -> 
                        CompactDocumentItem(
                            document = doc,
                            onOpenFile = onOpenFile,
                            isSelected = selectedFiles.contains(doc.id),
                            inSelectionMode = inSelectionMode,
                            onLongPress = { doc.id?.let { viewModel.toggleSelection(it) } },
                            onClick = { doc.id?.let { viewModel.toggleSelection(it) } },
                            searchQuery = searchQuery,
                            onOptionsClick = {
                                onOptionsClick(doc)
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SettingsTabContent(
    userDisplayName: String,
    userEmail: String,
    userPhotoUrl: String?,
    onLogout: () -> Unit,
    themeViewModel: com.example.docmanager.ui.theme.ThemeViewModel,
    viewModel: DashboardViewModel,
    hazeState: HazeState
) {
    val themePreference by themeViewModel.themePreference.collectAsState()
    val storageUsed by viewModel.storageUsedBytes.collectAsState()
    val storageLimit by viewModel.storageLimitBytes.collectAsState()
    val customPhotoUri by viewModel.profilePhotoUri.collectAsState()
    val colors = getThemeColors()
    val isDark = colors.isDark

    val photoSource = if (!customPhotoUri.isNullOrBlank()) customPhotoUri else userPhotoUrl
    var showPhotoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val internalFile = java.io.File(context.filesDir, "profile_picture_${System.currentTimeMillis()}.jpg")
                internalFile.outputStream().use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("profile_picture_") && file.absolutePath != internalFile.absolutePath) {
                        file.delete()
                    }
                }
                viewModel.saveProfilePicture(internalFile.toURI().toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    CustomGlassDialog(
        visible = showPhotoDialog,
        onDismiss = { showPhotoDialog = false },
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Profile Picture",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Choose an action to update your profile photo.",
                fontFamily = OutfitFontFamily,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!customPhotoUri.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            viewModel.deleteProfilePicture()
                            try {
                                val uri = Uri.parse(customPhotoUri)
                                if (uri.scheme == "file") {
                                    uri.path?.let { path ->
                                        val file = java.io.File(path)
                                        if (file.exists()) file.delete()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            showPhotoDialog = false
                        }
                    ) {
                        Text("Remove Photo", color = MaterialTheme.colorScheme.error, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(
                    onClick = {
                        imageLauncher.launch("image/*")
                        showPhotoDialog = false
                    }
                ) {
                    Text("Choose Photo", color = Color(0xFF6366F1), fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = { showPhotoDialog = false }
                ) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Format storage
    val usedGB = storageUsed / (1024.0 * 1024.0 * 1024.0)
    val limitGB = storageLimit / (1024.0 * 1024.0 * 1024.0)
    val usedFormatted = String.format(Locale.getDefault(), "%.1f", usedGB)
    val limitFormatted = String.format(Locale.getDefault(), "%.0f", limitGB)
    val progress = if (storageLimit > 0) (storageUsed.toFloat() / storageLimit.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.size(90.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF1E1B4B).copy(alpha = 0.08f), CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFB8B6C8))
                        .clickable {
                            showPhotoDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoSource.isNullOrBlank()) {
                        AsyncImage(
                            model = photoSource,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(userDisplayName.take(1).uppercase(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF6366F1), CircleShape)
                    .border(2.dp, if (isDark) Color(0xFF100E1D) else Color.White, CircleShape)
                    .clickable {
                        showPhotoDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Profile Picture",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(userDisplayName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1E1B4B))
        Text(userEmail, fontSize = 13.sp, color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        
        Spacer(modifier = Modifier.height(36.dp))
        
        // Storage Usage - Real data with progress bar (reverted layout structure with glass style)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {},
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.30f else 0.40f),
                        Color.White.copy(alpha = 0.0f)
                    )
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                GlassRefraction(modifier = Modifier.matchParentSize(), shape = RoundedCornerShape(16.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).background(if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF1E1B4B).copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cloud,
                                contentDescription = null,
                                tint = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF1E1B4B).copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Storage Usage", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color(0xFF1E1B4B))
                        Spacer(modifier = Modifier.weight(1f))
                        Text("$usedFormatted GB / $limitFormatted GB", fontSize = 12.sp, color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF8B5CF6),
                        trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFF1E1B4B).copy(alpha = 0.1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Theme row
        ProfileSettingsRow(
            icon = Icons.Rounded.Palette,
            label = "Theme",
            trailingText = when (themePreference) {
                "light" -> "Light"
                "dark" -> "Dark"
                else -> "System"
            },
            onClick = {
                val next = when (themePreference) {
                    "system" -> "light"
                    "light" -> "dark"
                    else -> "system"
                }
                themeViewModel.setTheme(next)
            }
        )
        
        val alwaysOpen by viewModel.alwaysOpenWithDefault.collectAsState()
        Spacer(modifier = Modifier.height(12.dp))
        ProfileSettingsRow(
            icon = Icons.AutoMirrored.Rounded.Launch,
            label = "Open Files Directly",
            trailingText = if (alwaysOpen) "Default App" else "Always Ask",
            onClick = {
                viewModel.setAlwaysOpenWithDefault(!alwaysOpen)
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        ProfileSettingsRow(
            icon = Icons.AutoMirrored.Rounded.Logout,
            label = "Sign Out",
            onClick = { onLogout() }
        )
    }
}

@Composable
fun ProfileSettingsRow(
    icon: ImageVector,
    label: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    val colors = getThemeColors()
    val isDark = colors.isDark
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft drop shadow Box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = if (isDark) 0.20f else 0.08f),
                            Color.Black.copy(alpha = if (isDark) 0.05f else 0.02f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.2.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                        Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                    )
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                GlassRefraction(modifier = Modifier.matchParentSize(), shape = RoundedCornerShape(16.dp))
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).background(if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF1E1B4B).copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = label, tint = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF1E1B4B).copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color(0xFF1E1B4B), modifier = Modifier.weight(1f), fontFamily = OutfitFontFamily)
                    if (trailingText != null) {
                        Text(trailingText, fontSize = 13.sp, color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f), fontWeight = FontWeight.Medium, fontFamily = OutfitFontFamily)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF1E1B4B).copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    baseColor: Color,
    highlightColor: Color = Color(0xFF8B5CF6),
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = 1
) {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        Text(
            text = text,
            color = baseColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        var startIdx = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        
        while (true) {
            val idx = lowerText.indexOf(lowerQuery, startIdx)
            if (idx == -1) {
                append(text.substring(startIdx))
                break
            }
            append(text.substring(startIdx, idx))
            withStyle(style = SpanStyle(color = highlightColor, fontWeight = FontWeight.ExtraBold)) {
                append(text.substring(idx, idx + query.length))
            }
            startIdx = idx + query.length
        }
    }

    Text(
        text = annotatedString,
        color = baseColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactDocumentItem(
    modifier: Modifier = Modifier,
    document: com.google.api.services.drive.model.File,
    onOpenFile: (fileId: String, fileName: String, mimeType: String) -> Unit,
    isSelected: Boolean = false,
    inSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    searchQuery: String = "",
    onOptionsClick: () -> Unit = {}
) {
    val colors = getThemeColors()
    val isDark = colors.isDark
    val mime = document.mimeType?.lowercase() ?: ""
    val (typeLabel, badgeColor) = when {
        mime.contains("pdf") -> "PDF" to Color(0xFFEF4444)
        mime.contains("image") || mime.contains("jpeg") || mime.contains("png") || mime.contains("webp") -> "IMG" to Color(0xFFEC4899)
        mime.contains("excel") || mime.contains("spreadsheet") || mime.contains("csv") -> "XLS" to Color(0xFF10B981)
        mime.contains("powerpoint") || mime.contains("presentation") -> "PPT" to Color(0xFFF59E0B)
        mime.contains("text/plain") || mime.contains("txt") || mime.contains("rtf") -> "TXT" to Color(0xFF6B7280)
        else -> "DOC" to Color(0xFF3B82F6)
    }
    val typeBg = if (isDark) badgeColor.copy(alpha = 0.15f) else badgeColor.copy(alpha = 0.12f)
    val typeColor = if (isDark) {
        when (typeLabel) {
            "PDF" -> Color(0xFFFCA5A5)
            "IMG" -> Color(0xFFFBCFE8)
            "DOC" -> Color(0xFF93C5FD)
            "XLS" -> Color(0xFF6EE7B7)
            "PPT" -> Color(0xFFFDBA74)
            else -> Color(0xFFD1D5DB)
        }
    } else {
        when (typeLabel) {
            "PDF" -> Color(0xFFB91C1C)
            "IMG" -> Color(0xFFBE185D)
            "DOC" -> Color(0xFF1D4ED8)
            "XLS" -> Color(0xFF047857)
            "PPT" -> Color(0xFFB45309)
            else -> Color(0xFF4B5563)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = if (isDark) 0.28f else 0.12f),
                            Color.Black.copy(alpha = if (isDark) 0.08f else 0.03f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onLongClick = onLongPress,
                    onClick = {
                        if (inSelectionMode) {
                            onClick()
                        } else {
                            document.id?.let { fileId ->
                                onOpenFile(fileId, document.name ?: "Document", document.mimeType ?: "application/octet-stream")
                            }
                        }
                    }
                ),
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.2.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                        Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                    )
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                GlassRefraction(modifier = Modifier.matchParentSize(), shape = RoundedCornerShape(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(typeBg)
                            .border(1.dp, if (isDark) badgeColor.copy(alpha = 0.3f) else badgeColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = typeLabel,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = typeColor
                        )
                        
                        if (inSelectionMode) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.background(Color(0xFF8B5CF6), CircleShape)
                                )
                            }
                        }
                    }
     
                    Spacer(modifier = Modifier.width(14.dp))
     
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        HighlightedText(
                            text = document.name ?: "Unknown",
                            query = searchQuery,
                            baseColor = if (isDark) Color.White else Color(0xFF1E1B4B),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(formatDriveDate(document.createdTime?.toString()), fontSize = 12.sp, color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                    }
                    
                    if (inSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF8B5CF6), uncheckedColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF1E1B4B).copy(alpha = 0.5f))
                        )
                    } else {
                        IconButton(
                            onClick = onOptionsClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1E1B4B).copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerCompactDocumentItem() {
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )
    val colors = getThemeColors()
    val isDark = colors.isDark

    val c1 = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Gray.copy(alpha = 0.1f)
    val c2 = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.3f)
    val brush = Brush.linearGradient(
        colors = listOf(c1, c2, c1),
        start = Offset.Zero, end = Offset(x = translateAnim, y = translateAnim)
    )

    Surface(
        modifier = Modifier.fillMaxWidth().height(88.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        shadowElevation = 0.dp, // Avoid shadow bleed
        border = BorderStroke(
            0.5.dp,
            Brush.linearGradient(
                colors = listOf(
                    colors.cardBorder,
                    colors.cardBorderSecondary
                )
            )
        )
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(brush))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.height(20.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(6.dp)).background(brush))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.4f).clip(RoundedCornerShape(6.dp)).background(brush))
            }
        }
    }
}

@Composable
fun UploadOptionCard(title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = getThemeColors()
    val isDark = colors.isDark
    Box(
        modifier = modifier
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Drop shadow Box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = if (isDark) 0.20f else 0.08f),
                            Color.Black.copy(alpha = if (isDark) 0.05f else 0.02f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.2.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                        Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                    )
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                GlassRefraction(shape = RoundedCornerShape(32.dp), modifier = Modifier.matchParentSize())
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = OutfitFontFamily, color = if (isDark) Color.White else Color(0xFF1E1B4B))
                        Text(subtitle, fontSize = 15.sp, color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1E1B4B).copy(alpha = 0.7f), fontWeight = FontWeight.Medium, fontFamily = OutfitFontFamily)
                    }
                }
            }
        }
    }
}

private fun formatDriveDate(dateString: String?): String {
    if (dateString == null) return "Unknown Date"
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date: Date? = parser.parse(dateString)
        if (date != null) {
            val formatter = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
            return formatter.format(date)
        }
    } catch (e: Exception) { e.printStackTrace() }
    return dateString
}



fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    result = cursor.getString(displayNameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    if (result != null && result.contains(".")) {
        result = result.substringBeforeLast(".")
    }
    return result ?: "Document"
}

fun getFileExtensionFromUri(context: Context, uri: Uri): String {
    val mimeType = context.contentResolver.getType(uri)
    var ext = mimeType?.let { android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    if (ext.isNullOrEmpty()) {
        var name = ""
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        name = cursor.getString(displayNameIndex) ?: ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        if (name.isEmpty()) {
            name = uri.path ?: ""
        }
        ext = name.substringAfterLast('.', "")
    }
    return ext
}

@Composable
fun DocOptionItem(
    icon: ImageVector,
    label: String,
    isDark: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else (if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1E1B4B).copy(alpha = 0.9f))
    val bgAlpha = if (isDark) 0.1f else 0.05f
    val iconBgColor = contentColor.copy(alpha = bgAlpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBgColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontFamily = OutfitFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "Unknown size"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}

@Composable
fun PremiumSortToggle(
    text: String,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.2.dp,
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                    Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
            GlassRefraction(shape = RoundedCornerShape(12.dp), modifier = Modifier.matchParentSize())
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1E1B4B).copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    color = if (isDark) Color.White else Color(0xFF1E1B4B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = OutfitFontFamily
                )
            }
        }
    }
}

@Composable
fun FilterTabBar(
    activeFilters: Set<String>,
    onFilterSelected: (String?) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val density = androidx.compose.ui.platform.LocalDensity.current
    val options = listOf("ALL", "PDF", "DOC", "XLS", "IMG")
    
    val selectedIndex = when {
        activeFilters.contains("PDF") -> 1
        activeFilters.contains("DOC") -> 2
        activeFilters.contains("XLS") -> 3
        activeFilters.contains("IMG") -> 4
        else -> 0
    }

    var totalWidthPx by remember { mutableIntStateOf(0) }
    var targetIndex by remember { mutableIntStateOf(selectedIndex) }
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != targetIndex) {
            previousIndex = targetIndex
            targetIndex = selectedIndex
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft drop shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = 5.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = if (isDark) 0.28f else 0.12f),
                            Color.Black.copy(alpha = if (isDark) 0.08f else 0.03f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1A1635).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.2.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                        Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        totalWidthPx = coordinates.size.width
                    }
            ) {
                GlassRefraction(shape = RoundedCornerShape(24.dp), modifier = Modifier.matchParentSize())

                // Sliding fluid droplet indicator
                if (totalWidthPx > 0) {
                    val tabWidthPx = totalWidthPx / 5f
                    
                    val stationaryWidthPx = tabWidthPx - with(density) { 10.dp.toPx() }
                    val heightPx = with(density) { 36.dp.toPx() }
                    
                    val targetCenterX = tabWidthPx * (targetIndex + 0.5f)
                    val targetLeft = targetCenterX - stationaryWidthPx / 2f
                    val targetRight = targetCenterX + stationaryWidthPx / 2f
                    
                    val movingRight = targetIndex > previousIndex
                    
                    val fastSpring = spring<Float>(stiffness = Spring.StiffnessMedium, dampingRatio = 0.65f)
                    val slowSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f)
                    
                    val animatedLeft by animateFloatAsState(
                        targetValue = targetLeft,
                        animationSpec = if (movingRight) slowSpring else fastSpring,
                        label = "filterLeft"
                    )
                    val animatedRight by animateFloatAsState(
                        targetValue = targetRight,
                        animationSpec = if (movingRight) fastSpring else slowSpring,
                        label = "filterRight"
                    )
                    
                    val currentWidth = animatedRight - animatedLeft
                    val widthScale = currentWidth / stationaryWidthPx
                    val heightScale = (1f - 0.25f * (widthScale - 1f).coerceIn(0f, 2f)).coerceIn(0.5f, 1f)
                    val height = heightPx * heightScale
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val left = animatedLeft
                        val right = animatedRight
                        val width = right - left
                        val top = (size.height - height) / 2f
                        val bottom = top + height
                        
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF6366F1).copy(alpha = 0.35f), Color(0xFF8B5CF6).copy(alpha = 0.35f))
                                } else {
                                    listOf(Color(0xFF6366F1).copy(alpha = 0.22f), Color(0xFF8B5CF6).copy(alpha = 0.22f))
                                }
                            ),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(width, height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f, height / 2f)
                        )
                        
                        val path = androidx.compose.ui.graphics.Path().apply {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    left = left,
                                    top = top,
                                    right = right,
                                    bottom = bottom,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f, height / 2f)
                                )
                            )
                        }
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.50f else 0.70f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    options.forEachIndexed { index, option ->
                        val selected = selectedIndex == index
                        val animatedColor by animateColorAsState(
                            if (selected) {
                                if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF1E1B4B).copy(alpha = 0.55f)
                            },
                            label = "filterText"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val filterName = if (option == "ALL") null else option
                                    onFilterSelected(filterName)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = animatedColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomGlassBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = Modifier.zIndex(500f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(tint = Color.Black.copy(alpha = 0.35f), blurRadius = 24.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
        ) + fadeIn(tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(250)
        ) + fadeOut(tween(200)),
        modifier = Modifier.zIndex(501f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = if (isDark) Color(0xFF131024) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                border = BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                            Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                        )
                    )
                ),
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    GlassRefraction(shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), modifier = Modifier.matchParentSize())
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 20.dp)
                                .size(36.dp, 4.dp)
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun CustomGlassDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)),
        exit = fadeOut(tween(300)),
        modifier = Modifier.zIndex(502f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(tint = Color.Black.copy(alpha = 0.35f), blurRadius = 24.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f)
        ) + fadeIn(tween(250)),
        exit = scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(200)
        ) + fadeOut(tween(200)),
        modifier = Modifier.zIndex(503f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f),
                color = if (isDark) Color(0xFF131024) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.45f else 0.60f),
                            Color.Black.copy(alpha = if (isDark) 0.18f else 0.30f)
                        )
                    )
                ),
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    GlassRefraction(shape = RoundedCornerShape(28.dp), modifier = Modifier.matchParentSize())
                    content()
                }
            }
        }
    }
}

