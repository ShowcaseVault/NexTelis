package com.showcasevault.nextelis.home

import android.content.Intent
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.account.AccountStatus
import com.showcasevault.nextelis.account.PhoneAccountManager
import com.showcasevault.nextelis.session.SessionStore

/**
 * Dashboard shown once the user is paired, has granted permissions, and
 * has a NexTelis number. Registers the PhoneAccount here since this is
 * the first screen reached with all prerequisites satisfied.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var textGreeting: TextView
    private lateinit var textNumberValue: TextView
    private lateinit var textAccountStatus: TextView
    private lateinit var textAccountHint: TextView
    private lateinit var btnEnableAccount: Button
    private lateinit var btnTestCall: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        textGreeting = findViewById(R.id.textGreeting)
        textNumberValue = findViewById(R.id.textNumberValue)
        textAccountStatus = findViewById(R.id.textAccountStatus)
        textAccountHint = findViewById(R.id.textAccountHint)
        btnEnableAccount = findViewById(R.id.btnEnableAccount)
        btnTestCall = findViewById(R.id.btnTestCall)

        btnEnableAccount.setOnClickListener { openCallingAccountsSettings() }
        btnTestCall.setOnClickListener { openNativeDialer() }

        PhoneAccountManager.register(this)
    }

    override fun onResume() {
        super.onResume()
        renderGreeting()
        renderNumber()
        renderAccountStatus()
    }

    private fun renderGreeting() {
        val name = SessionStore.getDisplayName(this).orEmpty()
        textGreeting.text = getString(R.string.home_greeting, name).uppercase()
    }

    private fun renderNumber() {
        textNumberValue.text = SessionStore.getNumberValue(this).orEmpty()
    }

    private fun renderAccountStatus() {
        when (PhoneAccountManager.getStatus(this)) {
            AccountStatus.ENABLED -> {
                textAccountStatus.text = getString(R.string.status_account_enabled)
                textAccountStatus.setBackgroundResource(R.drawable.bg_pill_good)
                textAccountStatus.setTextColor(getColor(R.color.status_good))
                textAccountHint.visibility = TextView.GONE
                btnEnableAccount.visibility = Button.GONE
            }
            AccountStatus.REGISTERED_DISABLED,
            AccountStatus.NOT_REGISTERED -> {
                textAccountStatus.text = getString(R.string.status_account_registered_not_enabled)
                textAccountStatus.setBackgroundResource(R.drawable.bg_pill_warn)
                textAccountStatus.setTextColor(getColor(R.color.status_warn))
                textAccountHint.visibility = TextView.VISIBLE
                btnEnableAccount.visibility = Button.VISIBLE
            }
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
