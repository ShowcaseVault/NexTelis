package com.showcasevault.nextelis.telecom

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import com.showcasevault.nextelis.network.NexTelisApiClient
import com.showcasevault.nextelis.sip.CallStateBus
import com.showcasevault.nextelis.sip.SipCallState
import com.showcasevault.nextelis.sip.SipManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        // Note: the SipManager listener itself is installed by SipCallRouter
        // (from SipCallService), not here — this service doesn't exist until
        // Telecom already has a call, which is too late to catch an inbound
        // INVITE. We only observe state for the call currently in progress.
        CallStateBus.setObserver { state -> onSipStateChanged(state) }
    }

    override fun onDestroy() {
        CallStateBus.setObserver(null)
        super.onDestroy()
    }

    private fun onSipStateChanged(state: SipCallState) {
        val connection = activeConnection ?: return
        when (state) {
            SipCallState.CONNECTED -> connection.setActive()
            SipCallState.RINGING_OUTGOING -> connection.setDialing()
            SipCallState.RINGING_INCOMING -> Unit
            SipCallState.ENDED -> {
                connection.setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
                connection.destroy()
                activeConnection = null
            }
        }
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        val destination = request.address?.schemeSpecificPart.orEmpty()
        Log.d(TAG, "Outgoing call → $destination")

        SipManager.placeCall(destination)

        return NexTelisConnection(isIncoming = false).apply {
            setAddress(
                Uri.fromParts(PhoneAccount.SCHEME_SIP, destination, null),
                TelecomManager.PRESENTATION_ALLOWED
            )
            setDialing()
            activeConnection = this
            if (destination.isNotEmpty()) resolveCallerDisplayName(destination, this)
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        val remoteNumber = request.address?.schemeSpecificPart.orEmpty()
        Log.d(TAG, "Incoming call ← $remoteNumber")

        return NexTelisConnection(isIncoming = true).apply {
            setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
            setRinging()
            activeConnection = this
            if (remoteNumber.isNotEmpty()) resolveCallerDisplayName(remoteNumber, this)
        }
    }

    /** Looks up the remote party's NexTelis display name so a name shows even
     * when the number isn't in the phone's local contacts. Best-effort: the
     * raw number stays as the caller ID if the lookup fails or is slow. */
    private fun resolveCallerDisplayName(number: String, connection: NexTelisConnection) {
        displayNameCache[number]?.let {
            connection.applyCallerDisplayName(it)
            return
        }
        scope.launch {
            try {
                val result = NexTelisApiClient.api.lookupNumber(number)
                displayNameCache[number] = result.display_name
                // Telecom Connection mutators must run on the main thread.
                withContext(Dispatchers.Main) {
                    connection.applyCallerDisplayName(result.display_name)
                }
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
