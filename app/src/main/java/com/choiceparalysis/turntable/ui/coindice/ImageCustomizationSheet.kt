package com.choiceparalysis.turntable.ui.coindice

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.choiceparalysis.turntable.viewmodel.CoinDiceViewModel
import com.choiceparalysis.turntable.viewmodel.CoinSide

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageCustomizationSheet(
    viewModel: CoinDiceViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val customCoinHeadsUri by viewModel.customCoinHeadsUri.collectAsState()
    val customCoinTailsUri by viewModel.customCoinTailsUri.collectAsState()
    val customDiceUris by viewModel.customDiceUris.collectAsState()

    var selectedSlot by remember { mutableStateOf<ImageSlot?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Some providers don't support persistable permissions
            }
            val uriString = selectedUri.toString()
            when (val slot = selectedSlot) {
                is ImageSlot.CoinHeads -> viewModel.setCustomCoinImage(CoinSide.HEADS, uriString)
                is ImageSlot.CoinTails -> viewModel.setCustomCoinImage(CoinSide.TAILS, uriString)
                is ImageSlot.DiceFace -> viewModel.setCustomDiceFace(slot.face, uriString)
                null -> {}
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "自定义图片",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Coin section
            Text(
                text = "硬币",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageSlotItem(
                    label = "正面",
                    uri = customCoinHeadsUri,
                    shape = ImageShape.Circle,
                    onClick = {
                        selectedSlot = ImageSlot.CoinHeads
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                ImageSlotItem(
                    label = "反面",
                    uri = customCoinTailsUri,
                    shape = ImageShape.Circle,
                    onClick = {
                        selectedSlot = ImageSlot.CoinTails
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dice section
            Text(
                text = "骰子",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (face in 1..6) {
                    ImageSlotItem(
                        label = "$face 点",
                        uri = customDiceUris[face],
                        shape = ImageShape.RoundedSquare,
                        onClick = {
                            selectedSlot = ImageSlot.DiceFace(face)
                            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reset button
            TextButton(
                onClick = { viewModel.clearAllImages() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("恢复默认")
            }
        }
    }
}

private sealed class ImageSlot {
    data object CoinHeads : ImageSlot()
    data object CoinTails : ImageSlot()
    data class DiceFace(val face: Int) : ImageSlot()
}

private enum class ImageShape { Circle, RoundedSquare }

@Composable
private fun ImageSlotItem(
    label: String,
    uri: String?,
    shape: ImageShape,
    onClick: () -> Unit,
) {
    val slotSize = 72.dp
    val clipShape = when (shape) {
        ImageShape.Circle -> CircleShape
        ImageShape.RoundedSquare -> RoundedCornerShape(12.dp)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(slotSize)
                .clip(clipShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    clipShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = uri),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize().clip(clipShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加图片",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
