package com.showcasevault.nextelis.telecom

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import com.showcasevault.nextelis.sip.SipManager

/**
 * Telecom-facing half of a call. Real signaling/audio is owned by
 * [SipManager] (Linphone) — this class only forwards user actions
 * (answer/hold/hangup) to it and reflects state changes back.
 */
class NexTelisConnection(private val isIncoming: Boolean) : Connection() {

    companion object {
        private const val TAG = "NexTelisConnection"
    }

    init {
        audioModeIsVoip = true
        connectionCapabilities = CAPABILITY_HOLD or
                CAPABILITY_SUPPORT_HOLD or
                CAPABILITY_MUTE
    }

    override fun onAnswer() {
        Log.d(TAG, "onAnswer")
        SipManager.answerCurrentCall()
    }

    override fun onHold() {
        Log.d(TAG, "onHold")
        setOnHold()
    }

    override fun onUnhold() {
        Log.d(TAG, "onUnhold")
        setActive()
    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisconnect")
        SipManager.endCurrentCall()
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAbort() {
        Log.d(TAG, "onAbort")
        SipManager.endCurrentCall()
        destroy()
    }

    override fun onReject() {
        Log.d(TAG, "onReject")
        SipManager.endCurrentCall()
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }
}
