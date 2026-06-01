# Choice Paralysis 全面代码整改实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对项目进行全面架构整改，引入 Hilt DI、Room 数据库、Type-Safe Navigation，拆分 God ViewModel，重构音频震动系统为策略模式，清理 UI 层代码。

**Architecture:** MVVM 模式 + Hilt 依赖注入 + Room 持久化 + Type-Safe Navigation。每个功能模块有独立 ViewModel，音频震动系统使用策略模式支持多种设备。

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, Navigation Compose (Type-Safe), DataStore Preferences, kotlinx.serialization

---

## Task 1: 引入 Hilt 依赖注入基础

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/choiceparalysis/turntable/di/AppModule.kt`
- Modify: `app/src/main/java/com/choiceparalysis/turntable/MainActivity.kt`

- [ ] **Step 1: 添加 Hilt 依赖**

在 `app/build.gradle.kts` 的 plugins 块添加：
```kotlin
alias(libs.plugins.hilt.android)
alias(libs.plugins.ksp)
```

在 dependencies 块添加：
```kotlin
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.hilt.navigation.compose)
```

在 `gradle/libs.versions.toml` 添加版本：
```toml
[versions]
hilt = "2.51.1"
ksp = "1.9.22-1.0.17"

[plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
```

- [ ] **Step 2: 创建 Application 类**

创建 `app/src/main/java/com/choiceparalysis/turntable/ChoiceParalysisApp.kt`：
```kotlin
package com.choiceparalysis.turntable

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChoiceParalysisApp : Application()
```

- [ ] **Step 3: 注册 Application 在 AndroidManifest**

修改 `app/src/main/AndroidManifest.xml`，在 `<application>` 标签添加：
```android:name=".ChoiceParalysisApp"```

- [ ] **Step 4: 创建 AppModule**

创建 `app/src/main/java/com/choiceparalysis/turntable/di/AppModule.kt`：
```kotlin
package com.choiceparalysis.turntable.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_data")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        SettingsRepository(dataStore)

    @Provides
    @Singleton
    fun provideHistoryRepository(dataStore: DataStore<Preferences>): HistoryRepository =
        HistoryRepository(dataStore)

    @Provides
    @Singleton
    fun provideAudioHapticManager(@ApplicationContext context: Context): AudioHapticManager =
        AudioHapticManager.getInstance(context)
}
```

- [ ] **Step 5: 修改 MainActivity 添加 @AndroidEntryPoint**

修改 `app/src/main/java/com/choiceparalysis/turntable/MainActivity.kt`：
```kotlin
package com.choiceparalysis.turntable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // ... 现有代码保持不变
        }
    }
}
```

- [ ] **Step 6: 验证编译**

运行 `./gradlew assembleDebug` 确保编译通过。

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "feat: introduce Hilt dependency injection foundation"
```

---

## Task 2: 修改 SettingsRepository 支持注入

**Files:**
- Modify: `app/src/main/java/com/choiceparalysis/turntable/data/repository/SettingsRepository.kt`
- Modify: `app/src/main/java/com/choiceparalysis/turntable/data/datastore/AppDataStore.kt`

- [ ] **Step 1: 修改 SettingsRepository 构造函数**

修改 `SettingsRepository.kt`，移除 `Context` 参数，直接接收 `DataStore<Preferences>`：
```kotlin
package com.choiceparalysis.turntable.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // ... 现有代码保持不变，但移除 context 相关代码
}
```

- [ ] **Step 2: 修改 HistoryRepository 构造函数**

同样修改 `HistoryRepository.kt`：
```kotlin
package com.choiceparalysis.turntable.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // ... 现有代码保持不变
}
```

- [ ] **Step 3: 清理 AppDataStore.kt**

删除未使用的 `OPTION_LISTS` key，保留必要的 key 定义。

- [ ] **Step 4: 验证编译**

