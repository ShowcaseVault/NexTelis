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

    /** Short enough that a dead contact ages out quickly, long enough not to
     * hammer the network. Asterisk will renew via the REGISTER refresh. */
    private const val REGISTRATION_EXPIRY_SECONDS = 300

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
            unregisterAndStop(existingCore)
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
        // Without an explicit expiry the registration lapses and is never
        // renewed: outgoing INVITEs still work (they don't need a valid
        // registration) but Asterisk no longer knows where to reach us, so
        // incoming calls silently stop. This is the "could call out but
        // never receive" bug.
        accountParams.expires = REGISTRATION_EXPIRY_SECONDS

        val account = newCore.createAccount(accountParams)
        newCore.addAccount(account)
        newCore.defaultAccount = account

        core = newCore
        Log.d(TAG, "Registering $number@$host")
    }

    fun stop() {
        core?.let { unregisterAndStop(it) }
        core = null
    }

    /**
     * Drops the SIP registration before tearing the Core down.
     *
     * Each Core binds a fresh random UDP port, so Asterisk treats a restart
     * as a *new* contact rather than a refresh of the old one. Without an
     * explicit unregister, dead contacts accumulate against the AOR's
     * max_contacts limit and inbound calls fork to phones that no longer
     * exist. Clearing registration on the way out keeps the AOR clean.
     */
    private fun unregisterAndStop(target: Core) {
        try {
            target.defaultAccount?.params?.let { params ->
                val clone = params.clone()
                clone.isRegisterEnabled = false
                target.defaultAccount?.params = clone
            }
            // Give the un-REGISTER a chance to go out before the Core dies.
            target.iterate()
        } catch (e: Exception) {
            Log.w(TAG, "Clean unregister failed, stopping anyway: ${e.message}")
        }
        target.stop()
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
                // Bare user part only ("7002"), not the full "sip:7002@host"
                // URI — callers build their own SIP/tel URIs from this, and
                // the NexTelis directory lookup keys on the number alone.
                Call.State.IncomingReceived ->
                    listener?.onIncomingCall(call.remoteAddress.username.orEmpty())
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
