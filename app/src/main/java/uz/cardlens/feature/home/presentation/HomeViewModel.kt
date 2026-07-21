package uz.cardlens.feature.home.presentation

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
import uz.cardlens.core.domain.FollowUp
import uz.cardlens.core.supabase.AuthRepository
import java.time.LocalDateTime
import java.time.ZoneId

data class HomeUiState(
    val userName: String = "",
    val contacts: List<Contact> = emptyList(),
    val dueToday: List<FollowUp> = emptyList(),
    val upcomingCount: Int = 0,
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: CardLensRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        repository.observeContacts(),
        repository.observeFollowUps(),
    ) { contacts, followUps ->
        val endOfToday = LocalDateTime.now()
            .withHour(23).withMinute(59).withSecond(59)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val due = followUps.filter { !it.completed && it.dueAt <= endOfToday }
        val upcoming = followUps.count { !it.completed && it.dueAt > System.currentTimeMillis() }
        val email = authRepository.currentUser.value?.email ?: ""
        HomeUiState(
            userName = email.takeWhile { it != '@' }.ifBlank { "Networker" },
            contacts = contacts,
            dueToday = due,
            upcomingCount = upcoming,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            repository.seedDemoDataIfNeeded()
        }
    }
}
