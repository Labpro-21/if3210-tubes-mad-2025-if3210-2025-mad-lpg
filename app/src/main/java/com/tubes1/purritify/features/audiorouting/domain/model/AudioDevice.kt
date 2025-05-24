package com.tubes1.purritify.core.domain.model

import android.media.AudioDeviceInfo

data class AudioDevice(
    val id: Int,
    val name: CharSequence,
    val type: DeviceType,
    val systemType: Int,
    val isCurrentlySelectedOutput: Boolean = false
)

enum class DeviceType {
    BUILTIN_SPEAKER,
    BUILTIN_EARPIECE,
    WIRED_HEADSET,
    WIRED_HEADPHONES,
    BLUETOOTH_A2DP,
    BLUETOOTH_SCO,
    HEARING_AID,
    USB_AUDIO_DEVICE,
    UNKNOWN
}