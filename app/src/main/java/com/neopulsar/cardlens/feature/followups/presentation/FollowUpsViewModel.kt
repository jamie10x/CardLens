package com.neopulsar.cardlens.feature.followups.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.neopulsar.cardlens.core.data.CardLensRepository
import com.neopulsar.cardlens.core.domain.Contact
import com.neopulsar.cardlens.core.domain.FollowUp
import java.time.LocalDateTime
import java.time.ZoneId

data class FollowUpsUiState(
    val contacts: List<Contact> = emptyList(),
    val followUps: List<FollowUp> = emptyList(),
) {
    val dueToday: List<FollowUp>
        get() {
            val endOfToday = LocalDateTime.now()
                .withHour(23).withMinute(59).withSecond(59)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return followUps.filter { !it.completed && it.dueAt <= endOfToday }
        }

    val upcoming: List<FollowUp>
        get() = followUps.filter { !it.completed && it.dueAt > System.currentTimeMillis() }

    val completed: List<FollowUp>
        get() = followUps.filter { it.completed }
}

sealed interface FollowUpsAction {
    data class CompleteFollowUp(val followUp: FollowUp) : FollowUpsAction
}

class FollowUpsViewModel(
    private val repository: CardLensRepository,
) : ViewModel() {

    val state: StateFlow<FollowUpsUiState> = combine(
        repository.observeContacts(),
        repository.observeFollowUps(),
    ) { contacts, followUps ->
        FollowUpsUiState(contacts = contacts, followUps = followUps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FollowUpsUiState())

    fun onAction(action: FollowUpsAction) {
        when (action) {
            is FollowUpsAction.CompleteFollowUp -> viewModelScope.launch {
                repository.completeFollowUp(action.followUp.id, action.followUp.contactId)
            }
        }
    }
}
