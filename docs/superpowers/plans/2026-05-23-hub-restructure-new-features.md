# Hub Navigation Restructure + New Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure navigation from 5-tab to 3-tab (Hub/History/Settings), split Coin/Dice into independent screens, add haptic+sound system, finger roulette game, and stats dashboard.

**Architecture:** Hub-and-spoke pattern — `HubScreen` is a card grid launcher that navigates to independent feature screens. Bottom nav has 3 items: Hub, History, Settings. Each feature screen has its own ViewModel. `AudioHapticManager` singleton handles all sound and haptic feedback across the app.

**Tech Stack:** Jetpack Compose, Material 3, Navigation Compose, `SoundPool`, `HapticFeedback`, `Animatable`, Compose Canvas for charts, `awaitPointerEventScope` for multi-touch, DataStore Preferences, kotlinx-serialization

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `ui/hub/HubScreen.kt` | **Create** | Card grid launcher screen |
| `ui/coin/CoinScreen.kt` | **Create** | Standalone coin flip screen |
| `ui/dice/DiceScreen.kt` | **Create** | Standalone dice roll screen |
| `viewmodel/CoinViewModel.kt` | **Create** | Coin-specific state management |
| `viewmodel/DiceViewModel.kt` | **Create** | Dice-specific state management |
| `ui/fingerroulette/FingerRouletteScreen.kt` | **Create** | Finger roulette main screen |
| `ui/fingerroulette/FingerRouletteViewModel.kt` | **Create** | Finger roulette state management |
| `ui/stats/StatsScreen.kt` | **Create** | Stats dashboard screen |
| `ui/stats/StatsViewModel.kt` | **Create** | Stats data processing |
| `audio/AudioHapticManager.kt` | **Create** | Sound + haptic singleton |
| `navigation/AppDestinations.kt` | **Modify** | New routes, remove COIN_DICE |
| `navigation/AppNavGraph.kt` | **Modify** | New nav graph with hub start |
| `MainActivity.kt` | **Modify** | 3-tab bottom nav |
| `data/datastore/AppDataStore.kt` | **Modify** | Add sound/haptic keys |
| `data/repository/SettingsRepository.kt` | **Modify** | Add sound/haptic flows |
| `ui/settings/SettingsScreen.kt` | **Modify** | Add sound/haptic toggles |
| `ui/coindice/ImageCustomizationSheet.kt` | **Move** | → `ui/components/ImageCustomizationSheet.kt` |
| `ui/coindice/CircularCropDialog.kt` | **Move** | → `ui/components/CircularCropDialog.kt` |
| `ui/coindice/CircularCropUtil.kt` | **Move** | → `ui/components/CircularCropUtil.kt` |

---

## Task 1: Navigation Restructure — Hub Screen + Routes

**Files:**
- Modify: `navigation/AppDestinations.kt`
- Modify: `navigation/AppNavGraph.kt`
- Modify: `MainActivity.kt`
- Create: `ui/hub/HubScreen.kt`

### Step 1: Rewrite AppDestinations.kt

Replace the entire enum with 3 bottom nav items + feature routes as string constants:

```kotlin
package com.choiceparalysis.turntable.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavDestinations(
    val label: String,
    val icon: ImageVector,
    val route: String,
) {
    HUB("首页", Icons.Default.Home, "hub"),
    HISTORY("历史记录", Icons.Default.History, "history"),
    SETTINGS("设置", Icons.Default.Settings, "settings"),
}

object Routes {
    const val HUB = "hub"
    const val SPIN_WHEEL = "spin_wheel"
    const val COIN = "coin"
    const val DICE = "dice"
    const val YES_NO = "yes_no"
    const val FINGER_ROULETTE = "finger_roulette"
    const val STATS = "stats"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}
```

### Step 2: Create HubScreen.kt

