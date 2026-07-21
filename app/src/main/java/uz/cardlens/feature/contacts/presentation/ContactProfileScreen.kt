package uz.cardlens.feature.contacts.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.ui.components.EditableField
import uz.cardlens.core.ui.components.InfoRow
import uz.cardlens.core.ui.components.QuickActions
import uz.cardlens.core.ui.components.SectionHeader
import uz.cardlens.core.ui.components.StatusBadge
import uz.cardlens.core.ui.components.formatDate

@Composable
fun ContactProfileRoute(
    contactId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit = onBack,
    viewModel: ContactProfileViewModel = koinViewModel(parameters = { parametersOf(contactId) }),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactProfileEffect.ShowSnackbar -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    if (effect.message == "Contact deleted") {
                        onDeleted()
                    }
                }
            }
        }
    }

    ContactProfileScreen(
        state = state,
        context = context,
        onBack = onBack,
        onGenerateMessage = { state.contact?.let { viewModel.onAction(ContactProfileAction.GenerateMessage(it)) } },
        onAction = viewModel::onAction,
    )
}

@Composable
private fun ContactProfileScreen(
    state: ContactProfileUiState,
    context: android.content.Context,
    onBack: () -> Unit,
    onGenerateMessage: () -> Unit,
    onAction: (ContactProfileAction) -> Unit,
) {
    val contact = state.contact ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = if (state.isEditing) {{ onAction(ContactProfileAction.CancelEdit) }} else onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isEditing) "Edit Contact" else "Contact Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                if (!state.isEditing) {
                    IconButton(onClick = { onAction(ContactProfileAction.RequestDelete) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { onAction(ContactProfileAction.StartEdit) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }
        }

        if (state.isEditing) {
            item {
                EditableField(label = "Full Name", value = state.editName) { onAction(ContactProfileAction.EditName(it)) }
            }
            item {
                EditableField(label = "Company", value = state.editCompany) { onAction(ContactProfileAction.EditCompany(it)) }
            }
            item {
                EditableField(label = "Job Title", value = state.editJobTitle) { onAction(ContactProfileAction.EditJobTitle(it)) }
            }
            item {
                EditableField(label = "Email", value = state.editEmail) { onAction(ContactProfileAction.EditEmail(it)) }
            }
            item {
                EditableField(label = "Phone", value = state.editPhone) { onAction(ContactProfileAction.EditPhone(it)) }
            }
            item {
                EditableField(label = "Location Met", value = state.editLocationMet) { onAction(ContactProfileAction.EditLocationMet(it)) }
            }
            item {
                OutlinedButton(
                    onClick = { onAction(ContactProfileAction.ShowDatePicker) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Date Met: ${formatDate(state.editDateMet)}")
                }
            }
            item {
                EditableField(label = "Notes", value = state.editNotes, minLines = 3) { onAction(ContactProfileAction.EditNotes(it)) }
            }
            item {
                Button(
                    onClick = { onAction(ContactProfileAction.SaveEdit) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save Changes")
                }
            }
            return@LazyColumn
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        contact.fullName.take(1).ifBlank { "?" },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(contact.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${contact.jobTitle} at ${contact.company}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    StatusBadge(contact.status.label)
                    DropdownMenu(
                        expanded = state.showStatusMenu,
                        onDismissRequest = { onAction(ContactProfileAction.ToggleStatusMenu) },
                    ) {
                        ContactStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.label) },
                                onClick = { onAction(ContactProfileAction.ChangeStatus(status)) },
                            )
                        }
                    }
                }
            }
        }

        contact.cardImageUri?.let { uri ->
            item {
                AsyncImage(
                    model = uri,
                    contentDescription = "Original card image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        item {
            QuickActions(
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                    context.startActivity(intent)
                },
                onEmail = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                    context.startActivity(intent)
                },
                onCopy = {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.setPrimaryClip(ClipData.newPlainText("Contact", contact.fullName))
                },
                onGenerate = onGenerateMessage,
            )
        }

        item {
            OutlinedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(Icons.Default.Person, "Name", contact.fullName)
                    InfoRow(Icons.Default.Work, "Title", contact.jobTitle)
                    InfoRow(Icons.Default.Business, "Company", contact.company)
                    InfoRow(Icons.Default.Email, "Email", contact.email)
                    InfoRow(Icons.Default.Phone, "Phone", contact.phone)
                    InfoRow(Icons.Default.Event, "Met", "${formatDate(contact.dateMet)} ${contact.locationMet}")
                }
            }
        }

        if (contact.notes.isNotBlank()) {
            item {
                OutlinedCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("Relationship notes", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(contact.notes)
                    }
                }
            }
        }

        if (contact.tags.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contact.tags.forEach {
                        AssistChip(onClick = {}, label = { Text(it.name) })
                    }
                }
            }
        }

        item { SectionHeader("Follow-up Message") }
        item {
            OutlinedButton(
                onClick = onGenerateMessage,
                enabled = !state.isGenerating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isGenerating) "Generating..." else "Generate Message")
            }
        }
        if (state.generatedMessage.isNotBlank()) {
            item {
                OutlinedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Suggested message", fontWeight = FontWeight.SemiBold)
                        Text(state.generatedMessage)
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { onAction(ContactProfileAction.DismissDelete) },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete ${contact.fullName}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onAction(ContactProfileAction.ConfirmDelete) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ContactProfileAction.DismissDelete) }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.editDateMet)
        DatePickerDialog(
            onDismissRequest = { onAction(ContactProfileAction.DismissDatePicker) },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onAction(ContactProfileAction.SelectDate(it)) }
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ContactProfileAction.DismissDatePicker) }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
