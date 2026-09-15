package uz.cardlens.feature.contacts.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.neopulsar.cardlens.R
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.ui.components.EditableField
import uz.cardlens.core.ui.components.InfoRow
import uz.cardlens.core.ui.components.QuickActions
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
    val contactUpdatedText = stringResource(R.string.contact_updated)
    val contactDeletedText = stringResource(R.string.contact_deleted)
    val contactStatusChangedText = stringResource(R.string.contact_status_changed)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactProfileEffect.ShowSnackbar -> {
                    val message = when (effect.messageResId) {
                        R.string.contact_updated -> contactUpdatedText
                        R.string.contact_deleted -> contactDeletedText
                        R.string.contact_status_changed -> String.format(contactStatusChangedText, effect.formatArg)
                        else -> ""
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (effect.messageResId == R.string.contact_deleted) onDeleted()
                }
            }
        }
    }

    ContactProfileScreen(state = state, context = context, onBack = onBack, onAction = viewModel::onAction)
}

@Composable
private fun ContactProfileScreen(
    state: ContactProfileUiState,
    context: Context,
    onBack: () -> Unit,
    onAction: (ContactProfileAction) -> Unit,
) {
    val contact = state.contact ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = if (state.isEditing) {{ onAction(ContactProfileAction.CancelEdit) }} else onBack,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.contact_back)) }
                Spacer(Modifier.width(12.dp))
                Text(if (state.isEditing) stringResource(R.string.contact_edit) else stringResource(R.string.contact_details), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (!state.isEditing) {
                    IconButton(onClick = { onAction(ContactProfileAction.RequestDelete) }, modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))) {
                        Icon(Icons.Default.Delete, stringResource(R.string.contact_delete_icon), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { onAction(ContactProfileAction.StartEdit) }, modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                        Icon(Icons.Default.Edit, stringResource(R.string.contact_edit_icon), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (state.isEditing) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditableField(stringResource(R.string.field_full_name), state.editName) { onAction(ContactProfileAction.EditName(it)) }
                        EditableField(stringResource(R.string.field_company), state.editCompany) { onAction(ContactProfileAction.EditCompany(it)) }
                        EditableField(stringResource(R.string.field_job_title), state.editJobTitle) { onAction(ContactProfileAction.EditJobTitle(it)) }
                        EditableField(stringResource(R.string.field_email), state.editEmail) { onAction(ContactProfileAction.EditEmail(it)) }
                        EditableField(stringResource(R.string.field_phone), state.editPhone) { onAction(ContactProfileAction.EditPhone(it)) }
                        EditableField(stringResource(R.string.field_location_met), state.editLocationMet) { onAction(ContactProfileAction.EditLocationMet(it)) }
                        OutlinedButton(onClick = { onAction(ContactProfileAction.ShowDatePicker) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Event, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.contact_date_met, formatDate(state.editDateMet)), fontWeight = FontWeight.Medium)
                        }
                        EditableField(stringResource(R.string.field_notes), state.editNotes, minLines = 3) { onAction(ContactProfileAction.EditNotes(it)) }
                    }
                }
            }
            item {
                Button(onClick = { onAction(ContactProfileAction.SaveEdit) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(stringResource(R.string.contact_save_changes), fontWeight = FontWeight.SemiBold)
                }
            }
            return@LazyColumn
        }

        // Premium avatar header card
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(76.dp).clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                            .border(2.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(contact.fullName.take(1).uppercase().ifBlank { "?" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(contact.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        val sub = listOf(contact.jobTitle, contact.company).filter { it.isNotBlank() }.joinToString(" · ")
                        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        else Text("No title", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).clickable { onAction(ContactProfileAction.ToggleStatusMenu) }) { StatusBadge(contact.status) }
                    DropdownMenu(expanded = state.showStatusMenu, onDismissRequest = { onAction(ContactProfileAction.ToggleStatusMenu) }) {
                        ContactStatus.entries.forEach { status ->
                            DropdownMenuItem(text = { Text(stringResource(status.displayResId)) }, onClick = { onAction(ContactProfileAction.ChangeStatus(status)) })
                        }
                    }
                }
                Text(
                    "${formatDate(contact.dateMet)}${if (contact.locationMet.isNotBlank()) " · ${contact.locationMet}" else ""}",
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        contact.cardImageUri?.let { uri ->
            item {
                Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(model = uri, contentDescription = stringResource(R.string.contact_card_image), modifier = Modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                }
            }
        }

        item { QuickActions(onCall = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))) }, onEmail = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))) }, onCopy = { val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; clip.setPrimaryClip(ClipData.newPlainText("Contact", contact.fullName)) }) }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))) {
                Column(Modifier.padding(18.dp)) {
                    InfoRow(Icons.Default.Person, stringResource(R.string.contact_info_name), contact.fullName)
                    DividerRow()
                    InfoRow(Icons.Default.Work, stringResource(R.string.contact_info_title), contact.jobTitle)
                    DividerRow()
                    InfoRow(Icons.Default.Business, stringResource(R.string.contact_info_company), contact.company)
                    DividerRow()
                    InfoRow(Icons.Default.Email, stringResource(R.string.contact_info_email), contact.email)
                    DividerRow()
                    InfoRow(Icons.Default.Phone, stringResource(R.string.contact_info_phone), contact.phone)
                    if (contact.locationMet.isNotBlank() || contact.dateMet != 0L) {
                        DividerRow()
                        InfoRow(Icons.Default.Event, stringResource(R.string.contact_info_met), "${formatDate(contact.dateMet)} ${contact.locationMet}".trim())
                    }
                }
            }
        }

        if (contact.notes.isNotBlank()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                            Text(stringResource(R.string.contact_relationship_notes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(contact.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
                    }
                }
            }
        }

        if (contact.tags.isNotEmpty()) {
            item {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contact.tags.forEach {
                        AssistChip(onClick = {}, label = { Text("#${it.name}", fontWeight = FontWeight.Medium) }, shape = RoundedCornerShape(999.dp), colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), labelColor = MaterialTheme.colorScheme.primary), border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f), borderWidth = 0.dp))
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(onDismissRequest = { onAction(ContactProfileAction.DismissDelete) }, title = { Text(stringResource(R.string.contacts_delete_title), fontWeight = FontWeight.Bold) }, text = { Text(stringResource(R.string.contact_delete_confirm_message, contact.fullName)) }, confirmButton = { TextButton(onClick = { onAction(ContactProfileAction.ConfirmDelete) }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) } }, dismissButton = { TextButton(onClick = { onAction(ContactProfileAction.DismissDelete) }) { Text(stringResource(R.string.cancel), fontWeight = FontWeight.Medium) } }, shape = RoundedCornerShape(24.dp))
    }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.editDateMet)
        DatePickerDialog(onDismissRequest = { onAction(ContactProfileAction.DismissDatePicker) }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { onAction(ContactProfileAction.SelectDate(it)) } }) { Text(stringResource(R.string.ok), fontWeight = FontWeight.SemiBold) } }, dismissButton = { TextButton(onClick = { onAction(ContactProfileAction.DismissDatePicker) }) { Text(stringResource(R.string.cancel)) } }, shape = RoundedCornerShape(24.dp)) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun DividerRow() {
    Spacer(Modifier.height(14.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))); Spacer(Modifier.height(14.dp))
}
