package com.showcasevault.nextelis.account

import android.content.ComponentName
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import com.showcasevault.nextelis.telecom.NexTelisConnectionService

object PhoneAccountManager {

    private const val TAG = "PhoneAccountManager"
    private const val ACCOUNT_ID = "nextelis_account"
    private const val ACCOUNT_LABEL = "NexTelis"

    fun getHandle(context: Context): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context, NexTelisConnectionService::class.java),
            ACCOUNT_ID
        )
    }

    fun register(context: Context) {
        // registerPhoneAccount() unconditionally resets isEnabled back to
        // false, even for an already-registered account the user had just
        // enabled in Settings — this is intentional Android behavior (an
        // app can't silently re-enable itself). So this must be a no-op
        // once registered, or every HomeActivity recreation (e.g. returning
        // from the Calling Accounts Settings screen) would immediately
        // undo the toggle the user just flipped on.
        //
        // Exception: an account registered by an older build may be missing
        // properties we now depend on (notably supported URI schemes, whose
        // absence stops Telecom routing dialer calls to us at all). Those
        // must be re-registered once, at the cost of the user re-enabling.
        val existing = readPhoneAccount(context)
        if (existing != null && existing.supportedUriSchemes.isNotEmpty()) return

        val handle = getHandle(context)
        // CAPABILITY_CALL_PROVIDER (not SELF_MANAGED): self-managed apps
        // never receive calls dialed from the native Phone app's dialpad —
        // they're expected to call TelecomManager.placeCall() from their own
        // UI instead. Native-dialer-initiated calling is the whole point of
        // this project (docs/PROJECT.md §2), so we stay call-provider even
        // though NexTelis drives its own SIP/RTP rather than a modem/SIM.
        // Telecom only routes a call to an account whose supported schemes
        // match the dialed address. Without this list the account registers
        // and shows up in Settings, but the native dialer never actually
        // hands us a call — TEL is what the dialpad produces, SIP is what
        // our own SIP URIs use.
        val schemes = listOf(PhoneAccount.SCHEME_TEL, PhoneAccount.SCHEME_SIP)

        val account = PhoneAccount.builder(handle, ACCOUNT_LABEL)
            .setCapabilities(
                PhoneAccount.CAPABILITY_CALL_PROVIDER or
                        PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS
            )
            .setSupportedUriSchemes(schemes)
            .setShortDescription(ACCOUNT_LABEL)
            .build()

        telecomManager(context).registerPhoneAccount(account)
        Log.d(TAG, "PhoneAccount registered: $handle")
    }

    fun isRegistered(context: Context): Boolean = readPhoneAccount(context) != null

    // registerPhoneAccount() alone leaves the account disabled — the user
    // must flip it on in Settings > Calling accounts before it appears in
    // the dialer's "call using" chooser. This never touches the real SIM account.
    fun isEnabled(context: Context): Boolean = readPhoneAccount(context)?.isEnabled ?: false

    /** Combines [isRegistered]/[isEnabled] into a single state for the UI to react to. */
    fun getStatus(context: Context): AccountStatus {
        val account = readPhoneAccount(context) ?: return AccountStatus.NOT_REGISTERED
        return if (account.isEnabled) AccountStatus.ENABLED else AccountStatus.REGISTERED_DISABLED
    }

    private fun telecomManager(context: Context): TelecomManager =
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    // getPhoneAccount() enforces READ_PHONE_NUMBERS on API 31+, so this must
    // never crash the app even if that permission is missing/denied.
    private fun readPhoneAccount(context: Context): PhoneAccount? {
        return try {
            telecomManager(context).getPhoneAccount(getHandle(context))
        } catch (e: SecurityException) {
            Log.w(TAG, "getPhoneAccount: missing permission", e)
            null
        }
    }
}