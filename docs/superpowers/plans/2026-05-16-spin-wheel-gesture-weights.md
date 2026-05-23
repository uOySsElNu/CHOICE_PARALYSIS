# Spin Wheel Gesture + Weights Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite spin wheel to support finger-drag rotation with flick-to-spin momentum, result-drag detection with toast, and per-option weight-based segment sizing with visual sliders.

**Architecture:** Single source of truth for rotation lives in `SpinWheelComposable` via `Animatable`. The composable handles all gesture logic (drag, flick, button) and reports results to the ViewModel via callbacks. Weights are stored in the ViewModel, persisted via DataStore, and used to compute proportional segment angles in the drawing code.

**Tech Stack:** Jetpack Compose Canvas, `Animatable` with `exponentialDecay`, `detectDragGestures`, DataStore Preferences, kotlinx-serialization

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `SpinWheelViewModel.kt` | Modify | Add weights state, update spin/result logic to be callback-driven, remove dead `spinWithVelocity` |
| `SpinWheelComposable.kt` | **Rewrite** | Clean animation model: `rememberCoroutineScope` for direct animation control from gesture callbacks, weight-aware drawing |
| `SpinWheelScreen.kt` | Modify | Wire up weights UI, weight sliders in OptionsEditorPanel |
| `AppDataStore.kt` | Modify | Add `OPTION_WEIGHTS` key |
| `SettingsRepository.kt` | Modify | Add `currentWeights` flow and `setCurrentWeights` |
| `OptionGroup.kt` | Modify | Add `weights: List<Int>` field |

---

## Task 1: Add Weights to Data Layer

**Files:**
- Modify: `AppDataStore.kt`
- Modify: `SettingsRepository.kt`
- Modify: `OptionGroup.kt`

### Step 1: Add DataStore key

In `AppDataStore.kt`, add to `DataStoreKeys`:
```kotlin
val OPTION_WEIGHTS = stringPreferencesKey("option_weights")
```

### Step 2: Add weights flow and setter to SettingsRepository

Add after `currentOptions` flow:
```kotlin
val currentWeights: Flow<List<Int>> = context.dataStore.data.map { prefs ->
    val json = prefs[DataStoreKeys.OPTION_WEIGHTS] ?: "[1,1]"
    runCatching { Json.decodeFromString<List<Int>>(json) }.getOrElse { listOf(1, 1) }
}

suspend fun setCurrentWeights(weights: List<Int>) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.OPTION_WEIGHTS] = Json.encodeToString(weights)
    }
}
```

### Step 3: Add weights to OptionGroup

```kotlin
@Serializable
data class OptionGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val options: List<String>,
    val weights: List<Int> = options.map { 1 },
    val createdAt: Long = System.currentTimeMillis(),
)
```

### Step 4: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

---

## Task 2: Add Weights to ViewModel

**Files:**
- Modify: `SpinWheelViewModel.kt`

### Step 1: Add weights state

After `_options` declaration:
```kotlin
private val _weights = MutableStateFlow(listOf(1, 1))
val weights: StateFlow<List<Int>> = _weights.asStateFlow()
```

### Step 2: Load weights in init

Add after the `currentOptions` collection:
```kotlin
viewModelScope.launch {
    settingsRepository.currentWeights.collect { loaded ->
        _weights.value = loaded
    }
}
```

### Step 3: Add weight update method

```kotlin
fun updateWeight(index: Int, weight: Int) {
    val current = _weights.value.toMutableList()
    while (current.size <= index) current.add(1)
    current[index] = weight.coerceIn(1, 10)
    _weights.value = current
    viewModelScope.launch { settingsRepository.setCurrentWeights(current) }
}
```

### Step 4: Sync weights with option count in addOption/removeOption

In `addOption()`, after `updateOptions(current + ...)`:
```kotlin
_weights.value = _weights.value + 1
viewModelScope.launch { settingsRepository.setCurrentWeights(_weights.value) }
```

In `removeOption(index)`, after `current.removeAt(index)`:
```kotlin
val w = _weights.value.toMutableList()
if (index in w.indices && w.size > 2) {
    w.removeAt(index)
    _weights.value = w
    viewModelScope.launch { settingsRepository.setCurrentWeights(w) }
}
```

