package com.linqi.triplog

import android.app.Application
import com.linqi.triplog.di.AppContainer

class TripApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.of(this)
    }
}
