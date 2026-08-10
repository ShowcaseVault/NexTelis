package com.showcasevault.nextelis.sip

/**
 * App-level call state, decoupled from org.linphone.core.Call.State so the
 * Telecom layer (NexTelisConnection) never has to depend on Linphone types.
 */
enum class SipCallState {
    RINGING_OUTGOING,
    RINGING_INCOMING,
    CONNECTED,
    ENDED
}