```kotlin
package com.choiceparalysis.turntable.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ThumbsUpDown
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.navigation.Routes

data class HubCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
)

private val hubCards = listOf(
    HubCard("转盘", "转动命运之轮", Icons.Default.Autorenew, Routes.SPIN_WHEEL),
    HubCard("硬币", "抛一枚命运硬币", Icons.Default.Casino, Routes.COIN),
    HubCard("骰子", "掷出你的答案", Icons.Default.Casino, Routes.DICE),
    HubCard("Yes/No", "让宇宙替你决定", Icons.Default.ThumbsUpDown, Routes.YES_NO),
    HubCard("指尖轮盘", "多人淘汰，谁是天选", Icons.Default.TouchApp, Routes.FINGER_ROULETTE),
    HubCard("数据洞察", "看看你的决策模式", Icons.Default.BarChart, Routes.STATS),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "选择困难症助手",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(hubCards) { card ->
                Card(
                    onClick = { onNavigate(card.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = card.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
```

### Step 3: Rewrite AppNavGraph.kt

```kotlin
package com.choiceparalysis.turntable.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.choiceparalysis.turntable.ui.coin.CoinScreen
import com.choiceparalysis.turntable.ui.components.StandardEasing
import com.choiceparalysis.turntable.ui.dice.DiceScreen
import com.choiceparalysis.turntable.ui.fingerroulette.FingerRouletteScreen
import com.choiceparalysis.turntable.ui.history.HistoryScreen
import com.choiceparalysis.turntable.ui.home.SpinWheelScreen
import com.choiceparalysis.turntable.ui.hub.HubScreen
import com.choiceparalysis.turntable.ui.settings.SettingsScreen
import com.choiceparalysis.turntable.ui.stats.StatsScreen
import com.choiceparalysis.turntable.ui.yesno.YesNoScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HUB,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)) +
                scaleIn(initialScale = 0.5f, animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)) +
                scaleOut(targetScale = 1.5f, animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)) +
                scaleIn(initialScale = 0.5f, animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)) +
                scaleOut(targetScale = 1.5f, animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart))
        },
    ) {
        composable(Routes.HUB) {
            HubScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        composable(Routes.SPIN_WHEEL) {
            SpinWheelScreen()
        }
        composable(Routes.COIN) {
            CoinScreen()
        }
        composable(Routes.DICE) {
            DiceScreen()
        }
        composable(Routes.YES_NO) {
            YesNoScreen()
        }
        composable(Routes.FINGER_ROULETTE) {
            FingerRouletteScreen()
        }
        composable(Routes.STATS) {
            StatsScreen()
        }
        composable(Routes.HISTORY) {
            HistoryScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
```

### Step 4: Rewrite MainActivity.kt bottom nav

Replace the `ChoiceParalysisApp` composable. The `BottomNavDestinations` enum replaces `AppDestinations` for the nav bar. Default start route changes from `SPIN_WHEEL` to `HUB`.

```kotlin
package com.choiceparalysis.turntable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import com.choiceparalysis.turntable.navigation.AppNavGraph
import com.choiceparalysis.turntable.navigation.BottomNavDestinations
import com.choiceparalysis.turntable.ui.theme.CHOICEPARALYSISTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val followSystem by SettingsRepository(context).followSystemTheme
                .collectAsState(initial = true)
            val darkTheme = if (followSystem) isSystemInDarkTheme() else false
            CHOICEPARALYSISTheme(darkTheme = darkTheme) {
                ChoiceParalysisApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun ChoiceParalysisApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavDestinations.HUB.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            BottomNavDestinations.entries.forEach { dest ->
                item(
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                    selected = currentRoute == dest.route,
                    onClick = {
                        if (currentRoute != dest.route) {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            AppNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
```

### Step 5: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

### Step 6: Commit

```bash
git add -A
git commit -m "feat: restructure navigation to hub-and-spoke with 3-tab bottom nav"
```

---

## Task 2: Split Coin/Dice into Independent Screens

**Files:**
- Create: `viewmodel/CoinViewModel.kt`
- Create: `viewmodel/DiceViewModel.kt`
- Create: `ui/coin/CoinScreen.kt`
- Create: `ui/dice/DiceScreen.kt`
- Move: `ui/coindice/ImageCustomizationSheet.kt` → `ui/components/ImageCustomizationSheet.kt`
- Move: `ui/coindice/CircularCropDialog.kt` → `ui/components/CircularCropDialog.kt`
- Move: `ui/coindice/CircularCropUtil.kt` → `ui/components/CircularCropUtil.kt`

