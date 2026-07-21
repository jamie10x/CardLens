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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
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
                "Hello, ${state.userName.ifBlank { "Networker" }}!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Your network snapshot for today", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Contacts", state.contacts.size.toString(), Modifier.weight(1f))
                MetricCard("Due Today", state.dueToday.size.toString(), Modifier.weight(1f))
            }
        }

        if (state.dueToday.isNotEmpty()) {
            item { SectionHeader("Action Needed") }
            items(state.dueToday.take(3), key = { it.id }) { followUp ->
                FollowUpCard(
                    followUp = followUp,
                    contactName = state.contacts.firstOrNull { it.id == followUp.contactId }?.fullName,
                    onComplete = { },
                )
            }
            item {
                TextButton(onClick = onNavigateToFollowUps) {
                    Text("View all tasks")
                }
            }
        }

        item { SectionHeader("Recent Scans") }
        if (state.isLoading) {
            item { ShimmerCard() }
            item { ShimmerCard() }
        } else if (state.contacts.isEmpty()) {
            item { EmptyText("No contacts yet. Start by scanning a business card!") }
        } else {
            items(state.contacts.take(5), key = { it.id }) { contact ->
                ContactCard(contact, onClick = { onContactClick(contact.id) })
            }
        }
    }
}
