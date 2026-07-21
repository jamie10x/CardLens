package uz.cardlens.feature.contacts.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.ui.components.ContactCard
import uz.cardlens.core.ui.components.EmptyText
import uz.cardlens.ui.theme.statusColor

@Composable
fun ContactsRoute(
    onNavigateToContact: (String) -> Unit,
    viewModel: ContactsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onAction(ContactsAction.Search(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("Search by name, company, or tags") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(12.dp),
        )

        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.statusFilter == null,
                onClick = { viewModel.onAction(ContactsAction.FilterByStatus(null)) },
                label = { Text("All") },
            )
            ContactStatus.entries.filter { it != ContactStatus.Archived }.forEach { status ->
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { viewModel.onAction(ContactsAction.FilterByStatus(status)) },
                    label = { Text(status.label) },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.visibleContacts.isEmpty() && state.searchQuery.isNotEmpty()) {
                item { EmptyText("No contacts match your search.") }
            } else if (state.visibleContacts.isEmpty()) {
                item { EmptyText("No contacts yet. Scan your first business card!") }
            }
            items(state.visibleContacts, key = { it.id }) { contact ->
                ContactCard(contact, onClick = { onNavigateToContact(contact.id) })
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(ContactsAction.DismissDelete) },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete this contact?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(ContactsAction.ConfirmDelete) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(ContactsAction.DismissDelete) }) {
                    Text("Cancel")
                }
            },
        )
    }
}
