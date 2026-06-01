# Choice Paralysis 全面代码整改设计文档

## 概述

对项目进行全面架构整改，保留现有业务逻辑和实现效果，解决以下核心问题：
- 无依赖注入，Repository 在多处直接实例化
- God ViewModel（SpinWheelViewModel 混合动画、CRUD、颜色、历史、设置）
- DataStore Preferences 存储大 JSON 序列化数据，无错误处理
- 音频震动系统调试代码多、逻辑重复、MiHaptic 反射调用分散
- 导航用纯 String 路由，无类型安全

## 阶段 1：架构骨架（Hilt + ViewModel 拆分 + Type-Safe Navigation）

### 1.1 Hilt 依赖注入

引入 Hilt 作为 DI 框架：

- `@HiltAndroidApp` 标注 Application
- `@AndroidEntryPoint` 标注 MainActivity
- `@HiltViewModel` 标注所有 ViewModel
- AppModule 提供单例：
  - `DataStore<Preferences>` (单例)
  - `SettingsRepository` (单例)
  - `HistoryRepository` (单例)
  - `AudioHapticManager` (单例)
- DatabaseModule 提供 Room 相关依赖（阶段 2）

移除所有 `AndroidViewModel` + `application` 的手动 Repository 构造。

### 1.2 ViewModel 拆分

将 `SpinWheelViewModel`（当前 God ViewModel）按职责拆分为：

| ViewModel | 职责 | 注入依赖 |
|-----------|------|----------|
| `SpinWheelVM` | 转盘动画状态、旋转触发、结果输出 | 无（纯 UI 状态） |
| `OptionsVM` | 选项 CRUD、权重管理、选项组存取 | OptionListRepository |
| `ColorSchemeVM` | 颜色方案、动态颜色刷新 | SettingsRepository |
| `SettingsVM` | 设置项（主题、震动、音效）、选项组管理 | SettingsRepository |

- 每个 VM 只暴露 `StateFlow`，不暴露 `suspend fun`
- `setXxx()` 方法内部 `viewModelScope.launch` 处理异步
- 删除 60 秒颜色刷新 hack，改为 `ColorSchemeVM` 内部定时器或 `combine` 流

### 1.3 Type-Safe Navigation

使用 Kotlin Serialization 定义路由：

```kotlin
@Serializable sealed class Route {
    @Serializable data object Hub : Route()
    @Serializable data object SpinWheel : Route()
    @Serializable data object Coin : Route()
    @Serializable data object Dice : Route()
    @Serializable data object YesNo : Route()
    @Serializable data object FingerRoulette : Route()
    @Serializable data object Stats : Route()
    @Serializable data object History : Route()
    @Serializable data object Settings : Route()
}
```

- NavHost 用 `composable<Route.SpinWheel>` 替代 `composable("spin_wheel")`
- `BottomNavDestinations` 引用 `Route` 子类而非 String
- 统一 `onBack` 回调：所有 feature screen 接收 `onBack: () -> Unit`

### 1.4 依赖关系（阶段 1 完成后）

```
MainActivity (@AndroidEntryPoint)
├── SpinWheelScreen
│   ├── SpinWheelVM (@HiltViewModel)
│   ├── OptionsVM (@HiltViewModel)
│   └── ColorSchemeVM (@HiltViewModel)
├── CoinScreen
│   └── CoinVM (@HiltViewModel)
├── DiceScreen
│   └── DiceVM (@HiltViewModel)
├── YesNoScreen
│   └── YesNoVM (@HiltViewModel)
├── FingerRouletteScreen
│   └── FingerRouletteVM (@HiltViewModel)
├── StatsScreen
│   └── StatsVM (@HiltViewModel)
├── HistoryScreen
│   └── HistoryVM (@HiltViewModel)
└── SettingsScreen
    └── SettingsVM (@HiltViewModel)
```

## 阶段 2：Room 数据层

### 2.1 数据库设计

```kotlin
@Database(
    entities = [OptionGroupEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun optionGroupDao(): OptionGroupDao
    abstract fun historyDao(): HistoryDao
}
```

### 2.2 实体设计

**OptionGroupEntity:**
- `id: Long` (PrimaryKey, autoGenerate)
- `name: String`
- `options: String` (JSON 序列化的 List<OptionItem>)
- `weights: String` (JSON 序列化的 List<Int>)
- `createdAt: Long`
- `updatedAt: Long`

**HistoryEntity:**
- `id: Long` (PrimaryKey, autoGenerate)
- `method: String` (DecisionMethod enum name)
- `result: String`
- `optionsSnapshot: String` (JSON 快照)
- `timestamp: Long`

### 2.3 DAO 设计

```kotlin
@Dao
interface OptionGroupDao {
    @Query("SELECT * FROM option_groups ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<OptionGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OptionGroupEntity)

    @Delete
    suspend fun delete(entity: OptionGroupEntity)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getRecent(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trimOld()
}
```

### 2.4 迁移策略

采用「启动时一次性迁移」方案：

