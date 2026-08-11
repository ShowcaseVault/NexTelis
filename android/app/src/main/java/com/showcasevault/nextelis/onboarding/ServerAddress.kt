package com.showcasevault.nextelis.onboarding

import com.showcasevault.nextelis.session.SessionStore

/**
 * Parses whatever the user typed into the server field into a scheme, a host,
 * and an optional port.
 *
 * Three deployment shapes have to work from one field:
 *
 *   192.168.1.50:8000        a LAN box, plain HTTP on an explicit port
 *   calls.example.com        a deployment behind a reverse proxy on 443
 *   abc123.ngrok-free.app    a tunnel, also HTTPS on 443
 *
 * so the scheme is inferred rather than asked for: an explicit scheme wins,
 * otherwise a bare IP is assumed to be plain HTTP and a hostname HTTPS.
 *
 * A tunnel host is why there is no separate port field. The local port a
 * tunnel forwards to (8000) is not part of its public URL — the edge listens
 * on 443 — so offering a port box invites appending one, which breaks the
 * address. A port belongs in the address itself or not at all.
 */
data class ServerAddress(
    val scheme: String,
    val host: String,
    /** [SessionStore.PORT_DEFAULT] when the scheme's default port applies. */
    val port: Int,
) {
    companion object {

        private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

        /** Returns null if [input] can't be read as an address. */
        fun parse(input: String): ServerAddress? {
            var rest = input.trim()
            if (rest.isEmpty()) return null

            // An explicit scheme is authoritative; without one we infer below.
            var scheme: String? = null
            val schemeMatch = Regex("""^([a-zA-Z][a-zA-Z0-9+.-]*)://""").find(rest)
            if (schemeMatch != null) {
                scheme = schemeMatch.groupValues[1].lowercase()
                if (scheme != SessionStore.SCHEME_HTTP && scheme != SessionStore.SCHEME_HTTPS) {
                    return null
                }
                rest = rest.removeRange(schemeMatch.range)
            }

            // Tolerate a pasted URL rather than rejecting it: everything from
            // the first / onward is a path we don't want.
            rest = rest.substringBefore('/').substringBefore('?')
            if (rest.isEmpty()) return null

            var host = rest
            var port = SessionStore.PORT_DEFAULT

            // Split host:port, but only on the last colon and only when what
            // follows is numeric — a bare IPv6 literal is full of colons.
            val colon = rest.lastIndexOf(':')
            if (colon != -1 && rest.indexOf(':') == colon) {
                val portText = rest.substring(colon + 1)
                val parsed = portText.toIntOrNull() ?: return null
                if (parsed !in 1..65535) return null
                host = rest.substring(0, colon)
                port = parsed
            }

            if (host.isEmpty() || !isPlausibleHost(host)) return null

            val resolvedScheme = scheme ?: inferScheme(host, port)
            return ServerAddress(resolvedScheme, host, port)
        }

        private fun inferScheme(host: String, port: Int): String {
            // Nobody puts a certificate on a LAN IP.
            if (IPV4.matches(host) || host.equals("localhost", ignoreCase = true)) {
                return SessionStore.SCHEME_HTTP
            }
            // A hostname on a spelled-out non-TLS port is someone reaching a
            // dev server by name; HTTPS on 8000 would just fail to connect.
            if (port != SessionStore.PORT_DEFAULT && port != HTTPS_PORT) {
                return SessionStore.SCHEME_HTTP
            }
            // Otherwise a hostname means a proxy or tunnel terminating TLS.
            return SessionStore.SCHEME_HTTPS
        }

        private const val HTTPS_PORT = 443

        // Deliberately loose: catches obvious typos without trying to be a
        // full hostname validator, since the real test is whether it resolves.
        private fun isPlausibleHost(host: String): Boolean {
            if (host.any { it.isWhitespace() }) return false
            return host.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
        }
    }
}
