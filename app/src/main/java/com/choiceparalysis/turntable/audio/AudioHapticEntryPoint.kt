package com.choiceparalysis.turntable.audio

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AudioHapticEntryPoint {
    fun hapticEngine(): HapticEngine
}
