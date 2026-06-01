package com.choiceparalysis.turntable.di

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import com.choiceparalysis.turntable.audio.CompositionEngine
import com.choiceparalysis.turntable.audio.HapticEngine
import com.choiceparalysis.turntable.audio.LegacyEngine
import com.choiceparalysis.turntable.audio.MiHapticEngineImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HapticModule {

    @Provides
    @Singleton
    @Named("defaultVibrator")
    fun provideVibrator(@ApplicationContext context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @Provides
    @Singleton
    fun provideHapticEngine(
        @Named("defaultVibrator") vibrator: Vibrator
    ): HapticEngine {
        // Auto-detect best available engine: MiHaptic (Xiaomi) > Composition > Legacy
        val mi = MiHapticEngineImpl()
        if (mi.isAvailable()) return mi

        val composition = CompositionEngine(vibrator)
        if (composition.isAvailable()) return composition

        return LegacyEngine(vibrator)
    }
}
