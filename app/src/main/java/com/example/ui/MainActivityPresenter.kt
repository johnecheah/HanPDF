package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@Composable
fun MainActivityPresenter(
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val activity = context as? Activity

    // Launchers
    val scannerLauncher = rememberScannerLauncher(viewModel)
    val idScannerLauncher = rememberIdScannerLauncher(viewModel, state, context)

    // Sandbox fallback states
    var showSandboxScanner by remember { mutableStateOf(false) }
    var showSandboxIdScanner by remember { mutableStateOf(false) }

    // Reset sandbox when screen changes
    LaunchedEffect(state.currentScreen) {
        if (state.currentScreen is Screen.ScanCamera) showSandboxScanner = false
        if (state.currentScreen is Screen.IdScanCamera) showSandboxIdScanner = false
    }

    // Snackbar for feedback messages
    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissFeedback()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = state.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Dashboard -> {
                        DashboardScreen(state = state, viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    }
                    is Screen.ScanCamera -> {
                        ScannerContent(
                            activity = activity,
                            viewModel = viewModel,
                            state = state,
                            scannerLauncher = scannerLauncher,
                            showSandbox = showSandboxScanner,
                            onSandboxChange = { showSandboxScanner = it }
                        )
                    }
                    is Screen.ScanEdit -> ScanEditScreen(state = state, viewModel = viewModel)
                    is Screen.IdScanCamera -> {
                        IdScanContent(
                            activity = activity,
                            viewModel = viewModel,
                            state = state,
                            idScannerLauncher = idScannerLauncher,
                            showSandbox = showSandboxIdScanner,
                            onSandboxChange = { showSandboxIdScanner = it }
                        )
                    }
                    is Screen.IdScanEdit -> IdScanEditScreen(state = state, viewModel = viewModel)
                    is Screen.PdfMerger -> MergerScreen(state = state, viewModel = viewModel)
                    is Screen.OcrViewer -> OcrScreen(state = state, viewModel = viewModel)
                    is Screen.Editor -> EditorScreen(state = state, viewModel = viewModel)
                    is Screen.SignatureStudio -> SignatureStudioScreen(viewModel = viewModel)
                }
            }

            // Top Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) { data ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = data.visuals.message,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

// ==================== Reusable Scanner Launchers ====================

@Composable
private fun rememberScannerLauncher(viewModel: MainViewModel) =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        handleScanResult(result, viewModel)
    }

@Composable
private fun rememberIdScannerLauncher(viewModel: MainViewModel, state: UiState, context: Context) =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        handleIdScanResult(result, viewModel, state, context)
    }

private fun handleScanResult(result: androidx.activity.result.ActivityResult, viewModel: MainViewModel) {
    if (result.resultCode != Activity.RESULT_OK) {
        viewModel.navigateTo(Screen.Dashboard)
        return
    }

    val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
    val uris = scanResult?.pages?.mapNotNull { it.imageUri } ?: emptyList()

    if (uris.isNotEmpty()) {
        viewModel.addAndCompileScannedPagesFromUris(uris)
    } else {
        viewModel.navigateTo(Screen.Dashboard)
    }
}

private fun handleIdScanResult(
    result: androidx.activity.result.ActivityResult,
    viewModel: MainViewModel,
    state: UiState,
    context: Context
) {
    if (result.resultCode != Activity.RESULT_OK) {
        viewModel.triggerFeedback("ID Scan cancelled.")
        viewModel.navigateTo(Screen.Dashboard)
        return
    }

    val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
    val uris = scanResult?.pages?.mapNotNull { it.imageUri } ?: emptyList()

    if (uris.isEmpty()) {
        viewModel.triggerFeedback("No image captured.")
        return
    }

    val bitmap = try {
        context.contentResolver.openInputStream(uris[0])?.use { inputStream ->
            android.graphics.BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        Log.e("MainActivityPresenter", "Failed to decode bitmap", e)
        null
    }

    if (bitmap != null) {
        if (!state.isScanningIdBack) {
            viewModel.captureIdCardFront(bitmap)
            viewModel.triggerFeedback("ID Card Front captured! Launching scanner for Back side.")
        } else {
            viewModel.captureIdCardBack(bitmap)
            viewModel.triggerFeedback("ID Card Back captured! Opening Filter Page.")
            viewModel.navigateTo(Screen.IdScanEdit)
        }
    } else {
        viewModel.triggerFeedback("Failed to decode scanned ID image.")
    }
}

// ==================== Scanner Content Composables ====================

@Composable
private fun ScannerContent(
    activity: Activity?,
    viewModel: MainViewModel,
    state: UiState,
    scannerLauncher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>,
    showSandbox: Boolean,
    onSandboxChange: (Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        launchMlKitScanner(activity, scannerLauncher, viewModel, onSandboxChange, isIdScan = false)
    }

    if (showSandbox) {
        ScannerScreen(state = state, viewModel = viewModel)
    } else {
        LoadingScannerScreen(
            title = "Starting Quick Scan...",
            subtitle = "Powered by Google Play Services ML Kit",
            onOpenSandbox = { onSandboxChange(true) }
        )
    }
}

@Composable
private fun IdScanContent(
    activity: Activity?,
    viewModel: MainViewModel,
    state: UiState,
    idScannerLauncher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>,
    showSandbox: Boolean,
    onSandboxChange: (Boolean) -> Unit
) {
    LaunchedEffect(state.isScanningIdBack) {
        launchMlKitScanner(activity, idScannerLauncher, viewModel, onSandboxChange, isIdScan = true)
    }

    if (showSandbox) {
        IdScanCameraScreen(state = state, viewModel = viewModel)
    } else {
        LoadingScannerScreen(
            title = if (!state.isScanningIdBack) "Scan Front Side of ID Card" else "Scan Back Side of ID Card",
            subtitle = "Align ID in card boundaries. Captured images are unified into an A4 sheet.",
            onOpenSandbox = { onSandboxChange(true) }
        )
    }
}

private fun launchMlKitScanner(
    activity: Activity?,
    launcher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>,
    viewModel: MainViewModel,
    onSandboxChange: (Boolean) -> Unit,
    isIdScan: Boolean
) {
    try {
        val options = GmsDocumentScannerOptions.Builder()
            .setPageLimit(if (isIdScan) 1 else 100)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        if (activity == null) {
            onSandboxChange(true)
            return
        }

        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                try {
                    val request = androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    launcher.launch(request)
                } catch (e: Exception) {
                    Log.e("MainActivityPresenter", "Failed to launch intent", e)
                    onSandboxChange(true)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivityPresenter", "ML Kit scanner failed", e)
                viewModel.triggerFeedback("ML Kit unavailable, launching sandbox...")
                onSandboxChange(true)
            }
    } catch (e: Exception) {
        Log.e("MainActivityPresenter", "Scanner initialization failed", e)
        viewModel.triggerFeedback("Scanner failed to start. Using sandbox mode.")
        onSandboxChange(true)
    }
}

@Composable
private fun LoadingScannerScreen(
    title: String,
    subtitle: String? = null,
    onOpenSandbox: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            CircularProgressIndicator(color = Color(0xFF10AC84), strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            if (onOpenSandbox != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onOpenSandbox,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open Sandbox Scanner", color = Color.White)
                }
            }
        }
    }
}
