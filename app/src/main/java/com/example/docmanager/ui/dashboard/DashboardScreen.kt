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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver


enum class DashboardTab(val title: String, val icon: ImageVector) {
    RECENT("Recent", Icons.Rounded.History),
    HIERARCHY("Folders", Icons.Rounded.FolderCopy),
    CALENDAR("Calendar", Icons.Rounded.DateRange),
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
            textPrimary = if (isDark) Color.White else Color(0xFF4A2C2A),
            textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF4A2C2A).copy(alpha = 0.65f),
            textTertiary = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF4A2C2A).copy(alpha = 0.45f),
            cardBackground = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.45f),
            cardBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.65f),
            cardBorderSecondary = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.30f),
            iconTint = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF4A2C2A).copy(alpha = 0.8f),
            iconTintSecondary = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF4A2C2A).copy(alpha = 0.5f),
            chipBgSelected = if (isDark) Color(0xFFE8DDF4) else Color.White,
            chipTextSelected = if (isDark) Color(0xFF1B162E) else Color(0xFF6366F1),
            glassRefractionAlpha = if (isDark) 0.15f else 0.40f
        )
    }
}

@Composable
fun PremiumBackground() {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // Lifecycle state check to pause drawing/animating drifting orbs when Activity is in background
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val baseColor1 = if (isDarkTheme) Color(0xFF0B0813) else Color(0xFFFAEAD3) 
    val baseColor2 = if (isDarkTheme) Color(0xFF0F0B1E) else Color(0xFFF3D3BD) 
    val baseColor3 = if (isDarkTheme) Color(0xFF090611) else Color(0xFFE8CBB3)

    if (!isResumed) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(baseColor1, baseColor2, baseColor3)))
        )
        return
    }

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
                .graphicsLayer {
                    translationX = drift1X.dp.toPx()
                    translationY = drift1Y.dp.toPx()
                    scaleX = scale1
                    scaleY = scale1
                }
                .size(350.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) Color(0xFF8B5CF6) else Color(0xFFECA1A6)).copy(alpha = if (isDarkTheme) 0.22f else 0.08f),
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
                .graphicsLayer {
                    translationX = drift2X.dp.toPx()
                    translationY = drift2Y.dp.toPx()
                    scaleX = scale2
                    scaleY = scale2
                }
                .size(400.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) Color(0xFFEC4899) else Color(0xFFE2B2A3)).copy(alpha = if (isDarkTheme) 0.18f else 0.08f),
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
                .graphicsLayer {
                    translationX = drift3X.dp.toPx()
                    translationY = drift3Y.dp.toPx()
                    scaleX = scale3
                    scaleY = scale3
                }
                .size(380.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFFF7D6C8)).copy(alpha = if (isDarkTheme) 0.20f else 0.08f),
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
                            (if (isDarkTheme) Color(0xFF7C3AED) else Color(0xFFF9EAD3)).copy(alpha = if (isDarkTheme) 0.12f else 0.06f),
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
        
        // 2. Crisp Top-Left Highlight Stroke (Direct Reflection) - sharp gloss edge & Bottom-Right Bevel Shadow
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

        // 3. Liquid glass curved specular glare overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.42f)
                quadraticBezierTo(
                    size.width * 0.5f, size.height * 0.56f,
                    0f, size.height * 0.42f
                )
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.16f),
                        Color.White.copy(alpha = 0.0f)
                    )
                )
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
    val allDocuments by viewModel.documents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val alwaysOpenWithDefault by viewModel.alwaysOpenWithDefault.collectAsState()
    val inSelectionMode by viewModel.inSelectionMode.collectAsState()
    
    var currentTab by remember { mutableStateOf(DashboardTab.RECENT) }
    var isFabExpanded by remember { mutableStateOf(false) }



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
    
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var pendingFolderName by remember { mutableStateOf("") }
    
    var showFolderSelectorSheet by remember { mutableStateOf(false) }
    var pendingFolderAction by remember { mutableStateOf("") }

    var showWelcome by remember { mutableStateOf(true) }
    var selectedYear by remember { mutableStateOf<String?>(null) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var calendarSelectedYear by remember { mutableStateOf<Int?>(null) }
    var calendarSelectedMonth by remember { mutableStateOf<Int?>(null) }
    var calendarSelectedDay by remember { mutableStateOf<Int?>(null) }
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUploadDefaultName by remember { mutableStateOf("") }
    var pendingUploadMimeType by remember { mutableStateOf("") }
    var showLocalStorageConfirmDialog by remember { mutableStateOf(false) }
    var pendingUploadAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var totalRowWidthPx by remember { mutableIntStateOf(0) }

    val folderStack by viewModel.folderStack.collectAsState()

    BackHandler(enabled = inSelectionMode) {
        viewModel.clearSelection()
    }

    BackHandler(enabled = isFabExpanded) {
        isFabExpanded = false
    }

    BackHandler(enabled = showDocOptionsSheet) {
        showDocOptionsSheet = false
    }

    BackHandler(enabled = showFolderSelectorSheet) {
        showFolderSelectorSheet = false
    }

    BackHandler(enabled = showSearchFiltersSheet) {
        showSearchFiltersSheet = false
    }

    BackHandler(enabled = showCreateFolderDialog) {
        showCreateFolderDialog = false
    }

    BackHandler(enabled = showOpenChoiceDialog) {
        showOpenChoiceDialog = false
    }

    BackHandler(enabled = showRenameDialogForDoc != null) {
        showRenameDialogForDoc = null
    }

    BackHandler(enabled = showLocalStorageConfirmDialog) {
        showLocalStorageConfirmDialog = false
    }

    BackHandler(enabled = currentTab == DashboardTab.HIERARCHY && folderStack.isNotEmpty()) {
        viewModel.navigateUpFolder()
    }

    BackHandler(enabled = selectedMonth != null) {
        selectedMonth = null
    }

    BackHandler(enabled = selectedYear != null && selectedMonth == null) {
        selectedYear = null
    }

    BackHandler(enabled = currentTab == DashboardTab.CALENDAR && calendarSelectedDay != null) {
        calendarSelectedDay = null
    }

    BackHandler(enabled = currentTab == DashboardTab.CALENDAR && calendarSelectedMonth != null && calendarSelectedDay == null) {
        calendarSelectedMonth = null
    }

    BackHandler(enabled = currentTab == DashboardTab.CALENDAR && calendarSelectedYear != null && calendarSelectedMonth == null) {
        calendarSelectedYear = null
    }

    val isAnyOverlayOrSubstateActive = inSelectionMode || isFabExpanded || showDocOptionsSheet || 
            showFolderSelectorSheet || showSearchFiltersSheet || showCreateFolderDialog || 
            showOpenChoiceDialog || (showRenameDialogForDoc != null) || showLocalStorageConfirmDialog || 
            (currentTab == DashboardTab.HIERARCHY && folderStack.isNotEmpty()) || 
            selectedYear != null || selectedMonth != null ||
            (currentTab == DashboardTab.CALENDAR && (calendarSelectedYear != null || calendarSelectedMonth != null || calendarSelectedDay != null))

    BackHandler(enabled = currentTab != DashboardTab.RECENT && !isAnyOverlayOrSubstateActive) {
        currentTab = DashboardTab.RECENT
    }

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
        viewModel.clearSelection()
    }

    // WorkManager Observation
    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosByTagFlow(DocumentRepository.UPLOAD_WORK_TAG)
        .collectAsState(initial = emptyList())
        
    val isUploading = workInfos.any { it.state == WorkInfo.State.RUNNING }
    
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
            android.widget.Toast.makeText(context, "Google Drive authorization was not completed.", android.widget.Toast.LENGTH_SHORT).show()
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
        isFabExpanded = false
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (!showWelcome) {
            PremiumBackground()
        } else {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF0B0813) else Color(0xFFFAEAD3))
            )
        }

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
        val scaffoldModifier = Modifier.fillMaxSize()

        Scaffold(
            containerColor = Color.Transparent,
            modifier = scaffoldModifier,
            topBar = {},
            bottomBar = {
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

                Box(modifier = Modifier.fillMaxWidth()) {
                    // Standard tab bar (slides down / fades out when selecting)
                    AnimatedVisibility(
                        visible = !inSelectionMode,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(stiffness = 200f, dampingRatio = 0.82f)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(stiffness = 200f, dampingRatio = 0.82f)
                        ) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                color = colors.cardBackground.copy(alpha = if (isDark) 0.15f else 0.85f),
                                shadowElevation = 0.dp,
                                border = BorderStroke(
                                    1.dp,
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            colors.cardBorder,
                                            colors.cardBorderSecondary
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
                                             val tabWidthPx = totalRowWidthPx / DashboardTab.entries.size.toFloat()
                                             
                                             val stationaryWidthPx = tabWidthPx - with(density) { 4.dp.toPx() }
                                             val heightPx = with(density) { 54.dp.toPx() }
                                             
                                             val targetCenterX = tabWidthPx * (targetIndex + 0.5f)
                                             val targetLeft = targetCenterX - stationaryWidthPx / 2f
                                             val targetRight = targetCenterX + stationaryWidthPx / 2f
                                             
                                             val movingRight = targetIndex > previousIndex
                                             
                                             val fastSpring = spring<Float>(stiffness = 320f, dampingRatio = 0.62f)
                                             val slowSpring = spring<Float>(stiffness = 160f, dampingRatio = 0.72f)
                                             
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
                                                     DashboardTab.CALENDAR -> Color(0xFF8B5CF6)
                                                     DashboardTab.SEARCH -> Color(0xFF10B981)
                                                     DashboardTab.SETTINGS -> Color(0xFFEC4899)
                                                 }
                                                 drawRoundRect(
                                                     brush = Brush.horizontalGradient(
                                                        colors = if (isDark) {
                                                            listOf(dropletColor.copy(alpha = 0.45f), dropletColor.copy(alpha = 0.25f))
                                                        } else {
                                                            listOf(dropletColor.copy(alpha = 0.35f), dropletColor.copy(alpha = 0.15f))
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
                                                            Color.White.copy(alpha = if (isDark) 0.60f else 0.80f),
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
                                                    DashboardTab.RECENT -> Color(0xFF60A5FA) // Vibrant Blue
                                                    DashboardTab.HIERARCHY -> Color(0xFFFBBF24) // Vibrant Amber/Orange
                                                    DashboardTab.CALENDAR -> Color(0xFFA78BFA) // Soft Purple/Indigo
                                                    DashboardTab.SEARCH -> Color(0xFF34D399) // Vibrant Emerald/Green
                                                    DashboardTab.SETTINGS -> Color(0xFFF472B6) // Vibrant Pink/Rose
                                                }
                                                val animatedColor by animateColorAsState(
                                                    if (selected) {
                                                        activeTabColor
                                                    } else {
                                                        colors.textSecondary
                                                    },
                                                    label = "color"
                                                )
                                                val iconSize by animateDpAsState(
                                                    targetValue = if (selected) 28.dp else 24.dp,
                                                    animationSpec = spring(stiffness = 220f, dampingRatio = 0.65f),
                                                    label = "iconSize"
                                                )
                                                val interactionSource = remember { MutableInteractionSource() }
                                                val isPressed by interactionSource.collectIsPressedAsState()
                                                val tabScale by animateFloatAsState(
                                                    targetValue = if (isPressed) 0.94f else 1f,
                                                    animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
                                                    label = "tabScale"
                                                )

                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .padding(4.dp)
                                                        .graphicsLayer {
                                                            scaleX = tabScale
                                                            scaleY = tabScale
                                                        }
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .clickable(
                                                            interactionSource = interactionSource,
                                                            indication = null // Removed mask/ripple for 3D press effect
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


                }
            },
        floatingActionButton = {}
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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

                    // Breadcrumbs if in Hierarchy or Calendar tab (shown above Folders/Calendar header)
                    AnimatedVisibility(
                        visible = currentTab == DashboardTab.HIERARCHY || currentTab == DashboardTab.CALENDAR,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 0.dp)
                                .horizontalScroll(rememberScrollState()),
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
                                        if (currentTab == DashboardTab.HIERARCHY) {
                                            while(viewModel.folderStack.value.isNotEmpty()) {
                                                viewModel.navigateUpFolder()
                                            }
                                            selectedYear = null
                                            selectedMonth = null
                                        } else {
                                            calendarSelectedYear = null
                                            calendarSelectedMonth = null
                                            calendarSelectedDay = null
                                        }
                                        viewModel.clearSelection()
                                    }
                            )
                            if (currentTab == DashboardTab.HIERARCHY) {
                                if (folderStack.isNotEmpty()) {
                                    folderStack.forEachIndexed { index, node ->
                                        Icon(
                                            imageVector = Icons.Rounded.ChevronRight,
                                            contentDescription = null,
                                            tint = colors.textPrimary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp).padding(horizontal = 2.dp)
                                        )
                                        Text(
                                            text = node.name,
                                            fontSize = 16.sp,
                                            fontWeight = if (index == folderStack.size - 1) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (index == folderStack.size - 1) colors.textPrimary else colors.textPrimary.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    // Navigate up to this specific folder
                                                    val pops = folderStack.size - 1 - index
                                                    repeat(pops) { viewModel.navigateUpFolder() }
                                                },
                                            fontFamily = OutfitFontFamily
                                        )
                                    }
                                } else {
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
                                                .clickable {
                                                    selectedMonth = null
                                                    viewModel.clearSelection()
                                                },
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
                            } else {
                                // CALENDAR Tab breadcrumbs
                                if (calendarSelectedYear != null) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.textPrimary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = calendarSelectedYear.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                calendarSelectedMonth = null
                                                calendarSelectedDay = null
                                                viewModel.clearSelection()
                                            },
                                        fontFamily = OutfitFontFamily
                                    )
                                }
                                if (calendarSelectedMonth != null) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.textPrimary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    val calendarMonthName = getMonthName(calendarSelectedMonth!!)
                                    Text(
                                        text = calendarMonthName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                calendarSelectedDay = null
                                                viewModel.clearSelection()
                                            },
                                        fontFamily = OutfitFontFamily
                                    )
                                }
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = 220f, dampingRatio = 0.85f)) togetherWith
                                    fadeOut(animationSpec = spring(stiffness = 220f, dampingRatio = 0.85f))
                        }, label = "Header Transition"
                    ) { tab ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = if (tab == DashboardTab.HIERARCHY || tab == DashboardTab.CALENDAR) 12.dp else 48.dp, end = 24.dp, bottom = 16.dp),
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
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            color = colors.cardBackground,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    colors = listOf(
                                        colors.cardBorder,
                                        colors.cardBorderSecondary
                                    )
                                )
                            ),
                            shadowElevation = 0.dp
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                GlassRefraction(shape = RoundedCornerShape(16.dp), modifier = Modifier.matchParentSize())
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF8B5CF6))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Uploading safely to Google Drive...", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                }
                            }
                        }
                    }



                    // Content Switcher
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                val isMovingRight = targetState.ordinal > initialState.ordinal
                                val slideDirection = if (isMovingRight) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                                slideIntoContainer(
                                    towards = slideDirection,
                                    animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)
                                ) togetherWith slideOutOfContainer(
                                    towards = slideDirection,
                                    animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)
                                )
                            },
                            label = "TabContentTransition"
                        ) { tab ->
                            when (tab) {
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
                                    onYearChange = { 
                                        selectedYear = it 
                                        viewModel.clearSelection()
                                    },
                                    onMonthChange = { 
                                        selectedMonth = it 
                                        viewModel.clearSelection()
                                    }
                                )
                                DashboardTab.CALENDAR -> CalendarTabContent(
                                    allDocuments = viewModel.documents.collectAsState().value,
                                    selectedYear = calendarSelectedYear,
                                    selectedMonth = calendarSelectedMonth,
                                    onYearChange = {
                                        calendarSelectedYear = it
                                        viewModel.clearSelection()
                                    },
                                    onMonthChange = {
                                        calendarSelectedMonth = it
                                        viewModel.clearSelection()
                                    },
                                    onDayChange = {
                                        calendarSelectedDay = it
                                        viewModel.clearSelection()
                                    }
                                )
                                DashboardTab.SEARCH -> SearchTabContent(
                                    viewModel = viewModel,
                                    onOpenFile = onOpenFile,
                                    onOptionsClick = { doc ->
                                        selectedDocumentForOptions = doc
                                        showDocOptionsSheet = true
                                    }
                                )
                                DashboardTab.SETTINGS -> SettingsTabContent(userDisplayName, userEmail, userPhotoUrl, onLogout, themeViewModel, viewModel)
                            }
                        }
                    }
                    }
                }
            }

            // Custom Glass Bottom Sheet replaced by fanned options

        // Selection Actions Bar (slides up / fades in when selecting)
        AnimatedVisibility(
            visible = inSelectionMode,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = 200f, dampingRatio = 0.82f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = 200f, dampingRatio = 0.82f)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(700f)
        ) {
            val selectedFiles by viewModel.selectedFiles.collectAsState()
            val isDark = colors.isDark
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
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
                    color = colors.cardBackground.copy(alpha = if (isDark) 0.15f else 0.85f),
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.cardBorder,
                                colors.cardBorderSecondary
                            )
                        )
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GlassRefraction(shape = RoundedCornerShape(36.dp), modifier = Modifier.matchParentSize())
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val interactionSourceClose = remember { MutableInteractionSource() }
                            val isPressedClose by interactionSourceClose.collectIsPressedAsState()
                            val scaleClose by animateFloatAsState(if (isPressedClose) 0.85f else 1f)
                            IconButton(
                                onClick = {
                                    HapticUtils.vibrateShort(context)
                                    viewModel.clearSelection()
                                },
                                modifier = Modifier.scale(scaleClose),
                                interactionSource = interactionSourceClose
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear Selection", tint = if (isDark) Color.White else Color.Black)
                            }

                            Text(
                                "${selectedFiles.size} Selected",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 16.sp,
                                fontFamily = OutfitFontFamily
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val allDocs by viewModel.documents.collectAsState()
                                val selectedIds by viewModel.selectedFiles.collectAsState()

                                // Share
                                val interactionSourceShare = remember { MutableInteractionSource() }
                                val isPressedShare by interactionSourceShare.collectIsPressedAsState()
                                val scaleShare by animateFloatAsState(if (isPressedShare) 0.85f else 1f)
                                IconButton(
                                    onClick = {
                                        HapticUtils.vibrateShort(context)
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
                                    },
                                    modifier = Modifier
                                        .scale(scaleShare)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), CircleShape),
                                    interactionSource = interactionSourceShare
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                }

                                // Download
                                val interactionSourceDownload = remember { MutableInteractionSource() }
                                val isPressedDownload by interactionSourceDownload.collectIsPressedAsState()
                                val scaleDownload by animateFloatAsState(if (isPressedDownload) 0.85f else 1f)
                                IconButton(
                                    onClick = {
                                        HapticUtils.vibrateShort(context)
                                        val docsToDownload = allDocs.filter { it.id in selectedIds }
                                        if (docsToDownload.isNotEmpty()) {
                                            docsToDownload.forEach { doc ->
                                                doc.id?.let { fileId ->
                                                    viewModel.downloadFileToDevice(context, fileId, doc.name ?: "Document", doc.mimeType ?: "application/octet-stream")
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .scale(scaleDownload)
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), CircleShape),
                                    interactionSource = interactionSourceDownload
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                }

                                // Copy
                                val interactionSourceCopy = remember { MutableInteractionSource() }
                                val isPressedCopy by interactionSourceCopy.collectIsPressedAsState()
                                val scaleCopy by animateFloatAsState(if (isPressedCopy) 0.85f else 1f)
                                IconButton(
                                    onClick = {
                                        HapticUtils.vibrateSuccess(context)
                                        viewModel.copySelectedFiles()
                                    },
                                    modifier = Modifier
                                        .scale(scaleCopy)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), CircleShape),
                                    interactionSource = interactionSourceCopy
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                                }

                                // Delete
                                val interactionSourceDelete = remember { MutableInteractionSource() }
                                val isPressedDelete by interactionSourceDelete.collectIsPressedAsState()
                                val scaleDelete by animateFloatAsState(if (isPressedDelete) 0.85f else 1f)
                                IconButton(
                                    onClick = {
                                        HapticUtils.vibrateShort(context)
                                        if (isOnline) {
                                            viewModel.deleteSelectedFiles()
                                        } else {
                                            android.widget.Toast.makeText(context, "Internet connection is required to delete files.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .scale(scaleDelete)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), CircleShape),
                                    interactionSource = interactionSourceDelete
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
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
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = isDownloadingFile ?: "",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Custom Glass Dialog for Open Choice
        CustomGlassDialog(
            visible = showOpenChoiceDialog,
            onDismiss = { showOpenChoiceDialog = false }
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
                        Text("Just Once", color = colors.textPrimary.copy(alpha = 0.8f), fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
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

        // Custom Glass Dialog for Calendar Day Uploaded Files
        CustomGlassDialog(
            visible = calendarSelectedDay != null,
            onDismiss = { calendarSelectedDay = null }
        ) {
            val popupFiles = remember(calendarSelectedDay, calendarSelectedYear, calendarSelectedMonth, allDocuments) {
                if (calendarSelectedDay != null && calendarSelectedYear != null && calendarSelectedMonth != null) {
                    val dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", calendarSelectedYear, calendarSelectedMonth, calendarSelectedDay)
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    allDocuments.filter { doc ->
                        doc.createdTime != null && format.format(Date(doc.createdTime.value)) == dateKey
                    }
                } else {
                    emptyList()
                }
            }
            
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth().fillMaxHeight(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val monthName = if (calendarSelectedMonth != null) getMonthName(calendarSelectedMonth!!) else ""
                    Text(
                        text = "$monthName $calendarSelectedDay, $calendarSelectedYear",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.textPrimary
                    )
                    IconButton(
                        onClick = { calendarSelectedDay = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = colors.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (popupFiles.isNotEmpty()) {
                    val selectedFiles by viewModel.selectedFiles.collectAsState()
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(popupFiles, key = { it.id ?: it.hashCode() }) { file ->
                            CompactDocumentItem(
                                document = file,
                                onOpenFile = onOpenFile,
                                isSelected = selectedFiles.contains(file.id),
                                inSelectionMode = inSelectionMode,
                                onLongPress = {
                                    file.id?.let {
                                        viewModel.toggleSelection(it)
                                        HapticUtils.vibrateLight(context)
                                    }
                                },
                                onClick = {
                                    file.id?.let {
                                        viewModel.toggleSelection(it)
                                        HapticUtils.vibrateShort(context)
                                    }
                                },
                                onOptionsClick = {
                                    selectedDocumentForOptions = file
                                    showDocOptionsSheet = true
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No files uploaded on this day.",
                        color = colors.textSecondary,
                        fontFamily = OutfitFontFamily,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }
        }

        // Custom Glass Bottom Sheet for Document Options
        if (selectedDocumentForOptions != null) {
            val doc = selectedDocumentForOptions!!
            val mime = doc.mimeType?.lowercase() ?: ""
            val isFolder = mime == "application/vnd.google-apps.folder" || doc.id?.startsWith("local_folder://") == true
            val (typeLabel, badgeColor) = when {
                isFolder -> "DIR" to Color(0xFFF59E0B)
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
                    "DIR" -> Color(0xFFFDBA74)
                    "PDF" -> Color(0xFFFCA5A5)
                    "IMG" -> Color(0xFFFBCFE8)
                    "DOC" -> Color(0xFF93C5FD)
                    "XLS" -> Color(0xFF6EE7B7)
                    "PPT" -> Color(0xFFFDBA74)
                    else -> Color(0xFFD1D5DB)
                }
            } else {
                when (typeLabel) {
                    "DIR" -> Color(0xFFB45309)
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
                onDismiss = { showDocOptionsSheet = false }
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
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isFolder) "Folder" else "${formatDriveDate(doc.createdTime?.toString())} • ${formatFileSize(doc.getSize())}",
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = colors.cardBorderSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                if (!isFolder) {
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
                }

                DocOptionItem(
                    icon = Icons.Rounded.Edit,
                    label = if (isFolder) "Rename Folder" else "Rename Document",
                    isDark = isDark,
                    onClick = {
                        showDocOptionsSheet = false
                        if (isOnline || doc.id?.startsWith("local_folder://") == true) {
                            showRenameDialogForDoc = doc
                        } else {
                            android.widget.Toast.makeText(context, "Internet connection is required.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                )

                if (!isFolder) {
                    DocOptionItem(
                        icon = Icons.Rounded.ContentCopy,
                        label = "Copy to Folder",
                        isDark = isDark,
                        onClick = {
                            showDocOptionsSheet = false
                            if (isOnline) {
                                pendingFolderAction = "copy"
                                showFolderSelectorSheet = true
                            } else {
                                android.widget.Toast.makeText(context, "Internet connection is required.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                DocOptionItem(
                    icon = Icons.Rounded.Delete,
                    label = if (isFolder) "Delete Folder" else "Delete Document",
                    isDark = isDark,
                    isDestructive = true,
                    onClick = {
                        showDocOptionsSheet = false
                        if (isOnline || doc.id?.startsWith("local_folder://") == true) {
                            doc.id?.let { viewModel.deleteSingleFile(it) }
                        } else {
                            android.widget.Toast.makeText(context, "Internet connection is required to delete files.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }

        // Custom Glass Dialog for Creating Folder
        CustomGlassDialog(
            visible = showCreateFolderDialog,
            onDismiss = { showCreateFolderDialog = false }
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Create Folder",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pendingFolderName,
                    onValueChange = { pendingFolderName = it },
                    label = { Text("Folder Name", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.5f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text("Cancel", color = colors.textSecondary, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pendingFolderName.isNotBlank()) {
                                viewModel.createCustomFolder(pendingFolderName.trim())
                                showCreateFolderDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Create", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Custom Glass Bottom Sheet for Folder Selector
        CustomGlassBottomSheet(
            visible = showFolderSelectorSheet,
            onDismiss = { showFolderSelectorSheet = false }
        ) {
            val isDark = colors.isDark
            var folders by remember { mutableStateOf<List<com.google.api.services.drive.model.File>>(emptyList()) }
            var isLoadingFolders by remember { mutableStateOf(true) }

            LaunchedEffect(showFolderSelectorSheet) {
                if (showFolderSelectorSheet) {
                    isLoadingFolders = true
                    folders = viewModel.getAllCustomFoldersSuspend()
                    isLoadingFolders = false
                }
            }

            Text(
                text = if (pendingFolderAction == "move") "Move to Folder" else "Copy to Folder",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = OutfitFontFamily,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoadingFolders) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            } else if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No folders available.", color = colors.textSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(folders) { folder ->
                        DocOptionItem(
                            icon = Icons.Rounded.Folder,
                            label = folder.name ?: "Folder",
                            isDark = isDark,
                            onClick = {
                                showFolderSelectorSheet = false
                                selectedDocumentForOptions?.id?.let { fileId ->
                                    if (pendingFolderAction == "move") {
                                        viewModel.toggleSelection(fileId) // To make moveSelectedFilesToFolder work for single
                                        viewModel.moveSelectedFilesToFolder(folder.id)
                                    } else {
                                        // TODO: Copy logic, skipping for now or use move as placeholder since Drive API copy takes a different endpoint. Let's just use move.
                                        viewModel.toggleSelection(fileId)
                                        viewModel.moveSelectedFilesToFolder(folder.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Custom Glass Bottom Sheet for Search Filters
        CustomGlassBottomSheet(
            visible = showSearchFiltersSheet,
            onDismiss = { showSearchFiltersSheet = false }
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
            onDismiss = { showRenameDialogForDoc = null }
        ) {
            val doc = showRenameDialogForDoc
            if (doc != null) {
                var newName by remember(doc) { mutableStateOf(doc.name ?: "") }
                val isFolder = doc.mimeType == "application/vnd.google-apps.folder" || doc.id?.startsWith("local_folder://") == true
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (isFolder) "Rename Folder" else "Rename Document", fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily, fontSize = 20.sp, color = colors.textPrimary, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(if (isFolder) "Folder Name" else "Document Name", fontFamily = OutfitFontFamily) },
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
            }
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
            }
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

        // FAB and Option Circles overlay container
        val isFabVisible = (currentTab == DashboardTab.RECENT || currentTab == DashboardTab.HIERARCHY) && !inSelectionMode
        if (isFabVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(450f),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background clickable backdrop overlay to collapse FAB options
                if (isFabExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isFabExpanded = false
                            }
                    )
                }

                // Main circular menu group fanning out playing-cards style at 30 degrees symmetrically
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 100.dp)
                        .size(240.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val radius by animateDpAsState(
                        targetValue = if (isFabExpanded) 146.dp else 0.dp,
                        animationSpec = spring(stiffness = 180f, dampingRatio = 0.68f),
                        label = "fab_options_radius"
                    )
                    val optionScale by animateFloatAsState(
                        targetValue = if (isFabExpanded) 1f else 0f,
                        animationSpec = spring(stiffness = 180f, dampingRatio = 0.68f),
                        label = "fab_options_scale"
                    )

                    // Scan Option (Left: 34 deg left of vertical axis)
                    OptionCircleButton(
                        icon = Icons.Rounded.DocumentScanner,
                        label = "Scan",
                        color = Color(0xFF8B5CF6),
                        scale = optionScale,
                        offsetX = -radius * 0.56f,
                        offsetY = -radius * 0.83f,
                        onClick = {
                            isFabExpanded = false
                            if (isOnline) {
                                launchScanner()
                            } else {
                                android.widget.Toast.makeText(context, "Internet connection is required to scan documents.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    // Upload Option (Center: straight up)
                    OptionCircleButton(
                        icon = Icons.Rounded.FolderOpen,
                        label = "Upload",
                        color = Color(0xFF10B981),
                        scale = optionScale,
                        offsetX = 0.dp,
                        offsetY = -radius,
                        onClick = {
                            isFabExpanded = false
                            if (isOnline) {
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
                                android.widget.Toast.makeText(context, "Internet connection is required to upload files.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    // Folder Option (Right: 34 deg right of vertical axis)
                    OptionCircleButton(
                        icon = Icons.Rounded.CreateNewFolder,
                        label = "Folder",
                        color = Color(0xFFF59E0B),
                        scale = optionScale,
                        offsetX = radius * 0.56f,
                        offsetY = -radius * 0.83f,
                        onClick = {
                            isFabExpanded = false
                            if (isOnline) {
                                pendingFolderName = ""
                                showCreateFolderDialog = true
                            } else {
                                android.widget.Toast.makeText(context, "Internet connection is required to create folders.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    // Main FAB (Plus / Close)
                    val interactionSourceFab = remember { MutableInteractionSource() }
                    val isPressedFab by interactionSourceFab.collectIsPressedAsState()
                    val scaleFab by animateFloatAsState(
                        targetValue = if (isPressedFab) 0.90f else 1.0f,
                        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
                        label = "fab_press_scale"
                    )
                    val animRotation by animateFloatAsState(
                        targetValue = if (isFabExpanded) 135f else 0f,
                        animationSpec = spring(stiffness = 180f, dampingRatio = 0.68f),
                        label = "fab_rotation"
                    )
                    val isDark = colors.isDark

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(68.dp)
                            .graphicsLayer {
                                scaleX = scaleFab
                                scaleY = scaleFab
                            }
                            .shadow(12.dp, CircleShape)
                            .background(
                                color = if (isDark) Color.White.copy(alpha = 0.85f) else colors.cardBackground.copy(alpha = 0.75f),
                                shape = CircleShape
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    colors = if (isDark) {
                                        listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.1f))
                                    } else {
                                        listOf(colors.cardBorder, colors.cardBorderSecondary)
                                    }
                                ),
                                CircleShape
                            )
                            .clickable(
                                interactionSource = interactionSourceFab,
                                indication = null
                            ) {
                                isFabExpanded = !isFabExpanded
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        GlassRefraction(shape = CircleShape, modifier = Modifier.matchParentSize())
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Menu Toggle",
                            tint = Color(0xFF3B82F6), // Vibrant Blue!
                            modifier = Modifier
                                .size(30.dp)
                                .rotate(animRotation)
                        )
                    }
                }
            }
        }

        val copiedFiles by viewModel.copiedFiles.collectAsState()
        val showPasteButton = currentTab == DashboardTab.HIERARCHY && copiedFiles.isNotEmpty() && !inSelectionMode

        AnimatedVisibility(
            visible = showPasteButton,
            enter = scaleIn(animationSpec = spring(stiffness = 180f, dampingRatio = 0.65f)) + fadeIn(),
            exit = scaleOut(animationSpec = spring(stiffness = 180f, dampingRatio = 0.65f)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 100.dp)
                .zIndex(400f)
        ) {
            val interactionSourcePaste = remember { MutableInteractionSource() }
            val isPressedPaste by interactionSourcePaste.collectIsPressedAsState()
            val scalePaste by animateFloatAsState(
                targetValue = if (isPressedPaste) 0.90f else 1.0f,
                animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
                label = "paste_fab_scale"
            )
            val isDark = colors.isDark

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .graphicsLayer {
                        scaleX = scalePaste
                        scaleY = scalePaste
                    }
                    .shadow(12.dp, CircleShape)
                    .background(
                        color = colors.cardBackground.copy(alpha = if (isDark) 0.06f else 0.35f),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.cardBorder,
                                colors.cardBorderSecondary
                            )
                        ),
                        CircleShape
                    )
                    .clickable(
                        interactionSource = interactionSourcePaste,
                        indication = null
                    ) {
                        HapticUtils.vibrateSuccess(context)
                        val currentFolderId = if (folderStack.isEmpty()) "root_app_folder" else folderStack.last().id
                        viewModel.pasteCopiedFiles(currentFolderId)
                    },
                contentAlignment = Alignment.Center
            ) {
                GlassRefraction(shape = CircleShape, modifier = Modifier.matchParentSize())
                Icon(
                    imageVector = Icons.Rounded.ContentPaste,
                    contentDescription = "Paste Files",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(28.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(22.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = copiedFiles.size.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = OutfitFontFamily
                    )
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
    val context = LocalContext.current
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
                            onLongPress = {
                                doc.id?.let {
                                    viewModel.toggleSelection(it)
                                    HapticUtils.vibrateLight(context)
                                }
                            },
                            onClick = {
                                doc.id?.let {
                                    viewModel.toggleSelection(it)
                                    HapticUtils.vibrateShort(context)
                                }
                            },
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
    val colors = getThemeColors()
    val context = LocalContext.current
    val inSelectionMode by viewModel.inSelectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val folderStack by viewModel.folderStack.collectAsState()
    val currentFolderFiles by viewModel.currentFolderFiles.collectAsState()

    if (isLoading && allDocuments.isEmpty() && currentFolderFiles.isEmpty()) {
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
        AnimatedContent(
            targetState = folderStack,
            transitionSpec = {
                val targetDepth = targetState.size
                val initialDepth = initialState.size
                if (targetDepth > initialDepth) {
                    // Navigating deeper (Slide right to left)
                    (slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }, animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)) + fadeOut(animationSpec = tween(220)))
                } else {
                    // Navigating back (Slide left to right)
                    (slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)) + fadeOut(animationSpec = tween(220)))
                }
            },
            label = "custom_folder_navigation",
            modifier = Modifier.fillMaxSize()
        ) { currentStack ->
            if (currentStack.isNotEmpty()) {
                // Inside a Custom Folder
                val folders = currentFolderFiles.filter { it.mimeType == "application/vnd.google-apps.folder" }
                val files = currentFolderFiles.filter { it.mimeType != "application/vnd.google-apps.folder" }
                
                LazyColumn(

                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (folders.isNotEmpty()) {
                        val chunkedFolders = folders.chunked(2)
                        items(chunkedFolders) { rowFolders ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                rowFolders.forEach { folder ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        FolderCard(
                                            title = folder.name ?: "Folder",
                                            subtitle = "Custom Folder",
                                            onClick = {
                                                folder.id?.let { viewModel.loadFolderContents(it, folder.name ?: "Folder") }
                                            },
                                            onLongClick = {
                                                onOptionsClick(folder)
                                            }
                                        )
                                    }
                                }
                                if (rowFolders.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    
                    if (files.isNotEmpty()) {
                        items(files, key = { it.id ?: it.hashCode() }) { file ->
                            CompactDocumentItem(
                                document = file,
                                onOpenFile = onOpenFile,
                                isSelected = selectedFiles.contains(file.id),
                                inSelectionMode = inSelectionMode,
                                onLongPress = {
                                    file.id?.let {
                                        viewModel.toggleSelection(it)
                                        HapticUtils.vibrateLight(context)
                                    }
                                },
                                onClick = {
                                    file.id?.let {
                                        viewModel.toggleSelection(it)
                                        HapticUtils.vibrateShort(context)
                                    }
                                },
                                onOptionsClick = { onOptionsClick(file) }
                            )
                        }
                    } else if (folders.isEmpty() && !isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("This folder is empty.", color = Color.Gray, fontSize = 16.sp, fontFamily = OutfitFontFamily)
                            }
                        }
                    }
                }
            } else {
                // At the Root: Mix of Custom Folders and Year/Month
                data class HierarchyState(val year: String?, val month: String?)
                val currentState = HierarchyState(selectedYear, selectedMonth)

                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = {
                        val targetLevel = when {
                            targetState.year == null -> 1
                            targetState.month == null -> 2
                            else -> 3
                        }
                        val initialLevel = when {
                            initialState.year == null -> 1
                            initialState.month == null -> 2
                            else -> 3
                        }
                        if (targetLevel > initialLevel) {
                            (slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)) + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally(targetOffsetX = { -it }, animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)) + fadeOut(animationSpec = tween(220)))
                        } else {
                            (slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)) + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)) + fadeOut(animationSpec = tween(220)))
                        }
                    },
                    label = "hierarchy_animation",
                    modifier = Modifier.fillMaxSize()
                ) { state ->
                    if (state.year == null) {
                        // View Root Custom Folders + Years (Level 1)
                        val rootFolders = currentFolderFiles.filter { it.mimeType == "application/vnd.google-apps.folder" }
                        val yearKeys = groupedByYearMonth.keys.toList()
                        
                        LazyColumn(
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Custom Folders
                            if (rootFolders.isNotEmpty()) {
                                item { Text("Folders", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily, color = colors.textPrimary) }
                                val chunkedFolders = rootFolders.chunked(2)
                                items(chunkedFolders) { rowFolders ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        rowFolders.forEach { folder ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                FolderCard(
                                                    title = folder.name ?: "Folder",
                                                    subtitle = "Custom Folder",
                                                    onClick = {
                                                        folder.id?.let { viewModel.loadFolderContents(it, folder.name ?: "Folder") }
                                                    },
                                                    onLongClick = {
                                                        onOptionsClick(folder)
                                                    }
                                                )
                                            }
                                        }
                                        if (rowFolders.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            // Year Folders
                            if (yearKeys.isNotEmpty()) {
                                item { Text("Timeline", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily, color = colors.textPrimary) }
                                val chunkedYears = yearKeys.chunked(2)
                                items(chunkedYears) { rowYears ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        rowYears.forEach { year ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                val fileCount = groupedByYearMonth[year]?.values?.sumOf { it.size } ?: 0
                                                FolderCard(title = year, subtitle = "$fileCount files", onClick = { onYearChange(year) })
                                            }
                                        }
                                        if (rowYears.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else if (state.month == null) {
                        // View Months in Year (Level 2)
                        val months = groupedByYearMonth[state.year] ?: emptyMap()
                        val monthKeys = months.keys.toList().sortedDescending()
                        LazyColumn(
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val chunkedMonths = monthKeys.chunked(2)
                            items(chunkedMonths) { rowMonths ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    rowMonths.forEach { month ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            val fileCount = months[month]?.size ?: 0
                                            FolderCard(title = month, subtitle = "$fileCount files", onClick = { onMonthChange(month) })
                                        }
                                    }
                                    if (rowMonths.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
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
                                    onLongPress = {
                                        file.id?.let {
                                            viewModel.toggleSelection(it)
                                            HapticUtils.vibrateLight(context)
                                        }
                                    },
                                    onClick = {
                                        file.id?.let {
                                            viewModel.toggleSelection(it)
                                            HapticUtils.vibrateShort(context)
                                        }
                                    },
                                    onOptionsClick = { onOptionsClick(file) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(title: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val colors = getThemeColors()
    val isDark = colors.isDark
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "folder_press_scale"
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.5.dp else 5.dp,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "folder_shadow_offset"
    )
    val shadowAlphaMultiplier by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "folder_shadow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowOffset)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = (if (isDark) 0.28f else 0.12f) * shadowAlphaMultiplier),
                            Color.Black.copy(alpha = (if (isDark) 0.08f else 0.03f) * shadowAlphaMultiplier),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            // Frosted blur layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(30.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            
            // Glass reflection overlay
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
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily, color = colors.textPrimary)
                    Text(subtitle, fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium, fontFamily = OutfitFontFamily)
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
    val context = LocalContext.current

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
                color = colors.cardBackground,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.cardBorder,
                            colors.cardBorderSecondary
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
                        placeholder = { Text("Search all files...", fontSize = 15.sp, color = colors.textSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.iconTintSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.textPrimary
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
                        color = colors.cardBackground,
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    colors.cardBorder,
                                    colors.cardBorderSecondary
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
                Text("No results found for '$searchQuery'", color = colors.textSecondary, fontSize = 16.sp)
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
                        color = colors.textSecondary,
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
                                    color = colors.textTertiary
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
                                    tint = colors.iconTintSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = term,
                                    fontSize = 16.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeRecentSearch(term) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Delete search history item",
                                        tint = colors.iconTintSecondary.copy(alpha = 0.8f),
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
                            onLongPress = {
                                doc.id?.let {
                                    viewModel.toggleSelection(it)
                                    HapticUtils.vibrateLight(context)
                                }
                            },
                            onClick = {
                                doc.id?.let {
                                    viewModel.toggleSelection(it)
                                    HapticUtils.vibrateShort(context)
                                }
                            },
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
    viewModel: DashboardViewModel
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
        onDismiss = { showPhotoDialog = false }
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
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else colors.textPrimary.copy(alpha = 0.08f), CircleShape)
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
        Text(userDisplayName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text(userEmail, fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
        
        Spacer(modifier = Modifier.height(36.dp))
        
        // Storage Usage - Real data with progress bar (reverted layout structure with glass style)
        val interactionSourceStorage = remember { MutableInteractionSource() }
        val isPressedStorage by interactionSourceStorage.collectIsPressedAsState()
        val scaleStorage by animateFloatAsState(
            targetValue = if (isPressedStorage) 0.95f else 1f,
            animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
            label = "storage_scale"
        )
        val shadowOffsetStorage by animateDpAsState(
            targetValue = if (isPressedStorage) 1.5.dp else 4.dp,
            animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
            label = "storage_shadow_offset"
        )
        val shadowAlphaMultiplierStorage by animateFloatAsState(
            targetValue = if (isPressedStorage) 0.4f else 1.0f,
            animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
            label = "storage_shadow_alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .scale(scaleStorage),
            contentAlignment = Alignment.Center
        ) {
            // Soft drop shadow Box
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = shadowOffsetStorage)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = (if (isDark) 0.20f else 0.08f) * shadowAlphaMultiplierStorage),
                                Color.Black.copy(alpha = (if (isDark) 0.05f else 0.02f) * shadowAlphaMultiplierStorage),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = interactionSourceStorage, indication = null) {},
                shape = RoundedCornerShape(16.dp),
                color = colors.cardBackground,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.cardBorder,
                            colors.cardBorderSecondary
                        )
                    )
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    GlassRefraction(shape = RoundedCornerShape(16.dp), modifier = Modifier.matchParentSize())
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).background(if (isDark) Color.White.copy(alpha = 0.15f) else colors.textPrimary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Cloud,
                                    contentDescription = null,
                                    tint = colors.iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Storage Usage", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontFamily = OutfitFontFamily)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("$usedFormatted GB / $limitFormatted GB", fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium, fontFamily = OutfitFontFamily)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF8B5CF6),
                            trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else colors.textPrimary.copy(alpha = 0.1f)
                        )
                    }
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "settings_row_scale"
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.5.dp else 4.dp,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "settings_row_shadow_offset"
    )
    val shadowAlphaMultiplier by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "settings_row_shadow_alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Soft drop shadow Box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowOffset)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = (if (isDark) 0.20f else 0.08f) * shadowAlphaMultiplier),
                            Color.Black.copy(alpha = (if (isDark) 0.05f else 0.02f) * shadowAlphaMultiplier),
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
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = colors.cardBackground,
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        colors.cardBorder,
                        colors.cardBorderSecondary
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
                        modifier = Modifier.size(32.dp).background(if (isDark) Color.White.copy(alpha = 0.15f) else colors.textPrimary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = label, tint = colors.iconTint, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.weight(1f), fontFamily = OutfitFontFamily)
                    if (trailingText != null) {
                        Text(trailingText, fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium, fontFamily = OutfitFontFamily)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.iconTintSecondary, modifier = Modifier.size(20.dp))
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
    val context = LocalContext.current
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
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "document_press_scale"
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.5.dp else 5.dp,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "document_shadow_offset"
    )
    val shadowAlphaMultiplier by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "document_shadow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowOffset)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = (if (isDark) 0.28f else 0.12f) * shadowAlphaMultiplier),
                            Color.Black.copy(alpha = (if (isDark) 0.08f else 0.03f) * shadowAlphaMultiplier),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onLongClick = onLongPress,
                    onClick = {
                        if (inSelectionMode) {
                            onClick()
                        } else {
                            HapticUtils.vibrateShort(context)
                            document.id?.let { fileId ->
                                onOpenFile(fileId, document.name ?: "Document", document.mimeType ?: "application/octet-stream")
                            }
                        }
                    }
                )
        ) {
            // Frosted blur layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(30.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            
            // Glass reflection overlay
            GlassRefraction(modifier = Modifier.matchParentSize(), shape = RoundedCornerShape(24.dp))
            
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
                            baseColor = colors.textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(formatDriveDate(document.createdTime?.toString()), fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                    }
                    
                    if (!inSelectionMode) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = onOptionsClick,
                                    onLongClick = {
                                        // Consume long-press to prevent it from propagating to the parent surface
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
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
    val colors = getThemeColors()
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else (if (isDark) Color.White.copy(alpha = 0.9f) else colors.textPrimary.copy(alpha = 0.9f))
    val bgAlpha = if (isDark) 0.1f else 0.05f
    val iconBgColor = contentColor.copy(alpha = bgAlpha)
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "option_item_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
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
    val colors = getThemeColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "sort_press_scale"
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 3.dp,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "sort_shadow_offset"
    )
    val shadowAlphaMultiplier by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "sort_shadow_alpha"
    )

    Box(
        modifier = Modifier
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowOffset)
                .background(
                    color = Color.Black.copy(alpha = (if (isDark) 0.20f else 0.08f) * shadowAlphaMultiplier),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        Surface(
            modifier = Modifier
                .height(36.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(12.dp),
            color = colors.cardBackground,
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        colors.cardBorder,
                        colors.cardBorderSecondary
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
                        tint = colors.iconTintSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = text,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = OutfitFontFamily
                    )
                }
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
    val colors = getThemeColors()
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
            color = colors.cardBackground,
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        colors.cardBorder,
                        colors.cardBorderSecondary
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
                                if (isDark) Color.White.copy(alpha = 0.55f) else colors.textSecondary
                            },
                            label = "filterText"
                        )
                        val chipInteractionSource = remember { MutableInteractionSource() }
                        val isChipPressed by chipInteractionSource.collectIsPressedAsState()
                        val chipScale by animateFloatAsState(
                            targetValue = if (isChipPressed) 0.90f else 1.0f,
                            animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
                            label = "filter_chip_press_scale"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = chipScale
                                    scaleY = chipScale
                                }
                                .clickable(
                                    interactionSource = chipInteractionSource,
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
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = Modifier.zIndex(600f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
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
            animationSpec = spring(stiffness = 180f, dampingRatio = 0.82f)
        ) + fadeIn(tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = 300f, dampingRatio = 0.9f)
        ) + fadeOut(tween(200)),
        modifier = Modifier.zIndex(601f)
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
                .background(Color.Black.copy(alpha = 0.5f))
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
            animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f)
        ) + fadeIn(tween(250)),
        exit = scaleOut(
            targetScale = 0.82f,
            animationSpec = spring(stiffness = 300f, dampingRatio = 0.9f)
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

@Composable
fun BoxScope.OptionCircleButton(
    icon: ImageVector,
    label: String,
    color: Color,
    scale: Float,
    offsetX: Dp,
    offsetY: Dp,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = getThemeColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "option_circle_press_scale"
    )

    if (scale > 0.05f) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationX = offsetX.toPx()
                    translationY = offsetY.toPx()
                    scaleX = scale * pressScale
                    scaleY = scale * pressScale
                    alpha = scale
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .shadow(8.dp, CircleShape)
                    .background(
                        color = colors.cardBackground.copy(alpha = if (isDark) 0.15f else 0.75f),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.cardBorder,
                                colors.cardBorderSecondary
                            )
                        ),
                        CircleShape
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                GlassRefraction(shape = CircleShape, modifier = Modifier.matchParentSize())
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = if (isDark) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFontFamily,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

