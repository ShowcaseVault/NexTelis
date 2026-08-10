package com.showcasevault.nextelis.onboarding

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.network.NexTelisApiClient
import com.showcasevault.nextelis.session.SessionStore
import com.showcasevault.nextelis.ui.LoadingOverlay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Screen 3 of onboarding: number assignment is an explicit step on the
 * backend (POST /users/{id}/number), not automatic at registration —
 * see backend/api/v1/routes/numbers.py.
 */
class GetNumberActivity : AppCompatActivity() {

    private lateinit var textNumberCaption: TextView
    private lateinit var textNumberValue: TextView
    private lateinit var textError: TextView
    private lateinit var loadingOverlay: LoadingOverlay
    private lateinit var btnGetNumber: Button
    private lateinit var btnContinue: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_number)

        textNumberCaption = findViewById(R.id.textNumberCaption)
        textNumberValue = findViewById(R.id.textNumberValue)
        textError = findViewById(R.id.textError)
        loadingOverlay = LoadingOverlay(findViewById(android.R.id.content))
        btnGetNumber = findViewById(R.id.btnGetNumber)
        btnContinue = findViewById(R.id.btnContinue)

        btnGetNumber.setOnClickListener { assignNumber() }
        btnContinue.setOnClickListener { AppFlow.routeFromLaunch(this) }
    }

    private fun assignNumber() {
        val userId = SessionStore.getUserId(this)?.let(UUID::fromString) ?: return

        clearError()
        setLoading(true)
        lifecycleScope.launch {
            try {
                val number = NexTelisApiClient.api.assignNumber(userId)
                SessionStore.saveNumber(this@GetNumberActivity, number.value, number.sip_password)
                showAssignedNumber(number.value)
            } catch (e: Exception) {
                showError(getString(R.string.error_network_generic, e.message ?: e.javaClass.simpleName))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showAssignedNumber(value: String) {
        textNumberCaption.visibility = TextView.VISIBLE
        textNumberValue.visibility = TextView.VISIBLE
        textNumberValue.text = value
        btnGetNumber.visibility = Button.GONE
        btnContinue.visibility = Button.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        if (loading) loadingOverlay.show() else loadingOverlay.hide()
        btnGetNumber.isEnabled = !loading
    }

    private fun showError(message: String) {
        textError.text = message
        textError.visibility = TextView.VISIBLE
    }

    private fun clearError() {
        textError.visibility = TextView.GONE
    }
}
