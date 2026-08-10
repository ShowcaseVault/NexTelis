package com.showcasevault.nextelis.telecom

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import com.showcasevault.nextelis.account.PhoneAccountManager
import com.showcasevault.nextelis.sip.SipCallListener
import com.showcasevault.nextelis.sip.SipCallState
import com.showcasevault.nextelis.sip.SipManager

class NexTelisConnectionService : ConnectionService() {

    companion object {
        private const val TAG = "NexTelisConnSvc"
        private var activeConnection: NexTelisConnection? = null
    }

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
        return NexTelisConnection(isIncoming = true).apply {
            setRinging()
            setAddress(request.address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
            activeConnection = this
        }
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        Log.e(TAG, "Outgoing connection FAILED → ${request.address}")
    }
}