### Step 5: Update saveOptionGroup/loadOptionGroup to include weights

```kotlin
fun saveOptionGroup(name: String) {
    viewModelScope.launch {
        settingsRepository.saveOptionGroup(
            OptionGroup(name = name, options = _options.value, weights = _weights.value)
        )
    }
}

fun loadOptionGroup(group: OptionGroup) {
    updateOptions(group.options)
    _weights.value = group.weights.ifEmpty { group.options.map { 1 } }
    viewModelScope.launch { settingsRepository.setCurrentWeights(_weights.value) }
}
```

### Step 6: Rewrite spin() — report result via callback, not internal state

Remove `_result`, `_rotationDegrees`, `_isSpinning`, `spinWithVelocity`, `finishSpin`. Replace with:

```kotlin
private val _optionsEditorOpen = MutableStateFlow(false)
val optionsEditorOpen: StateFlow<Boolean> = _optionsEditorOpen.asStateFlow()

// Result is now set externally by the composable after animation completes
private val _result = MutableStateFlow<String?>(null)
val result: StateFlow<String?> = _result.asStateFlow()

fun onSpinResult(selectedOption: String) {
    _result.value = selectedOption
    viewModelScope.launch {
        historyRepository.addEntry(
            HistoryEntry(
                method = DecisionMethod.SPIN_WHEEL,
                options = _options.value,
                result = selectedOption,
            )
        )
    }
}

fun clearResult() {
    _result.value = null
}
```

Remove `isSpinning`, `rotationDegrees`, `spin()`, `spinWithVelocity()`, `finishSpin()`.

### Step 7: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

---

## Task 3: Rewrite SpinWheelComposable — Animation & Gesture

**Files:**
- **Rewrite:** `SpinWheelComposable.kt`

### Design

**Animation model:**
- `animatable: Animatable<Float, AnimationVector1D>` — single source of truth for rotation
- `rememberCoroutineScope()` — allows launching animations from gesture callbacks (non-suspend)
- Three modes: IDLE, DRAGGING, SPINNING

**Gesture flow:**
1. `onDragStart`: enter DRAGGING mode, record center angle
2. `onDrag`: compute angle delta, `scope.launch { animatable.snapTo(animatable.value + delta) }`, track velocity
3. `onDragEnd`: if velocity > threshold → SPINNING mode, `scope.launch { animatable.animateDecay(...) }`; else → IDLE
4. Button click: SPINNING mode, `scope.launch { animatable.snapTo(0); animatable.animateTo(target) }`

**Result detection:**
- After any SPINNING animation completes → compute segment index → call `onSpinResult(option)`
- While IDLE with `settledResult != null` → if current segment differs from settled → call `onResultDragged()`

### New function signature

```kotlin
@Composable
fun SpinWheel(
    options: List<String>,
    weights: List<Int>,
    colorScheme: WheelColorScheme,
    modifier: Modifier = Modifier,
    onSpinResult: (String) -> Unit = {},
    onResultDragged: () -> Unit = {},
)
```

Note: `rotationDegrees` and `isSpinning` are removed — the composable owns all animation state internally.

### Full rewrite code structure

