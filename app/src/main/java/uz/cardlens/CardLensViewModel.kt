package uz.cardlens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.cardlens.core.common.AppResult
import uz.cardlens.core.data.CardLensRepository
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.domain.ContactDraft
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.domain.FollowUp
import uz.cardlens.core.notifications.ReminderScheduler
import uz.cardlens.core.ocr.MlKitOcrProcessor
import uz.cardlens.core.supabase.AuthError
import uz.cardlens.core.supabase.AuthRepository
import uz.cardlens.core.supabase.EdgeAiClient
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

enum class MainTab {
    Home,
    Contacts,
    Scan,
    FollowUps,
    Settings
}

data class CardLensState(
    val isSignedIn: Boolean = false,
    val isAuthConfigured: Boolean = false,
    val isAuthLoading: Boolean = false,
    val isDemoMode: Boolean = false,
    val userEmail: String = "",
    val activeTab: MainTab = MainTab.Home,
    val contacts: List<Contact> = emptyList(),
    val followUps: List<FollowUp> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: ContactStatus? = null,
    val selectedContactId: String? = null,
    val reviewDraft: ContactDraft? = null,
    val rawOcrText: String = "",
    val isProcessingScan: Boolean = false,
    val generatedMessage: String = "",
    val isGeneratingMessage: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long? = null,
    val snackbar: String? = null,
) {
    val selectedContact: Contact?
        get() = contacts.firstOrNull { it.id == selectedContactId }

    val visibleContacts: List<Contact>
        get() {
            val query = searchQuery.trim().lowercase()
            return contacts.filter { contact ->
                val matchesQuery = query.isBlank() ||
                    listOf(
                        contact.fullName,
                        contact.company,
                        contact.jobTitle,
                        contact.email,
                        contact.phone,
                        contact.notes,
                        contact.locationMet,
                        contact.tags.joinToString(" ") { it.name },
                    ).any { it.lowercase().contains(query) }
                val matchesFilter = (statusFilter == null) || (contact.status == statusFilter)
                matchesQuery && matchesFilter
            }
        }

    val dueToday: List<FollowUp>
        get() {
            val endOfToday = LocalDateTime.now()
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            return followUps.filter { !it.completed && (it.dueAt <= endOfToday) }
        }

    val upcomingFollowUps: List<FollowUp>
        get() = followUps.filter { !it.completed && (it.dueAt > System.currentTimeMillis()) }

    val completedFollowUps: List<FollowUp>
        get() = followUps.filter { it.completed }
}

sealed interface CardLensAction {
    data class SignIn(val email: String, val password: String) : CardLensAction
    data class SignUp(val email: String, val password: String) : CardLensAction
    data object ContinueDemo : CardLensAction
    data object SignOut : CardLensAction
    data object SyncNow : CardLensAction
    data class SelectTab(val tab: MainTab) : CardLensAction
    data class ProcessCardImage(val uri: Uri) : CardLensAction
    data class UpdateDraft(val draft: ContactDraft) : CardLensAction
    data class SelectReminderPreset(val preset: ReminderPreset) : CardLensAction
    data object SaveReviewedContact : CardLensAction
    data object RetakeScan : CardLensAction
    data class SelectContact(val contactId: String) : CardLensAction
    data object BackToContacts : CardLensAction
    data class SearchContacts(val query: String) : CardLensAction
    data class FilterByStatus(val status: ContactStatus?) : CardLensAction
    data class GenerateMessage(val contact: Contact) : CardLensAction
    data class CompleteFollowUp(val followUp: FollowUp) : CardLensAction
    data object ClearSnackbar : CardLensAction
}

enum class ReminderPreset(val label: String) {
    LaterToday("Later today"),
    Tomorrow("Tomorrow"),
    ThreeDays("In 3 days"),
    NextWeek("Next week"),
}

