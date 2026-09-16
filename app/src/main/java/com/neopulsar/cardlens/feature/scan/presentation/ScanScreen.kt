package com.neopulsar.cardlens.feature.scan.presentation

import com.neopulsar.cardlens.R
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import android.widget.Toast
import org.koin.androidx.compose.koinViewModel
import com.neopulsar.cardlens.core.ui.components.EditableField
import com.neopulsar.cardlens.core.ui.components.SectionHeader

@Composable
fun ScanRoute(
    onOcrComplete: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: ScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScanEffect.ContactSaved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOcrComplete()
                }
            }
        }
    }

    val context = LocalContext.current
    val snackbarMessage = state.snackbarResId?.let { stringResource(it) }
    LaunchedEffect(state.snackbarResId) {
        snackbarMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onAction(ScanAction.ClearSnackbar)
        }
    }

    if (state.reviewDraft != null) {
        OcrReviewContent(state = state, viewModel = viewModel)
    } else {
        ScanTab(
            isProcessing = state.isProcessing,
            onImageCaptured = { viewModel.onAction(ScanAction.ProcessCardImage(it)) },
        )
    }
}

@Composable
private fun ScanTab(
    isProcessing: Boolean,
    onImageCaptured: (Uri) -> Unit,
) {
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onImageCaptured)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(stringResource(R.string.scan_capture_title))
                Text(
                    "Align card within the frame — CardLens crops and reads it instantly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                CameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    onImageCaptured = onImageCaptured,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_upload_gallery), fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                "Tip: Use good lighting for best results",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
        if (isProcessing) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Column {
                            Text(stringResource(R.string.scan_processing), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.scan_reading), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember { PreviewView(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

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
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            },
            executor,
        )
        onDispose { providerFuture.get().unbindAll() }
    }

    previewView.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                previewView.width.toFloat(), previewView.height.toFloat(),
            )
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            camera?.cameraControl?.startFocusAndMetering(action)
        }
        true
    }

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Frosted flash toggle
        IconButton(
            onClick = {
                isFlashOn = !isFlashOn
                camera?.cameraControl?.enableTorch(isFlashOn)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
        ) {
            Icon(
                if (isFlashOn) Icons.Default.Bolt else Icons.Default.FlashOff,
                contentDescription = if (isFlashOn) stringResource(R.string.scan_flash_on) else stringResource(R.string.scan_flash_off),
                tint = if (isFlashOn) Color(0xFFFFEB3B) else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        // Premium frame — 4 corner brackets
        Box(
            modifier = Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth(0.86f).height(168.dp),
        ) {
            val bracket = Modifier.size(width = 26.dp, height = 26.dp)
            Box(bracket.align(Alignment.TopStart).border(3.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(topStart = 14.dp)))
            Box(bracket.align(Alignment.TopEnd).border(3.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(topEnd = 14.dp)))
            Box(bracket.align(Alignment.BottomStart).border(3.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(bottomStart = 14.dp)))
            Box(bracket.align(Alignment.BottomEnd).border(3.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(bottomEnd = 14.dp)))
            // subtle center guide
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.92f)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.18f)),
            )
        }

        // Hint pill above capture
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text("Position card within frame", color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }

        // Premium capture button — outer ring + gradient core
        Box(
            Modifier
                .padding(bottom = 18.dp)
                .size(74.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = {
                    val file = File(context.cacheDir, "card_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture?.takePicture(
                        outputOptions, executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(o: ImageCapture.OutputFileResults) { onImageCaptured(Uri.fromFile(file)) }
                            override fun onError(e: ImageCaptureException) {}
                        },
                    )
                },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
            ) {
                Box(Modifier.size(58.dp).clip(CircleShape).border(2.5.dp, Color.White.copy(alpha = 0.92f), CircleShape))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OcrReviewContent(
    state: ScanUiState,
    viewModel: ScanViewModel,
) {
    val draft = state.reviewDraft ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(stringResource(R.string.scan_review_title))
                Text("Check OCR results before saving", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DraftField(stringResource(R.string.field_full_name), draft.fullName) { viewModel.onAction(ScanAction.UpdateDraft(draft.copy(fullName = it))) }
                    DraftField(stringResource(R.string.field_job_title), draft.jobTitle) { viewModel.onAction(ScanAction.UpdateDraft(draft.copy(jobTitle = it))) }
                    DraftField(stringResource(R.string.field_company), draft.company) { viewModel.onAction(ScanAction.UpdateDraft(draft.copy(company = it))) }
                    DraftField(stringResource(R.string.field_email), draft.email) { viewModel.onAction(ScanAction.UpdateDraft(draft.copy(email = it))) }
                    DraftField(stringResource(R.string.field_phone), draft.phone) { viewModel.onAction(ScanAction.UpdateDraft(draft.copy(phone = it))) }
                    DraftField(stringResource(R.string.field_website), draft.website) { viewModel.onAction(ScanAction.UpdateDraft(draft.copy(website = it))) }
                    DraftField(stringResource(R.string.field_address), draft.address) { viewModel.onAction(ScanAction.UpdateDraft(draft.copy(address = it))) }
                    DraftField(stringResource(R.string.field_notes), draft.notes, minLines = 3) { viewModel.onAction(ScanAction.UpdateDraft(draft.copy(notes = it))) }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(stringResource(R.string.scan_set_reminder))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderPreset.entries.forEach { preset ->
                        val isSelected = state.selectedReminderPreset == preset
                        AssistChip(
                            onClick = { viewModel.onAction(ScanAction.SelectReminderPreset(preset)) },
                            label = { Text(stringResource(preset.labelResId), fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium) },
                            leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(999.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.0f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                borderWidth = 1.dp,
                            ),
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.onAction(ScanAction.RetakeScan) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(stringResource(R.string.scan_retake), fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { viewModel.onAction(ScanAction.SaveReviewedContact) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(stringResource(R.string.scan_save_contact), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun DraftField(
    label: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    EditableField(label = label, value = value, onChange = onValueChange, minLines = minLines)
}
