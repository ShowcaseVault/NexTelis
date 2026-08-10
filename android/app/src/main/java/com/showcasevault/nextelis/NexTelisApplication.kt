package com.showcasevault.nextelis

import android.app.Application

/**
 * Holds the process-wide application context for singletons that need one
 * but aren't handed a Context directly (e.g. NexTelisApiClient's auth
 * interceptor). Kept minimal — this is not a DI container.
 */
class NexTelisApplication : Application() {

    companion object {
        lateinit var instance: NexTelisApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
