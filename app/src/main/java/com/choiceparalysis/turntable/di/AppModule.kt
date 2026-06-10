package com.choiceparalysis.turntable.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.data.datastore.dataStore
import com.choiceparalysis.turntable.data.local.DataMigration
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
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
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(dataStore, context)

    @Provides
    @Singleton
    fun provideHistoryRepository(historyDao: HistoryDao): HistoryRepository =
        HistoryRepository(historyDao)

    @Provides
    @Singleton
    fun provideDataMigration(
        dataStore: DataStore<Preferences>,
        historyDao: HistoryDao
    ): DataMigration = DataMigration(dataStore, historyDao)

    @Provides
    @Singleton
    fun provideAudioHapticManager(@ApplicationContext context: Context): AudioHapticManager =
        AudioHapticManager.getInstance(context)
}
