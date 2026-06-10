package com.choiceparalysis.turntable.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.choiceparalysis.turntable.R
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var red by remember { mutableFloatStateOf(initialColor.red * 255) }
    var green by remember { mutableFloatStateOf(initialColor.green * 255) }
    var blue by remember { mutableFloatStateOf(initialColor.blue * 255) }
    var hexInput by remember { mutableStateOf(colorToHex(initialColor)) }
    var hexError by remember { mutableStateOf(false) }

    val currentColor = Color(red / 255f, green / 255f, blue / 255f)

    fun syncHexFromSliders() {
        hexInput = colorToHex(currentColor)
        hexError = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.dialog_select_color), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hex input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        val parsed = parseHexColor(input)
                        if (parsed != null) {
                            red = parsed.red * 255
                            green = parsed.green * 255
                            blue = parsed.blue * 255
                            hexError = false
                        } else {
                            hexError = input.length >= 6
                        }
                    },
                    label = { Text(stringResource(R.string.label_color_code)) },
                    prefix = { Text("#") },
                    isError = hexError,
                    supportingText = if (hexError) {
                        { Text(stringResource(R.string.error_invalid_color_code)) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // RGB sliders
                Text(
                    text = "RGB",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ColorSlider(
                    label = "R",
                    value = red,
                    color = Color(red / 255f, 0f, 0f),
                    onValueChange = { red = it; syncHexFromSliders() }
                )
                ColorSlider(
                    label = "G",
                    value = green,
                    color = Color(0f, green / 255f, 0f),
                    onValueChange = { green = it; syncHexFromSliders() }
                )
                ColorSlider(
                    label = "B",
                    value = blue,
                    color = Color(0f, 0f, blue / 255f),
                    onValueChange = { blue = it; syncHexFromSliders() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Common colors palette
                Text(
                    text = stringResource(R.string.color_picker_common_colors),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    COMMON_COLORS.forEach { color ->
                        val isSelected = currentColor.toArgb() == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    red = color.red * 255
                                    green = color.green * 255
                                    blue = color.blue * 255
                                    syncHexFromSliders()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor) }) {
                Text(stringResource(R.string.btn_confirm))
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
private fun ColorSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
            )
        )
        Text(
            text = value.roundToInt().toString(),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp)
        )
    }
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).roundToInt()
    val g = (color.green * 255).roundToInt()
    val b = (color.blue * 255).roundToInt()
    return "%02X%02X%02X".format(r, g, b)
}

private fun parseHexColor(hex: String): Color? {
    val clean = hex.removePrefix("#").trim()
    return when (clean.length) {
        6 -> runCatching {
            val r = clean.substring(0, 2).toInt(16)
            val g = clean.substring(2, 4).toInt(16)
            val b = clean.substring(4, 6).toInt(16)
            Color(r / 255f, g / 255f, b / 255f)
        }.getOrNull()
        3 -> runCatching {
            val r = clean[0].toString().repeat(2).toInt(16)
            val g = clean[1].toString().repeat(2).toInt(16)
            val b = clean[2].toString().repeat(2).toInt(16)
            Color(r / 255f, g / 255f, b / 255f)
        }.getOrNull()
        else -> null
    }
}


private val COMMON_COLORS = listOf(
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF4CAF50), // Green
    Color(0xFF8BC34A), // Light Green
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFF9800), // Orange
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF795548), // Brown
    Color(0xFF607D8B), // Blue Grey
    Color(0xFFE53935), // Red
    Color(0xFF1E88E5), // Light Blue
    Color(0xFF43A047), // Dark Green
    Color(0xFFFDD835), // Dark Yellow
    Color(0xFFFB8C00), // Dark Orange
    Color(0xFFD81B60), // Dark Pink
    Color(0xFF00ACC1), // Dark Cyan
    Color(0xFF5E35B1), // Dark Purple
    Color(0xFF3949AB), // Dark Indigo
    Color(0xFFC0CA33), // Lime
    Color(0xFFFFFFFF), // White
    Color(0xFF9E9E9E), // Grey
    Color(0xFF424242), // Dark Grey
    Color(0xFF000000), // Black
)