### Step 1: Create CoinViewModel.kt

```kotlin
package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.CoinPreset
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository.Companion.DEFAULT_PRESET_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CoinViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _coinResult = MutableStateFlow<CoinSide?>(null)
    val coinResult: StateFlow<CoinSide?> = _coinResult.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _pendingCoinResult = MutableStateFlow<CoinSide?>(null)
    val pendingCoinResult: StateFlow<CoinSide?> = _pendingCoinResult.asStateFlow()

    private val _customCoinHeadsUri = MutableStateFlow<String?>(null)
    val customCoinHeadsUri: StateFlow<String?> = _customCoinHeadsUri.asStateFlow()

    private val _customCoinTailsUri = MutableStateFlow<String?>(null)
    val customCoinTailsUri: StateFlow<String?> = _customCoinTailsUri.asStateFlow()

    private val _coinPresets = MutableStateFlow<List<CoinPreset>>(emptyList())
    val coinPresets: StateFlow<List<CoinPreset>> = _coinPresets.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.coinHeadsImage.collect { _customCoinHeadsUri.value = it }
        }
        viewModelScope.launch {
            settingsRepository.coinTailsImage.collect { _customCoinTailsUri.value = it }
        }
        viewModelScope.launch {
            settingsRepository.coinPresets.collect { _coinPresets.value = it }
        }
        viewModelScope.launch {
            settingsRepository.ensureDefaultCoinPreset()
        }
    }

    fun flipCoin() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _coinResult.value = null
        _pendingCoinResult.value = if (Math.random() < 0.5) CoinSide.HEADS else CoinSide.TAILS
    }

    fun onCoinFlipAnimationComplete() {
        val result = _pendingCoinResult.value ?: return
        _coinResult.value = result
        _isAnimating.value = false
        viewModelScope.launch {
            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.COIN_FLIP,
                    options = listOf("正面", "反面"),
                    result = result.displayName,
                )
            )
        }
    }

    fun flipCoinDirectly(result: CoinSide) {
        _pendingCoinResult.value = result
        _coinResult.value = result
        viewModelScope.launch {
            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.COIN_FLIP,
                    options = listOf("正面", "反面"),
                    result = result.displayName,
                )
            )
        }
    }

    fun clearResult() {
        _coinResult.value = null
        _isAnimating.value = false
    }

    fun setCustomCoinImage(side: CoinSide, uri: String?) {
        viewModelScope.launch {
            when (side) {
                CoinSide.HEADS -> settingsRepository.setCoinHeadsImage(uri)
                CoinSide.TAILS -> settingsRepository.setCoinTailsImage(uri)
            }
        }
    }

    fun clearAllImages() {
        viewModelScope.launch {
            settingsRepository.clearAllCustomImages()
        }
    }

    fun saveCoinPreset(name: String) {
        viewModelScope.launch {
            val headsUri = _customCoinHeadsUri.value ?: return@launch
            val tailsUri = _customCoinTailsUri.value ?: return@launch
            settingsRepository.saveCoinPreset(
                CoinPreset(name = name, headsImagePath = headsUri, tailsImagePath = tailsUri)
            )
        }
    }

    fun deleteCoinPreset(id: String) {
        if (id == DEFAULT_PRESET_ID) return
        viewModelScope.launch {
            settingsRepository.deleteCoinPreset(id)
        }
    }

    fun loadCoinPreset(preset: CoinPreset) {
        viewModelScope.launch {
            settingsRepository.setCoinHeadsImage(preset.headsImagePath)
            settingsRepository.setCoinTailsImage(preset.tailsImagePath)
        }
    }
}
```

### Step 2: Create DiceViewModel.kt

