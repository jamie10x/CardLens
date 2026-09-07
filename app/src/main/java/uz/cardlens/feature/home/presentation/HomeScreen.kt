package uz.cardlens.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import uz.cardlens.R
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.ui.components.ContactCard
import uz.cardlens.core.ui.components.EmptyState
import uz.cardlens.core.ui.components.FollowUpCard
import uz.cardlens.core.ui.components.PremiumMetricCard
import uz.cardlens.core.ui.components.SectionHeader
import uz.cardlens.core.ui.components.ShimmerCard
import uz.cardlens.ui.theme.GradientDeepTeal
import uz.cardlens.ui.theme.GradientHero
import uz.cardlens.ui.theme.GradientIndigo

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
        onNavigateToScan = onNavigateToScan,
        onContactClick = onNavigateToContact,
        onNavigateToFollowUps = onNavigateToFollowUps,
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onNavigateToScan: () -> Unit,
    onContactClick: (String) -> Unit,
    onNavigateToFollowUps: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { HeroSection() }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumMetricCard(
                    label = stringResource(R.string.home_contacts_label),
                    value = state.contacts.size.toString(),
                    modifier = Modifier.weight(1f),
                    gradient = GradientDeepTeal,
                    icon = Icons.Default.Contacts,
                )
                PremiumMetricCard(
                    label = stringResource(R.string.home_due_today),
                    value = state.dueToday.size.toString(),
                    modifier = Modifier.weight(1f),
                    gradient = GradientIndigo,
                    icon = Icons.Default.Schedule,
                )
            }
        }

        if (state.dueToday.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.home_action_needed)) }
            items(state.dueToday.take(3), key = { it.id }) { followUp ->
                FollowUpCard(
                    followUp = followUp,
                    contactName = state.contacts.firstOrNull { it.id == followUp.contactId }?.fullName,
                    onComplete = { },
                    actionNeeded = true,
                )
            }
            item {
                TextButton(onClick = onNavigateToFollowUps) {
                    Text(
                        stringResource(R.string.home_view_all_tasks),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        item { SectionHeader(stringResource(R.string.home_recent_scans)) }
        if (state.isLoading) {
            item { ShimmerCard() }
            item { ShimmerCard() }
        } else if (state.contacts.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.home_no_contacts),
                    icon = Icons.Default.Contacts,
                    actionLabel = stringResource(R.string.home_scan_cta),
                    onAction = onNavigateToScan,
                )
            }
        } else {
            items(state.contacts.take(5), key = { it.id }) { contact ->
                ContactCard(contact, onClick = { onContactClick(contact.id) })
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(GradientHero)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.home_greeting),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_network_snapshot),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
