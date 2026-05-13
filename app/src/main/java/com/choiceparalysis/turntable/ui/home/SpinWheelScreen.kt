package com.choiceparalysis.turntable.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choiceparalysis.turntable.ui.components.AnimatedResult
import com.choiceparalysis.turntable.viewmodel.SpinWheelViewModel

@Composable
fun SpinWheelScreen(
    modifier: Modifier = Modifier,
    viewModel: SpinWheelViewModel = viewModel(),
) {
    val options by viewModel.options.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()
    val result by viewModel.result.collectAsState()
    val rotationDegrees by viewModel.rotationDegrees.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "转盘决策",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Spin Wheel
        SpinWheel(
            options = options,
            rotationDegrees = rotationDegrees,
            isSpinning = isSpinning,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Spin Button
        Button(
            onClick = { viewModel.spin() },
            enabled = !isSpinning && options.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isSpinning) "转动中..." else "开始转动",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result
        AnimatedResult(result = result)

        Spacer(modifier = Modifier.height(16.dp))

        // Options Editor
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "自定义选项",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newValue ->
                                val newOptions = options.toMutableList()
                                newOptions[index] = newValue
                                viewModel.updateOptions(newOptions)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                val newOptions = options.toMutableList()
                                newOptions.removeAt(index)
                                viewModel.updateOptions(newOptions)
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "删除")
                        }
                    }
                }

                if (options.size < 10) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.updateOptions(options + "选项${options.size + 1}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                        Text("添加选项")
                    }
                }
            }
        }
    }
}
