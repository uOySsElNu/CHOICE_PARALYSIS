package com.choiceparalysis.turntable.di

import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import com.choiceparalysis.turntable.audio.CompositionEngine
import com.choiceparalysis.turntable.audio.HapticEngine
import com.choiceparalysis.turntable.audio.LegacyEngine
import com.choiceparalysis.turntable.audio.RichTapEngineImpl
import com.choiceparalysis.turntable.audio.RichTapSdkEngineImpl
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
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        return manager.defaultVibrator
    }

    @Provides
    @Singleton
    fun provideHapticEngine(
        @ApplicationContext context: Context,
        @Named("defaultVibrator") vibrator: Vibrator
    ): HapticEngine {
        // Auto-detect best available engine:
        // RichTap SDK (AAC official) > RichTap reflection > Composition > Legacy
        val richTapSdk = RichTapSdkEngineImpl(context)
        if (richTapSdk.isAvailable()) return richTapSdk

        val richTap = RichTapEngineImpl()
        if (richTap.isAvailable()) return richTap

        val composition = CompositionEngine(vibrator)
        if (composition.isAvailable()) return composition

        return LegacyEngine(vibrator)
    }
}
