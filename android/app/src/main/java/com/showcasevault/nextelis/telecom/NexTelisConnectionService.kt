package com.showcasevault.nextelis.telecom

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.util.Log

class NexTelisConnectionService : ConnectionService() {

    companion object {
        private const val TAG = "NexTelisConnSvc"
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        Log.d(TAG, "Outgoing call → ${request.address}")
        // Later: start SIP session to Asterisk here
        return NexTelisConnection().apply { setDialing() }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        Log.d(TAG, "Incoming call ← ${request.address}")
        // Later: receive SIP invite from Asterisk here
        return NexTelisConnection().apply { setRinging() }
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccountHandle: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        Log.e(TAG, "Outgoing connection FAILED → ${request.address}")
    }
}