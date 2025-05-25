package com.tubes1.purritify.features.audiorouting.domain.model

import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.media.AudioDeviceInfo

data class AudioDevice(
    val systemApiId: Int?,
    val name: CharSequence,
    val type: DeviceType,
    val systemDeviceType: Int,
    val address: String?,
    var isCurrentlySelectedOutput: Boolean = false,
    var pairingStatus: PairingStatus = PairingStatus.NONE,
    val source: AudioDeviceSource,
    val isConnectable: Boolean = true,
    @Transient val underlyingSystemApiDevice: AudioDeviceInfo? = null,
    @Transient val underlyingBluetoothDevice: AndroidBluetoothDevice? = null
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