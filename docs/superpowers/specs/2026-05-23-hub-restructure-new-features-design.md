# Hub Navigation Restructure + New Features Design

**Date:** 2026-05-23
**Goal:** Restructure navigation to card-based hub, add finger roulette, haptic/sound system, and stats dashboard.

---

## 1. Navigation Architecture

### Current State
- 5 bottom nav tabs: Spin Wheel | Coin/Dice | Yes/No | History | Settings
- Coin and Dice share one screen with internal Tab switching

### Target State
- 3 bottom nav tabs: **Hub** | **History** | **Settings**
- Hub is a card grid launcher; each card navigates to a dedicated full-screen page

### Card List on Hub

| Card | Route | Description |
|------|-------|-------------|
| 转盘 | `spin_wheel` | "转动命运之轮" |
| 硬币 | `coin` | "抛一枚命运硬币" |
| 骰子 | `dice` | "掷出你的答案" |
| Yes/No | `yes_no` | "让宇宙替你决定" |
| 指尖轮盘 | `finger_roulette` | "多人淘汰，谁是天选" |
| 数据洞察 | `stats` | "看看你的决策模式" |

### Route Changes

**AppDestinations.kt:**
- Remove `COIN_DICE` route
- Add `HUB`, `COIN`, `DICE`, `FINGER_ROULETTE`, `STATS` routes

**AppNavGraph.kt:**
- Start destination changes from `spin_wheel` to `hub`
- Each feature page uses `popUpTo(hub)` to avoid return stack buildup
- Coin and Dice become independent navigation targets with their own screens

**Bottom Navigation:**
- `NavigationBar` with 3 items: Hub (home icon), History (history icon), Settings (settings icon)
- Hub uses `NavigationSuiteScaffold` start destination

---

## 2. Hub Screen (HubScreen.kt)

### Layout
- `LazyVerticalGrid` with 2 columns
- Each card: icon + name + one-line description
- Card style: `Card` with `Elevation`, Material 3 surface color
- Tap → `navController.navigate(route)`
- Header: app name + optional greeting based on time of day

### Files
- New: `ui/hub/HubScreen.kt`
- Modify: `navigation/AppDestinations.kt`
- Modify: `navigation/AppNavGraph.kt`
- Modify: `MainActivity.kt` (bottom nav changes)

---

## 3. Coin/Dice Split

### Current
- `CoinDiceScreen.kt` contains both coin and dice with internal `TabRow`
- `Coin3DComposable.kt` and `Dice3DComposable.kt` are already separate composables

### Target
- New: `ui/coin/CoinScreen.kt` — wraps `Coin3DFlip` with its own top bar and controls
- New: `ui/dice/DiceScreen.kt` — wraps `Dice3DRoll` with its own top bar and controls
- Shared: `ImageCustomizationSheet`, `CircularCropDialog` stay in `ui/coindice/` or move to `ui/components/`
- `CoinDiceViewModel` can be split or kept shared (it manages both coin and dice state)

### Decision
- Split `CoinDiceViewModel` into `CoinViewModel` and `DiceViewModel` for clean separation
- Move shared composables (`ImageCustomizationSheet`, `CircularCropDialog`) to `ui/components/`
- `ShakeDetector` stays with dice (only dice uses shake)

---

## 4. Finger Roulette (FingerRoulette)

### Core Interaction
1. 2-6 people place fingers on screen simultaneously
2. App displays circular markers at each finger position (numbered 1-N)
3. User taps "Start" button → elimination rounds begin
4. Each round: one random finger is highlighted, flash animation + sound, marker shrinks and disappears
5. Remaining fingers get a brief pause (suspense)
6. Last finger remaining = winner, celebration animation

### Gesture Detection
- `Modifier.pointerInput(Unit) { awaitPointerEventScope { ... } }` continuous monitoring
- Track `Map<PointerId, Offset>` for active pointers
- `PointerEventType.Down` → add to map, `PointerEventType.Up` → remove from map
- `PointerEventType.Move` → update position
- Start button disabled when finger count < 2

