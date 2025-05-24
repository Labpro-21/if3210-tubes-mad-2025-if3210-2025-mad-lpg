package com.tubes1.purritify.features.audiorouting.presentation

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDevice
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDeviceSource
import com.tubes1.purritify.features.audiorouting.domain.model.DeviceType
import com.tubes1.purritify.features.audiorouting.domain.model.PairingStatus
import org.koin.androidx.compose.koinViewModel // For Koin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDeviceSelectionScreen(
    navController: NavController,
    viewModel: AudioDeviceSelectionViewModel = koinViewModel() // Use Koin or your preferred DI
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current // For potential future use (e.g., Toasts)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResultMap ->
        viewModel.onPermissionsResult(permissionsResultMap)
    }

    LaunchedEffect(key1 = uiState.requiredPermissions, key2 = uiState.allPermissionsGranted) {
        if (uiState.requiredPermissions.isNotEmpty() && !uiState.allPermissionsGranted) {
            permissionLauncher.launch(uiState.requiredPermissions.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pilih Output Audio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Or background
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.devices.isEmpty() && !uiState.allPermissionsGranted && uiState.requiredPermissions.isNotEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Memeriksa izin...", modifier = Modifier.padding(top = 60.dp))
                    }
                }
                uiState.isLoading && uiState.devices.isEmpty() && uiState.allPermissionsGranted -> {

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Mendeteksi perangkat...", modifier = Modifier.padding(top = 60.dp))
                    }
                }

                uiState.error != null -> {
                    ErrorStateView(
                        message = uiState.error!!,
                        onRetry = {
                            if (uiState.requiredPermissions.isNotEmpty() && !uiState.allPermissionsGranted) {
                                permissionLauncher.launch(uiState.requiredPermissions.toTypedArray())
                            } else {
                                viewModel.refreshAudioDeviceList() // This will re-trigger permission check if needed internally
                            }
                        }
                    )
                }
                !uiState.allPermissionsGranted && uiState.requiredPermissions.isNotEmpty() -> {
                    PermissionNeededView(
                        onGrantPermissions = {
                            permissionLauncher.launch(uiState.requiredPermissions.toTypedArray())
                        },
                        permissions = uiState.requiredPermissions
                    )
                }
                // Device list (or empty state within DeviceListView)
                // No specific EmptyStateView here, DeviceListView handles it
                else -> {
                    DeviceListView(
                        devices = uiState.devices,
                        onDeviceClick = { device ->
                            viewModel.selectDevice(device)
                        },
                        isDiscoveringBluetooth = uiState.isDiscoveringBluetooth // Pass this state
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceListView(
    devices: List<AudioDevice>,
    onDeviceClick: (AudioDevice) -> Unit,
    isDiscoveringBluetooth: Boolean // Pass this from the ViewModel's UI state
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isDiscoveringBluetooth) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Mencari perangkat Bluetooth...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (devices.isEmpty() && !isDiscoveringBluetooth) { // Show empty state only if not discovering
            EmptyStateView(onRefresh = {  })
        } else if (devices.isEmpty() && isDiscoveringBluetooth) {
            // Optionally show a different message like "Scanning... no devices found yet."
            // For now, the global "Mencari perangkat Bluetooth..." above might suffice.
        }
        else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Fill remaining space
                    .padding(horizontal = 8.dp)
            ) {
                items(devices, key = { device -> device.uniqueKey }) { device -> // <-- Use uniqueKey
                    DeviceItemRow(
                        device = device,
                        onClick = { onDeviceClick(device) }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceItemRow(
    device: AudioDevice,
    onClick: () -> Unit
) {
    val isClickable = device.pairingStatus != PairingStatus.PAIRING && device.isConnectable
    // ^ Prevent clicks while pairing, or if device is marked not connectable

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp), // Slightly adjusted padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon & Pairing Status Indicator
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (device.pairingStatus == PairingStatus.PAIRING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = getIconForDeviceType(device.type, device.pairingStatus),
                    contentDescription = device.type.name,
                    modifier = Modifier.size(24.dp),
                    tint = if (device.isCurrentlySelectedOutput) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Device Name and Subtitle for Status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (device.isCurrentlySelectedOutput && device.pairingStatus != PairingStatus.FAILED) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle for pairing status or connection hint
            val subtitleText = when {
                device.pairingStatus == PairingStatus.PAIRING -> "Sedang memasangkan..."
                device.pairingStatus == PairingStatus.FAILED -> "Gagal memasangkan"
                device.source == AudioDeviceSource.BLUETOOTH_DISCOVERY &&
                        device.pairingStatus == PairingStatus.NONE &&
                        device.type.name.contains("BLUETOOTH", ignoreCase = true) -> "Ketuk untuk memasangkan"
                // device.isCurrentlySelectedOutput -> "Terhubung (aktif)" // Already indicated by checkmark
                device.pairingStatus == PairingStatus.PAIRED && !device.isCurrentlySelectedOutput &&
                        device.type.name.contains("BLUETOOTH", ignoreCase = true) -> "Terpasang" // Paired but not active
                else -> null // No subtitle needed
            }

            subtitleText?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (device.pairingStatus) {
                        PairingStatus.FAILED -> MaterialTheme.colorScheme.error
                        PairingStatus.PAIRING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp)) // Space before the checkmark

        // Trailing Checkmark Icon (if selected)
        if (device.isCurrentlySelectedOutput && device.pairingStatus != PairingStatus.FAILED) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected Output",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        // Optionally, show a different icon if pairing failed but it was previously selected
        // else if (device.pairingStatus == PairingStatus.FAILED && wasPreviouslySelected) { ... }
    }
}

@Composable
fun getIconForDeviceType(type: DeviceType, pairingStatus: PairingStatus? = null): ImageVector { // Added pairingStatus
    // You could make icons more specific based on pairing status, but for now, type-based is fine.
    // Example: if (type == DeviceType.BLUETOOTH_A2DP && pairingStatus == PairingStatus.NONE) return Icons.Filled.BluetoothSearching
    return when (type) {
        DeviceType.BUILTIN_SPEAKER -> Icons.Filled.Speaker
        DeviceType.BUILTIN_EARPIECE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Icons.Filled.PhoneInTalk // More specific for earpiece
        } else {
            Icons.Filled.Speaker // Fallback
        }
        DeviceType.WIRED_HEADSET, DeviceType.WIRED_HEADPHONES -> Icons.Filled.Headset
        DeviceType.BLUETOOTH_A2DP, DeviceType.BLUETOOTH_SCO, DeviceType.HEARING_AID -> Icons.Filled.BluetoothAudio
        DeviceType.USB_AUDIO_DEVICE -> Icons.Filled.Usb
        DeviceType.UNKNOWN -> Icons.Filled.DevicesOther // More generic unknown
    }
}

@Composable
fun PermissionNeededView(onGrantPermissions: () -> Unit, permissions: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.BluetoothSearching,
            contentDescription = "Permissions Needed",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Izin Diperlukan",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Untuk mendeteksi perangkat audio Bluetooth, aplikasi ini memerlukan izin Bluetooth. " +
                    "Izin yang dibutuhkan: ${permissions.joinToString { it.substringAfterLast('.') }}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGrantPermissions) {
            Text("Berikan Izin")
        }
    }
}

@Composable
fun ErrorStateView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Oops, terjadi kesalahan!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Coba Lagi / Berikan Izin")
        }
    }
}

@Composable
fun EmptyStateView(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AudioFile, // Or VolumeOff, HeadsetOff
            contentDescription = "No Devices Found",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Tidak Ada Perangkat Audio",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tidak ada perangkat audio eksternal yang terdeteksi. Pastikan perangkat Bluetooth Anda aktif dan dapat ditemukan, atau hubungkan headset kabel Anda.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onRefresh) {
            Text("Segarkan Daftar")
        }
    }
}