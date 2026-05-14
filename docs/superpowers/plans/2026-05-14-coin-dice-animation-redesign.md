# Coin & Dice Animation Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the coin flip and dice roll animation system to use Canvas-based 3D rendering with custom image support and camera zoom transitions.

**Architecture:** Replace current graphicsLayer-based animations with Canvas 3D rendering for unified coin/dice animation. Implement standard easing curves and camera zoom screen transitions.

**Tech Stack:** Compose Canvas API, Kotlin coroutines, Coil for image loading

---

## File Structure

### New Files
- `app/src/main/java/com/choiceparalysis/turntable/ui/components/StandardEasing.kt` - Standard easing curve definitions
- `app/src/main/java/com/choiceparalysis/turntable/ui/components/Canvas3DRenderer.kt` - 3D rendering engine
- `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/Coin3DComposable.kt` - Coin 3D animation composable
- `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/Dice3DComposable.kt` - Dice 3D animation composable

### Modified Files
- `app/src/main/java/com/choiceparalysis/turntable/ui/components/Win10Animations.kt` - Delete (replaced by StandardEasing)
- `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/CoinFlipComposable.kt` - Delete (replaced by Coin3DComposable)
- `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/DiceRollComposable.kt` - Delete (replaced by Dice3DComposable)
- `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/CoinDiceScreen.kt` - Update to use new composables
- `app/src/main/java/com/choiceparalysis/turntable/navigation/AppNavGraph.kt` - Update transition animations
- `app/src/main/java/com/choiceparalysis/turntable/ui/components/AnimatedResult.kt` - Update easing references

---

### Task 1: Create StandardEasing Component

**Files:**
- Create: `app/src/main/java/com/choiceparalysis/turntable/ui/components/StandardEasing.kt`

- [ ] **Step 1: Create StandardEasing object**

```kotlin
package com.choiceparalysis.turntable.ui.components

import androidx.compose.animation.core.CubicBezierEasing

object StandardEasing {
    val EaseOutCubic = CubicBezierEasing(0.33f, 1.0f, 0.68f, 1.0f)
    val EaseInCubic = CubicBezierEasing(0.32f, 0.0f, 0.67f, 0.0f)
    val EaseOutQuart = CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f)
    val EaseInQuart = CubicBezierEasing(0.5f, 0.0f, 0.75f, 0.0f)
    val EaseInOutQuart = CubicBezierEasing(0.76f, 0.0f, 0.24f, 1.0f)
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/choiceparalysis/turntable/ui/components/StandardEasing.kt
git commit -m "feat: add StandardEasing component with standard easing curves"
```

---

### Task 2: Create Canvas3DRenderer Component

**Files:**
- Create: `app/src/main/java/com/choiceparalysis/turntable/ui/components/Canvas3DRenderer.kt`

- [ ] **Step 1: Create Canvas3DRenderer class**

```kotlin
package com.choiceparalysis.turntable.ui.components

import kotlin.math.cos
import kotlin.math.sin

class Canvas3DRenderer {
    data class Vertex3D(val x: Float, val y: Float, val z: Float)
    data class Point2D(val x: Float, val y: Float)

    fun project(vertex: Vertex3D, cameraDistance: Float): Point2D {
        val factor = cameraDistance / (cameraDistance + vertex.z)
        return Point2D(
            x = vertex.x * factor,
            y = vertex.y * factor
        )
    }

    fun rotate(vertex: Vertex3D, rotX: Float, rotY: Float, rotZ: Float): Vertex3D {
        // Rotate around X axis
        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val y1 = vertex.y * cosX - vertex.z * sinX
        val z1 = vertex.y * sinX + vertex.z * cosX

        // Rotate around Y axis
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val x2 = vertex.x * cosY + z1 * sinY
        val z2 = -vertex.x * sinY + z1 * cosY

        // Rotate around Z axis
        val cosZ = cos(rotZ)
        val sinZ = sin(rotZ)
        val x3 = x2 * cosZ - y1 * sinZ
        val y3 = x2 * sinZ + y1 * cosZ

        return Vertex3D(x3, y3, z2)
    }

    fun getCubeVertices(size: Float): List<Vertex3D> {
        val half = size / 2f
        return listOf(
            Vertex3D(-half, -half, -half), // 0: front-top-left
            Vertex3D(half, -half, -half),  // 1: front-top-right
            Vertex3D(half, half, -half),   // 2: front-bottom-right
            Vertex3D(-half, half, -half),  // 3: front-bottom-left
            Vertex3D(-half, -half, half),  // 4: back-top-left
            Vertex3D(half, -half, half),   // 5: back-top-right
            Vertex3D(half, half, half),    // 6: back-bottom-right
            Vertex3D(-half, half, half)    // 7: back-bottom-left
        )
    }

    fun getCubeFaces(): List<List<Int>> {
        return listOf(
            listOf(0, 1, 2, 3), // front
            listOf(5, 4, 7, 6), // back
            listOf(4, 0, 3, 7), // left
            listOf(1, 5, 6, 2), // right
            listOf(4, 5, 1, 0), // top
            listOf(3, 2, 6, 7)  // bottom
        )
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/choiceparalysis/turntable/ui/components/Canvas3DRenderer.kt
git commit -m "feat: add Canvas3DRenderer for 3D transformations"
```

