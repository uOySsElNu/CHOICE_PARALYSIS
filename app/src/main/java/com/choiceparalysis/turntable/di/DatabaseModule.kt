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
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideOptionGroupDao(database: AppDatabase): OptionGroupDao =
        database.optionGroupDao()

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao =
        database.historyDao()
}
