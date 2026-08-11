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
    private const val KEY_SIP_PASSWORD = "sip_password"
    private const val KEY_SERVER_HOST = "server_host"
    private const val KEY_SERVER_PORT = "server_port"
    private const val KEY_SERVER_SCHEME = "server_scheme"
    private const val KEY_SIP_HOST = "sip_host"
    private const val KEY_SIP_PORT = "sip_port"
    private const val KEY_SIP_TRANSPORT = "sip_transport"

    const val DEFAULT_SIP_PORT = 5060
    const val DEFAULT_SIP_TRANSPORT = "udp"

    // A bare LAN address has no TLS in front of it, so it stays http. A domain
    // is assumed to be a real deployment behind a reverse proxy on 443.
    const val SCHEME_HTTP = "http"
    const val SCHEME_HTTPS = "https"

    // Sentinel for "no explicit port" — use the scheme's default (80/443).
    const val PORT_DEFAULT = 0

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Saves the NexTelis server this device should talk to. Pass
     * [PORT_DEFAULT] to omit the port and use the scheme's default.
     */
    fun saveServerConfig(
        context: Context,
        host: String,
        port: Int,
        scheme: String = SCHEME_HTTP,
    ) {
        prefs(context).edit()
            .putString(KEY_SERVER_HOST, host)
            .putInt(KEY_SERVER_PORT, port)
            .putString(KEY_SERVER_SCHEME, scheme)
            .apply()
    }

    fun getServerHost(context: Context): String? = prefs(context).getString(KEY_SERVER_HOST, null)

    fun getServerPort(context: Context): Int = prefs(context).getInt(KEY_SERVER_PORT, 8000)

    fun getServerScheme(context: Context): String =
        prefs(context).getString(KEY_SERVER_SCHEME, null) ?: SCHEME_HTTP

    fun hasServerConfig(context: Context): Boolean = getServerHost(context) != null

    /**
     * e.g. "http://192.168.1.50:8000/" or "https://calls.example.com/" — the
     * base URL for the API. The port is omitted when the scheme's default
     * applies, which is the normal case for a domain behind a reverse proxy.
     */
    fun getServerBaseUrl(context: Context): String? {
        val host = getServerHost(context) ?: return null
        val port = getServerPort(context)
        val authority = if (port == PORT_DEFAULT) host else "$host:$port"
        return "${getServerScheme(context)}://$authority/"
    }

    /** Caches what /server/info reported, fetched during server setup. */
    fun saveSipConfig(context: Context, host: String, port: Int, transport: String) {
        prefs(context).edit()
            .putString(KEY_SIP_HOST, host)
            .putInt(KEY_SIP_PORT, port)
            .putString(KEY_SIP_TRANSPORT, transport)
            .apply()
    }

    /**
     * Where to send SIP traffic. Falls back to the API host for installs
     * paired before the server started reporting this, which is correct
     * whenever the API and Asterisk share an address.
     */
    fun getSipHost(context: Context): String? =
        prefs(context).getString(KEY_SIP_HOST, null) ?: getServerHost(context)

    fun getSipPort(context: Context): Int =
        prefs(context).getInt(KEY_SIP_PORT, DEFAULT_SIP_PORT)

    fun getSipTransport(context: Context): String =
        prefs(context).getString(KEY_SIP_TRANSPORT, null) ?: DEFAULT_SIP_TRANSPORT

    fun savePairing(context: Context, userId: String, deviceToken: String, displayName: String) {
        prefs(context).edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_DEVICE_TOKEN, deviceToken)
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
    }

    fun saveNumber(context: Context, numberValue: String, sipPassword: String) {
        prefs(context).edit()
            .putString(KEY_NUMBER_VALUE, numberValue)
            .putString(KEY_SIP_PASSWORD, sipPassword)
            .apply()
    }

    fun getUserId(context: Context): String? = prefs(context).getString(KEY_USER_ID, null)

    fun getDeviceToken(context: Context): String? = prefs(context).getString(KEY_DEVICE_TOKEN, null)

    fun getDisplayName(context: Context): String? = prefs(context).getString(KEY_DISPLAY_NAME, null)

    fun getNumberValue(context: Context): String? = prefs(context).getString(KEY_NUMBER_VALUE, null)

    fun getSipPassword(context: Context): String? = prefs(context).getString(KEY_SIP_PASSWORD, null)

    fun isPaired(context: Context): Boolean = getDeviceToken(context) != null

    fun hasNumber(context: Context): Boolean = getNumberValue(context) != null

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