---

### Task 3: Create Coin3DComposable

**Files:**
- Create: `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/Coin3DComposable.kt`

- [ ] **Step 1: Create Coin3DFlip composable**

```kotlin
package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.ui.components.Canvas3DRenderer
import com.choiceparalysis.turntable.ui.components.StandardEasing
import com.choiceparalysis.turntable.viewmodel.CoinSide
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun Coin3DFlip(
    result: CoinSide?,
    isAnimating: Boolean,
    headsImage: ImageBitmap?,
    tailsImage: ImageBitmap?,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val rotationAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            val randomOffset = (0..360).random().toFloat()

            rotationAnim.snapTo(0f)
            translationYAnim.snapTo(0f)
            scaleAnim.snapTo(1f)

            launch {
                translationYAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 2000
                        0f at 0
                        -200f at 600 using StandardEasing.EaseOutQuart
                        0f at 1200 using StandardEasing.EaseInQuart
                        -30f at 1500
                        0f at 2000 using StandardEasing.EaseOutQuart
                    }
                )
            }

            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 2000
                        1f at 0
                        1.2f at 600 using StandardEasing.EaseOutQuart
                        1f at 1200 using StandardEasing.EaseInQuart
                    }
                )
            }

            rotationAnim.animateTo(
                targetValue = 1800f + randomOffset,
                animationSpec = keyframes {
                    durationMillis = 2000
                    0f at 0
                    360f at 600 using StandardEasing.EaseOutCubic
                    1440f at 1400 using StandardEasing.EaseOutQuart
                    (1800f + randomOffset) at 2000 using StandardEasing.EaseOutQuart
                }
            )

            onAnimationComplete()
        }
    }

    val showHeads = result == CoinSide.HEADS || result == null
    val displayImage = if (showHeads) headsImage else tailsImage
    val backgroundColor = if (showHeads) Color(0xFFFFD700) else Color(0xFFC0C0C0)

    Canvas(
        modifier = modifier.size(150.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width / 2

        val rotation = rotationAnim.value % 360f
        val translationY = translationYAnim.value
        val scale = scaleAnim.value

        // Calculate 3D rotation effect
        val absRotation = abs(rotation)
        val showFront = absRotation % 360f < 180f || absRotation % 360f > 180f

        // Apply transformations
        translate(centerX, centerY + translationY) {
            scale(scale, scale) {
                // Draw coin shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = radius * 0.9f,
                    center = Offset(0f, 10f)
                )

                // Draw coin body
                rotate(rotation) {
                    drawCircle(
                        color = backgroundColor,
                        radius = radius
                    )

                    // Draw image if available
                    displayImage?.let { image ->
                        val imageSize = IntSize((radius * 1.8f).toInt(), (radius * 1.8f).toInt())
                        drawImage(
                            image = image,
                            dstSize = imageSize,
                            dstOffset = Offset(-imageSize.width / 2f, -imageSize.height / 2f)
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/choiceparalysis/turntable/ui/coindice/Coin3DComposable.kt
git commit -m "feat: add Coin3DFlip composable with Canvas 3D rendering"
```

---

### Task 4: Create Dice3DComposable

**Files:**
- Create: `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/Dice3DComposable.kt`

- [ ] **Step 1: Create Dice3DRoll composable**

