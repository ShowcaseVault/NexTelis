package com.showcasevault.nextelis.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.onboarding.AppFlow

/**
 * Screen 2 of onboarding: explains why each permission is needed before
 * requesting them, and offers a way to reach system Settings if the user
 * previously denied a permission permanently.
 */
class PermissionsActivity : AppCompatActivity() {

    private lateinit var textDenied: TextView
    private lateinit var btnGrant: Button
    private lateinit var btnOpenSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        textDenied = findViewById(R.id.textDenied)
        btnGrant = findViewById(R.id.btnGrant)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        btnGrant.setOnClickListener { PermissionManager.requestAll(this) }
        btnOpenSettings.setOnClickListener { openAppSettings() }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionManager.allGranted(this)) {
            AppFlow.routeFromLaunch(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PermissionManager.REQUEST_CODE) return

        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            AppFlow.routeFromLaunch(this)
        } else {
            textDenied.visibility = TextView.VISIBLE
            btnOpenSettings.visibility = Button.VISIBLE
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
