package com.showcasevault.nextelis.sip

/**
 * Fans SIP call-state changes out to whoever is currently interested.
 *
 * [SipCallRouter] owns the single SipManager listener (it must outlive any
 * ConnectionService instance), but the live Connection is the thing that
 * needs to react to state changes. This lets the ConnectionService subscribe
 * for the lifetime of a call without competing for that one listener slot.
 */
object CallStateBus {

    private var observer: ((SipCallState) -> Unit)? = null

    fun setObserver(observer: ((SipCallState) -> Unit)?) {
        this.observer = observer
    }

    fun publish(state: SipCallState) {
        observer?.invoke(state)
    }
}
