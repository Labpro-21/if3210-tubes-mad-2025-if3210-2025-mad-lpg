package com.tubes1.purritify.features.library.presentation.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TopChartItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isImageOnly: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = modifier
                .size(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1DB954), Color(0xFF191414)) // Hijau ke biru gelap
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (!isImageOnly) {
            Text(
                text = "Chart lagu terpopuler harian - $subtitle",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}