package com.choiceparalysis.turntable.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.choiceparalysis.turntable.ui.components.StandardEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.viewmodel.ColorSchemeVM
import com.choiceparalysis.turntable.viewmodel.OptionsVM
import com.choiceparalysis.turntable.viewmodel.SpinWheelVM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinWheelScreen(
    modifier: Modifier = Modifier,
    spinWheelVM: SpinWheelVM = hiltViewModel(),
    optionsVM: OptionsVM = hiltViewModel(),
    colorSchemeVM: ColorSchemeVM = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val options by optionsVM.options.collectAsState()
    val weights by optionsVM.weights.collectAsState()
    val isAnimating by spinWheelVM.isAnimating.collectAsState()
    val result by spinWheelVM.result.collectAsState()
    val spinTrigger by spinWheelVM.spinTrigger.collectAsState()
    val colorScheme by colorSchemeVM.colorScheme.collectAsState()
    val optionsEditorOpen by optionsVM.optionsEditorOpen.collectAsState()
    val dynamicColorEnabled by colorSchemeVM.dynamicColorEnabled.collectAsState()
    val customColors by colorSchemeVM.customColors.collectAsState()
    val optionGroups by optionsVM.optionGroups.collectAsState()

    // Clear display result when leaving screen so it restores from persistence on re-enter
    DisposableEffect(Unit) {
        onDispose { spinWheelVM.clearDisplayResult() }
    }

    // Keep ColorSchemeVM in sync with current options count
    LaunchedEffect(options.size) {
        colorSchemeVM.setOptionsCount(options.size)
    }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSaveGroupDialog by remember { mutableStateOf(false) }
    var showLoadGroupDialog by remember { mutableStateOf(false) }
    var colorPickerIndex by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val toastWhatAreYouDoing = stringResource(R.string.toast_what_are_you_doing)
    val toastDynamicColorOn = stringResource(R.string.toast_dynamic_color_on)
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Text(
                    text = stringResource(R.string.spin_wheel_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_wheel_settings),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spin Wheel
            SpinWheel(
                options = options,
                weights = weights,
                colorScheme = colorScheme,
                lastResult = result,
                modifier = Modifier.padding(bottom = 16.dp),
                onSpinResult = { selected ->
                    spinWheelVM.recordSpinResult(selected, options)
                },
                onSpinStart = { spinWheelVM.startSpin() },
                onSpinEnd = { spinWheelVM.endSpin() },
                onResultDragged = {
                    Toast.makeText(context, toastWhatAreYouDoing, Toast.LENGTH_SHORT).show()
                }
            )

            // Spin Button
            Button(
                onClick = { triggerSpinWheelSpin() },
                enabled = !isAnimating && options.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.indicatorColor
                )
            ) {
                Text(
                    text = if (isAnimating) stringResource(R.string.btn_spinning) else stringResource(R.string.btn_start_spin),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Options editor toggle button
            ElevatedButton(
                onClick = { optionsVM.toggleOptionsEditor() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_edit_options),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (optionsEditorOpen) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            // Expandable options editor
            AnimatedVisibility(
                visible = optionsEditorOpen,
                enter = expandVertically(tween(300, easing = StandardEasing.EaseOutCubic)) +
                        fadeIn(tween(250, easing = StandardEasing.EaseOutCubic)),
                exit = shrinkVertically(tween(250, easing = StandardEasing.EaseInCubic)) +
                       fadeOut(tween(200, easing = StandardEasing.EaseInCubic))
            ) {
                OptionsEditorPanel(
                    options = options,
                    weights = weights,
                    colorScheme = colorScheme,
                    dynamicColorEnabled = dynamicColorEnabled,
                    customColors = customColors,
                    onUpdateOption = { index, name -> optionsVM.updateOptionName(index, name) },
                    onRemoveOption = { optionsVM.removeOption(it) },
                    onAddOption = { optionsVM.addOption() },
                    onColorClick = { colorPickerIndex = it },
                    onDynamicColorClick = {
                        Toast.makeText(context, toastDynamicColorOn, Toast.LENGTH_SHORT).show()
                    },
                    onUpdateWeight = { index, weight -> optionsVM.updateWeight(index, weight) },
                    onSaveGroup = { showSaveGroupDialog = true },
                    onLoadGroup = { showLoadGroupDialog = true },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Show Android Toast only for new spin results (not restored from persistence)
    LaunchedEffect(spinTrigger) {
        if (spinTrigger > 0) {
            result?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }
    }

    // Settings bottom sheet
    if (showSettingsSheet) {
        WheelSettingsSheet(
            colorSchemeVM = colorSchemeVM,
            optionsVM = optionsVM,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Save group dialog
    if (showSaveGroupDialog) {
        SaveGroupDialog(
            onDismiss = { showSaveGroupDialog = false },
            onSave = { name ->
                optionsVM.saveOptionGroup(name)
                showSaveGroupDialog = false
            }
        )
    }

    // Load group dialog
    if (showLoadGroupDialog) {
        LoadGroupDialog(
            groups = optionGroups,
            onDismiss = { showLoadGroupDialog = false },
            onLoad = { group ->
                optionsVM.loadOptionGroup(group)
                showLoadGroupDialog = false
            },
            onDelete = { optionsVM.deleteOptionGroup(it) }
        )
    }

    // Color picker dialog
    val editingIndex = colorPickerIndex
    if (editingIndex != null) {
        val currentColor = if (editingIndex < customColors.size) {
            Color(customColors[editingIndex])
        } else {
            colorScheme.getColorForIndex(editingIndex)
        }
        ColorPickerDialog(
            initialColor = currentColor,
            onConfirm = { color ->
                colorSchemeVM.updateOptionColor(editingIndex, color.toArgb())
                colorPickerIndex = null
            },
            onDismiss = { colorPickerIndex = null }
        )
    }
}

@Composable
private fun OptionsEditorPanel(
    options: List<String>,
    weights: List<Int>,
    colorScheme: WheelColorScheme,
    dynamicColorEnabled: Boolean,
    customColors: List<Int>,
    onUpdateOption: (Int, String) -> Unit,
    onRemoveOption: (Int) -> Unit,
    onAddOption: () -> Unit,
    onColorClick: (Int) -> Unit,
    onDynamicColorClick: () -> Unit,
    onUpdateWeight: (Int, Int) -> Unit,
    onSaveGroup: () -> Unit,
    onLoadGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            options.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color indicator
                    val segmentColor = if (!dynamicColorEnabled && index < customColors.size) {
                        Color(customColors[index])
                    } else {
                        colorScheme.getColorForIndex(index)
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (dynamicColorEnabled) segmentColor.copy(alpha = 0.5f) else segmentColor)
                            .clickable {
                                if (dynamicColorEnabled) {
                                    onDynamicColorClick()
                                } else {
                                    onColorClick(index)
                                }
                            }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = option,
                        onValueChange = { onUpdateOption(index, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    IconButton(
                        onClick = { onRemoveOption(index) },
                        enabled = options.size > 2
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.cd_delete),
                            tint = if (options.size > 2) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                    }
                }
                // Weight slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.option_weight_label) + weights.getOrElse(index) { 1 }.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.width(48.dp)
                    )
                    Slider(
                        value = (weights.getOrElse(index) { 1 }).toFloat(),
                        onValueChange = { onUpdateWeight(index, it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (options.size < 10) {
                FilledTonalButton(
                    onClick = onAddOption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_add_option))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSaveGroup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_save_group))
                }
                FilledTonalButton(
                    onClick = onLoadGroup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_load_group))
                }
            }
        }
    }
}

@Composable
private fun SaveGroupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_save_option_group)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_group_name)) },
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

@Composable
private fun LoadGroupDialog(
    groups: List<com.choiceparalysis.turntable.data.model.OptionGroup>,
    onDismiss: () -> Unit,
    onLoad: (com.choiceparalysis.turntable.data.model.OptionGroup) -> Unit,
    onDelete: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_load_option_group)) },
        text = {
            if (groups.isEmpty()) {
                Text(stringResource(R.string.dialog_no_saved_groups))
            } else {
                Column {
                    groups.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = group.options.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { onLoad(group) }) {
                                Text(stringResource(R.string.btn_load))
                            }
                            IconButton(onClick = { onDelete(group.id) }) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.cd_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

