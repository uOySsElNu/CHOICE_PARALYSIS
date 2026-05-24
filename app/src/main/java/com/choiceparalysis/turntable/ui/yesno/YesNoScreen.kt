package com.choiceparalysis.turntable.ui.yesno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choiceparalysis.turntable.viewmodel.YesNoResult
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.audio.SoundEffect
import com.choiceparalysis.turntable.viewmodel.YesNoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YesNoScreen(
    modifier: Modifier = Modifier,
    viewModel: YesNoViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val result by viewModel.result.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val customQuestion by viewModel.customQuestion.collectAsState()
    val context = LocalContext.current
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Yes / No 决策",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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
        }
    }

    // Show Android Toast for result
    LaunchedEffect(result) {
        result?.let { yesNoResult ->
            val audioHaptic = AudioHapticManager.getInstance(context)
            audioHaptic.playFeedback(SoundEffect.YESNO_CHIME)
            val displayText = "${yesNoResult.emoji} ${yesNoResult.displayName}"
            Toast.makeText(context, displayText, Toast.LENGTH_SHORT).show()
            viewModel.clearResult()
        }
    }
}
