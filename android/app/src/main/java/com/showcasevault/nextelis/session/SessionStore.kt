package com.showcasevault.nextelis.session

import android.content.Context

/**
 * Local device-side session state — everything the backend hands us that
 * we must hold onto ourselves (device_token is only ever returned once).
 */
object SessionStore {

    private const val PREFS_NAME = "nextelis_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_DEVICE_TOKEN = "device_token"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_NUMBER_VALUE = "number_value"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun savePairing(context: Context, userId: String, deviceToken: String, displayName: String) {
        prefs(context).edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_DEVICE_TOKEN, deviceToken)
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
    }

    fun saveNumber(context: Context, numberValue: String) {
        prefs(context).edit().putString(KEY_NUMBER_VALUE, numberValue).apply()
    }

    fun getUserId(context: Context): String? = prefs(context).getString(KEY_USER_ID, null)

    fun getDeviceToken(context: Context): String? = prefs(context).getString(KEY_DEVICE_TOKEN, null)

    fun getDisplayName(context: Context): String? = prefs(context).getString(KEY_DISPLAY_NAME, null)

    fun getNumberValue(context: Context): String? = prefs(context).getString(KEY_NUMBER_VALUE, null)

    fun isPaired(context: Context): Boolean = getDeviceToken(context) != null

    fun hasNumber(context: Context): Boolean = getNumberValue(context) != null

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