1. App 启动时检查 DataStore 中是否存在旧格式数据
2. 如果存在：
   - 读取 `OPTION_GROUPS` JSON → 解析为 `List<OptionGroupEntity>` → 批量插入 Room
   - 读取 `HISTORY` JSON → 解析为 `List<HistoryEntity>` → 批量插入 Room
   - 删除 DataStore 中已迁移的 key
   - 写入 `MIGRATION_COMPLETED = true` 标记
3. 后续启动跳过迁移

设置项（主题、震动、音效、自定义颜色）保留 DataStore Preferences，不迁移。

### 2.5 清理

- 删除 `AppDataStore.kt` 中未使用的 `OPTION_LISTS` key
- `HistoryRepository` 改为基于 Room DAO，自动限制 100 条
- `SettingsRepository` 保留 DataStore，但规范化 key 结构

## 阶段 3：音频震动策略模式

### 3.1 接口设计

```kotlin
interface HapticEngine {
    fun isAvailable(): Boolean
    fun playTick()
    fun playEffect(effect: HapticEffect)
    fun release()
}

enum class HapticEffect {
    CLICK,      // 按钮点击
    TICK,       // 轮盘边界
    THUD,       // 重击（硬币落地）
    RISE,       // 上升感（消除）
    CELEBRATION // 庆祝（赢家）
}
```

### 3.2 实现层次

```
HapticEngine (interface)
├── MiHapticEngine      — 小米设备，反射调用 MiHaptic API
│   ├── 双包名支持 (miui.os / android.os)
│   ├── 缓存 HapticPlayer 实例
│   └── playTick() 使用预缓存 effect
├── CompositionEngine   — Android 11+，VibrationEffect.Composition
│   └── 使用 PRIMITIVE_CLICK/TICK/THUD 等原语
└── LegacyEngine        — 基础 Vibrator API
    └── createOneShot / createWaveform
```

### 3.3 AudioHapticManager 重构

```kotlin
@Singleton
class AudioHapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hapticEngine: HapticEngine,
) {
    // 只负责：
    // 1. SoundPool 音效加载和播放
    // 2. 调用 hapticEngine.playEffect/playTick
    // 3. 音效/震动开关管理

    // 不再包含：
    // - 任何反射逻辑（已移至 HapticEngine 实现）
    // - Vibrator/VibratorManager 直接访问
    // - Composition API 调用
}
```

### 3.4 Hilt Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object HapticModule {
    @Provides
    @Singleton
    fun provideHapticEngine(@ApplicationContext context: Context): HapticEngine {
        val mi = MiHapticEngine(context)
        if (mi.isAvailable()) return mi
        val comp = CompositionEngine(context)
        if (comp.isAvailable()) return comp
        return LegacyEngine(context)
    }
}
```

### 3.5 清理

- 删除所有 `Log.d("AudioHaptic", ...)` 和 `Log.d("MiHaptic", ...)` 调试日志
- 删除 `AudioHapticManager` 中的 `tick()` 方法（已被 `playFeedback(WHEEL_TICK)` 替代）
- 统一 `playFeedback(SoundEffect)` 为唯一音效触发入口

## 阶段 4：UI 层清理

### 4.1 通用清理

- 删除 `SpinWheelComposable.kt` 中的 `toArgb()` 扩展（使用 Compose 内置 `Color.toArgb()`）
- 清理 `SpinWheelScreen.kt` 中的重复代码
- 统一所有 feature screen 的 `onBack` 回调模式
- 清理 `WheelDesign` / `WheelDesignPreviews` 中的冗余代码

### 4.2 组件整理

- `StandardEasing` 保持不变（自定义缓动曲线）
- `AnimatedResult` 检查是否需要保留
- `ShakeDetector` 保持不变（硬件功能）

## 执行顺序

1. **阶段 1.1**：引入 Hilt（gradle 依赖 + Application + Module）
2. **阶段 1.2**：拆分 ViewModel（每个功能独立 VM）
3. **阶段 1.3**：Type-Safe Navigation
4. **阶段 2.1-2.3**：引入 Room（Database + Entity + DAO）
5. **阶段 2.4-2.5**：数据迁移 + DataStore 清理
6. **阶段 3.1-3.3**：音频震动策略模式
7. **阶段 3.4-3.5**：清理调试代码
8. **阶段 4**：UI 层清理

每个阶段完成后应能编译运行，功能不受影响。

## 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| Hilt 编译时间增加 | 使用 KSP 替代 KAPT |
| Room 迁移数据丢失 | 先备份 DataStore 文件，提供回滚方案 |
| MiHaptic 反射在新架构中失败 | 保持 `isAvailable()` 检查 + fallback |
| ViewModel 拆分后状态同步问题 | 使用 `SharedFlow` 或 `StateFlow` 共享状态 |
| Type-Safe Navigation 参数传递 | 初期不传参数，后续按需添加 |

## 验收标准

- [ ] 所有功能正常运行（转盘、硬币、骰子、Yes-No、指尖轮盘、统计、历史、设置）
- [ ] 音效和震动正常工作
- [ ] MiHaptic 引擎正常检测和使用
- [ ] 导航流程不变
- [ ] 无编译警告（除 deprecation 外）
- [ ] 调试日志全部移除
