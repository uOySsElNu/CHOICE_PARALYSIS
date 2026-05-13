package com.choiceparalysis.turntable.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choiceparalysis.turntable.ui.components.AnimatedResult
import com.choiceparalysis.turntable.viewmodel.SpinWheelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinWheelScreen(
    modifier: Modifier = Modifier,
    viewModel: SpinWheelViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val options by viewModel.options.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()
    val result by viewModel.result.collectAsState()
    val rotationDegrees by viewModel.rotationDegrees.collectAsState()
    val colorScheme by viewModel.colorScheme.collectAsState()
    val optionsEditorOpen by viewModel.optionsEditorOpen.collectAsState()
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()
    val optionGroups by viewModel.optionGroups.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSaveGroupDialog by remember { mutableStateOf(false) }
    var showLoadGroupDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                Text(
                    text = "转盘决策",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "转盘设置",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spin Wheel
            SpinWheel(
                options = options,
                rotationDegrees = rotationDegrees,
                isSpinning = isSpinning,
                colorScheme = colorScheme,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Spin Button
            Button(
                onClick = { viewModel.spin() },
                enabled = !isSpinning && options.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.indicatorColor
                )
            ) {
                Text(
                    text = if (isSpinning) "转动中..." else "开始转动",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Result
            AnimatedResult(result = result)

            Spacer(modifier = Modifier.height(12.dp))

            // Options editor toggle button
            ElevatedButton(
                onClick = { viewModel.toggleOptionsEditor() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "编辑选项",
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
                enter = expandVertically(tween(300)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(150))
            ) {
                OptionsEditorPanel(
                    options = options,
                    colorScheme = colorScheme,
                    dynamicColorEnabled = dynamicColorEnabled,
                    onUpdateOption = { index, name -> viewModel.updateOptionName(index, name) },
                    onRemoveOption = { viewModel.removeOption(it) },
                    onAddOption = { viewModel.addOption() },
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

    // Settings bottom sheet
    if (showSettingsSheet) {
        WheelSettingsSheet(
            viewModel = viewModel,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Save group dialog
    if (showSaveGroupDialog) {
        SaveGroupDialog(
            onDismiss = { showSaveGroupDialog = false },
            onSave = { name ->
                viewModel.saveOptionGroup(name)
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
                viewModel.loadOptionGroup(group)
                showLoadGroupDialog = false
            },
            onDelete = { viewModel.deleteOptionGroup(it) }
        )
    }
}

@Composable
private fun OptionsEditorPanel(
    options: List<String>,
    colorScheme: WheelColorScheme,
    dynamicColorEnabled: Boolean,
    onUpdateOption: (Int, String) -> Unit,
    onRemoveOption: (Int) -> Unit,
    onAddOption: () -> Unit,
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
                    val segmentColor = colorScheme.getColorForIndex(index)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (dynamicColorEnabled) segmentColor.copy(alpha = 0.5f) else segmentColor)
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
                            contentDescription = "删除",
                            tint = if (options.size > 2) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                    }
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
                    Text("+ 添加选项")
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
                    Text("保存组合")
                }
                FilledTonalButton(
                    onClick = onLoadGroup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("加载组合")
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
        title = { Text("保存选项组合") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("组合名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
        title = { Text("加载选项组合") },
        text = {
            if (groups.isEmpty()) {
                Text("暂无保存的组合")
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
                                Text("加载")
                            }
                            IconButton(onClick = { onDelete(group.id) }) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "删除",
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
                Text("关闭")
            }
        }
    )
}
