package uz.cardlens.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import uz.cardlens.R
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.ui.components.ContactCard
import uz.cardlens.core.ui.components.EmptyText
import uz.cardlens.core.ui.components.FollowUpCard
import uz.cardlens.core.ui.components.MetricCard
import uz.cardlens.core.ui.components.SectionHeader
import uz.cardlens.core.ui.components.ShimmerCard

@Composable
fun HomeRoute(
    onNavigateToScan: () -> Unit,
    onNavigateToContact: (String) -> Unit,
    onNavigateToFollowUps: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    HomeScreen(
        state = state,
        onContactClick = onNavigateToContact,
        onNavigateToFollowUps = onNavigateToFollowUps,
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onContactClick: (String) -> Unit,
    onNavigateToFollowUps: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                stringResource(R.string.home_greeting, state.userName.ifBlank { stringResource(R.string.home_default_name) }),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.home_network_snapshot), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(stringResource(R.string.home_contacts_label), state.contacts.size.toString(), Modifier.weight(1f))
                MetricCard(stringResource(R.string.home_due_today), state.dueToday.size.toString(), Modifier.weight(1f))
            }
        }

        if (state.dueToday.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.home_action_needed)) }
            items(state.dueToday.take(3), key = { it.id }) { followUp ->
                FollowUpCard(
                    followUp = followUp,
                    contactName = state.contacts.firstOrNull { it.id == followUp.contactId }?.fullName,
                    onComplete = { },
                )
            }
            item {
                TextButton(onClick = onNavigateToFollowUps) {
                    Text(stringResource(R.string.home_view_all_tasks))
                }
            }
        }

        item { SectionHeader(stringResource(R.string.home_recent_scans)) }
        if (state.isLoading) {
            item { ShimmerCard() }
            item { ShimmerCard() }
        } else if (state.contacts.isEmpty()) {
            item { EmptyText(stringResource(R.string.home_no_contacts)) }
        } else {
            items(state.contacts.take(5), key = { it.id }) { contact ->
                ContactCard(contact, onClick = { onContactClick(contact.id) })
            }
        }
    }
}
