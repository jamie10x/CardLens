package uz.cardlens.feature.followups.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.neopulsar.cardlens.R
import uz.cardlens.core.domain.FollowUp
import uz.cardlens.core.ui.components.EmptyState
import uz.cardlens.core.ui.components.FollowUpCard
import uz.cardlens.core.ui.components.SectionHeader
import uz.cardlens.ui.theme.GradientIndigo
import uz.cardlens.ui.theme.GradientTeal

@Composable
fun FollowUpsRoute(viewModel: FollowUpsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    FollowUpsScreen(state = state, onComplete = { viewModel.onAction(FollowUpsAction.CompleteFollowUp(it)) })
}

@Composable
private fun FollowUpsScreen(state: FollowUpsUiState, onComplete: (FollowUp) -> Unit) {
    val total = state.dueToday.size + state.upcoming.size
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Premium summary banner
        item {
            FollowUpsHero(totalDue = state.dueToday.size, totalUpcoming = state.upcoming.size)
        }

        item { SectionHeader(stringResource(R.string.followups_due_today)) }
        if (state.dueToday.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EmptyState(title = stringResource(R.string.followups_empty_due), icon = Icons.Default.CheckCircle)
                }
            }
        } else {
            items(state.dueToday, key = { it.id }) { followUp ->
                FollowUpCard(
                    followUp = followUp,
                    contactName = state.contacts.firstOrNull { it.id == followUp.contactId }?.fullName,
                    onComplete = onComplete, actionNeeded = true,
                )
            }
        }

        item { SectionHeader(stringResource(R.string.followups_upcoming)) }
        if (state.upcoming.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) { EmptyState(title = stringResource(R.string.followups_empty)) }
            }
        } else {
            items(state.upcoming, key = { it.id }) { followUp ->
                FollowUpCard(
                    followUp = followUp,
                    contactName = state.contacts.firstOrNull { it.id == followUp.contactId }?.fullName,
                    onComplete = onComplete,
                )
            }
        }

        if (state.completed.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.followups_completed)) }
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

@Composable
private fun FollowUpsHero(totalDue: Int, totalUpcoming: Int) {
    val gradient = if (totalDue > 0) GradientTeal else GradientIndigo
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(gradient))
            .padding(18.dp),
    ) {
        // subtle circle
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).align(Alignment.TopEnd)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Schedule, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                Text(
                    if (totalDue > 0) "$totalDue due today" else "All caught up",
                    color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                when {
                    totalDue > 0 -> "Stay on top — tap ✓ when done"
                    totalUpcoming > 0 -> "$totalUpcoming upcoming follow-ups scheduled"
                    else -> "No follow-ups yet. Scan a card and set a reminder"
                },
                color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
