package uz.cardlens

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.domain.ContactDraft
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.domain.FollowUp
import uz.cardlens.ui.theme.CardLensTheme
import uz.cardlens.ui.theme.statusColor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CardLensTheme {
                val viewModel: CardLensViewModel = koinViewModel()
                CardLensApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardLensApp(viewModel: CardLensViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(CardLensAction.ClearSnackbar)
        }
    }

    if (!state.isSignedIn) {
        AuthScreen(
            state = state,
            onSignIn = { email, password -> viewModel.onAction(CardLensAction.SignIn(email, password)) },
            onSignUp = { email, password -> viewModel.onAction(CardLensAction.SignUp(email, password)) },
        ) { viewModel.onAction(CardLensAction.ContinueDemo) }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CardLens", fontWeight = FontWeight.Bold)
                        Text(
                            "Scan cards. Capture contacts. Follow up smarter.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(CardLensAction.SelectTab(MainTab.Scan)) },
                shape = CircleShape,
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan card")
            }
        },
        bottomBar = {
            CardLensBottomBar(
                activeTab = state.activeTab,
                onTabClick = { viewModel.onAction(CardLensAction.SelectTab(it)) },
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                state.reviewDraft != null -> OcrReviewScreen(
                    state = state,
                    onAction = viewModel::onAction,
                )
                state.selectedContact != null -> ContactProfileScreen(
                    state = state,
                    contact = state.selectedContact!!,
                    onAction = viewModel::onAction,
                )
                state.activeTab == MainTab.Home -> HomeScreen(state, viewModel::onAction)
                state.activeTab == MainTab.Contacts -> ContactsScreen(state, viewModel::onAction)
                state.activeTab == MainTab.Scan -> ScanScreen(state, viewModel::onAction)
                state.activeTab == MainTab.FollowUps -> FollowUpsScreen(state, viewModel::onAction)
                state.activeTab == MainTab.Settings -> SettingsScreen(state, viewModel::onAction)
            }
        }
    }
}

@Composable
private fun AuthScreen(
    state: CardLensState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onDemo: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Badge,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text("CardLens", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Never lose a business card again.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.isAuthConfigured) {
                Text(
                    "Supabase is not configured yet. Demo mode is available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSignIn(email, password) },
                enabled = !state.isAuthLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isAuthLoading) "Signing in..." else "Sign In")
            }
            TextButton(
                onClick = { onSignUp(email, password) },
                enabled = !state.isAuthLoading,
            ) {
                Text("Create account")
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Text(
                    " OR ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            OutlinedButton(
                onClick = onDemo,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue in Demo Mode")
            }
        }
    }
}

@Composable
private fun HomeScreen(state: CardLensState, onAction: (CardLensAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    "Hello, ${state.userEmail.takeWhile { it != '@' }.ifBlank { "Networker" }}!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("Your network snapshot for today", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Contacts", state.contacts.size.toString(), Modifier.weight(1f))
                MetricCard("Due Today", state.dueToday.size.toString(), Modifier.weight(1f))
            }
        }

        if (state.dueToday.isNotEmpty()) {
            item { SectionHeader("Action Needed") }
            items(state.dueToday.take(3), key = { it.id }) { followUp ->
                FollowUpCard(followUp, state.contacts.firstOrNull { it.id == followUp.contactId }, onAction)
            }
            item {
                TextButton(onClick = { onAction(CardLensAction.SelectTab(MainTab.FollowUps)) }) {
                    Text("View all tasks")
                }
            }
        }

        item { SectionHeader("Recent Scans") }
        if (state.contacts.isEmpty()) {
            item { EmptyText("No contacts yet. Start by scanning a business card!") }
        } else {
            items(state.contacts.take(5), key = { it.id }) { contact ->
                ContactCard(contact, onClick = { onAction(CardLensAction.SelectContact(contact.id)) })
            }
        }
    }
}

