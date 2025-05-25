package com.tubes1.purritify.core.data.local.preferences

import com.tubes1.purritify.features.audiorouting.domain.model.AudioDevice
import com.tubes1.purritify.features.audiorouting.domain.model.DeviceType

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDeviceSource
import com.tubes1.purritify.features.audiorouting.domain.model.PairingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "purritify_user_prefs")

internal data class StoredAudioDevicePreference(
    val systemApiId: Int?,
    val name: String?,
    val typeName: String?,
    val systemDeviceType: Int?,
    val address: String?,
    val sourceName: String?
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {

        val PREFERRED_AUDIO_DEVICE_SYSTEM_API_ID = intPreferencesKey("preferred_audio_device_system_api_id")
        val PREFERRED_AUDIO_DEVICE_NAME = stringPreferencesKey("preferred_audio_device_name")
        val PREFERRED_AUDIO_DEVICE_TYPE_NAME = stringPreferencesKey("preferred_audio_device_type_name")
        val PREFERRED_AUDIO_DEVICE_SYSTEM_TYPE = intPreferencesKey("preferred_audio_device_system_type")
        val PREFERRED_AUDIO_DEVICE_ADDRESS = stringPreferencesKey("preferred_audio_device_address")

        val PREFERRED_AUDIO_DEVICE_SOURCE_NAME = stringPreferencesKey("preferred_audio_device_source_name")
    }

    val preferredAudioDeviceFlow: Flow<AudioDevice?> = context.dataStore.data
        .map { preferences ->

            val systemApiId = preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_SYSTEM_API_ID]
            val name = preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_NAME]
            val typeName = preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_TYPE_NAME]
            val systemDeviceType = preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_SYSTEM_TYPE]
            val address = preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_ADDRESS]
            val sourceName = preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_SOURCE_NAME]

            if (name != null && typeName != null && systemDeviceType != null && sourceName != null && (systemApiId != null || address != null)) {
                val storedPrefs = StoredAudioDevicePreference(
                    systemApiId = systemApiId,
                    name = name,
                    typeName = typeName,
                    systemDeviceType = systemDeviceType,
                    address = address,
                    sourceName = sourceName
                )
                mapStoredPrefsToAudioDevice(storedPrefs)
            } else {

                null
            }
        }.distinctUntilChanged()

    suspend fun savePreferredAudioDevice(device: AudioDevice?) {
        context.dataStore.edit { preferences ->
            if (device != null) {

                if (device.systemApiId != null) {
                    preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_SYSTEM_API_ID] = device.systemApiId
                } else {
                    preferences.remove(PreferencesKeys.PREFERRED_AUDIO_DEVICE_SYSTEM_API_ID)
                }

                preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_NAME] = device.name.toString()
                preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_TYPE_NAME] = device.type.name
                preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_SYSTEM_TYPE] = device.systemDeviceType
                preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_SOURCE_NAME] = device.source.name

                if (device.address != null) {
                    preferences[PreferencesKeys.PREFERRED_AUDIO_DEVICE_ADDRESS] = device.address
                } else {
                    preferences.remove(PreferencesKeys.PREFERRED_AUDIO_DEVICE_ADDRESS)
                }
            } else {

                preferences.remove(PreferencesKeys.PREFERRED_AUDIO_DEVICE_SYSTEM_API_ID)
                preferences.remove(PreferencesKeys.PREFERRED_AUDIO_DEVICE_NAME)
                preferences.remove(PreferencesKeys.PREFERRED_AUDIO_DEVICE_TYPE_NAME)
                preferences.remove(PreferencesKeys.PREFERRED_AUDIO_DEVICE_SYSTEM_TYPE)
                preferences.remove(PreferencesKeys.PREFERRED_AUDIO_DEVICE_ADDRESS)
                preferences.remove(PreferencesKeys.PREFERRED_AUDIO_DEVICE_SOURCE_NAME)
            }
        }
    }

    private fun mapStoredPrefsToAudioDevice(prefs: StoredAudioDevicePreference?): AudioDevice? {
        if (prefs == null) return null


        if (prefs.name == null || prefs.typeName == null || prefs.systemDeviceType == null || prefs.sourceName == null) {
            return null
        }
        if (prefs.systemApiId == null && prefs.address == null) {
            return null
        }

        return try {
            val appDeviceType = DeviceType.valueOf(prefs.typeName)
            val appDeviceSource = AudioDeviceSource.valueOf(prefs.sourceName)


            val pairingStatus = if (appDeviceType == DeviceType.BLUETOOTH_A2DP ||
                appDeviceType == DeviceType.BLUETOOTH_SCO ||
                appDeviceType == DeviceType.HEARING_AID) {
                PairingStatus.PAIRED
            } else {
                PairingStatus.NONE
            }

            AudioDevice(
                systemApiId = prefs.systemApiId,
                name = prefs.name,
                type = appDeviceType,
                systemDeviceType = prefs.systemDeviceType,
                address = prefs.address,
                source = appDeviceSource,
                pairingStatus = pairingStatus,

                isCurrentlySelectedOutput = false,
                isConnectable = true,
                underlyingSystemApiDevice = null,
                underlyingBluetoothDevice = null
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}