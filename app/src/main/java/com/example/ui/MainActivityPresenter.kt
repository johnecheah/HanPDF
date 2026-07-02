package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.screens.*

@Composable
fun MainActivityPresenter(
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uris = scanResult?.pages?.mapNotNull { it.imageUri }
            if (uris != null && uris.isNotEmpty()) {
                viewModel.addAndCompileScannedPagesFromUris(uris)
            } else {
                viewModel.navigateTo(Screen.Dashboard)
            }
        } else {
            viewModel.navigateTo(Screen.Dashboard)
        }
    }

    val context = LocalContext.current
    val activity = context as? Activity

    val idScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uris = scanResult?.pages?.mapNotNull { it.imageUri }
            if (uris != null && uris.isNotEmpty()) {
                val bitmap = try {
                    val inputStream = context.contentResolver.openInputStream(uris[0])
                    val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bmp
                } catch (e: Exception) {
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
            } else {
                viewModel.triggerFeedback("No image captured.")
            }
        } else {
            viewModel.triggerFeedback("ID Scan cancelled.")
            viewModel.navigateTo(Screen.Dashboard)
        }
    }

    var showSandboxScanner by remember { mutableStateOf(false) }
    var showSandboxIdScanner by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentScreen) {
        if (state.currentScreen is Screen.ScanCamera) {
            showSandboxScanner = false
        }
        if (state.currentScreen is Screen.IdScanCamera) {
            showSandboxIdScanner = false
        }
    }

    // Observe feedback messages and trigger beautiful toast SnackBar overlays!
    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        // Render screen transitions smoothly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = state.currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenSwitchTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Dashboard -> {
                        DashboardScreen(
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is Screen.ScanCamera -> {
                        val context = LocalContext.current
                        val activity = context as? Activity

                        LaunchedEffect(Unit) {
                            try {
                                val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
                                    .setPageLimit(100)
                                    .setResultFormats(
                                        com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                                        com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                                    )
                                    .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                                    .build()

                                val scanner = com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)

                                if (activity != null) {
                                    scanner.getStartScanIntent(activity)
                                        .addOnSuccessListener { intentSender ->
                                            try {
                                                val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                                                scannerLauncher.launch(intentSenderRequest)
                                            } catch (e: Exception) {
                                                viewModel.triggerFeedback("GMS Scanner failed to start, launching sandbox...")
                                                showSandboxScanner = true
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("MainActivityPresenter", "GMS Scanner failed to get intent", e)
                                            viewModel.triggerFeedback("GMS Scanner unavailable, launching sandbox...")
                                            showSandboxScanner = true
                                        }
                                } else {
                                    showSandboxScanner = true
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivityPresenter", "Failed to initialize GMS Scanner client", e)
                                viewModel.triggerFeedback("GMS Scanner initialization failed, launching sandbox...")
                                showSandboxScanner = true
                            }
                        }

                        if (showSandboxScanner) {
                            ScannerScreen(
                                state = state,
                                viewModel = viewModel
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF10AC84),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Starting Quick Scan...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Powered by Google Play Services ML Kit",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    is Screen.ScanEdit -> {
                        ScanEditScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    is Screen.IdScanCamera -> {
                        val scanContext = LocalContext.current
                        val scanActivity = scanContext as? Activity

                        // Launch effect triggered when entering ID scanner, or when we transition from front to back side!
                        LaunchedEffect(state.isScanningIdBack) {
                            try {
                                val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
                                    .setPageLimit(1) // Capture exactly one image per scan session
                                    .setResultFormats(
                                        com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
                                    )
                                    .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                                    .build()

                                val scanner = com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)

                                if (scanActivity != null) {
                                    scanner.getStartScanIntent(scanActivity)
                                        .addOnSuccessListener { intentSender ->
                                            try {
                                                val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                                                idScannerLauncher.launch(intentSenderRequest)
                                            } catch (e: Exception) {
                                                viewModel.triggerFeedback("ML Kit ID Scanner failed to start, launching sandbox...")
                                                showSandboxIdScanner = true
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("MainActivityPresenter", "ML Kit ID Scanner failed to get intent", e)
                                            viewModel.triggerFeedback("ML Kit Scanner unavailable, launching sandbox...")
                                            showSandboxIdScanner = true
                                        }
                                } else {
                                    showSandboxIdScanner = true
                                }
                            } catch (e: java.lang.NoClassDefFoundError) {
                                Log.e("MainActivityPresenter", "ML Kit Document Scanner library missing, launching sandbox...", e)
                                viewModel.triggerFeedback("ML Kit Scanner library missing, launching sandbox...")
                                showSandboxIdScanner = true
                            } catch (e: Exception) {
                                Log.e("MainActivityPresenter", "Failed to initialize ML Kit ID Scanner client", e)
                                viewModel.triggerFeedback("ML Kit Scanner initialization failed, launching sandbox...")
                                showSandboxIdScanner = true
                            }
                        }

                        if (showSandboxIdScanner) {
                            IdScanCameraScreen(
                                state = state,
                                viewModel = viewModel
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0B0F19)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF10AC84),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (!state.isScanningIdBack) "Scan Front Side of ID Card" else "Scan Back Side of ID Card",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Align ID in card boundaries. Captured images are unified into an A4 sheet.",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { showSandboxIdScanner = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Open Sandbox Scanner", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    is Screen.IdScanEdit -> {
                        IdScanEditScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    is Screen.PdfMerger -> {
                        MergerScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    is Screen.OcrViewer -> {
                        OcrScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    is Screen.Editor -> {
                        EditorScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    is Screen.SignatureStudio -> {
                        SignatureStudioScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Overlay SNACKBAR at the Top!
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) { data ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = data.visuals.message,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