@Composable
private fun ContactsScreen(state: CardLensState, onAction: (CardLensAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(CardLensAction.SearchContacts(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("Search by name, company, or tags") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(12.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.visibleContacts, key = { it.id }) { contact ->
                ContactCard(contact, onClick = { onAction(CardLensAction.SelectContact(contact.id)) })
            }
            if (state.visibleContacts.isEmpty()) {
                item { EmptyText("No contacts found.") }
            }
        }
    }
}

@Composable
private fun ScanScreen(state: CardLensState, onAction: (CardLensAction) -> Unit) {
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onAction(CardLensAction.ProcessCardImage(it)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionHeader("Capture Business Card") }
        item {
            CameraCaptureCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp)),
                onImageCaptured = { onAction(CardLensAction.ProcessCardImage(it)) },
            )
        }
        item {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Upload from Gallery")
            }
        }
        if (state.isProcessingScan) {
            item { EmptyText("Processing card image...") }
        }
    }
}

@Composable
private fun CameraCaptureCard(
    modifier: Modifier = Modifier,
    onImageCaptured: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            },
            executor,
        )
        onDispose {
            providerFuture.get().unbindAll()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        IconButton(
            onClick = {
                val file = File(context.cacheDir, "card_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture?.takePicture(
                    outputOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            onImageCaptured(Uri.fromFile(file))
                        }

                        override fun onError(exception: ImageCaptureException) {}
                    },
                )
            },
            modifier = Modifier
                .padding(bottom = 20.dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.Black)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OcrReviewScreen(state: CardLensState, onAction: (CardLensAction) -> Unit) {
    val draft = state.reviewDraft ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionHeader("Review Details") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Full Name", draft.fullName) { onAction(CardLensAction.UpdateDraft(draft.copy(fullName = it))) }
                Field("Job Title", draft.jobTitle) { onAction(CardLensAction.UpdateDraft(draft.copy(jobTitle = it))) }
                Field("Company", draft.company) { onAction(CardLensAction.UpdateDraft(draft.copy(company = it))) }
                Field("Email", draft.email) { onAction(CardLensAction.UpdateDraft(draft.copy(email = it))) }
                Field("Phone", draft.phone) { onAction(CardLensAction.UpdateDraft(draft.copy(phone = it))) }
                Field("Website", draft.website) { onAction(CardLensAction.UpdateDraft(draft.copy(website = it))) }
                Field("Address", draft.address) { onAction(CardLensAction.UpdateDraft(draft.copy(address = it))) }
                Field("Notes", draft.notes) { onAction(CardLensAction.UpdateDraft(draft.copy(notes = it))) }
            }
        }

        item { SectionHeader("Set Follow-up Reminder") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderPreset.entries.forEach { preset ->
                    AssistChip(
                        onClick = { onAction(CardLensAction.SelectReminderPreset(preset)) },
                        label = { Text(preset.label) },
                        leadingIcon = {
                            if (draft.followUpTitle.isNotBlank() && preset.label.contains("Later", ignoreCase = true)) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            }
                        },
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onAction(CardLensAction.RetakeScan) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Retake")
                }
                Button(
                    onClick = { onAction(CardLensAction.SaveReviewedContact) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save Contact")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactProfileScreen(state: CardLensState, contact: Contact, onAction: (CardLensAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onAction(CardLensAction.BackToContacts) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text("Contact Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
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
                Column {
                    Text(contact.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${contact.jobTitle} at ${contact.company}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                StatusBadge(contact.status)
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
            QuickActions(onGenerate = { onAction(CardLensAction.GenerateMessage(contact)) })
        }

        item {
            InfoCard(contact)
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contact.tags.forEach {
                        AssistChip(onClick = {}, label = { Text(it.name) })
                    }
                }
            }
        }

        item { SectionHeader("Follow-up Message") }
        item {
            Button(
                onClick = { onAction(CardLensAction.GenerateMessage(contact)) },
                enabled = !state.isGeneratingMessage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isGeneratingMessage) "Generating..." else "Generate Follow-up Message")
            }
        }
        if (state.generatedMessage.isNotBlank()) {
            item {
                OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Suggested message", fontWeight = FontWeight.SemiBold)
                        Text(state.generatedMessage)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onGenerate: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AssistChip(onClick = {}, label = { Text("Call") }, leadingIcon = { Icon(Icons.Default.Phone, null) })
        AssistChip(onClick = {}, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) })
        AssistChip(onClick = {}, label = { Text("Copy") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
        AssistChip(onClick = onGenerate, label = { Text("AI") }, leadingIcon = { Icon(Icons.Default.SmartToy, null) })
    }
}

