package com.example.docmanager.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docmanager.ui.theme.OutfitFontFamily
import com.example.docmanager.util.HapticUtils
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.clip

fun getMonthName(month: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, month - 1)
    }
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}

@Composable
fun CalendarTabContent(
    allDocuments: List<com.google.api.services.drive.model.File>,
    selectedYear: Int?,
    selectedMonth: Int?,
    onYearChange: (Int?) -> Unit,
    onMonthChange: (Int?) -> Unit,
    onDayChange: (Int?) -> Unit
) {
    val colors = getThemeColors()
    
    // Group files by date for highlighting
    val documentsForDate = remember(allDocuments) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val map = mutableMapOf<String, MutableList<com.google.api.services.drive.model.File>>()
        allDocuments.forEach { doc ->
            if (doc.createdTime != null) {
                val dateStr = format.format(Date(doc.createdTime.value))
                if (!map.containsKey(dateStr)) {
                    map[dateStr] = mutableListOf()
                }
                map[dateStr]!!.add(doc)
            }
        }
        map
    }

    // Group files by year/month to display counts in folders
    val filesByYear = remember(allDocuments) {
        val formatYear = SimpleDateFormat("yyyy", Locale.getDefault())
        val map = mutableMapOf<Int, Int>()
        allDocuments.forEach { doc ->
            if (doc.createdTime != null) {
                val year = formatYear.format(Date(doc.createdTime.value)).toIntOrNull()
                if (year != null) {
                    map[year] = (map[year] ?: 0) + 1
                }
            }
        }
        map
    }

    val filesByMonth = remember(allDocuments, selectedYear) {
        val formatYear = SimpleDateFormat("yyyy", Locale.getDefault())
        val formatMonth = SimpleDateFormat("M", Locale.getDefault()) // 1-12
        val map = mutableMapOf<Int, Int>()
        if (selectedYear != null) {
            allDocuments.forEach { doc ->
                if (doc.createdTime != null) {
                    val date = Date(doc.createdTime.value)
                    val year = formatYear.format(date).toIntOrNull()
                    if (year == selectedYear) {
                        val month = formatMonth.format(date).toIntOrNull()
                        if (month != null) {
                            map[month] = (map[month] ?: 0) + 1
                        }
                    }
                }
            }
        }
        map
    }

    data class CalendarState(val year: Int?, val month: Int?)
    val calendarState = remember(selectedYear, selectedMonth) {
        CalendarState(selectedYear, selectedMonth)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = calendarState,
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
            label = "calendar_level_transition",
            modifier = Modifier.fillMaxSize()
        ) { state ->
            if (state.year == null) {
                // Level 1: Years Grid (2026 to 2040)
                val years = (2026..2040).toList()
                val chunkedYears = years.chunked(2)
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Timeline Years",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OutfitFontFamily,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(chunkedYears) { rowYears ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowYears.forEach { year ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val count = filesByYear[year] ?: 0
                                    FolderCard(
                                        title = year.toString(),
                                        subtitle = "$count files",
                                        onClick = { onYearChange(year) }
                                    )
                                }
                            }
                            if (rowYears.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else if (state.month == null) {
                // Level 2: Months Grid (January to December)
                val months = (1..12).toList()
                val chunkedMonths = months.chunked(2)
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "${state.year} Months",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OutfitFontFamily,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(chunkedMonths) { rowMonths ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowMonths.forEach { month ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val count = filesByMonth[month] ?: 0
                                    FolderCard(
                                        title = getMonthName(month),
                                        subtitle = "$count files",
                                        onClick = { onMonthChange(month) }
                                    )
                                }
                            }
                            if (rowMonths.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                // Level 3: Days Calendar Grid
                val days = remember(state.year, state.month) {
                    val list = mutableListOf<Int?>()
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, state.year)
                        set(Calendar.MONTH, state.month - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    repeat(firstDayOfWeek - 1) {
                        list.add(null)
                    }
                    for (d in 1..daysInMonth) {
                        list.add(d)
                    }
                    list
                }
                val chunkedDays = days.chunked(7)
                val weekdays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 140.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        val monthName = getMonthName(state.month)
                        Text(
                            text = "$monthName ${state.year}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OutfitFontFamily,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekdays.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textSecondary,
                                    fontFamily = OutfitFontFamily
                                )
                            }
                        }
                    }

                    items(chunkedDays) { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            week.forEach { d ->
                                if (d == null) {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", state.year, state.month, d)
                                    val uploadedFiles = documentsForDate[dateKey] ?: emptyList()
                                    val hasUploadedFiles = uploadedFiles.isNotEmpty()
                                    DayCell(
                                        day = d,
                                        hasUploadedFiles = hasUploadedFiles,
                                        onClick = {
                                            if (hasUploadedFiles) {
                                                onDayChange(d)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (week.size < 7) {
                                repeat(7 - week.size) {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    hasUploadedFiles: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = getThemeColors()
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.72f),
        label = "day_press_scale"
    )

    val cellBg = if (hasUploadedFiles) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF8B5CF6),
                Color(0xFF6366F1)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colors.cardBackground,
                colors.cardBackground
            )
        )
    }

    val borderStroke = if (hasUploadedFiles) {
        BorderStroke(
            1.5.dp,
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.8f),
                    Color.White.copy(alpha = 0.2f)
                )
            )
        )
    } else {
        BorderStroke(
            0.5.dp,
            Brush.verticalGradient(
                colors = listOf(
                    colors.cardBorder,
                    colors.cardBorderSecondary
                )
            )
        )
    }

    val textColor = if (hasUploadedFiles) {
        Color.White
    } else {
        colors.textPrimary
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(cellBg)
            .border(borderStroke, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!hasUploadedFiles) {
            GlassRefraction(modifier = Modifier.matchParentSize(), shape = RoundedCornerShape(14.dp))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                fontSize = 16.sp,
                fontWeight = if (hasUploadedFiles) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = textColor,
                fontFamily = OutfitFontFamily
            )
            if (hasUploadedFiles) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}
