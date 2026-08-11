package com.showcasevault.nextelis.sip

import android.content.Context
import android.util.Log
import com.showcasevault.nextelis.session.SessionStore
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

/**
 * Owns the Linphone Core: registers this device's NexTelis number to
 * Asterisk over SIP and translates call events into [SipCallListener]
 * callbacks. NexTelisConnection/NexTelisConnectionService drive calls
 * through this rather than talking to Linphone directly.
 */
object SipManager {

    private const val TAG = "SipManager"

    private var core: Core? = null
    private var listener: SipCallListener? = null
    private var appContext: Context? = null

    fun setCallListener(listener: SipCallListener) {
        this.listener = listener
    }

    /** Registers [number] with Asterisk using [sipPassword]. Safe to call again to re-register. */
    fun start(context: Context, number: String, sipPassword: String) {
        appContext = context.applicationContext

        val existingCore = core
        if (existingCore != null) {
            existingCore.stop()
        }

        val factory = Factory.instance()
        val newCore = factory.createCore(null, null, context)
        newCore.addListener(coreListener)
        newCore.isAutoIterateEnabled = true
        newCore.start()

        val host = sipHost()
        val identity = factory.createAddress("sip:$number@$host")
        val serverAddress = factory.createAddress("sip:$host")
        if (identity == null || serverAddress == null) {
            Log.e(TAG, "Failed to build SIP addresses for $number@$host")
            return
        }
        identity.transport = TransportType.Udp
        serverAddress.transport = TransportType.Udp

        val authInfo = factory.createAuthInfo(number, null, sipPassword, null, null, host)
        newCore.addAuthInfo(authInfo)

        val accountParams = newCore.createAccountParams()
        accountParams.identityAddress = identity
        accountParams.serverAddress = serverAddress
        accountParams.isRegisterEnabled = true

        val account = newCore.createAccount(accountParams)
        newCore.addAccount(account)
        newCore.defaultAccount = account

        core = newCore
        Log.d(TAG, "Registering $number@$host")
    }

    fun stop() {
        core?.stop()
        core = null
    }

    fun placeCall(destinationNumber: String) {
        val host = sipHost()
        core?.invite("sip:$destinationNumber@$host")
    }

    fun answerCurrentCall() {
        core?.currentCall?.accept()
    }

    fun endCurrentCall() {
        core?.currentCall?.terminate()
    }

    /** Asterisk host has no separate SIP domain — same address the user configured for the API. */
    private fun sipHost(): String {
        val context = appContext ?: error("SipManager.start() must be called before placing/receiving calls")
        return SessionStore.getServerHost(context)
            ?: error("No NexTelis server configured — set one via the Connect to Server screen.")
    }

    private val coreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            Log.d(TAG, "Registration state: $state ($message)")
        }

        override fun onCallStateChanged(
            lc: Core,
            call: Call,
            cstate: Call.State,
            message: String
        ) {
            Log.d(TAG, "Call state: $cstate ($message)")
            when (cstate) {
                Call.State.IncomingReceived ->
                    listener?.onIncomingCall(call.remoteAddress.asStringUriOnly())
                Call.State.OutgoingInit, Call.State.OutgoingProgress, Call.State.OutgoingRinging ->
                    listener?.onCallStateChanged(SipCallState.RINGING_OUTGOING)
                Call.State.Connected, Call.State.StreamsRunning ->
                    listener?.onCallStateChanged(SipCallState.CONNECTED)
                Call.State.End, Call.State.Error, Call.State.Released ->
                    listener?.onCallStateChanged(SipCallState.ENDED)
                else -> Unit
            }
        }
    }
}