```kotlin
package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiceViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepository(application)

    private val _diceValue = MutableStateFlow<Int?>(null)
    val diceValue: StateFlow<Int?> = _diceValue.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _pendingDiceValue = MutableStateFlow<Int?>(null)

    fun rollDice() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _diceValue.value = null
        _pendingDiceValue.value = (1..6).random()
    }

    fun onDiceRollAnimationComplete() {
        val result = _pendingDiceValue.value ?: return
        _diceValue.value = result
        _isAnimating.value = false
        viewModelScope.launch {
            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.DICE_ROLL,
                    options = listOf("1", "2", "3", "4", "5", "6"),
                    result = result.toString(),
                )
            )
        }
    }

    fun clearResult() {
        _diceValue.value = null
        _isAnimating.value = false
    }
}
```

### Step 3: Create CoinScreen.kt

```kotlin
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
```

### Step 4: Create DiceScreen.kt

```kotlin
package com.choiceparalysis.turntable.ui.dice

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choiceparalysis.turntable.ui.coindice.Dice3DRoll
import com.choiceparalysis.turntable.ui.coindice.ShakeDetector
import com.choiceparalysis.turntable.viewmodel.DiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceScreen(
    modifier: Modifier = Modifier,
    viewModel: DiceViewModel = viewModel(),
) {
    val diceValue by viewModel.diceValue.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val context = LocalContext.current

    DisposableEffect(isAnimating) {
        val shakeDetector = ShakeDetector(context) {
            if (!isAnimating) {
                viewModel.rollDice()
            }
        }
        shakeDetector.start()
        onDispose { shakeDetector.stop() }
    }

    val toastMessage = diceValue?.let { "点数: $it" }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
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
                    text = "掷骰子",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

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
```

### Step 5: Move shared composables

Move these files from `ui/coindice/` to `ui/components/`:
- `ImageCustomizationSheet.kt`
- `CircularCropDialog.kt`
- `CircularCropUtil.kt`

Update their `package` declarations from `com.choiceparalysis.turntable.ui.coindice` to `com.choiceparalysis.turntable.ui.components`.

Update all import references in:
- `CoinScreen.kt` (already correct in step 3)
- `CoinViewModel.kt` — update `CircularCropUtil` import in `SettingsRepository.kt` reference
- `SettingsRepository.kt` — update `CircularCropUtil` import

### Step 6: Move CoinSide enum and delete old files

The `CoinSide` enum is currently in `CoinDiceViewModel.kt`. Move it to `CoinViewModel.kt` (already done above). Then delete:

```bash
rm app/src/main/java/com/choiceparalysis/turntable/ui/coindice/CoinDiceScreen.kt
rm app/src/main/java/com/choiceparalysis/turntable/viewmodel/CoinDiceViewModel.kt
```

### Step 7: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

### Step 8: Commit

```bash
git add -A
git commit -m "feat: split CoinDice into independent CoinScreen and DiceScreen"
```

---

## Task 3: AudioHapticManager — Sound + Haptic System

**Files:**
- Create: `audio/AudioHapticManager.kt`
- Modify: `data/datastore/AppDataStore.kt`
- Modify: `data/repository/SettingsRepository.kt`
- Modify: `ui/settings/SettingsScreen.kt`

### Step 1: Add DataStore keys for sound/haptic

In `AppDataStore.kt`, add to `DataStoreKeys`:

```kotlin
val SOUND_ENABLED = stringPreferencesKey("sound_enabled")
val HAPTIC_ENABLED = stringPreferencesKey("haptic_enabled")
```

### Step 2: Add sound/haptic flows to SettingsRepository

Add after `followSystemTheme` flow:

```kotlin
val soundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[DataStoreKeys.SOUND_ENABLED]?.toBooleanStrictOrNull() ?: true
}

val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[DataStoreKeys.HAPTIC_ENABLED]?.toBooleanStrictOrNull() ?: true
}

suspend fun setSoundEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.SOUND_ENABLED] = enabled.toString()
    }
}

suspend fun setHapticEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.HAPTIC_ENABLED] = enabled.toString()
    }
}
```

### Step 3: Create AudioHapticManager.kt

