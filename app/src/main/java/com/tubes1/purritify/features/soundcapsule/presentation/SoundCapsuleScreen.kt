package com.tubes1.purritify.features.soundcapsule.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.tubes1.purritify.R 
import com.tubes1.purritify.features.soundcapsule.domain.model.TopArtist
import com.tubes1.purritify.features.soundcapsule.domain.model.TopSong
import com.tubes1.purritify.features.soundcapsule.domain.model.DayStreak
import com.tubes1.purritify.features.soundcapsule.domain.usecase.ExportAnalyticsUseCase
import com.tubes1.purritify.features.soundcapsule.domain.usecase.ExportFormat
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundCapsuleScreen(
    navController: NavController,
    viewModel: SoundCapsuleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            
            
        }
    }

    LaunchedEffect(Unit) { 
        viewModel.triggerFileViewIntent.collectLatest { fileUri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "text/csv") 
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Buka dengan"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sound Capsule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.currentMonthAnalytics?.hasData == true) {
                        IconButton(onClick = {
                            showExportDialog = true
                        }) {
                            Icon(Icons.Filled.Download, "Export Analytics")
                        }
                    }
                    if (showExportDialog) {
                        AlertDialog(
                            onDismissRequest = { showExportDialog = false },
                            title = { Text("Choose Export Format") },
                            text = { Text("Select the format for your Sound Capsule export.") },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.exportAnalytics(ExportFormat.CSV)
                                    showExportDialog = false
                                }) { Text("CSV") }
                            },
                            dismissButton = {
                                Button(onClick = {
                                    Toast.makeText(context, "PDF export coming soon!", Toast.LENGTH_SHORT).show()
                                    showExportDialog = false
                                }) { Text("PDF (Soon)") }
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            MonthNavigationHeader(
                selectedMonthYear = uiState.selectedMonthYear,
                onPreviousMonth = { viewModel.selectPreviousMonth() },
                onNextMonth = { viewModel.selectNextMonth() },
                
                
                isPreviousEnabled = uiState.availableMonths.indexOf(uiState.selectedMonthYear)
                    .let { it != -1 && it < uiState.availableMonths.size - 1 }, 
                isNextEnabled = uiState.availableMonths.indexOf(uiState.selectedMonthYear) > 0
            )

            HorizontalDivider()

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Loading analytics...", modifier = Modifier.padding(top = 60.dp))
                }
            } else if (uiState.error != null && uiState.currentMonthAnalytics?.hasData == false) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                }
            } else if (uiState.currentMonthAnalytics != null) {
                val analytics = uiState.currentMonthAnalytics!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    item {
                        val timeToShow = if (uiState.selectedMonthYear == getCurrentMonthYear()) {
                            uiState.liveTimeListenedThisMonthMs 
                        } else {
                            analytics.totalTimeListenedMs 
                        }
                        AnalyticsStatCard(
                            title = "Time Listened This Month",
                            value = formatDuration(timeToShow),
                            icon = Icons.Filled.Schedule
                        )
                    }

                    
                    if (analytics.topSongs.isNotEmpty()) {
                        item { SectionTitle("Top Songs") }
                        items(analytics.topSongs) { song ->
                            TopItemRow(
                                rank = song.rank,
                                imageUrl = song.songArtUri,
                                title = song.title,
                                subtitle = song.artist,
                                playCount = song.playCount
                            )
                        }
                    }

                    
                    if (analytics.topArtists.isNotEmpty()) {
                        item { SectionTitle("Top Artists") }
                        items(analytics.topArtists) { artist ->
                            TopItemRow(
                                rank = artist.rank,
                                
                                imageUrl = null, 
                                title = artist.name,
                                subtitle = "${formatDuration(artist.totalDurationMs)} listened",
                                playCount = artist.playCount,
                                defaultIcon = Icons.Filled.Person
                            )
                        }
                    }

                    
                    if (analytics.dayStreaks.isNotEmpty()) {
                        item { SectionTitle("Day Streaks") }
                        items(analytics.dayStreaks) { streak ->
                            DayStreakItemRow(streak)
                        }
                    } else if (analytics.hasData) { 
                        item { Text("No significant day streaks this month.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                    }

                    
                    if (uiState.isExporting) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Exporting analytics...")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthNavigationHeader(
    selectedMonthYear: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    isPreviousEnabled: Boolean, 
    isNextEnabled: Boolean
) {
    val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val internalFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val date = internalFormat.parse(selectedMonthYear)
    val displayMonthYear = date?.let { displayFormat.format(it) } ?: selectedMonthYear

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth, enabled = isPreviousEnabled) { 
            Icon(Icons.Filled.ArrowBackIosNew, "Previous Month", tint = if (isPreviousEnabled) LocalContentColor.current else Color.Gray)
        }
        Text(displayMonthYear, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNextMonth, enabled = isNextEnabled) {
            Icon(Icons.Filled.ArrowForwardIos, "Next Month", tint = if (isNextEnabled) LocalContentColor.current else Color.Gray)
        }
    }
}

@Composable
fun AnalyticsStatCard(title: String, value: String, icon: ImageVector) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun TopItemRow(
    rank: Int?,
    imageUrl: String?,
    title: String,
    subtitle: String,
    playCount: Int,
    defaultIcon: ImageVector = Icons.Filled.Audiotrack
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        rank?.let {
            Text(
                String.format("%02d", it),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(30.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Image(
            painter = rememberAsyncImagePainter(
                model = imageUrl,
                error = painterResource(id = R.drawable.dummy_song_art) 
            ),
            contentDescription = title,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )




        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("$playCount plays", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun DayStreakItemRow(streak: DayStreak) {
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = streak.songArtUri,
                error = painterResource(id = R.drawable.dummy_song_art) 
            ),
            contentDescription = streak.title,
            modifier = Modifier.size(56.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(streak.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(streak.artist, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            "${streak.streakDays} day streak",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    

    if (hours > 0) {
        return String.format("%d hr %d min", hours, minutes)
    }
    return String.format("%d min", minutes)
}