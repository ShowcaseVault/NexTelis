package com.showcasevault.nextelis.onboarding

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.network.NexTelisApiClient
import com.showcasevault.nextelis.session.SessionStore

/**
 * First screen shown when the app has no server configured: lets the user
 * point this install at any NexTelis deployment by host[:port], rather than
 * baking one server into the APK. Reachable again later from Home to switch
 * servers. See SessionStore.saveServerConfig / NexTelisApiClient.reset.
 */
class ServerSetupActivity : AppCompatActivity() {

    private lateinit var inputServerHost: TextInputEditText
    private lateinit var inputServerPort: TextInputEditText
    private lateinit var textError: TextView
    private lateinit var btnConnect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_setup)

        inputServerHost = findViewById(R.id.inputServerHost)
        inputServerPort = findViewById(R.id.inputServerPort)
        textError = findViewById(R.id.textError)
        btnConnect = findViewById(R.id.btnConnect)

        SessionStore.getServerHost(this)?.let { inputServerHost.setText(it) }
        inputServerPort.setText(SessionStore.getServerPort(this).toString())

        btnConnect.setOnClickListener { onConnectClicked() }
    }

    private fun onConnectClicked() {
        val host = inputServerHost.text?.toString()?.trim().orEmpty()
        val portText = inputServerPort.text?.toString()?.trim().orEmpty()

        if (host.isEmpty()) {
            showError(getString(R.string.server_setup_error_host_required))
            return
        }

        val port = portText.toIntOrNull()
        if (port == null || port !in 1..65535) {
            showError(getString(R.string.server_setup_error_port_invalid))
            return
        }

        clearError()
        SessionStore.saveServerConfig(this, host = host, port = port)
        NexTelisApiClient.reset()
        AppFlow.routeFromLaunch(this)
    }

    private fun showError(message: String) {
        textError.text = message
        textError.visibility = TextView.VISIBLE
    }

    private fun clearError() {
        textError.visibility = TextView.GONE
    }
}