```kotlin
package com.choiceparalysis.turntable.audio

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SoundEffect(val resId: Int) {
    SPIN_DING(R.raw.spin_ding),
    COIN_CLINK(R.raw.coin_clink),
    DICE_TAP(R.raw.dice_tap),
    YESNO_CHIME(R.raw.yesno_chime),
    ELIMINATION_DRUM(R.raw.elimination_drum),
    WINNER_CHEER(R.raw.winner_cheer),
}

enum class HapticType {
    SPIN_TICK,
    RESULT_HIT,
    COIN_FLIP,
    DICE_BOUNCE,
    ELIMINATION,
    WINNER,
}

class AudioHapticManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: AudioHapticManager? = null

        fun getInstance(context: Context): AudioHapticManager {
            return instance ?: synchronized(this) {
                instance ?: AudioHapticManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val settingsRepository = SettingsRepository(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .build()

    private val soundMap = mutableMapOf<SoundEffect, Int>()
    private var loaded = false

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        scope.launch {
            settingsRepository.soundEnabled.collect { _soundEnabled.value = it }
        }
        scope.launch {
            settingsRepository.hapticEnabled.collect { _hapticEnabled.value = it }
        }
    }

    fun loadSounds() {
        if (loaded) return
        SoundEffect.entries.forEach { effect ->
            soundMap[effect] = soundPool.load(context, effect.resId, 1)
        }
        loaded = true
    }

    fun playSound(effect: SoundEffect) {
        if (!_soundEnabled.value) return
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        if (!loaded) loadSounds()
        val soundId = soundMap[effect] ?: return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun performHaptic(type: HapticType, view: View? = null) {
        if (!_hapticEnabled.value) return
        when (type) {
            HapticType.SPIN_TICK -> {
                view?.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                    ?: vibrateTick()
            }
            HapticType.RESULT_HIT -> {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    ?: vibrateHeavy()
            }
            HapticType.COIN_FLIP -> {
                view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    ?: vibrateMedium()
            }
            HapticType.DICE_BOUNCE -> {
                view?.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                    ?: vibrateTick()
            }
            HapticType.ELIMINATION -> {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    ?: vibrateHeavy()
            }
            HapticType.WINNER -> {
                vibratePattern(longArrayOf(0, 100, 50, 100, 50, 200))
            }
        }
    }

    private fun vibrateTick() {
        vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateMedium() {
        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateHeavy() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibratePattern(pattern: LongArray) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun release() {
        soundPool.release()
    }
}
```

### Step 4: Add sound/haptic toggles to SettingsScreen.kt

In the "显示" section, add a new Card for "音效与震动" before the existing "显示" Card. Add after `followSystemTheme` declaration:

```kotlin
val soundEnabled by settingsRepository.soundEnabled.collectAsState(initial = true)
val hapticEnabled by settingsRepository.hapticEnabled.collectAsState(initial = true)
```

Add a new section after "显示":

```kotlin
Text(
    text = "音效与震动",
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
)

Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) {
    Column {
        ListItem(
            headlineContent = { Text("音效") },
            supportingContent = { Text("决策时播放音效") },
            trailingContent = {
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { value ->
                        scope.launch { settingsRepository.setSoundEnabled(value) }
                    }
                )
            }
        )
        ListItem(
            headlineContent = { Text("震动") },
            supportingContent = { Text("决策时触觉反馈") },
            trailingContent = {
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = { value ->
                        scope.launch { settingsRepository.setHapticEnabled(value) }
                    }
                )
            }
        )
    }
}

Spacer(modifier = Modifier.height(16.dp))
```

### Step 5: Add placeholder audio files

Create empty/stub audio files in `app/src/main/res/raw/`:
- `spin_ding.mp3`
- `coin_clink.mp3`
- `dice_tap.mp3`
- `yesno_chime.mp3`
- `elimination_drum.mp3`
- `winner_cheer.mp3`

These are placeholder files. Replace with real audio before release. For now, use a minimal valid MP3 file (or silence).

### Step 6: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

