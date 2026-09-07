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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.ui.components.ContactCard
import uz.cardlens.core.ui.components.EmptyState

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
            placeholder = { Text(stringResource(R.string.contacts_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    androidx.compose.material3.IconButton(
                        onClick = { viewModel.onAction(ContactsAction.Search("")) },
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
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
                label = { Text(stringResource(R.string.contacts_filter_all)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            ContactStatus.entries.filter { it != ContactStatus.Archived }.forEach { status ->
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { viewModel.onAction(ContactsAction.FilterByStatus(status)) },
                    label = { Text(stringResource(status.displayResId)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.visibleContacts.isEmpty()) {
                item {
                    EmptyState(
                        title = if (state.searchQuery.isNotEmpty() || state.statusFilter != null) {
                            stringResource(R.string.contacts_no_results)
                        } else {
                            stringResource(R.string.contacts_empty)
                        },
                        icon = Icons.Default.Badge,
                    )
                }
            }
            items(state.visibleContacts, key = { it.id }) { contact ->
                ContactCard(contact, onClick = { onNavigateToContact(contact.id) })
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(ContactsAction.DismissDelete) },
            title = {
                Text(
                    stringResource(R.string.contacts_delete_title),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = { Text(stringResource(R.string.contacts_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(ContactsAction.ConfirmDelete) }) {
                    Text(stringResource(R.string.contacts_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(ContactsAction.DismissDelete) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
