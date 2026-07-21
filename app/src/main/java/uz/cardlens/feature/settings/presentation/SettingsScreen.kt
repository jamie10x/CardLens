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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import uz.cardlens.R
import uz.cardlens.core.ui.components.SectionHeader
import uz.cardlens.core.ui.components.SettingsRow
import uz.cardlens.core.ui.components.formatDate

@Composable
fun SettingsRoute(
    onSignedOut: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.SignedOut -> onSignedOut()
                is SettingsEffect.ShowSnackbar -> {
                    Toast.makeText(context, context.getString(effect.messageResId), Toast.LENGTH_SHORT).show()
                }
                is SettingsEffect.ExportCsv -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TEXT, effect.csvContent)
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject))
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.settings_export)))
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
        onToggleLanguage = { viewModel.onAction(SettingsAction.ToggleLanguage) },
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
    onToggleLanguage: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionHeader(stringResource(R.string.settings_title)) }
        item {
            SettingsRow(
                stringResource(R.string.settings_profile),
                if (state.isDemoMode) stringResource(R.string.settings_demo_account) else state.userEmail.ifBlank { stringResource(R.string.settings_signed_in_account) },
            )
        }
        item {
            SettingsRow(
                stringResource(R.string.settings_supabase),
                when {
                    state.isDemoMode -> stringResource(R.string.settings_demo_local)
                    state.lastSyncedAt != null -> stringResource(R.string.settings_last_synced, formatDate(state.lastSyncedAt))
                    else -> stringResource(R.string.settings_sync_ready)
                },
            )
        }
        item {
            Button(
                onClick = onSyncNow,
                enabled = !state.isDemoMode && !state.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSyncing) stringResource(R.string.settings_syncing) else stringResource(R.string.settings_sync_now))
            }
        }
        item { SettingsRow(stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_desc)) }
        item {
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export))
            }
        }
        item { SettingsRow(stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_desc)) }
        item { SettingsRow(stringResource(R.string.settings_app_version), stringResource(R.string.settings_app_version_value)) }
        item {
            SettingsRow(
                stringResource(R.string.settings_language),
                if (state.isUzbek) stringResource(R.string.language_uzbek) else stringResource(R.string.language_english),
            )
        }
        item {
            OutlinedButton(onClick = onToggleLanguage, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isUzbek) stringResource(R.string.language_english) else stringResource(R.string.language_uzbek))
            }
        }
        item {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_sign_out))
            }
        }
    }

    if (state.showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissSignOut,
            title = { Text(stringResource(R.string.settings_sign_out_title)) },
            text = { Text(stringResource(R.string.settings_sign_out_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmSignOut) {
                    Text(stringResource(R.string.settings_sign_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSignOut) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
