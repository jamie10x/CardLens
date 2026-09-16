package com.neopulsar.cardlens.feature.contacts.presentation

import com.neopulsar.cardlens.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.neopulsar.cardlens.core.domain.ContactStatus
import com.neopulsar.cardlens.core.ui.components.ContactCard
import com.neopulsar.cardlens.core.ui.components.EmptyState

@Composable
fun ContactsRoute(
    onNavigateToContact: (String) -> Unit,
    viewModel: ContactsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Premium search field — elevated card feel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    RoundedCornerShape(20.dp),
                ),
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onAction(ContactsAction.Search(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.contacts_search_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onAction(ContactsAction.Search("")) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }

        // Filter chips — premium pill style with border
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PremiumFilterChip(
                selected = state.statusFilter == null,
                label = stringResource(R.string.contacts_filter_all),
                onClick = { viewModel.onAction(ContactsAction.FilterByStatus(null)) },
            )
            ContactStatus.entries.filter { it != ContactStatus.Archived }.forEach { status ->
                PremiumFilterChip(
                    selected = state.statusFilter == status,
                    label = stringResource(status.displayResId),
                    onClick = { viewModel.onAction(ContactsAction.FilterByStatus(status)) },
                )
            }
        }

        // Result count — subtle
        if (state.visibleContacts.isNotEmpty() && (state.searchQuery.isNotEmpty() || state.statusFilter != null)) {
            Text(
                text = "${state.visibleContacts.size} contacts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                letterSpacing = 0.4.sp,
            )
        } else {
            Spacer(Modifier.size(12.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
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
                        description = if (state.searchQuery.isNotEmpty() || state.statusFilter != null) null
                        else "Tap Scan to capture your first card",
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
            title = { Text(stringResource(R.string.contacts_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.contacts_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(ContactsAction.ConfirmDelete) }) {
                    Text(stringResource(R.string.contacts_delete_confirm), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(ContactsAction.DismissDelete) }) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }
}

@Composable
private fun PremiumFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp,
        ),
    )
}
