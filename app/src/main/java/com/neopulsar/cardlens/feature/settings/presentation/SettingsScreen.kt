package com.neopulsar.cardlens.feature.settings.presentation

import com.neopulsar.cardlens.R
import android.content.Intent
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.neopulsar.cardlens.core.ui.components.SectionHeader
import com.neopulsar.cardlens.ui.theme.GradientHero

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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionHeader(stringResource(R.string.settings_title)) }
        item { OfflineHero() }

        // Group: Data & Privacy
        item { SectionHeader(stringResource(R.string.settings_storage)) }
        item {
            PremiumGroupCard {
                PremiumSettingsRow(Icons.Default.Business, "Profile", stringResource(R.string.settings_local_account))
                DividerThin()
                PremiumSettingsRow(Icons.Default.Notifications, stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_desc))
            }
        }

        item { SectionHeader(stringResource(R.string.settings_privacy)) }
        item {
            PremiumGroupCard {
                PremiumSettingsRow(Icons.Default.Lock, stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_desc))
                DividerThin()
                PremiumSettingsRow(Icons.Default.Storage, stringResource(R.string.settings_storage), stringResource(R.string.settings_storage_desc))
            }
        }

        item { SectionHeader(stringResource(R.string.settings_language)) }
        item {
            PremiumGroupCard {
                PremiumSettingsRow(
                    Icons.Default.Language,
                    stringResource(R.string.settings_language),
                    if (state.isUzbek) stringResource(R.string.language_uzbek) else stringResource(R.string.language_english),
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onToggleLanguage,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isUzbek) stringResource(R.string.language_english) else stringResource(R.string.language_uzbek),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item { SectionHeader(stringResource(R.string.settings_export)) }
        item {
            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_export), fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Saves all contacts as CSV — share anywhere",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        item { SectionHeader(stringResource(R.string.settings_app_version)) }
        item {
            PremiumGroupCard {
                PremiumSettingsRow(Icons.Default.Info, stringResource(R.string.settings_app_version), stringResource(R.string.settings_app_version_value), showArrow = false)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PremiumGroupCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun PremiumSettingsRow(icon: ImageVector, title: String, subtitle: String, showArrow: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showArrow) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f), modifier = Modifier.size(1.dp))
        }
    }
}

@Composable
private fun DividerThin() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
}

@Composable
private fun OfflineHero() {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(GradientHero)).padding(16.dp),
    ) {
        Box(Modifier.size(100.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).align(Alignment.TopEnd))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                Icon(Icons.Default.CloudOff, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Fully offline", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.settings_offline_note),
                    color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
