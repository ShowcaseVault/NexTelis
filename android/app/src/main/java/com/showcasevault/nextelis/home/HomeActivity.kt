package com.showcasevault.nextelis.home

import android.content.Intent
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.account.AccountStatus
import com.showcasevault.nextelis.account.PhoneAccountManager
import com.showcasevault.nextelis.session.SessionStore
import com.showcasevault.nextelis.sip.SipCallService

/**
 * Dashboard shown once the user is paired, has granted permissions, and
 * has a NexTelis number. Registers the PhoneAccount here since this is
 * the first screen reached with all prerequisites satisfied.
 *
 * The big circular button is the master on/off control:
 *  - if the calling account isn't enabled yet, tapping it sends the user
 *    to system Settings (there is no API to enable it programmatically —
 *    see docs/FINDINGS.md)
 *  - once enabled, tapping it starts/stops the SIP background service
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var textGreeting: TextView
    private lateinit var textNumberValue: TextView
    private lateinit var textAccountStatus: TextView
    private lateinit var textAccountHint: TextView
    private lateinit var btnMasterToggle: FrameLayout
    private lateinit var btnTestCall: Button

    private var serviceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        textGreeting = findViewById(R.id.textGreeting)
        textNumberValue = findViewById(R.id.textNumberValue)
        textAccountStatus = findViewById(R.id.textAccountStatus)
        textAccountHint = findViewById(R.id.textAccountHint)
        btnMasterToggle = findViewById(R.id.btnMasterToggle)
        btnTestCall = findViewById(R.id.btnTestCall)

        btnMasterToggle.setOnClickListener { onMasterToggleClicked() }
        btnTestCall.setOnClickListener { openNativeDialer() }

        PhoneAccountManager.register(this)
    }

    override fun onResume() {
        super.onResume()
        renderGreeting()
        renderNumber()
        renderAccountStatus()
    }

    private fun onMasterToggleClicked() {
        if (PhoneAccountManager.getStatus(this) != AccountStatus.ENABLED) {
            openCallingAccountsSettings()
            return
        }
        if (serviceRunning) stopSipService() else startSipService()
        renderAccountStatus()
    }

    private fun startSipService() {
        if (SessionStore.getNumberValue(this) == null || SessionStore.getSipPassword(this) == null) return
        ContextCompat.startForegroundService(this, SipCallService.intent(this))
        serviceRunning = true
    }

    private fun stopSipService() {
        stopService(SipCallService.intent(this))
        serviceRunning = false
    }

    private fun renderGreeting() {
        val name = SessionStore.getDisplayName(this).orEmpty()
        textGreeting.text = getString(R.string.home_greeting, name).uppercase()
    }

    private fun renderNumber() {
        textNumberValue.text = SessionStore.getNumberValue(this).orEmpty()
    }

    private fun renderAccountStatus() {
        val enabled = PhoneAccountManager.getStatus(this) == AccountStatus.ENABLED
        btnMasterToggle.setBackgroundResource(
            if (enabled && serviceRunning) R.drawable.bg_master_toggle else R.drawable.bg_master_toggle_off
        )

        if (enabled) {
            val statusRes = if (serviceRunning) R.string.status_account_enabled else R.string.status_service_off
            val colorRes = if (serviceRunning) R.color.status_good else R.color.text_muted
            val pillRes = if (serviceRunning) R.drawable.bg_pill_good else R.drawable.bg_pill_muted
            textAccountStatus.text = getString(statusRes)
            textAccountStatus.setBackgroundResource(pillRes)
            textAccountStatus.setTextColor(getColor(colorRes))
            textAccountHint.visibility = View.GONE
        } else {
            textAccountStatus.text = getString(R.string.status_account_registered_not_enabled)
            textAccountStatus.setBackgroundResource(R.drawable.bg_pill_warn)
            textAccountStatus.setTextColor(getColor(R.color.status_warn))
            textAccountHint.visibility = View.VISIBLE
        }
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
