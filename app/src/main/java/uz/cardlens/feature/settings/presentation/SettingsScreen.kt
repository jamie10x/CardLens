package uz.cardlens.feature.settings.presentation

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import uz.cardlens.core.ui.components.SectionHeader
import uz.cardlens.core.ui.components.SettingsRow
import uz.cardlens.core.ui.components.formatDate

@Composable
fun SettingsRoute(
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.SignedOut -> onSignedOut()
                is SettingsEffect.ShowSnackbar -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEffect.ExportCsv -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TEXT, effect.csvContent)
                        putExtra(Intent.EXTRA_SUBJECT, "CardLens Contacts Export")
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Contacts"))
                }
            }
        }
    }

    SettingsScreen(
        state = state,
        onSyncNow = { viewModel.onAction(SettingsAction.SyncNow) },
        onExport = { viewModel.onAction(SettingsAction.ExportContacts) },
        onSignOut = { viewModel.onAction(SettingsAction.SignOut) },
        onConfirmSignOut = { viewModel.onAction(SettingsAction.ConfirmSignOut) },
        onDismissSignOut = { viewModel.onAction(SettingsAction.DismissSignOut) },
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onSyncNow: () -> Unit,
    onExport: () -> Unit,
    onSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismissSignOut: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionHeader("Settings") }
        item {
            SettingsRow(
                "Profile",
                if (state.isDemoMode) "Demo user account" else state.userEmail.ifBlank { "Signed-in account" },
            )
        }
        item {
            SettingsRow(
                "Supabase",
                when {
                    state.isDemoMode -> "Demo mode is local only"
                    state.lastSyncedAt != null -> "Last synced ${formatDate(state.lastSyncedAt)}"
                    else -> "Ready to sync contacts and follow-ups"
                },
            )
        }
        item {
            Button(
                onClick = onSyncNow,
                enabled = !state.isDemoMode && !state.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSyncing) "Syncing..." else "Sync with Supabase")
            }
        }
        item { SettingsRow("Notifications", "Local reminders are enabled when permission is granted") }
        item {
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Export Contacts (CSV)")
            }
        }
        item { SettingsRow("Privacy", "Card images are kept private by default") }
        item { SettingsRow("App version", "1.0.0 MVP") }
        item {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign Out")
            }
        }
    }

    if (state.showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissSignOut,
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your local data will remain, but it won't sync until you sign back in.") },
            confirmButton = {
                TextButton(onClick = onConfirmSignOut) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSignOut) {
                    Text("Cancel")
                }
            },
        )
    }
}
