package com.tubes1.purritify.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.tubes1.purritify.R

@Composable
fun BottomNavigation(modifier: Modifier = Modifier, selectedIndex: Int = 0) {
    val navController = rememberNavController()
    val items = listOf("Beranda", "Koleksi Anda", "Profil")
    val icons = listOf(
        Icons.Outlined.Home,
        ImageVector.vectorResource(id = R.drawable.library_icon),
        Icons.Outlined.Person
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = item,
                        modifier = Modifier
                            .height(32.dp)
                            .width(32.dp),
                        tint = if (selected) Color.White else Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = item,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(32.dp)
                                .height(2.dp)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}