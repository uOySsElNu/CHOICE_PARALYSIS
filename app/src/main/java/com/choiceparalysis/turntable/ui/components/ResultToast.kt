package com.choiceparalysis.turntable.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ResultToast(
    result: String?,
    modifier: Modifier = Modifier,
    displayDurationMs: Long = 2500L,
    onDismiss: () -> Unit = {},
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = result != null

    LaunchedEffect(result) {
        if (result != null) {
            delay(displayDurationMs)
            visibleState.targetState = false
            delay(400) // Wait for exit animation
            onDismiss()
        }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(tween(300, easing = StandardEasing.EaseOutCubic)) +
                slideInVertically(
                    tween(400, easing = StandardEasing.EaseOutCubic)
                ) { it / 2 },
        exit = fadeOut(tween(300, easing = StandardEasing.EaseInCubic)) +
               slideOutVertically(
                   tween(300, easing = StandardEasing.EaseInCubic)
               ) { it / 2 }
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = result ?: "",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
