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
            _state.update {
                it.copy(isUzbek = appPreferences.language.first() == "uz")
            }
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
            val contacts = repository.observeContacts().first()
            val csv = buildString {
                appendLine("Name,Company,Job Title,Email,Phone,Status,Date Met,Location Met,Notes")
                contacts.forEach { c ->
                    appendLine(
                        listOf(
                            c.fullName, c.company, c.jobTitle, c.email,
                            c.phone, c.status.label, c.dateMet.toString(),
                            c.locationMet, c.notes,
                        ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                    )
                }
            }
            _effects.emit(SettingsEffect.ExportCsv(csv))
        }
    }

    private fun toggleLanguage() {
        viewModelScope.launch {
            val newLang = if (appPreferences.language.first() == "en") "uz" else "en"
            appPreferences.setLanguage(newLang)
            _state.update { it.copy(isUzbek = newLang == "uz") }
        }
    }
}
