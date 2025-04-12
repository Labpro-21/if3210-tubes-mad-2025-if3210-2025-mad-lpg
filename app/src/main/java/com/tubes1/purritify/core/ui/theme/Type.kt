package com.tubes1.purritify.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tubes1.purritify.core.ui.font.PoppinsFontFamily

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = PoppinsFontFamily.regular,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsFontFamily.bold,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    displayLarge = TextStyle(
        fontFamily = PoppinsFontFamily.bold,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFontFamily.medium,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PoppinsFontFamily.semibold,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFontFamily.regular,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsFontFamily.thin,
        fontWeight = FontWeight.Thin,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsFontFamily.semibold,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PoppinsFontFamily.extrabold,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp
    )
)
