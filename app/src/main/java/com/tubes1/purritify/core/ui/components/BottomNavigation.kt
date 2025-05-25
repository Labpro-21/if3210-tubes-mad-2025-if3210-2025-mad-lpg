package com.tubes1.purritify.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
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
import androidx.navigation.NavController
import com.tubes1.purritify.R
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.core.common.navigation.isLandscape

@Composable
fun BottomNavigation(
    onClick: () -> Unit,
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val items = listOf(Screen.Home, Screen.Library, Screen.SoundCapsule)
    val icons = listOf(
        Icons.Outlined.Home,
        ImageVector.vectorResource(id = R.drawable.library_icon),
        Icons.Outlined.Analytics
    )

    if (isLandscape()) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .width(200.dp), // sidebar width
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items.forEachIndexed { index, screen ->
                    val selected = currentRoute == screen.route

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                if (!selected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                    ) {
                        // Vertical line indicator
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .background(if (selected) Color.White else Color.Transparent)
                        )

                        // Icon and Text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = screen.route,
                                tint = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .height(24.dp).width(24.dp)
                            )

                            Text(
                                text = screen.route.replaceFirstChar { it.uppercase() },
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Portrait (Bottom Navigation)
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
                items.forEachIndexed { index, screen ->
                    val selected = currentRoute == screen.route

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                if (!selected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                    ) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = screen.route,
                            tint = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.height(24.dp).width(24.dp)
                        )
                        Text(
                            text = screen.route.replaceFirstChar { it.uppercase() },
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
