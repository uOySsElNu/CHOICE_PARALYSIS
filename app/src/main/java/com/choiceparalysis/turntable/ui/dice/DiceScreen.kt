package com.choiceparalysis.turntable.ui.dice

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choiceparalysis.turntable.ui.coindice.Dice3DRoll
import com.choiceparalysis.turntable.ui.coindice.ShakeDetector
import com.choiceparalysis.turntable.viewmodel.DiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceScreen(
    modifier: Modifier = Modifier,
    viewModel: DiceViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val diceValue by viewModel.diceValue.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val context = LocalContext.current
    DisposableEffect(isAnimating) {
        val shakeDetector = ShakeDetector(context) {
            if (!isAnimating) {
                viewModel.rollDice()
            }
        }
        shakeDetector.start()
        onDispose { shakeDetector.stop() }
    }

    val toastMessage = diceValue?.let { "点数: $it" }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearResult()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "掷骰子",
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

        Spacer(modifier = Modifier.height(16.dp))

        Dice3DRoll(
            value = diceValue,
            isAnimating = isAnimating,
            onAnimationComplete = { viewModel.onDiceRollAnimationComplete() },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = { viewModel.rollDice() },
            enabled = !isAnimating,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isAnimating) "滚动中..." else "掷骰子",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
