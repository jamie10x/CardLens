package uz.cardlens.core.datastore

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cardlens_prefs", Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(ONBOARDING_KEY, false)
        set(value) = prefs.edit().putBoolean(ONBOARDING_KEY, value).apply()

    var language: String
        get() = prefs.getString(LANGUAGE_KEY, "en") ?: "en"
        set(value) = prefs.edit().putString(LANGUAGE_KEY, value).apply()

    companion object {
        private const val ONBOARDING_KEY = "onboarding_completed"
        private const val LANGUAGE_KEY = "language"
    }
}
