package com.tubes1.purritify.features.profile.presentation.profiledetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubes1.purritify.R
import com.tubes1.purritify.core.ui.components.BottomNavigation
import com.tubes1.purritify.features.profile.presentation.profiledetail.components.StatItem

@Composable
fun ProfileScreen() {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF005A66),
            Color(0xFF003540),
            Color(0xFF001A20)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dummy_profile),
                    contentDescription = "Gambar Profil",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )

                // edit button
                IconButton(
                    onClick = { /* coming soon */ },
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profil",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // username and location
            Text(
                text = "13522xxx",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "Indonesia",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            // edit profile button
            Button(
                onClick = { /* coming soon */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(120.dp)
                    .height(40.dp)
            ) {
                Text("Edit Profil", color = Color.White)
            }

            // stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 10.dp, end = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                StatItem(count = "135", label = "LAGU", modifier = Modifier.weight(1f))
                StatItem(count = "32", label = "DISUKAI", modifier = Modifier.weight(1f))
                StatItem(count = "50", label = "DIDENGARKAN", modifier = Modifier.weight(1f))
            }
        }
    }
}