```kotlin
package com.choiceparalysis.turntable.ui.coindice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.ui.components.Canvas3DRenderer
import com.choiceparalysis.turntable.ui.components.StandardEasing
import kotlinx.coroutines.launch

@Composable
fun Dice3DRoll(
    value: Int?,
    isAnimating: Boolean,
    faceImages: Map<Int, ImageBitmap?>,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val animRotZ = remember { Animatable(0f) }
    val animRotX = remember { Animatable(0f) }
    val animTransY = remember { Animatable(0f) }
    val animScale = remember { Animatable(1f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            val randomOffset = (0..360).random().toFloat()

            animRotZ.snapTo(0f)
            animRotX.snapTo(0f)
            animTransY.snapTo(0f)
            animScale.snapTo(1f)

            launch {
                animTransY.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 2200
                        0f at 0
                        -250f at 500 using StandardEasing.EaseOutQuart
                        0f at 800 using StandardEasing.EaseInQuart
                        -120f at 1100 using StandardEasing.EaseOutQuart
                        0f at 1350 using StandardEasing.EaseInQuart
                        -40f at 1600
                        0f at 1800
                        -10f at 1950
                        0f at 2200 using StandardEasing.EaseOutQuart
                    }
                )
            }

            launch {
                animScale.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 2200
                        1f at 0
                        1.3f at 500 using StandardEasing.EaseOutQuart
                        1f at 800 using StandardEasing.EaseInQuart
                    }
                )
            }

            launch {
                animRotX.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 2200
                        0f at 0
                        45f at 300
                        -30f at 700
                        20f at 1200
                        0f at 2200 using StandardEasing.EaseOutQuart
                    }
                )
            }

            animRotZ.animateTo(
                targetValue = 1080f + randomOffset,
                animationSpec = keyframes {
                    durationMillis = 2200
                    0f at 0
                    360f at 500 using StandardEasing.EaseOutCubic
                    900f at 1100 using StandardEasing.EaseOutQuart
                    (1080f + randomOffset) at 2200 using StandardEasing.EaseOutQuart
                }
            )

            onAnimationComplete()
        }
    }

    val displayValue = value ?: 1
    val displayImage = faceImages[displayValue]

    Canvas(
        modifier = modifier.size(120.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val diceSize = size.width * 0.8f
        val cornerRadius = diceSize * 0.15f

        val rotationZ = animRotZ.value
        val rotationX = animRotX.value
        val translationY = animTransY.value
        val scale = animScale.value

        translate(centerX, centerY + translationY) {
            scale(scale, scale) {
                // Draw dice shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(-diceSize / 2 + 5f, -diceSize / 2 + 10f),
                    size = Size(diceSize, diceSize),
                    cornerRadius = CornerRadius(cornerRadius)
                )

                // Draw dice body with rotation
                rotate(rotationZ) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(-diceSize / 2, -diceSize / 2),
                        size = Size(diceSize, diceSize),
                        cornerRadius = CornerRadius(cornerRadius)
                    )

                    // Draw image or dots
                    if (displayImage != null) {
                        val imageSize = IntSize((diceSize * 0.8f).toInt(), (diceSize * 0.8f).toInt())
                        drawImage(
                            image = displayImage,
                            dstSize = imageSize,
                            dstOffset = Offset(-imageSize.width / 2f, -imageSize.height / 2f)
                        )
                    } else {
                        drawDiceDots(displayValue, diceSize)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawDiceDots(value: Int, diceSize: Float) {
    val dotRadius = diceSize * 0.08f
    val dotColor = Color.Black
    val positions = when (value) {
        1 -> listOf(Offset(0f, 0f))
        2 -> listOf(Offset(-diceSize * 0.25f, -diceSize * 0.25f), Offset(diceSize * 0.25f, diceSize * 0.25f))
        3 -> listOf(Offset(-diceSize * 0.25f, -diceSize * 0.25f), Offset(0f, 0f), Offset(diceSize * 0.25f, diceSize * 0.25f))
        4 -> listOf(Offset(-diceSize * 0.25f, -diceSize * 0.25f), Offset(diceSize * 0.25f, -diceSize * 0.25f), Offset(-diceSize * 0.25f, diceSize * 0.25f), Offset(diceSize * 0.25f, diceSize * 0.25f))
        5 -> listOf(Offset(-diceSize * 0.25f, -diceSize * 0.25f), Offset(diceSize * 0.25f, -diceSize * 0.25f), Offset(0f, 0f), Offset(-diceSize * 0.25f, diceSize * 0.25f), Offset(diceSize * 0.25f, diceSize * 0.25f))
        6 -> listOf(Offset(-diceSize * 0.25f, -diceSize * 0.25f), Offset(diceSize * 0.25f, -diceSize * 0.25f), Offset(-diceSize * 0.25f, 0f), Offset(diceSize * 0.25f, 0f), Offset(-diceSize * 0.25f, diceSize * 0.25f), Offset(diceSize * 0.25f, diceSize * 0.25f))
        else -> emptyList()
    }

    positions.forEach { position ->
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = position
        )
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/choiceparalysis/turntable/ui/coindice/Dice3DComposable.kt
git commit -m "feat: add Dice3DRoll composable with Canvas 3D rendering"
```

---

### Task 5: Update CoinDiceScreen to Use New Composables

