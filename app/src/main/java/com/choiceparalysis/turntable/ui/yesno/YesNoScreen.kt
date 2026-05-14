package com.choiceparalysis.turntable.ui.yesno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choiceparalysis.turntable.ui.components.ResultToast
import com.choiceparalysis.turntable.viewmodel.YesNoResult
import com.choiceparalysis.turntable.viewmodel.YesNoViewModel

@Composable
fun YesNoScreen(
    modifier: Modifier = Modifier,
    viewModel: YesNoViewModel = viewModel(),
) {
    val result by viewModel.result.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val customQuestion by viewModel.customQuestion.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Yes / No 决策",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Question Input
            OutlinedTextField(
                value = customQuestion,
                onValueChange = { viewModel.updateQuestion(it) },
                label = { Text("输入你的问题 (可选)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true
            )

            // Decide Button
            Button(
                onClick = { viewModel.decide() },
                enabled = !isAnimating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (isAnimating) "决定中..." else "帮我决定!",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Result Toast
        result?.let { yesNoResult ->
            val displayText = "${yesNoResult.emoji} ${yesNoResult.displayName}"
            ResultToast(
                result = displayText,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                onDismiss = { viewModel.clearResult() }
            )
        }
    }
}
