package com.showcasevault.nextelis.account

/**
 * State of the NexTelis [android.telecom.PhoneAccount] as seen by the app.
 * Kept separate from Android APIs so it's trivial to reason about/test.
 */
enum class AccountStatus {
    NOT_REGISTERED,
    REGISTERED_DISABLED,
    ENABLED
}
