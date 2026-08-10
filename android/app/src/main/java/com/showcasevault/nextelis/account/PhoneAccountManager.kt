package com.showcasevault.nextelis.account

import android.content.ComponentName
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import com.showcasevault.nextelis.telecom.NexTelisConnectionService

object PhoneAccountManager {

    private const val TAG = "PhoneAccountManager"
    private const val ACCOUNT_ID = "nextelis_account"

    fun getHandle(context: Context): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context, NexTelisConnectionService::class.java),
            ACCOUNT_ID
        )
    }

    fun register(context: Context) {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val handle = getHandle(context)

        val account = PhoneAccount.builder(handle, "NexTelis")
            .setCapabilities(
                PhoneAccount.CAPABILITY_CALL_PROVIDER or      // appear as a calling provider
                        PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS // required to show in chooser
            )
            .build()

        telecom.registerPhoneAccount(account)
        Log.d(TAG, "PhoneAccount registered: $handle")
    }

    fun isRegistered(context: Context): Boolean {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return telecom.getPhoneAccount(getHandle(context)) != null
    }

    // registerPhoneAccount() alone leaves the account disabled — the user
    // must flip it on in Settings > Calling accounts before it appears in
    // the dialer's "call using" chooser. This never touches the real SIM account.
    fun isEnabled(context: Context): Boolean {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return telecom.getPhoneAccount(getHandle(context))?.isEnabled ?: false
    }
}