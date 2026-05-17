package com.choiceparalysis.turntable.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private fun WheelDesign.toColorScheme() = WheelColorScheme(
    segmentColors = colors,
    borderColor = borderColor,
    textColor = textColor,
    indicatorColor = indicatorColor,
)

@Preview(
    name = "Classic Rainbow Wheel",
    showBackground = true,
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun PreviewClassicRainbowWheel() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Classic Rainbow",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SpinWheel(
                    options = listOf("选项1", "选项2", "选项3", "选项4", "选项5"),
                    weights = listOf(1, 1, 1, 1, 1),
                    colorScheme = WheelDesign.CLASSIC_RAINBOW.toColorScheme(),
                    modifier = Modifier.size(300.dp)
                )
            }
        }
    }
}

@Preview(
    name = "Monochrome Wheel",
    showBackground = true,
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun PreviewMonochromeWheel() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Monochrome",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SpinWheel(
                    options = listOf("吃火锅", "吃烧烤", "吃日料", "吃西餐"),
                    weights = listOf(1, 1, 1, 1),
                    colorScheme = WheelDesign.MONOCHROME.toColorScheme(),
                    modifier = Modifier.size(300.dp)
                )
            }
        }
    }
}

@Preview(
    name = "Warm Sunset Wheel",
    showBackground = true,
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun PreviewWarmSunsetWheel() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Warm Sunset",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SpinWheel(
                    options = listOf("看电影", "打游戏", "看书", "运动", "睡觉", "逛街"),
                    weights = listOf(1, 1, 1, 1, 1, 1),
                    colorScheme = WheelDesign.WARM_SUNSET.toColorScheme(),
                    modifier = Modifier.size(300.dp)
                )
            }
        }
    }
}

@Preview(
    name = "Ocean Breeze Wheel",
    showBackground = true,
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun PreviewOceanBreezeWheel() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ocean Breeze",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SpinWheel(
                    options = listOf("咖啡", "奶茶", "果汁", "可乐", "柠檬水"),
                    weights = listOf(1, 1, 1, 1, 1),
                    colorScheme = WheelDesign.OCEAN_BREEZE.toColorScheme(),
                    modifier = Modifier.size(300.dp)
                )
            }
        }
    }
}

@Preview(
    name = "Minimal Wheel",
    showBackground = true,
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun PreviewMinimalWheel() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Minimal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SpinWheel(
                    options = listOf("是", "否", "也许", "不知道"),
                    weights = listOf(1, 1, 1, 1),
                    colorScheme = WheelDesign.MINIMAL.toColorScheme(),
                    modifier = Modifier.size(300.dp)
                )
            }
        }
    }
}