@Composable
private fun InfoCard(contact: Contact) {
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

@Composable
private fun FollowUpsScreen(state: CardLensState, onAction: (CardLensAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionHeader("Due Today") }
        if (state.dueToday.isEmpty()) {
            item { EmptyText("No due follow-ups.") }
        } else {
            items(state.dueToday, key = { it.id }) { followUp ->
                FollowUpCard(followUp, state.contacts.firstOrNull { it.id == followUp.contactId }, onAction)
            }
        }
        item { SectionHeader("Upcoming") }
        items(state.upcomingFollowUps, key = { it.id }) { followUp ->
            FollowUpCard(followUp, state.contacts.firstOrNull { it.id == followUp.contactId }, onAction)
        }
        if (state.completedFollowUps.isNotEmpty()) {
            item { SectionHeader("Completed") }
            items(state.completedFollowUps, key = { it.id }) { followUp ->
                FollowUpCard(followUp, state.contacts.firstOrNull { it.id == followUp.contactId }, onAction)
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: CardLensState, onAction: (CardLensAction) -> Unit) {
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
                onClick = { onAction(CardLensAction.SyncNow) },
                enabled = !state.isDemoMode && !state.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSyncing) "Syncing..." else "Sync with Supabase")
            }
        }
        item { SettingsRow("Notifications", "Local reminders are enabled when permission is granted") }
        item { SettingsRow("Export contacts", "CSV export placeholder for the next iteration") }
        item { SettingsRow("Privacy", "Card images are kept private by default") }
        item { SettingsRow("App version", "1.0.0 MVP") }
        item {
            OutlinedButton(onClick = { onAction(CardLensAction.SignOut) }, modifier = Modifier.fillMaxWidth()) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun ContactCard(contact: Contact, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    contact.fullName.take(1).ifBlank { "?" },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.fullName, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(contact.jobTitle, contact.company)
                        .asSequence()
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (contact.tags.isNotEmpty()) {
                    Text(
                        contact.tags.joinToString("  ") { "#${it.name}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            StatusBadge(contact.status)
        }
    }
}

@Composable
private fun FollowUpCard(followUp: FollowUp, contact: Contact?, onAction: (CardLensAction) -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(contact?.fullName ?: "Unknown contact", fontWeight = FontWeight.SemiBold)
                Text(followUp.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDate(followUp.dueAt), style = MaterialTheme.typography.labelMedium)
            }
            if (!followUp.completed) {
                IconButton(onClick = { onAction(CardLensAction.CompleteFollowUp(followUp)) }) {
                    Icon(Icons.Default.Check, contentDescription = "Mark done")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusBadge(status: ContactStatus) {
    val color = MaterialTheme.colorScheme.statusColor(status.label)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    if (value.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value)
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = if (label == "Notes") 3 else 1,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsRow(title: String, subtitle: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun CardLensBottomBar(activeTab: MainTab, onTabClick: (MainTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = activeTab == MainTab.Home,
            onClick = { onTabClick(MainTab.Home) },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = activeTab == MainTab.Contacts,
            onClick = { onTabClick(MainTab.Contacts) },
            icon = { Icon(Icons.Default.Badge, null) },
            label = { Text("Contacts") },
        )
        NavigationBarItem(
            selected = activeTab == MainTab.Scan,
            onClick = { onTabClick(MainTab.Scan) },
            icon = { Icon(Icons.Default.CameraAlt, null) },
            label = { Text("Scan") },
        )
        NavigationBarItem(
            selected = activeTab == MainTab.FollowUps,
            onClick = { onTabClick(MainTab.FollowUps) },
            icon = { Icon(Icons.Default.Check, null) },
            label = { Text("Tasks") },
        )
        NavigationBarItem(
            selected = activeTab == MainTab.Settings,
            onClick = { onTabClick(MainTab.Settings) },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") },
        )
    }
}
