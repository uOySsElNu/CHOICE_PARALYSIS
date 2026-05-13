# Spin Wheel Redesign - Design Spec

## Overview

Redesign the spin wheel module with a flexible color system, improved option management, persistent state, and Win10-style animations. Add a global settings page for app-wide configuration.

## Architecture

### Settings Split

**Per-module settings** (spin wheel page, gear icon):
- Dynamic color toggle (time-based 24h gradient)
- Preset theme selection (existing WheelDesign presets)
- Per-option color customization
- Option editing panel

**Global settings** (bottom navigation bar):
- About section (version, privacy policy, terms)
- Future app-wide configuration

### Navigation Changes

Remove "选项列表" (Lists) tab, add "设置" (Settings) tab at rightmost position:

```
Before: 转盘 | 硬币骰子 | Yes/No | 选项列表 | 历史记录
After:  转盘 | 硬币骰子 | Yes/No | 历史记录 | 设置
```

Spin wheel page gear icon navigates to settings page (for now, until per-module settings are implemented for other modules).

Wait - re-reading the user's feedback: the spin wheel settings should be ON the spin wheel page, not in global settings. The gear icon on spin wheel page opens a settings panel/dialog specific to the spin wheel. The global settings page (bottom bar) is separate.

**Revised:**
- Gear icon on spin wheel page → opens an in-page settings panel (bottom sheet or expandable) for dynamic color, themes, option colors
- Bottom bar "设置" → global settings page with About, privacy, etc.

### Color System

#### WheelColorScheme Data Model

```kotlin
data class WheelColorScheme(
    val segmentColors: List<Color>,
    val borderColor: Color,
    val textColor: Color,
    val indicatorColor: Color,
)
```

#### Color Modes

1. **Dynamic (time-based)**: 24h continuous gradient with 6 anchor points
   - Dawn (5-7h): soft pink/orange tones
   - Morning (7-11h): light blue tones
   - Noon (11-14h): bright golden tones
   - Afternoon (14-17h): warm orange tones
   - Evening (17-20h): deep orange/red tones
   - Night (20-5h): deep blue/purple tones
   - Linear interpolation between anchors
   - Each option gets a different hue offset from the base
   - Updates every minute

2. **Preset**: Existing 5 WheelDesign themes
   - CLASSIC_RAINBOW, MONOCHROME, WARM_SUNSET, OCEAN_BREEZE, MINIMAL

3. **Custom**: User picks individual colors per option

#### Dynamic Color When Toggle On

- Color picker in option editor is grayed out / disabled
- Snackbar at bottom: "动态色彩已开启"
- Wheel segments use time-based colors automatically

### Option Management

#### Default Options

First launch: `["Yes", "No"]` (not "选项1", "选项2", etc.)

#### Persistence

Current wheel options auto-persisted to DataStore on every change. Restored on app launch.

#### Option Groups

```kotlin
@Serializable
data class OptionGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val options: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
)
```

Saved groups persisted in DataStore. User can save current options as a named group, load a group, delete groups.

#### Option Editor Panel

Button below the wheel: "编辑选项 ▼" / "编辑选项 ▲"
Clicking expands/collapses a panel with:
- List of options with text fields and color indicators
- Remove button per option
- Add option button (max 10)
- "保存为组合" button
- When dynamic color is on, color indicators are grayed out

### Spin Wheel Page Layout

```
┌─────────────────────────────┐
│ [←] 转盘决策          [⚙️]  │
├─────────────────────────────┤
│         [转盘]              │
│                             │
│       [开始转动]            │
│                             │
│        [结果]               │
│                             │
│    [编辑选项 ▼]             │
│  ┌───────────────────────┐  │
│  │ Yes    [颜色] [-]     │  │  (expanded)
│  │ No     [颜色] [-]     │  │
│  │ [+ 添加选项]          │  │
│  │ [保存为组合]          │  │
│  └───────────────────────┘  │
└─────────────────────────────┘
```

### Global Settings Page Layout

```
┌─────────────────────────────┐
│           设置               │
├─────────────────────────────┤
│ ℹ️ 关于                     │
│ ┌─────────────────────────┐ │
│ │ 版本    v1.0.0           │ │
│ │ 隐私政策          →      │ │
│ │ 使用条款          →      │ │
│ └─────────────────────────┘ │
│                             │
│ (预留未来全局配置区域)       │
└─────────────────────────────┘
```

### Win10-Style Animations

Applied to global navigation transitions (NavHost):

```kotlin
val win10Easing = CubicBezierEasing(0.1f, 0.0f, 0.0f, 1.0f)

enterTransition = fadeIn(tween(200, easing = win10Easing))
exitTransition = fadeOut(tween(150, easing = win10Easing))
```

Non-asymmetric timing: fade-in slightly slower than fade-out for the characteristic Win10 feel.

### Settings Panel (Spin Wheel)

The spin wheel settings are presented as a ModalBottomSheet triggered by the gear icon:

```
┌─────────────────────────────┐
│         转盘设置             │
├─────────────────────────────┤
│ 动态色彩        [开关]      │
│ 根据时间自动调整转盘色彩     │
│                             │
│ (开关关闭时显示)             │
│ ─────────────────────────── │
│ 预设主题                    │
│ [🌈经典] [🎨单色] [🌅日落]  │
│ [🌊海洋] [⚪极简]           │
│                             │
│ ─────────────────────────── │
│ 选项颜色                    │
│ Yes  ● [调色板]             │
│ No   ● [调色板]             │
│ (动态色彩开启时灰色禁用)     │
└─────────────────────────────┘
```

## DataStore Keys

```kotlin
// Spin wheel settings
DYNAMIC_COLOR_ENABLED (Boolean) = true
SELECTED_PRESET (String) = "CLASSIC_RAINBOW"
CUSTOM_COLORS (String) = JSON list of Color ints
CURRENT_OPTIONS (String) = JSON list of strings
OPTION_GROUPS (String) = JSON list of OptionGroup
```

## File Changes

| Operation | File | Description |
|-----------|------|-------------|
| Create | `ui/settings/SettingsScreen.kt` | Global settings page |
| Create | `viewmodel/SettingsViewModel.kt` | Global settings logic |
| Create | `data/repository/SettingsRepository.kt` | Settings persistence |
| Create | `ui/home/TimeBasedColorGenerator.kt` | 24h color generation |
| Create | `ui/home/WheelSettingsSheet.kt` | Bottom sheet for wheel settings |
| Modify | `navigation/AppDestinations.kt` | Remove LISTS, add SETTINGS |
| Modify | `navigation/AppNavGraph.kt` | Add settings route, nav animations |
| Modify | `MainActivity.kt` | Update nav items |
| Modify | `SpinWheelScreen.kt` | Gear icon, option editor panel, remove design selection |
| Modify | `SpinWheelViewModel.kt` | Settings integration, option persistence |
| Modify | `SpinWheelComposable.kt` | Support WheelColorScheme |
| Modify | `WheelDesign.kt` | Keep as preset data source |
| Modify | `data/datastore/AppDataStore.kt` | Add new preference keys |
| Delete | `ui/lists/ListsScreen.kt` | Remove unused lists screen |
| Delete | `viewmodel/ListsViewModel.kt` | Remove unused lists VM |
