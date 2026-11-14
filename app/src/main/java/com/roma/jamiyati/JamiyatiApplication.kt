package com.roma.jamiyati

import android.app.Application
import com.roma.jamiyati.data.local.JamiyatiDatabase
import com.roma.jamiyati.data.preferences.AppPreferences
import com.roma.jamiyati.data.repository.JamiyatiRepository

class JamiyatiApplication : Application() {
    val database: JamiyatiDatabase by lazy {
        JamiyatiDatabase.getInstance(this)
    }

    val preferences: AppPreferences by lazy {
        AppPreferences(this)
    }

    val repository: JamiyatiRepository by lazy {
        JamiyatiRepository(
            dao = database.jamiyatiDao(),
            preferences = preferences,
            database = database
        )
    }
}
