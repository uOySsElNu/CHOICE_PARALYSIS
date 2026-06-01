package com.choiceparalysis.turntable.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.choiceparalysis.turntable.viewmodel.ColorSchemeVM
import com.choiceparalysis.turntable.viewmodel.OptionsVM
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WheelSettingsSheet(
    colorSchemeVM: ColorSchemeVM,
    optionsVM: OptionsVM,
    onDismiss: () -> Unit,
) {
    val dynamicColorEnabled by colorSchemeVM.dynamicColorEnabled.collectAsState()
    val selectedPreset by colorSchemeVM.selectedPresetName.collectAsState()
    val customColors by colorSchemeVM.customColors.collectAsState()
    val options by optionsVM.options.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var colorPickerIndex by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "转盘设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dynamic color toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "动态色彩",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "根据时间自动调整转盘色彩",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = dynamicColorEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { colorSchemeVM.setDynamicColorEnabled(enabled) }
                    }
                )
            }

            if (!dynamicColorEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Preset themes
                Text(
                    text = "预设主题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WheelDesign.entries.forEach { design ->
                        val isSelected = selectedPreset == design.name
                        PresetThemeChip(
                            design = design,
                            isSelected = isSelected,
                            onClick = {
                                scope.launch { colorSchemeVM.setSelectedPreset(design.name) }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Per-option colors
                Text(
                    text = "选项颜色",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        val color = if (index < customColors.size) {
                            Color(customColors[index])
                        } else {
                            WheelDesign.CLASSIC_RAINBOW.getColorForIndex(index)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { colorPickerIndex = index }
                        )
                    }
                }
            }

            if (dynamicColorEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "动态色彩已开启，颜色将根据时间自动变化",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
            }
        }
    }

    // Color picker dialog
    val editingIndex = colorPickerIndex
    if (editingIndex != null) {
        val currentColor = if (editingIndex < customColors.size) {
            Color(customColors[editingIndex])
        } else {
            WheelDesign.CLASSIC_RAINBOW.getColorForIndex(editingIndex)
        }
        ColorPickerDialog(
            initialColor = currentColor,
            onConfirm = { color ->
                scope.launch { colorSchemeVM.updateOptionColor(editingIndex, color.toArgb()) }
                colorPickerIndex = null
            },
            onDismiss = { colorPickerIndex = null }
        )
    }
}

@Composable
private fun PresetThemeChip(
    design: WheelDesign,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        border = if (isSelected) {
            ButtonDefaults.outlinedButtonBorder(enabled = true)
        } else {
            null
        },
        modifier = Modifier.height(40.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(design.colors.take(6))
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${design.emoji} ${design.displayName}",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).roundToInt()
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
