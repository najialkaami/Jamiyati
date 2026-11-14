package com.roma.jamiyati.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roma.jamiyati.data.preferences.ThemeMode
import com.roma.jamiyati.data.repository.JamiyatiRepository
import com.roma.jamiyati.util.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isPinEnabled: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val repository: JamiyatiRepository
) : ViewModel() {

    private val statusMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.themeModeFlow,
        repository.isPinEnabledFlow,
        statusMessageFlow
    ) { themeMode, isPinEnabled, message ->
        SettingsUiState(
            themeMode = themeMode,
            isPinEnabled = isPinEnabled,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun enablePin(pin: String) {
        if (pin.length != 4 || !pin.all(Char::isDigit)) {
            statusMessageFlow.value = "يجب أن يتكون رمز القفل من 4 أرقام"
            return
        }
        viewModelScope.launch {
            repository.enablePin(pin)
            statusMessageFlow.value = "تم تفعيل قفل التطبيق برمز PIN بنجاح"
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            repository.disablePin()
            statusMessageFlow.value = "تم إيقاف قفل التطبيق"
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            val circles = repository.getAllCirclesOnce()
            if (circles.isEmpty()) {
                statusMessageFlow.value = "لا توجد جمعيات لتصديرها حالياً"
                return@launch
            }
            val result = BackupManager.exportAndShareBackup(context, circles)
            statusMessageFlow.value = result.fold(
                onSuccess = { "تم تجهيز النسخة الاحتياطية للمشاركة" },
                onFailure = { "تعذر تصدير النسخة الاحتياطية: ${it.localizedMessage}" }
            )
        }
    }

    fun importBackup(context: Context, uri: Uri, replaceExisting: Boolean) {
        viewModelScope.launch {
            val parsed = BackupManager.parseBackupFromUri(context, uri)
            parsed.onSuccess { payload ->
                repository.restoreFromBackup(payload, replaceExisting = replaceExisting)
                statusMessageFlow.value = "تم استيراد ${payload.circles.size} جمعية بنجاح"
            }.onFailure { err ->
                statusMessageFlow.value = "ملف النسخة الاحتياطية غير صالح: ${err.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    class Factory(private val repository: JamiyatiRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