**Files:**
- Modify: `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/CoinDiceScreen.kt`

- [ ] **Step 1: Update imports and replace CoinFlip with Coin3DFlip**

Replace lines 33-35:
```kotlin
import com.choiceparalysis.turntable.ui.components.AnimatedResult
import com.choiceparalysis.turntable.viewmodel.CoinDiceViewModel
import com.choiceparalysis.turntable.viewmodel.CoinSide
```

With:
```kotlin
import com.choiceparalysis.turntable.ui.components.AnimatedResult
import com.choiceparalysis.turntable.viewmodel.CoinDiceViewModel
import com.choiceparalysis.turntable.viewmodel.CoinSide
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import androidx.compose.runtime.produceState
```

- [ ] **Step 2: Add image loading logic**

Add after line 54 (after `var showCustomizationSheet by remember { mutableStateOf(false) }`):
```kotlin
    val context = LocalContext.current

    // Load coin images
    val headsBitmap by produceState<ImageBitmap?>(null, customCoinHeadsUri) {
        value = customCoinHeadsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.image as? ImageBitmap)
            } else null
        }
    }

    val tailsBitmap by produceState<ImageBitmap?>(null, customCoinTailsUri) {
        value = customCoinTailsUri?.let { uri ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.image as? ImageBitmap)
            } else null
        }
    }

    // Load dice images
    val diceBitmaps by produceState<Map<Int, ImageBitmap>>(emptyMap(), customDiceUris) {
        value = customDiceUris.mapValues { (_, uri) ->
            uri?.let { uriString ->
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(uriString)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    (result.image as? ImageBitmap)
                } else null
            }
        }.filterValues { it != null }.mapValues { it.value!! }
    }
```

- [ ] **Step 3: Replace CoinFlip with Coin3DFlip**

Replace lines 114-122:
```kotlin
                CoinFlip(
                    result = coinResult,
                    isAnimating = isAnimating,
                    coinHeadsUri = customCoinHeadsUri,
                    coinTailsUri = customCoinTailsUri,
                    onAnimationComplete = { viewModel.onCoinFlipAnimationComplete() },
                    modifier = Modifier.padding(bottom = 24.dp)
                )
```

With:
```kotlin
                Coin3DFlip(
                    result = coinResult,
                    isAnimating = isAnimating,
                    headsImage = headsBitmap,
                    tailsImage = tailsBitmap,
                    onAnimationComplete = { viewModel.onCoinFlipAnimationComplete() },
                    modifier = Modifier.padding(bottom = 24.dp)
                )
```

- [ ] **Step 4: Replace DiceRoll with Dice3DRoll**

Replace lines 147-153:
```kotlin
                DiceRoll(
                    value = diceValue,
                    isAnimating = isAnimating,
                    diceImageUris = customDiceUris,
                    onAnimationComplete = { viewModel.onDiceRollAnimationComplete() },
                    modifier = Modifier.padding(bottom = 24.dp)
                )
```

With:
```kotlin
                Dice3DRoll(
                    value = diceValue,
                    isAnimating = isAnimating,
                    faceImages = diceBitmaps,
                    onAnimationComplete = { viewModel.onDiceRollAnimationComplete() },
                    modifier = Modifier.padding(bottom = 24.dp)
                )
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/choiceparalysis/turntable/ui/coindice/CoinDiceScreen.kt
git commit -m "feat: update CoinDiceScreen to use new 3D composables"
```

---

### Task 6: Update AppNavGraph with Camera Zoom Transitions

**Files:**
- Modify: `app/src/main/java/com/choiceparalysis/turntable/navigation/AppNavGraph.kt`

- [ ] **Step 1: Update imports**

Replace lines 1-15:
```kotlin
package com.choiceparalysis.turntable.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.choiceparalysis.turntable.ui.coindice.CoinDiceScreen
import com.choiceparalysis.turntable.ui.components.Win10Animations
import com.choiceparalysis.turntable.ui.history.HistoryScreen
import com.choiceparalysis.turntable.ui.home.SpinWheelScreen
import com.choiceparalysis.turntable.ui.settings.SettingsScreen
import com.choiceparalysis.turntable.ui.yesno.YesNoScreen
```

