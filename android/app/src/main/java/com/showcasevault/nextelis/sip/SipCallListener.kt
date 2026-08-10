package com.showcasevault.nextelis.sip

/** Bridges Linphone call events to the Telecom layer (NexTelisConnection). */
interface SipCallListener {
    fun onIncomingCall(remoteAddress: String)
    fun onCallStateChanged(state: SipCallState)
}
