package com.github.sotfheif.weatherforecast

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree


class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}