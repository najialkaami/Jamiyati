package com.roma.jamiyati

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roma.jamiyati.data.preferences.ThemeMode
import com.roma.jamiyati.ui.navigation.JamiyatiNavGraph
import com.roma.jamiyati.ui.screens.PinLockScreen
import com.roma.jamiyati.ui.theme.JamiyatiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as JamiyatiApplication

        setContent {
            val themeMode by app.preferences.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            val isPinEnabled by app.preferences.isPinEnabledFlow.collectAsStateWithLifecycle(
                initialValue = false
            )
            val pinHash by app.preferences.pinHashFlow.collectAsStateWithLifecycle(
                initialValue = null
            )

            var isUnlocked by rememberSaveable { mutableStateOf(false) }

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                JamiyatiTheme(darkTheme = darkTheme) {
                    if (isPinEnabled && !pinHash.isNullOrBlank() && !isUnlocked) {
                        PinLockScreen(
                            expectedPinHash = pinHash!!,
                            onUnlocked = { isUnlocked = true }
                        )
                    } else {
                        JamiyatiNavGraph(repository = app.repository)
                    }
                }
            }
        }
    }
}
