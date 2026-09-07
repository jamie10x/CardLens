package uz.cardlens.feature.settings.presentation

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

@Composable
fun SettingsRoute(
    onToggleLanguage: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val exportSubject = stringResource(R.string.export_subject)
    val exportTitle = stringResource(R.string.settings_export)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ExportCsv -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TEXT, effect.csvContent)
                        putExtra(Intent.EXTRA_SUBJECT, exportSubject)
                    }
                    context.startActivity(Intent.createChooser(intent, exportTitle))
                }
            }
        }
    }

    SettingsScreen(
        state = state,
        onExport = { viewModel.onAction(SettingsAction.ExportContacts) },
        onToggleLanguage = { viewModel.onAction(SettingsAction.ToggleLanguage) },
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onExport: () -> Unit,
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
                stringResource(R.string.settings_local_account),
            )
        }
        item { SettingsRow(stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_desc)) }
        item { SettingsRow(stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_desc)) }
        item {
            SettingsRow(stringResource(R.string.settings_storage), stringResource(R.string.settings_storage_desc))
        }
        item {
            SettingsRow(stringResource(R.string.settings_app_version), stringResource(R.string.settings_app_version_value))
        }
        item {
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export))
            }
        }
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
            Text(
                stringResource(R.string.settings_offline_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}