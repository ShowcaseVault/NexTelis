package com.showcasevault.nextelis

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.showcasevault.nextelis.account.PhoneAccountManager
import com.showcasevault.nextelis.permissions.PermissionManager

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        PermissionManager.logStatus(this)

        if (PermissionManager.allGranted(this)) {
            registerAccount()
        } else {
            PermissionManager.requestAll(this)
        }

        findViewById<Button>(R.id.btnTestCall).setOnClickListener {
            openNativeDialer()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionManager.REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                registerAccount()
            } else {
                statusText.text = "Permissions denied"
            }
        }
    }

    private fun registerAccount() {
        PhoneAccountManager.register(this)
        statusText.text = "NexTelis registered ✓\nOpen your dialer and call — pick NexTelis when prompted"
    }

    private fun openNativeDialer() {
        // Just open the native Samsung dialer
        // Android will show SIM / NexTelis chooser
        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
        startActivity(intent)
    }
}