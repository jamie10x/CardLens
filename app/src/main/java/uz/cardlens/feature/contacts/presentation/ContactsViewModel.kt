package uz.cardlens.feature.contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.cardlens.core.data.CardLensRepository
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.domain.ContactStatus

data class ContactsUiState(
    val allContacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: ContactStatus? = null,
    val showDeleteConfirmation: Boolean = false,
    val contactToDelete: Contact? = null,
) {
    val visibleContacts: List<Contact>
        get() {
            val query = searchQuery.trim().lowercase()
            return allContacts.filter { contact ->
                val matchesQuery = query.isBlank() ||
                    listOf(
                        contact.fullName, contact.company, contact.jobTitle,
                        contact.email, contact.phone, contact.notes,
                        contact.locationMet,
                        contact.tags.joinToString(" ") { it.name },
                    ).any { it.lowercase().contains(query) }
                val matchesFilter = statusFilter == null || contact.status == statusFilter
                matchesQuery && matchesFilter
            }
        }
}

sealed interface ContactsAction {
    data class Search(val query: String) : ContactsAction
    data class FilterByStatus(val status: ContactStatus?) : ContactsAction
    data class RequestDelete(val contact: Contact) : ContactsAction
    data object ConfirmDelete : ContactsAction
    data object DismissDelete : ContactsAction
}

class ContactsViewModel(
    private val repository: CardLensRepository,
) : ViewModel() {

    private val transientState = MutableStateFlow(ContactsUiState())

    val state: StateFlow<ContactsUiState> = combine(
        transientState,
        repository.observeContacts(),
    ) { local, contacts ->
        local.copy(allContacts = contacts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsUiState())

    fun onAction(action: ContactsAction) {
        when (action) {
            is ContactsAction.Search -> transientState.update { it.copy(searchQuery = action.query) }
            is ContactsAction.FilterByStatus -> transientState.update { it.copy(statusFilter = action.status) }
            is ContactsAction.RequestDelete -> transientState.update {
                it.copy(showDeleteConfirmation = true, contactToDelete = action.contact)
            }
            is ContactsAction.ConfirmDelete -> confirmDelete()
            is ContactsAction.DismissDelete -> transientState.update {
                it.copy(showDeleteConfirmation = false, contactToDelete = null)
            }
        }
    }

    private fun confirmDelete() {
        val contact = transientState.value.contactToDelete ?: return
        viewModelScope.launch {
            repository.deleteContact(contact.id)
            transientState.update { it.copy(showDeleteConfirmation = false, contactToDelete = null) }
        }
    }
}
