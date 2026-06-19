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
import com.example.ui.screens.*

@Composable
fun MainActivityPresenter(
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp) // Ensure it is above bottom navigation safe zones!
            ) { data ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = data.visuals.message,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
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
                        ScannerScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    is Screen.ScanEdit -> {
                        ScanEditScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    is Screen.DocumentEditor -> {
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
                }
            }
        }
    }
}
