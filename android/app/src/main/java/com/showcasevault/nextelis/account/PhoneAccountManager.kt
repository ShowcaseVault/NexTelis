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

    // Bumped whenever the PhoneAccount's properties change in a way that
    // requires re-registering an already-registered account. Re-registration
    // silently disables the account, so this must change only when necessary.
    //   1 -> added supported URI schemes (TEL, SIP)
    private const val ACCOUNT_VERSION = 1

    private const val PREFS_NAME = "nextelis_telecom"
    private const val KEY_REGISTERED_VERSION = "registered_account_version"

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
        //
        // That "needs upgrading?" decision is recorded locally rather than
        // re-derived from the live PhoneAccount on each launch. Reading it
        // back is not reliable enough to gate a destructive write:
        // getPhoneAccount() can return a stale copy for a short window after
        // the user toggles the account in Settings, and returns null outright
        // if READ_PHONE_NUMBERS is denied. Either would look like "not
        // upgraded yet" and re-register, wiping the toggle the user just set.
        //
        // The flag lives in app storage, so clearing app data clears it too —
        // exactly when Telecom drops the account and we do need to re-register.
        if (registeredVersion(context) == ACCOUNT_VERSION) return

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
        setRegisteredVersion(context, ACCOUNT_VERSION)
        Log.d(TAG, "PhoneAccount registered: $handle")
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun registeredVersion(context: Context): Int =
        prefs(context).getInt(KEY_REGISTERED_VERSION, 0)

    // Committed synchronously: if the process dies between registering the
    // account and persisting this, the next launch re-registers and disables
    // an account the user may have already enabled.
    private fun setRegisteredVersion(context: Context, version: Int) {
        prefs(context).edit().putInt(KEY_REGISTERED_VERSION, version).commit()
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