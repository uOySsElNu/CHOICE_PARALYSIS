package com.choiceparalysis.turntable.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.data.datastore.dataStore
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.OptionListRepository
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
    fun provideHistoryRepository(dataStore: DataStore<Preferences>): HistoryRepository =
        HistoryRepository(dataStore)

    @Provides
    @Singleton
    fun provideOptionListRepository(dataStore: DataStore<Preferences>): OptionListRepository =
        OptionListRepository(dataStore)

    @Provides
    @Singleton
    fun provideAudioHapticManager(@ApplicationContext context: Context): AudioHapticManager =
        AudioHapticManager.getInstance(context)
}
