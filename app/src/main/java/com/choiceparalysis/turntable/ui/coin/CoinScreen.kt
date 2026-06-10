package com.choiceparalysis.turntable.ui.coin

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.ui.coindice.Coin3DFlip
import com.choiceparalysis.turntable.ui.components.ImageCustomizationSheet
import com.choiceparalysis.turntable.viewmodel.CoinSide
import com.choiceparalysis.turntable.viewmodel.CoinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinScreen(
    onBack: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    viewModel: CoinViewModel = hiltViewModel(),
) {
    val coinResult by viewModel.coinResult.collectAsState()
    val flipTrigger by viewModel.flipTrigger.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val isFling by viewModel.isFling.collectAsState()
    val customCoinHeadsUri by viewModel.customCoinHeadsUri.collectAsState()

    // Reset animation state when leaving screen to prevent stuck button
    DisposableEffect(Unit) {
        onDispose {
            if (isAnimating) {
                viewModel.onCoinFlipAnimationComplete()
            }
            viewModel.setFling(false)
            viewModel.clearDisplayResult()
        }
    }
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

    val toastHeads = stringResource(R.string.toast_coin_heads)
    val toastTails = stringResource(R.string.toast_coin_tails)
    val toastMessage = coinResult?.let {
        if (it == CoinSide.HEADS) toastHeads else toastTails
    }

    LaunchedEffect(flipTrigger) {
        if (flipTrigger > 0) {
            toastMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
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
                    text = stringResource(R.string.coin_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            },
            actions = {
                IconButton(onClick = { showCustomizationSheet = true }) {
                    Icon(Icons.Default.Palette, contentDescription = stringResource(R.string.cd_customize_images))
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Coin3DFlip(
            result = coinResult,
            isAnimating = isAnimating,
            headsImage = headsBitmap,
            tailsImage = tailsBitmap,
            onDragFlipComplete = { viewModel.flipCoinDirectly(it) },
            onFlingChanged = { viewModel.setFling(it) },
            modifier = Modifier.padding(bottom = 24.dp),
            contentDescription = stringResource(R.string.cd_coin),
        )

        Button(
            onClick = { viewModel.flipCoin() },
            enabled = !isAnimating && !isFling,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isAnimating) stringResource(R.string.btn_flipping) else stringResource(R.string.btn_flip_coin),
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