运行 `./gradlew assembleDebug` 确保编译通过。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "refactor: make repositories injectable with DataStore dependency"
```

---

## Task 3: 拆分 SpinWheelViewModel

**Files:**
- Create: `app/src/main/java/com/choiceparalysis/turntable/viewmodel/SpinWheelVM.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/viewmodel/OptionsVM.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/viewmodel/ColorSchemeVM.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/viewmodel/SettingsVM.kt`
- Modify: `app/src/main/java/com/choiceparalysis/turntable/viewmodel/SpinWheelViewModel.kt` (最终删除)
- Modify: `app/src/main/java/com/choiceparalysis/turntable/ui/home/SpinWheelScreen.kt`

- [ ] **Step 1: 创建 SpinWheelVM（纯 UI 状态）**

创建 `SpinWheelVM.kt`：
```kotlin
package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SpinWheelVM @Inject constructor() : ViewModel() {
    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    fun setAnimating(value: Boolean) {
        _isAnimating.value = value
    }

    fun setResult(value: String?) {
        _result.value = value
    }
}
```

- [ ] **Step 2: 创建 OptionsVM（选项 CRUD）**

创建 `OptionsVM.kt`，从 SpinWheelViewModel 迁移选项相关逻辑：
```kotlin
package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OptionsVM @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _options = MutableStateFlow<List<String>>(emptyList())
    val options: StateFlow<List<String>> = _options.asStateFlow()

    private val _weights = MutableStateFlow<List<Int>>(emptyList())
    val weights: StateFlow<List<Int>> = _weights.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.currentOptions.collect { _options.value = it }
        }
        viewModelScope.launch {
            settingsRepository.currentWeights.collect { _weights.value = it }
        }
    }

    fun addOption(name: String) { /* 迁移自 SpinWheelViewModel */ }
    fun removeOption(index: Int) { /* 迁移自 SpinWheelViewModel */ }
    fun updateOptionName(index: Int, name: String) { /* 迁移自 SpinWheelViewModel */ }
    fun updateWeight(index: Int, weight: Int) { /* 迁移自 SpinWheelViewModel */ }
}
```

- [ ] **Step 3: 创建 ColorSchemeVM（颜色方案）**

创建 `ColorSchemeVM.kt`，从 SpinWheelViewModel 迁移颜色相关逻辑：
```kotlin
package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ColorSchemeVM @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _colorScheme = MutableStateFlow(WheelColorScheme.default())
    val colorScheme: StateFlow<WheelColorScheme> = _colorScheme.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.colorScheme.collect { _colorScheme.value = it }
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) { /* 迁移 */ }
    fun setSelectedPreset(preset: String) { /* 迁移 */ }
    fun setCustomColors(colors: List<Int>) { /* 迁移 */ }
}
```

- [ ] **Step 4: 创建 SettingsVM（设置项）**

创建 `SettingsVM.kt`，从 SpinWheelViewModel 迁移设置相关逻辑：
```kotlin
package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsVM @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _followSystemTheme = MutableStateFlow(true)
    val followSystemTheme: StateFlow<Boolean> = _followSystemTheme.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.soundEnabled.collect { _soundEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepository.hapticEnabled.collect { _hapticEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepository.followSystemTheme.collect { _followSystemTheme.value = it }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticEnabled(enabled) }
    }

    fun setFollowSystemTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setFollowSystemTheme(enabled) }
    }
}
```

- [ ] **Step 5: 更新 SpinWheelScreen 使用新 VMs**

修改 `SpinWheelScreen.kt`，注入多个 ViewModel：
```kotlin
@Composable
fun SpinWheelScreen(
    onBack: () -> Unit,
    spinWheelVM: SpinWheelVM = hiltViewModel(),
    optionsVM: OptionsVM = hiltViewModel(),
    colorSchemeVM: ColorSchemeVM = hiltViewModel(),
    settingsVM: SettingsVM = hiltViewModel(),
) {
    val isAnimating by spinWheelVM.isAnimating.collectAsState()
    val options by optionsVM.options.collectAsState()
    val weights by optionsVM.weights.collectAsState()
    val colorScheme by colorSchemeVM.colorScheme.collectAsState()
    // ... 使用新 VMs
}
```

- [ ] **Step 6: 验证编译并删除旧 ViewModel**

运行 `./gradlew assembleDebug` 确保编译通过后，删除 `SpinWheelViewModel.kt`。

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "refactor: split SpinWheelViewModel into focused VMs (SpinWheel, Options, ColorScheme, Settings)"
```