```kotlin
@Composable
fun SpinWheel(
    options: List<String>,
    weights: List<Int>,
    colorScheme: WheelColorScheme,
    modifier: Modifier = Modifier,
    onSpinResult: (String) -> Unit = {},
    onResultDragged: () -> Unit = {},
) {
    val animatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var settledResult by remember { mutableStateOf<String?>(null) }
    var isAnimating by remember { mutableStateOf(false) }

    // Compute weighted segment angles
    val totalWeight = weights.take(options.size).sum().coerceAtLeast(options.size)
    val segmentAngles = options.indices.map { i ->
        (weights.getOrElse(i) { 1 }.toFloat() / totalWeight) * 360f
    }

    // Helper: compute which segment the indicator points to
    fun currentSegmentIndex(): Int {
        val norm = ((animatable.value % 360f) + 360f) % 360f
        val indicatorAngle = (360f - norm) % 360f
        var accumulated = 0f
        for (i in segmentAngles.indices) {
            accumulated += segmentAngles[i]
            if (indicatorAngle < accumulated) return i
        }
        return segmentAngles.indices.last
    }

    // Helper: launch button spin
    fun launchButtonSpin() {
        if (isAnimating || options.isEmpty()) return
        isAnimating = true
        settledResult = null
        scope.launch {
            val target = (1440..2160).random().toFloat() + (0..360).random().toFloat()
            animatable.snapTo(0f)
            animatable.animateTo(target, tween(3000, easing = StandardEasing.EaseOutQuart))
            isAnimating = false
            val idx = currentSegmentIndex()
            settledResult = options[idx]
            onSpinResult(options[idx])
        }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val wheelSize = minOf(maxWidth, 300.dp)
        // ... paint setup ...

        Canvas(
            modifier = Modifier
                .size(wheelSize)
                .padding(12.dp)
                .pointerInput(options.size, weights) {
                    var prevAngle = 0f
                    var prevTime = 0L
                    val velocities = mutableListOf<Float>()
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (isAnimating) return@detectDragGestures
                            settledResult = null
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            prevAngle = atan2(offset.y - cy, offset.x - cx)
                            prevTime = System.currentTimeMillis()
                            velocities.clear()
                        },
                        onDrag = { change, _ ->
                            if (isAnimating) return@detectDragGestures
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val curAngle = atan2(change.position.y - cy, change.position.x - cx)
                            var delta = Math.toDegrees((curAngle - prevAngle).toDouble()).toFloat()
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            scope.launch { animatable.snapTo(animatable.value + delta) }
                            prevAngle = curAngle
                            val now = System.currentTimeMillis()
                            val dt = (now - prevTime).coerceAtLeast(1)
                            velocities.add(delta / dt * 1000f)
                            if (velocities.size > 5) velocities.removeAt(0)
                            prevTime = now
                            change.consume()
                        },
                        onDragEnd = {
                            if (isAnimating) return@detectDragGestures
                            val avgVelocity = if (velocities.isNotEmpty()) {
                                velocities.sorted().let {
                                    it.subList(it.size / 4, it.size * 3 / 4).average().toFloat()
                                }
                            } else 0f
                            if (kotlin.math.abs(avgVelocity) > 100f) {
                                isAnimating = true
                                scope.launch {
                                    val decay = exponentialDecay<Float>(frictionMultiplier = 2f)
                                    animatable.animateDecay(avgVelocity, decay)
                                    isAnimating = false
                                    val idx = currentSegmentIndex()
                                    settledResult = options[idx]
                                    onSpinResult(options[idx])
                                }
                            }
                        }
                    )
                }
        ) {
            // Drawing code uses segmentAngles for proportional segments
            // ... (same structure as before but with weighted angles)
        }

        // Triangle indicator
        // ...
    }

    // Result-drag detection
    if (settledResult != null && !isAnimating) {
        val currentIdx = currentSegmentIndex()
        if (options.getOrNull(currentIdx) != settledResult) {
            onResultDragged()
            settledResult = null
        }
    }
}
```

### Weighted segment drawing

Replace the fixed `segmentAngle = 360f / options.size` with:
```kotlin
var startAngle = -90f  // top of wheel
options.forEachIndexed { index, option ->
    val sweepAngle = segmentAngles[index]
    val color = colorScheme.getColorForIndex(index)

    drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, ...)
    drawArc(color = borderColor, startAngle = startAngle, sweepAngle = sweepAngle, style = Stroke(...), ...)

    // Text on bisector
    val textAngleDeg = startAngle + sweepAngle / 2
    // ... text drawing ...

    startAngle += sweepAngle
}
```

### Step 1: Write the full rewritten SpinWheelComposable.kt

### Step 2: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

---

## Task 4: Update SpinWheelScreen for New API

**Files:**
- Modify: `SpinWheelScreen.kt`

### Step 1: Collect weights from ViewModel

Add after other collectAsState calls:
```kotlin
val weights by viewModel.weights.collectAsState()
```

### Step 2: Update SpinWheel invocation

