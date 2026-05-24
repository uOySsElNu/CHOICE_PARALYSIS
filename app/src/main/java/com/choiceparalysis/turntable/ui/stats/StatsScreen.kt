package com.choiceparalysis.turntable.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choiceparalysis.turntable.data.model.DecisionMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val stats by viewModel.stats.collectAsState()
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(stats.totalCount) {
        if (stats.totalCount > 0) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(800))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "数据洞察",
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

        if (stats.totalCount == 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "还没有决策记录",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "去试试转盘吧！",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        // Overview section
        StatsCard(title = "决策总览") {
            StatsRow("总决策次数", "${stats.totalCount}")
            stats.mostUsedMethod?.let {
                StatsRow("最常用", "${it.displayName} (${stats.methodDistribution[it] ?: 0}次)")
            }
            stats.lastDecisionTime?.let {
                val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                StatsRow("最近决策", sdf.format(Date(it)))
            }
        }

        // Method distribution
        StatsCard(title = "决策方式占比") {
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
                            text = method.displayName,
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
        StatsCard(title = "热门结果 Top 5") {
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
                        text = "${count}次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Hourly distribution
        StatsCard(title = "决策时间分布") {
            val maxHourly = stats.hourlyDistribution.maxOrNull() ?: 1
            val textMeasurer = rememberTextMeasurer()
            val barColor = MaterialTheme.colorScheme.primary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
