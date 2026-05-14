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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import com.choiceparalysis.turntable.ui.components.StandardEasing
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import android.graphics.BitmapFactory
import com.choiceparalysis.turntable.R
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
    val pendingCoinResult by viewModel.pendingCoinResult.collectAsState()
    val diceValue by viewModel.diceValue.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val customCoinHeadsUri by viewModel.customCoinHeadsUri.collectAsState()
    val customCoinTailsUri by viewModel.customCoinTailsUri.collectAsState()
    var showCustomizationSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Default coin images
    val defaultHeadsBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.coin_default_heads).asImageBitmap()
    }
    val defaultTailsBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.coin_default_tails).asImageBitmap()
    }

    // Load coin images with fallback to defaults
    val headsBitmap by produceState<ImageBitmap?>(null, customCoinHeadsUri) {
        value = customCoinHeadsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                result.image.toBitmap().asImageBitmap()
            } else null
        } ?: defaultHeadsBitmap
    }

    val tailsBitmap by produceState<ImageBitmap?>(null, customCoinTailsUri) {
        value = customCoinTailsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                result.image.toBitmap().asImageBitmap()
            } else null
        } ?: defaultTailsBitmap
    }

    // Clear results when switching modes
    LaunchedEffect(mode) {
        viewModel.clearResults()
    }

    // Determine toast message
    val toastMessage = when (mode) {
        CoinDiceMode.COIN -> coinResult?.let {
            if (it == CoinSide.HEADS) "正面朝上" else "反面朝上"
        }
        CoinDiceMode.DICE -> diceValue?.let { "点数: $it" }
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
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

            // Coin or Dice with transition animation
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(500, easing = StandardEasing.EaseInOutQuart)) +
                     scaleIn(tween(500, easing = StandardEasing.EaseInOutQuart))) togetherWith
                    (fadeOut(tween(500, easing = StandardEasing.EaseInOutQuart)) +
                     scaleOut(tween(500, easing = StandardEasing.EaseInOutQuart))) using
                    SizeTransform(clip = false)
                },
                label = "modeSwitch"
            ) { targetMode ->
                when (targetMode) {
                    CoinDiceMode.COIN -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Coin3DFlip(
                                result = coinResult,
                                pendingResult = pendingCoinResult,
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
                        }
                    }

                    CoinDiceMode.DICE -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Show Android Toast for result
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearResults()
        }
    }

    if (showCustomizationSheet) {
        ImageCustomizationSheet(
            viewModel = viewModel,
            onDismiss = { showCustomizationSheet = false }
        )
    }
}
