package com.choiceparalysis.turntable.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.data.datastore.dataStore
import com.choiceparalysis.turntable.data.local.DataMigration
import com.choiceparalysis.turntable.data.local.dao.CoinPresetDao
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.dao.OptionGroupDao
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>,
        @ApplicationContext context: Context,
        optionGroupDao: OptionGroupDao,
        coinPresetDao: CoinPresetDao,
    ): SettingsRepository = SettingsRepository(dataStore, context, optionGroupDao, coinPresetDao)

    @Provides
    @Singleton
    fun provideHistoryRepository(historyDao: HistoryDao): HistoryRepository =
        HistoryRepository(historyDao)

    @Provides
    @Singleton
    fun provideDataMigration(
        dataStore: DataStore<Preferences>,
        historyDao: HistoryDao,
        optionGroupDao: OptionGroupDao,
        coinPresetDao: CoinPresetDao,
    ): DataMigration = DataMigration(dataStore, historyDao, optionGroupDao, coinPresetDao)

    @Provides
    @Singleton
    fun provideAudioHapticManager(@ApplicationContext context: Context): AudioHapticManager =
        AudioHapticManager.getInstance(context)
}
