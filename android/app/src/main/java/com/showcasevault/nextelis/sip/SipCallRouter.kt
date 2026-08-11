package com.showcasevault.nextelis.sip

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.util.Log
import com.showcasevault.nextelis.account.PhoneAccountManager

/**
 * Bridges Linphone call events into Android Telecom.
 *
 * This deliberately does NOT live in [com.showcasevault.nextelis.telecom.NexTelisConnectionService]:
 * Android only instantiates a ConnectionService once Telecom already has a
 * call to hand it, so a listener registered in its onCreate() is absent
 * exactly when an inbound INVITE arrives — incoming calls were silently
 * dropped. Installing it alongside the SIP registration (from SipCallService)
 * guarantees a listener exists for as long as we're registered to Asterisk.
 */
object SipCallRouter {

    private const val TAG = "SipCallRouter"

    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true

        val appContext = context.applicationContext
        SipManager.setCallListener(object : SipCallListener {
            override fun onIncomingCall(remoteAddress: String) {
                reportIncomingCall(appContext, remoteAddress)
            }

            override fun onCallStateChanged(state: SipCallState) {
                CallStateBus.publish(state)
            }
        })
    }

    /** Hands an inbound SIP call to Telecom, which then asks our
     * ConnectionService for a Connection and shows the native ringing UI. */
    @SuppressLint("MissingPermission")
    private fun reportIncomingCall(context: Context, remoteNumber: String) {
        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val handle = PhoneAccountManager.getHandle(context)

        val extras = android.os.Bundle().apply {
            putParcelable(
                TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                Uri.fromParts(PhoneAccount.SCHEME_SIP, remoteNumber, null)
            )
        }

        try {
            telecomManager.addNewIncomingCall(handle, extras)
        } catch (e: SecurityException) {
            // PhoneAccount not enabled by the user in Settings > Calling
            // accounts — nothing we can do from here (see docs/FINDINGS.md).
            Log.e(TAG, "Cannot report incoming call from $remoteNumber: ${e.message}")
        }
    }
}
