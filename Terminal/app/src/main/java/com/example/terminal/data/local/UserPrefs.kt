package com.example.terminal.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.terminal.data.network.ApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATA_STORE_NAME = "user_prefs"
private val LAST_EMPLOYEE_KEY = stringPreferencesKey("last_employee_id")
private val SERVER_ADDRESS_KEY = stringPreferencesKey("server_address")
private val BEEP_VOLUME_KEY = floatPreferencesKey("beep_volume")

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

class UserPrefs private constructor(private val appContext: Context) {

    val lastEmployeeId: Flow<String?>
        get() = appContext.dataStore.data.map { preferences ->
            preferences[LAST_EMPLOYEE_KEY]
        }

    val serverAddress: Flow<String>
        get() = appContext.dataStore.data.map { preferences ->
            val savedAddress = preferences[SERVER_ADDRESS_KEY] ?: ApiClient.DEFAULT_BASE_URL
            ApiClient.normalizeBaseUrl(savedAddress)
        }

    val beepVolume: Flow<Float>
        get() = appContext.dataStore.data.map { preferences ->
            preferences[BEEP_VOLUME_KEY] ?: 1f
        }

    suspend fun saveLastEmployeeId(employeeId: String) {
        appContext.dataStore.edit { preferences ->
            preferences[LAST_EMPLOYEE_KEY] = employeeId
        }
    }

    suspend fun saveServerAddress(address: String) {
        val normalizedAddress = ApiClient.normalizeBaseUrl(address)
        appContext.dataStore.edit { preferences ->
            preferences[SERVER_ADDRESS_KEY] = normalizedAddress
        }
    }

    suspend fun saveBeepVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        appContext.dataStore.edit { preferences ->
            preferences[BEEP_VOLUME_KEY] = clampedVolume
        }
    }

    companion object {
        fun create(context: Context): UserPrefs {
            return UserPrefs(context.applicationContext)
        }
    }
}
