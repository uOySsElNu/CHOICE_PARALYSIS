package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.choiceparalysis.turntable.ui.components.AnimatedResult
import com.choiceparalysis.turntable.viewmodel.CoinDiceViewModel
import com.choiceparalysis.turntable.viewmodel.CoinSide

enum class CoinDiceMode(val displayName: String) {
    COIN("抛硬币"),
    DICE("掷骰子")
}

@Composable
fun CoinDiceScreen(
    modifier: Modifier = Modifier,
    viewModel: CoinDiceViewModel = viewModel(),
) {
    var mode by remember { mutableStateOf(CoinDiceMode.COIN) }
    val coinResult by viewModel.coinResult.collectAsState()
    val diceValue by viewModel.diceValue.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val customCoinHeadsUri by viewModel.customCoinHeadsUri.collectAsState()
    val customCoinTailsUri by viewModel.customCoinTailsUri.collectAsState()
    val customDiceUris by viewModel.customDiceUris.collectAsState()
    var showCustomizationSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Load coin images
    val headsBitmap by produceState<ImageBitmap?>(null, customCoinHeadsUri) {
        value = customCoinHeadsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.image as? ImageBitmap)
            } else null
        }
    }

    val tailsBitmap by produceState<ImageBitmap?>(null, customCoinTailsUri) {
        value = customCoinTailsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.image as? ImageBitmap)
            } else null
        }
    }

    // Load dice images
    val diceBitmaps by produceState<Map<Int, ImageBitmap>>(emptyMap(), customDiceUris) {
        value = customDiceUris.mapValues { (_, uri) ->
            uri?.let { uriString ->
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(uriString)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    (result.image as? ImageBitmap)
                } else null
            }
        }.filterValues { it != null }.mapValues { it.value!! }
    }

    // Clear results when switching modes
    LaunchedEffect(mode) {
        viewModel.clearResults()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "硬币 & 骰子",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showCustomizationSheet = true }) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = "自定义图片",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CoinDiceMode.entries.forEach { coinDiceMode ->
                OutlinedButton(
                    onClick = {
                        if (mode != coinDiceMode) {
                            mode = coinDiceMode
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = coinDiceMode.displayName,
                        fontWeight = if (mode == coinDiceMode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Coin or Dice
        when (mode) {
            CoinDiceMode.COIN -> {
                Coin3DFlip(
                    result = coinResult,
                    isAnimating = isAnimating,
                    headsImage = headsBitmap,
                    tailsImage = tailsBitmap,
                    onAnimationComplete = { viewModel.onCoinFlipAnimationComplete() },
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = { viewModel.flipCoin() },
                    enabled = !isAnimating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (isAnimating) "翻转中..." else "抛硬币",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                coinResult?.let { result ->
                    AnimatedResult(
                        result = result.displayName,
                        subtitle = if (result == CoinSide.HEADS) "正面朝上" else "反面朝上"
                    )
                }
            }

            CoinDiceMode.DICE -> {
                Dice3DRoll(
                    value = diceValue,
                    isAnimating = isAnimating,
                    faceImages = diceBitmaps,
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

                Spacer(modifier = Modifier.height(16.dp))

                diceValue?.let { value ->
                    AnimatedResult(
                        result = value.toString(),
                        subtitle = "点数: $value"
                    )
                }
            }
        }
    }

    if (showCustomizationSheet) {
        ImageCustomizationSheet(
            viewModel = viewModel,
            onDismiss = { showCustomizationSheet = false }
        )
    }
}