### Step 7: Commit

```bash
git add -A
git commit -m "feat: add AudioHapticManager with sound/haptic system and settings toggles"
```

---

## Task 4: Integrate Haptic + Sound into Existing Features

**Files:**
- Modify: `ui/home/SpinWheelScreen.kt` or `ui/home/SpinWheelComposable.kt`
- Modify: `ui/coin/CoinScreen.kt`
- Modify: `ui/dice/DiceScreen.kt`
- Modify: `ui/yesno/YesNoScreen.kt`

### Step 1: Integrate into SpinWheelComposable.kt

Add `AudioHapticManager` parameter to the `SpinWheel` composable. At the top of the composable, get the instance:

```kotlin
val audioHaptic = AudioHapticManager.getInstance(LocalContext.current)
```

In the button spin callback, after animation completes:
```kotlin
audioHaptic.playSound(SoundEffect.SPIN_DING)
audioHaptic.performHaptic(HapticType.RESULT_HIT)
```

In the drag spin callback, after decay animation completes:
```kotlin
audioHaptic.playSound(SoundEffect.SPIN_DING)
audioHaptic.performHaptic(HapticType.RESULT_HIT)
```

During drag, in the `onDrag` callback (throttled to every 100ms):
```kotlin
audioHaptic.performHaptic(HapticType.SPIN_TICK)
```

### Step 2: Integrate into CoinScreen.kt

In the `LaunchedEffect(toastMessage)` block, before showing toast:
```kotlin
val audioHaptic = AudioHapticManager.getInstance(context)
audioHaptic.playSound(SoundEffect.COIN_CLINK)
audioHaptic.performHaptic(HapticType.COIN_FLIP)
```

### Step 3: Integrate into DiceScreen.kt

In the `LaunchedEffect(toastMessage)` block, before showing toast:
```kotlin
val audioHaptic = AudioHapticManager.getInstance(context)
audioHaptic.playSound(SoundEffect.DICE_TAP)
audioHaptic.performHaptic(HapticType.DICE_BOUNCE)
```

### Step 4: Integrate into YesNoScreen.kt

In the result display callback, when result appears:
```kotlin
val audioHaptic = AudioHapticManager.getInstance(context)
audioHaptic.playSound(SoundEffect.YESNO_CHIME)
audioHaptic.performHaptic(HapticType.RESULT_HIT)
```

### Step 5: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

### Step 6: Commit

```bash
git add -A
git commit -m "feat: integrate haptic feedback and sound effects into all decision methods"
```

---

## Task 5: Finger Roulette

**Files:**
- Create: `ui/fingerroulette/FingerRouletteScreen.kt`
- Create: `ui/fingerroulette/FingerRouletteViewModel.kt`

### Step 1: Create FingerRouletteViewModel.kt

