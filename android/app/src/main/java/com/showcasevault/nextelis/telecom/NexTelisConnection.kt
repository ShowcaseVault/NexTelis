package com.showcasevault.nextelis.telecom

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log

class NexTelisConnection : Connection() {

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
        setActive()
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
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAbort() {
        Log.d(TAG, "onAbort")
        destroy()
    }
}