package com.showcasevault.nextelis

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.showcasevault.nextelis.account.AccountStatus
import com.showcasevault.nextelis.account.PhoneAccountManager
import com.showcasevault.nextelis.permissions.PermissionManager

/**
 * Entry screen. Responsible only for UI wiring — account/permission logic
 * lives in [PhoneAccountManager] and [PermissionManager].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        findViewById<Button>(R.id.btnEnableAccount).setOnClickListener { openCallingAccountsSettings() }
        findViewById<Button>(R.id.btnTestCall).setOnClickListener { openNativeDialer() }

        PermissionManager.logStatus(this)
        ensurePermissionsThenRegister()
    }

    override fun onResume() {
        super.onResume()
        // Reflects any change the user made in system Settings (e.g. enabling
        // the account) while this activity was in the background.
        if (PhoneAccountManager.isRegistered(this)) {
            refreshStatusText()
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
            registerAccount()
        } else {
            statusText.text = getString(R.string.status_permissions_denied)
        }
    }

    private fun ensurePermissionsThenRegister() {
        if (PermissionManager.allGranted(this)) {
            registerAccount()
        } else {
            PermissionManager.requestAll(this)
        }
    }

    private fun registerAccount() {
        PhoneAccountManager.register(this)
        refreshStatusText()
    }

    private fun refreshStatusText() {
        val messageRes = when (PhoneAccountManager.getStatus(this)) {
            AccountStatus.ENABLED -> R.string.status_account_enabled
            AccountStatus.REGISTERED_DISABLED,
            AccountStatus.NOT_REGISTERED -> R.string.status_account_registered_not_enabled
        }
        statusText.text = getString(messageRes)
    }

    private fun openCallingAccountsSettings() {
        try {
            startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_open_calling_accounts_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNativeDialer() {
        // Opens the device's default dialer; Android shows the SIM / NexTelis
        // account chooser itself once the account is enabled.
        startActivity(Intent(Intent.ACTION_DIAL))
    }
}
