package uz.cardlens.feature.followups.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import uz.cardlens.core.ui.components.EmptyText
import uz.cardlens.core.ui.components.FollowUpCard
import uz.cardlens.core.ui.components.SectionHeader

@Composable
fun FollowUpsRoute(
    viewModel: FollowUpsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    FollowUpsScreen(
        state = state,
        onComplete = { viewModel.onAction(FollowUpsAction.CompleteFollowUp(it)) },
    )
}

@Composable
private fun FollowUpsScreen(
    state: FollowUpsUiState,
    onComplete: (uz.cardlens.core.domain.FollowUp) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionHeader("Due Today") }
        if (state.dueToday.isEmpty()) {
            item { EmptyText("No due follow-ups.") }
        } else {
            items(state.dueToday, key = { it.id }) { followUp ->
                FollowUpCard(
                    followUp = followUp,
                    contactName = state.contacts.firstOrNull { it.id == followUp.contactId }?.fullName,
                    onComplete = onComplete,
                )
            }
        }
        item { SectionHeader("Upcoming") }
        items(state.upcoming, key = { it.id }) { followUp ->
            FollowUpCard(
                followUp = followUp,
                contactName = state.contacts.firstOrNull { it.id == followUp.contactId }?.fullName,
                onComplete = onComplete,
            )
        }
        if (state.completed.isNotEmpty()) {
            item { SectionHeader("Completed") }
            items(state.completed, key = { it.id }) { followUp ->
                FollowUpCard(
                    followUp = followUp,
                    contactName = state.contacts.firstOrNull { it.id == followUp.contactId }?.fullName,
                    onComplete = onComplete,
                )
            }
        }
    }
}
