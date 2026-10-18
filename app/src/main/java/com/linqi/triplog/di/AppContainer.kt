package com.linqi.triplog.di

import android.content.Context
import com.linqi.triplog.ai.AiClient
import com.linqi.triplog.data.local.SettingsStore
import com.linqi.triplog.data.local.TripDatabase
import com.linqi.triplog.data.repo.TripRepository

/** 手写依赖容器，不引入 Hilt，自用 app 保持构建简单。 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    private val database: TripDatabase by lazy { TripDatabase.get(appContext) }

    val tripRepository: TripRepository by lazy {
        TripRepository(database.tripDao())
    }

    val aiClient: AiClient by lazy { AiClient(settings) }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun of(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
