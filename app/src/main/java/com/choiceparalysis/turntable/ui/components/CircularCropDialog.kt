package com.choiceparalysis.turntable.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.rememberAsyncImagePainter

@Composable
fun CircularCropDialog(
    imageUri: Uri,
    onCropConfirmed: (croppedImagePath: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var cropCenterX by remember { mutableFloatStateOf(0.5f) }
    var cropCenterY by remember { mutableFloatStateOf(0.5f) }
    var cropRadius by remember { mutableFloatStateOf(0.4f) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "裁剪硬币图片",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Image with crop overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                // Pan to move crop center
                                cropCenterX = (cropCenterX + pan.x / size.width)
                                    .coerceIn(cropRadius, 1f - cropRadius)
                                cropCenterY = (cropCenterY + pan.y / size.height)
                                    .coerceIn(cropRadius, 1f - cropRadius)

                                // Pinch to resize crop radius
                                if (zoom != 1f) {
                                    val newRadius = (cropRadius * zoom).coerceIn(0.15f, 0.5f)
                                    cropRadius = newRadius
                                    // Re-clamp center after radius change
                                    cropCenterX = cropCenterX.coerceIn(cropRadius, 1f - cropRadius)
                                    cropCenterY = cropCenterY.coerceIn(cropRadius, 1f - cropRadius)
                                }
                            }
                        }
                ) {
                    // Draw the image
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Draw the crop overlay on top
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.99f)
                    ) {
                        val circleCenter = Offset(
                            cropCenterX * size.width,
                            cropCenterY * size.height
                        )
                        val circleRadius = cropRadius * minOf(size.width, size.height)

                        // Dark overlay with circular hole
                        val overlayPath = Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                            addOval(
                                Rect(
                                    center = circleCenter,
                                    radius = circleRadius
                                )
                            )
                            fillType = PathFillType.EvenOdd
                        }
                        drawPath(overlayPath, Color.Black.copy(alpha = 0.5f))

                        // Circle border
                        drawCircle(
                            color = Color.White,
                            radius = circleRadius,
                            center = circleCenter,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "拖动移动位置，双指缩放大小",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm / Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!isProcessing) {
                                isProcessing = true
                                val sourceBitmap = CircularCropUtil.loadBitmapFromUri(context, imageUri)
                                if (sourceBitmap != null) {
                                    val cropped = CircularCropUtil.cropToCircle(
                                        sourceBitmap, cropCenterX, cropCenterY, cropRadius
                                    )
                                    val path = CircularCropUtil.saveToInternalStorage(
                                        context, cropped, "coin_${System.currentTimeMillis()}.png"
                                    )
                                    sourceBitmap.recycle()
                                    onCropConfirmed(path)
                                } else {
                                    isProcessing = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Text(if (isProcessing) "处理中..." else "确认")
                    }
                }
            }
        }
    }
}
