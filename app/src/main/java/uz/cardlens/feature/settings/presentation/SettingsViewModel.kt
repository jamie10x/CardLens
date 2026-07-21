package uz.cardlens.feature.settings.presentation

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
import uz.cardlens.R
import uz.cardlens.core.common.AppResult
import uz.cardlens.core.data.CardLensRepository
import uz.cardlens.core.datastore.AppPreferences
import uz.cardlens.core.supabase.AuthRepository

data class SettingsUiState(
    val isDemoMode: Boolean = false,
    val userEmail: String = "",
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long? = null,
    val isAuthConfigured: Boolean = false,
    val isUzbek: Boolean = false,
    val snackbarResId: Int? = null,
    val showSignOutConfirmation: Boolean = false,
)

sealed interface SettingsAction {
    data object SyncNow : SettingsAction
    data object SignOut : SettingsAction
    data object ConfirmSignOut : SettingsAction
    data object DismissSignOut : SettingsAction
    data object ExportContacts : SettingsAction
    data object ClearSnackbar : SettingsAction
    data object ToggleLanguage : SettingsAction
}

sealed interface SettingsEffect {
    data object SignedOut : SettingsEffect
    data class ShowSnackbar(val messageResId: Int) : SettingsEffect
    data class ExportCsv(val csvContent: String) : SettingsEffect
}

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val repository: CardLensRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>()
    val effects = _effects.asSharedFlow()

    init {
        val user = authRepository.currentUser.value
        _state.update {
            it.copy(
                isDemoMode = user?.isDemo == true,
                userEmail = user?.email ?: "",
                isAuthConfigured = authRepository.isConfigured,
                isUzbek = appPreferences.language == "uz",
            )
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SyncNow -> syncNow()
            is SettingsAction.SignOut -> _state.update { it.copy(showSignOutConfirmation = true) }
            is SettingsAction.ConfirmSignOut -> signOut()
            is SettingsAction.DismissSignOut -> _state.update { it.copy(showSignOutConfirmation = false) }
            is SettingsAction.ExportContacts -> exportContacts()
            is SettingsAction.ClearSnackbar -> _state.update { it.copy(snackbarResId = null) }
            is SettingsAction.ToggleLanguage -> {
                val newLang = if (appPreferences.language == "en") "uz" else "en"
                appPreferences.language = newLang
                _state.update { it.copy(isUzbek = newLang == "uz") }
            }
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

    private fun syncNow() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            when (repository.syncNow()) {
                is AppResult.Success -> _state.update {
                    it.copy(isSyncing = false, lastSyncedAt = System.currentTimeMillis(), snackbarResId = R.string.settings_sync_complete)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isSyncing = false, snackbarResId = if (it.isDemoMode) R.string.settings_demo_local else R.string.settings_sync_error)
                }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            repository.setActiveOwner("demo")
            _effects.emit(SettingsEffect.SignedOut)
        }
    }
}
