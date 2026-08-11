package com.showcasevault.nextelis.onboarding

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.network.NexTelisApiClient
import com.showcasevault.nextelis.session.SessionStore
import kotlinx.coroutines.launch

/**
 * First screen shown when the app has no server configured: lets the user
 * point this install at any NexTelis deployment by host[:port], rather than
 * baking one server into the APK. Reachable again later from Home to switch
 * servers. See SessionStore.saveServerConfig / NexTelisApiClient.reset.
 */
class ServerSetupActivity : AppCompatActivity() {

    private lateinit var inputServerHost: TextInputEditText
    private lateinit var textError: TextView
    private lateinit var btnConnect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_setup)

        inputServerHost = findViewById(R.id.inputServerHost)
        textError = findViewById(R.id.textError)
        btnConnect = findViewById(R.id.btnConnect)

        // Prefill the address exactly as it would be typed, port included, so
        // editing it is round-trippable.
        SessionStore.getServerHost(this)?.let { host ->
            val port = SessionStore.getServerPort(this)
            inputServerHost.setText(
                if (port == SessionStore.PORT_DEFAULT) host else "$host:$port"
            )
        }

        btnConnect.setOnClickListener { onConnectClicked() }
    }

    private fun onConnectClicked() {
        val hostText = inputServerHost.text?.toString()?.trim().orEmpty()

        if (hostText.isEmpty()) {
            showError(getString(R.string.server_setup_error_host_required))
            return
        }

        val address = ServerAddress.parse(hostText)
        if (address == null) {
            showError(getString(R.string.server_setup_error_address_invalid))
            return
        }

        clearError()
        SessionStore.saveServerConfig(
            this,
            host = address.host,
            port = address.port,
            scheme = address.scheme,
        )
        NexTelisApiClient.reset()

        // Ask the server where its SIP service lives before moving on. This
        // also doubles as a reachability check on the address just entered,
        // so a typo is reported here rather than surfacing much later as a
        // failed registration.
        btnConnect.isEnabled = false
        lifecycleScope.launch {
            val sipHost = try {
                NexTelisApiClient.api.getServerInfo().sip_host
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't reach ${address.host}: ${e.message}")
                btnConnect.isEnabled = true
                showError(getString(R.string.server_setup_error_unreachable))
                return@launch
            }
            SessionStore.saveSipHost(this@ServerSetupActivity, sipHost)
            AppFlow.routeFromLaunch(this@ServerSetupActivity)
        }
    }

    private fun showError(message: String) {
        textError.text = message
        textError.visibility = TextView.VISIBLE
    }

    private fun clearError() {
        textError.visibility = TextView.GONE
    }

    private companion object {
        const val TAG = "ServerSetup"
    }
}
