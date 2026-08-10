package com.showcasevault.nextelis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.showcasevault.nextelis.onboarding.AppFlow

/**
 * Launcher entry point only — immediately routes to whichever onboarding
 * step or the home dashboard is appropriate for the current session state.
 * See [AppFlow] for the routing rules.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppFlow.routeFromLaunch(this)
    }
}
