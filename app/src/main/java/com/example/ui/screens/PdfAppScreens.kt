package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF as AndroidRectF
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.UiState
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

fun getFontFamily(fontName: String?): FontFamily {
    val typeface = when (fontName?.lowercase()) {
        "times new roman" -> android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
        "tahoma" -> android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)
        "calibri" -> android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
        "arial" -> android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        "serif" -> android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
        "monospace" -> android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        "cursive" -> android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
        else -> android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }
    return FontFamily(typeface)
}

@Composable
fun SelectableFeatureButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                maxLines = 1,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ESignStudioTabContent(
    state: UiState,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var signatureToDelete by remember { mutableStateOf<SignatureProfile?>(null) }
    var showImportNamingDialog by remember { mutableStateOf(false) }
    var importAlias by remember { mutableStateOf("") }
    var selectedUriForImport by remember { mutableStateOf<android.net.Uri?>(null) }

    var showCropStudio by remember { mutableStateOf(false) }
    var uriToCrop by remember { mutableStateOf<android.net.Uri?>(null) }
    var aliasToCrop by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            selectedUriForImport = uri
            showImportNamingDialog = true
        }
    }

    if (showImportNamingDialog) {
        AlertDialog(
            onDismissRequest = { showImportNamingDialog = false },
            title = { Text("Import Photo Signature", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a name/alias for this signature profile:")
                    OutlinedTextField(
                        value = importAlias,
                        onValueChange = { importAlias = it },
                        label = { Text("Signature Title / Holder Name") },
                        placeholder = { Text("e.g. John Doe - Scan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedUriForImport
                        if (uri != null) {
                            uriToCrop = uri
                            aliasToCrop = importAlias
                            showCropStudio = true
                        }
                        showImportNamingDialog = false
                        importAlias = ""
                        selectedUriForImport = null
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportNamingDialog = false
                    importAlias = ""
                    selectedUriForImport = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCropStudio) {
        val uri = uriToCrop
        if (uri != null) {
            CropImageStudioDialog(
                uri = uri,
                alias = aliasToCrop,
                onDismissRequest = {
                    showCropStudio = false
                    uriToCrop = null
                    aliasToCrop = ""
                },
                onCropCompleted = { croppedBitmap ->
                    viewModel.saveCroppedSignature(context, croppedBitmap, aliasToCrop)
                    showCropStudio = false
                    uriToCrop = null
                    aliasToCrop = ""
                }
            )
        }
    }

    if (signatureToDelete != null) {
        AlertDialog(
            onDismissRequest = { signatureToDelete = null },
            title = { Text("Delete Signature Profile", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this signature profile? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        signatureToDelete?.let { viewModel.deleteSignatureProfile(it) }
                        signatureToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { signatureToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "E-Sign Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create reusable digital signature profiles, draw on high-precision canvases, or import scan files.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.navigateTo(Screen.SignatureStudio) },
                modifier = Modifier.weight(1f).testTag("launch_draw_pad_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch Draw Pad", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { importLauncher.launch("image/*") },
                modifier = Modifier.weight(1f).testTag("import_signature_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Signature", fontSize = 13.sp)
            }
        }

        // List Header
        Text(
            text = "Stored Signature Profiles",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary
        )

        if (state.signatures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved profiles yet.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Draw on the drawing pad or import a photo of your signature above.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.signatures) { sig ->
                    SignatureProfileItem(
                        sig = sig,
                        onDelete = { signatureToDelete = sig },
                        onRename = { newName -> viewModel.renameSignatureProfile(sig, newName) },
                        onExport = { viewModel.exportSignatureProfileTo300Ppi(sig) }
                    )
                }
            }
        }
    }
}

// --- 1. DASHBOARD SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: UiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Recents, 1 = Starred, 2 = Signatures
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var multiSelectMode by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateListOf<Document>() }
    var searchQuery by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<Document?>(null) }
    var showCombineSourceDialog by remember { mutableStateOf(false) }
    var showCombineRecentsDialog by remember { mutableStateOf(false) }

    val multipleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleLocalFilesForMerging(context, uris)
        }
    }

    val singleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    var name = "imported_note"
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                    val extension = name.substringAfterLast(".", "jpg")
                    viewModel.createDocumentFromLocalFile(name, extension, bytes)
                }
            } catch (e: Exception) {
                viewModel.triggerFeedback("Failed to read selected file: ${e.localizedMessage}")
            }
        }
    }

    val categories = listOf("All", "PDF", "Scan", "ID Card", "Meeting Minutes")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Document")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Bento-Inspired Top Header and Contextual Operations Bar
            if (multiSelectMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(Color(0xFFD3E4FF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            multiSelectMode = false
                            selectedFiles.clear()
                        }) {
                            Icon(Icons.Default.Close, "Cancel Select", tint = Color(0xFF001C3B))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedFiles.size} selected",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001C3B),
                            fontSize = 14.sp
                        )
                    }

                    if (selectedFiles.size >= 2) {
                        Button(
                            onClick = {
                                viewModel.loadMergeSelection(selectedFiles.toList())
                                multiSelectMode = false
                                selectedFiles.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MergeType, "Merge", modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Merge", fontSize = 12.sp, color = Color.White)
                        }
                    } else {
                        Text(
                            text = "Select 2+ files",
                            color = Color(0xFF001C3B).copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(Color(0xFF1B3B6F), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HanPDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About App",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Top right icons (Select all and profile icon) are removed per user request
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }

            // Bento Rich Search Field Overlay
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search documents...", color = Color(0xFF64748B), fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear Search", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFEDF1F9),
                    unfocusedContainerColor = Color(0xFFEDF1F9),
                    disabledContainerColor = Color(0xFFEDF1F9),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color(0xFF001C3B),
                    unfocusedTextColor = Color(0xFF001C3B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Dynamic 3-Deck Bento Grid Quick Action Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Deck 1: Quick Scan (Primary Highlight Block)
                Card(
                    onClick = {
                        viewModel.resetScanner(isIdMode = false)
                        viewModel.navigateTo(Screen.ScanCamera)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("action_scan"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD3E4FF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Quick Scan",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF001C3B),
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Auto-detect and scan docs from camera",
                                color = Color(0xFF001C3B).copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF005FB8), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Scan icon",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Deck 2: Secondary Bento grids
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoSquareItem(
                        icon = Icons.Default.EditNote,
                        iconBackground = Color(0xFFFFEDD5),
                        iconTint = Color(0xFFEA580C),
                        title = "Edit & Notes",
                        subtitle = "New note and import docs",
                        tag = "action_note",
                        modifier = Modifier.weight(1f),
                        onClick = { showCreateDialog = true }
                    )

                    BentoSquareItem(
                        icon = Icons.Default.Draw,
                        iconBackground = Color(0xFFF3E8FF),
                        iconTint = Color(0xFF8B5CF6),
                        title = "E-Sign Studio",
                        subtitle = "Manage & draw signatures",
                        tag = "action_esign",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(Screen.SignatureStudio) }
                    )
                }

                // Deck 3: Supporting Bento cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoSquareItem(
                        icon = Icons.Default.CreditCard,
                        iconBackground = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF059669),
                        title = "Scan ID Card",
                        subtitle = "ID card scanner",
                        tag = "action_id",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.resetIdScanner()
                            viewModel.navigateTo(Screen.IdScanCamera)
                        }
                    )

                    BentoSquareItem(
                        icon = Icons.Default.MergeType,
                        iconBackground = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF2563EB),
                        title = "Combine Files",
                        subtitle = "Merge docs together",
                        tag = "action_combine",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showCombineSourceDialog = true
                        }
                    )
                }
            }

            // Tab bar for file organization (resized to center and cover roughly half screen)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp)
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Text("Recents", modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Text("Starred", modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // List Display Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                val displayList = when (activeTab) {
                    0 -> state.documents.filter {
                        it.isSaved &&
                        (selectedCategoryFilter == "All" || it.category == selectedCategoryFilter) &&
                        (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
                    }
                    1 -> state.starredDocuments.filter {
                        it.isSaved &&
                        (selectedCategoryFilter == "All" || it.category == selectedCategoryFilter) &&
                        (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
                    }
                    else -> emptyList()
                }

                if (displayList.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        title = "No documents found",
                        subtitle = "Create raw templates, scan ID cards, or import PDFs to start."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(displayList) { doc ->
                            val isSelected = selectedFiles.contains(doc)
                            DocumentListItem(
                                doc = doc,
                                multiSelectMode = multiSelectMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (multiSelectMode) {
                                        if (isSelected) selectedFiles.remove(doc) else selectedFiles.add(doc)
                                    } else {
                                        viewModel.openDocumentInEditor(doc)
                                    }
                                },
                                onStarClick = { viewModel.toggleStarDocument(doc) },
                                onDeleteClick = { fileToDelete = doc },
                                onShareClick = { sharePdfFile(context, doc) },
                                onRenameClick = { newTitle -> viewModel.renameDocument(doc, newTitle) }
                            )
                        }
                    }
                }
            }
        }

        // Custom creation choice dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Assemble New Document", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Select a structured paper layout or standard template:", fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TemplateCardButton(
                                icon = Icons.Default.BorderOuter,
                                label = "Blank Note",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.createNewTemplateDocument("Blank Note", "blank")
                                    showCreateDialog = false
                                }
                            )
                            TemplateCardButton(
                                icon = Icons.Default.Notes,
                                label = "Blank Doc",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.createWordDoc("Blank Doc")
                                    showCreateDialog = false
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                singleFilePickerLauncher.launch("*/*")
                                showCreateDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Select local file")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📂 Import Local File & Edit")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("About HanPDF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Text(
                            text = "Created by\nJohne Cheah\n@2026\n \nVersion V1.0",
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (fileToDelete != null) {
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("Delete This File?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete \"${fileToDelete?.title}\"? This action is irreversible.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            fileToDelete?.let { viewModel.deleteDocument(it) }
                            fileToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCombineSourceDialog) {
            AlertDialog(
                onDismissRequest = { showCombineSourceDialog = false },
                title = { 
                    Text(
                        "Combine Files", 
                        fontWeight = FontWeight.Bold, 
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Select the source for documents you want to combine:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            onClick = {
                                showCombineSourceDialog = false
                                try {
                                    multipleFilePickerLauncher.launch("*/*")
                                } catch (e: Exception) {
                                    viewModel.triggerFeedback("Failed to launch selector: ${e.localizedMessage}")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Select Local Files",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Select files from your device storage",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                showCombineSourceDialog = false
                                showCombineRecentsDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Choose from Recents",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Choose from your saved library history",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCombineSourceDialog = false }) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (showCombineRecentsDialog) {
            RecentsSelectorDialog(
                documents = state.documents,
                onDismissRequest = { showCombineRecentsDialog = false },
                onAddSelected = { added ->
                    showCombineRecentsDialog = false
                    viewModel.loadMergeSelection(added)
                }
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BentoSquareItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) Color(0xFF161E2E) else Color.White
    val borderCol = if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0)
    val textCol = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val subTextCol = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)

    Card(
        onClick = onClick,
        modifier = modifier
            .height(115.dp)
            .testTag(tag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, borderCol),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBackground, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = subTextCol,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TemplateCardButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun EmptyStatePanel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty",
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DocumentListItem(
    doc: Document,
    multiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStarClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onRenameClick: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameDraft by remember(doc.title, showRenameDialog) { mutableStateOf(doc.title) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document File", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameDraft.isNotBlank()) {
                            onRenameClick(renameDraft)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Rename", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document thumbnail mockup
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (doc.category.lowercase()) {
                            "pdf" -> Color(0xFFFDE8E8)
                            "scan" -> Color(0xFFE8F2FD)
                            "id card" -> Color(0xFFE2F9F3)
                            else -> Color(0xFFF2F4F7)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (doc.category.lowercase()) {
                        "pdf" -> Icons.Default.PictureAsPdf
                        "scan" -> Icons.Default.CameraAlt
                        "id card" -> Icons.Default.CreditCard
                        else -> Icons.Default.EditNote
                    },
                    contentDescription = null,
                    tint = when (doc.category.lowercase()) {
                        "pdf" -> Color(0xFFE52521)
                        "scan" -> Color(0xFF1B3B6F)
                        "id card" -> Color(0xFF10AC84)
                        else -> Color(0xFF5A6B7C)
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text labels
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = doc.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${doc.pageCount} ${if (doc.pageCount == 1) "Page" else "Pages"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                    )
                }
            }

            // Interactive Controls
            if (multiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                Row {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename file",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Send/Share file",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onStarClick) {
                        Icon(
                            imageVector = if (doc.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star file",
                            tint = if (doc.isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete file",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SignatureProfileItem(
    sig: SignatureProfile,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onExport: () -> Unit,
    onLoadToCanvas: (() -> Unit)? = null
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameDraft by remember { mutableStateOf(sig.alias) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Signature Profile", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("Signature Alias / Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameDraft.isNotBlank()) {
                            onRename(renameDraft)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var showPreviewDialog by remember { mutableStateOf(false) }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = {
                Text(
                    text = sig.alias,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (sig.pathDataJson.startsWith("image:")) {
                        val imagePath = sig.pathDataJson.removePrefix("image:")
                        val bitmap = remember(imagePath) {
                            try {
                                android.graphics.BitmapFactory.decodeFile(imagePath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Signature Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("Image Error", color = Color.Red, fontSize = 14.sp)
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val points = DocumentSerializer.pointsFromJson(sig.pathDataJson)
                            if (points.isNotEmpty()) {
                                val path = Path()
                                val first = points.first()
                                path.moveTo(first.x * size.width, first.y * size.height)
                                
                                for (i in 1 until points.size) {
                                    val p = points[i]
                                    if (p.x == -1f && p.y == -1f) {
                                        if (i + 1 < points.size) {
                                            val next = points[i + 1]
                                            path.moveTo(next.x * size.width, next.y * size.height)
                                        }
                                    } else {
                                        path.lineTo(p.x * size.width, p.y * size.height)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(android.graphics.Color.parseColor(sig.colorHex)),
                                    style = Stroke(
                                        width = sig.strokeWidth * (size.width / 200f).coerceAtLeast(1.5f),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPreviewDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sig.alias, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                
                // Active Signature Preview Box
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    if (sig.pathDataJson.startsWith("image:")) {
                        val imagePath = sig.pathDataJson.removePrefix("image:")
                        val bitmap = remember(imagePath) {
                            try {
                                android.graphics.BitmapFactory.decodeFile(imagePath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Signature Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("Image Error", color = Color.Red, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val points = DocumentSerializer.pointsFromJson(sig.pathDataJson)
                            if (points.isNotEmpty()) {
                                val path = Path()
                                val first = points.first()
                                path.moveTo(first.x * size.width, first.y * size.height)
                                
                                for (i in 1 until points.size) {
                                    val p = points[i]
                                    if (p.x == -1f && p.y == -1f) {
                                        if (i + 1 < points.size) {
                                            val next = points[i + 1]
                                            path.moveTo(next.x * size.width, next.y * size.height)
                                        }
                                    } else {
                                        path.lineTo(p.x * size.width, p.y * size.height)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(android.graphics.Color.parseColor(sig.colorHex)),
                                    style = Stroke(
                                        width = sig.strokeWidth * (size.width / 200f).coerceAtLeast(1.5f),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            if (onLoadToCanvas != null && !sig.pathDataJson.startsWith("image:")) {
                IconButton(onClick = onLoadToCanvas) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Load to Canvas",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Rename Signature", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Signature", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

// --- 2. SIGNATURE STUDIO DRAW CANVAS SCREEN ---
 
// --- Canvas Stroke definition for Signature Studio ---
data class CanvasStroke(
    val points: List<PointDef>,
    val colorHex: String,
    val thickness: Float,
    val penType: String // "pen", "calligraphy", "highlighter", "dashed"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureStudioScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var aliasText by remember { mutableStateOf(TextFieldValue("")) }

    var showCropNamingDialog by remember { mutableStateOf(false) }
    var selectedUriForCrop by remember { mutableStateOf<android.net.Uri?>(null) }
    var cropAlias by remember { mutableStateOf("") }
    var showCropStudio by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            selectedUriForCrop = uri
            cropAlias = aliasText.text
            showCropNamingDialog = true
        }
    }

    if (showCropNamingDialog) {
        AlertDialog(
            onDismissRequest = { showCropNamingDialog = false },
            title = { Text("Import Photo Signature", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a name/alias for this signature profile:")
                    OutlinedTextField(
                        value = cropAlias,
                        onValueChange = { cropAlias = it },
                        label = { Text("Signature Title / Holder Name") },
                        placeholder = { Text("e.g. John Doe - Scan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedUriForCrop
                        if (uri != null) {
                            showCropStudio = true
                        }
                        showCropNamingDialog = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCropNamingDialog = false
                    selectedUriForCrop = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCropStudio) {
        val uri = selectedUriForCrop
        if (uri != null) {
            CropImageStudioDialog(
                uri = uri,
                alias = cropAlias,
                onDismissRequest = {
                    showCropStudio = false
                    selectedUriForCrop = null
                    cropAlias = ""
                },
                onCropCompleted = { croppedBitmap ->
                    viewModel.saveCroppedSignature(context, croppedBitmap, cropAlias)
                    showCropStudio = false
                    selectedUriForCrop = null
                    cropAlias = ""
                }
            )
        }
    }

    // Professional drawing state
    val strokes = remember { mutableStateListOf<CanvasStroke>() }
    val redoStrokes = remember { mutableStateListOf<CanvasStroke>() }
    val activePoints = remember { mutableStateListOf<PointDef>() }

    // Canvas styling state
    var selectedColor by remember { mutableStateOf("#000000") } // Onyx Black
    var selectedThickness by remember { mutableFloatStateOf(4f) }
    var selectedPenType by remember { mutableStateOf("pen") } // "pen", "calligraphy", "highlighter", "dashed"
    var selectedTemplate by remember { mutableStateOf("white") } // "white", "lined", "graph", "slate"
    var isEraserMode by remember { mutableStateOf(false) }
    var customColorHexInput by remember { mutableStateOf("") }
    var showColorHelpDialog by remember { mutableStateOf(false) }

    val colors = listOf(
        Pair("#000000", "Midnight Black"),
        Pair("#1B3B6F", "Fountain Blue"),
        Pair("#D62246", "Crimson Red"),
        Pair("#10B981", "Emerald Green"),
        Pair("#8B5CF6", "Royal Purple"),
        Pair("#F59E0B", "Amber Gold")
    )

    fun eraseAt(rx: Float, ry: Float) {
        val threshold = 0.04f // Erase within 4% distance of the touch point
        val toRemove = strokes.filter { stroke ->
            stroke.points.any { pt ->
                val dx = pt.x - rx
                val dy = pt.y - ry
                (dx * dx + dy * dy) < (threshold * threshold)
            }
        }
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { stroke ->
                strokes.remove(stroke)
                redoStrokes.add(stroke)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Signature Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Vector Canvas & Digital Profiles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Reset Button
                    IconButton(
                        onClick = {
                            strokes.clear()
                            redoStrokes.clear()
                            activePoints.clear()
                            viewModel.triggerFeedback("Canvas cleared.")
                        },
                        enabled = strokes.isNotEmpty() || activePoints.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear Canvas")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            val flattenedPoints = mutableListOf<PointDef>()
                            strokes.forEach { stroke ->
                                flattenedPoints.addAll(stroke.points)
                                flattenedPoints.add(PointDef(-1f, -1f))
                            }
                            if (flattenedPoints.isNotEmpty() && flattenedPoints.last().x == -1f) {
                                flattenedPoints.removeAt(flattenedPoints.lastIndex)
                            }

                            if (flattenedPoints.isEmpty()) {
                                viewModel.triggerFeedback("Draw inside the canvas first!")
                            } else {
                                viewModel.saveDrawnSignature(
                                    aliasText.text,
                                    flattenedPoints.toList(),
                                    selectedColor,
                                    selectedThickness
                                )
                                strokes.clear()
                                redoStrokes.clear()
                                activePoints.clear()
                                aliasText = TextFieldValue("")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, "Save Profile", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile", fontSize = 13.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Profile details card (Bento style)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    TextField(
                        value = aliasText,
                        onValueChange = { aliasText = it },
                        label = { Text("Signature Title / Holder Name") },
                        placeholder = { Text("e.g. John Doe - Work Sign") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Title, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Quick Background Paper Selection Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Canvas Texture:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Pair("white", "Blank"),
                        Pair("lined", "Lined"),
                        Pair("graph", "Graph"),
                        Pair("slate", "Slate")
                    ).forEach { (type, name) ->
                        val isSel = selectedTemplate == type
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedTemplate = type },
                            label = { Text(name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            // Interactive Canvas Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.1f)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("signature_canvas")
                            .pointerInput(selectedColor, selectedThickness, selectedPenType, isEraserMode) {
                                if (isEraserMode) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            eraseAt(offset.x / size.width, offset.y / size.height)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            eraseAt(change.position.x / size.width, change.position.y / size.height)
                                        }
                                    )
                                } else {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            redoStrokes.clear()
                                            activePoints.clear()
                                            activePoints.add(PointDef(offset.x / size.width, offset.y / size.height))
                                        },
                                        onDragEnd = {
                                            if (activePoints.isNotEmpty()) {
                                                strokes.add(
                                                    CanvasStroke(
                                                        points = activePoints.toList(),
                                                        colorHex = selectedColor,
                                                        thickness = selectedThickness,
                                                        penType = selectedPenType
                                                    )
                                                )
                                                activePoints.clear()
                                            }
                                        },
                                        onDragCancel = {
                                            activePoints.clear()
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            activePoints.add(PointDef((change.position.x / size.width).coerceIn(0f, 1f), (change.position.y / size.height).coerceIn(0f, 1f)))
                                        }
                                    )
                                }
                            }
                    ) {
                        // 1. Draw Template background
                        drawCanvasTemplateBackground(selectedTemplate, size)

                        // 2. Draw completed strokes
                        strokes.forEach { stroke ->
                            drawStroke(stroke, size)
                        }

                        // 3. Draw active stroke
                        if (activePoints.isNotEmpty()) {
                            val temp = CanvasStroke(
                                points = activePoints.toList(),
                                colorHex = selectedColor,
                                thickness = selectedThickness,
                                penType = selectedPenType
                            )
                            drawStroke(temp, size)
                        }
                    }

                    // Watermark help if empty
                    if (strokes.isEmpty() && activePoints.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Gesture,
                                contentDescription = null,
                                tint = if (selectedTemplate == "slate") Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sign or sketch inside this boundary",
                                color = if (selectedTemplate == "slate") Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Floating overlays on Canvas (Undo, Redo, Eraser indicators)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (strokes.isNotEmpty()) {
                                    val last = strokes.removeAt(strokes.lastIndex)
                                    redoStrokes.add(last)
                                }
                            },
                            enabled = strokes.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo stroke",
                                tint = if (strokes.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (redoStrokes.isNotEmpty()) {
                                    val last = redoStrokes.removeAt(redoStrokes.lastIndex)
                                    strokes.add(last)
                                }
                            },
                            enabled = redoStrokes.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo stroke",
                                tint = if (redoStrokes.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Floating status pill on bottom-left
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(
                                color = if (isEraserMode) Color(0xFFEF4444).copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEraserMode) "Eraser Mode" else when (selectedPenType) {
                                "calligraphy" -> "Fountain Brush"
                                "highlighter" -> "Highlighter (Neon)"
                                "dashed" -> "Dashed Pen"
                                else -> "Standard Ink Pen"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Professional Bento Tool Selector Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Tool Selection (Standard, Calligraphy, Highlighter, Dashed, Eraser)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Brush Tool:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Standard Pen
                            IconButton(
                                onClick = { selectedPenType = "pen"; isEraserMode = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (!isEraserMode && selectedPenType == "pen") MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gesture,
                                    contentDescription = "Standard Pen",
                                    tint = if (!isEraserMode && selectedPenType == "pen") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Fountain Pen / Calligraphy
                            IconButton(
                                onClick = { selectedPenType = "calligraphy"; isEraserMode = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (!isEraserMode && selectedPenType == "calligraphy") MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = "Calligraphy Brush",
                                    tint = if (!isEraserMode && selectedPenType == "calligraphy") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Neon Highlighter
                            IconButton(
                                onClick = { selectedPenType = "highlighter"; isEraserMode = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (!isEraserMode && selectedPenType == "highlighter") MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BorderColor,
                                    contentDescription = "Neon Highlighter",
                                    tint = if (!isEraserMode && selectedPenType == "highlighter") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            // Dashed Pen
                            IconButton(
                                onClick = { selectedPenType = "dashed"; isEraserMode = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (!isEraserMode && selectedPenType == "dashed") MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LinearScale,
                                    contentDescription = "Dashed Pen",
                                    tint = if (!isEraserMode && selectedPenType == "dashed") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Vector Eraser
                            IconButton(
                                onClick = { isEraserMode = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isEraserMode) Color(0xFFEF4444) else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixNormal,
                                    contentDescription = "Vector Eraser",
                                    tint = if (isEraserMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Color Palette Selector Row
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Ink / Accent Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colors.forEach { (hex, name) ->
                                val isSelected = selectedColor.lowercase() == hex.lowercase() && !isEraserMode
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                        .border(
                                            BorderStroke(
                                                if (isSelected) 2.5.dp else 1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
                                            ),
                                            CircleShape
                                        )
                                        .clickable {
                                            selectedColor = hex
                                            isEraserMode = false
                                        }
                                )
                            }

                            // Custom Color Button
                            IconButton(
                                onClick = { showColorHelpDialog = true },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.ColorLens, "Custom color", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stroke Thickness Slider Control
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Line Thickness:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("${selectedThickness.roundToInt()} pt", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = selectedThickness,
                            onValueChange = { selectedThickness = it },
                            valueRange = 1f..24f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import image signature", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Photo Signature", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Signature Profile Database Directory Header
            Text(
                text = "Stored Signature Profiles",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (state.signatures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No saved profiles yet. Draw inside the box or import a signature image, then save.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                var signatureToDelete by remember { mutableStateOf<com.example.data.SignatureProfile?>(null) }

                if (signatureToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { signatureToDelete = null },
                        title = { Text("Delete Signature Profile", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to permanently delete this signature profile? This action cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    signatureToDelete?.let { viewModel.deleteSignatureProfile(it) }
                                    signatureToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { signatureToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.signatures) { sig ->
                        SignatureProfileItem(
                            sig = sig,
                            onDelete = { signatureToDelete = sig },
                            onRename = { newName -> viewModel.renameSignatureProfile(sig, newName) },
                            onExport = { viewModel.exportSignatureProfileTo300Ppi(sig) },
                            onLoadToCanvas = {
                                // Load back signature vectors directly to canvas!
                                val pts = DocumentSerializer.pointsFromJson(sig.pathDataJson)
                                strokes.clear()
                                redoStrokes.clear()
                                activePoints.clear()
                                if (pts.isNotEmpty()) {
                                    val currentStrokePoints = mutableListOf<PointDef>()
                                    pts.forEach { p ->
                                        if (p.x == -1f && p.y == -1f) {
                                            if (currentStrokePoints.isNotEmpty()) {
                                                strokes.add(
                                                    CanvasStroke(
                                                        points = currentStrokePoints.toList(),
                                                        colorHex = sig.colorHex,
                                                        thickness = sig.strokeWidth,
                                                        penType = "pen"
                                                    )
                                                )
                                                currentStrokePoints.clear()
                                            }
                                        } else {
                                            currentStrokePoints.add(p)
                                        }
                                    }
                                    if (currentStrokePoints.isNotEmpty()) {
                                        strokes.add(
                                            CanvasStroke(
                                                points = currentStrokePoints.toList(),
                                                colorHex = sig.colorHex,
                                                thickness = sig.strokeWidth,
                                                penType = "pen"
                                            )
                                        )
                                    }
                                    selectedColor = sig.colorHex
                                    selectedThickness = sig.strokeWidth
                                    aliasText = TextFieldValue(sig.alias)
                                    isEraserMode = false
                                    viewModel.triggerFeedback("Loaded vector signature back to workspace!")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Custom Color Hex Help dialog
    if (showColorHelpDialog) {
        AlertDialog(
            onDismissRequest = { showColorHelpDialog = false },
            title = { Text("Custom Color Hex Code", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type any hex color code below to draw with custom inks:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = customColorHexInput,
                        onValueChange = { customColorHexInput = it },
                        label = { Text("Hex Code (e.g. #FF5733 or #800080)") },
                        placeholder = { Text("#FF5733") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Quick custom palettes
                    Text("Presets:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#FF5733", "#FFD700", "#FF1493", "#00FFFF", "#800080").forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.parseColor(col)), CircleShape)
                                    .border(1.dp, Color.LightGray, CircleShape)
                                    .clickable {
                                        customColorHexInput = col
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = customColorHexInput.trim()
                        if (cleaned.isNotBlank()) {
                            val hexToApply = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
                            try {
                                android.graphics.Color.parseColor(hexToApply)
                                selectedColor = hexToApply
                                isEraserMode = false
                                showColorHelpDialog = false
                            } catch (e: Exception) {
                                viewModel.triggerFeedback("Invalid Hex color. Use format like #FF5733.")
                            }
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorHelpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- Background and Drawing Helpers for Canvas Rendering ---
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCanvasTemplateBackground(template: String, size: androidx.compose.ui.geometry.Size) {
    when (template) {
        "white" -> {
            drawRect(Color(0xFFFAF9F6)) // Warm Alabaster White
        }
        "lined" -> {
            drawRect(Color(0xFFFAF9F6))
            // Vertical margin line
            val leftMargin = size.width * 0.12f
            drawLine(
                color = Color(0xFFFFB6B6),
                start = Offset(leftMargin, 0f),
                end = Offset(leftMargin, size.height),
                strokeWidth = 2f
            )
            // Horizontal lines
            var lineY = size.height * 0.08f
            while (lineY < size.height) {
                drawLine(
                    color = Color(0xFFD4E2F5),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 1.5f
                )
                lineY += size.height * 0.08f
            }
        }
        "graph" -> {
            drawRect(Color(0xFFF3F7F9))
            val stepX = size.width * 0.05f
            var x = stepX
            while (x < size.width) {
                drawLine(
                    color = Color(0xFFE2EBF0),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += stepX
            }
            val stepY = size.height * 0.10f
            var y = stepY
            while (y < size.height) {
                drawLine(
                    color = Color(0xFFE2EBF0),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += stepY
            }
        }
        "slate" -> {
            drawRect(Color(0xFF1E293B)) // Deep Slate Blue Dark
            // Draw neat dotted patterns
            val stepX = size.width * 0.05f
            val stepY = size.height * 0.10f
            var x = stepX
            while (x < size.width) {
                var y = stepY
                while (y < size.height) {
                    drawCircle(
                        color = Color(0xFF475569),
                        radius = 2f,
                        center = Offset(x, y)
                    )
                    y += stepY
                }
                x += stepX
            }
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: CanvasStroke, size: androidx.compose.ui.geometry.Size) {
    val points = stroke.points
    if (points.isEmpty()) return

    val baseColor = try {
        Color(android.graphics.Color.parseColor(stroke.colorHex))
    } catch (e: Exception) {
        Color.Black
    }

    val drawColor = if (stroke.penType == "highlighter") {
        baseColor.copy(alpha = 0.35f)
    } else {
        baseColor
    }

    val path = Path()
    val first = points.first()
    path.moveTo(first.x * size.width, first.y * size.height)

    if (points.size > 1) {
        for (i in 1 until points.size) {
            val p = points[i]
            val prev = points[i - 1]
            val midX = (prev.x + p.x) / 2f
            val midY = (prev.y + p.y) / 2f
            path.quadraticTo(
                prev.x * size.width,
                prev.y * size.height,
                midX * size.width,
                midY * size.height
            )
        }
        val last = points.last()
        path.lineTo(last.x * size.width, last.y * size.height)
    }

    val finalThickness = if (stroke.penType == "highlighter") {
        stroke.thickness * 4.5f
    } else {
        stroke.thickness
    }

    val pathEffect = if (stroke.penType == "dashed") {
        androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
    } else {
        null
    }

    if (stroke.penType == "calligraphy") {
        drawPath(
            path = path,
            color = drawColor,
            style = Stroke(
                width = finalThickness * 1.3f,
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter,
                pathEffect = pathEffect
            )
        )
        // Overlapping offset brush layer
        val offsetPath = Path()
        offsetPath.moveTo(first.x * size.width + 1.5f, first.y * size.height + 1.5f)
        if (points.size > 1) {
            for (i in 1 until points.size) {
                val p = points[i]
                val prev = points[i - 1]
                val midX = ((prev.x + p.x) / 2f) * size.width + 1.5f
                val midY = ((prev.y + p.y) / 2f) * size.height + 1.5f
                offsetPath.quadraticTo(
                    prev.x * size.width + 1.5f,
                    prev.y * size.height + 1.5f,
                    midX,
                    midY
                )
            }
            offsetPath.lineTo(points.last().x * size.width + 1.5f, points.last().y * size.height + 1.5f)
        }
        drawPath(
            path = offsetPath,
            color = drawColor.copy(alpha = 0.7f),
            style = Stroke(
                width = finalThickness * 0.7f,
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter
            )
        )
    } else {
        drawPath(
            path = path,
            color = drawColor,
            style = Stroke(
                width = finalThickness,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = pathEffect
            )
        )
    }
}

// --- 3. IMMERSIVE DOCUMENT CAMERA SCANNER SCREEN ---

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onCornersDetected: (List<android.graphics.PointF>?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
        update = {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalyzer.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    DocumentAnalyzer(onCornersDetected)
                )

                onImageCaptureCreated(imageCapture)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    android.util.Log.e("CameraPreviewView", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

class DocumentAnalyzer(private val onCornersDetected: (List<android.graphics.PointF>?) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        val yBuffer = image.planes[0].buffer
        val ySize = yBuffer.remaining()
        val yArray = ByteArray(ySize)
        yBuffer.get(yArray)
        
        val width = image.width
        val height = image.height
        
        // Downscale luma frame for ultra-fast, low-latency processing
        val scaledW = 160
        val scaledH = (scaledW * (height.toFloat() / width.toFloat())).toInt()
        
        val lumaBitmap = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(scaledW * scaledH)
        
        val scaleX = width.toFloat() / scaledW
        val scaleY = height.toFloat() / scaledH
        
        for (y in 0 until scaledH) {
            for (x in 0 until scaledW) {
                val origX = (x * scaleX).toInt().coerceIn(0, width - 1)
                val origY = (y * scaleY).toInt().coerceIn(0, height - 1)
                val yIndex = origY * width + origX
                if (yIndex < ySize) {
                    val luma = yArray[yIndex].toInt() and 0xFF
                    pixels[y * scaledW + x] = (0xFF shl 24) or (luma shl 16) or (luma shl 8) or luma
                }
            }
        }
        lumaBitmap.setPixels(pixels, 0, scaledW, 0, 0, scaledW, scaledH)
        
        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(lumaBitmap, 0, 0, scaledW, scaledH, matrix, true)
        } else {
            lumaBitmap
        }
        
        val rawCorners = DocumentProcessor.detectCorners(rotatedBitmap)
        val rw = rotatedBitmap.width.toFloat()
        val rh = rotatedBitmap.height.toFloat()
        val normalized = rawCorners.map { android.graphics.PointF(it.x / rw, it.y / rh) }
        
        onCornersDetected(normalized)
        
        if (rotatedBitmap != lumaBitmap) rotatedBitmap.recycle()
        lumaBitmap.recycle()
        image.close()
    }
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val planeProxy = image.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees != 0 && bitmap != null) {
            val matrix = android.graphics.Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var scannedTitle by remember { mutableStateOf("") }
    var isFrontScapped by remember { mutableStateOf(false) }
    var draftFrontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // Check camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var liveCameraMode by remember { mutableStateOf(false) }
    // Auto-enable live mode if permission is already granted
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            liveCameraMode = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        liveCameraMode = isGranted
        if (isGranted) {
            viewModel.triggerFeedback("Live hardware camera active!")
        } else {
            viewModel.triggerFeedback("Using high-fidelity scanner sandbox.")
        }
    }

    // Gallery Picker launcher to select images from album
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val selectedBmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (selectedBmp != null) {
                    if (state.scannerIdMode) {
                        if (!isFrontScapped) {
                            viewModel.captureScannerStep(selectedBmp, isImported = true)
                            draftFrontBitmap = selectedBmp
                            isFrontScapped = true
                        } else {
                            val front = draftFrontBitmap ?: selectedBmp
                            viewModel.saveIdentityCardFrontBack(front, selectedBmp)
                            isFrontScapped = false
                            Toast.makeText(context, "Identity Card Compiled from Gallery!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.captureScannerStep(selectedBmp, isImported = true)
                        viewModel.triggerFeedback("Photo imported successfully! Tap SAVE below to compile scanned page directly.")
                    }
                } else {
                    Toast.makeText(context, "Unable to load image. File may be corrupted.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gallery import error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var guideStatusText by remember { mutableStateOf("Hold steady. Align card edges inside bounding frame.") }

    var detectedCorners by remember { mutableStateOf<List<android.graphics.PointF>?>(null) }
    var smoothedCorners by remember { mutableStateOf<List<android.graphics.PointF>?>(null) }

    val onSnapPage = {
        if (liveCameraMode && hasCameraPermission && imageCapture != null) {
            val capture = imageCapture
            if (capture != null) {
                capture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = imageProxyToBitmap(image)
                            image.close()
                            if (bitmap != null) {
                                if (state.scannerIdMode) {
                                    if (!isFrontScapped) {
                                        viewModel.captureScannerStep(bitmap)
                                        draftFrontBitmap = bitmap
                                        isFrontScapped = true
                                        guideStatusText = "Front side captured! Now scan the Back Side."
                                    } else {
                                        val front = draftFrontBitmap ?: bitmap
                                        viewModel.saveIdentityCardFrontBack(front, bitmap)
                                        isFrontScapped = false
                                        Toast.makeText(context, "Identity Card Compiled!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.captureScannerStep(bitmap)
                                    viewModel.triggerFeedback("Page captured! Tap SAVE below to compile scanned document.")
                                }
                            } else {
                                val mock = createMockScannedDocSheet(state.scannerStepBitmaps.size + 1)
                                viewModel.captureScannerStep(mock)
                                viewModel.triggerFeedback("Page captured (Sandbox Fallback)! Tap SAVE below to compile.")
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            val mock = createMockScannedDocSheet(state.scannerStepBitmaps.size + 1)
                            viewModel.captureScannerStep(mock)
                            viewModel.triggerFeedback("Page captured (Fallback Mode)! Tap SAVE below to compile.")
                        }
                    }
                )
            }
        } else {
            if (state.scannerIdMode) {
                if (!isFrontScapped) {
                    val simulatedFront = createMockIdentityCard(isFront = true)
                    viewModel.captureScannerStep(simulatedFront)
                    draftFrontBitmap = simulatedFront
                    isFrontScapped = true
                    guideStatusText = "Front captured in Sandbox. Now trigger click for back side."
                } else {
                    val simulatedBack = createMockIdentityCard(isFront = false)
                    val front = draftFrontBitmap ?: createMockIdentityCard(isFront = true)
                    viewModel.saveIdentityCardFrontBack(front, simulatedBack)
                    isFrontScapped = false
                    Toast.makeText(context, "ID Card PDF Compiled in Sandbox Mode!", Toast.LENGTH_SHORT).show()
                }
            } else {
                val simulatedDoc = createMockScannedDocSheet(pageIndex = state.scannerStepBitmaps.size + 1)
                viewModel.captureScannerStep(simulatedDoc)
                viewModel.triggerFeedback("Page captured (Sandbox Mock)! Tap SAVE below to compile.")
            }
        }
    }

    // Dynamic laser sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "Laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserFloat"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (state.scannerIdMode) "ID Security Scan Lab" else "HanPDF Scanner Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (liveCameraMode) "🟢 LIVE DEVICE HARDWARE CAMERA" else "📡 ACTIVE HIGH-FIDELITY SANDBOX",
                            fontSize = 10.sp,
                            color = if (liveCameraMode) Color.Green else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dashboard")
                    }
                },
                actions = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0B0F19))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = guideStatusText,
                color = if (liveCameraMode) Color.Green else Color(0xFF38BDF8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dynamic viewfinder box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF111827), RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(
                            2.dp, 
                            if (liveCameraMode) Color.Green.copy(alpha = 0.4f) else Color(0xFF38BDF8).copy(alpha = 0.4f)
                        ), 
                        RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (state.scannerStepBitmaps.isEmpty()) {
                    // Show live camera feed OR beautiful simulated viewfinder
                    if (liveCameraMode && hasCameraPermission) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CameraPreviewView(
                                modifier = Modifier.fillMaxSize(),
                                onImageCaptureCreated = { imageCapture = it },
                                onCornersDetected = { raw ->
                                    detectedCorners = raw
                                }
                            )

                            // Smooth the corners over consecutive frames to prevent jitter
                            LaunchedEffect(detectedCorners) {
                                val raw = detectedCorners
                                if (raw != null && raw.size == 4) {
                                    val current = smoothedCorners
                                    if (current == null || current.size != 4) {
                                        smoothedCorners = raw
                                    } else {
                                        smoothedCorners = List(4) { idx ->
                                            val r = raw[idx]
                                            val c = current[idx]
                                            android.graphics.PointF(
                                                c.x * 0.7f + r.x * 0.3f,
                                                c.y * 0.7f + r.y * 0.3f
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic high-precision neon corner overlay canvas
                            smoothedCorners?.let { corners ->
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val points = corners.map { 
                                        Offset(it.x * size.width, it.y * size.height)
                                    }
                                    
                                    val path = Path().apply {
                                        moveTo(points[0].x, points[0].y)
                                        lineTo(points[1].x, points[1].y)
                                        lineTo(points[2].x, points[2].y)
                                        lineTo(points[3].x, points[3].y)
                                        close()
                                    }
                                    
                                    // Highlighted translucent green document boundary area
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF10B981).copy(alpha = 0.18f)
                                    )
                                    
                                    // High-tech glowing neon green border outline with dashed effects
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF10B981),
                                        style = Stroke(
                                            width = 6f,
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                                        )
                                    )
                                    
                                    // Pulse target circles around corners
                                    points.forEach { pt ->
                                        drawCircle(
                                            color = Color.White,
                                            radius = 16f,
                                            center = pt
                                        )
                                        drawCircle(
                                            color = Color(0xFF10B981),
                                            radius = 24f,
                                            center = pt,
                                            style = Stroke(width = 4f)
                                        )
                                    }
                                }
                            }

                            // Superimposed laser beam scanner
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val yPos = size.height * laserYOffset
                                        val brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Green.copy(alpha = 0f),
                                                Color.Green.copy(alpha = 0.4f),
                                                Color.Green.copy(alpha = 0f)
                                            ),
                                            startY = yPos - 40f,
                                            endY = yPos + 40f
                                        )
                                        drawRect(
                                            brush = brush,
                                            topLeft = Offset(0f, yPos - 40f),
                                            size = androidx.compose.ui.geometry.Size(size.width, 80f)
                                        )
                                        drawLine(
                                            color = Color.Green,
                                            start = Offset(0f, yPos),
                                            end = Offset(size.width, yPos),
                                            strokeWidth = 3f
                                        )
                                    }
                            )
                        }
                    } else {
                        // Advanced Sandbox view with target overlay & presets
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Moving sweep scan laser for sandbox mode
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val yPos = size.height * laserYOffset
                                        val brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF38BDF8).copy(alpha = 0f),
                                                Color(0xFF38BDF8).copy(alpha = 0.3f),
                                                Color(0xFF38BDF8).copy(alpha = 0f)
                                            ),
                                            startY = yPos - 40f,
                                            endY = yPos + 40f
                                        )
                                        drawRect(
                                            brush = brush,
                                            topLeft = Offset(0f, yPos - 40f),
                                            size = androidx.compose.ui.geometry.Size(size.width, 80f)
                                        )
                                        drawLine(
                                            color = Color(0xFF38BDF8),
                                            start = Offset(0f, yPos),
                                            end = Offset(size.width, yPos),
                                            strokeWidth = 2f
                                        )
                                    }
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color(0xFF0369A1).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.scannerIdMode) Icons.Default.CreditCard else Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (state.scannerIdMode) "SANDBOX ID SCANNER MODE" else "SANDBOX DOCUMENT SCANNER MODE",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (state.scannerIdMode) {
                                        if (!isFrontScapped) "Choose: Snap Virtual, Upload Photo, or Try Sample Driver's License"
                                        else "Now: Capture Virtual card back elements"
                                    } else "Auto-rectify edges, level scans, apply Grayscale filters. Click buttons below to snap, import, or load preset receipt layouts.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Quick Presets container for sandbox testing
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Templates:",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Button(
                                        onClick = {
                                            val doc = createMockScannedDocSheet(state.scannerStepBitmaps.size + 1)
                                            viewModel.captureScannerStep(doc)
                                            viewModel.triggerFeedback("Receipt loaded! Tap SAVE below to compile.")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("🧾 Load Receipt", fontSize = 9.5.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            val card = createMockIdentityCard(isFront = true)
                                            if (state.scannerIdMode) {
                                                viewModel.captureScannerStep(card)
                                                draftFrontBitmap = card
                                                isFrontScapped = true
                                                guideStatusText = "Loaded ID Card Front Side!"
                                            } else {
                                                viewModel.captureScannerStep(card)
                                                viewModel.triggerFeedback("ID Card loaded! Tap SAVE below to compile.")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("🪪 Load ID card", fontSize = 9.5.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Display snapped page preview deck
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = state.scannerStepBitmaps.last().asImageBitmap(),
                            contentDescription = "Scan Preview",
                            contentScale = ContentScale.Inside,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )

                        // Retake button overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.resetScanner(state.scannerIdMode)
                                    isFrontScapped = false
                                    draftFrontBitmap = null
                                    guideStatusText = "Scanner cache reset. Try another capture."
                                }
                            ) {
                                Icon(Icons.Default.Refresh, "Retry", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retake/Reset", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Standard decorative corner brackets WITH dynamic auto-detect boundaries overlay!
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(if (state.scannerIdMode) 0.5f else 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action deck: Snapping, Compile, Gallery Upload, and Permission Grant
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    // Gallery Upload Button
                    IconButton(
                        onClick = { galleryPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(BorderStroke(1.dp, Color(0xFF334155)), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Upload from Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Capture Trigger
                    Button(
                        onClick = { onSnapPage() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (liveCameraMode) Color.Green else Color(0xFF0284C7)
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .height(64.dp)
                            .width(180.dp)
                    ) {
                        Icon(
                            imageVector = if (liveCameraMode) Icons.Default.CameraAlt else Icons.Default.AutoAwesome,
                            contentDescription = if (liveCameraMode) "Snap" else "Sandbox Capture",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.scannerIdMode) {
                                if (isFrontScapped) "Snap Back Side" else "Snap Front Side"
                            } else if (liveCameraMode) "SNAP PAGE" else "SANDBOX SNAP",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Elegant "Save" button to directly compile and save the document
                    if (state.scannerStepBitmaps.isNotEmpty()) {
                        Button(
                            onClick = {
                                val timestamp = System.currentTimeMillis() % 1000
                                viewModel.compileScannedDoc("Quick Scan $timestamp")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10AC84)),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .widthIn(min = 80.dp)
                                .testTag("scanner_save_button")
                        ) {
                            Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(54.dp))
                    }
                }

                // If no device camera permission, guide user on request option
                if (!hasCameraPermission) {
                    Card(
                        onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live hardware camera is off", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Tap to request permissions and use device camera feed.", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                            Text("GRANT", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

// --- 4. SCAN EDITING & FILTERS WORKBENCH ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanEditScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    var titleText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("original") }
    var isCropping by remember { mutableStateOf(true) }

    // Normalized rectangle coordinates for crop bounds (0f to 1f)
    var cropLeft by remember { mutableStateOf(0.05f) }
    var cropTop by remember { mutableStateOf(0.05f) }
    var cropRight by remember { mutableStateOf(0.95f) }
    var cropBottom by remember { mutableStateOf(0.95f) }

    var activeRatioPreset by remember { mutableStateOf("free") }

    val topLeft = Offset(cropLeft, cropTop)
    val topRight = Offset(cropRight, cropTop)
    val bottomRight = Offset(cropRight, cropBottom)
    val bottomLeft = Offset(cropLeft, cropBottom)

    val context = LocalContext.current

    // Image Picker launcher to directly upload/replace an image in PDF Crop Studio
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val selectedBmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (selectedBmp != null) {
                    viewModel.resetScanner()
                    viewModel.captureScannerStep(selectedBmp, isImported = true)
                    cropLeft = 0.05f
                    cropTop = 0.05f
                    cropRight = 0.95f
                    cropBottom = 0.95f
                    activeRatioPreset = "free"
                    viewModel.triggerFeedback("Image uploaded successfully to Crop Studio!")
                } else {
                    android.widget.Toast.makeText(context, "Could not decode selected image file.", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Upload failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(isCropping, state.scannerStepBitmaps) {
        if (isCropping && state.scannerStepBitmaps.isNotEmpty()) {
            val activeBitmap = state.scannerStepBitmaps.first()
            val targetRatio = activeBitmap.width.toFloat() / activeBitmap.height.toFloat()
            val result = com.example.data.DocumentProcessor.calculateRuleOfThirdsCrop(activeBitmap, targetRatio)
            cropLeft = result.left
            cropTop = result.top
            cropRight = result.right
            cropBottom = result.bottom
            activeRatioPreset = "free"
        }
    }

    if (isCropping) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PDF Crop Studio", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { isCropping = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (state.scannerStepBitmaps.isNotEmpty()) {
                            // Upload option to easily change the image
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.testTag("crop_change_image_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Change Image")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    viewModel.cropScannedPage(
                                        index = 0,
                                        topLeftX = cropLeft, topLeftY = cropTop,
                                        topRightX = cropRight, topRightY = cropTop,
                                        bottomRightX = cropRight, bottomRightY = cropBottom,
                                        bottomLeftX = cropLeft, bottomLeftY = cropBottom
                                    )
                                    isCropping = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10AC84)),
                                modifier = Modifier.testTag("apply_crop_button")
                            ) {
                                Icon(Icons.Default.Crop, "Crop")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply Crop")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF0F172A))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.scannerStepBitmaps.isNotEmpty()) {
                    val activeBitmap = state.scannerStepBitmaps.first()
                    val bitmapWidth = activeBitmap.width.toFloat()
                    val bitmapHeight = activeBitmap.height.toFloat()

                    Text(
                        text = "Composition-guided crop: Grid line intersections represent the Rule of Thirds. Lock ratio with presets.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Aspect ratio presets selector row (1:1, 4:3, 16:9 standard locked ratios)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        val presets = listOf(
                            "Free" to "free",
                            "Square 1:1" to "1:1",
                            "Classic 4:3" to "4:3",
                            "Widescreen 16:9" to "16:9"
                        )
                        presets.forEach { (label, preset) ->
                            val selected = activeRatioPreset == preset
                            Button(
                                onClick = {
                                    activeRatioPreset = preset
                                    val ratio = when (preset) {
                                        "1:1" -> 1.0f
                                        "16:9" -> 16f / 9f
                                        "4:3" -> 4f / 3f
                                        else -> bitmapWidth / bitmapHeight
                                    }
                                    val result = com.example.data.DocumentProcessor.calculateRuleOfThirdsCrop(activeBitmap, ratio)
                                    cropLeft = result.left
                                    cropTop = result.top
                                    cropRight = result.right
                                    cropBottom = result.bottom
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color(0xFF10AC84) else Color(0xFF1E293B),
                                    contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp).testTag("preset_$preset")
                            ) {
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val containerWidth = maxWidth
                        val containerHeight = maxHeight
                        val density = LocalDensity.current
                        val containerWidthPx = with(density) { containerWidth.toPx() }
                        val containerHeightPx = with(density) { containerHeight.toPx() }

                        val scaleFactor = minOf(
                            containerWidthPx / bitmapWidth,
                            containerHeightPx / bitmapHeight
                        )

                        val displayedImageWidth = bitmapWidth * scaleFactor
                        val displayedImageHeight = bitmapHeight * scaleFactor

                        val imageLeft = (containerWidthPx - displayedImageWidth) / 2f
                        val imageTop = (containerHeightPx - displayedImageHeight) / 2f

                        Image(
                            bitmap = activeBitmap.asImageBitmap(),
                            contentDescription = "Original scan to crop",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val p1 = Offset(imageLeft + cropLeft * displayedImageWidth, imageTop + cropTop * displayedImageHeight)
                            val p2 = Offset(imageLeft + cropRight * displayedImageWidth, imageTop + cropTop * displayedImageHeight)
                            val p3 = Offset(imageLeft + cropRight * displayedImageWidth, imageTop + cropBottom * displayedImageHeight)
                            val p4 = Offset(imageLeft + cropLeft * displayedImageWidth, imageTop + cropBottom * displayedImageHeight)

                            // Semi-transparent overlay to darken uncropped areas
                            val fullPath = Path().apply {
                                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                                addRect(androidx.compose.ui.geometry.Rect(imageLeft, imageTop, imageLeft + displayedImageWidth, imageTop + displayedImageHeight))
                                moveTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                lineTo(p3.x, p3.y)
                                lineTo(p4.x, p4.y)
                                close()
                            }
                            drawPath(fullPath, color = Color.Black.copy(alpha = 0.65f))

                            // Draw a bright, elegant borders for the cropping box
                            val cropBoxPath = Path().apply {
                                moveTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                lineTo(p3.x, p3.y)
                                lineTo(p4.x, p4.y)
                                close()
                            }
                            drawPath(cropBoxPath, color = Color(0xFF10AC84), style = Stroke(width = 5f))

                            // Draw Rule of Thirds 3x3 Grid inside the cropping box
                            val boxW = p2.x - p1.x
                            val boxH = p4.y - p1.y
                            
                            // Vertical grid lines
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(p1.x + boxW / 3f, p1.y),
                                end = Offset(p1.x + boxW / 3f, p4.y),
                                strokeWidth = 2f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(p1.x + 2f * boxW / 3f, p1.y),
                                end = Offset(p1.x + 2f * boxW / 3f, p4.y),
                                strokeWidth = 2f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )
                            
                            // Horizontal grid lines
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(p1.x, p1.y + boxH / 3f),
                                end = Offset(p2.x, p1.y + boxH / 3f),
                                strokeWidth = 2f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(p1.x, p1.y + 2f * boxH / 3f),
                                end = Offset(p2.x, p1.y + 2f * boxH / 3f),
                                strokeWidth = 2f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )

                            // Draw high contrast visual corner handles
                            drawCircle(Color.White, 20f, p1)
                            drawCircle(Color(0xFF10AC84), 12f, p1)

                            drawCircle(Color.White, 20f, p2)
                            drawCircle(Color(0xFF10AC84), 12f, p2)

                            drawCircle(Color.White, 20f, p3)
                            drawCircle(Color(0xFF10AC84), 12f, p3)

                            drawCircle(Color.White, 20f, p4)
                            drawCircle(Color(0xFF10AC84), 12f, p4)
                        }

                        // Absolute positioned draggable center overlay box
                        val cropWidthDp = with(density) { ((cropRight - cropLeft) * displayedImageWidth).toDp() }
                        val cropHeightDp = with(density) { ((cropBottom - cropTop) * displayedImageHeight).toDp() }
                        val cropLeftDp = with(density) { (imageLeft + cropLeft * displayedImageWidth).toDp() }
                        val cropTopDp = with(density) { (imageTop + cropTop * displayedImageHeight).toDp() }

                        Box(
                            modifier = Modifier
                                .offset(x = cropLeftDp, y = cropTopDp)
                                .size(width = cropWidthDp, height = cropHeightDp)
                                .pointerInput(displayedImageWidth, displayedImageHeight, cropLeft, cropRight, cropTop, cropBottom) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val dx = dragAmount.x / displayedImageWidth
                                        val dy = dragAmount.y / displayedImageHeight
                                        
                                        val currentWidth = cropRight - cropLeft
                                        val currentHeight = cropBottom - cropTop
                                        
                                        val newLeft = (cropLeft + dx).coerceIn(0f, 1f - currentWidth)
                                        val newTop = (cropTop + dy).coerceIn(0f, 1f - currentHeight)
                                        
                                        cropLeft = newLeft
                                        cropRight = newLeft + currentWidth
                                        cropTop = newTop
                                        cropBottom = newTop + currentHeight
                                    }
                                }
                                .testTag("crop_center_drag_area"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Draggable indicator
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = "Drag Position",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    .padding(6.dp)
                            )
                        }

                        // Draggable Touch Corner Boxes on top of center region for priority response
                        // Top Left Corner Drag handle
                        val topLeftX = with(density) { (imageLeft + cropLeft * displayedImageWidth).toDp() }
                        val topLeftY = with(density) { (imageTop + cropTop * displayedImageHeight).toDp() }
                        Box(
                            modifier = Modifier
                                .offset(x = topLeftX - 28.dp, y = topLeftY - 28.dp)
                                .size(56.dp)
                                .pointerInput(displayedImageWidth, displayedImageHeight, activeRatioPreset, cropRight, cropBottom) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val dx = dragAmount.x / displayedImageWidth
                                        val dy = dragAmount.y / displayedImageHeight
                                        
                                        if (activeRatioPreset == "free") {
                                            cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.05f)
                                            cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.05f)
                                        } else {
                                            val targetRatio = when (activeRatioPreset) {
                                                "1:1" -> 1.0f
                                                "16:9" -> 16f / 9f
                                                "4:3" -> 4f / 3f
                                                else -> 1.0f
                                            }
                                            var newLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.05f)
                                            val targetPixelWidth = (cropRight - newLeft) * bitmapWidth
                                            val targetPixelHeight = targetPixelWidth / targetRatio
                                            var newTop = cropBottom - (targetPixelHeight / bitmapHeight)
                                            
                                            if (newTop < 0f) {
                                                newTop = 0f
                                                val allowedPixelHeight = (cropBottom - newTop) * bitmapHeight
                                                val allowedPixelWidth = allowedPixelHeight * targetRatio
                                                newLeft = cropRight - (allowedPixelWidth / bitmapWidth)
                                            } else if (newTop > cropBottom - 0.05f) {
                                                newTop = cropBottom - 0.05f
                                                val allowedPixelHeight = (cropBottom - newTop) * bitmapHeight
                                                val allowedPixelWidth = allowedPixelHeight * targetRatio
                                                newLeft = cropRight - (allowedPixelWidth / bitmapWidth)
                                            }
                                            
                                            cropLeft = newLeft.coerceIn(0f, cropRight - 0.05f)
                                            cropTop = newTop.coerceIn(0f, cropBottom - 0.05f)
                                        }
                                    }
                                }
                                .testTag("crop_handle_top_left")
                        )

                        // Top Right Corner Drag handle
                        val topRightX = with(density) { (imageLeft + cropRight * displayedImageWidth).toDp() }
                        val topRightY = with(density) { (imageTop + cropTop * displayedImageHeight).toDp() }
                        Box(
                            modifier = Modifier
                                .offset(x = topRightX - 28.dp, y = topRightY - 28.dp)
                                .size(56.dp)
                                .pointerInput(displayedImageWidth, displayedImageHeight, activeRatioPreset, cropLeft, cropBottom) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val dx = dragAmount.x / displayedImageWidth
                                        val dy = dragAmount.y / displayedImageHeight
                                        
                                        if (activeRatioPreset == "free") {
                                            cropRight = (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f)
                                            cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.05f)
                                        } else {
                                            val targetRatio = when (activeRatioPreset) {
                                                "1:1" -> 1.0f
                                                "16:9" -> 16f / 9f
                                                "4:3" -> 4f / 3f
                                                else -> 1.0f
                                            }
                                            var newRight = (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f)
                                            val targetPixelWidth = (newRight - cropLeft) * bitmapWidth
                                            val targetPixelHeight = targetPixelWidth / targetRatio
                                            var newTop = cropBottom - (targetPixelHeight / bitmapHeight)
                                            
                                            if (newTop < 0f) {
                                                newTop = 0f
                                                val allowedPixelHeight = (cropBottom - newTop) * bitmapHeight
                                                val allowedPixelWidth = allowedPixelHeight * targetRatio
                                                newRight = cropLeft + (allowedPixelWidth / bitmapWidth)
                                            } else if (newTop > cropBottom - 0.05f) {
                                                newTop = cropBottom - 0.05f
                                                val allowedPixelHeight = (cropBottom - newTop) * bitmapHeight
                                                val allowedPixelWidth = allowedPixelHeight * targetRatio
                                                newRight = cropLeft + (allowedPixelWidth / bitmapWidth)
                                            }
                                            
                                            cropRight = newRight.coerceIn(cropLeft + 0.05f, 1f)
                                            cropTop = newTop.coerceIn(0f, cropBottom - 0.05f)
                                        }
                                    }
                                }
                                .testTag("crop_handle_top_right")
                        )

                        // Bottom Right Corner Drag handle
                        val bottomRightX = with(density) { (imageLeft + cropRight * displayedImageWidth).toDp() }
                        val bottomRightY = with(density) { (imageTop + cropBottom * displayedImageHeight).toDp() }
                        Box(
                            modifier = Modifier
                                .offset(x = bottomRightX - 28.dp, y = bottomRightY - 28.dp)
                                .size(56.dp)
                                .pointerInput(displayedImageWidth, displayedImageHeight, activeRatioPreset, cropLeft, cropTop) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val dx = dragAmount.x / displayedImageWidth
                                        val dy = dragAmount.y / displayedImageHeight
                                        
                                        if (activeRatioPreset == "free") {
                                            cropRight = (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f)
                                            cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.05f, 1f)
                                        } else {
                                            val targetRatio = when (activeRatioPreset) {
                                                "1:1" -> 1.0f
                                                "16:9" -> 16f / 9f
                                                "4:3" -> 4f / 3f
                                                else -> 1.0f
                                            }
                                            var newRight = (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f)
                                            val targetPixelWidth = (newRight - cropLeft) * bitmapWidth
                                            val targetPixelHeight = targetPixelWidth / targetRatio
                                            var newBottom = cropTop + (targetPixelHeight / bitmapHeight)
                                            
                                            if (newBottom > 1f) {
                                                newBottom = 1f
                                                val allowedPixelHeight = (newBottom - cropTop) * bitmapHeight
                                                val allowedPixelWidth = allowedPixelHeight * targetRatio
                                                newRight = cropLeft + (allowedPixelWidth / bitmapWidth)
                                            } else if (newBottom < cropTop + 0.05f) {
                                                newBottom = cropTop + 0.05f
                                                val allowedPixelHeight = (newBottom - cropTop) * bitmapHeight
                                                val allowedPixelWidth = allowedPixelHeight * targetRatio
                                                newRight = cropLeft + (allowedPixelWidth / bitmapWidth)
                                            }
                                            
                                            cropRight = newRight.coerceIn(cropLeft + 0.05f, 1f)
                                            cropBottom = newBottom.coerceIn(cropTop + 0.05f, 1f)
                                        }
                                    }
                                }
                                .testTag("crop_handle_bottom_right")
                        )

                        // Bottom Left Corner Drag handle
                        val bottomLeftX = with(density) { (imageLeft + cropLeft * displayedImageWidth).toDp() }
                        val bottomLeftY = with(density) { (imageTop + cropBottom * displayedImageHeight).toDp() }
                        Box(
                            modifier = Modifier
                                .offset(x = bottomLeftX - 28.dp, y = bottomLeftY - 28.dp)
                                .size(56.dp)
                                .pointerInput(displayedImageWidth, displayedImageHeight, activeRatioPreset, cropRight, cropTop) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val dx = dragAmount.x / displayedImageWidth
                                        val dy = dragAmount.y / displayedImageHeight
                                        
                                        if (activeRatioPreset == "free") {
                                            cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.05f)
                                            cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.05f, 1f)
                                        } else {
                                            val targetRatio = when (activeRatioPreset) {
                                                "1:1" -> 1.0f
                                                "16:9" -> 16f / 9f
                                                "4:3" -> 4f / 3f
                                                else -> 1.0f
                                            }
                                            var newLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.05f)
                                            val targetPixelWidth = (cropRight - newLeft) * bitmapWidth
                                            val targetPixelHeight = targetPixelWidth / targetRatio
                                            var newBottom = cropTop + (targetPixelHeight / bitmapHeight)
                                            
                                            if (newBottom > 1f) {
                                                newBottom = 1f
                                                val allowedPixelHeight = (newBottom - cropTop) * bitmapHeight
                                                val allowedPixelWidth = allowedPixelHeight * targetRatio
                                                newLeft = cropRight - (allowedPixelWidth / bitmapWidth)
                                            } else if (newBottom < cropTop + 0.05f) {
                                                newBottom = cropTop + 0.05f
                                                val allowedPixelHeight = (newBottom - cropTop) * bitmapHeight
                                                val allowedPixelWidth = allowedPixelHeight * targetRatio
                                                newLeft = cropRight - (allowedPixelWidth / bitmapWidth)
                                            }
                                            
                                            cropLeft = newLeft.coerceIn(0f, cropRight - 0.05f)
                                            cropBottom = newBottom.coerceIn(cropTop + 0.05f, 1f)
                                        }
                                    }
                                }
                                .testTag("crop_handle_bottom_left")
                        )
                    }
                } else {
                    // Display Beautiful Upload Image Canvas when scanner steps are empty
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                            .border(BorderStroke(2.dp, Color(0xFF10AC84).copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload To Canvas",
                                tint = Color(0xFF10AC84),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "PDF Crop Studio Canvas",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No image loaded yet. Tap here to select and upload an image from your device gallery to start cropping, processing and signing!",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10AC84)),
                                modifier = Modifier.testTag("gallery_upload_trigger_button")
                            ) {
                                Icon(Icons.Default.Add, "Select Image")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Image from Gallery")
                            }
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Filters & Flattening", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(Screen.ScanCamera) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Camera")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                viewModel.compileScannedDoc(titleText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save PDF")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Check, "Save Compile")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Document Filename") },
                    placeholder = { Text("e.g. Tax Receipt Scan 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Carousel preview of pages
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.scannerStepBitmaps.isNotEmpty()) {
                        val baseBmp = state.scannerStepBitmaps.first()
                        val filteredBmp = remember(baseBmp, state.scannerFilterType) {
                            viewModel.applyFilterToBitmap(baseBmp, state.scannerFilterType)
                        }
                        Image(
                            bitmap = filteredBmp.asImageBitmap(), // Preview thumbnail
                            contentDescription = "Previews compiled",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Inside
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { isCropping = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Crop, "Crop Page")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crop Page & Adjust Edges", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Apply Document Filters:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Pair("original", "Original"),
                        Pair("black_white", "B&W Document"),
                        Pair("grayscale", "Grayscale"),
                        Pair("enhance", "High Color")
                    ).forEach { (filterVal, display) ->
                        val isSel = selectedFilter == filterVal
                        OutlinedButton(
                            onClick = {
                                selectedFilter = filterVal
                                viewModel.applyFilterToScans(filterVal)
                            },
                            border = BorderStroke(iif = isSel, col = MaterialTheme.colorScheme.primary, defaultCol = Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Text(display, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Helper to draw clean borders
fun BorderStroke(iif: Boolean, col: Color, defaultCol: Color) = BorderStroke(
    width = if (iif) 2.dp else 1.dp,
    color = if (iif) col else defaultCol
)


@Composable
fun DrawingSettingsToolbar(
    drawColorHex: String,
    onColorSelected: (String) -> Unit,
    drawStrokeWidth: Float,
    onStrokeWidthChanged: (Float) -> Unit,
    drawPenType: String,
    onPenTypeChanged: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color.LightGray.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Brush & Pen Customizer",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "pen" to "Solid",
                        "highlighter" to "Marker",
                        "dashed" to "Dash"
                    ).forEach { (typeKey, typeVal) ->
                        val isSelected = drawPenType == typeKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onPenTypeChanged(typeKey) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeVal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1.3f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Size: ${drawStrokeWidth.toInt()}px",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = drawStrokeWidth,
                        onValueChange = onStrokeWidthChanged,
                        valueRange = 2f..40f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "#D62246" to Color(0xFFD62246), // Red
                        "#005FB8" to Color(0xFF005FB8), // Blue
                        "#10AC84" to Color(0xFF10AC84), // Green
                        "#F5A623" to Color(0xFFF5A623), // Yellow
                        "#000000" to Color.Black,       // Black
                        "#FFFFFF" to Color.White        // White
                    ).forEach { (colorKey, colorVal) ->
                        val isSelected = drawColorHex == colorKey
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(colorVal, CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (colorKey == "#FFFFFF") Color.LightGray else Color.White,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(colorKey) }
                        )
                    }
                }
            }
        }
    }
}


// —-- 5. RICH PDF CANVAS EDITOR & ANNOTATOR SCREEN —--

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    EditorScreenRemoved(state, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreenRemoved(
    state: UiState,
    viewModel: MainViewModel
) {
    val activeDoc = state.activeDocument ?: return
    val context = LocalContext.current
    val pages = state.activeDocumentContent.pages
    val activePage = pages.find { it.id == state.activePageId } ?: pages.firstOrNull() ?: return
    
    var showDeletePageConfirm by remember { mutableStateOf(false) }
    var showCloseConfirmDialog by remember { mutableStateOf(false) }
    var showPageEditorControls by remember { mutableStateOf(true) }
    var showPageWorkbench by remember { mutableStateOf(false) }
    
    var showSignatureSelectionDrawer by remember { mutableStateOf(false) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var annotationMode by remember { mutableStateOf("none") } // "none", "draw", "erase", "signature_overlay"
    var drawColorHex by remember { mutableStateOf("#D62246") } // Defaults to Red Pencil
    var drawStrokeWidth by remember { mutableFloatStateOf(6f) }
    var drawPenType by remember { mutableStateOf("pen") } // "pen", "highlighter", "dashed"
    var drawShapeType by remember { mutableStateOf("brush") } // "brush", "line", "box", "circle", "select"
    var selectedDrawingId by remember { mutableStateOf<String?>(null) }
    var textStickerDraft by remember { mutableStateOf("") }
    var isAddingTextMode by remember { mutableStateOf(false) }
 
    var showPreciseTextPlacementDialog by remember { mutableStateOf(false) }
    var precisePageNumberText by remember { mutableStateOf("") }
    var preciseXText by remember { mutableStateOf("0.50") }
    var preciseYText by remember { mutableStateOf("0.50") }
    var preciseTextContent by remember { mutableStateOf("New Independent Text Annotation") }

    var showEditWordDialog by remember { mutableStateOf(false) }
    var selectedWordToEdit by remember { mutableStateOf<TextAnnotationDef?>(null) }
    var selectedAnnotationId by remember { mutableStateOf<String?>(null) }
    var editWordTextDraft by remember { mutableStateOf("") }
 
    var showAddWordDialog by remember { mutableStateOf(false) }
    var addWordX by remember { mutableFloatStateOf(0f) }
    var addWordY by remember { mutableFloatStateOf(0f) }
    var addWordTextDraft by remember { mutableStateOf("") }
    var addWordFontFamily by remember { mutableStateOf("arial") }
    var editWordFontFamily by remember { mutableStateOf("arial") }
    var addWordFontSize by remember { mutableFloatStateOf(14f) }
    var editWordFontSize by remember { mutableFloatStateOf(14f) }
    var addWordColorHex by remember { mutableStateOf("#000000") }
    var editWordColorHex by remember { mutableStateOf("#000000") }
    var addWordBgColorHex by remember { mutableStateOf("transparent") }
    var editWordBgColorHex by remember { mutableStateOf("transparent") }
    var addWordHasOutline by remember { mutableStateOf(false) }
    var editWordHasOutline by remember { mutableStateOf(false) }
    var addWordHasUnderline by remember { mutableStateOf(false) }
    var editWordHasUnderline by remember { mutableStateOf(false) }
    var addWordIsPowerOf by remember { mutableStateOf(false) }
    var editWordIsPowerOf by remember { mutableStateOf(false) }
    var addWordIsItalic by remember { mutableStateOf(false) }
    var editWordIsItalic by remember { mutableStateOf(false) }
    var addWordHasStrikeThrough by remember { mutableStateOf(false) }
    var editWordHasStrikeThrough by remember { mutableStateOf(false) }
    var addWordIsBold by remember { mutableStateOf(false) }
    var editWordIsBold by remember { mutableStateOf(false) }
    var addWordAlignment by remember { mutableStateOf("left") } // "left", "center", "right"
    var editWordAlignment by remember { mutableStateOf("left") }
    var addWordOutlineColorHex by remember { mutableStateOf("#000000") }
    var editWordOutlineColorHex by remember { mutableStateOf("#000000") }

    val currentDrawings = remember { mutableStateListOf<DrawingDef>() }
    val currentTextAnns = remember { mutableStateListOf<TextAnnotationDef>() }
    val currentSignatures = remember { mutableStateListOf<SignatureOverlayDef>() }

    var lastActivePageId by remember { mutableStateOf("") }
    LaunchedEffect(activePage) {
        currentDrawings.colorsSync(activePage.drawings)
        currentTextAnns.annotationSync(activePage.textAnnotations)
        currentSignatures.signatureSync(activePage.signatures)
        if (activePage.id != lastActivePageId) {
            selectedAnnotationId = null
            lastActivePageId = activePage.id
        }
    }

    // Pan, Zoom, Rotate and Aspect orientation states
    var activeDraggingId by remember { mutableStateOf<String?>(null) }
    var scale by remember(activePage.id) { mutableStateOf(1f) }
    var offsetX by remember(activePage.id) { mutableStateOf(0f) }
    var offsetY by remember(activePage.id) { mutableStateOf(0f) }
    var pageMeasuredWidthPx by remember(activePage.id) { mutableStateOf(400f) }
    var pageMeasuredHeightPx by remember(activePage.id) { mutableStateOf(600f) }
    var pagePositionOnScreenX by remember(activePage.id) { mutableStateOf(0f) }
    var pagePositionOnScreenY by remember(activePage.id) { mutableStateOf(0f) }
    var pageRotationDegrees by remember(activePage.id) { mutableStateOf(activePage.rotationDegrees) }
    var isHorizontalOverride by remember(activePage.id) { mutableStateOf<Boolean?>(null) }

    val isModified = remember(state.activeDocumentContent, activeDoc.contentJson) {
        val currentJson = DocumentSerializer.toJson(state.activeDocumentContent)
        currentJson != activeDoc.contentJson
    }

    val isLandscape = remember(activePage.id, isHorizontalOverride) {
        if (isHorizontalOverride != null) {
            isHorizontalOverride == true
        } else {
            if (activePage.backgroundScanPath != null) {
                val file = java.io.File(activePage.backgroundScanPath)
                if (file.exists()) {
                    val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                    options.outWidth > options.outHeight
                } else false
            } else false
        }
    }

    val editorFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importAndAddPageToEditor(context, uri)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var activeEditorTab by remember { mutableStateOf("view") }
    var isPresentationMode by remember { mutableStateOf(false) }
    var isContinuousScrollMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activeDoc.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Page ${pages.indexOf(activePage) + 1} of ${pages.size}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isModified) {
                                    showCloseConfirmDialog = true
                                } else {
                                    viewModel.navigateTo(Screen.Dashboard)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dashboard")
                        }
                    },
                    actions = {
                        // Undo action
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = viewModel.canUndo()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (viewModel.canUndo()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                            )
                        }

                        // Redo action
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = viewModel.canRedo()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (viewModel.canRedo()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Quick Share
                        IconButton(
                            onClick = {
                                viewModel.saveEditorChanges()
                                sharePdfFile(context, activeDoc)
                                viewModel.navigateTo(Screen.Dashboard)
                            }
                        ) {
                            Icon(Icons.Default.Share, "Share PDF", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Save & Close
                        Button(
                            onClick = {
                                viewModel.saveEditorChanges()
                                viewModel.navigateTo(Screen.Dashboard)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, "Save Changes", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // 2. MAIN NAVIGATION TAB BAR
                BottomAppBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    tonalElevation = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "view" to ("Preview" to Icons.Default.Visibility),
                            "annotate" to ("Draw" to Icons.Default.Gesture),
                            "text" to ("Text" to Icons.Default.TextFields),
                            "signature" to ("Sign" to Icons.Default.Draw),
                            "filters" to ("Filters" to Icons.Default.FilterAlt),
                            "organize" to ("Pages" to Icons.Default.ViewAgenda)
                        ).forEach { (tabId, pair) ->
                            val (label, icon) = pair
                            val isSelected = activeEditorTab == tabId
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeEditorTab = tabId }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        // Sync active state variables based on selected tab mode
        LaunchedEffect(activeEditorTab) {
            when (activeEditorTab) {
                "view" -> {
                    annotationMode = "none"
                    isAddingTextMode = false
                }
                "annotate" -> {
                    annotationMode = "draw"
                    isAddingTextMode = false
                }
                "text" -> {
                    annotationMode = "none"
                    isAddingTextMode = true
                }
                "signature" -> {
                    annotationMode = "signature_overlay"
                    isAddingTextMode = false
                }
                "filters" -> {
                    annotationMode = "none"
                    isAddingTextMode = false
                }
                "organize" -> {
                    annotationMode = "none"
                    isAddingTextMode = false
                }
            }
        }

        val contextualOptionsPanel = @Composable {
            // 1. CONTEXTUAL OPTIONS PANEL FOR CURRENTLY ACTIVE TAB
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    when (activeEditorTab) {
                        "view" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("PREVIEW SETTINGS & DISPLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                                        Text("Manage display viewport settings and continuous flow layouts", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${pages.size} PAGES DETECTED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Single Page focus tab
                                    FilterChip(
                                        selected = !isContinuousScrollMode,
                                        onClick = { isContinuousScrollMode = false },
                                        label = { Text("Single Page", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    // Continuous flow view tab
                                    FilterChip(
                                        selected = isContinuousScrollMode,
                                        onClick = { isContinuousScrollMode = true },
                                        label = { Text("Continuous Scroll", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ViewStream,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Fullscreen Presentation Mode Button
                                    IconButton(
                                        onClick = { isPresentationMode = true },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Presentation Preview",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Rotate 90
                                    IconButton(
                                        onClick = {
                                            val newRotation = (pageRotationDegrees + 90) % 360
                                            pageRotationDegrees = newRotation
                                            viewModel.editActivePageRotation(newRotation)
                                        },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CropRotate,
                                            contentDescription = "Rotate 90 degrees",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Delete Page
                                    IconButton(
                                        onClick = { showDeletePageConfirm = true },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Remove page",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        "annotate" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tool: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        listOf(
                                            "brush" to Icons.Default.Gesture,
                                            "line" to Icons.Default.Remove,
                                            "box" to Icons.Default.BorderOuter,
                                            "filled_box" to Icons.Default.Square,
                                            "circle" to Icons.Default.Lens,
                                            "redact" to Icons.Default.Block,
                                            "select" to Icons.Default.OpenWith
                                        ).forEach { (type, icon) ->
                                            FilledIconButton(
                                                onClick = {
                                                    drawShapeType = type
                                                    if (type != "select") {
                                                        selectedDrawingId = null
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = if (drawShapeType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (drawShapeType == type) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            ) {
                                                Icon(icon, contentDescription = type, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    if (selectedDrawingId != null) {
                                        Button(
                                            onClick = {
                                                val updated = currentDrawings.filter { it.id != selectedDrawingId }
                                                currentDrawings.clear()
                                                currentDrawings.addAll(updated)
                                                viewModel.editActivePageDrawings(updated)
                                                selectedDrawingId = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Shape", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onError)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete Shape", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                                        }
                                    } else if (drawShapeType == "select") {
                                        Text("Tap shape to select", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontStyle = FontStyle.Italic)
                                    } else {
                                        Text("Drag on canvas to draw", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Color: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val colors = listOf(
                                        "#EF4444" to Color(0xFFEF4444),
                                        "#3B82F6" to Color(0xFF3B82F6),
                                        "#10B981" to Color(0xFF10B981),
                                        "#1E293B" to Color(0xFF1E293B),
                                        "#F59E0B" to Color(0xFFF59E0B),
                                        "#8B5CF6" to Color(0xFF8B5CF6),
                                        "#FFFFFF" to Color(0xFFFFFFFF)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        colors.forEach { (hex, col) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(col, CircleShape)
                                                    .border(
                                                        width = if (drawColorHex.lowercase() == hex.lowercase()) 2.dp else 1.dp,
                                                        color = if (drawColorHex.lowercase() == hex.lowercase()) MaterialTheme.colorScheme.onSurface else Color.LightGray.copy(alpha = 0.5f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        drawColorHex = hex
                                                        val selId = selectedDrawingId
                                                        if (selId != null) {
                                                            val idx = currentDrawings.indexOfFirst { it.id == selId }
                                                            if (idx != -1) {
                                                                val updatedDraw = currentDrawings[idx].copy(colorHex = hex)
                                                                currentDrawings[idx] = updatedDraw
                                                                viewModel.editActivePageDrawings(currentDrawings.toList())
                                                            }
                                                        }
                                                    }
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Size: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        listOf(3f to "3px", 6f to "6px", 12f to "12px", 24f to "24px").forEach { (sizeVal, name) ->
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (drawStrokeWidth == sizeVal) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (drawStrokeWidth == sizeVal) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable {
                                                        drawStrokeWidth = sizeVal
                                                        val selId = selectedDrawingId
                                                        if (selId != null) {
                                                            val idx = currentDrawings.indexOfFirst { it.id == selId }
                                                            if (idx != -1) {
                                                                val updatedDraw = currentDrawings[idx].copy(strokeWidth = sizeVal)
                                                                currentDrawings[idx] = updatedDraw
                                                                viewModel.editActivePageDrawings(currentDrawings.toList())
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "text" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Text Annotator Active", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Click button to insert text", fontSize = 10.sp, color = Color.Gray)
                                }

                                                                                                  Row(
                                      horizontalArrangement = Arrangement.spacedBy(18.dp),
                                      verticalAlignment = Alignment.CenterVertically
                                 ) {
                                      // 1. ADD TEXT
                                      Box(
                                          modifier = Modifier
                                              .size(38.dp)
                                              .clickable { 
                                                  addWordX = 0.35f
                                                  addWordY = 0.35f
                                                  addWordTextDraft = ""
                                                  showAddWordDialog = true
                                              },
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Box(
                                              modifier = Modifier
                                                  .size(24.dp)
                                                  .background(Color(0xFFDBEAFE), CircleShape),
                                              contentAlignment = Alignment.Center
                                          ) {
                                              Icon(
                                                  imageVector = Icons.Default.Add,
                                                  contentDescription = "Add Text Box",
                                                  tint = Color(0xFF2563EB),
                                                  modifier = Modifier.size(14.dp)
                                              )
                                          }
                                      }

                                      val selectedAnn = currentTextAnns.find { it.id == selectedAnnotationId }

                                      // 2. EDIT TEXT
                                      Box(
                                          modifier = Modifier
                                              .size(38.dp)
                                              .let {
                                                  if (selectedAnn != null) {
                                                      it.clickable {
                                                          selectedWordToEdit = selectedAnn
                                                          editWordTextDraft = selectedAnn.text
                                                          editWordFontSize = selectedAnn.fontSize
                                                          editWordColorHex = selectedAnn.colorHex
                                                          editWordFontFamily = selectedAnn.fontName
                                                          editWordBgColorHex = selectedAnn.bgColorHex
                                                          editWordHasOutline = selectedAnn.hasOutline
                                                          editWordHasUnderline = selectedAnn.hasUnderline
                                                          editWordOutlineColorHex = selectedAnn.outlineColorHex
                                                          editWordIsBold = selectedAnn.isBold
                                                          editWordAlignment = selectedAnn.alignment
                                                          editWordIsPowerOf = selectedAnn.isPowerOf
                                                          editWordIsItalic = selectedAnn.isItalic
                                                          editWordHasStrikeThrough = selectedAnn.hasStrikeThrough
                                                          showEditWordDialog = true
                                                      }
                                                  } else it
                                              },
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Box(
                                              modifier = Modifier
                                                  .size(24.dp)
                                                  .background(
                                                      if (selectedAnn != null) Color(0xFFDBEAFE) else Color(0xFF94A3B8),
                                                      CircleShape
                                                  ),
                                              contentAlignment = Alignment.Center
                                          ) {
                                              Icon(
                                                  imageVector = Icons.Default.Edit,
                                                  contentDescription = "Edit Text",
                                                  tint = if (selectedAnn != null) Color(0xFF2563EB) else Color.White,
                                                  modifier = Modifier.size(13.dp)
                                              )
                                          }
                                      }

                                      // 3. DUPLICATE TEXT
                                      Box(
                                          modifier = Modifier
                                              .size(38.dp)
                                              .let {
                                                  if (selectedAnn != null) {
                                                      it.clickable {
                                                          selectedAnn.let { ann ->
                                                              val duplicated = ann.copy(
                                                                  id = "word_${System.currentTimeMillis()}",
                                                                  x = (ann.x + 0.03f).coerceIn(0f, 0.9f),
                                                                  y = (ann.y + 0.03f).coerceIn(0f, 0.9f)
                                                              )
                                                              currentTextAnns.add(duplicated)
                                                              viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                                              selectedAnnotationId = duplicated.id
                                                          }
                                                      }
                                                  } else it
                                              },
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Box(
                                              modifier = Modifier
                                                  .size(24.dp)
                                                  .background(
                                                      if (selectedAnn != null) Color(0xFFD1FAE5) else Color(0xFF94A3B8),
                                                      CircleShape
                                                  ),
                                              contentAlignment = Alignment.Center
                                          ) {
                                              Icon(
                                                  imageVector = Icons.Default.ContentCopy,
                                                  contentDescription = "Duplicate Text",
                                                  tint = if (selectedAnn != null) Color(0xFF059669) else Color.White,
                                                  modifier = Modifier.size(13.dp)
                                              )
                                          }
                                      }

                                      // 4. DECREASE SIZE
                                      Box(
                                          modifier = Modifier
                                              .size(38.dp)
                                              .let {
                                                  if (selectedAnn != null) {
                                                      it.clickable {
                                                          selectedAnn.let { ann ->
                                                              val newSize = (ann.fontSize - 1f).coerceAtLeast(6f)
                                                              val updated = ann.copy(fontSize = newSize)
                                                              val idx = currentTextAnns.indexOfFirst { it.id == ann.id }
                                                              if (idx != -1) {
                                                                  currentTextAnns[idx] = updated
                                                                  viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                                              }
                                                          }
                                                      }
                                                  } else it
                                              },
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Box(
                                              modifier = Modifier
                                                  .size(24.dp)
                                                  .background(
                                                      if (selectedAnn != null) Color(0xFFFEF3C7) else Color(0xFF94A3B8),
                                                      CircleShape
                                                  ),
                                              contentAlignment = Alignment.Center
                                          ) {
                                              Text(
                                                  text = "A",
                                                  fontSize = 8.sp,
                                                  fontWeight = FontWeight.Bold,
                                                  color = if (selectedAnn != null) Color(0xFFD97706) else Color.White
                                              )
                                          }
                                      }

                                      // 5. INCREASE SIZE
                                      Box(
                                          modifier = Modifier
                                              .size(38.dp)
                                              .let {
                                                  if (selectedAnn != null) {
                                                      it.clickable {
                                                          selectedAnn.let { ann ->
                                                              val newSize = (ann.fontSize + 1f).coerceAtMost(100f)
                                                              val updated = ann.copy(fontSize = newSize)
                                                              val idx = currentTextAnns.indexOfFirst { it.id == ann.id }
                                                              if (idx != -1) {
                                                                  currentTextAnns[idx] = updated
                                                                  viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                                              }
                                                          }
                                                      }
                                                  } else it
                                              },
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Box(
                                              modifier = Modifier
                                                  .size(24.dp)
                                                  .background(
                                                      if (selectedAnn != null) Color(0xFFFEF3C7) else Color(0xFF94A3B8),
                                                      CircleShape
                                                  ),
                                              contentAlignment = Alignment.Center
                                          ) {
                                              Text(
                                                  text = "A",
                                                  fontSize = 14.sp,
                                                  fontWeight = FontWeight.Bold,
                                                  color = if (selectedAnn != null) Color(0xFFD97706) else Color.White
                                              )
                                          }
                                      }
                                 }
                            }
                        }
                        "signature" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Saved Signature Profiles", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    
                                    Button(
                                        onClick = { viewModel.navigateTo(Screen.SignatureStudio) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Signature Studio", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // "A,A" size controls under the "+Signature Studio" button
                                val selectedSig = currentSignatures.find { it.id == selectedAnnotationId }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedSig != null) {
                                        Text(
                                            text = "Selected Signature Size: ",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    
                                    // 1. Decrease signature size
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (selectedSig != null) Color(0xFFFEF3C7) else Color(0xFF94A3B8).copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                            .clickable(enabled = selectedSig != null) {
                                                selectedSig?.let { sig ->
                                                    val newWidth = (sig.width * 0.85f).coerceAtLeast(20f)
                                                    val newHeight = (sig.height * 0.85f).coerceAtLeast(10f)
                                                    val idx = currentSignatures.indexOfFirst { it.id == sig.id }
                                                    if (idx != -1) {
                                                        currentSignatures[idx] = sig.copy(width = newWidth, height = newHeight)
                                                        viewModel.editActivePageSignatures(currentSignatures.toList())
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "A",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedSig != null) Color(0xFFD97706) else Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // 2. Increase signature size
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (selectedSig != null) Color(0xFFFEF3C7) else Color(0xFF94A3B8).copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                            .clickable(enabled = selectedSig != null) {
                                                selectedSig?.let { sig ->
                                                    val newWidth = (sig.width * 1.15f).coerceAtMost(600f)
                                                    val newHeight = (sig.height * 1.15f).coerceAtMost(300f)
                                                    val idx = currentSignatures.indexOfFirst { it.id == sig.id }
                                                    if (idx != -1) {
                                                        currentSignatures[idx] = sig.copy(width = newWidth, height = newHeight)
                                                        viewModel.editActivePageSignatures(currentSignatures.toList())
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "A",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedSig != null) Color(0xFFD97706) else Color.White
                                        )
                                    }
                                }

                                if (state.signatures.isEmpty()) {
                                    Text(
                                        text = "No profiles found. Tap 'Signature Studio' to draw or import one.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(state.signatures) { sig ->
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        // Instant placement in the visible center of the page
                                                        val spawnX = (0.5f - (offsetX / (scale * pageMeasuredWidthPx.coerceAtLeast(1f)))).coerceIn(0.05f, 0.85f)
                                                        val spawnY = (0.5f - (offsetY / (scale * pageMeasuredHeightPx.coerceAtLeast(1f)))).coerceIn(0.05f, 0.85f)
                                                        
                                                        var finalWidth = 110f
                                                        var finalHeight = 55f
                                                        if (sig.pathDataJson.startsWith("image:")) {
                                                            try {
                                                                val path = sig.pathDataJson.removePrefix("image:")
                                                                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                                android.graphics.BitmapFactory.decodeFile(path, opts)
                                                                if (opts.outWidth > 0 && opts.outHeight > 0) {
                                                                    finalWidth = 110f
                                                                    finalHeight = finalWidth / (opts.outWidth.toFloat() / opts.outHeight.toFloat())
                                                                }
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        }

                                                        val overlay = SignatureOverlayDef(
                                                            id = java.util.UUID.randomUUID().toString(),
                                                            x = spawnX,
                                                            y = spawnY,
                                                            width = finalWidth,
                                                            height = finalHeight,
                                                            signatureProfileId = sig.id
                                                        )
                                                        viewModel.addSignatureOverlay(overlay)
                                                        // Keep signature tab open
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(sig.alias, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "filters" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Enhance Scanned Document", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "original" to "Original",
                                        "black_white" to "B&W Mono",
                                        "grayscale" to "Grayscale",
                                        "enhance" to "Enhance Doc"
                                    ).forEach { (filterType, name) ->
                                        val isSelected = activePage.filterType == filterType
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.editActivePageFilter(filterType) },
                                            label = { Text(name, fontSize = 11.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = when (filterType) {
                                                        "original" -> Icons.Default.Image
                                                        "black_white" -> Icons.Default.Contrast
                                                        "grayscale" -> Icons.Default.Lens
                                                        else -> Icons.Default.AutoAwesome
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "organize" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Page Manager & Reorder", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    
                                    Button(
                                        onClick = { editorFilePickerLauncher.launch("*/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Append Page", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    itemsIndexed(pages) { index, page ->
                                        val isActive = page.id == activePage.id
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 75.dp, height = 100.dp)
                                                    .background(
                                                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.White,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        border = BorderStroke(
                                                            width = if (isActive) 2.dp else 1.dp,
                                                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.6f)
                                                        ),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { viewModel.setActivePageId(page.id) }
                                            ) {
                                                if (page.backgroundScanPath != null && java.io.File(page.backgroundScanPath).exists()) {
                                                    val bitmap = remember(page.id, page.backgroundScanPath) { AndroidBitmapLoader.load(page.backgroundScanPath) }
                                                    if (bitmap != null) {
                                                        Image(
                                                            bitmap = bitmap.asImageBitmap(),
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(4.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Book,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = page.type.uppercase(),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                
                                                // Badge Page Index Number
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(4.dp)
                                                        .background(
                                                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                                                            shape = CircleShape
                                                        )
                                                        .size(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Actions Row: Left, Right, Delete
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (index > 0) {
                                                            val mutablePages = pages.toMutableList()
                                                            val temp = mutablePages[index]
                                                            mutablePages[index] = mutablePages[index - 1]
                                                            mutablePages[index - 1] = temp
                                                            viewModel.reorderActivePages(mutablePages)
                                                        }
                                                    },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Move Left", modifier = Modifier.size(11.dp))
                                                }
                                                
                                                IconButton(
                                                    onClick = {
                                                        viewModel.removePageFromActiveDocument(page.id)
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, "Remove Page", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
                                                }
                                                
                                                IconButton(
                                                    onClick = {
                                                        if (index < pages.size - 1) {
                                                            val mutablePages = pages.toMutableList()
                                                            val temp = mutablePages[index]
                                                            mutablePages[index] = mutablePages[index + 1]
                                                            mutablePages[index + 1] = temp
                                                            viewModel.reorderActivePages(mutablePages)
                                                        }
                                                    },
                                                    enabled = index < pages.size - 1,
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowForward, "Move Right", modifier = Modifier.size(11.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val pageWorkbenchPanel = @Composable {
            // ---------------- PAGE WORKBENCH PANEL ----------------
            if (showPageWorkbench) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DOCUMENT WORKBENCH (${pages.size} Pages)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { showPageWorkbench = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Workbench",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(pages) { index, page ->
                                val isActive = page.id == activePage.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 100.dp, height = 135.dp)
                                            .background(
                                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.White,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                border = BorderStroke(
                                                    width = if (isActive) 2.5.dp else 1.dp,
                                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.6f)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setActivePageId(page.id) }
                                    ) {
                                        if (page.backgroundScanPath != null && File(page.backgroundScanPath).exists()) {
                                            val bitmap = remember(page.id, page.backgroundScanPath) { AndroidBitmapLoader.load(page.backgroundScanPath) }
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Book,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = page.type.uppercase(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        
                                        // Badge Page Index Number
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .background(
                                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f),
                                                    shape = CircleShape
                                                )
                                                .size(20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    // Row of Workbench Page Actions: Left, Right, Delete
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Move Page Left/Up button
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val mutablePages = pages.toMutableList()
                                                    val temp = mutablePages[index]
                                                    mutablePages[index] = mutablePages[index - 1]
                                                    mutablePages[index - 1] = temp
                                                    viewModel.reorderActivePages(mutablePages)
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Move Left",
                                                tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        
                                        // Remove Page button
                                        IconButton(
                                            onClick = {
                                                viewModel.removePageFromActiveDocument(page.id)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Page",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        
                                        // Move Page Right/Down button
                                        IconButton(
                                            onClick = {
                                                if (index < pages.size - 1) {
                                                    val mutablePages = pages.toMutableList()
                                                    val temp = mutablePages[index]
                                                    mutablePages[index] = mutablePages[index + 1]
                                                    mutablePages[index + 1] = temp
                                                    viewModel.reorderActivePages(mutablePages)
                                                }
                                            },
                                            enabled = index < pages.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Move Right",
                                                tint = if (index < pages.size - 1) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A)) // Premium deep charcoal canvas desk workspace
        ) {
            // Central Document Page A4 Canvas sheet
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color.DarkGray), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(activePage.id, annotationMode, isAddingTextMode, activeDraggingId) {
                        if (annotationMode != "draw" && activeDraggingId == null) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            )
                        }
                    }
                    .pointerInput(activePage.id, annotationMode, isAddingTextMode, activeDraggingId) {
                        if (annotationMode != "draw" && activeDraggingId == null) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                    }
            ) {
                if (isContinuousScrollMode) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(pages) { index, page ->
                            val isCurrent = page.id == activePage.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.setActivePageId(page.id) }
                                    .widthIn(max = 450.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = if (isCurrent) 2.dp else 1.dp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    StaticPagePreview(
                                        page = page,
                                        state = state,
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Page ${index + 1} of ${pages.size}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    val pageRatio = remember(activePage.id, isLandscape, activePage.backgroundScanPath) {
                    if (activePage.backgroundScanPath != null) {
                        val file = java.io.File(activePage.backgroundScanPath)
                        if (file.exists()) {
                            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                            if (options.outWidth > 0 && options.outHeight > 0) {
                                options.outWidth.toFloat() / options.outHeight.toFloat()
                            } else {
                                if (isLandscape) 1.414f else 0.707f
                            }
                        } else {
                            if (isLandscape) 1.414f else 0.707f
                        }
                    } else {
                        if (isLandscape) 1.414f else 0.707f
                    }
                }
                
                // Nested transformation container that scales, translates, and rotates dynamically!
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .aspectRatio(pageRatio, matchHeightConstraintsFirst = true)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                            rotationZ = pageRotationDegrees.toFloat()
                        )
                        .background(Color.White)
                        .shadow(4.dp, RoundedCornerShape(2.dp))
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                val pos = coordinates.positionOnScreen()
                                pagePositionOnScreenX = pos.x
                                pagePositionOnScreenY = pos.y
                            }
                    ) {
                        val density = LocalDensity.current
                        val widthDp = maxWidth
                        val heightDp = maxHeight
                        val widthPx = with(density) { widthDp.toPx() }
                        val heightPx = with(density) { heightDp.toPx() }

                        LaunchedEffect(widthPx, heightPx) {
                            pageMeasuredWidthPx = widthPx
                            pageMeasuredHeightPx = heightPx
                        }

                    // 1. Render Background Scan Image Thumbnail safely AT THE VERY BOTTOM!
                    if (activePage.backgroundScanPath != null) {
                        val file = File(activePage.backgroundScanPath)
                        if (file.exists()) {
                            val bitmap = remember(activePage.id, activePage.filterType, activePage.backgroundScanPath) {
                                val base = AndroidBitmapLoader.load(file.absolutePath)
                                if (base != null && activePage.filterType != "original") {
                                    val filtered = viewModel.applyFilterToBitmap(base, activePage.filterType)
                                    if (filtered != base) {
                                        base.recycle()
                                    }
                                    filtered
                                } else {
                                    base
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    // 2. 2D Gesture-Interactive Sheet Canvas ON TOP OF THE IMAGE!
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activePage.id, annotationMode, isAddingTextMode, drawShapeType) {
                                if (annotationMode == "draw") {
                                    if (drawShapeType == "select") {
                                        var dragTargetId: String? = null
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val rx = offset.x / size.width
                                                val ry = offset.y / size.height
                                                var found: DrawingDef? = null
                                                for (drawing in currentDrawings.asReversed()) {
                                                    if (isTapNearDrawing(rx, ry, drawing)) {
                                                        found = drawing
                                                        break
                                                    }
                                                }
                                                selectedDrawingId = found?.id
                                                dragTargetId = found?.id
                                            },
                                            onDragEnd = {
                                                dragTargetId = null
                                                viewModel.editActivePageDrawings(currentDrawings.toList())
                                            },
                                            onDragCancel = {
                                                dragTargetId = null
                                            },
                                            onDrag = { change, dragAmount ->
                                                val selId = dragTargetId
                                                if (selId != null) {
                                                    change.consume()
                                                    val deltaX = dragAmount.x / size.width
                                                    val deltaY = dragAmount.y / size.height
                                                    val idx = currentDrawings.indexOfFirst { it.id == selId }
                                                    if (idx != -1) {
                                                        val target = currentDrawings[idx]
                                                        val movedPoints = target.points.map { PointDef(it.x + deltaX, it.y + deltaY) }
                                                        currentDrawings[idx] = target.copy(points = movedPoints)
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val rx = offset.x / size.width
                                                val ry = offset.y / size.height
                                                val shapeId = java.util.UUID.randomUUID().toString()
                                                val currentDrawnPoints = mutableListOf(PointDef(rx, ry))
                                                currentDrawings.add(
                                                    DrawingDef(
                                                        points = currentDrawnPoints,
                                                        colorHex = drawColorHex,
                                                        strokeWidth = drawStrokeWidth,
                                                        isHighlighter = (drawPenType == "highlighter"),
                                                        isDashed = (drawPenType == "dashed"),
                                                        id = shapeId,
                                                        shapeType = drawShapeType
                                                    )
                                                )
                                                selectedDrawingId = shapeId
                                            },
                                            onDragEnd = {
                                                viewModel.editActivePageDrawings(currentDrawings.toList())
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val offset = change.position
                                                val rx = offset.x / size.width
                                                val ry = offset.y / size.height
                                                
                                                if (currentDrawings.isNotEmpty()) {
                                                    val lastDraw = currentDrawings.last()
                                                    val updatedPoints = if (drawShapeType == "brush" || drawShapeType == "freehand") {
                                                        lastDraw.points + PointDef(rx, ry)
                                                     } else {
                                                        listOf(lastDraw.points.first(), PointDef(rx, ry))
                                                     }
                                                    currentDrawings[currentDrawings.size - 1] = lastDraw.copy(points = updatedPoints)
                                                }
                                            }
                                        )
                                    }
                                } else if (isAddingTextMode) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        },
                                        onTap = {
                                            selectedAnnotationId = null
                                        }
                                    )
                                } else {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        },
                                        onTap = {
                                            selectedAnnotationId = null
                                        }
                                    )
                                }
                            }
                    ) {
                    // Draw base templates
                    when (activePage.type.lowercase()) {
                        "lined" -> {
                            val leftMargin = size.width * 0.15f
                            drawLine(Color(0xFFE09090), Offset(leftMargin, 0f), Offset(leftMargin, size.height), 2f)
                            var lineY = size.height * 0.08f
                            while (lineY < size.height * 0.95f) {
                                drawLine(Color(0xFFC5D3E8), Offset(0f, lineY), Offset(size.width, lineY), 1.5f)
                                lineY += 36f
                            }
                        }
                        "cornell" -> {
                            drawLine(Color(0xFF80B3D6), Offset(0f, size.height * 0.08f), Offset(size.width, size.height * 0.08f), 3f)
                            drawLine(Color(0xFF80B3D6), Offset(size.width * 0.3f, size.height * 0.08f), Offset(size.width * 0.3f, size.height * 0.85f), 3f)
                            drawLine(Color(0xFF80B3D6), Offset(0f, size.height * 0.85f), Offset(size.width, size.height * 0.85f), 3f)
                        }
                    }

                    // Render vector drawings path
                    for (draw in currentDrawings) {
                        if (draw.points.size < 2) continue
                        val baseColor = try { Color(AndroidColor.parseColor(draw.colorHex)) } catch (e: Exception) { Color.Black }
                        val drawColor = if (draw.isHighlighter) baseColor.copy(alpha = 0.45f) else baseColor
                        val scaledStrokeWidth = draw.strokeWidth * (size.width / 400f)
                        val pathStyle = Stroke(
                            width = scaledStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (draw.isDashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f * (size.width / 400f), 15f * (size.width / 400f)), 0f) else null
                        )

                        val isThisSelected = (draw.id.isNotEmpty() && draw.id == selectedDrawingId)

                        when (draw.shapeType.lowercase()) {
                            "line" -> {
                                val start = draw.points.first()
                                val end = draw.points.last()
                                drawLine(
                                    color = drawColor,
                                    start = Offset(start.x * size.width, start.y * size.height),
                                    end = Offset(end.x * size.width, end.y * size.height),
                                    strokeWidth = scaledStrokeWidth,
                                    cap = StrokeCap.Round,
                                    pathEffect = if (draw.isDashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f * (size.width / 400f), 15f * (size.width / 400f)), 0f) else null
                                )
                            }
                            "box" -> {
                                val start = draw.points.first()
                                val end = draw.points.last()
                                val left = minOf(start.x, end.x) * size.width
                                val top = minOf(start.y, end.y) * size.height
                                val right = maxOf(start.x, end.x) * size.width
                                val bottom = maxOf(start.y, end.y) * size.height
                                drawRect(
                                    color = drawColor,
                                    topLeft = Offset(left, top),
                                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                    style = pathStyle
                                )
                            }
                            "filled_box" -> {
                                val start = draw.points.first()
                                val end = draw.points.last()
                                val left = minOf(start.x, end.x) * size.width
                                val top = minOf(start.y, end.y) * size.height
                                val right = maxOf(start.x, end.x) * size.width
                                val bottom = maxOf(start.y, end.y) * size.height
                                drawRect(
                                    color = drawColor,
                                    topLeft = Offset(left, top),
                                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                    style = androidx.compose.ui.graphics.drawscope.Fill
                                )
                            }
                            "redact" -> {
                                val start = draw.points.first()
                                val end = draw.points.last()
                                val left = minOf(start.x, end.x) * size.width
                                val top = minOf(start.y, end.y) * size.height
                                val right = maxOf(start.x, end.x) * size.width
                                val bottom = maxOf(start.y, end.y) * size.height
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(left, top),
                                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                    style = androidx.compose.ui.graphics.drawscope.Fill
                                )
                                drawRect(
                                    color = Color(0xFFEF4444),
                                    topLeft = Offset(left, top),
                                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                    style = Stroke(
                                        width = 1.5f * (size.width / 400f),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                )
                            }
                            "circle" -> {
                                val start = draw.points.first()
                                val end = draw.points.last()
                                val left = minOf(start.x, end.x) * size.width
                                val top = minOf(start.y, end.y) * size.height
                                val right = maxOf(start.x, end.x) * size.width
                                val bottom = maxOf(start.y, end.y) * size.height
                                drawOval(
                                    color = drawColor,
                                    topLeft = Offset(left, top),
                                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                    style = pathStyle
                                )
                            }
                            else -> {
                                // freehand / brush
                                val path = Path()
                                path.moveTo(draw.points.first().x * size.width, draw.points.first().y * size.height)
                                for (pt in draw.points.drop(1)) {
                                    path.lineTo(pt.x * size.width, pt.y * size.height)
                                }
                                drawPath(
                                    path = path,
                                    color = drawColor,
                                    style = pathStyle
                                )
                            }
                        }

                        // Drawing selected visualization bounding box
                        if (isThisSelected) {
                            val p1 = draw.points.first()
                            val p2 = draw.points.last()
                            val (minX, maxX) = if (draw.shapeType.lowercase() == "freehand" || draw.shapeType.lowercase() == "brush") {
                                val xs = draw.points.map { it.x }
                                Pair(xs.minOrNull() ?: p1.x, xs.maxOrNull() ?: p1.x)
                            } else {
                                Pair(minOf(p1.x, p2.x), maxOf(p1.x, p2.x))
                            }
                            val (minY, maxY) = if (draw.shapeType.lowercase() == "freehand" || draw.shapeType.lowercase() == "brush") {
                                val ys = draw.points.map { it.y }
                                Pair(ys.minOrNull() ?: p1.y, ys.maxOrNull() ?: p1.y)
                            } else {
                                Pair(minOf(p1.y, p2.y), maxOf(p1.y, p2.y))
                            }

                            val margin = 8f
                            val l = minX * size.width - margin
                            val t = minY * size.height - margin
                            val w = (maxX - minX) * size.width + margin * 2
                            val h = (maxY - minY) * size.height + margin * 2
                            drawRect(
                                color = Color(0xFF2563EB),
                                topLeft = Offset(l, t),
                                size = androidx.compose.ui.geometry.Size(w, h),
                                style = Stroke(
                                    width = 2f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }
                    }
                }

                if (activePage.type.lowercase() == "word") {
                    val wordAnn = activePage.textAnnotations.firstOrNull { it.id == "word_main_content" }
                    
                    var wordTextValue by remember(activePage.id) {
                        mutableStateOf(wordAnn?.text ?: "")
                    }
                    var wordFontSize by remember(activePage.id) { mutableStateOf(wordAnn?.fontSize ?: 12f) }
                    var wordColorHex by remember(activePage.id) { mutableStateOf(wordAnn?.colorHex ?: "#1E293B") }
                    var wordIsBold by remember(activePage.id) { mutableStateOf(wordAnn?.isBold ?: false) }
                    var wordIsItalic by remember(activePage.id) { mutableStateOf(wordAnn?.isItalic ?: false) }
                    var wordAlignment by remember(activePage.id) { mutableStateOf(wordAnn?.alignment ?: "left") }
                    var wordFontName by remember(activePage.id) { mutableStateOf(wordAnn?.fontName ?: "sans-serif") }
                    var wordHasUnderline by remember(activePage.id) { mutableStateOf(wordAnn?.hasUnderline ?: false) }
                    var wordHasStrikeThrough by remember(activePage.id) { mutableStateOf(wordAnn?.hasStrikeThrough ?: false) }

                    val updateWordAnn = { newText: String, size: Float, color: String, bold: Boolean, italic: Boolean, align: String, font: String, underline: Boolean, strike: Boolean ->
                        val updatedList = currentTextAnns.toMutableList()
                        val idx = updatedList.indexOfFirst { it.id == "word_main_content" }
                        val updatedAnn = com.example.data.TextAnnotationDef(
                            id = "word_main_content",
                            text = newText,
                            x = 0.08f,
                            y = 0.08f,
                            fontSize = size,
                            colorHex = color,
                            fontName = font,
                            isBold = bold,
                            isItalic = italic,
                            alignment = align,
                            hasUnderline = underline,
                            hasStrikeThrough = strike
                        )
                        if (idx != -1) {
                            updatedList[idx] = updatedAnn
                        } else {
                            updatedList.add(updatedAnn)
                        }
                        
                        val idxLocal = currentTextAnns.indexOfFirst { it.id == "word_main_content" }
                        if (idxLocal != -1) {
                            currentTextAnns[idxLocal] = updatedAnn
                        } else {
                            currentTextAnns.add(updatedAnn)
                        }
                        viewModel.editActivePageAnnotations(updatedList)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        // 2. TEXT WORKSPACE WINDOW
                        val pFontFamily = when (wordFontName.lowercase()) {
                            "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                            "monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                            else -> androidx.compose.ui.text.font.FontFamily.Default
                        }
                        val pTextDecoration = when {
                            wordHasUnderline && wordHasStrikeThrough -> androidx.compose.ui.text.style.TextDecoration.Underline + androidx.compose.ui.text.style.TextDecoration.LineThrough
                            wordHasUnderline -> androidx.compose.ui.text.style.TextDecoration.Underline
                            wordHasStrikeThrough -> androidx.compose.ui.text.style.TextDecoration.LineThrough
                            else -> androidx.compose.ui.text.style.TextDecoration.None
                        }
                        val pFontWeight = if (wordIsBold) FontWeight.Bold else FontWeight.Normal
                        val pFontStyle = if (wordIsItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                        val pTextAlign = when (wordAlignment.lowercase()) {
                            "center" -> androidx.compose.ui.text.style.TextAlign.Center
                            "right" -> androidx.compose.ui.text.style.TextAlign.Right
                            else -> androidx.compose.ui.text.style.TextAlign.Left
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                                .padding(horizontal = 24.dp, vertical = 18.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = wordTextValue,
                                onValueChange = { newText ->
                                    wordTextValue = newText
                                    updateWordAnn(newText, wordFontSize, wordColorHex, wordIsBold, wordIsItalic, wordAlignment, wordFontName, wordHasUnderline, wordHasStrikeThrough)
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = wordFontSize.sp,
                                    color = Color(android.graphics.Color.parseColor(wordColorHex)),
                                    fontFamily = pFontFamily,
                                    fontWeight = pFontWeight,
                                    fontStyle = pFontStyle,
                                    textDecoration = pTextDecoration,
                                    textAlign = pTextAlign,
                                    lineHeight = (wordFontSize * 1.5f).sp
                                ),
                                modifier = Modifier.fillMaxSize(),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (wordTextValue.isEmpty()) {
                                            Text(
                                                text = "Type here...",
                                                color = Color.LightGray,
                                                fontSize = wordFontSize.sp,
                                                fontFamily = pFontFamily,
                                                textAlign = pTextAlign,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                val textAnnsToRender = if (activePage.type.lowercase() == "word") {
                    currentTextAnns.filter { it.id != "word_main_content" }
                } else {
                    currentTextAnns
                }

                // Custom FrameLayout container managing Dynamic Resizable & Draggable Text Annotation Views over the PDF renderer
                AndroidView(
                    factory = { ctx ->
                        android.widget.FrameLayout(ctx).apply {
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { frameLayout ->
                        val currentIds = textAnnsToRender.map { it.id }.toSet()
                        val toRemove = mutableListOf<android.view.View>()
                        for (i in 0 until frameLayout.childCount) {
                            val child = frameLayout.getChildAt(i)
                            val tagId = child.tag as? String
                            if (tagId == null || !currentIds.contains(tagId)) toRemove.add(child)
                        }
                        toRemove.forEach { frameLayout.removeView(it) }

                        textAnnsToRender.forEach { txtAnn ->
                            val existingView = frameLayout.findViewWithTag<AdvancedTextAnnotationView>(txtAnn.id)
                            val isSelected = selectedAnnotationId == txtAnn.id

                            val view = existingView ?: AdvancedTextAnnotationView(frameLayout.context).apply {
                                tag = txtAnn.id
                                val lp = android.widget.FrameLayout.LayoutParams(
                                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                                )
                                frameLayout.addView(this, lp)
                            }

                            view.zoomScale = scale
                            if (!view.isEditing) {
                                view.bind(txtAnn, pageMeasuredWidthPx, pageMeasuredHeightPx)
                            } else {
                                view.refWidthPx = pageMeasuredWidthPx
                                view.refHeightPx = pageMeasuredHeightPx
                            }
                            view.isSelectedState = isSelected

                            view.onSelected = { selectedAnnotationId = txtAnn.id }

                            view.onDeleteRequested = {
                                val idxLocal = currentTextAnns.indexOfFirst { it.id == txtAnn.id }
                                if (idxLocal != -1) currentTextAnns.removeAt(idxLocal)
                                viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                selectedAnnotationId = null
                            }

                            view.onDefChanged = { updatedDef ->
                                val updatedList = currentTextAnns.map { if (it.id == txtAnn.id) updatedDef else it }
                                viewModel.editActivePageAnnotations(updatedList)
                            }
                        }
                    }
                )
                                   // Render Placeable Signature Overlays
                AndroidView(
                    factory = { ctx ->
                        android.widget.FrameLayout(ctx).apply {
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { frameLayout ->
                        // Synchronize child views
                        val currentIds = currentSignatures.map { it.id }.toSet()
                        val toRemove = mutableListOf<android.view.View>()
                        for (i in 0 until frameLayout.childCount) {
                            val child = frameLayout.getChildAt(i)
                            val tagId = child.tag as? String
                            if (tagId == null || !currentIds.contains(tagId)) {
                                toRemove.add(child)
                            }
                        }
                        toRemove.forEach { frameLayout.removeView(it) }

                        currentSignatures.forEach { sigOverlay ->
                            val resolvedSigProfile = state.signatures.find { it.id == sigOverlay.signatureProfileId }
                            val isSelected = selectedAnnotationId == sigOverlay.id

                            val scaleFactor = (pageMeasuredWidthPx / 400f).coerceAtLeast(0.1f)
                            val viewWidthPx = (sigOverlay.width * scaleFactor).toInt().coerceAtLeast(30)
                            val viewHeightPx = (sigOverlay.height * scaleFactor).toInt().coerceAtLeast(15)

                            val existingView = frameLayout.findViewWithTag<ResizableDraggableSignatureView>(sigOverlay.id)
                            val view = existingView ?: ResizableDraggableSignatureView(frameLayout.context).apply {
                                tag = sigOverlay.id
                                val lp = android.widget.FrameLayout.LayoutParams(
                                    viewWidthPx,
                                    viewHeightPx
                                )
                                frameLayout.addView(this, lp)
                            }

                            // Apply layouts safely
                            if (view.layoutParams.width != viewWidthPx || view.layoutParams.height != viewHeightPx) {
                                view.layoutParams.width = viewWidthPx
                                view.layoutParams.height = viewHeightPx
                                view.requestLayout()
                            }

                            view.isSelectedState = isSelected
                            view.setSignatureProfile(resolvedSigProfile, sigOverlay.colorHex)

                            // Set coordinates scaled to current view dimension
                            val pdfX = sigOverlay.x * pageMeasuredWidthPx
                            val pdfY = sigOverlay.y * pageMeasuredHeightPx
                            view.setAnnotationPosition(pdfX, pdfY)

                            // Setup view event callbacks
                            view.onSelected = {
                                selectedAnnotationId = sigOverlay.id
                            }

                            view.onDeleteRequested = {
                                viewModel.removeSignatureOverlay(sigOverlay.id)
                                selectedAnnotationId = null
                            }

                            view.onSignatureChanged = { tx, ty, newW, newH ->
                                val newX = (tx / pageMeasuredWidthPx).coerceIn(0f, 0.99f)
                                val newY = (ty / pageMeasuredHeightPx).coerceIn(0f, 0.99f)

                                val unscaledW = newW / scaleFactor
                                val unscaledH = newH / scaleFactor

                                val idxToUpdate = currentSignatures.indexOfFirst { it.id == sigOverlay.id }
                                if (idxToUpdate != -1) {
                                    currentSignatures[idxToUpdate] = sigOverlay.copy(x = newX, y = newY, width = unscaledW, height = unscaledH)
                                }

                                val updatedSignatures = currentSignatures.map {
                                    if (it.id == sigOverlay.id) it.copy(x = newX, y = newY, width = unscaledW, height = unscaledH) else it
                                }
                                viewModel.editActivePageSignatures(updatedSignatures)
                            }
                        }
                    }
                )
            }
        }
    } // Close the 'else' block for isContinuousScrollMode
    } // Close BoxWithConstraints (line 5001)

    contextualOptionsPanel()
    pageWorkbenchPanel()
    } // Close Column (scaffold content)

    // Signature Overlay Choice Drawer
        if (showSignatureSelectionDrawer) {
            AlertDialog(
                onDismissRequest = { showSignatureSelectionDrawer = false },
                title = { Text("Deploy Signature Layer", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Choose one of your drew signature profiles to place onto your active page document canvas:", fontSize = 13.sp)
                        if (state.signatures.isEmpty()) {
                            Text("No Signatures Saved. Please visit the Signature Studio from details panel first.", color = Color.Red, fontSize = 12.sp)
                        } else {
                            LazyColumn(modifier = Modifier.height(180.dp)) {
                                items(state.signatures) { sig ->
                                    Card(
                                        onClick = {
                                            // Spawn signature overlay precisely at the current visible center of the zoomed viewport
                                            val spawnX = (0.5f - (offsetX / (scale * pageMeasuredWidthPx.coerceAtLeast(1f)))).coerceIn(0.05f, 0.85f)
                                            val spawnY = (0.5f - (offsetY / (scale * pageMeasuredHeightPx.coerceAtLeast(1f)))).coerceIn(0.05f, 0.85f)
                                            
                                            var finalWidth = 110f
                                            var finalHeight = 55f
                                            if (sig.pathDataJson.startsWith("image:")) {
                                                try {
                                                    val path = sig.pathDataJson.removePrefix("image:")
                                                    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                    android.graphics.BitmapFactory.decodeFile(path, opts)
                                                    if (opts.outWidth > 0 && opts.outHeight > 0) {
                                                        finalWidth = 110f
                                                        finalHeight = finalWidth / (opts.outWidth.toFloat() / opts.outHeight.toFloat())
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }

                                            val overlay = SignatureOverlayDef(
                                                id = UUID.randomUUID().toString(),
                                                x = spawnX,
                                                y = spawnY,
                                                width = finalWidth,
                                                height = finalHeight,
                                                signatureProfileId = sig.id
                                            )
                                            viewModel.addSignatureOverlay(overlay)
                                            annotationMode = "none"
                                            showSignatureSelectionDrawer = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(sig.alias, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSignatureSelectionDrawer = false }) {
                        Text("Back")
                    }
                }
            )
        }

        if (showAddWordDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showAddWordDialog = false
                    isAddingTextMode = false
                },
                title = { Text("Type Word / Text Annotation", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = addWordTextDraft,
                            onValueChange = { addWordTextDraft = it },
                            label = { Text("Enter Word/Sentence") },
                            placeholder = { Text("Type here...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Live Interactive Design Preview Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Preview Text Styling",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (addWordBgColorHex.lowercase() == "transparent" || addWordBgColorHex.isBlank()) {
                                                Color.Transparent
                                            } else {
                                                Color(android.graphics.Color.parseColor(addWordBgColorHex))
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = if (addWordBgColorHex.lowercase() == "transparent") 1.dp else 0.dp,
                                            color = if (addWordBgColorHex.lowercase() == "transparent") Color.LightGray else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (addWordTextDraft.isNotBlank()) addWordTextDraft else "Sample Text Layer Preview",
                                        fontSize = addWordFontSize.sp,
                                        fontFamily = getFontFamily(addWordFontFamily),
                                        fontWeight = if (addWordIsBold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (addWordIsItalic) FontStyle.Italic else FontStyle.Normal,
                                        color = Color(android.graphics.Color.parseColor(addWordColorHex)),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = when (addWordAlignment.lowercase()) {
                                            "center" -> TextAlign.Center
                                            "right" -> TextAlign.End
                                            else -> TextAlign.Start
                                        }
                                    )
                                }
                            }
                        }

                        Text("Select Font Family:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Pair("arial", "Arial"),
                                Pair("calibri", "Calibri"),
                                Pair("tahoma", "Tahoma"),
                                Pair("times new roman", "Times NR")
                            ).forEach { (fontKey, fontVal) ->
                                val isSelected = addWordFontFamily.lowercase() == fontKey.lowercase()
                                OutlinedButton(
                                    onClick = { addWordFontFamily = fontKey },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                                    )
                                ) {
                                    Text(
                                        text = fontVal,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = getFontFamily(fontKey),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Size: ${addWordFontSize.toInt()}sp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (addWordFontSize > 8f) addWordFontSize -= 2f },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, "Decrease Size")
                                }
                                Text("${addWordFontSize.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(
                                    onClick = { if (addWordFontSize < 72f) addWordFontSize += 2f },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, "Increase Size")
                                }
                            }
                        }

                        Text("Text Color:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "#000000" to Color.Black,
                                "#D62246" to Color(0xFFD62246),
                                "#005FB8" to Color(0xFF005FB8),
                                "#10AC84" to Color(0xFF10AC84),
                                "#F5A623" to Color(0xFFF5A623)
                            ).forEach { (colorKey, colorVal) ->
                                val isSelected = addWordColorHex.lowercase() == colorKey.lowercase()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(colorVal, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { addWordColorHex = colorKey }
                                )
                            }
                        }

                        Text("Background Fill Color:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "transparent" to Color.Transparent,
                                "#FFF275" to Color(0xFFFFF275), // Yellow Highlight
                                "#FFFFFF" to Color.White,       // Solid White
                                "#D2E9FF" to Color(0xFFD2E9FF), // Soft Blue
                                "#D4F7D3" to Color(0xFFD4F7D3), // Soft Green
                                "#FFD2D2" to Color(0xFFFFD2D2)  // Soft Pink
                            ).forEach { (colorKey, colorVal) ->
                                val isSelected = addWordBgColorHex.lowercase() == colorKey.lowercase()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(colorVal, RoundedCornerShape(4.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { addWordBgColorHex = colorKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (colorKey == "transparent") {
                                        Icon(
                                            Icons.Default.Block,
                                            contentDescription = "No Background",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Style & Formatting:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bold Option
                            IconButton(
                                onClick = { addWordIsBold = !addWordIsBold },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (addWordIsBold) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (addWordIsBold) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatBold,
                                    contentDescription = "Bold Toggle",
                                    tint = if (addWordIsBold) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Italic Option
                            IconButton(
                                onClick = { addWordIsItalic = !addWordIsItalic },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (addWordIsItalic) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (addWordIsItalic) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatItalic,
                                    contentDescription = "Italic Toggle",
                                    tint = if (addWordIsItalic) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Single Underline Option
                            IconButton(
                                onClick = { addWordHasUnderline = !addWordHasUnderline },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (addWordHasUnderline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (addWordHasUnderline) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatUnderlined,
                                    contentDescription = "Underline Toggle",
                                    tint = if (addWordHasUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Cross Text / Strikethrough Option
                            IconButton(
                                onClick = { addWordHasStrikeThrough = !addWordHasStrikeThrough },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (addWordHasStrikeThrough) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (addWordHasStrikeThrough) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatStrikethrough,
                                    contentDescription = "Strikethrough Toggle",
                                    tint = if (addWordHasStrikeThrough) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }


                        }

                        // Outline Border Toggler
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Border Outline Around Box:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(
                                onClick = { addWordHasOutline = !addWordHasOutline },
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(36.dp)
                                    .background(
                                        if (addWordHasOutline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (addWordHasOutline) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BorderOuter,
                                    contentDescription = "Border Outer",
                                    tint = if (addWordHasOutline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (addWordHasOutline) {
                            Text("Border Outline Color:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "#000000" to Color.Black,
                                    "#D62246" to Color(0xFFD62246),
                                    "#005FB8" to Color(0xFF005FB8),
                                    "#10AC84" to Color(0xFF10AC84),
                                    "#F5A623" to Color(0xFFF5A623)
                                ).forEach { (colorKey, colorVal) ->
                                    val isSelected = addWordOutlineColorHex.lowercase() == colorKey.lowercase()
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(colorVal, CircleShape)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { addWordOutlineColorHex = colorKey }
                                    )
                                }
                            }
                        }

                        Text("Text Alignment:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "left" to Icons.Default.FormatAlignLeft,
                                "center" to Icons.Default.FormatAlignCenter,
                                "right" to Icons.Default.FormatAlignRight
                            ).forEach { (alignKey, alignIcon) ->
                                val isSelected = addWordAlignment.lowercase() == alignKey.lowercase()
                                IconButton(
                                    onClick = { addWordAlignment = alignKey },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .background(
                                            if (isSelected) Color.Black else Color.White,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color.Black,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = alignIcon,
                                        contentDescription = "$alignKey alignment",
                                        tint = if (isSelected) Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val sticker = TextAnnotationDef(
                                id = UUID.randomUUID().toString(),
                                text = addWordTextDraft.ifBlank { "Text Markup" },
                                x = addWordX,
                                y = addWordY,
                                fontSize = addWordFontSize,
                                colorHex = addWordColorHex,
                                fontName = addWordFontFamily,
                                bgColorHex = addWordBgColorHex,
                                hasOutline = addWordHasOutline,
                                hasUnderline = addWordHasUnderline,
                                outlineColorHex = addWordOutlineColorHex,
                                hasDoubleUnderline = false,
                                isBold = addWordIsBold,
                                alignment = addWordAlignment,
                                isPowerOf = addWordIsPowerOf,
                                isItalic = addWordIsItalic,
                                hasStrikeThrough = addWordHasStrikeThrough
                            )
                            currentTextAnns.add(sticker)
                            viewModel.editActivePageAnnotations(currentTextAnns.toList())
                            showAddWordDialog = false
                            isAddingTextMode = false
                        }
                    ) {
                        Text("Place Word", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showAddWordDialog = false
                            isAddingTextMode = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPreciseTextPlacementDialog) {
            AlertDialog(
                onDismissRequest = { showPreciseTextPlacementDialog = false },
                title = { Text("Precise Text Annotation Placement", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Force this text into an independent, standalone text object using an absolute overlay layer on any page.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        OutlinedTextField(
                            value = preciseTextContent,
                            onValueChange = { preciseTextContent = it },
                            label = { Text("Text Content") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = precisePageNumberText,
                                onValueChange = { precisePageNumberText = it },
                                label = { Text("Page Number (1-${pages.size})") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = preciseXText,
                                onValueChange = { preciseXText = it },
                                label = { Text("X (0.00 - 1.00)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = preciseYText,
                                onValueChange = { preciseYText = it },
                                label = { Text("Y (0.00 - 1.00)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pageNum = precisePageNumberText.toIntOrNull() ?: (pages.indexOf(activePage) + 1)
                            val coercedPageNum = pageNum.coerceIn(1, pages.size)
                            val targetPage = pages[coercedPageNum - 1]
                            val parsedX = preciseXText.toFloatOrNull() ?: 0.50f
                            val parsedY = preciseYText.toFloatOrNull() ?: 0.50f

                            val sticker = TextAnnotationDef(
                                id = UUID.randomUUID().toString(),
                                text = preciseTextContent.ifBlank { "Standalone Text" },
                                x = parsedX.coerceIn(0f, 1f),
                                y = parsedY.coerceIn(0f, 1f),
                                fontSize = addWordFontSize,
                                colorHex = addWordColorHex,
                                fontName = addWordFontFamily,
                                bgColorHex = addWordBgColorHex,
                                hasOutline = addWordHasOutline,
                                hasUnderline = addWordHasUnderline,
                                outlineColorHex = addWordOutlineColorHex,
                                hasDoubleUnderline = false,
                                isBold = addWordIsBold,
                                alignment = addWordAlignment,
                                isPowerOf = addWordIsPowerOf,
                                isItalic = addWordIsItalic,
                                hasStrikeThrough = addWordHasStrikeThrough
                            )

                            if (targetPage.id == activePage.id) {
                                currentTextAnns.add(sticker)
                                viewModel.editActivePageAnnotations(currentTextAnns.toList())
                            } else {
                                val updatedAnns = targetPage.textAnnotations + sticker
                                viewModel.editPageAnnotations(targetPage.id, updatedAnns)
                            }

                            showPreciseTextPlacementDialog = false
                        }
                    ) {
                        Text("Place Standalone Text", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPreciseTextPlacementDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEditWordDialog && selectedWordToEdit != null) {
            AlertDialog(
                onDismissRequest = { showEditWordDialog = false },
                title = { Text("Edit Word / Text Annotation", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editWordTextDraft,
                            onValueChange = { editWordTextDraft = it },
                            label = { Text("Word Context") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Live Interactive Design Preview Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Preview Text Styling",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (editWordBgColorHex.lowercase() == "transparent" || editWordBgColorHex.isBlank()) {
                                                Color.Transparent
                                            } else {
                                                Color(android.graphics.Color.parseColor(editWordBgColorHex))
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = if (editWordBgColorHex.lowercase() == "transparent") 1.dp else 0.dp,
                                            color = if (editWordBgColorHex.lowercase() == "transparent") Color.LightGray else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (editWordTextDraft.isNotBlank()) editWordTextDraft else "Sample Text Layer Preview",
                                        fontSize = editWordFontSize.sp,
                                        fontFamily = getFontFamily(editWordFontFamily),
                                        fontWeight = if (editWordIsBold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (editWordIsItalic) FontStyle.Italic else FontStyle.Normal,
                                        color = Color(android.graphics.Color.parseColor(editWordColorHex)),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = when (editWordAlignment.lowercase()) {
                                            "center" -> TextAlign.Center
                                            "right" -> TextAlign.End
                                            else -> TextAlign.Start
                                        }
                                    )
                                }
                            }
                        }

                        Text("Select Font Family:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Pair("arial", "Arial"),
                                Pair("calibri", "Calibri"),
                                Pair("tahoma", "Tahoma"),
                                Pair("times new roman", "Times NR")
                            ).forEach { (fontKey, fontVal) ->
                                val isSelected = editWordFontFamily.lowercase() == fontKey.lowercase()
                                OutlinedButton(
                                    onClick = { editWordFontFamily = fontKey },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                                    )
                                ) {
                                    Text(
                                        text = fontVal,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = getFontFamily(fontKey),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Size: ${editWordFontSize.toInt()}sp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (editWordFontSize > 8f) editWordFontSize -= 2f },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, "Decrease Size")
                                }
                                Text("${editWordFontSize.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(
                                    onClick = { if (editWordFontSize < 72f) editWordFontSize += 2f },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, "Increase Size")
                                }
                            }
                        }

                        Text("Text Color:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "#000000" to Color.Black,
                                "#D62246" to Color(0xFFD62246),
                                "#005FB8" to Color(0xFF005FB8),
                                "#10AC84" to Color(0xFF10AC84),
                                "#F5A623" to Color(0xFFF5A623)
                            ).forEach { (colorKey, colorVal) ->
                                val isSelected = editWordColorHex.lowercase() == colorKey.lowercase()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(colorVal, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { editWordColorHex = colorKey }
                                )
                            }
                        }

                        Text("Background Fill Color:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "transparent" to Color.Transparent,
                                "#FFF275" to Color(0xFFFFF275), // Yellow Highlight
                                "#FFFFFF" to Color.White,       // Solid White
                                "#D2E9FF" to Color(0xFFD2E9FF), // Soft Blue
                                "#D4F7D3" to Color(0xFFD4F7D3), // Soft Green
                                "#FFD2D2" to Color(0xFFFFD2D2)  // Soft Pink
                            ).forEach { (colorKey, colorVal) ->
                                val isSelected = editWordBgColorHex.lowercase() == colorKey.lowercase()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(colorVal, RoundedCornerShape(4.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { editWordBgColorHex = colorKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (colorKey == "transparent") {
                                        Icon(
                                            Icons.Default.Block,
                                            contentDescription = "No Background",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Style & Formatting:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bold Option
                            IconButton(
                                onClick = { editWordIsBold = !editWordIsBold },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (editWordIsBold) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (editWordIsBold) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatBold,
                                    contentDescription = "Bold Toggle",
                                    tint = if (editWordIsBold) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Italic Option
                            IconButton(
                                onClick = { editWordIsItalic = !editWordIsItalic },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (editWordIsItalic) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (editWordIsItalic) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatItalic,
                                    contentDescription = "Italic Toggle",
                                    tint = if (editWordIsItalic) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Underline Option (Single)
                            IconButton(
                                onClick = { editWordHasUnderline = !editWordHasUnderline },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (editWordHasUnderline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (editWordHasUnderline) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatUnderlined,
                                    contentDescription = "Underline Toggle",
                                    tint = if (editWordHasUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Cross Text / Strikethrough Option
                            IconButton(
                                onClick = { editWordHasStrikeThrough = !editWordHasStrikeThrough },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (editWordHasStrikeThrough) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (editWordHasStrikeThrough) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatStrikethrough,
                                    contentDescription = "Strikethrough Toggle",
                                    tint = if (editWordHasStrikeThrough) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }


                        }

                        // Outline Border Toggler
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Border Outline Around Box:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(
                                onClick = { editWordHasOutline = !editWordHasOutline },
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(36.dp)
                                    .background(
                                        if (editWordHasOutline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (editWordHasOutline) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BorderOuter,
                                    contentDescription = "Border Outer",
                                    tint = if (editWordHasOutline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (editWordHasOutline) {
                            Text("Border Outline Color:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "#000000" to Color.Black,
                                    "#D62246" to Color(0xFFD62246),
                                    "#005FB8" to Color(0xFF005FB8),
                                    "#10AC84" to Color(0xFF10AC84),
                                    "#F5A623" to Color(0xFFF5A623)
                                ).forEach { (colorKey, colorVal) ->
                                    val isSelected = editWordOutlineColorHex.lowercase() == colorKey.lowercase()
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(colorVal, CircleShape)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { editWordOutlineColorHex = colorKey }
                                    )
                                }
                            }
                        }

                        Text("Text Alignment:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "left" to Icons.Default.FormatAlignLeft,
                                "center" to Icons.Default.FormatAlignCenter,
                                "right" to Icons.Default.FormatAlignRight
                            ).forEach { (alignKey, alignIcon) ->
                                val isSelected = editWordAlignment.lowercase() == alignKey.lowercase()
                                IconButton(
                                    onClick = { editWordAlignment = alignKey },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .background(
                                            if (isSelected) Color.Black else Color.White,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color.Black,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = alignIcon,
                                        contentDescription = "$alignKey alignment",
                                        tint = if (isSelected) Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = selectedWordToEdit ?: return@TextButton
                            val updatedItem = target.copy(
                                text = editWordTextDraft,
                                fontSize = editWordFontSize,
                                colorHex = editWordColorHex,
                                fontName = editWordFontFamily,
                                bgColorHex = editWordBgColorHex,
                                hasOutline = editWordHasOutline,
                                hasUnderline = editWordHasUnderline,
                                outlineColorHex = editWordOutlineColorHex,
                                hasDoubleUnderline = false,
                                isBold = editWordIsBold,
                                alignment = editWordAlignment,
                                isPowerOf = editWordIsPowerOf,
                                isItalic = editWordIsItalic,
                                hasStrikeThrough = editWordHasStrikeThrough
                            )
                            val idx = currentTextAnns.indexOfFirst { it.id == target.id }
                            if (idx != -1) {
                                currentTextAnns[idx] = updatedItem
                            }
                            viewModel.editActivePageAnnotations(currentTextAnns.toList())
                            showEditWordDialog = false
                            selectedWordToEdit = null
                        }
                    ) {
                        Text("Update Word", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                val target = selectedWordToEdit ?: return@TextButton
                                val idx = currentTextAnns.indexOfFirst { it.id == target.id }
                                if (idx != -1) {
                                    currentTextAnns.removeAt(idx)
                                }
                                viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                showEditWordDialog = false
                                selectedWordToEdit = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Delete")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showEditWordDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        androidx.activity.compose.BackHandler {
            if (isModified) {
                showCloseConfirmDialog = true
            } else {
                viewModel.navigateTo(Screen.Dashboard)
            }
        }

        if (showDeletePageConfirm) {
            AlertDialog(
                onDismissRequest = { showDeletePageConfirm = false },
                title = { Text("Delete This Page?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete Page ${pages.indexOf(activePage) + 1} from this document?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteActivePage()
                            showDeletePageConfirm = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Delete Page", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePageConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCloseConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showCloseConfirmDialog = false },
                title = { Text("Save Document Changes?", fontWeight = FontWeight.Bold) },
                text = { Text("Do you want to save any modifications made to this document before closing, or close directly without saving?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.saveEditorChanges()
                            viewModel.navigateTo(Screen.Dashboard)
                            showCloseConfirmDialog = false
                        }
                    ) {
                        Text("Save and Exit", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                viewModel.navigateTo(Screen.Dashboard)
                                showCloseConfirmDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Discard", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showCloseConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        if (isPresentationMode) {
            Dialog(
                onDismissRequest = { isPresentationMode = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Title row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PRESENTATION PREVIEW",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                                
                                IconButton(
                                    onClick = { isPresentationMode = false },
                                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, "Exit Preview", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            // Immersive single page preview
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                StaticPagePreview(
                                    page = activePage,
                                    state = state,
                                    viewModel = viewModel,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(0.707f)
                                )
                            }
                            
                            // Immersive status and navigation controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val activeIdx = pages.indexOf(activePage)
                                        if (activeIdx > 0) {
                                            viewModel.setActivePageId(pages[activeIdx - 1].id)
                                        }
                                    },
                                    enabled = pages.indexOf(activePage) > 0,
                                    modifier = Modifier.background(Color.White.copy(alpha = if (pages.indexOf(activePage) > 0) 0.15f else 0.05f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, "Previous", tint = if (pages.indexOf(activePage) > 0) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                                }
                                
                                Surface(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Text(
                                        text = "PAGE ${pages.indexOf(activePage) + 1} OF ${pages.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        val activeIdx = pages.indexOf(activePage)
                                        if (activeIdx < pages.size - 1) {
                                            viewModel.setActivePageId(pages[activeIdx + 1].id)
                                        }
                                    },
                                    enabled = pages.indexOf(activePage) < pages.size - 1,
                                    modifier = Modifier.background(Color.White.copy(alpha = if (pages.indexOf(activePage) < pages.size - 1) 0.15f else 0.05f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ChevronRight, "Next", tint = if (pages.indexOf(activePage) < pages.size - 1) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Coordinate collection syncing helper
fun MutableList<DrawingDef>.colorsSync(list: List<DrawingDef>) = apply {
    clear()
    addAll(list)
}

fun MutableList<TextAnnotationDef>.annotationSync(list: List<TextAnnotationDef>) = apply {
    clear()
    addAll(list)
}

fun MutableList<SignatureOverlayDef>.signatureSync(list: List<SignatureOverlayDef>) = apply {
    clear()
    addAll(list)
}

fun stickerDefaultText() = "Acrobatic PDF Note"

object AndroidBitmapLoader {
    fun load(path: String): Bitmap? = try {
        BitmapFactory.decodeFile(path)
    } catch (e: Exception) {
        null
    }
}

// —-- 6. HIGH PERFORMANCE OCR VIEWER SCREEN —--

val tesseractHtml = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <script src="https://cdn.jsdelivr.net/npm/tesseract.js@5.0.3/dist/tesseract.min.js"></script>
  <style>
    body {
      font-family: monospace;
      background-color: #0f172a;
      color: #38bdf8;
      padding: 12px;
      margin: 0;
      font-size: 11px;
      line-height: 1.5;
    }
    #status {
      font-weight: bold;
      color: #34d399;
      margin-bottom: 8px;
    }
    #log {
      color: #94a3b8;
    }
    .err {
      color: #f87171 !important;
    }
  </style>
</head>
<body>
  <div id="status">Initializing Tesseract.js Engine...</div>
  <div id="log">> Console initialized. Ready.</div>

  <script>
    function updateStatus(msg) {
      document.getElementById('status').innerText = msg;
    }
    
    function logMessage(msg, isErr = false) {
      const logDiv = document.getElementById('log');
      const cls = isErr ? " class='err'" : "";
      logDiv.innerHTML += "<br/><span" + cls + ">> " + msg + "</span>";
      window.scrollTo(0, document.body.scrollHeight);
    }

    function runTesseractOcr(base64Data) {
      updateStatus("Processing image bits...");
      logMessage("Starting Tesseract.js local OCR session");
      
      Tesseract.recognize(
        base64Data,
        'eng',
        {
          logger: m => {
            if (m.status === 'recognizing text') {
              const pct = Math.round(m.progress * 100);
              updateStatus("Recognizing Document text... " + pct + "%");
              if (window.AndroidBridge) {
                window.AndroidBridge.onProgress(m.progress);
              }
            } else {
              updateStatus(m.status);
              logMessage(m.status + (m.progress ? " " + Math.round(m.progress * 100) + "%" : ""));
            }
          }
        }
      ).then(({ data: { text } }) => {
        updateStatus("OCR Complete!");
        logMessage("Verbatim text extracted successfully.");
        if (window.AndroidBridge) {
          window.AndroidBridge.onTextRecognized(text);
        }
      }).catch(err => {
        updateStatus("OCR Failed!");
        logMessage("Error: " + err.toString(), true);
        if (window.AndroidBridge) {
          window.AndroidBridge.onError(err.toString());
        }
      });
    }
  </script>
</body>
</html>
""".trimIndent()

@Composable
fun TesseractWebViewLoader(
    base64Image: String,
    onProgress: (Float) -> Unit,
    onTextRecognized: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoadedByHtml = remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowContentAccess = true
                    allowFileAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (!isLoadedByHtml.value) {
                            isLoadedByHtml.value = true
                            view?.evaluateJavascript("javascript:runTesseractOcr('$base64Image')", null)
                        }
                    }
                }
                
                webChromeClient = WebChromeClient()
                
                val bridge = object {
                    @JavascriptInterface
                    fun onProgress(progress: Float) {
                        onProgress(progress)
                    }

                    @JavascriptInterface
                    fun onTextRecognized(text: String) {
                        onTextRecognized(text)
                    }

                    @JavascriptInterface
                    fun onError(error: String) {
                        onError(error)
                    }
                }
                
                addJavascriptInterface(bridge, "AndroidBridge")
                loadDataWithBaseURL("https://localhost", tesseractHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (isLoadedByHtml.value) {
                webView.evaluateJavascript("javascript:runTesseractOcr('$base64Image')", null)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Text Recognition", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back To Dashboard")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.runOcrOnActivePage()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Retry Scan")
                    }
                    IconButton(
                        onClick = {
                            val txt = state.ocrTextResult ?: ""
                            if (txt.isNotBlank()) {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("OCR Transcript", txt)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied verbatim text!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy Transcript")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Engine selection Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Gemini", "TesseractJS").forEach { engine ->
                    val isSelected = state.ocrEngine == engine
                    val btnBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val btnText = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(btnBg)
                            .clickable {
                                viewModel.updateOcrEngine(engine)
                                viewModel.runOcrOnActivePage()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (engine == "Gemini") Icons.Default.Star else Icons.Default.Code,
                                contentDescription = null,
                                tint = btnText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (engine == "Gemini") "Gemini LLM" else "Tesseract.js Engine",
                                color = btnText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Analytics, "Intelligence OCR Status", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Active Document: ${state.ocrDocTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (state.ocrEngine == "Gemini") "Engine: Gemini Core Cloud OCR" else "Engine: Offline Tesseract.js (WebAssembly)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.ocrLoading) {
                if (state.ocrEngine == "TesseractJS") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Tesseract.js OCR Engine",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Local high-compatibility JS WebAssembly execution",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val pct = (state.ocrProgress * 100).toInt()
                        Text(
                            "Extraction Progress: $pct%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.ocrProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("LIVE JS CONSOLE LOGS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        ) {
                            if (state.ocrBase64Image != null) {
                                TesseractWebViewLoader(
                                    base64Image = state.ocrBase64Image,
                                    onProgress = { viewModel.updateOcrProgress(it) },
                                    onTextRecognized = { viewModel.saveOcrResult(it) },
                                    onError = { viewModel.saveOcrResult("Tesseract OCR Error: $it") },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x0A000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Converting page raster channels...", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Reading camera pixel channels layout...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text("Deconstructing document lines with AI", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            } else {
                Text("RECOGNIZED TEXT RESULTS:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = state.ocrTextResult ?: "No transcription found for this sheet.",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 18.sp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val txt = state.ocrTextResult ?: ""
                        if (txt.isNotBlank()) {
                            val sharingIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "OCR Transcript from ${state.ocrDocTitle}")
                                putExtra(android.content.Intent.EXTRA_TEXT, txt)
                            }
                            context.startActivity(android.content.Intent.createChooser(sharingIntent, "Share Document Text"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, "Share text content")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Transcript Text")
                }
            }
        }
    }
}

// —-- 7. COMBINE MULTIPLE PDFs (MERGER UTILITY) SCREEN —--

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergerScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    var masterTitle by remember { mutableStateOf("") }
    val selectedDraftDocs = remember { mutableStateListOf<Document>().apply { addAll(state.mergerSelectedDocs) } }
    val context = LocalContext.current
    var showRecentsDialog by remember { mutableStateOf(false) }

    val allCombinedPages = remember(selectedDraftDocs.toList()) {
        val list = mutableListOf<Pair<String, PageDef>>()
        selectedDraftDocs.forEach { doc ->
            try {
                val content = DocumentSerializer.fromJson(doc.contentJson)
                content.pages.forEach { p ->
                    list.add(doc.title to p)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        list
    }

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.importAndAppendLocalFilesToMerger(
                context = context,
                uris = uris,
                currentDocs = selectedDraftDocs.toList(),
                onComplete = { updatedDocs ->
                    selectedDraftDocs.clear()
                    selectedDraftDocs.addAll(updatedDocs)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acrobat Combine PDF Workbench", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dashboard")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showRecentsDialog = true
                        }
                    ) {
                        Icon(Icons.Default.History, "Choose from Recents")
                    }
                    IconButton(
                        onClick = {
                            multiFilePickerLauncher.launch("*/*")
                        }
                    ) {
                        Icon(Icons.Default.Add, "Add selected files")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = masterTitle,
                onValueChange = { masterTitle = it },
                label = { Text("Combined Document New Filename") },
                placeholder = { Text("e.g. Master Combined Document") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic live horizontal preview!
            if (allCombinedPages.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COMBINED LAYOUT PREVIEW (${allCombinedPages.size} pages)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(allCombinedPages) { index, item ->
                                val docTitle = item.first
                                val page = item.second
                                Box(
                                    modifier = Modifier
                                        .size(width = 82.dp, height = 116.dp)
                                        .background(Color.White, RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
                                        .clip(RoundedCornerShape(6.dp))
                                ) {
                                    if (page.backgroundScanPath != null && File(page.backgroundScanPath).exists()) {
                                        val bitmap = remember(page.id, page.backgroundScanPath) { AndroidBitmapLoader.load(page.backgroundScanPath) }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Book,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = page.type.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    // Page Number Badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                            .size(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Document overlay text
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = docTitle,
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Beautiful Call-to-action cards
            if (selectedDraftDocs.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Combine Local Files Directly",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select multiple local PDFs or images from your device folders and combine them instantly into a single multi-page PDF document.",
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    multiFilePickerLauncher.launch("*/*")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload files")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Local Files")
                            }

                            Button(
                                onClick = {
                                    showRecentsDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.History, contentDescription = "Recent files")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose from Recents")
                            }
                        }
                    }
                }
            } else {
                Text("Re-order / Remove documents arrays:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedDraftDocs) { doc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    Text("Contains: ${doc.pageCount} pages", fontSize = 10.sp, color = Color.Gray)
                                }
                                // Move buttons up/down to set fusion order
                                Row {
                                    IconButton(
                                        onClick = {
                                            val idx = selectedDraftDocs.indexOf(doc)
                                            if (idx > 0) {
                                                selectedDraftDocs.removeAt(idx)
                                                selectedDraftDocs.add(idx - 1, doc)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, "Move Up", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val idx = selectedDraftDocs.indexOf(doc)
                                            if (idx < selectedDraftDocs.size - 1) {
                                                selectedDraftDocs.removeAt(idx)
                                                selectedDraftDocs.add(idx + 1, doc)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, "Move Down", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedDraftDocs.remove(doc)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, "Remove from merge", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // High priority horizontal actions card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.compileMergedDocuments(masterTitle, selectedDraftDocs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save to library")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }

                    Button(
                        onClick = {
                            viewModel.compileMergedDocuments(masterTitle, selectedDraftDocs, shareAfterCompile = true, context = context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Combined")
                    }
                }
            }
        }
    }

    if (showRecentsDialog) {
        RecentsSelectorDialog(
            documents = state.documents,
            onDismissRequest = { showRecentsDialog = false },
            onAddSelected = { added ->
                selectedDraftDocs.addAll(added)
                showRecentsDialog = false
            }
        )
    }
}

@Composable
fun RecentsSelectorDialog(
    documents: List<Document>,
    onDismissRequest: () -> Unit,
    onAddSelected: (List<Document>) -> Unit
) {
    val selectedDocs = remember { mutableStateListOf<Document>() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Select Documents from Recents",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose one or more documents saved in your recents library to combine:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                val savedDocs = remember(documents) { documents.filter { it.isSaved } }

                if (savedDocs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved documents in Recents.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedDocs) { doc ->
                            val isChecked = selectedDocs.contains(doc)
                            Surface(
                                onClick = {
                                    if (isChecked) {
                                        selectedDocs.remove(doc)
                                    } else {
                                        selectedDocs.add(doc)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChecked) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                } else {
                                    Color.Transparent
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isChecked) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = if (doc.category == "PDF" || doc.category == "Combined PDF") Color.Red else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${doc.pageCount} pages • ${doc.category}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedDocs.add(doc)
                                            } else {
                                                selectedDocs.remove(doc)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddSelected(selectedDocs.toList())
                },
                enabled = selectedDocs.isNotEmpty()
            ) {
                Text("Add Selected (${selectedDocs.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

// —-- MOCK ASSET CREATORS (CAMERA IMAGES FALLBACKS FOR TESTING STABILITY) —--

fun createMockScannedDocSheet(pageIndex: Int): Bitmap {
    val bmp = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    
    // Fill slightly textured scanning paper
    canvas.drawColor(AndroidColor.parseColor("#FAF8F5"))
    
    val borderPaint = AndroidPaint().apply {
        color = AndroidColor.parseColor("#E0E5EA")
        strokeWidth = 10f
        style = AndroidPaint.Style.STROKE
    }
    canvas.drawRect(AndroidRectF(5f, 5f, 395f, 595f), borderPaint)

    val paint = AndroidPaint().apply {
        color = AndroidColor.parseColor("#2F3542")
        textSize = 22f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }

    val bodyPaint = AndroidPaint().apply {
        color = AndroidColor.parseColor("#57606F")
        textSize = 12f
        isAntiAlias = true
    }

    canvas.drawText("ACROPDF SCANNER REPORT", 20f, 60f, paint)
    paint.textSize = 11f
    canvas.drawText("DATE: JUNE 16, 2026", 20f, 90f, bodyPaint)
    canvas.drawText("INDEX ID: ACRO-0192-X", 20f, 110f, bodyPaint)

    // Draw some mock lines represents document text content
    var rowY = 160f
    val phrases = listOf(
        "SUBJECT: Multi-Page Scan Compilation Project",
        "Document OCR accuracy target: 99.8% with Gemini.",
        "Signatures are rendered as true vector stroke paths.",
        "Identity card dimensions: auto-rectified front/back.",
        "Merger merges separate documents seamlessly.",
        "AcroPDF supports offline operations perfectly."
    )

    for (p in phrases) {
        bodyPaint.color = AndroidColor.parseColor("#2F3542")
        canvas.drawRect(20f, rowY - 14f, 25f, rowY, bodyPaint) // prefix bullet
        bodyPaint.color = AndroidColor.parseColor("#57606F")
        canvas.drawText(p, 36f, rowY, bodyPaint)
        rowY += 45f
    }

    // Footnote
    bodyPaint.textSize = 10f
    paint.color = AndroidColor.parseColor("#E52521")
    canvas.drawRect(AndroidRectF(20f, rowY + 30f, 380f, rowY + 35f), paint)
    canvas.drawText("SECURE SCANNED PAGE SHEET $pageIndex OF HIGH-FIDELITY COMPILATION", 20f, rowY + 60f, bodyPaint)

    return bmp
}

fun createMockIdentityCard(isFront: Boolean): Bitmap {
    val bmp = Bitmap.createBitmap(400, 250, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    
    // Plastic textured card color
    canvas.drawColor(AndroidColor.parseColor(if (isFront) "#F1F5F9" else "#1E293B"))

    val paint = AndroidPaint().apply {
        color = AndroidColor.parseColor(if (isFront) "#0284C7" else "#FFFFFF")
        strokeWidth = 2f
        style = AndroidPaint.Style.FILL
    }
    
    // Header stripe
    canvas.drawRect(AndroidRectF(0f, 0f, 400f, 45f), paint)

    paint.color = AndroidColor.WHITE
    paint.textSize = 14f
    paint.isAntiAlias = true
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    canvas.drawText(if (isFront) "NATIONAL IDENTITY CARD" else "CARD SECURITY MATRIX / TRACK", 15f, 28f, paint)

    val detailPaint = AndroidPaint().apply {
        color = AndroidColor.parseColor(if (isFront) "#334155" else "#94A3B8")
        textSize = 10f
        isAntiAlias = true
    }

    if (isFront) {
        // Mock Avatar photo
        paint.color = AndroidColor.parseColor("#CBD5E1")
        canvas.drawRect(AndroidRectF(15f, 60f, 105f, 170f), paint)
        paint.color = AndroidColor.parseColor("#475569")
        canvas.drawCircle(60f, 100f, 22f, paint) // Avatar head
        canvas.drawRect(AndroidRectF(30f, 130f, 90f, 168f), paint) // Avatar jacket
        
        detailPaint.textSize = 11f
        detailPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        canvas.drawText("SURNAME: ECHEAH", 125f, 75f, detailPaint)
        canvas.drawText("NAME: JOHN", 125f, 95f, detailPaint)
        
        detailPaint.typeface = android.graphics.Typeface.DEFAULT
        detailPaint.textSize = 9f
        canvas.drawText("SEX: M  •  DOB: 16 APR 1988", 125f, 118f, detailPaint)
        canvas.drawText("ID NO: ID-29707-778-9B", 125f, 140f, detailPaint)
        canvas.drawText("EXPIRES: 16 APR 2032", 125f, 162f, detailPaint)
        
        // Chip mockup
        paint.color = AndroidColor.parseColor("#E2E8F0")
        canvas.drawRect(AndroidRectF(20f, 185f, 55f, 215f), paint)
        paint.color = AndroidColor.parseColor("#D97706") // Gold Chip contact lines
        canvas.drawRoundRect(AndroidRectF(24f, 190f, 51f, 210f), 4f, 4f, paint)
    } else {
        // Back card details
        canvas.drawText("DOCUMENT AUTHORITY:", 20f, 75f, detailPaint)
        canvas.drawText("FEDERAL ADMINISTRATIVE DEPT OF SCANNING", 20f, 95f, detailPaint)
        
        // Magnetic stripe mockup
        paint.color = AndroidColor.parseColor("#0F172A")
        canvas.drawRect(AndroidRectF(0f, 115f, 400f, 150f), paint)
        
        // Barcode lines mockup
        var xOffset = 20f
        paint.color = AndroidColor.parseColor("#334155")
        while (xOffset < 380f) {
            val width = if (xOffset % 12f == 0f) 5f else 2f
            canvas.drawRect(AndroidRectF(xOffset, 170f, xOffset + width, 225f), paint)
            xOffset += (8f + (xOffset % 6f))
        }
    }

    return bmp
}

fun sharePdfFile(context: android.content.Context, document: com.example.data.Document) {
    try {
        val docsDir = java.io.File(context.filesDir, "documents")
        val file = java.io.File(docsDir, "${document.title.replace(" ", "_")}_${document.id}.pdf")
        if (!file.exists()) {
            android.widget.Toast.makeText(context, "PDF file does not exist yet. Please wait or Save it first!", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val fileUri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "HanPDF: ${document.title}")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Send PDF Via"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun distanceToSegment(tx: Float, ty: Float, sx: Float, sy: Float, ex: Float, ey: Float): Float {
    val l2 = (ex - sx) * (ex - sx) + (ey - sy) * (ey - sy)
    if (l2 == 0f) return (tx - sx) * (tx - sx) + (ty - sy) * (ty - sy)
    var t = ((tx - sx) * (ex - sx) + (ty - sy) * (ey - sy)) / l2
    t = maxOf(0f, minOf(1f, t))
    val px = sx + t * (ex - sx)
    val py = sy + t * (ey - sy)
    return (tx - px) * (tx - px) + (ty - py) * (ty - py)
}

fun isTapNearDrawing(tapX: Float, tapY: Float, drawing: DrawingDef): Boolean {
    if (drawing.points.isEmpty()) return false
    if (drawing.points.size == 1) {
        val pt = drawing.points[0]
        val dx = tapX - pt.x
        val dy = tapY - pt.y
        return (dx * dx + dy * dy) < 0.0016f
    }
    
    return when (drawing.shapeType.lowercase()) {
        "line" -> {
            val start = drawing.points.first()
            val end = drawing.points.last()
            distanceToSegment(tapX, tapY, start.x, start.y, end.x, end.y) < 0.0025f
        }
        "box" -> {
            val start = drawing.points.first()
            val end = drawing.points.last()
            val l = minOf(start.x, end.x)
            val r = maxOf(start.x, end.x)
            val t = minOf(start.y, end.y)
            val b = maxOf(start.y, end.y)
            
            val d1 = distanceToSegment(tapX, tapY, l, t, r, t)
            val d2 = distanceToSegment(tapX, tapY, r, t, r, b)
            val d3 = distanceToSegment(tapX, tapY, r, b, l, b)
            val d4 = distanceToSegment(tapX, tapY, l, b, l, t)
            minOf(d1, d2, d3, d4) < 0.0025f
        }
        "filled_box" -> {
            val start = drawing.points.first()
            val end = drawing.points.last()
            val l = minOf(start.x, end.x)
            val r = maxOf(start.x, end.x)
            val t = minOf(start.y, end.y)
            val b = maxOf(start.y, end.y)
            
            (tapX in l..r && tapY in t..b) || {
                val d1 = distanceToSegment(tapX, tapY, l, t, r, t)
                val d2 = distanceToSegment(tapX, tapY, r, t, r, b)
                val d3 = distanceToSegment(tapX, tapY, r, b, l, b)
                val d4 = distanceToSegment(tapX, tapY, l, b, l, t)
                minOf(d1, d2, d3, d4) < 0.0025f
            }()
        }
        "circle" -> {
            val start = drawing.points.first()
            val end = drawing.points.last()
            val l = minOf(start.x, end.x)
            val r = maxOf(start.x, end.x)
            val t = minOf(start.y, end.y)
            val b = maxOf(start.y, end.y)
            
            val d1 = distanceToSegment(tapX, tapY, l, t, r, t)
            val d2 = distanceToSegment(tapX, tapY, r, t, r, b)
            val d3 = distanceToSegment(tapX, tapY, r, b, l, b)
            val d4 = distanceToSegment(tapX, tapY, l, b, l, t)
            minOf(d1, d2, d3, d4) < 0.003f
        }
        else -> {
            // "freehand", "brush" or custom lines
            drawing.points.any { pt ->
                val dx = tapX - pt.x
                val dy = tapY - pt.y
                (dx * dx + dy * dy) < 0.002f
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdScanCameraScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var imageCapture: androidx.camera.core.ImageCapture? by remember { mutableStateOf(null) }

    // Camera permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var liveCameraMode by remember { mutableStateOf(false) }
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            liveCameraMode = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        liveCameraMode = isGranted
        if (isGranted) {
            viewModel.triggerFeedback("Live hardware camera active!")
        } else {
            viewModel.triggerFeedback("Using high-fidelity scanner sandbox.")
        }
    }

    // Gallery picker launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val selectedBmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (selectedBmp != null) {
                    if (!state.isScanningIdBack) {
                        viewModel.captureIdCardFront(selectedBmp)
                        viewModel.triggerFeedback("ID Card Front captured! Open scanner for Back side.")
                    } else {
                        viewModel.captureIdCardBack(selectedBmp)
                        viewModel.navigateTo(Screen.IdScanEdit)
                    }
                } else {
                    android.widget.Toast.makeText(context, "Unable to load image.", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Gallery import error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val onSnapPage = {
        if (liveCameraMode && hasCameraPermission && imageCapture != null) {
            val capture = imageCapture
            if (capture != null) {
                capture.takePicture(
                    androidx.core.content.ContextCompat.getMainExecutor(context),
                    object : androidx.camera.core.ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                            val bitmap = imageProxyToBitmap(image)
                            image.close()
                            if (bitmap != null) {
                                if (!state.isScanningIdBack) {
                                    viewModel.captureIdCardFront(bitmap)
                                    viewModel.triggerFeedback("ID Card Front captured! Open scanner for Back side.")
                                } else {
                                    viewModel.captureIdCardBack(bitmap)
                                    viewModel.navigateTo(Screen.IdScanEdit)
                                }
                            } else {
                                val mock = createMockIdentityCard(isFront = !state.isScanningIdBack)
                                if (!state.isScanningIdBack) {
                                    viewModel.captureIdCardFront(mock)
                                    viewModel.triggerFeedback("ID Card Front captured (Fallback)! Open scanner for Back side.")
                                } else {
                                    viewModel.captureIdCardBack(mock)
                                    viewModel.navigateTo(Screen.IdScanEdit)
                                }
                            }
                        }

                        override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                            val mock = createMockIdentityCard(isFront = !state.isScanningIdBack)
                            if (!state.isScanningIdBack) {
                                viewModel.captureIdCardFront(mock)
                                viewModel.triggerFeedback("ID Card Front captured (Error Fallback)! Open scanner for Back side.")
                            } else {
                                viewModel.captureIdCardBack(mock)
                                viewModel.navigateTo(Screen.IdScanEdit)
                            }
                        }
                    }
                )
            }
        } else {
            // Sandbox Simulation Mode!
            val mock = createMockIdentityCard(isFront = !state.isScanningIdBack)
            if (!state.isScanningIdBack) {
                viewModel.captureIdCardFront(mock)
                viewModel.triggerFeedback("ID Card Front captured in Sandbox! Now open scanner for Back side.")
            } else {
                viewModel.captureIdCardBack(mock)
                viewModel.navigateTo(Screen.IdScanEdit)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserFloat"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ID Security Scan Lab", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (liveCameraMode) "🟢 LIVE DEVICE HARDWARE CAMERA" else "📡 ACTIVE HIGH-FIDELITY SANDBOX",
                            fontSize = 10.sp,
                            color = if (liveCameraMode) Color.Green else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dashboard")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0B0F19))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val currentStepLabel = if (!state.isScanningIdBack) "Scan Front Side of ID Card" else "Scan Back Side of ID Card"
            val currentInstruction = if (!state.isScanningIdBack) {
                "Hold steady. Align the Front Side of your card inside the bounding frame."
            } else {
                "Front captured successfully! Now align the Back Side of your card in the frame."
            }

            Text(
                text = currentStepLabel,
                color = Color(0xFF10AC84),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = currentInstruction,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Viewfinder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF111827), RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(2.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (liveCameraMode && hasCameraPermission) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraPreviewView(
                            modifier = Modifier.fillMaxSize(),
                            onImageCaptureCreated = { imageCapture = it },
                            onCornersDetected = {}
                        )

                        // Laser scan sweep
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val yPos = size.height * laserYOffset
                                    val brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Green.copy(alpha = 0f),
                                            Color.Green.copy(alpha = 0.4f),
                                            Color.Green.copy(alpha = 0f)
                                        ),
                                        startY = yPos - 40f,
                                        endY = yPos + 40f
                                    )
                                    drawRect(
                                        brush = brush,
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, yPos - 40f),
                                        size = androidx.compose.ui.geometry.Size(size.width, 80f)
                                    )
                                    drawLine(
                                        color = Color.Green,
                                        start = androidx.compose.ui.geometry.Offset(0f, yPos),
                                        end = androidx.compose.ui.geometry.Offset(size.width, yPos),
                                        strokeWidth = 3f
                                    )
                                }
                        )
                    }
                } else {
                    // Sandbox visual representation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Laser sweep
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val yPos = size.height * laserYOffset
                                    val brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF38BDF8).copy(alpha = 0f),
                                            Color(0xFF38BDF8).copy(alpha = 0.3f),
                                            Color(0xFF38BDF8).copy(alpha = 0f)
                                        ),
                                        startY = yPos - 40f,
                                        endY = yPos + 40f
                                    )
                                    drawRect(
                                        brush = brush,
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, yPos - 40f),
                                        size = androidx.compose.ui.geometry.Size(size.width, 80f)
                                    )
                                    drawLine(
                                        color = Color(0xFF38BDF8),
                                        start = androidx.compose.ui.geometry.Offset(0f, yPos),
                                        end = androidx.compose.ui.geometry.Offset(size.width, yPos),
                                        strokeWidth = 2f
                                    )
                                }
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF0369A1).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "SANDBOX ID CARD SCANNER",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Virtual high-fidelity capture active. Tap Snap below to simulate card framing.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snap & Controls deck
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Import Gallery button
                IconButton(
                    onClick = { galleryPickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }

                // Primary Snap Button
                Button(
                    onClick = { onSnapPage() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10AC84)),
                    shape = CircleShape,
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("id_scanner_snap_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Snap",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!state.isScanningIdBack) "SNAP FRONT SIDE" else "SNAP BACK SIDE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Preset Loader button
                IconButton(
                    onClick = {
                        val card = createMockIdentityCard(isFront = !state.isScanningIdBack)
                        if (!state.isScanningIdBack) {
                            viewModel.captureIdCardFront(card)
                            viewModel.triggerFeedback("Loaded ID Card Front Side Preset!")
                        } else {
                            viewModel.captureIdCardBack(card)
                            viewModel.triggerFeedback("Loaded ID Card Back Side Preset! Opening Filter Page.")
                            viewModel.navigateTo(Screen.IdScanEdit)
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Load Mock ID",
                        tint = Color(0xFF10AC84)
                    )
                }
            }

            if (!hasCameraPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Camera, "Camera Permission", tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Tap to authorize Live Device Camera",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdScanEditScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    var docTitle by remember { mutableStateOf("Unified ID Card Scan") }

    val baseFront = state.idCardFrontBitmap
    val baseBack = state.idCardBackBitmap

    val filteredFront = remember(baseFront, state.idScanFilterType) {
        if (baseFront != null) viewModel.applyFilterToBitmap(baseFront, state.idScanFilterType) else null
    }
    val filteredBack = remember(baseBack, state.idScanFilterType) {
        if (baseBack != null) viewModel.applyFilterToBitmap(baseBack, state.idScanFilterType) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ID Card Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.IdScanCamera) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveIdCardA4Document(docTitle)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10AC84))
                    ) {
                        Text("SAVE")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = docTitle,
                onValueChange = { docTitle = it },
                label = { Text("Document Filename") },
                placeholder = { Text("e.g. ID Card unified") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // A4 Layout Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f / 1.414f)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val scale = maxWidth / 210f
                        val cardWidthDp = scale * 86f
                        val cardHeightDp = scale * 54f

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Front Side portion
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (filteredFront != null) {
                                    Image(
                                        bitmap = filteredFront.asImageBitmap(),
                                        contentDescription = "Front side preview",
                                        modifier = Modifier
                                            .size(width = cardWidthDp, height = cardHeightDp)
                                            .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(4.dp))
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(width = cardWidthDp, height = cardHeightDp)
                                            .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No Front Side Captured", color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                }
                            }

                            // Divider
                            HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), color = Color(0xFFE2E8F0))

                            // Back Side portion
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (filteredBack != null) {
                                    Image(
                                        bitmap = filteredBack.asImageBitmap(),
                                        contentDescription = "Back side preview",
                                        modifier = Modifier
                                            .size(width = cardWidthDp, height = cardHeightDp)
                                            .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(4.dp))
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(width = cardWidthDp, height = cardHeightDp)
                                            .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No Back Side Captured", color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Converts screen-space touch coordinates to page-relative coordinate space (0..pageWidth, 0..pageHeight),
 * taking into account the page container's screen position, rotation, scale, and pan offsets.
 */
private fun convScreenPtToPagePt(
    screenX: Float,
    screenY: Float,
    pageWidthPx: Float,
    pageHeightPx: Float,
    pagePositionOnScreenX: Float,
    pagePositionOnScreenY: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationDegrees: Float
): android.graphics.PointF {
    val matrix = android.graphics.Matrix()
    
    // Calculate the touch relative to the page container position on screen
    val localX = screenX - pagePositionOnScreenX
    val localY = screenY - pagePositionOnScreenY

    // Construct the forward transformation applied by graphicsLayer:
    matrix.postTranslate(-pageWidthPx / 2f, -pageHeightPx / 2f)
    matrix.postRotate(rotationDegrees)
    matrix.postScale(scale, scale)
    matrix.postTranslate(pageWidthPx / 2f + offsetX, pageHeightPx / 2f + offsetY)

    // Invert the transformation to map from Screen/Container coordinates back to unscaled PDF Page space
    val inverse = android.graphics.Matrix()
    matrix.invert(inverse)

    val pts = floatArrayOf(localX, localY)
    inverse.mapPoints(pts)

    return android.graphics.PointF(pts[0], pts[1])
}

/**
 * Safely accesses the native Android MotionEvent via reflection to avoid compile issues
 * with Compose's internal visibility rules on getMotionEvent.
 */
private fun getNativeMotionEvent(pointerEvent: androidx.compose.ui.input.pointer.PointerEvent): android.view.MotionEvent? {
    return try {
        val method = pointerEvent::class.java.methods.firstOrNull { it.name == "getMotionEvent" }
        method?.invoke(pointerEvent) as? android.view.MotionEvent
    } catch (e: Exception) {
        null
    }
}

@Composable
fun StaticPagePreview(
    page: PageDef,
    state: UiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isLandscape = page.rotationDegrees % 180 != 0
    val pageRatio = 0.707f

    Card(
        modifier = modifier
            .aspectRatio(pageRatio)
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .background(Color.White),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthDp = maxWidth
            val heightDp = maxHeight
            
            // 1. Background image
            if (page.backgroundScanPath != null) {
                val file = java.io.File(page.backgroundScanPath)
                if (file.exists()) {
                    val bitmap = remember(page.id, page.filterType, page.backgroundScanPath) {
                        val base = AndroidBitmapLoader.load(file.absolutePath)
                        if (base != null && page.filterType != "original") {
                            viewModel.applyFilterToBitmap(base, page.filterType)
                        } else {
                            base
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            // 2. Base vector template background
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (page.type.lowercase()) {
                    "lined" -> {
                        val leftMargin = size.width * 0.15f
                        drawLine(Color(0xFFE09090), Offset(leftMargin, 0f), Offset(leftMargin, size.height), 2f)
                        var lineY = size.height * 0.08f
                        while (lineY < size.height * 0.95f) {
                            drawLine(Color(0xFFC5D3E8), Offset(0f, lineY), Offset(size.width, lineY), 1.5f)
                            lineY += 36f
                        }
                    }
                    "cornell" -> {
                        drawLine(Color(0xFF80B3D6), Offset(0f, size.height * 0.08f), Offset(size.width, size.height * 0.08f), 3f)
                        drawLine(Color(0xFF80B3D6), Offset(size.width * 0.3f, size.height * 0.08f), Offset(size.width * 0.3f, size.height * 0.85f), 3f)
                        drawLine(Color(0xFF80B3D6), Offset(0f, size.height * 0.85f), Offset(size.width, size.height * 0.85f), 3f)
                    }
                }
                
                // 3. Draw drawings
                for (draw in page.drawings) {
                    if (draw.points.size < 2) continue
                    val baseColor = try { Color(AndroidColor.parseColor(draw.colorHex)) } catch (e: Exception) { Color.Black }
                    val drawColor = if (draw.isHighlighter) baseColor.copy(alpha = 0.45f) else baseColor
                    val scaledStrokeWidth = draw.strokeWidth * (size.width / 400f)
                    val pathStyle = Stroke(
                        width = scaledStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (draw.isDashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f * (size.width / 400f), 15f * (size.width / 400f)), 0f) else null
                    )
                    
                    when (draw.shapeType.lowercase()) {
                        "line" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            drawLine(
                                color = drawColor,
                                start = Offset(start.x * size.width, start.y * size.height),
                                end = Offset(end.x * size.width, end.y * size.height),
                                strokeWidth = scaledStrokeWidth,
                                cap = StrokeCap.Round,
                                pathEffect = if (draw.isDashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f * (size.width / 400f), 15f * (size.width / 400f)), 0f) else null
                            )
                        }
                        "box" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            val left = minOf(start.x, end.x) * size.width
                            val top = minOf(start.y, end.y) * size.height
                            val right = maxOf(start.x, end.x) * size.width
                            val bottom = maxOf(start.y, end.y) * size.height
                            drawRect(
                                color = drawColor,
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = pathStyle
                            )
                        }
                        "filled_box" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            val left = minOf(start.x, end.x) * size.width
                            val top = minOf(start.y, end.y) * size.height
                            val right = maxOf(start.x, end.x) * size.width
                            val bottom = maxOf(start.y, end.y) * size.height
                            drawRect(
                                color = drawColor,
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = androidx.compose.ui.graphics.drawscope.Fill
                            )
                        }
                        "redact" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            val left = minOf(start.x, end.x) * size.width
                            val top = minOf(start.y, end.y) * size.height
                            val right = maxOf(start.x, end.x) * size.width
                            val bottom = maxOf(start.y, end.y) * size.height
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = androidx.compose.ui.graphics.drawscope.Fill
                            )
                            drawRect(
                                color = Color(0xFFEF4444),
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = Stroke(
                                    width = 1.5f * (size.width / 400f),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }
                        "circle" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            val left = minOf(start.x, end.x) * size.width
                            val top = minOf(start.y, end.y) * size.height
                            val right = maxOf(start.x, end.x) * size.width
                            val bottom = maxOf(start.y, end.y) * size.height
                            drawOval(
                                color = drawColor,
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = pathStyle
                            )
                        }
                        else -> {
                            val path = Path()
                            path.moveTo(draw.points.first().x * size.width, draw.points.first().y * size.height)
                            for (pt in draw.points.drop(1)) {
                                path.lineTo(pt.x * size.width, pt.y * size.height)
                            }
                            drawPath(
                                path = path,
                                color = drawColor,
                                style = pathStyle
                            )
                        }
                    }
                }
            }
            
            // 4. Draw text annotations
            page.textAnnotations.forEach { txtAnn ->
                val isBgTransparent = txtAnn.bgColorHex.lowercase() == "transparent" || txtAnn.bgColorHex.isBlank()
                val backgroundParsedColor = if (isBgTransparent) Color.Transparent else {
                    try { Color(AndroidColor.parseColor(txtAnn.bgColorHex)) } catch (e: Exception) { Color.Transparent }
                }
                val textColor = try { Color(AndroidColor.parseColor(txtAnn.colorHex)) } catch (e: Exception) { Color.Black }
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = widthDp * txtAnn.x,
                            y = heightDp * txtAnn.y
                        )
                        .background(backgroundParsedColor, RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = txtAnn.text,
                        color = textColor,
                        fontSize = (txtAnn.fontSize * (widthDp.value / 400f)).sp,
                        fontFamily = getFontFamily(txtAnn.fontName),
                        fontWeight = if (txtAnn.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (txtAnn.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.combine(
                            buildList {
                                if (txtAnn.hasUnderline) add(androidx.compose.ui.text.style.TextDecoration.Underline)
                                if (txtAnn.hasStrikeThrough) add(androidx.compose.ui.text.style.TextDecoration.LineThrough)
                            }
                        )
                    )
                }
            }
            
            // 5. Draw signatures
            page.signatures.forEach { sig ->
                val resolvedSigProfile = state.signatures.find { it.id == sig.signatureProfileId }
                if (resolvedSigProfile != null) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = widthDp * sig.x,
                                y = heightDp * sig.y
                            )
                            .size(
                                width = widthDp * (sig.width / 400f),
                                height = widthDp * (sig.height / 400f)
                            )
                    ) {
                        if (resolvedSigProfile.pathDataJson.startsWith("image:")) {
                            val imagePath = resolvedSigProfile.pathDataJson.removePrefix("image:")
                            val bitmap = remember(imagePath) {
                                try {
                                    android.graphics.BitmapFactory.decodeFile(imagePath)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                val vectorPoints = DocumentSerializer.pointsFromJson(resolvedSigProfile.pathDataJson)
                                if (vectorPoints.isNotEmpty()) {
                                    val overlayPath = Path()
                                    overlayPath.moveTo(vectorPoints.first().x * size.width, vectorPoints.first().y * size.height)
                                    for (pt in vectorPoints.drop(1)) {
                                        if (pt.x == -1f && pt.y == -1f) continue
                                        overlayPath.lineTo(pt.x * size.width, pt.y * size.height)
                                    }
                                    drawPath(
                                        path = overlayPath,
                                        color = try { Color(AndroidColor.parseColor(resolvedSigProfile.colorHex)) } catch (e: Exception) { Color.Black },
                                        style = Stroke(
                                            width = resolvedSigProfile.strokeWidth * (size.width / 160f),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropImageStudioDialog(
    uri: android.net.Uri,
    alias: String,
    onDismissRequest: () -> Unit,
    onCropCompleted: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val b = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap = b
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var rotationAngle by remember { mutableFloatStateOf(0f) }
            var translateX by remember { mutableFloatStateOf(0f) }
            var translateY by remember { mutableFloatStateOf(0f) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Crop Image Studio", color = Color.White, style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = onDismissRequest) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1E293B),
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                },
                containerColor = Color(0xFF0F172A)
            ) { paddingValues ->
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (bitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Failed to load selected photo", color = Color.White)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Crop and Position your Signature inside the white box. Drag to pan, pinch to zoom.",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Viewport Box
                        Box(
                            modifier = Modifier
                                .size(320.dp, 180.dp)
                                .shadow(8.dp, RoundedCornerShape(8.dp))
                                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        scale = (scale * zoom).coerceIn(0.1f, 15f)
                                        rotationAngle += rotation
                                        translateX += pan.x
                                        translateY += pan.y
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (bitmap != null) {
                                    drawIntoCanvas { canvas ->
                                        val nativeBmpCanvas = canvas.nativeCanvas
                                        nativeBmpCanvas.save()
                                        
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        
                                        nativeBmpCanvas.translate(centerX + translateX, centerY + translateY)
                                        nativeBmpCanvas.scale(scale, scale)
                                        nativeBmpCanvas.rotate(rotationAngle)
                                        
                                        val bmpW = bitmap!!.width.toFloat()
                                        val bmpH = bitmap!!.height.toFloat()
                                        
                                        nativeBmpCanvas.drawBitmap(
                                            bitmap!!,
                                            -bmpW / 2f,
                                            -bmpH / 2f,
                                            AndroidPaint().apply {
                                                isFilterBitmap = true
                                            }
                                        )
                                        nativeBmpCanvas.restore()
                                    }
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val dashPathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        val paint = AndroidPaint().apply {
                                            color = android.graphics.Color.BLUE
                                            alpha = 40
                                            style = android.graphics.Paint.Style.STROKE
                                            strokeWidth = 2f
                                            pathEffect = dashPathEffect
                                        }
                                        drawContext.canvas.nativeCanvas.drawLine(size.width / 2f, 0f, size.width / 2f, size.height, paint)
                                        drawContext.canvas.nativeCanvas.drawLine(0f, size.height / 2f, size.width, size.height / 2f, paint)
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Fine Adjustments & Presets",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rotate 90°", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    scale = 1f
                                    rotationAngle = 0f
                                    translateX = 0f
                                    translateY = 0f
                                },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", fontSize = 11.sp)
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Zoom: ${String.format("%.1fx", scale)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { scale = (scale - 0.15f).coerceAtLeast(0.1f) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { scale = (scale + 0.15f).coerceAtMost(15f) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Slider(
                                value = scale,
                                onValueChange = { scale = it },
                                valueRange = 0.1f..10f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Rotation Angle: ${rotationAngle.roundToInt()}°",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Slider(
                                value = rotationAngle,
                                onValueChange = { rotationAngle = it },
                                valueRange = -180f..180f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val currentBmp = bitmap
                                if (currentBmp != null) {
                                    val density = context.resources.displayMetrics.density
                                    val viewportWidthPx = (320 * density).toInt()
                                    val viewportHeightPx = (180 * density).toInt()

                                    val croppedBitmap = Bitmap.createBitmap(
                                        viewportWidthPx,
                                        viewportHeightPx,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = AndroidCanvas(croppedBitmap)

                                    canvas.drawColor(android.graphics.Color.WHITE)

                                    val centerX = viewportWidthPx / 2f
                                    val centerY = viewportHeightPx / 2f

                                    canvas.save()
                                    canvas.translate(centerX + translateX, centerY + translateY)
                                    canvas.scale(scale, scale)
                                    canvas.rotate(rotationAngle)

                                    val bmpW = currentBmp.width.toFloat()
                                    val bmpH = currentBmp.height.toFloat()

                                    val paint = AndroidPaint().apply {
                                        isFilterBitmap = true
                                    }
                                    canvas.drawBitmap(
                                        currentBmp,
                                        -bmpW / 2f,
                                        -bmpH / 2f,
                                        paint
                                    )
                                    canvas.restore()

                                    onCropCompleted(croppedBitmap)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("complete_crop_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = bitmap != null && !isLoading
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Complete Crop & Save Signature", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

