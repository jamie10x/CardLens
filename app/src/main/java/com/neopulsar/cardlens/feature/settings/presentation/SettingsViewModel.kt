package com.neopulsar.cardlens.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.neopulsar.cardlens.core.data.CardLensRepository
import com.neopulsar.cardlens.core.datastore.AppPreferences

data class SettingsUiState(
    val isUzbek: Boolean = false,
)

sealed interface SettingsAction {
    data object ExportContacts : SettingsAction
    data object ToggleLanguage : SettingsAction
}

sealed interface SettingsEffect {
    data class ExportCsv(val csvContent: String) : SettingsEffect
}

class SettingsViewModel(
    private val repository: CardLensRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>()
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                val lang = try { appPreferences.language.first() } catch (_: Exception) { "en" }
                _state.update { it.copy(isUzbek = lang == "uz") }
            } catch (_: Exception) {}
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ExportContacts -> exportContacts()
            is SettingsAction.ToggleLanguage -> toggleLanguage()
        }
    }

    private fun exportContacts() {
        viewModelScope.launch {
            try {
                val contacts = try { repository.observeContacts().first() } catch (_: Exception) { emptyList() }
                val csv = buildString {
                    appendLine("Name,Company,Job Title,Email,Phone,Status,Date Met,Location Met,Notes")
                    contacts.forEach { c ->
                        try {
                            appendLine(
                                listOf(
                                    c.fullName, c.company, c.jobTitle, c.email,
                                    c.phone, c.status.label, c.dateMet.toString(),
                                    c.locationMet, c.notes,
                                ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                            )
                        } catch (_: Exception) {}
                    }
                }
                _effects.emit(SettingsEffect.ExportCsv(csv))
            } catch (_: Exception) {
                try { _effects.emit(SettingsEffect.ExportCsv("Name,Company\n")) } catch (_: Exception) {}
            }
        }
    }

    private fun toggleLanguage() {
        viewModelScope.launch {
            try {
                val current = try { appPreferences.language.first() } catch (_: Exception) { "en" }
                val newLang = if (current == "en") "uz" else "en"
                try { appPreferences.setLanguage(newLang) } catch (_: Exception) {}
                _state.update { it.copy(isUzbek = newLang == "uz") }
            } catch (_: Exception) {}
        }
    }
}
