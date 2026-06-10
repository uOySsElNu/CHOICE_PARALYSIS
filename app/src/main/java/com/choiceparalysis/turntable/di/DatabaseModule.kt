package com.choiceparalysis.turntable.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.choiceparalysis.turntable.data.local.AppDatabase
import com.choiceparalysis.turntable.data.local.dao.CoinPresetDao
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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Recreate option_groups with String PK (was Long autoGenerate)
            db.execSQL("DROP TABLE IF EXISTS option_groups")
            db.execSQL("""
                CREATE TABLE option_groups (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    options TEXT NOT NULL,
                    weights TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            // Create coin_presets table
            db.execSQL("""
                CREATE TABLE coin_presets (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    headsImagePath TEXT NOT NULL,
                    tailsImagePath TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "choice_paralysis.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideOptionGroupDao(database: AppDatabase): OptionGroupDao =
        database.optionGroupDao()

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao =
        database.historyDao()

    @Provides
    fun provideCoinPresetDao(database: AppDatabase): CoinPresetDao =
        database.coinPresetDao()
}