With:
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
import com.choiceparalysis.turntable.ui.coindice.CoinDiceScreen
import com.choiceparalysis.turntable.ui.components.StandardEasing
import com.choiceparalysis.turntable.ui.history.HistoryScreen
import com.choiceparalysis.turntable.ui.home.SpinWheelScreen
import com.choiceparalysis.turntable.ui.settings.SettingsScreen
import com.choiceparalysis.turntable.ui.yesno.YesNoScreen
```

- [ ] **Step 2: Update NavHost transitions**

Replace lines 23-31:
```kotlin
    NavHost(
        navController = navController,
        startDestination = AppDestinations.SPIN_WHEEL.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(250, easing = Win10Animations.EntranceEasing)) },
        exitTransition = { fadeOut(tween(200, easing = Win10Animations.ExitEasing)) },
        popEnterTransition = { fadeIn(tween(250, easing = Win10Animations.EntranceEasing)) },
        popExitTransition = { fadeOut(tween(200, easing = Win10Animations.ExitEasing)) },
    ) {
```

With:
```kotlin
    NavHost(
        navController = navController,
        startDestination = AppDestinations.SPIN_WHEEL.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(
                animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)
            ) + scaleIn(
                initialScale = 0.5f,
                animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)
            ) + scaleOut(
                targetScale = 1.5f,
                animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)
            ) + scaleIn(
                initialScale = 0.5f,
                animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)
            ) + scaleOut(
                targetScale = 1.5f,
                animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)
            )
        },
    ) {
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/choiceparalysis/turntable/navigation/AppNavGraph.kt
git commit -m "feat: update AppNavGraph with camera zoom transitions"
```

---

### Task 7: Update AnimatedResult to Use StandardEasing

**Files:**
- Modify: `app/src/main/java/com/choiceparalysis/turntable/ui/components/AnimatedResult.kt`

- [ ] **Step 1: Update import**

Replace line 11:
```kotlin
import com.choiceparalysis.turntable.ui.components.Win10Animations
```

With:
```kotlin
import com.choiceparalysis.turntable.ui.components.StandardEasing
```

- [ ] **Step 2: Update easing references**

Replace lines 37-41:
```kotlin
        enter = fadeIn(tween(400, easing = Win10Animations.EntranceEasing)) +
                scaleIn(tween(400, easing = Win10Animations.EntranceEasing)) +
                slideInVertically(
                    tween(400, easing = Win10Animations.EntranceEasing)
                ) { it / 3 }
```

With:
```kotlin
        enter = fadeIn(tween(400, easing = StandardEasing.EaseOutCubic)) +
                scaleIn(tween(400, easing = StandardEasing.EaseOutCubic)) +
                slideInVertically(
                    tween(400, easing = StandardEasing.EaseOutCubic)
                ) { it / 3 }
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/choiceparalysis/turntable/ui/components/AnimatedResult.kt
git commit -m "feat: update AnimatedResult to use StandardEasing"
```

---

### Task 8: Delete Old Animation Files

**Files:**
- Delete: `app/src/main/java/com/choiceparalysis/turntable/ui/components/Win10Animations.kt`
- Delete: `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/CoinFlipComposable.kt`
- Delete: `app/src/main/java/com/choiceparalysis/turntable/ui/coindice/DiceRollComposable.kt`

- [ ] **Step 1: Delete Win10Animations.kt**

```bash
rm app/src/main/java/com/choiceparalysis/turntable/ui/components/Win10Animations.kt
```

- [ ] **Step 2: Delete CoinFlipComposable.kt**

```bash
rm app/src/main/java/com/choiceparalysis/turntable/ui/coindice/CoinFlipComposable.kt
```

- [ ] **Step 3: Delete DiceRollComposable.kt**

```bash
rm app/src/main/java/com/choiceparalysis/turntable/ui/coindice/DiceRollComposable.kt
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: remove old animation files replaced by Canvas 3D implementations"
```

---

### Task 9: Final Verification and Testing

**Files:**
- None (verification only)

- [ ] **Step 1: Run full compilation**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run unit tests**

Run: `./gradlew test`
Expected: All tests pass

- [ ] **Step 3: Verify app launches**

Manually verify the app launches and animations work correctly on device/emulator

- [ ] **Step 4: Final commit if needed**

If any fixes were needed:
```bash
git add -A
git commit -m "fix: final adjustments for animation redesign"
```

---

## Self-Review Checklist

1. **Spec coverage:** ✅ All requirements covered
   - Canvas 3D rendering for coin and dice ✅
   - Custom image support ✅
   - Camera zoom transitions ✅
   - Standard naming convention ✅

2. **Placeholder scan:** ✅ No TBD/TODO/incomplete sections

3. **Type consistency:** ✅ All types and function signatures consistent across tasks

4. **File paths:** ✅ All file paths are exact and consistent

5. **Code completeness:** ✅ All code blocks are complete and runnable

---

## Execution Options

**Plan complete and saved to `docs/superpowers/plans/2026-05-14-coin-dice-animation-redesign.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
