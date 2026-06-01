package com.choiceparalysis.turntable

import android.app.Application
import com.choiceparalysis.turntable.data.local.DataMigration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ChoiceParalysisApp : Application() {

    @Inject lateinit var dataMigration: DataMigration

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                dataMigration.migrateIfNeeded()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
