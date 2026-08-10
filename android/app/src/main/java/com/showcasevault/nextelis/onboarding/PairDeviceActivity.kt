package com.showcasevault.nextelis.onboarding

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.network.DeviceClaimRequest
import com.showcasevault.nextelis.network.NexTelisApiClient
import com.showcasevault.nextelis.network.UserCreateRequest
import com.showcasevault.nextelis.network.UserWithClaimCode
import com.showcasevault.nextelis.session.SessionStore
import com.showcasevault.nextelis.ui.LoadingOverlay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Screen 1 of onboarding: register the user, then pair (claim) this
 * specific device using the one-time claim code the backend returns.
 * See backend/api/v1/routes/users.py and devices.py for the contract.
 */
class PairDeviceActivity : AppCompatActivity() {

    private lateinit var inputDisplayName: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var sectionClaim: LinearLayout
    private lateinit var textClaimCode: TextView
    private lateinit var textClaimExpiry: TextView
    private lateinit var textError: TextView
    private lateinit var loadingOverlay: LoadingOverlay
    private lateinit var btnRegister: Button
    private lateinit var btnClaimDevice: Button

    private var registration: UserWithClaimCode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pair_device)

        inputDisplayName = findViewById(R.id.inputDisplayName)
        inputEmail = findViewById(R.id.inputEmail)
        sectionClaim = findViewById(R.id.sectionClaim)
        textClaimCode = findViewById(R.id.textClaimCode)
        textClaimExpiry = findViewById(R.id.textClaimExpiry)
        textError = findViewById(R.id.textError)
        loadingOverlay = LoadingOverlay(findViewById(android.R.id.content))
        btnRegister = findViewById(R.id.btnRegister)
        btnClaimDevice = findViewById(R.id.btnClaimDevice)

        btnRegister.setOnClickListener { onRegisterClicked() }
        btnClaimDevice.setOnClickListener { onClaimClicked() }
    }

    private fun onRegisterClicked() {
        val displayName = inputDisplayName.text?.toString()?.trim().orEmpty()
        val email = inputEmail.text?.toString()?.trim().orEmpty()

        if (displayName.isEmpty() || email.isEmpty()) {
            showError("Enter your name and email to continue.")
            return
        }

        clearError()
        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = NexTelisApiClient.api.registerUser(
                    UserCreateRequest(email = email, display_name = displayName)
                )
                registration = result
                showClaimStep(result)
            } catch (e: Exception) {
                showError(describeNetworkError(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun onClaimClicked() {
        val claimCode = registration?.claim_code ?: return
        val displayName = inputDisplayName.text?.toString()?.trim().orEmpty()

        clearError()
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = NexTelisApiClient.api.claimDevice(
                    DeviceClaimRequest(
                        claim_code = claimCode,
                        device_name = Build.MODEL ?: "Android device"
                    )
                )
                SessionStore.savePairing(
                    context = this@PairDeviceActivity,
                    userId = response.device.user_id.toString(),
                    deviceToken = response.device_token,
                    displayName = displayName
                )
                AppFlow.routeFromLaunch(this@PairDeviceActivity)
            } catch (e: Exception) {
                showError(describeNetworkError(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showClaimStep(result: UserWithClaimCode) {
        sectionClaim.visibility = LinearLayout.VISIBLE
        textClaimCode.text = result.claim_code
        textClaimExpiry.text = getString(R.string.claim_code_expiry, formatExpiry(result.claim_code_expires_at))
    }

    private fun formatExpiry(isoTimestamp: String): String {
        return try {
            val expiry = OffsetDateTime.parse(isoTimestamp)
            val secondsLeft = expiry.toEpochSecond() - Instant.now().epochSecond
            val minutes = (secondsLeft / 60).coerceAtLeast(0)
            getString(R.string.claim_code_minutes, minutes)
        } catch (e: DateTimeParseException) {
            ""
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) loadingOverlay.show() else loadingOverlay.hide()
        btnRegister.isEnabled = !loading
        btnClaimDevice.isEnabled = !loading
    }

    private fun showError(message: String) {
        textError.text = message
        textError.visibility = TextView.VISIBLE
    }

    private fun clearError() {
        textError.visibility = TextView.GONE
    }

    private fun describeNetworkError(e: Exception): String {
        return getString(R.string.error_network_generic, e.message ?: e.javaClass.simpleName)
    }
}