### Elimination Animation
- Selected marker: scale up (1.0 → 1.5) → red flash (3 blinks) → scale down to 0 + fade out
- Other markers: brief pause (alpha 0.5) during elimination
- Timing: `animateTo` with `tween(600ms)` per phase
- Between rounds: 1.5s initially, decreasing by 0.2s each round (tension builds)

### UI Layout
```
┌─────────────────────────────┐
│  [Back]  指尖轮盘           │
│                             │
│        ┌───┐    ┌───┐      │
│        │ 1 │    │ 2 │      │
│        └───┘    └───┘      │
│           ┌───┐             │
│           │ 3 │             │
│           └───┘             │
│                             │
│   请放置 2-6 根手指          │
│                             │
│     [  开始淘汰  ]          │
│                             │
│  当前检测到: 3 根手指        │
└─────────────────────────────┘
```

### Winner Screen
- Confetti-like particle animation (Compose Canvas)
- Winner marker grows large with golden glow
- Text: "天选之人！" with bounce animation
- "再来一局" button resets state

### Files
- New: `ui/fingerroulette/FingerRouletteScreen.kt`
- New: `ui/fingerroulette/FingerRouletteViewModel.kt`
- New: `ui/fingerroulette/FingerMarker.kt` (marker composable)
- New: `ui/fingerroulette/EliminationAnimator.kt` (animation logic)

---

## 5. Haptic + Sound System (AudioHapticManager)

### Architecture
- Singleton class: `AudioHapticManager`
- Wraps `SoundPool` for audio + `HapticFeedback` for vibration
- Initialized in `Application.onCreate()` or lazily on first use

### Feedback Matrix

| Scene | Haptic | Sound |
|-------|--------|-------|
| Spin wheel rotating | `TextHandleMove` every 100ms | None |
| Spin wheel stopped | `LongPress` | Ding tone |
| Coin flip | `Confirm` | Metal clink |
| Dice bounce | `TextHandleMove` per bounce | Wood tap |
| Yes/No result | `LongPress` | Mystery chime |
| Finger roulette elimination | `LongPress` | Tense drum hit |
| Finger roulette winner | `LongPress` sequence (3x) | Crowd cheer |

### Sound Files
- Place 6-8 short audio files (< 100KB each) in `res/raw/`
- `SoundPool` with maxStreams = 2, load on first playback
- Files: `spin_ding.mp3`, `coin_clink.mp3`, `dice_tap.mp3`, `yesno_chime.mp3`, `elimination_drum.mp3`, `winner_cheer.mp3`

### Silent Mode
- Check `AudioManager.ringerMode` — if `RINGER_MODE_SILENT`, auto-mute sounds (haptics preserved)
- Settings screen: two independent toggles — "音效" and "震动", both default ON
- Toggles persisted via DataStore

### Files
- New: `audio/AudioHapticManager.kt`
- Modify: `SettingsRepository.kt` — add soundEnabled / hapticEnabled flows
- Modify: `SettingsScreen.kt` — add toggle UI
- Modify: All screen files — integrate feedback calls
- New: `res/raw/` — 6 audio files

---

## 6. Stats Dashboard (StatsScreen)

### Data Source
- Read from existing `HistoryRepository` / `HistoryEntry` list
- No new data collection needed

### Page Layout

**Section 1: Decision Overview**
- Total decisions count
- Most used method (name + count)
- Last decision timestamp

**Section 2: Method Distribution (Horizontal Bar Chart)**
- One bar per method: Spin Wheel, Coin, Dice, Yes/No, Finger Roulette
- Bar width proportional to count
- Show percentage label
- Animation: bars grow from left (0 → target width) on page enter

**Section 3: Top 5 Results**
- Ranked list of most frequently selected options
- Each item: rank, name, count

**Section 4: Hourly Distribution (Vertical Bar Chart)**
- 24 bars for each hour of the day (0-23)
- Bar height proportional to decisions in that hour
- X-axis labels: 0, 6, 12, 18, 24
- Animation: bars grow upward on page enter

