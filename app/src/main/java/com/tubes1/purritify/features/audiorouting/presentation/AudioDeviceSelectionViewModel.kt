package com.tubes1.purritify.features.audiorouting.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDevice
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDeviceSource
import com.tubes1.purritify.features.audiorouting.domain.model.DeviceType
import com.tubes1.purritify.features.audiorouting.domain.model.PairingStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository


data class AudioDeviceSelectionUiState(
    val devices: List<AudioDevice> = emptyList(),
    val isLoading: Boolean = false, // General loading for initial list
    val isDiscoveringBluetooth: Boolean = false, // Specific state for BT discovery
    val error: String? = null,
    val requiredPermissions: List<String> = emptyList(),
    val allPermissionsGranted: Boolean = false,
    val currentlySelectedSystemApiId: Int? = null // To highlight the active device based on AudioManager
)

@SuppressLint("MissingPermission") // Permissions are checked dynamically
class AudioDeviceSelectionViewModel(
    private val applicationContext: Context,
    private val musicPlayerRepository: MusicPlayerRepository

) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioDeviceSelectionUiState())
    val uiState: StateFlow<AudioDeviceSelectionUiState> = _uiState.asStateFlow()

    private val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var systemApiAudioDevices = mutableListOf<AudioDevice>()
    private var discoveredBluetoothDevices = mutableMapOf<String, AudioDevice>() // MAC address -> AudioDevice

    private var discoveryJob: Job? = null


    companion object {
        private const val TAG = "AudioDeviceVM"
        private const val BLUETOOTH_DISCOVERY_DURATION_MS = 15000L // 15 seconds
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        Log.d(TAG, "Bluetooth device FOUND: Name: ${it.name ?: "N/A"}, Address: ${it.address}, BondState: ${it.bondState}")
                        // Filter for audio devices if possible (e.g. by BluetoothClass)
                        if (it.name != null && isBluetoothDeviceAudioOutput(it)) { // Only add named devices that are audio capable
                            mapBluetoothDeviceToAppDevice(it)?.let { appDevice ->
                                discoveredBluetoothDevices[appDevice.address!!] = appDevice
                                updateDeviceListInUi()
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "Bluetooth discovery STARTED")
                    _uiState.update { it.copy(isDiscoveringBluetooth = true, error = null) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Bluetooth discovery FINISHED")
                    _uiState.update { it.copy(isDiscoveringBluetooth = false) }
                    // The list is already updated progressively by ACTION_FOUND
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    val previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)

                    device?.let { btDevice ->
                        Log.i(TAG, "Bond state changed for ${btDevice.name ?: btDevice.address}: " +
                                "${bondStateToString(previousBondState)} -> ${bondStateToString(bondState)}")
                        val newPairingStatus = when (bondState) {
                            BluetoothDevice.BOND_BONDED -> PairingStatus.PAIRED
                            BluetoothDevice.BOND_BONDING -> PairingStatus.PAIRING
                            BluetoothDevice.BOND_NONE -> PairingStatus.NONE
                            else -> PairingStatus.FAILED // Or keep current if unknown
                        }
                        // Update the specific device in the list
                        updateDevicePairingStatus(btDevice.address, newPairingStatus)

                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            // Attempt to set as preferred device after successful pairing
                            // Find the full AudioDevice object
                            val appDevice = _uiState.value.devices.find { it.address == btDevice.address }
                            if (appDevice != null) {
                                Log.d(TAG, "Device ${btDevice.name} successfully paired. Attempting to select.")
                                // We might want to automatically select it, or let the user confirm.
                                // For now, let's refresh the list and the user can tap again if needed,
                                // or call selectDevice directly.
                                selectDevice(appDevice.copy(pairingStatus = PairingStatus.PAIRED)) // Reselect with new status
                            } else {
                                refreshAudioDeviceList() // Refresh list to pick up newly paired device
                            }
                        } else if (bondState == BluetoothDevice.BOND_NONE && previousBondState == BluetoothDevice.BOND_BONDING) {
                            // Pairing failed
                            Log.w(TAG, "Pairing failed for ${btDevice.name}")
                            // Error message can be set if desired
                        }
                    }
                }
                // Optional: Listen for A2DP connection state changes for more fine-grained control
                // BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED
            }
        }
    }
    init {
        val perms = determineRequiredPermissions()
        val granted = checkAllPermissions(perms)
        _uiState.update { it.copy(requiredPermissions = perms, allPermissionsGranted = granted) }

        if (granted) {
            registerBluetoothReceiver()
            refreshAudioDeviceList() // This will also start discovery if appropriate
        }

        viewModelScope.launch {
            musicPlayerRepository.getPreferredAudioDevice()
                .distinctUntilChanged()
                .collectLatest { preferredDeviceFromRepo ->
                    Log.d(TAG, "User preference from repo changed to: ${preferredDeviceFromRepo?.name}")
                    if (_uiState.value.allPermissionsGranted) {
                        refreshAudioDeviceList() // Refresh to reflect new active state
                    }
                }
        }
    }

    private fun bondStateToString(bondState: Int): String {
        return when (bondState) {
            BluetoothDevice.BOND_NONE -> "NONE"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            BluetoothDevice.BOND_BONDED -> "BONDED"
            BluetoothDevice.ERROR -> "ERROR"
            else -> "UNKNOWN ($bondState)"
        }
    }
    private fun updateDevicePairingStatus(deviceAddress: String, newStatus: PairingStatus) {
        val updatedDevices = _uiState.value.devices.map {
            if (it.address == deviceAddress) {
                it.copy(pairingStatus = newStatus)
            } else {
                it
            }
        }
        _uiState.update { it.copy(devices = updatedDevices, isLoading = false) } // Stop general loading

        // Also update the discoveredBluetoothDevices map if the device originated there
        discoveredBluetoothDevices[deviceAddress]?.let {
            discoveredBluetoothDevices[deviceAddress] = it.copy(pairingStatus = newStatus)
        }
    }


    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            // addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) // If using A2DP profile
        }
        applicationContext.registerReceiver(bluetoothStateReceiver, filter)
    }

    private fun determineRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            // Location permission is needed for Bluetooth scanning on Android 6-11
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        return permissions.distinct()
    }

    private fun checkAllPermissions(permissions: List<String>): Boolean {
        if (permissions.isEmpty()) return true
        return permissions.all {
            ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun onPermissionsResult(grantedPermissions: Map<String, Boolean>) {
        val allNowGranted = grantedPermissions.values.all { it } &&
                checkAllPermissions(_uiState.value.requiredPermissions) // Re-check all required

        _uiState.update { it.copy(allPermissionsGranted = allNowGranted) }

        if (allNowGranted) {
            _uiState.update { it.copy(error = null) }
            registerBluetoothReceiver() // Register if not already (e.g., if granted after initial load)
            refreshAudioDeviceList()
        } else {
            val deniedPermissions = _uiState.value.requiredPermissions.filterNot {
                ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
            }
            val deniedString = deniedPermissions.joinToString { it.substringAfterLast('.') }

            _uiState.update { it.copy(error = "Permissions denied: $deniedString. Cannot list or manage all audio devices.") }
            Log.w(TAG, "Permissions denied: $deniedString")
            // Clear device list if crucial permissions are missing
            if (deniedPermissions.contains(Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                _uiState.update { it.copy(devices = emptyList()) }
            }
        }
    }

    fun selectDevice(deviceToSelect: AudioDevice) {
        viewModelScope.launch {
            Log.d(TAG, "User selected device: ${deviceToSelect.name}, Address: ${deviceToSelect.address}, Source: ${deviceToSelect.source}, Pairing: ${deviceToSelect.pairingStatus}")
            _uiState.update { it.copy(isLoading = true) }

            try {
                if (deviceToSelect.underlyingBluetoothDevice != null &&
                    deviceToSelect.pairingStatus == PairingStatus.NONE && // Only attempt to bond if not paired
                    deviceToSelect.underlyingBluetoothDevice.bondState == BluetoothDevice.BOND_NONE) {

                    Log.i(TAG, "Initiating pairing with ${deviceToSelect.name} (${deviceToSelect.address})")
                    updateDevicePairingStatus(deviceToSelect.address!!, PairingStatus.PAIRING)
                    val pairingStarted = deviceToSelect.underlyingBluetoothDevice.createBond()

                    if (!pairingStarted) {
                        Log.w(TAG, "Failed to start pairing with ${deviceToSelect.name}")
                        updateDevicePairingStatus(deviceToSelect.address, PairingStatus.FAILED)
                        _uiState.update { it.copy(isLoading = false, error = "Gagal memulai pairing dengan ${deviceToSelect.name}") }
                    } else {
                        // Pairing initiated, wait for ACTION_BOND_STATE_CHANGED. Stop loading indicator here.
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    return@launch // Exit, BroadcastReceiver will handle bond state changes
                } else if (deviceToSelect.underlyingBluetoothDevice != null &&
                    deviceToSelect.underlyingBluetoothDevice.bondState == BluetoothDevice.BOND_BONDING) {
                    Log.i(TAG, "${deviceToSelect.name} is already pairing.")
                    _uiState.update { it.copy(isLoading = false) } // Just reflect current state
                    return@launch
                }

                // If device is already paired or not a Bluetooth device needing pairing
                musicPlayerRepository.setPreferredAudioDevice(deviceToSelect)
                // Delay to allow system to process the routing request
                delay(1000) // Increased slightly
                refreshAudioDeviceList() // Refresh to show the *actual* active device
                _uiState.update { it.copy(isLoading = false, error = null) }

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during selectDevice: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Izin ditolak: ${e.message}") }
                refreshAudioDeviceList()
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting device: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Gagal memilih '${deviceToSelect.name}': ${e.message}") }
                refreshAudioDeviceList()
            }
        }
    }

    fun refreshAudioDeviceList() {
        if (!_uiState.value.allPermissionsGranted) {
            Log.w(TAG, "Cannot refresh devices without all permissions.")
            if (_uiState.value.error == null) {
                _uiState.update { it.copy(error = "Required permissions are not granted to list devices.") }
            }
            // Potentially request permissions again or guide user.
            val perms = determineRequiredPermissions()
            val granted = checkAllPermissions(perms)
            if (!granted) {
                _uiState.update { it.copy(requiredPermissions = perms, allPermissionsGranted = false) }
                return // Don't proceed if permissions are still not there. UI should show permission needed view.
            } else {
                _uiState.update { it.copy(allPermissionsGranted = true, error = null) } // Permissions might have been granted in background
            }
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        systemApiAudioDevices.clear()
        // Discovered devices are not cleared here, they persist until a new scan cycle or if they connect and appear in systemApi

        viewModelScope.launch {
            try {
                // 1. Get devices from AudioManager (connected, remembered)
                val outputDevicesInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val actualActiveDevice: AudioDeviceInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // S = API 31
                    audioManager.communicationDevice
                } else {
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                        .firstOrNull { it.isSink && isDeviceAudioOutput(it) }
                }
                val actualActiveSystemApiId = actualActiveDevice?.id
                _uiState.update { it.copy(currentlySelectedSystemApiId = actualActiveSystemApiId) }

                Log.d(TAG, "Actual active device from AudioManager: Name=${actualActiveDevice?.productName}, ID=${actualActiveSystemApiId}, Type=${actualActiveDevice?.type}")

                outputDevicesInfo.forEach { deviceInfo ->
                    if (isDeviceSuitableForUserSelection(deviceInfo)) {
                        mapAudioDeviceInfoToAppDevice(deviceInfo, actualActiveSystemApiId)?.let { appDevice ->
                            systemApiAudioDevices.add(appDevice)
                        }
                    }
                }

                // 2. Start Bluetooth Discovery for other nearby devices
                startBluetoothDiscovery() // This will update discoveredBluetoothDevices via receiver

                // 3. Merge and update UI (initial merge, will be updated by discovery)
                updateDeviceListInUi()
                _uiState.update { it.copy(isLoading = false) } // Initial load from AudioManager done

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException getting audio devices: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Permission issue: ${e.message}", allPermissionsGranted = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading audio devices: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Gagal memuat perangkat: ${e.message}") }
            }
        }
    }

    private fun isDeviceAudioOutput(deviceInfo: AudioDeviceInfo): Boolean {
        return when (deviceInfo.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_HEARING_AID, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC, AudioDeviceInfo.TYPE_HDMI
                -> true
            else -> false
        }
    }
    private fun startBluetoothDiscovery() {
        if (bluetoothAdapter == null || !_uiState.value.allPermissionsGranted) {
            Log.w(TAG, "Bluetooth not supported or permissions missing for discovery.")
            _uiState.update { it.copy(isDiscoveringBluetooth = false) } // Ensure it's false
            return
        }

        if (bluetoothAdapter.isDiscovering) {
            Log.d(TAG, "Already discovering Bluetooth devices.")
            // Update UI state just in case it was missed
            _uiState.update { it.copy(isDiscoveringBluetooth = true) }
            return
        }

        Log.d(TAG, "Attempting to start Bluetooth discovery on Android 12+ logic path.")

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_SCAN permission is NOT granted at runtime! Cannot start discovery.")
            _uiState.update { it.copy(
                isDiscoveringBluetooth = false,
                error = "Izin Pindai Bluetooth tidak diberikan."
            )}
            return // CRITICAL: Do not proceed without the permission
        } else {
            Log.i(TAG, "BLUETOOTH_SCAN permission IS granted at runtime.")
        }

// Also good to check BLUETOOTH_CONNECT if you use it before discovery (though SCAN is primary for discovery start)
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission is NOT granted at runtime. This might affect later operations like pairing or getting names.")
            // Discovery might still start, but other things will fail.
        }

        // Clear previously *only* discovered devices that aren't paired yet. Paired ones might still be relevant.
        // Or, rely on them being overwritten if found again.
        // For simplicity, let's allow them to persist a bit and get updated if found/paired.
        // A more aggressive clear: discoveredBluetoothDevices.clear()
        // However, this might remove a device that user was about to tap if discovery restarts.

        if (bluetoothAdapter.startDiscovery()) {
            Log.i(TAG, "Starting Bluetooth discovery...")
            _uiState.update { it.copy(isDiscoveringBluetooth = true, error = null) }
            discoveryJob?.cancel()
            discoveryJob = viewModelScope.launch {
                delay(BLUETOOTH_DISCOVERY_DURATION_MS)
                if (bluetoothAdapter.isDiscovering) {
                    Log.i(TAG, "Bluetooth discovery timeout. Cancelling.")
                    bluetoothAdapter.cancelDiscovery() // This will trigger ACTION_DISCOVERY_FINISHED
                }
                _uiState.update { it.copy(isDiscoveringBluetooth = false) }
            }
        } else {
            Log.e(TAG, "Failed to start Bluetooth discovery.")
            _uiState.update { it.copy(isDiscoveringBluetooth = false, error = "Gagal memulai pencarian Bluetooth.") }
        }
    }

    private fun updateDeviceListInUi() {
        val combinedDevices = mutableMapOf<String, AudioDevice>() // Use uniqueKey as map key

        // Add system API devices first (they might have more accurate "isCurrentlySelectedOutput")
        systemApiAudioDevices.forEach { device ->
            combinedDevices[device.uniqueKey] = device
        }

        // Add/update with discovered Bluetooth devices
        // If a device from discovery has the same address as one from system API,
        // the system API one (already in map) might be preferred or info merged.
        discoveredBluetoothDevices.values.forEach { discoveredDevice ->
            val existing = combinedDevices[discoveredDevice.uniqueKey]
            if (existing != null) {
                // Merge: prioritize system API for `isCurrentlySelectedOutput`, but update pairing status
                // and underlyingBluetoothDevice from discovery if it's more current.
                combinedDevices[discoveredDevice.uniqueKey] = existing.copy(
                    pairingStatus = discoveredDevice.pairingStatus, // Discovery has fresher pairing intent
                    underlyingBluetoothDevice = discoveredDevice.underlyingBluetoothDevice ?: existing.underlyingBluetoothDevice,
                    name = if (discoveredDevice.name.isNotBlank() && discoveredDevice.name != "N/A") discoveredDevice.name else existing.name // Prefer non-generic name
                )
            } else {
                combinedDevices[discoveredDevice.uniqueKey] = discoveredDevice
            }
        }
        val sortedList = combinedDevices.values.sortedWith(
            compareByDescending<AudioDevice> { it.isCurrentlySelectedOutput }
                .thenByDescending { it.pairingStatus == PairingStatus.PAIRED && it.source == AudioDeviceSource.SYSTEM_API } // Paired & connected
                .thenBy { it.pairingStatus != PairingStatus.PAIRED && it.source == AudioDeviceSource.BLUETOOTH_DISCOVERY } // Discoverable last
                .thenBy { it.name.toString() }
        )

        _uiState.update { it.copy(devices = sortedList) }
    }


    private fun isDeviceSuitableForUserSelection(deviceInfo: AudioDeviceInfo): Boolean {
        // Your existing logic, ensure it aligns with DeviceType mapping
        return when (deviceInfo.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_HEARING_AID -> true
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX, AudioDeviceInfo.TYPE_TELEPHONY -> false
            else -> {
                Log.d(TAG, "Considering device suitability for type ${deviceInfo.type} (${deviceInfo.productName}): false by default")
                false // Default to false for unhandled types
            }
        }
    }

    private fun mapAudioDeviceInfoToAppDevice(deviceInfo: AudioDeviceInfo, activeSystemApiId: Int?): AudioDevice? {
        val deviceName: CharSequence = getDeviceNameFromInfo(deviceInfo)
        val appDeviceType = getAppDeviceTypeFromInfo(deviceInfo)

        if (appDeviceType == DeviceType.UNKNOWN && deviceName.contains("Unknown", ignoreCase = true) && deviceInfo.id != activeSystemApiId) {
            return null // Skip truly unknown devices unless active
        }

        val isBluetooth = appDeviceType == DeviceType.BLUETOOTH_A2DP || appDeviceType == DeviceType.BLUETOOTH_SCO || appDeviceType == DeviceType.HEARING_AID
        var underlyingBtDevice: BluetoothDevice? = null
        var pairingStat = PairingStatus.NONE

        if (isBluetooth && bluetoothAdapter != null && !deviceInfo.address.isNullOrEmpty()) {
            try {
                underlyingBtDevice = bluetoothAdapter.getRemoteDevice(deviceInfo.address)
                pairingStat = when (underlyingBtDevice.bondState) {
                    BluetoothDevice.BOND_BONDED -> PairingStatus.PAIRED
                    BluetoothDevice.BOND_BONDING -> PairingStatus.PAIRING
                    else -> PairingStatus.NONE
                }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid Bluetooth address from AudioDeviceInfo: ${deviceInfo.address}")
            }
        } else if (!isBluetooth) {
            pairingStat = PairingStatus.PAIRED // Non-BT devices are implicitly "paired" or not applicable
        }


        return AudioDevice(
            systemApiId = deviceInfo.id,
            name = deviceName,
            type = appDeviceType,
            systemDeviceType = deviceInfo.type,
            address = if (isBluetooth) deviceInfo.address else null,
            isCurrentlySelectedOutput = (deviceInfo.id == activeSystemApiId),
            pairingStatus = pairingStat,
            source = AudioDeviceSource.SYSTEM_API,
            isConnectable = true, // Assume connectable if from AudioManager and suitable
            underlyingSystemApiDevice = deviceInfo,
            underlyingBluetoothDevice = underlyingBtDevice
        )
    }
    private fun getDeviceNameFromInfo(deviceInfo: AudioDeviceInfo): CharSequence {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                deviceInfo.productName.takeIf { !it.isNullOrEmpty() } ?: getFallbackDeviceName(deviceInfo, false)
            } else {
                getFallbackDeviceName(deviceInfo, false)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting product name for device ID ${deviceInfo.id}. Fallback. Error: ${e.message}")
            getFallbackDeviceName(deviceInfo, true) // Force fallback if security exception
        }
    }

    private fun getAppDeviceTypeFromInfo(deviceInfo: AudioDeviceInfo) : DeviceType {
        return when (deviceInfo.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> DeviceType.BUILTIN_SPEAKER
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> DeviceType.BUILTIN_EARPIECE
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> DeviceType.WIRED_HEADSET
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> DeviceType.WIRED_HEADPHONES
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> DeviceType.BLUETOOTH_A2DP
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceType.BLUETOOTH_SCO
            AudioDeviceInfo.TYPE_HEARING_AID -> DeviceType.HEARING_AID
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY -> DeviceType.USB_AUDIO_DEVICE
            else -> DeviceType.UNKNOWN
        }
    }
    private fun isBluetoothDeviceAudioOutput(btDevice: BluetoothDevice): Boolean {
        // Check BluetoothClass for audio capabilities
        // This is a hint, not a guarantee it's A2DP or HFP.
        val btClass = btDevice.bluetoothClass ?: return false // No class info, assume not audio
        return when (btClass.majorDeviceClass) {
            android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                when (btClass.deviceClass) {
                    android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE,
                    android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
                    android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
                    android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
                    android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
                    android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> true
                    else -> false
                }
            }
            else -> false
        }
    }


    private fun mapBluetoothDeviceToAppDevice(btDevice: BluetoothDevice): AudioDevice? {
        // Don't add if address is null, which shouldn't happen for real devices
        if (btDevice.address == null) return null
        val deviceName = btDevice.name ?: "Unknown Bluetooth Device"
        // Determine DeviceType based on BluetoothClass if possible, default to BLUETOOTH_A2DP or UNKNOWN
        // This is a simplification; a full mapping from BluetoothClass to your DeviceType would be complex.
        val appDeviceType = if (isBluetoothDeviceAudioOutput(btDevice)) DeviceType.BLUETOOTH_A2DP else DeviceType.UNKNOWN
        if (appDeviceType == DeviceType.UNKNOWN && !deviceName.contains("speaker", true) && !deviceName.contains("headphone", true) && !deviceName.contains("audio", true)) {
            // Log.d(TAG, "Skipping discovered BT device '${deviceName}' as it doesn't appear to be audio output by class/name.")
            // return null // Be more lenient for discovered devices, let user decide if name sounds plausible.
        }


        val pairingStatus = when (btDevice.bondState) {
            BluetoothDevice.BOND_BONDED -> PairingStatus.PAIRED
            BluetoothDevice.BOND_BONDING -> PairingStatus.PAIRING
            else -> PairingStatus.NONE
        }

        // systemApiId is null because this is from discovery, not yet (or maybe never) from AudioDeviceInfo
        return AudioDevice(
            systemApiId = null, // Will be populated if it connects and appears in AudioManager
            name = deviceName,
            type = appDeviceType, // Or more specific based on btClass
            systemDeviceType = btDevice.bluetoothClass?.deviceClass ?: 0, // Store raw BT class
            address = btDevice.address,
            isCurrentlySelectedOutput = false, // Discovered devices are not active output initially
            pairingStatus = pairingStatus,
            source = AudioDeviceSource.BLUETOOTH_DISCOVERY,
            isConnectable = true, // Assume connectable
            underlyingSystemApiDevice = null,
            underlyingBluetoothDevice = btDevice
        )
    }

    @SuppressLint("MissingPermission")
    private fun getFallbackDeviceName(deviceInfo: AudioDeviceInfo, forceBluetoothLookup: Boolean = false): CharSequence {
        val isBluetoothType = deviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                deviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                deviceInfo.type == AudioDeviceInfo.TYPE_HEARING_AID

        if (isBluetoothType || forceBluetoothLookup) {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && _uiState.value.allPermissionsGranted) {
                try {
                    if (!deviceInfo.address.isNullOrEmpty()) {
                        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceInfo.address)
                        bluetoothDevice?.name?.let { if (it.isNotBlank()) return it }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error looking up BT device name for address ${deviceInfo.address}: ${e.message}")
                }
            }
            // Fallback for Bluetooth if name not found via adapter or address missing from AudioDeviceInfo
            return when (deviceInfo.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Audio"
                AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing Aid"
                else -> "Bluetooth Device ${deviceInfo.id}" // Generic if forced and not a known BT type
            }
        }

        return when (deviceInfo.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Device Speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Device Earpiece"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Audio Device"
            else -> "Unknown Device ${deviceInfo.id}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            applicationContext.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Bluetooth receiver not registered or already unregistered.", e)
        }
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        discoveryJob?.cancel()
        Log.d(TAG, "ViewModel cleared, discovery cancelled, receiver unregistered.")
    }
}