---

## Task 4: Type-Safe Navigation

**Files:**
- Modify: `app/src/main/java/com/choiceparalysis/turntable/navigation/AppDestinations.kt`
- Modify: `app/src/main/java/com/choiceparalysis/turntable/navigation/AppNavGraph.kt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 添加 kotlinx-serialization 依赖**

在 `app/build.gradle.kts` 确保已有：
```kotlin
implementation(libs.kotlinx.serialization.json)
```

- [ ] **Step 2: 定义 Type-Safe Routes**

修改 `AppDestinations.kt`：
```kotlin
package com.choiceparalysis.turntable.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
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

enum class BottomNavDestinations(
    val label: String,
    val icon: ImageVector,
    val route: Route
) {
    HUB("首页", Icons.Default.Home, Route.Hub),
    HISTORY("历史", Icons.Default.History, Route.History),
    SETTINGS("设置", Icons.Default.Settings, Route.Settings)
}
```

- [ ] **Step 3: 更新 NavGraph 使用 Type-Safe Routes**

修改 `AppNavGraph.kt`：
```kotlin
NavHost(
    navController = navController,
    startDestination = Route.Hub
) {
    composable<Route.Hub> {
        HubScreen(onNavigate = { route -> navController.navigate(route) })
    }
    composable<Route.SpinWheel> {
        SpinWheelScreen(onBack = { navController.popBackStack() })
    }
    // ... 其他路由
}
```

- [ ] **Step 4: 更新 HubScreen 导航调用**

修改 `HubScreen.kt`，使用 `Route` 而非 String：
```kotlin
data class FeatureCard(
    val title: String,
    val icon: ImageVector,
    val route: Route  // 改为 Route 类型
)
```

- [ ] **Step 5: 验证编译**

运行 `./gradlew assembleDebug` 确保编译通过。

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor: implement type-safe navigation with Kotlin Serialization routes"
```

---

## Task 5: 引入 Room 数据库

**Files:**
- Create: `app/src/main/java/com/choiceparalysis/turntable/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/data/local/entity/OptionGroupEntity.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/data/local/entity/HistoryEntity.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/data/local/dao/OptionGroupDao.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/data/local/dao/HistoryDao.kt`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/choiceparalysis/turntable/di/DatabaseModule.kt`

- [ ] **Step 1: 添加 Room 依赖**

在 `app/build.gradle.kts` 添加：
```kotlin
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
```

在 `gradle/libs.versions.toml` 添加：
```toml
[versions]
room = "2.6.1"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

- [ ] **Step 2: 创建 Entity 类**

创建 `OptionGroupEntity.kt`：
```kotlin
package com.choiceparalysis.turntable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "option_groups")
data class OptionGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val options: String,
    val weights: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

创建 `HistoryEntity.kt`：
```kotlin
package com.choiceparalysis.turntable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: String,
    val result: String,
    val optionsSnapshot: String,
    val timestamp: Long
)
```

- [ ] **Step 3: 创建 DAO 接口**

创建 `OptionGroupDao.kt`：
```kotlin
package com.choiceparalysis.turntable.data.local.dao

