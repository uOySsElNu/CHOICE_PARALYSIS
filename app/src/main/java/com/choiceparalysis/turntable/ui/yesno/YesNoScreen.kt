package com.choiceparalysis.turntable.ui.yesno

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import com.choiceparalysis.turntable.ui.components.StandardEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Column(
        modifier = modifier
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

        // Result
        val visibleState = remember { MutableTransitionState(false) }
        visibleState.targetState = result != null

        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(400, easing = StandardEasing.EaseOutCubic)) +
                    scaleIn(tween(400, easing = StandardEasing.EaseOutCubic)) +
                    slideInVertically(
                        tween(400, easing = StandardEasing.EaseOutCubic)
                    ) { it / 3 }
        ) {
            result?.let { yesNoResult ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (yesNoResult) {
                            YesNoResult.YES -> MaterialTheme.colorScheme.primaryContainer
                            YesNoResult.NO -> MaterialTheme.colorScheme.errorContainer
                            YesNoResult.MAYBE -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = yesNoResult.emoji,
                            fontSize = 72.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = yesNoResult.displayName,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