class CardLensViewModel(
    private val repository: CardLensRepository,
    private val authRepository: AuthRepository,
    private val ocrProcessor: MlKitOcrProcessor,
    private val edgeAiClient: EdgeAiClient,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val transientState = MutableStateFlow(CardLensState())

    val state = combine(
        transientState,
        repository.observeContacts(),
        repository.observeFollowUps(),
    ) { localState, contacts, followUps ->
        localState.copy(contacts = contacts, followUps = followUps)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CardLensState(),
    )

    init {
        viewModelScope.launch {
            repository.seedDemoDataIfNeeded()
            transientState.update { it.copy(isAuthConfigured = authRepository.isConfigured) }
            when (val result = authRepository.restoreSession()) {
                is AppResult.Success -> {
                    val user = result.data
                    if (user != null) {
                        repository.setActiveOwner(user.id)
                        repository.syncNow()
                        transientState.update {
                            it.copy(
                                isSignedIn = true,
                                isDemoMode = user.isDemo,
                                userEmail = user.email,
                                isAuthConfigured = authRepository.isConfigured,
                                snackbar = "Welcome back"
                            )
                        }
                    }
                }
                is AppResult.Error -> transientState.update {
                    it.copy(
                        isAuthConfigured = authRepository.isConfigured,
                        snackbar = "Could not restore Supabase session"
                    )
                }
            }
        }
    }

    fun onAction(action: CardLensAction) {
        when (action) {
            is CardLensAction.SignIn -> signIn(action.email, action.password)
            is CardLensAction.SignUp -> signUp(action.email, action.password)
            CardLensAction.ContinueDemo -> continueDemo()
            CardLensAction.SignOut -> signOut()
            CardLensAction.SyncNow -> syncNow()
            is CardLensAction.SelectTab -> transientState.update {
                it.copy(activeTab = action.tab, selectedContactId = null, generatedMessage = "")
            }
            is CardLensAction.ProcessCardImage -> processCardImage(action.uri)
            is CardLensAction.UpdateDraft -> transientState.update { it.copy(reviewDraft = action.draft) }
            is CardLensAction.SelectReminderPreset -> applyReminderPreset(action.preset)
            CardLensAction.SaveReviewedContact -> saveReviewedContact()
            CardLensAction.RetakeScan -> transientState.update {
                it.copy(reviewDraft = null, rawOcrText = "", generatedMessage = "", activeTab = MainTab.Scan)
            }
            is CardLensAction.SelectContact -> transientState.update {
                it.copy(selectedContactId = action.contactId, activeTab = MainTab.Contacts, generatedMessage = "")
            }
            CardLensAction.BackToContacts -> transientState.update {
                it.copy(selectedContactId = null, generatedMessage = "")
            }
            is CardLensAction.SearchContacts -> transientState.update { it.copy(searchQuery = action.query) }
            is CardLensAction.FilterByStatus -> transientState.update { it.copy(statusFilter = action.status) }
            is CardLensAction.GenerateMessage -> generateMessage(action.contact)
            is CardLensAction.CompleteFollowUp -> completeFollowUp(action.followUp)
            CardLensAction.ClearSnackbar -> transientState.update { it.copy(snackbar = null) }
        }
    }

    private fun signIn(email: String, password: String) {
        viewModelScope.launch {
            transientState.update { it.copy(isAuthLoading = true) }
            when (val result = authRepository.signIn(email, password)) {
                is AppResult.Success -> {
                    val user = result.data
                    repository.setActiveOwner(user.id)
                    repository.syncNow()
                    transientState.update {
                        it.copy(
                            isSignedIn = true,
                            isAuthLoading = false,
                            isDemoMode = false,
                            userEmail = user.email,
                            snackbar = "Signed in as ${user.email}"
                        )
                    }
                }
                is AppResult.Error -> transientState.update {
                    it.copy(isAuthLoading = false, snackbar = result.error.toMessage())
                }
            }
        }
    }

    private fun signUp(email: String, password: String) {
        viewModelScope.launch {
            transientState.update { it.copy(isAuthLoading = true) }
            when (val result = authRepository.signUp(email, password)) {
                is AppResult.Success -> {
                    val user = result.data
                    if (user.needsEmailConfirmation) {
                        transientState.update {
                            it.copy(
                                isAuthLoading = false,
                                snackbar = "Check your email to confirm your account."
                            )
                        }
                    } else {
                        repository.setActiveOwner(user.id)
                        repository.syncNow()
                        transientState.update {
                            it.copy(
                                isSignedIn = true,
                                isAuthLoading = false,
                                isDemoMode = false,
                                userEmail = user.email,
                                snackbar = "Account created"
                            )
                        }
                    }
                }
                is AppResult.Error -> transientState.update {
                    it.copy(isAuthLoading = false, snackbar = result.error.toMessage())
                }
            }
        }
    }

    private fun continueDemo() {
        viewModelScope.launch {
            val user = authRepository.continueDemo()
            repository.setActiveOwner(user.id)
            repository.seedDemoDataIfNeeded()
            transientState.update {
                it.copy(
                    isSignedIn = true,
                    isDemoMode = true,
                    userEmail = user.email,
                    snackbar = "Demo mode uses local data only"
                )
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            repository.setActiveOwner("demo")
            transientState.update {
                CardLensState(isAuthConfigured = authRepository.isConfigured)
            }
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            transientState.update { it.copy(isSyncing = true) }
            when (repository.syncNow()) {
                is AppResult.Success -> transientState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncedAt = System.currentTimeMillis(),
                        snackbar = "Supabase sync complete"
                    )
                }
                is AppResult.Error -> transientState.update {
                    it.copy(
                        isSyncing = false,
                        snackbar = if (it.isDemoMode) "Demo mode is local only" else "Could not sync with Supabase"
                    )
                }
            }
        }
    }

    private fun processCardImage(uri: Uri) {
        viewModelScope.launch {
            transientState.update {
                it.copy(
                    isProcessingScan = true,
                    activeTab = MainTab.Scan,
                    snackbar = "Reading business card..."
                )
            }
            when (val result = ocrProcessor.readCard(uri)) {
                is AppResult.Success -> transientState.update {
                    it.copy(
                        reviewDraft = result.data.draft,
                        rawOcrText = result.data.rawText,
                        isProcessingScan = false,
                        snackbar = "OCR complete. Review the details before saving."
                    )
                }
                is AppResult.Error -> transientState.update {
                    it.copy(
                        reviewDraft = ContactDraft(cardImageUri = uri.toString()),
                        rawOcrText = "",
                        isProcessingScan = false,
                        snackbar = "Could not read the card automatically. You can still add details manually."
                    )
                }
            }
        }
    }

    private fun applyReminderPreset(preset: ReminderPreset) {
        val dueAt = when (preset) {
            ReminderPreset.LaterToday -> LocalDateTime.now().plusHours(4)
            ReminderPreset.Tomorrow -> LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
            ReminderPreset.ThreeDays -> LocalDateTime.now().plusDays(3).withHour(9).withMinute(0)
            ReminderPreset.NextWeek -> LocalDateTime.now().plusWeeks(1).withHour(9).withMinute(0)
        }.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        transientState.update { state ->
            val draft = state.reviewDraft ?: ContactDraft()
            state.copy(
                reviewDraft = draft.copy(
                    followUpDueAt = dueAt,
                    followUpTitle = draft.followUpTitle.ifBlank {
                        "Follow up with ${draft.fullName.ifBlank { "new contact" }}"
                    }
                )
            )
        }
    }

    private fun saveReviewedContact() {
        val draft = transientState.value.reviewDraft ?: return
        viewModelScope.launch {
            val contact = draft.toContact()
            val followUp = draft.followUpDueAt?.let { dueAt ->
                FollowUp(
                    id = UUID.randomUUID().toString(),
                    contactId = contact.id,
                    title = draft.followUpTitle.ifBlank { "Follow up with ${contact.fullName}" },
                    dueAt = dueAt
                )
            }
            when (repository.saveContact(contact, followUp)) {
                is AppResult.Success -> {
                    followUp?.let {
                reminderScheduler.schedule(it)
            }
                    transientState.update {
                        it.copy(
                            reviewDraft = null,
                            rawOcrText = "",
                            activeTab = MainTab.Contacts,
                            selectedContactId = contact.id,
                            snackbar = "Contact saved"
                        )
                    }
                }
                is AppResult.Error -> transientState.update {
                    it.copy(snackbar = "Could not save contact")
                }
            }
        }
    }

    private fun generateMessage(contact: Contact) {
        viewModelScope.launch {
            transientState.update { it.copy(isGeneratingMessage = true, generatedMessage = "") }
            val message = edgeAiClient.generateFollowUp(contact)
            transientState.update {
                it.copy(
                    isGeneratingMessage = false,
                    generatedMessage = message,
                    snackbar = "Follow-up message generated"
                )
            }
        }
    }

    private fun completeFollowUp(followUp: FollowUp) {
        viewModelScope.launch {
            repository.completeFollowUp(followUp.id, followUp.contactId)
            transientState.update { it.copy(snackbar = "Follow-up completed") }
        }
    }
}

private fun AuthError.toMessage(): String {
    return when (this) {
        AuthError.NOT_CONFIGURED -> "Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties first."
        AuthError.INVALID_INPUT -> "Enter a valid email and a password with at least 6 characters."
        AuthError.EMAIL_CONFIRMATION_REQUIRED -> "Check your email to confirm your account."
        AuthError.UNKNOWN -> "Authentication failed. Check your credentials and Supabase settings."
    }
}
