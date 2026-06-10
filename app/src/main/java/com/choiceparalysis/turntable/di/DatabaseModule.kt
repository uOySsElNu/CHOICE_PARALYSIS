package com.choiceparalysis.turntable.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE history ADD COLUMN legacyId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE history ADD COLUMN listName TEXT DEFAULT NULL")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "choice_paralysis.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideOptionGroupDao(database: AppDatabase): OptionGroupDao =
        database.optionGroupDao()

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao =
        database.historyDao()
}
