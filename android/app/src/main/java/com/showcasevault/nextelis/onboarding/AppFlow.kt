package com.showcasevault.nextelis.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.showcasevault.nextelis.home.HomeActivity
import com.showcasevault.nextelis.permissions.PermissionManager
import com.showcasevault.nextelis.permissions.PermissionsActivity
import com.showcasevault.nextelis.session.SessionStore

/**
 * Decides which screen the user should see next, based on onboarding
 * progress: pairing -> permissions -> number -> home. Centralized here so
 * no single activity has to know the whole sequence.
 */
object AppFlow {

    fun routeFromLaunch(activity: Activity) {
        activity.startActivity(nextStepIntent(activity))
        activity.finish()
    }

    fun nextStepIntent(context: Context): Intent {
        return when {
            !SessionStore.isPaired(context) -> Intent(context, PairDeviceActivity::class.java)
            !PermissionManager.allGranted(context) -> Intent(context, PermissionsActivity::class.java)
            !SessionStore.hasNumber(context) -> Intent(context, GetNumberActivity::class.java)
            else -> Intent(context, HomeActivity::class.java)
        }
    }
}
