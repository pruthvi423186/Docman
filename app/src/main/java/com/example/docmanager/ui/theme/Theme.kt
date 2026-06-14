package com.example.docmanager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import com.example.docmanager.R

val Indigo500 = Color(0xFF6366F1)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo800 = Color(0xFF3730A3)

val Emerald500 = Color(0xFF10B981)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald800 = Color(0xFF065F46)

val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

val Rose500 = Color(0xFFF43F5E)
val Rose100 = Color(0xFFFFE4E6)
val Rose800 = Color(0xFF9F1239)

val Amber500 = Color(0xFFF59E0B)

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val OutfitFont = GoogleFont("Inter")

val OutfitFontFamily = FontFamily(
    Font(googleFont = OutfitFont, fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = OutfitFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = OutfitFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = OutfitFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = OutfitFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = OutfitFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = OutfitFontFamily),
    displayMedium = TextStyle(fontFamily = OutfitFontFamily),
    displaySmall = TextStyle(fontFamily = OutfitFontFamily),
    headlineLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal)
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo800,
    
    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald100,
    onSecondaryContainer = Emerald800,
    
    error = Rose500,
    onError = Color.White,
    errorContainer = Rose100,
    onErrorContainer = Rose800,
    
    background = Color(0xFFFAEAD3),
    onBackground = Color(0xFF4A2C2A),
    surface = Color.White,
    onSurface = Color(0xFF4A2C2A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF1E293B),
    
    tertiary = Amber500,
    onTertiary = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF312E81),
    primaryContainer = Color(0xFF4338CA),
    onPrimaryContainer = Color(0xFFE0E7FF),
    
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF047857),
    onSecondaryContainer = Color(0xFFD1FAE5),
    
    error = Color(0xFFFB7185),
    onError = Color(0xFF881337),
    errorContainer = Color(0xFFBE123C),
    onErrorContainer = Color(0xFFFFE4E6),
    
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Slate100,
    
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF78350F)
)

@Composable
fun DocManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
