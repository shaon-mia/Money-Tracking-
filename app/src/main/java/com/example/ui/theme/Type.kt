package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val HindSiliguriFontFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri, FontWeight.Medium),
    Font(R.font.hind_siliguri, FontWeight.SemiBold),
    Font(R.font.hind_siliguri, FontWeight.Bold)
)

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

fun buildHisabKoriTypography(scaleMultiplier: Float = 1.0f): Typography {
    fun scaled(spVal: Float) = (spVal * scaleMultiplier).sp
    return Typography(
        displaySmall = TextStyle(
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaled(32f),
            lineHeight = scaled(38f)
        ),
        headlineMedium = TextStyle(
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaled(24f),
            lineHeight = scaled(32f)
        ),
        headlineSmall = TextStyle(
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(20f),
            lineHeight = scaled(28f)
        ),
        titleLarge = TextStyle(
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(18f),
            lineHeight = scaled(24f)
        ),
        titleMedium = TextStyle(
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(16f),
            lineHeight = scaled(22f)
        ),
        bodyLarge = TextStyle(
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(15f),
            lineHeight = scaled(22f)
        ),
        bodyMedium = TextStyle(
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(13.5f),
            lineHeight = scaled(20f)
        ),
        labelLarge = TextStyle(
            fontFamily = PlusJakartaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(13.5f),
            lineHeight = scaled(18f)
        ),
        labelSmall = TextStyle(
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(11.5f),
            lineHeight = scaled(16f)
        )
    )
}
