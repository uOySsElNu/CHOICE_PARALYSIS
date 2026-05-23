package com.choiceparalysis.turntable.ui.coin

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.audio.HapticType
import com.choiceparalysis.turntable.audio.SoundEffect
import com.choiceparalysis.turntable.ui.coindice.Coin3DFlip
import com.choiceparalysis.turntable.ui.components.ImageCustomizationSheet
import com.choiceparalysis.turntable.viewmodel.CoinSide
import com.choiceparalysis.turntable.viewmodel.CoinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinScreen(
    modifier: Modifier = Modifier,
    viewModel: CoinViewModel = viewModel(),
) {
    val coinResult by viewModel.coinResult.collectAsState()
    val pendingCoinResult by viewModel.pendingCoinResult.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val customCoinHeadsUri by viewModel.customCoinHeadsUri.collectAsState()
    val customCoinTailsUri by viewModel.customCoinTailsUri.collectAsState()
    var showCustomizationSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val defaultHeadsBitmap by produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(context.resources, R.drawable.coin_default_heads).asImageBitmap()
        }
    }
    val defaultTailsBitmap by produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(context.resources, R.drawable.coin_default_tails).asImageBitmap()
        }
    }

    val headsBitmap by produceState<ImageBitmap?>(null, customCoinHeadsUri, defaultHeadsBitmap) {
        value = customCoinHeadsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(uri).build()
            val result = withContext(Dispatchers.IO) { loader.execute(request) }
            if (result is SuccessResult) result.image.toBitmap().asImageBitmap() else null
        } ?: defaultHeadsBitmap
    }

    val tailsBitmap by produceState<ImageBitmap?>(null, customCoinTailsUri, defaultTailsBitmap) {
        value = customCoinTailsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(uri).build()
            val result = withContext(Dispatchers.IO) { loader.execute(request) }
            if (result is SuccessResult) result.image.toBitmap().asImageBitmap() else null
        } ?: defaultTailsBitmap
    }

    val toastMessage = coinResult?.let {
        if (it == CoinSide.HEADS) "正面朝上" else "反面朝上"
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            val audioHaptic = AudioHapticManager.getInstance(context)
            audioHaptic.playSound(SoundEffect.COIN_CLINK)
            audioHaptic.performHaptic(HapticType.COIN_FLIP)
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
                    text = "抛硬币",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            },
            actions = {
                IconButton(onClick = { showCustomizationSheet = true }) {
                    Icon(Icons.Default.Palette, contentDescription = "自定义图片")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Coin3DFlip(
            result = coinResult,
            pendingResult = pendingCoinResult,
            isAnimating = isAnimating,
            headsImage = headsBitmap,
            tailsImage = tailsBitmap,
            onAnimationComplete = { viewModel.onCoinFlipAnimationComplete() },
            onDragFlipComplete = { viewModel.flipCoinDirectly(it) },
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

    if (showCustomizationSheet) {
        ImageCustomizationSheet(
            viewModel = viewModel,
            onDismiss = { showCustomizationSheet = false }
        )
    }
}
