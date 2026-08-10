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
    private const val ACCOUNT_LABEL = "NexTelis"

    fun getHandle(context: Context): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context, NexTelisConnectionService::class.java),
            ACCOUNT_ID
        )
    }

    fun register(context: Context) {
        val handle = getHandle(context)
        val account = PhoneAccount.builder(handle, ACCOUNT_LABEL)
            .setCapabilities(
                PhoneAccount.CAPABILITY_CALL_PROVIDER or      // appear as a calling provider
                        PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS // required to show in chooser
            )
            .build()

        telecomManager(context).registerPhoneAccount(account)
        Log.d(TAG, "PhoneAccount registered: $handle")
    }

    fun isRegistered(context: Context): Boolean = readPhoneAccount(context) != null

    // registerPhoneAccount() alone leaves the account disabled — the user
    // must flip it on in Settings > Calling accounts before it appears in
    // the dialer's "call using" chooser. This never touches the real SIM account.
    fun isEnabled(context: Context): Boolean = readPhoneAccount(context)?.isEnabled ?: false

    /** Combines [isRegistered]/[isEnabled] into a single state for the UI to react to. */
    fun getStatus(context: Context): AccountStatus {
        val account = readPhoneAccount(context) ?: return AccountStatus.NOT_REGISTERED
        return if (account.isEnabled) AccountStatus.ENABLED else AccountStatus.REGISTERED_DISABLED
    }

    private fun telecomManager(context: Context): TelecomManager =
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    // getPhoneAccount() enforces READ_PHONE_NUMBERS on API 31+, so this must
    // never crash the app even if that permission is missing/denied.
    private fun readPhoneAccount(context: Context): PhoneAccount? {
        return try {
            telecomManager(context).getPhoneAccount(getHandle(context))
        } catch (e: SecurityException) {
            Log.w(TAG, "getPhoneAccount: missing permission", e)
            null
        }
    }
}