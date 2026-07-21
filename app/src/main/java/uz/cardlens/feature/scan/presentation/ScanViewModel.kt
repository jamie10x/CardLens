package uz.cardlens.feature.scan.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.cardlens.core.common.AppResult
import uz.cardlens.core.data.CardLensRepository
import uz.cardlens.core.domain.ContactDraft
import uz.cardlens.core.domain.FollowUp
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.notifications.ReminderScheduler
import uz.cardlens.core.ocr.MlKitOcrProcessor
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

enum class ReminderPreset(val label: String) {
    LaterToday("Later today"),
    Tomorrow("Tomorrow"),
    ThreeDays("In 3 days"),
    NextWeek("Next week"),
}

data class ScanUiState(
    val isProcessing: Boolean = false,
    val reviewDraft: ContactDraft? = null,
    val rawOcrText: String = "",
    val snackbar: String? = null,
    val selectedReminderPreset: ReminderPreset? = null,
)

sealed interface ScanAction {
    data class ProcessCardImage(val uri: Uri) : ScanAction
    data class UpdateDraft(val draft: ContactDraft) : ScanAction
    data class SelectReminderPreset(val preset: ReminderPreset) : ScanAction
    data object SaveReviewedContact : ScanAction
    data object RetakeScan : ScanAction
    data object ClearSnackbar : ScanAction
}

sealed interface ScanEffect {
    data class ContactSaved(val contactId: String) : ScanEffect
}

class ScanViewModel(
    private val ocrProcessor: MlKitOcrProcessor,
    private val repository: CardLensRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ScanEffect>()
    val effect: SharedFlow<ScanEffect> = _effect.asSharedFlow()

    fun onAction(action: ScanAction) {
        when (action) {
            is ScanAction.ProcessCardImage -> processCard(action.uri)
            is ScanAction.UpdateDraft -> _state.update { it.copy(reviewDraft = action.draft) }
            is ScanAction.SelectReminderPreset -> applyReminder(action.preset)
            is ScanAction.SaveReviewedContact -> saveContact()
            is ScanAction.RetakeScan -> _state.update {
                ScanUiState()
            }
            is ScanAction.ClearSnackbar -> _state.update { it.copy(snackbar = null) }
        }
    }

    private fun processCard(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, snackbar = "Reading business card...") }
            when (val result = ocrProcessor.readCard(uri)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        reviewDraft = result.data.draft,
                        rawOcrText = result.data.rawText,
                        isProcessing = false,
                        snackbar = null,
                    )
                }
                is AppResult.Error -> _state.update {
                    it.copy(
                        reviewDraft = ContactDraft(cardImageUri = uri.toString()),
                        rawOcrText = "",
                        isProcessing = false,
                        snackbar = "Could not read the card automatically.",
                    )
                }
            }
        }
    }

    private fun applyReminder(preset: ReminderPreset) {
        val dueAt = when (preset) {
            ReminderPreset.LaterToday -> LocalDateTime.now().plusHours(4)
            ReminderPreset.Tomorrow -> LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
            ReminderPreset.ThreeDays -> LocalDateTime.now().plusDays(3).withHour(9).withMinute(0)
            ReminderPreset.NextWeek -> LocalDateTime.now().plusWeeks(1).withHour(9).withMinute(0)
        }.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        _state.update { state ->
            val draft = state.reviewDraft ?: ContactDraft()
            state.copy(
                selectedReminderPreset = preset,
                reviewDraft = draft.copy(
                    followUpDueAt = dueAt,
                    followUpTitle = draft.followUpTitle.ifBlank {
                        "Follow up with ${draft.fullName.ifBlank { "new contact" }}"
                    }
                )
            )
        }
    }

    private fun saveContact() {
        val draft = _state.value.reviewDraft ?: return
        viewModelScope.launch {
            val contact = draft.toContact()
            val followUp = draft.followUpDueAt?.let { dueAt ->
                FollowUp(
                    id = UUID.randomUUID().toString(),
                    contactId = contact.id,
                    title = draft.followUpTitle.ifBlank { "Follow up with ${contact.fullName}" },
                    dueAt = dueAt,
                )
            }
            when (repository.saveContact(contact, followUp)) {
                is AppResult.Success -> {
                    followUp?.let { reminderScheduler.schedule(it) }
                    _effect.emit(ScanEffect.ContactSaved(contact.id))
                }
                is AppResult.Error -> _state.update { it.copy(snackbar = "Could not save contact") }
            }
        }
    }
}
