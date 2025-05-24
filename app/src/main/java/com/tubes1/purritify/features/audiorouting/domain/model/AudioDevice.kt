package com.tubes1.purritify.features.audiorouting.domain.model

import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice // Alias for clarity
import android.media.AudioDeviceInfo

data class AudioDevice(
    val systemApiId: Int?,
    val name: CharSequence,
    val type: DeviceType, // Your app's abstracted device type
    val systemDeviceType: Int, // Original system type from AudioDeviceInfo or BluetoothClass
    val address: String?, // MAC address for BT devices, crucial for identification and pairing
    var isCurrentlySelectedOutput: Boolean = false, // Is this the actively routed output device?
    var pairingStatus: PairingStatus = PairingStatus.NONE, // Pairing status for Bluetooth devices
    val source: AudioDeviceSource, // Where did this device info come from?
    val isConnectable: Boolean = true, // Can the app attempt to connect/select this device?
    @Transient val underlyingSystemApiDevice: AudioDeviceInfo? = null, // Original AudioDeviceInfo if source is SYSTEM_API
    @Transient val underlyingBluetoothDevice: AndroidBluetoothDevice? = null // Original BluetoothDevice if BT
) {



    val uniqueKey: String
        get() = address ?: "system_${systemApiId}_${systemDeviceType}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioDevice
        return uniqueKey == other.uniqueKey
    }

    override fun hashCode(): Int {
        return uniqueKey.hashCode()
    }
}