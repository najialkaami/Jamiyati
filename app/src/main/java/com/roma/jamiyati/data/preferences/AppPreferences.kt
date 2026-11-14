package com.roma.jamiyati.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jamiyati_prefs")

enum class ThemeMode(val labelAr: String) {
    SYSTEM("تلقائي حسب النظام"),
    LIGHT("الوضع الفاتح"),
    DARK("الوضع الداكن")
}

class AppPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    val isPinEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PIN_ENABLED] ?: false
    }

    val pinHashFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.PIN_HASH]
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun enablePin(pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = hash
            prefs[Keys.PIN_ENABLED] = true
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = false
            prefs.remove(Keys.PIN_HASH)
        }
    }

    companion object {
        fun hashPin(pin: String): String {
            val salted = "com.roma.jamiyati::$pin"
            val bytes = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
