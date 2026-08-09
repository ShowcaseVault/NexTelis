package com.showcasevault.nextelis.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {

    const val REQUEST_CODE = 101

    // READ_PHONE_STATE behaves differently on Android 11
    // so we separate it and request only what we need
    private fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
        )
        // Only add READ_PHONE_STATE on Android 11 and below
        // Android 12+ splits this into READ_BASIC_PHONE_STATE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        return permissions.toTypedArray()
    }

    fun allGranted(activity: Activity): Boolean {
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(
                activity, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestAll(activity: Activity) {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(
                activity, it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missing.toTypedArray(),
                REQUEST_CODE
            )
        }
    }

    // Call this to log exactly what's granted and what's not
    fun logStatus(activity: Activity) {
        requiredPermissions().forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            android.util.Log.d(
                "PermissionManager",
                "${permission.substringAfterLast('.')} → ${if (granted) "GRANTED" else "DENIED"}"
            )
        }
    }
}