```kotlin
SpinWheel(
    options = options,
    weights = weights,
    colorScheme = colorScheme,
    modifier = Modifier.padding(bottom = 16.dp),
    onSpinResult = { selected -> viewModel.onSpinResult(selected) },
    onResultDragged = {
        Toast.makeText(context, "你在干嘛？！", Toast.LENGTH_SHORT).show()
    }
)
```

Remove `rotationDegrees`, `isSpinning` parameters.

### Step 3: Remove `isSpinning` from spin button enabled check

Since `isSpinning` is no longer exposed from the ViewModel, the button should always be enabled when options exist. But we need to prevent double-spins. Add a local state or keep `isSpinning` as a simpler internal flag.

Actually, keep `isSpinning` in the ViewModel but rename it to `isAnimating` and make it purely for UI purposes (disable button during animation). The composable will call `viewModel.setAnimating(true)` at spin start and `viewModel.setAnimating(false)` at spin end.

Revised approach — add to ViewModel:
```kotlin
private val _isAnimating = MutableStateFlow(false)
val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

fun setAnimating(value: Boolean) { _isAnimating.value = value }
```

Update SpinWheel to call `onSpinStart` and `onSpinEnd`:
```kotlin
fun launchButtonSpin() {
    if (isAnimating || options.isEmpty()) return
    isAnimating = true
    onSpinStart()
    // ...
    animatable.animateTo(...)
    isAnimating = false
    onSpinEnd()
    // ...
}
```

Screen:
```kotlin
SpinWheel(
    // ...
    onSpinStart = { viewModel.setAnimating(true) },
    onSpinEnd = { viewModel.setAnimating(false) },
)

Button(
    onClick = { /* trigger spin via state */ },
    enabled = !isAnimating && options.isNotEmpty(),
)
```

For button-triggered spin, use a `triggerSpin` state:
```kotlin
var triggerSpin by remember { mutableStateOf(0) }
// In SpinWheel: LaunchedEffect(triggerSpin) { if (triggerSpin > 0) launchButtonSpin() }
// Button: onClick = { triggerSpin++ }
```

### Step 4: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

---

## Task 5: Add Weight Sliders to OptionsEditorPanel

**Files:**
- Modify: `SpinWheelScreen.kt` (OptionsEditorPanel)

### Step 1: Update OptionsEditorPanel signature

Add `weights: List<Int>` and `onUpdateWeight: (Int, Int) -> Unit` parameters.

### Step 2: Add weight slider below each option row

```kotlin
options.forEachIndexed { index, option ->
    // Existing option row with color indicator + text field + delete button

    // New: weight slider
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "权重: ${weights.getOrElse(index) { 1 }}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(48.dp)
        )
        Slider(
            value = (weights.getOrElse(index) { 1 }).toFloat(),
            onValueChange = { onUpdateWeight(index, it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.weight(1f)
        )
    }
}
```

### Step 3: Wire up in SpinWheelScreen

```kotlin
OptionsEditorPanel(
    // ... existing params ...
    weights = weights,
    onUpdateWeight = { index, weight -> viewModel.updateWeight(index, weight) },
)
```

### Step 4: Verify build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

---

## Task 6: Integration Test & Polish

### Step 1: Manual test checklist

- [ ] Button spin works, wheel stops at correct segment
- [ ] Drag wheel with finger — follows touch in real-time
- [ ] Flick wheel fast — continues spinning with momentum, slows down naturally
- [ ] After any spin result, drag result to another segment → "你在干嘛？！" toast
- [ ] After any spin result, drag within same segment → no toast
- [ ] Weight sliders change segment sizes proportionally
- [ ] Options with weight 10 are visibly larger than options with weight 1
- [ ] Save/load option groups preserves weights
- [ ] Adding/removing options adjusts weights list correctly
- [ ] Split-screen mode: wheel scales down, scrollable

### Step 2: Final build

```bash
cd D:/Android/Projects/CHOICEPARALYSIS && ./gradlew assembleDebug
```

### Step 3: Commit

```bash
git add -A
git commit -m "feat: rewrite spin wheel gesture handling + add option weights"
```