### Chart Implementation
- All charts drawn with Compose `Canvas` + `drawRect` + `drawText`
- Animation via `Animatable<Float>` (0f → 1f) with `tween(800ms)`
- No third-party chart library

### Empty State
- When history is empty: centered illustration + "还没有决策记录，去试试转盘吧！" + navigate button

### ViewModel Methods
- `getMethodDistribution(): Map<DecisionMethod, Int>`
- `getTopResults(limit: Int): List<Pair<String, Int>>`
- `getHourlyDistribution(): List<Int>` (24 entries)
- `getTotalCount(): Int`
- `getMostUsedMethod(): DecisionMethod?`
- `getLastDecisionTime(): Long?`

### Files
- New: `ui/stats/StatsScreen.kt`
- New: `ui/stats/StatsViewModel.kt`
- New: `ui/stats/charts/BarChart.kt` (reusable horizontal bar)
- New: `ui/stats/charts/Histogram.kt` (vertical 24-hour chart)
- Modify: `HistoryRepository.kt` — add query methods if needed

---

## 7. Settings Screen Updates

Add new sections:

**Sound & Haptics** (new section):
- "音效" switch → `settingsRepository.setSoundEnabled()`
- "震动" switch → `settingsRepository.setHapticEnabled()`

**Display** (existing):
- "跟随系统深色模式" toggle

**About** (existing):
- Version, Privacy Policy, Terms, Licenses

---

## 8. File Summary

| File | Action | Purpose |
|------|--------|---------|
| `ui/hub/HubScreen.kt` | **New** | Card grid launcher |
| `ui/coin/CoinScreen.kt` | **New** | Standalone coin screen |
| `ui/dice/DiceScreen.kt` | **New** | Standalone dice screen |
| `ui/fingerroulette/FingerRouletteScreen.kt` | **New** | Finger roulette main screen |
| `ui/fingerroulette/FingerRouletteViewModel.kt` | **New** | Finger roulette state management |
| `ui/fingerroulette/FingerMarker.kt` | **New** | Finger marker composable |
| `ui/fingerroulette/EliminationAnimator.kt` | **New** | Elimination animation logic |
| `ui/stats/StatsScreen.kt` | **New** | Stats dashboard |
| `ui/stats/StatsViewModel.kt` | **New** | Stats data processing |
| `ui/stats/charts/BarChart.kt` | **New** | Horizontal bar chart composable |
| `ui/stats/charts/Histogram.kt` | **New** | 24-hour histogram composable |
| `audio/AudioHapticManager.kt` | **New** | Sound + haptic singleton |
| `res/raw/*.mp3` | **New** | 6 audio effect files |
| `navigation/AppDestinations.kt` | **Modify** | New routes, remove COIN_DICE |
| `navigation/AppNavGraph.kt` | **Modify** | New nav graph with hub start |
| `MainActivity.kt` | **Modify** | 3-tab bottom nav |
| `ui/coindice/CoinDiceScreen.kt` | **Delete** | Replaced by CoinScreen + DiceScreen |
| `ui/coindice/ImageCustomizationSheet.kt` | **Move** | → `ui/components/` |
| `ui/coindice/CircularCropDialog.kt` | **Move** | → `ui/components/` |
| `viewmodel/CoinDiceViewModel.kt` | **Split** | → `CoinViewModel` + `DiceViewModel` |
| `data/repository/SettingsRepository.kt` | **Modify** | Add sound/haptic enabled flows |
| `ui/settings/SettingsScreen.kt` | **Modify** | Add sound/haptic toggles |

---

## 9. Implementation Order

1. **Navigation restructure** — Hub screen + route changes + bottom nav (foundation)
2. **Coin/Dice split** — Extract from CoinDiceScreen into independent screens
3. **AudioHapticManager** — Build sound/haptic system + settings toggles
4. **Integrate haptic/sound into existing features** — Spin wheel, coin, dice, yes/no
5. **Finger Roulette** — New feature with full gesture + animation
6. **Stats Dashboard** — Charts + data processing from history
7. **Polish** — Animations, empty states, edge cases
