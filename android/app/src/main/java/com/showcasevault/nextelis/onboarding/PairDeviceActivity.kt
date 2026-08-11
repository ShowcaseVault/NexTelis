package com.showcasevault.nextelis.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.network.ClaimCodeRequest
import com.showcasevault.nextelis.network.DeviceClaimRequest
import com.showcasevault.nextelis.network.NexTelisApiClient
import com.showcasevault.nextelis.network.UserCreateRequest
import com.showcasevault.nextelis.network.UserRegisteredResponse
import com.showcasevault.nextelis.session.SessionStore
import com.showcasevault.nextelis.ui.LoadingOverlay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Screen 1 of onboarding: register the user, then pair (claim) this
 * specific device using the one-time claim code the backend returns.
 * See backend/api/v1/routes/users.py and devices.py for the contract.
 *
 * If the email is already registered (409), the account can only be
 * re-paired by proving ownership with the recovery code shown once at
 * registration — email alone is not enough (see docs/FINDINGS.md).
 */
class PairDeviceActivity : AppCompatActivity() {

    private lateinit var inputDisplayName: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var sectionRecoveryCode: LinearLayout
    private lateinit var textRecoveryCode: TextView
    private lateinit var btnCopyRecoveryCode: Button
    private lateinit var checkRecoverySaved: CheckBox
    private lateinit var sectionRecoveryInput: LinearLayout
    private lateinit var inputRecoveryCode: TextInputEditText
    private lateinit var btnSubmitRecoveryCode: Button
    private lateinit var sectionClaim: LinearLayout
    private lateinit var textClaimCode: TextView
    private lateinit var textClaimExpiry: TextView
    private lateinit var textError: TextView
    private lateinit var loadingOverlay: LoadingOverlay
    private lateinit var btnRegister: Button
    private lateinit var btnClaimDevice: Button

    private var claimCode: String? = null
    private var recoveryCode: String? = null
    private var pendingReissueEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pair_device)

        inputDisplayName = findViewById(R.id.inputDisplayName)
        inputEmail = findViewById(R.id.inputEmail)
        sectionRecoveryCode = findViewById(R.id.sectionRecoveryCode)
        textRecoveryCode = findViewById(R.id.textRecoveryCode)
        btnCopyRecoveryCode = findViewById(R.id.btnCopyRecoveryCode)
        checkRecoverySaved = findViewById(R.id.checkRecoverySaved)
        sectionRecoveryInput = findViewById(R.id.sectionRecoveryInput)
        inputRecoveryCode = findViewById(R.id.inputRecoveryCode)
        btnSubmitRecoveryCode = findViewById(R.id.btnSubmitRecoveryCode)
        sectionClaim = findViewById(R.id.sectionClaim)
        textClaimCode = findViewById(R.id.textClaimCode)
        textClaimExpiry = findViewById(R.id.textClaimExpiry)
        textError = findViewById(R.id.textError)
        loadingOverlay = LoadingOverlay(findViewById(android.R.id.content))
        btnRegister = findViewById(R.id.btnRegister)
        btnClaimDevice = findViewById(R.id.btnClaimDevice)

        btnRegister.setOnClickListener { onRegisterClicked() }
        btnCopyRecoveryCode.setOnClickListener { copyRecoveryCode() }
        checkRecoverySaved.setOnCheckedChangeListener { _, checked -> btnClaimDevice.isEnabled = checked }
        btnSubmitRecoveryCode.setOnClickListener { onSubmitRecoveryCodeClicked() }
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
                recoveryCode = result.recovery_code
                showRecoveryCodeStep(result)
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    // Already registered — this device must prove ownership
                    // with the recovery code before we'll reissue a claim code.
                    pendingReissueEmail = email
                    showRecoveryInputStep()
                } else {
                    showError(describeNetworkError(e))
                }
            } catch (e: Exception) {
                showError(describeNetworkError(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun onSubmitRecoveryCodeClicked() {
        val email = pendingReissueEmail ?: return
        val enteredCode = inputRecoveryCode.text?.toString()?.trim().orEmpty()

        if (enteredCode.isEmpty()) {
            showError("Enter your recovery code to continue.")
            return
        }

        clearError()
        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = NexTelisApiClient.api.reissueClaimCode(
                    ClaimCodeRequest(email = email, recovery_code = enteredCode)
                )
                sectionRecoveryInput.visibility = LinearLayout.GONE
                showClaimStep(result.claim_code, result.claim_code_expires_at)
            } catch (e: Exception) {
                showError(describeNetworkError(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun copyRecoveryCode() {
        val code = recoveryCode ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("NexTelis recovery code", code))
    }

    private fun showRecoveryCodeStep(result: UserRegisteredResponse) {
        sectionRecoveryCode.visibility = LinearLayout.VISIBLE
        textRecoveryCode.text = result.recovery_code
        checkRecoverySaved.isChecked = false
        showClaimStep(result.claim_code, result.claim_code_expires_at)
        btnClaimDevice.isEnabled = false
    }

    private fun showRecoveryInputStep() {
        sectionRecoveryInput.visibility = LinearLayout.VISIBLE
    }

    private fun onClaimClicked() {
        val claimCode = claimCode ?: return
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

    private fun showClaimStep(code: String, expiresAt: String) {
        claimCode = code
        sectionClaim.visibility = LinearLayout.VISIBLE
        textClaimCode.text = code
        textClaimExpiry.text = getString(R.string.claim_code_expiry, formatExpiry(expiresAt))
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