```kotlin
package com.choiceparalysis.turntable.ui.fingerroulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FingerInfo(
    val id: Int,
    val x: Float,
    val y: Float,
    val isEliminated: Boolean = false,
    val isCurrentlyTargeted: Boolean = false,
)

enum class RoulettePhase {
    WAITING,    // Waiting for fingers
    READY,      // Enough fingers placed, can start
    PLAYING,    // Elimination in progress
    FINISHED,   // Winner determined
}

class FingerRouletteViewModel : ViewModel() {

    private val _fingers = MutableStateFlow<List<FingerInfo>>(emptyList())
    val fingers: StateFlow<List<FingerInfo>> = _fingers.asStateFlow()

    private val _phase = MutableStateFlow(RoulettePhase.WAITING)
    val phase: StateFlow<RoulettePhase> = _phase.asStateFlow()

    private val _winnerId = MutableStateFlow<Int?>(null)
    val winnerId: StateFlow<Int?> = _winnerId.asStateFlow()

    private var nextId = 0

    fun addFinger(x: Float, y: Float): Int {
        val id = nextId++
        val current = _fingers.value.toMutableList()
        current.add(FingerInfo(id = id, x = x, y = y))
        _fingers.value = current
        updatePhase()
        return id
    }

    fun updateFingerPosition(id: Int, x: Float, y: Float) {
        val current = _fingers.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(x = x, y = y)
            _fingers.value = current
        }
    }

    fun removeFinger(id: Int) {
        val current = _fingers.value.toMutableList()
        current.removeAll { it.id == id }
        _fingers.value = current
        updatePhase()
    }

    private fun updatePhase() {
        val activeFingers = _fingers.value.filter { !it.isEliminated }
        _phase.value = when {
            _winnerId.value != null -> RoulettePhase.FINISHED
            activeFingers.size < 2 -> RoulettePhase.WAITING
            else -> RoulettePhase.READY
        }
    }

    fun startElimination() {
        if (_phase.value != RoulettePhase.READY) return
        _phase.value = RoulettePhase.PLAYING
        viewModelScope.launch {
            runEliminationLoop()
        }
    }

    private suspend fun runEliminationLoop() {
        var delayMs = 1500L
        while (true) {
            val activeFingers = _fingers.value.filter { !it.isEliminated }
            if (activeFingers.size <= 1) {
                // Winner!
                if (activeFingers.size == 1) {
                    _winnerId.value = activeFingers[0].id
                }
                _phase.value = RoulettePhase.FINISHED
                return
            }

            // Pick random target
            val target = activeFingers.random()
            // Highlight target
            val current = _fingers.value.toMutableList()
            val index = current.indexOfFirst { it.id == target.id }
            if (index >= 0) {
                current[index] = current[index].copy(isCurrentlyTargeted = true)
                _fingers.value = current
            }

            delay(800) // Flash duration

            // Eliminate target
            val updated = _fingers.value.toMutableList()
            val idx = updated.indexOfFirst { it.id == target.id }
            if (idx >= 0) {
                updated[idx] = updated[idx].copy(isEliminated = true, isCurrentlyTargeted = false)
                _fingers.value = updated
            }

            delayMs = (delayMs * 0.85).toLong().coerceAtLeast(400L)
            delay(delayMs - 800) // Remaining delay after flash
        }
    }

    fun reset() {
        _fingers.value = emptyList()
        _phase.value = RoulettePhase.WAITING
        _winnerId.value = null
        nextId = 0
    }
}
```

### Step 2: Create FingerRouletteScreen.kt

```kotlin
package com.choiceparalysis.turntable.ui.fingerroulette

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerRouletteScreen(
    modifier: Modifier = Modifier,
    viewModel: FingerRouletteViewModel = viewModel(),
) {
    val fingers by viewModel.fingers.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val winnerId by viewModel.winnerId.collectAsState()

    val markerRadius = with(LocalDensity.current) { 36.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()

    // Winner animation
    val winnerScale = remember { Animatable(1f) }
    LaunchedEffect(winnerId) {
        if (winnerId != null) {
            winnerScale.animateTo(2f, tween(600))
            winnerScale.animateTo(1.5f, tween(300))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "指尖轮盘",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(phase) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        val fingerId = viewModel.addFinger(
                            down.position.x,
                            down.position.y
                        )
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.find { it.id == pointerId }
                                if (pointer == null || !pointer.pressed) {
                                    viewModel.removeFinger(fingerId)
                                    break
                                } else {
                                    viewModel.updateFingerPosition(
                                        fingerId,
                                        pointer.position.x,
                                        pointer.position.y
                                    )
                                    pointer.consume()
                                }
                            }
                        } catch (_: Exception) {
                            viewModel.removeFinger(fingerId)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                fingers.forEach { finger ->
                    if (finger.isEliminated) return@forEach
                    val scale = if (finger.id == winnerId) winnerScale.value else 1f
                    val radius = markerRadius * scale
                    val color = when {
                        finger.id == winnerId -> Color(0xFFFFD700) // Gold
                        finger.isCurrentlyTargeted -> Color.Red
                        else -> MaterialTheme.colorScheme.primary
                    }

                    // Draw circle
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(finger.x, finger.y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        center = Offset(finger.x, finger.y),
                        style = Stroke(width = 3f)
                    )

                    // Draw number
                    val number = fingers.indexOf(finger) + 1
                    val textResult = textMeasurer.measure(
                        text = number.toString(),
                        style = TextStyle(
                            fontSize = (16 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    drawText(
                        textResult,
                        topLeft = Offset(
                            finger.x - textResult.size.width / 2,
                            finger.y - textResult.size.height / 2
                        )
                    )
                }
            }

            // Winner overlay
            AnimatedVisibility(
                visible = phase == RoulettePhase.FINISHED,
                enter = fadeIn() + scaleIn(initialScale = 0.5f),
                exit = fadeOut() + scaleOut(targetScale = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "天选之人！",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.reset() }) {
                        Text("再来一局")
                    }
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val activeCount = fingers.count { !it.isEliminated }
            val statusText = when (phase) {
                RoulettePhase.WAITING -> "请放置 2-6 根手指"
                RoulettePhase.READY -> "检测到 $activeCount 根手指"
                RoulettePhase.PLAYING -> "淘汰中... 剩余 $activeCount 人"
                RoulettePhase.FINISHED -> "游戏结束"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.startElimination() },
                enabled = phase == RoulettePhase.READY
            ) {
                Text("开始淘汰")
            }
        }
    }
}
```

