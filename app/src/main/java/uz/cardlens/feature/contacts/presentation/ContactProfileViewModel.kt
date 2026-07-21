package uz.cardlens.feature.contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.cardlens.R
import uz.cardlens.core.data.CardLensRepository
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.supabase.EdgeAiClient

data class ContactProfileUiState(
    val contact: Contact? = null,
    val generatedMessage: String = "",
    val isGenerating: Boolean = false,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editCompany: String = "",
    val editJobTitle: String = "",
    val editEmail: String = "",
    val editPhone: String = "",
    val editNotes: String = "",
    val editLocationMet: String = "",
    val editDateMet: Long = System.currentTimeMillis(),
    val showDatePicker: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showStatusMenu: Boolean = false,
)

sealed interface ContactProfileAction {
    data class GenerateMessage(val contact: Contact) : ContactProfileAction
    data object StartEdit : ContactProfileAction
    data object CancelEdit : ContactProfileAction
    data object SaveEdit : ContactProfileAction
    data class EditName(val value: String) : ContactProfileAction
    data class EditCompany(val value: String) : ContactProfileAction
    data class EditJobTitle(val value: String) : ContactProfileAction
    data class EditEmail(val value: String) : ContactProfileAction
    data class EditPhone(val value: String) : ContactProfileAction
    data class EditNotes(val value: String) : ContactProfileAction
    data class EditLocationMet(val value: String) : ContactProfileAction
    data object ShowDatePicker : ContactProfileAction
    data class SelectDate(val millis: Long) : ContactProfileAction
    data object DismissDatePicker : ContactProfileAction
    data object RequestDelete : ContactProfileAction
    data object ConfirmDelete : ContactProfileAction
    data object DismissDelete : ContactProfileAction
    data object ToggleStatusMenu : ContactProfileAction
    data class ChangeStatus(val status: ContactStatus) : ContactProfileAction
    data object Call : ContactProfileAction
    data object Email : ContactProfileAction
    data object Copy : ContactProfileAction
}

sealed interface ContactProfileEffect {
    data class ShowSnackbar(val messageResId: Int, val formatArg: String? = null) : ContactProfileEffect
}

class ContactProfileViewModel(
    private val contactId: String,
    private val repository: CardLensRepository,
    private val edgeAiClient: EdgeAiClient,
) : ViewModel() {

    private val mutableState = MutableStateFlow(ContactProfileUiState())

    val state: StateFlow<ContactProfileUiState> = combine(
        mutableState,
        repository.observeContacts(),
    ) { local, contacts ->
        local.copy(contact = contacts.firstOrNull { it.id == contactId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactProfileUiState())

    private val _effects = Channel<ContactProfileEffect>()
    val effects = _effects.receiveAsFlow()

    fun onAction(action: ContactProfileAction) {
        when (action) {
            is ContactProfileAction.GenerateMessage -> generateMessage(action.contact)
            is ContactProfileAction.StartEdit -> startEdit()
            is ContactProfileAction.CancelEdit -> mutableState.update { it.copy(isEditing = false) }
            is ContactProfileAction.SaveEdit -> saveEdit()
            is ContactProfileAction.EditName -> mutableState.update { it.copy(editName = action.value) }
            is ContactProfileAction.EditCompany -> mutableState.update { it.copy(editCompany = action.value) }
            is ContactProfileAction.EditJobTitle -> mutableState.update { it.copy(editJobTitle = action.value) }
            is ContactProfileAction.EditEmail -> mutableState.update { it.copy(editEmail = action.value) }
            is ContactProfileAction.EditPhone -> mutableState.update { it.copy(editPhone = action.value) }
            is ContactProfileAction.EditNotes -> mutableState.update { it.copy(editNotes = action.value) }
            is ContactProfileAction.EditLocationMet -> mutableState.update { it.copy(editLocationMet = action.value) }
            is ContactProfileAction.ShowDatePicker -> mutableState.update { it.copy(showDatePicker = true) }
            is ContactProfileAction.SelectDate -> mutableState.update { it.copy(editDateMet = action.millis, showDatePicker = false) }
            is ContactProfileAction.DismissDatePicker -> mutableState.update { it.copy(showDatePicker = false) }
            is ContactProfileAction.RequestDelete -> mutableState.update { it.copy(showDeleteConfirmation = true) }
            is ContactProfileAction.ConfirmDelete -> confirmDelete()
            is ContactProfileAction.DismissDelete -> mutableState.update { it.copy(showDeleteConfirmation = false) }
            is ContactProfileAction.ToggleStatusMenu -> mutableState.update { it.copy(showStatusMenu = !it.showStatusMenu) }
            is ContactProfileAction.ChangeStatus -> changeStatus(action.status)
            is ContactProfileAction.Call -> Unit
            is ContactProfileAction.Email -> Unit
            is ContactProfileAction.Copy -> Unit
        }
    }

    private fun startEdit() {
        val c = mutableState.value.contact ?: return
        mutableState.update {
            it.copy(
                isEditing = true,
                editName = c.fullName,
                editCompany = c.company,
                editJobTitle = c.jobTitle,
                editEmail = c.email,
                editPhone = c.phone,
                editNotes = c.notes,
                editLocationMet = c.locationMet,
                editDateMet = c.dateMet,
            )
        }
    }

    private fun saveEdit() {
        val c = mutableState.value.contact ?: return
        val s = mutableState.value
        viewModelScope.launch {
            val updated = c.copy(
                fullName = s.editName.ifBlank { c.fullName },
                company = s.editCompany,
                jobTitle = s.editJobTitle,
                email = s.editEmail,
                phone = s.editPhone,
                notes = s.editNotes,
                locationMet = s.editLocationMet,
                dateMet = s.editDateMet,
                updatedAt = System.currentTimeMillis(),
            )
            repository.saveContact(updated, null)
            mutableState.update { it.copy(isEditing = false) }
            _effects.send(ContactProfileEffect.ShowSnackbar(R.string.contact_updated))
        }
    }

    private fun confirmDelete() {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            _effects.send(ContactProfileEffect.ShowSnackbar(R.string.contact_deleted))
        }
    }

    private fun changeStatus(status: ContactStatus) {
        val c = mutableState.value.contact ?: return
        viewModelScope.launch {
            repository.saveContact(c.copy(status = status, updatedAt = System.currentTimeMillis()), null)
            mutableState.update { it.copy(showStatusMenu = false) }
            _effects.send(ContactProfileEffect.ShowSnackbar(R.string.contact_status_changed, status.label))
        }
    }

    private fun generateMessage(contact: Contact) {
        viewModelScope.launch {
            mutableState.update { it.copy(isGenerating = true, generatedMessage = "") }
            val message = edgeAiClient.generateFollowUp(contact)
            mutableState.update { it.copy(isGenerating = false, generatedMessage = message) }
        }
    }
}
