package uz.cardlens.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.cardLensDataStore by preferencesDataStore(name = "cardlens_prefs")

class AppPreferences(private val context: Context) {

    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val languageKey = stringPreferencesKey("language")

    val isOnboardingCompleted: Flow<Boolean> = context.cardLensDataStore.data
        .map { it[onboardingCompletedKey] ?: false }

    val language: Flow<String> = context.cardLensDataStore.data
        .map { it[languageKey] ?: "en" }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.cardLensDataStore.edit { it[onboardingCompletedKey] = completed }
    }

    suspend fun setLanguage(language: String) {
        context.cardLensDataStore.edit { it[languageKey] = language }
    }

    fun languageSync(): String = runBlocking { language.first() }
}