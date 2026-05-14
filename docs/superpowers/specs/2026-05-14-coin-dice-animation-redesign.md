# Coin & Dice Animation Redesign

## Overview

Redesign the coin flip and dice roll animation system to use Canvas-based 3D rendering, ensuring the entire coin/dice (including custom images) animates as a unified element rather than having static backgrounds with animated icons.

## Goals

1. **Unified Animation**: Coin and dice shapes should animate as complete 3D objects, not just backgrounds
2. **Custom Image Support**: Support custom images for coin faces (2) and dice faces (6) that animate with the object
3. **Camera Zoom Transitions**: Implement screen transitions with zoom-in/zoom-out effects simulating camera focal length changes
4. **Standard Naming**: Replace `Win10Animations` with standard easing curve names
5. **Flat Design**: Maintain modern, flat design aesthetics

## Technical Approach

### Canvas 3D Rendering System

**Core Components**:
- `Canvas3DRenderer`: 3D rendering engine for coordinate transformation and projection
- `CoinAnimator`: Coin animation controller managing flip logic and face switching
- `DiceAnimator`: Dice animation controller managing roll and face display
- `StandardEasing`: Standard animation curve definitions

**Technology Stack**:
- Compose Canvas API for custom rendering
- Matrix transformations for 3D rotation
- Custom projection algorithms for 3D-to-2D mapping

### Coin Animation Design

**3D Flip Implementation**:
1. Calculate rotated vertex positions using sine/cosine functions
2. Determine face visibility based on rotation angle
3. Apply perspective projection for 3D depth perception
4. Rotate custom images with the coin surface

**Animation Flow**:
1. User tap → Coin launches upward (Y-axis displacement)
2. Simultaneous Y-axis rotation (simulating flip)
3. Switch displayed image when rotation exceeds 90°
4. Coin descends with deceleration
5. Display final result

**Visual Effects**:
- Dynamic shadow based on height
- Slight X-axis tilt for enhanced 3D feel
- Custom image support for heads/tails faces

### Dice Animation Design

**3D Roll Implementation**:
1. Use cube geometry to represent dice
2. Apply X/Y/Z axis rotation transformations
3. Calculate visible face based on rotation angle
4. Render corresponding face image or dot pattern

**Animation Flow**:
1. User tap → Dice launches upward
2. Simultaneous multi-axis rotation (simulating roll)
3. Determine display face based on final angle
4. Dice descends with bounce effect
5. Display final value

**Visual Effects**:
- 6-face cube with custom image support
- Real 3D perspective during rotation
- Shadow and lighting for depth
- Bounce animation on landing

### Screen Transition Animation

**Camera Zoom Effect**:
1. **Old Page**: Scale 1.0 → 1.5 (zoom in) + Fade out (alpha 1.0 → 0.0)
2. **New Page**: Scale 0.5 → 1.0 (zoom in from small) + Fade in (alpha 0.0 → 1.0)
3. **Easing Curve**: `EaseInOutQuart` for non-linear zoom motion

**Animation Parameters**:
- Duration: 500ms
- Old page scale: 1.5 (pronounced zoom effect)
- New page start scale: 0.5 (small to large)
- Easing: `EaseInOutQuart` (slow start - fast middle - slow end)

### Naming Convention

**Rename `Win10Animations` to `StandardEasing`**:
- `EntranceEasing` → `EaseOutCubic`
- `ExitEasing` → `EaseInCubic`
- `DecelerateEasing` → `EaseOutQuart`
- `AccelerateEasing` → `EaseInQuart`

**Standard Easing Curves**:
```kotlin
object StandardEasing {
    val EaseOutCubic = CubicBezierEasing(0.33f, 1.0f, 0.68f, 1.0f)
    val EaseInCubic = CubicBezierEasing(0.32f, 0.0f, 0.67f, 0.0f)
    val EaseOutQuart = CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f)
    val EaseInQuart = CubicBezierEasing(0.5f, 0.0f, 0.75f, 0.0f)
    val EaseInOutQuart = CubicBezierEasing(0.76f, 0.0f, 0.24f, 1.0f)
}
```

## File Structure

### New Files
- `ui/components/Canvas3DRenderer.kt` - 3D rendering engine
- `ui/components/StandardEasing.kt` - Standard easing curves
- `ui/coindice/Coin3DAnimator.kt` - Coin 3D animation
- `ui/coindice/Dice3DAnimator.kt` - Dice 3D animation

### Modified Files
- `ui/coindice/CoinFlipComposable.kt` - Rewrite with Canvas 3D
- `ui/coindice/DiceRollComposable.kt` - Rewrite with Canvas 3D
- `navigation/AppNavGraph.kt` - Update transition animations
- `ui/components/AnimatedResult.kt` - Update easing references
- All files using `Win10Animations` - Update to `StandardEasing`

## Implementation Details

### Canvas3DRenderer

```kotlin
class Canvas3DRenderer {
    // 3D vertex data class
    data class Vertex3D(val x: Float, val y: Float, val z: Float)

    // 2D projected point
    data class Point2D(val x: Float, val y: Float)

    // Project 3D vertex to 2D screen coordinates
    fun project(vertex: Vertex3D, cameraDistance: Float): Point2D

    // Apply rotation transformation
    fun rotate(vertex: Vertex3D, rotX: Float, rotY: Float, rotZ: Float): Vertex3D
}
```

### Coin3DAnimator

```kotlin
@Composable
fun Coin3DFlip(
    result: CoinSide?,
    isAnimating: Boolean,
    headsImage: ImageBitmap?,
    tailsImage: ImageBitmap?,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    // Canvas-based 3D rendering
    Canvas(modifier = modifier) {
        // Calculate rotation angles
        // Determine visible face
        // Draw coin with perspective
        // Apply custom images
    }
}
```

### Dice3DAnimator

```kotlin
@Composable
fun Dice3DRoll(
    value: Int?,
    isAnimating: Boolean,
    faceImages: Map<Int, ImageBitmap?>,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    // Canvas-based 3D rendering
    Canvas(modifier = modifier) {
        // Calculate cube vertices
        // Apply rotation transformations
        // Determine visible faces
        // Draw cube with images
    }
}
```

### Screen Transitions

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
    }
)
```

## Performance Considerations

1. **Canvas Optimization**: Minimize draw calls by batching operations
2. **Image Caching**: Cache decoded ImageBitmap objects
3. **Animation Throttling**: Use `LaunchedEffect` to prevent multiple simultaneous animations
4. **Memory Management**: Release resources when composables leave composition

## Compatibility

- **Minimum SDK**: API 34 (Android 14)
- **Target SDK**: API 36
- **Compose Version**: BOM 2025.12.00
- **Coil Version**: 3.2.0 (for image loading)

## Testing Strategy

1. **Unit Tests**: Animation logic and 3D calculations
2. **UI Tests**: Visual regression tests for animations
3. **Performance Tests**: Frame rate monitoring during animations
4. **Device Tests**: Test on various screen sizes and densities

## Success Criteria

1. Coin and dice animate as complete 3D objects with custom images
2. Screen transitions show camera zoom effect
3. All animations run at 60fps on target devices
4. Custom images load and display correctly
5. Code is maintainable with clear naming conventions