import androidx.room.*
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OptionGroupDao {
    @Query("SELECT * FROM option_groups ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<OptionGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OptionGroupEntity)

    @Delete
    suspend fun delete(entity: OptionGroupEntity)
}
```

创建 `HistoryDao.kt`：
```kotlin
package com.choiceparalysis.turntable.data.local.dao

import androidx.room.*
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

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

- [ ] **Step 4: 创建 AppDatabase**

创建 `AppDatabase.kt`：
```kotlin
package com.choiceparalysis.turntable.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.dao.OptionGroupDao
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity

@Database(
    entities = [OptionGroupEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun optionGroupDao(): OptionGroupDao
    abstract fun historyDao(): HistoryDao
}
```

- [ ] **Step 5: 创建 DatabaseModule**

创建 `DatabaseModule.kt`：
```kotlin
package com.choiceparalysis.turntable.di

import android.content.Context
import androidx.room.Room
import com.choiceparalysis.turntable.data.local.AppDatabase
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.dao.OptionGroupDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "choice_paralysis.db")
            .build()

    @Provides
    fun provideOptionGroupDao(database: AppDatabase): OptionGroupDao =
        database.optionGroupDao()

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao =
        database.historyDao()
}
```

- [ ] **Step 6: 验证编译**

运行 `./gradlew assembleDebug` 确保编译通过。

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "feat: introduce Room database with OptionGroup and History entities"
```

---

## Task 6: 数据迁移到 Room

**Files:**
- Create: `app/src/main/java/com/choiceparalysis/turntable/data/local/DataMigration.kt`
- Modify: `app/src/main/java/com/choiceparalysis/turntable/data/repository/HistoryRepository.kt`
- Modify: `app/src/main/java/com/choiceparalysis/turntable/di/AppModule.kt`

- [ ] **Step 1: 创建数据迁移工具**

创建 `DataMigration.kt`：
```kotlin
package com.choiceparalysis.turntable.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.dao.OptionGroupDao
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class DataMigration(
    private val dataStore: DataStore<Preferences>,
    private val optionGroupDao: OptionGroupDao,
    private val historyDao: HistoryDao
) {
    private val MIGRATION_COMPLETED = booleanPreferencesKey("migration_completed")

    suspend fun migrateIfNeeded() {
        val prefs = dataStore.data.first()
        if (prefs[MIGRATION_COMPLETED] == true) return

        // 迁移选项组
        val optionGroupsJson = prefs[stringPreferencesKey("option_groups")]
        if (optionGroupsJson != null) {
            val groups = Json.decodeFromString<List<OptionGroup>>(optionGroupsJson)
            groups.forEach { group ->
                optionGroupDao.upsert(
                    OptionGroupEntity(
                        name = group.name,
                        options = Json.encodeToString(group.options),
                        weights = Json.encodeToString(group.weights),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // 迁移历史记录
        val historyJson = prefs[stringPreferencesKey("history")]
        if (historyJson != null) {
            val entries = Json.decodeFromString<List<HistoryEntry>>(historyJson)
            entries.forEach { entry ->
                historyDao.insert(
                    HistoryEntity(
                        method = entry.method.name,
                        result = entry.result,
                        optionsSnapshot = Json.encodeToString(entry.options),
                        timestamp = entry.timestamp
                    )
                )
            }
        }

        // 标记迁移完成
        dataStore.edit { it[MIGRATION_COMPLETED] = true }
    }
}
```

- [ ] **Step 2: 更新 HistoryRepository 使用 Room**

修改 `HistoryRepository.kt`，注入 `HistoryDao`：
```kotlin
@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    val recentHistory: Flow<List<HistoryEntity>> = historyDao.getRecent()

    suspend fun addEntry(method: DecisionMethod, result: String, options: List<String>) {
        historyDao.insert(
            HistoryEntity(
                method = method.name,
                result = result,
                optionsSnapshot = Json.encodeToString(options),
                timestamp = System.currentTimeMillis()
            )
        )
        historyDao.trimOld()
    }
}
```

- [ ] **Step 3: 更新 AppModule 提供迁移**

修改 `AppModule.kt`，添加迁移提供：
```kotlin
@Provides
@Singleton
fun provideDataMigration(
    dataStore: DataStore<Preferences>,
    optionGroupDao: OptionGroupDao,
    historyDao: HistoryDao
): DataMigration = DataMigration(dataStore, optionGroupDao, historyDao)
```

- [ ] **Step 4: 在 Application 中触发迁移**

修改 `ChoiceParalysisApp.kt`：
```kotlin
@HiltAndroidApp
class ChoiceParalysisApp : Application() {
    @Inject lateinit var dataMigration: DataMigration

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            dataMigration.migrateIfNeeded()
        }
    }
}
```

- [ ] **Step 5: 验证编译**

运行 `./gradlew assembleDebug` 确保编译通过。

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: migrate data from DataStore to Room with automatic migration"
```

---

## Task 7: 音频震动策略模式

**Files:**
- Create: `app/src/main/java/com/choiceparalysis/turntable/audio/HapticEngine.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/audio/MiHapticEngineImpl.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/audio/CompositionEngine.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/audio/LegacyEngine.kt`
- Create: `app/src/main/java/com/choiceparalysis/turntable/di/HapticModule.kt`
- Modify: `app/src/main/java/com/choiceparalysis/turntable/audio/AudioHapticManager.kt`
- Delete: `app/src/main/java/com/choiceparalysis/turntable/audio/MiHapticEngine.kt`

- [ ] **Step 1: 定义 HapticEngine 接口**

创建 `HapticEngine.kt`：
```kotlin
package com.choiceparalysis.turntable.audio

interface HapticEngine {
    fun isAvailable(): Boolean
    fun playTick()
    fun playEffect(effect: HapticEffect)
    fun release()
}

enum class HapticEffect {
    CLICK,
    TICK,
    THUD,
    RISE,
    CELEBRATION
}
```

- [ ] **Step 2: 实现 MiHapticEngineImpl**

创建 `MiHapticEngineImpl.kt`，从现有 `MiHapticEngine.kt` 迁移：
```kotlin
package com.choiceparalysis.turntable.audio

import android.content.Context
import android.util.Log

class MiHapticEngineImpl(private val context: Context) : HapticEngine {
    // 迁移现有 MiHapticEngine 的反射逻辑
    // 保持双包名支持 (miui.os / android.os)
    // 保持缓存 HapticPlayer 实例

    override fun isAvailable(): Boolean { /* 现有逻辑 */ }
    override fun playTick() { /* 现有逻辑 */ }
    override fun playEffect(effect: HapticEffect) {
        when (effect) {
            HapticEffect.CLICK -> playTransient(100, 50)
            HapticEffect.TICK -> playTick()
            HapticEffect.THUD -> playTransient(100, 30)
            HapticEffect.RISE -> playContinuous(80, 60, 200)
            HapticEffect.CELEBRATION -> playTransient(100, 100)
        }
    }
    override fun release() { /* 清理资源 */ }
}
```

- [ ] **Step 3: 实现 CompositionEngine**

创建 `CompositionEngine.kt`：
```kotlin
package com.choiceparalysis.turntable.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class CompositionEngine(private val context: Context) : HapticEngine {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            val supported = vibrator.arePrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK
            )
            supported.any { it }
        } catch (_: Exception) { false }
    }

    override fun playTick() {
        vibrator.vibrate(
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 0)
                .compose()
        )
    }

    override fun playEffect(effect: HapticEffect) {
        val primitive = when (effect) {
            HapticEffect.CLICK -> VibrationEffect.Composition.PRIMITIVE_CLICK
            HapticEffect.TICK -> VibrationEffect.Composition.PRIMITIVE_TICK
            HapticEffect.THUD -> VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            HapticEffect.RISE -> VibrationEffect.Composition.PRIMITIVE_TICK
            HapticEffect.CELEBRATION -> VibrationEffect.Composition.PRIMITIVE_CLICK
        }
        val intensity = when (effect) {
            HapticEffect.RISE -> 0.5f
            else -> 1.0f
        }
        vibrator.vibrate(
            VibrationEffect.startComposition()
                .addPrimitive(primitive, intensity, 0)
                .compose()
        )
    }

    override fun release() {}
}
```

- [ ] **Step 4: 实现 LegacyEngine**

创建 `LegacyEngine.kt`：
```kotlin
package com.choiceparalysis.turntable.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class LegacyEngine(private val context: Context) : HapticEngine {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun isAvailable(): Boolean = true

    override fun playTick() {
        vibrator.vibrate(VibrationEffect.createOneShot(15, 200))
    }

    override fun playEffect(effect: HapticEffect) {
        val duration = when (effect) {
            HapticEffect.CLICK -> 20L
            HapticEffect.TICK -> 15L
            HapticEffect.THUD -> 30L
            HapticEffect.RISE -> 50L
            HapticEffect.CELEBRATION -> 100L
        }
        val amplitude = when (effect) {
            HapticEffect.RISE -> 150
            else -> 255
        }
        vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
    }

    override fun release() {}
}
```

- [ ] **Step 5: 创建 HapticModule**

创建 `HapticModule.kt`：
```kotlin
package com.choiceparalysis.turntable.di

import android.content.Context
import com.choiceparalysis.turntable.audio.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HapticModule {

    @Provides
    @Singleton
    fun provideHapticEngine(@ApplicationContext context: Context): HapticEngine {
        val mi = MiHapticEngineImpl(context)
        if (mi.isAvailable()) return mi
        val comp = CompositionEngine(context)
        if (comp.isAvailable()) return comp
        return LegacyEngine(context)
    }
}
```

- [ ] **Step 6: 重构 AudioHapticManager**

修改 `AudioHapticManager.kt`，注入 `HapticEngine`：
```kotlin
@Singleton
class AudioHapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hapticEngine: HapticEngine
) {
    // 只保留 SoundPool 逻辑
    // 删除所有反射代码
    // 删除 tick() 方法
    // playFeedback 调用 hapticEngine.playEffect/playTick

    fun playFeedback(effect: SoundEffect) {
        // 播放音效
        playSound(effect)
        // 播放震动
        if (_hapticEnabled.value) {
            when (effect) {
                SoundEffect.WHEEL_TICK -> hapticEngine.playTick()
                else -> hapticEngine.playEffect(effect.toHapticEffect())
            }
        }
    }

    private fun SoundEffect.toHapticEffect(): HapticEffect = when (this) {
        SoundEffect.SPIN_DING -> HapticEffect.CELEBRATION
        SoundEffect.YESNO_CHIME -> HapticEffect.CLICK
        SoundEffect.ELIMINATION_DRUM -> HapticEffect.THUD
        SoundEffect.WINNER_CHEER -> HapticEffect.CELEBRATION
        SoundEffect.WHEEL_TICK -> HapticEffect.TICK
        SoundEffect.DICE_ROLL -> HapticEffect.THUD
        SoundEffect.COIN_BUTTON -> HapticEffect.CLICK
        SoundEffect.COIN_DRAG -> HapticEffect.THUD
    }
}
```

- [ ] **Step 7: 删除旧 MiHapticEngine**

删除 `MiHapticEngine.kt` 文件。

- [ ] **Step 8: 验证编译**

运行 `./gradlew assembleDebug` 确保编译通过。

- [ ] **Step 9: 提交**

```bash
git add -A
git commit -m "refactor: implement haptic strategy pattern with MiHaptic/Composition/Legacy engines"
```

---

## Task 8: UI 层清理

**Files:**
- Modify: `app/src/main/java/com/choiceparalysis/turntable/ui/home/SpinWheelComposable.kt`
- Modify: `app/src/main/java/com/choiceparalysis/turntable/ui/home/SpinWheelScreen.kt`
- Modify: 各 feature screen 添加 `@AndroidEntryPoint`

- [ ] **Step 1: 删除 toArgb 扩展函数**

在 `SpinWheelComposable.kt` 中删除 `toArgb()` 扩展函数，使用 Compose 内置的 `Color.toArgb()`。

- [ ] **Step 2: 清理调试日志**

在所有文件中删除 `Log.d("AudioHaptic", ...)` 和 `Log.d("MiHaptic", ...)` 调试日志。

- [ ] **Step 3: 统一 onBack 回调**

确保所有 feature screen 都有 `onBack: () -> Unit` 参数。

- [ ] **Step 4: 添加 @AndroidEntryPoint 到各 Screen**

在各 Screen 添加 `@AndroidEntryPoint` 注解（如果需要直接注入 ViewModel）。

- [ ] **Step 5: 验证编译**

运行 `./gradlew assembleDebug` 确保编译通过。

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "cleanup: remove debug logs, standardize onBack callbacks, use compose Color.toArgb"
```

---

## Task 9: 最终验证

- [ ] **Step 1: 编译检查**

运行 `./gradlew assembleDebug` 确保无编译错误。

- [ ] **Step 2: 安装到设备**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: 功能测试**

测试以下功能：
1. 转盘旋转（按钮 + 拖动）
2. 硬币翻转
3. 骰子滚动
4. Yes/No 决策
5. 指尖轮盘
6. 统计页面
7. 历史记录
8. 设置页面
9. 音效和震动
10. MiHaptic 引擎检测

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "chore: final verification and cleanup for full refactoring"
```