### Step 3: Add FINGER_ROULETTE to DecisionMethod

In `DecisionMethod.kt`, add:

```kotlin
FINGER_ROULETTE("指尖轮盘"),
```

### Step 4: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

### Step 5: Commit

```bash
git add -A
git commit -m "feat: add finger roulette multi-touch elimination game"
```

---

## Task 6: Stats Dashboard

**Files:**
- Create: `ui/stats/StatsScreen.kt`
- Create: `ui/stats/StatsViewModel.kt`

### Step 1: Create StatsViewModel.kt

```kotlin
package com.choiceparalysis.turntable.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class StatsState(
    val totalCount: Int = 0,
    val methodDistribution: Map<DecisionMethod, Int> = emptyMap(),
    val topResults: List<Pair<String, Int>> = emptyList(),
    val hourlyDistribution: List<Int> = List(24) { 0 },
    val mostUsedMethod: DecisionMethod? = null,
    val lastDecisionTime: Long? = null,
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepository(application)

    private val _stats = MutableStateFlow(StatsState())
    val stats: StateFlow<StatsState> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.history.collect { entries ->
                _stats.value = computeStats(entries)
            }
        }
    }

    private fun computeStats(entries: List<HistoryEntry>): StatsState {
        if (entries.isEmpty()) return StatsState()

        val methodDistribution = entries.groupBy { it.method }.mapValues { it.value.size }
        val mostUsed = methodDistribution.maxByOrNull { it.value }?.key

        val topResults = entries
            .groupBy { it.result }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val hourlyDistribution = MutableList(24) { 0 }
        val calendar = Calendar.getInstance()
        entries.forEach { entry ->
            calendar.timeInMillis = entry.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyDistribution[hour]++
        }

        return StatsState(
            totalCount = entries.size,
            methodDistribution = methodDistribution,
            topResults = topResults,
            hourlyDistribution = hourlyDistribution,
            mostUsedMethod = mostUsed,
            lastDecisionTime = entries.firstOrNull()?.timestamp
        )
    }
}
```

### Step 2: Create StatsScreen.kt

```kotlin
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
            }
        )

        if (stats.totalCount == 0) {
            // Empty state
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

                    // Hour label (show every 6 hours)
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
```

### Step 3: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

### Step 4: Commit

```bash
git add -A
git commit -m "feat: add stats dashboard with method distribution, top results, and hourly chart"
```

---

## Task 7: Polish and Integration Fixes

### Step 1: Update version in SettingsScreen.kt

Change version from "1.3.0" to "1.5.0" in the Settings screen.

### Step 2: Update MEMORY.md

Update the project overview memory to reflect the new architecture and features.

### Step 3: Final verification

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

### Step 4: Final commit

```bash
git add -A
git commit -m "chore: bump version to 1.5.0 and polish integration"
```
