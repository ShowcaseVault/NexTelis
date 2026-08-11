package com.showcasevault.nextelis.telecom

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import com.showcasevault.nextelis.account.PhoneAccountManager
import com.showcasevault.nextelis.network.NexTelisApiClient
import com.showcasevault.nextelis.sip.SipCallListener
import com.showcasevault.nextelis.sip.SipCallState
import com.showcasevault.nextelis.sip.SipManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NexTelisConnectionService : ConnectionService() {

    companion object {
        private const val TAG = "NexTelisConnSvc"
        private var activeConnection: NexTelisConnection? = null

        // Numbers are stable within a NexTelis deployment, so caching
        // resolved names process-wide avoids a lookup on every call.
        private val displayNameCache = mutableMapOf<String, String>()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // One listener for the process lifetime — SipManager is a
        // singleton, so re-registering per-Service-instance would leak.
        SipManager.setCallListener(object : SipCallListener {
            override fun onIncomingCall(remoteAddress: String) {
                reportIncomingCall(remoteAddress)
            }

            override fun onCallStateChanged(state: SipCallState) {
                when (state) {
                    SipCallState.CONNECTED -> activeConnection?.setActive()
                    SipCallState.ENDED -> {
                        activeConnection?.setDisconnected(
                            android.telecom.DisconnectCause(android.telecom.DisconnectCause.REMOTE)
                        )
                        activeConnection?.destroy()
                        activeConnection = null
                    }
                    SipCallState.RINGING_OUTGOING -> activeConnection?.setDialing()
                    SipCallState.RINGING_INCOMING -> Unit
                }
            }
        })
    }

    private fun reportIncomingCall(remoteAddress: String) {
        val telecomManager = getSystemService(TelecomManager::class.java)
        val handle = PhoneAccountManager.getHandle(this)
        val extras = android.os.Bundle().apply {
            putParcelable(
                TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                Uri.fromParts(android.telecom.PhoneAccount.SCHEME_SIP, remoteAddress, null)
            )
        }
        telecomManager.addNewIncomingCall(handle, extras)
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        Log.d(TAG, "Outgoing call → ${request.address}")
        val destination = request.address?.schemeSpecificPart.orEmpty()
        SipManager.placeCall(destination)
        return NexTelisConnection(isIncoming = false).apply {
            setDialing()
            activeConnection = this
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        Log.d(TAG, "Incoming call ← ${request.address}")
        val remoteNumber = request.address?.schemeSpecificPart
        return NexTelisConnection(isIncoming = true).apply {
            setRinging()
            setAddress(request.address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
            activeConnection = this
            if (!remoteNumber.isNullOrEmpty()) resolveCallerDisplayName(remoteNumber, this)
        }
    }

    /** Looks up the caller's NexTelis display name so it shows even when the
     * number isn't saved in the phone's local contacts. Best-effort: leaves
     * the raw number as the caller ID if the lookup fails or is slow. */
    private fun resolveCallerDisplayName(number: String, connection: NexTelisConnection) {
        displayNameCache[number]?.let {
            connection.applyCallerDisplayName(it)
            return
        }
        scope.launch {
            try {
                val result = NexTelisApiClient.api.lookupNumber(number)
                displayNameCache[number] = result.display_name
                connection.applyCallerDisplayName(result.display_name)
            } catch (e: Exception) {
                Log.d(TAG, "Caller name lookup failed for $number: ${e.message}")
            }
        }
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        Log.e(TAG, "Outgoing connection FAILED → ${request.address}")
    }
}
