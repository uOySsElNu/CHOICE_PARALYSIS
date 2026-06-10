package com.choiceparalysis.turntable.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.model.CoinPreset
import com.choiceparalysis.turntable.data.repository.SettingsRepository.Companion.DEFAULT_PRESET_ID
import com.choiceparalysis.turntable.ui.coindice.Coin3DFlip
import com.choiceparalysis.turntable.viewmodel.CoinSide
import com.choiceparalysis.turntable.viewmodel.CoinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCustomizationSheet(
    viewModel: CoinViewModel,
    onDismiss: () -> Unit,
) {
    val customCoinHeadsUri by viewModel.customCoinHeadsUri.collectAsState()
    val customCoinTailsUri by viewModel.customCoinTailsUri.collectAsState()
    val coinPresets by viewModel.coinPresets.collectAsState()
    var showEditor by remember { mutableStateOf(false) }

    // Editor state
    var editingHeadsUri by remember { mutableStateOf<String?>(null) }
    var editingTailsUri by remember { mutableStateOf<String?>(null) }

    if (showEditor) {
        CoinImageEditorDialog(
            viewModel = viewModel,
            initialHeadsUri = editingHeadsUri,
            initialTailsUri = editingTailsUri,
            onDismiss = { showEditor = false }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.coin_presets_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { viewModel.clearAllImages() }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_restore_default))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2-column grid: "+" first, then presets
            val allItems = listOf<CoinPreset?>(null) + coinPresets
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(allItems, key = { it?.id ?: "add_new" }) { preset ->
                    if (preset == null) {
                        // "+" button — exact same layout as preset cards
                        ElevatedCard(
                            onClick = {
                                editingHeadsUri = null
                                editingTailsUri = null
                                showEditor = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    MaterialTheme.shapes.medium
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Same row structure as presets
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.preset_custom),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Match "默认" label height or delete button
                                Text(
                                    text = " ",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    } else {
                        val isActive = (customCoinHeadsUri == null && customCoinTailsUri == null && preset.id == DEFAULT_PRESET_ID) ||
                                (preset.headsImagePath == customCoinHeadsUri && preset.tailsImagePath == customCoinTailsUri)
                        CoinPresetCard(
                            preset = preset,
                            isActive = isActive,
                            onLoad = { viewModel.loadCoinPreset(preset) },
                            onDelete = { viewModel.deleteCoinPreset(preset.id) }
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoinImageEditorDialog(
    viewModel: CoinViewModel,
    initialHeadsUri: String?,
    initialTailsUri: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var headsUri by remember { mutableStateOf(initialHeadsUri) }
    var tailsUri by remember { mutableStateOf(initialTailsUri) }
    var selectedSlot by remember { mutableStateOf<CoinSide?>(null) }
    var cropSourceUri by remember { mutableStateOf<Uri?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            cropSourceUri = selectedUri
        }
    }

    // Crop dialog
    cropSourceUri?.let { uri ->
        CircularCropDialog(
            imageUri = uri,
            onCropConfirmed = { croppedPath ->
                when (selectedSlot) {
                    CoinSide.HEADS -> headsUri = croppedPath
                    CoinSide.TAILS -> tailsUri = croppedPath
                    null -> {}
                }
                cropSourceUri = null
            },
            onDismiss = { cropSourceUri = null }
        )
    }

    // Save preset dialog
    if (showSaveDialog) {
        SaveCoinPresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                // Apply images to ViewModel first
                viewModel.setCustomCoinImage(CoinSide.HEADS, headsUri)
                viewModel.setCustomCoinImage(CoinSide.TAILS, tailsUri)
                viewModel.saveCoinPreset(name)
                showSaveDialog = false
                onDismiss()
            }
        )
    }

    // Load selected images for preview (null = plain coin)
    val headsBitmap by produceState<ImageBitmap?>(null, headsUri) {
        value = headsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(uri).build()
            val result = withContext(Dispatchers.IO) { loader.execute(request) }
            if (result is SuccessResult) result.image.toBitmap().asImageBitmap() else null
        }
    }
    val tailsBitmap by produceState<ImageBitmap?>(null, tailsUri) {
        value = tailsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(uri).build()
            val result = withContext(Dispatchers.IO) { loader.execute(request) }
            if (result is SuccessResult) result.image.toBitmap().asImageBitmap() else null
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.custom_coin_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Coin preview — draggable
            Coin3DFlip(
                result = null,
                isAnimating = false,
                headsImage = headsBitmap,
                tailsImage = tailsBitmap,
                modifier = Modifier.size(200.dp)
            )
            Text(
                text = stringResource(R.string.custom_coin_drag_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Image selection buttons — compact row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ImageSelectButton(
                    label = stringResource(R.string.coin_heads),
                    uri = headsUri,
                    size = 64.dp,
                    onClick = {
                        selectedSlot = CoinSide.HEADS
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                Spacer(modifier = Modifier.width(24.dp))
                ImageSelectButton(
                    label = stringResource(R.string.coin_tails),
                    uri = tailsUri,
                    size = 64.dp,
                    onClick = {
                        selectedSlot = CoinSide.TAILS
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save button
            OutlinedButton(
                onClick = {
                    viewModel.setCustomCoinImage(CoinSide.HEADS, headsUri)
                    viewModel.setCustomCoinImage(CoinSide.TAILS, tailsUri)
                    showSaveDialog = true
                },
                enabled = headsUri != null && tailsUri != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_save_as_preset))
            }

            // Apply button
            TextButton(
                onClick = {
                    viewModel.setCustomCoinImage(CoinSide.HEADS, headsUri)
                    viewModel.setCustomCoinImage(CoinSide.TAILS, tailsUri)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_apply_directly))
            }
        }
    }
}

@Composable
private fun ImageSelectButton(
    label: String,
    uri: String?,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = uri),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_select_image),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CoinPresetCard(
    preset: CoinPreset,
    isActive: Boolean = false,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDefault = preset.id == DEFAULT_PRESET_ID
    val colors = if (isActive) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        CardDefaults.elevatedCardColors()
    }

    ElevatedCard(
        onClick = onLoad,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isActive) Modifier.border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    MaterialTheme.shapes.medium
                ) else Modifier
            ),
        colors = colors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = preset.headsImagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = preset.tailsImagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (isDefault) {
                Text(
                    text = stringResource(R.string.preset_default),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete),
                        modifier = Modifier.size(16.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveCoinPresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_save_coin_preset)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_preset_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
