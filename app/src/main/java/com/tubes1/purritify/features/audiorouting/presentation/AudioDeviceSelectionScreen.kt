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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDeviceSelectionScreen(
    navController: NavController,
    viewModel: AudioDeviceSelectionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                    containerColor = MaterialTheme.colorScheme.surface,
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
                                viewModel.refreshAudioDeviceList()
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


                else -> {
                    DeviceListView(
                        devices = uiState.devices,
                        onDeviceClick = { device ->
                            viewModel.selectDevice(device)
                        },
                        isDiscoveringBluetooth = uiState.isDiscoveringBluetooth
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
    isDiscoveringBluetooth: Boolean
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
        if (devices.isEmpty() && !isDiscoveringBluetooth) {
            EmptyStateView(onRefresh = {  })
        } else if (devices.isEmpty() && isDiscoveringBluetooth) {


        }
        else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                items(devices, key = { device -> device.uniqueKey }) { device ->
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


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

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


        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (device.isCurrentlySelectedOutput && device.pairingStatus != PairingStatus.FAILED) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )


            val subtitleText = when {
                device.pairingStatus == PairingStatus.PAIRING -> "Sedang memasangkan..."
                device.pairingStatus == PairingStatus.FAILED -> "Gagal memasangkan"
                device.source == AudioDeviceSource.BLUETOOTH_DISCOVERY &&
                        device.pairingStatus == PairingStatus.NONE &&
                        device.type.name.contains("BLUETOOTH", ignoreCase = true) -> "Ketuk untuk memasangkan"

                device.pairingStatus == PairingStatus.PAIRED && !device.isCurrentlySelectedOutput &&
                        device.type.name.contains("BLUETOOTH", ignoreCase = true) -> "Terpasang"
                else -> null
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

        Spacer(modifier = Modifier.width(8.dp))


        if (device.isCurrentlySelectedOutput && device.pairingStatus != PairingStatus.FAILED) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected Output",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }


    }
}

@Composable
fun getIconForDeviceType(type: DeviceType, pairingStatus: PairingStatus? = null): ImageVector {


    return when (type) {
        DeviceType.BUILTIN_SPEAKER -> Icons.Filled.Speaker
        DeviceType.BUILTIN_EARPIECE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Icons.Filled.PhoneInTalk
        } else {
            Icons.Filled.Speaker
        }
        DeviceType.WIRED_HEADSET, DeviceType.WIRED_HEADPHONES -> Icons.Filled.Headset
        DeviceType.BLUETOOTH_A2DP, DeviceType.BLUETOOTH_SCO, DeviceType.HEARING_AID -> Icons.Filled.BluetoothAudio
        DeviceType.USB_AUDIO_DEVICE -> Icons.Filled.Usb
        DeviceType.UNKNOWN -> Icons.Filled.DevicesOther
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
            imageVector = Icons.Filled.AudioFile,
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