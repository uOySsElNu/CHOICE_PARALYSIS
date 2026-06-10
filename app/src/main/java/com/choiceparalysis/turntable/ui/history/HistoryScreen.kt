package com.choiceparalysis.turntable.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.choiceparalysis.turntable.ui.components.StatsCard
import com.choiceparalysis.turntable.ui.components.StatsRow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.ui.stats.StatsViewModel
import com.choiceparalysis.turntable.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
    statsViewModel: StatsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val history by viewModel.history.collectAsState()
    val stats by statsViewModel.stats.collectAsState()
    var showStats by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<String?>(null) }
    val animationProgress = remember { Animatable(0f) }
    val context = LocalContext.current

    LaunchedEffect(stats.totalCount, showStats) {
        if (stats.totalCount > 0 && showStats) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(800))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = { showClearConfirm = true }
                ) {
                    Text(stringResource(R.string.history_clear))
                }
            }
        }

        // Stats toggle
        if (stats.totalCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStats = !showStats }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_data_insights),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (showStats) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_expand_collapse),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = showStats,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    // Overview
                    StatsCard(title = stringResource(R.string.stats_decision_overview)) {
                        StatsRow(stringResource(R.string.stats_total_decisions), "${stats.totalCount}")
                        stats.mostUsedMethod?.let {
                            StatsRow(stringResource(R.string.stats_most_used), "${stringResource(it.displayNameRes)} (${stringResource(R.string.stats_count_suffix, stats.methodDistribution[it] ?: 0)})")
                        }
                        stats.lastDecisionTime?.let {
                            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                            StatsRow(stringResource(R.string.stats_last_decision), sdf.format(Date(it)))
                        }
                    }

                    // Method distribution
                    StatsCard(title = stringResource(R.string.stats_method_distribution)) {
                        val maxCount = stats.methodDistribution.values.maxOrNull() ?: 1
                        DecisionMethod.entries.forEach { method ->
                            val count = stats.methodDistribution[method] ?: 0
                            if (count > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(method.displayNameRes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(72.dp)
                                    )
                                    LinearProgressIndicator(
                                        progress = { (count.toFloat() / maxCount) * animationProgress.value },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(16.dp),
                                    )
                                    Text(
                                        text = "${(count * 100 / stats.totalCount)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Top 5 results
                    StatsCard(title = stringResource(R.string.stats_top_results)) {
                        stats.topResults.forEachIndexed { index, (result, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = stringResource(R.string.stats_count_suffix, count),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Hourly distribution
                    StatsCard(title = stringResource(R.string.stats_hourly_distribution)) {
                        val maxHourly = stats.hourlyDistribution.maxOrNull() ?: 1
                        val textMeasurer = rememberTextMeasurer()
                        val barColor = MaterialTheme.colorScheme.primary
                        val hourlyChartDesc = stringResource(R.string.cd_hourly_chart)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .semantics {
                                    contentDescription = hourlyChartDesc
                                }
                        ) {
                            val barWidth = size.width / 28f
                            val barSpacing = size.width / 24f
                            val bottomPadding = 20f

                            stats.hourlyDistribution.forEachIndexed { hour, count ->
                                val barHeight = (count.toFloat() / maxHourly) * (size.height - bottomPadding) * animationProgress.value
                                val x = hour * barSpacing + barSpacing / 2 - barWidth / 2

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, size.height - bottomPadding - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )

                                if (hour % 6 == 0) {
                                    val textResult = textMeasurer.measure(
                                        text = "$hour",
                                        style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                                    )
                                    drawText(
                                        textResult,
                                        topLeft = Offset(
                                            hour * barSpacing + barSpacing / 2 - textResult.size.width / 2,
                                            size.height - bottomPadding + 4
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // History list
        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📝",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.semantics {
                        contentDescription = context.getString(R.string.cd_no_history_icon)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.history_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(R.string.history_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(entry.method.displayNameRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                                            .format(Date(entry.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.history_result_prefix, entry.result),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (entry.options.size <= 5) {
                                    Text(
                                        text = entry.options.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { entryToDelete = entry.id }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.cd_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear History confirmation dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.history_clear)) },
            text = { Text(stringResource(R.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearConfirm = false
                }) {
                    Text(stringResource(R.string.btn_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Delete entry confirmation dialog
    entryToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text(stringResource(R.string.history_delete_entry)) },
            text = { Text(stringResource(R.string.history_delete_entry_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(id)
                    entryToDelete = null
                }) {
                    Text(stringResource(R.string.btn_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}


