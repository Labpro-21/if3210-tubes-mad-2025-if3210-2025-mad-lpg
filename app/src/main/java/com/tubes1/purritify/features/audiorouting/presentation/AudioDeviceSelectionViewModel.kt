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
    val isLoading: Boolean = false,
    val isDiscoveringBluetooth: Boolean = false,
    val error: String? = null,
    val requiredPermissions: List<String> = emptyList(),
    val allPermissionsGranted: Boolean = false,
    val currentlySelectedSystemApiId: Int? = null
)

@SuppressLint("MissingPermission")
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
    private var discoveredBluetoothDevices = mutableMapOf<String, AudioDevice>()

    private var discoveryJob: Job? = null

    companion object {
        private const val TAG = "AudioDeviceVM"
        private const val BLUETOOTH_DISCOVERY_DURATION_MS = 15000L
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

                        if (it.name != null && isBluetoothDeviceAudioOutput(it)) {
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
                        val oldUiDeviceStatus = _uiState.value.devices.find { it.address == btDevice.address }?.pairingStatus
                        Log.i(TAG, "Bond state changed for ${btDevice.name ?: btDevice.address}: " +
                                "${bondStateToString(previousBondState)} -> ${bondStateToString(bondState)}")

                        val newPairingStatus = when (bondState) {
                            BluetoothDevice.BOND_BONDED -> PairingStatus.PAIRED
                            BluetoothDevice.BOND_BONDING -> PairingStatus.PAIRING
                            BluetoothDevice.BOND_NONE -> PairingStatus.NONE
                            else -> oldUiDeviceStatus ?: PairingStatus.FAILED
                        }


                        updateDevicePairingStatus(btDevice.address, newPairingStatus, oldUiDeviceStatus)

                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            Log.d(TAG, "Device ${btDevice.name ?: btDevice.address} successfully paired. Kicking off post-pairing selection.")



                            val originalDiscoveredDevice = discoveredBluetoothDevices[btDevice.address]
                            val appDeviceNowPaired = originalDiscoveredDevice?.copy(
                                pairingStatus = PairingStatus.PAIRED,

                                name = btDevice.name ?: originalDiscoveredDevice.name
                            ) ?: mapBluetoothDeviceToAppDevice(btDevice)?.copy(
                                pairingStatus = PairingStatus.PAIRED
                            )


                            if (appDeviceNowPaired != null) {
                                selectDeviceAfterPairing(appDeviceNowPaired)
                            } else {
                                Log.e(TAG, "CRITICAL: Could not construct an AudioDevice for ${btDevice.address} post-bond. Refreshing list.")
                                _uiState.update { it.copy(isLoading = false) }
                                refreshAudioDeviceList()
                            }
                        } else if (bondState == BluetoothDevice.BOND_NONE && previousBondState == BluetoothDevice.BOND_BONDING) {
                            Log.w(TAG, "Pairing failed or reverted to NONE for ${btDevice.name ?: btDevice.address}")
                            _uiState.update { it.copy(error = "Pairing gagal dengan ${btDevice.name ?: btDevice.address}.") }

                        } else if (bondState == BluetoothDevice.BOND_BONDING) {
                            Log.d(TAG, "Device ${btDevice.name ?: btDevice.address} is still bonding.")

                        }
                    }
                }
            }
        }
    }
    init {
        val perms = determineRequiredPermissions()
        val granted = checkAllPermissions(perms)
        _uiState.update { it.copy(requiredPermissions = perms, allPermissionsGranted = granted) }

        if (granted) {
            registerBluetoothReceiver()
            refreshAudioDeviceList()
        }

        viewModelScope.launch {
            musicPlayerRepository.getPreferredAudioDevice()
                .distinctUntilChanged()
                .collectLatest { preferredDeviceFromRepo ->
                    Log.d(TAG, "User preference from repo changed to: ${preferredDeviceFromRepo?.name}")
                    if (_uiState.value.allPermissionsGranted) {
                        refreshAudioDeviceList()
                    }
                }
        }
    }

    private fun selectDeviceAfterPairing(devicePaired: AudioDevice) {
        viewModelScope.launch {
            Log.d(TAG, "selectDeviceAfterPairing: For ${devicePaired.name}. Current isLoading: ${_uiState.value.isLoading}")
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "selectDeviceAfterPairing: Waiting for device to register with AudioManager (2.5s)...")
            delay(2500L)

            Log.d(TAG, "selectDeviceAfterPairing: Refreshing system API audio devices now.")
            refreshSystemApiAudioDevicesInternalOnly()
            updateDeviceListInUi()

            val deviceToRoute = _uiState.value.devices.find { it.address == devicePaired.address } ?: devicePaired
            Log.d(TAG, "selectDeviceAfterPairing: Device to pass to repo: Name=${deviceToRoute.name}, " +
                    "SysID=${deviceToRoute.systemApiId}, Addr=${deviceToRoute.address}, " +
                    "Source=${deviceToRoute.source}, Type=${deviceToRoute.type}")

            val deviceForOptimisticUpdate = _uiState.value.devices.find { it.uniqueKey == deviceToRoute.uniqueKey }
                ?: deviceToRoute // Fallback to deviceToRoute if somehow not in the list

            Log.d(TAG, "selectDeviceAfterPairing: Device to pass to repo was: Name=${deviceToRoute.name}, " +
                    "SysID=${deviceToRoute.systemApiId}, Addr=${deviceToRoute.address}, " +
                    "Source=${deviceToRoute.source}, Type=${deviceToRoute.type}")
            Log.d(TAG, "selectDeviceAfterPairing: Device found in UI state for optimistic update: Name=${deviceForOptimisticUpdate.name}, " +
                    "SysID=${deviceForOptimisticUpdate.systemApiId}, Addr=${deviceForOptimisticUpdate.address}")


            try {
                musicPlayerRepository.setPreferredAudioDevice(deviceToRoute) // Use deviceToRoute for the actual repo call
                Log.d(TAG, "selectDeviceAfterPairing: Waiting for service routing (1.5s)...")
                delay(1500L)

                // Optimistic UI Update: Mark the device as selected in the UI
                // Use deviceForOptimisticUpdate.uniqueKey to ensure we update the right item in the list
                _uiState.update { currentState ->
                    val updatedDeviceList = currentState.devices.map { deviceInList ->
                        if (deviceInList.uniqueKey == deviceForOptimisticUpdate.uniqueKey) {
                            deviceInList.copy(isCurrentlySelectedOutput = true, pairingStatus = PairingStatus.PAIRED)
                        } else {
                            deviceInList.copy(isCurrentlySelectedOutput = false)
                        }
                    }
                    currentState.copy(
                        devices = updatedDeviceList,
                        // If routing was successful, currentlySelectedSystemApiId *should* update after refresh,
                        // but we can optimistically set it here too if deviceForOptimisticUpdate has a systemApiId.
                        currentlySelectedSystemApiId = deviceForOptimisticUpdate.systemApiId ?: currentState.currentlySelectedSystemApiId
                    )
                }
                Log.d(TAG, "Optimistically marked ${deviceForOptimisticUpdate.name} as selected in UI.")
            } catch (e: Exception) {
                Log.e(TAG, "Error in selectDeviceAfterPairing setting preferred device: ${e.message}", e)
                _uiState.update { it.copy(error = "Gagal mengatur ${deviceForOptimisticUpdate.name}: ${e.message}") }
            } finally {
                Log.d(TAG, "selectDeviceAfterPairing: Refreshing full audio device list to reflect final state.")
                refreshAudioDeviceList()
            }
        }
    }
    private suspend fun refreshSystemApiAudioDevicesInternalOnly() {
        if (!_uiState.value.allPermissionsGranted) {
            Log.w(TAG, "refreshSystemApiAudioDevicesInternalOnly: Permissions not granted.")
            return
        }
        try {
            val outputDevicesInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val activeDevice: AudioDeviceInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.communicationDevice
            } else {
                outputDevicesInfo.firstOrNull { it.isSink && isDeviceAudioOutput(it) && it.id == _uiState.value.currentlySelectedSystemApiId }
                    ?: outputDevicesInfo.firstOrNull { it.isSink && isDeviceAudioOutput(it) }
            }
            val activeId = activeDevice?.id
            _uiState.update { it.copy(currentlySelectedSystemApiId = activeId) }


            systemApiAudioDevices.clear()
            outputDevicesInfo.forEach { deviceInfo ->
                if (isDeviceSuitableForUserSelection(deviceInfo)) {
                    mapAudioDeviceInfoToAppDevice(deviceInfo, activeId)?.let { appDevice ->
                        systemApiAudioDevices.add(appDevice)
                    }
                }
            }
            Log.d(TAG, "refreshSystemApiAudioDevicesInternalOnly: Updated systemApiAudioDevices with ${systemApiAudioDevices.size} devices.")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in refreshSystemApiAudioDevicesInternalOnly: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error in refreshSystemApiAudioDevicesInternalOnly: ${e.message}", e)
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

    private fun updateDevicePairingStatus(deviceAddress: String, newStatus: PairingStatus, previousStatus: PairingStatus?) {
        Log.d(TAG, "updateDevicePairingStatus: Addr=$deviceAddress, NewStatus=$newStatus, PrevStatus=$previousStatus, CurrentIsLoading=${_uiState.value.isLoading}")

        val updatedDevices = _uiState.value.devices.map {
            if (it.address == deviceAddress) {
                it.copy(pairingStatus = newStatus)
            } else { it }
        }

        var turnOffLoading = false
        if (previousStatus == PairingStatus.PAIRING && (newStatus == PairingStatus.FAILED || newStatus == PairingStatus.NONE)) {
            turnOffLoading = true
        }

        if (previousStatus == PairingStatus.NONE && newStatus == PairingStatus.FAILED && _uiState.value.isLoading) {
            turnOffLoading = true
        }

        _uiState.update {
            it.copy(
                devices = updatedDevices,
                isLoading = if (turnOffLoading) false else it.isLoading,
                error = if (newStatus == PairingStatus.FAILED && it.error == null) "Pairing Gagal untuk ${deviceAddress.takeLast(5)}" else it.error
            )
        }

        discoveredBluetoothDevices[deviceAddress]?.let {
            discoveredBluetoothDevices[deviceAddress] = it.copy(pairingStatus = newStatus)
        }
        Log.d(TAG, "updateDevicePairingStatus: After update, isLoading=${_uiState.value.isLoading}")
    }


    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        applicationContext.registerReceiver(bluetoothStateReceiver, filter)
    }

    private fun determineRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
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
                checkAllPermissions(_uiState.value.requiredPermissions)

        _uiState.update { it.copy(allPermissionsGranted = allNowGranted) }

        if (allNowGranted) {
            _uiState.update { it.copy(error = null) }
            registerBluetoothReceiver()
            refreshAudioDeviceList()
        } else {
            val deniedPermissions = _uiState.value.requiredPermissions.filterNot {
                ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
            }
            val deniedString = deniedPermissions.joinToString { it.substringAfterLast('.') }

            _uiState.update { it.copy(error = "Permissions denied: $deniedString. Cannot list or manage all audio devices.") }
            Log.w(TAG, "Permissions denied: $deniedString")

            if (deniedPermissions.contains(Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                _uiState.update { it.copy(devices = emptyList()) }
            }
        }
    }

    fun selectDevice(deviceToSelect: AudioDevice) {
        viewModelScope.launch {
            Log.d(TAG, "selectDevice (user click): ${deviceToSelect.name}, Pairing: ${deviceToSelect.pairingStatus}")

            if (deviceToSelect.underlyingBluetoothDevice != null &&
                deviceToSelect.pairingStatus == PairingStatus.NONE &&
                deviceToSelect.underlyingBluetoothDevice.bondState == BluetoothDevice.BOND_NONE) {

                Log.i(TAG, "selectDevice: Initiating pairing for ${deviceToSelect.name}")
                updateDevicePairingStatus(deviceToSelect.address!!, PairingStatus.PAIRING, PairingStatus.NONE)
                _uiState.update { it.copy(isLoading = true) }

                val pairingStarted = deviceToSelect.underlyingBluetoothDevice.createBond()
                if (!pairingStarted) {
                    Log.w(TAG, "selectDevice: Failed to start pairing with ${deviceToSelect.name}")
                    updateDevicePairingStatus(deviceToSelect.address, PairingStatus.FAILED, PairingStatus.PAIRING)
                }

                return@launch
            } else if (deviceToSelect.underlyingBluetoothDevice != null &&
                deviceToSelect.underlyingBluetoothDevice.bondState == BluetoothDevice.BOND_BONDING) {
                Log.i(TAG, "selectDevice: ${deviceToSelect.name} is already pairing.")
                if (deviceToSelect.pairingStatus != PairingStatus.PAIRING) {
                    updateDevicePairingStatus(deviceToSelect.address!!, PairingStatus.PAIRING, deviceToSelect.pairingStatus)
                }
                _uiState.update { it.copy(isLoading = true) }
                return@launch
            }

            Log.d(TAG, "selectDevice: Device is already paired or non-BT. Attempting to set as preferred.")
            _uiState.update { it.copy(isLoading = true) }
            try {
                musicPlayerRepository.setPreferredAudioDevice(deviceToSelect)
                delay(1000L)
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting device (non-pairing): ${e.message}", e)
                _uiState.update { it.copy(error = "Gagal memilih '${deviceToSelect.name}': ${e.message}") }
            } finally {
                refreshAudioDeviceList()
            }
        }
    }

    fun refreshAudioDeviceList() {
        if (!_uiState.value.allPermissionsGranted) {
            Log.w(TAG, "refreshAudioDeviceList: Permissions not granted.")

            val perms = determineRequiredPermissions()
            val granted = checkAllPermissions(perms)
            if (!granted) {
                _uiState.update { it.copy(requiredPermissions = perms, allPermissionsGranted = false, isLoading = false, error = "Izin dibutuhkan.") }
                return
            } else {
                _uiState.update { it.copy(allPermissionsGranted = true, error = null) }
            }
        }

        Log.d(TAG, "refreshAudioDeviceList: Starting. Current isLoading: ${_uiState.value.isLoading}, isDiscovering: ${_uiState.value.isDiscoveringBluetooth}")
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                refreshSystemApiAudioDevicesInternalOnly()

                if (bluetoothAdapter?.isEnabled == true && _uiState.value.allPermissionsGranted && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    startBluetoothDiscovery()
                } else {
                    Log.w(TAG, "refreshAudioDeviceList: BT not enabled or SCAN perm missing, skipping BT discovery.")
                    _uiState.update { it.copy(isDiscoveringBluetooth = false) }
                }

                updateDeviceListInUi()

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException in refreshAudioDeviceList: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Izin bermasalah: ${e.message}", allPermissionsGranted = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Error in refreshAudioDeviceList: ${e.message}", e)
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
            _uiState.update { it.copy(isDiscoveringBluetooth = false) }
            return
        }

        if (bluetoothAdapter.isDiscovering) {
            Log.d(TAG, "Already discovering Bluetooth devices.")

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
            return
        } else {
            Log.i(TAG, "BLUETOOTH_SCAN permission IS granted at runtime.")
        }

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission is NOT granted at runtime. This might affect later operations like pairing or getting names.")

        }

        if (bluetoothAdapter.startDiscovery()) {
            Log.i(TAG, "Starting Bluetooth discovery...")
            _uiState.update { it.copy(isDiscoveringBluetooth = true, error = null) }
            discoveryJob?.cancel()
            discoveryJob = viewModelScope.launch {
                delay(BLUETOOTH_DISCOVERY_DURATION_MS)
                if (bluetoothAdapter.isDiscovering) {
                    Log.i(TAG, "Bluetooth discovery timeout. Cancelling.")
                    bluetoothAdapter.cancelDiscovery()
                }
                _uiState.update { it.copy(isDiscoveringBluetooth = false) }
            }
        } else {
            Log.e(TAG, "Failed to start Bluetooth discovery.")
            _uiState.update { it.copy(isDiscoveringBluetooth = false, error = "Gagal memulai pencarian Bluetooth.") }
        }
    }

    private fun updateDeviceListInUi() {
        val combinedDevices = mutableMapOf<String, AudioDevice>()

        systemApiAudioDevices.forEach { device ->
            combinedDevices[device.uniqueKey] = device
        }

        discoveredBluetoothDevices.values.forEach { discoveredDevice ->
            val existing = combinedDevices[discoveredDevice.uniqueKey]
            if (existing != null) {
                combinedDevices[discoveredDevice.uniqueKey] = existing.copy(
                    pairingStatus = discoveredDevice.pairingStatus,
                    underlyingBluetoothDevice = discoveredDevice.underlyingBluetoothDevice ?: existing.underlyingBluetoothDevice,
                    name = if (discoveredDevice.name.isNotBlank() && discoveredDevice.name != "N/A") discoveredDevice.name else existing.name
                )
            } else {
                combinedDevices[discoveredDevice.uniqueKey] = discoveredDevice
            }
        }
        val sortedList = combinedDevices.values.sortedWith(
            compareByDescending<AudioDevice> { it.isCurrentlySelectedOutput }
                .thenByDescending { it.pairingStatus == PairingStatus.PAIRED && it.source == AudioDeviceSource.SYSTEM_API }
                .thenBy { it.pairingStatus != PairingStatus.PAIRED && it.source == AudioDeviceSource.BLUETOOTH_DISCOVERY }
                .thenBy { it.name.toString() }
        )

        _uiState.update { it.copy(devices = sortedList) }
    }

    private fun isDeviceSuitableForUserSelection(deviceInfo: AudioDeviceInfo): Boolean {
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
                false
            }
        }
    }

    private fun mapAudioDeviceInfoToAppDevice(deviceInfo: AudioDeviceInfo, activeSystemApiId: Int?): AudioDevice? {
        val deviceName: CharSequence = getDeviceNameFromInfo(deviceInfo)
        val appDeviceType = getAppDeviceTypeFromInfo(deviceInfo)

        if (appDeviceType == DeviceType.UNKNOWN && deviceName.contains("Unknown", ignoreCase = true) && deviceInfo.id != activeSystemApiId) {
            return null
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
            pairingStat = PairingStatus.PAIRED
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
            isConnectable = true,
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
            getFallbackDeviceName(deviceInfo, true)
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
        val btClass = btDevice.bluetoothClass ?: return false
        Log.d(TAG, "isBluetoothDeviceAudioOutput for ${btDevice.name ?: btDevice.address}: " +
                "Major=${String.format("0x%04X", btClass.majorDeviceClass)}, " +
                "Device=${String.format("0x%06X", btClass.deviceClass)}, " +
                "HasAudioService=${btClass.hasService(android.bluetooth.BluetoothClass.Service.AUDIO)}")

        if (btClass.majorDeviceClass == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO) {
            if (btClass.hasService(android.bluetooth.BluetoothClass.Service.AUDIO)) {
                Log.d(TAG, " -> IS Audio Output (Major AUDIO_VIDEO + AUDIO service).")
                return true
            }

            return when (btClass.deviceClass) {
                android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
                android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE,
                android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
                android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
                android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
                android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
                android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> {
                    Log.d(TAG, " -> IS Audio Output (Major AUDIO_VIDEO + specific minor device class).")
                    true
                }
                else -> {
                    Log.d(TAG, " -> NOT Audio Output (Major AUDIO_VIDEO but no AUDIO service bit and no matching specific minor class).")
                    false
                }
            }
        }
        Log.d(TAG, " -> NOT AUDIO_VIDEO major device class.")
        return false
    }


    private fun mapBluetoothDeviceToAppDevice(btDevice: BluetoothDevice): AudioDevice? {
        if (btDevice.address == null) return null
        val deviceName = btDevice.name ?: "Unknown Bluetooth Device"

        val appDeviceType = if (isBluetoothDeviceAudioOutput(btDevice)) DeviceType.BLUETOOTH_A2DP else DeviceType.UNKNOWN

        val pairingStatus = when (btDevice.bondState) {
            BluetoothDevice.BOND_BONDED -> PairingStatus.PAIRED
            BluetoothDevice.BOND_BONDING -> PairingStatus.PAIRING
            else -> PairingStatus.NONE
        }

        return AudioDevice(
            systemApiId = null,
            name = deviceName,
            type = appDeviceType,
            systemDeviceType = btDevice.bluetoothClass?.deviceClass ?: 0,
            address = btDevice.address,
            isCurrentlySelectedOutput = false,
            pairingStatus = pairingStatus,
            source = AudioDeviceSource.BLUETOOTH_DISCOVERY,
            isConnectable = true,
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

            return when (deviceInfo.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Audio"
                AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing Aid"
                else -> "Bluetooth Device ${deviceInfo.id}"
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