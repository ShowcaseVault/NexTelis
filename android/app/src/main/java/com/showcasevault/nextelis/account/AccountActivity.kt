package com.showcasevault.nextelis.account

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.onboarding.ServerSetupActivity
import com.showcasevault.nextelis.session.SessionStore

/** Profile + server info, reached from Home's navigation drawer. */
class AccountActivity : AppCompatActivity() {

    private lateinit var textDisplayName: TextView
    private lateinit var textNumberValue: TextView
    private lateinit var textServerAddress: TextView
    private lateinit var btnChangeServer: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        // app:navigationIcon/title are set here instead of in XML — see the
        // comment in HomeActivity.onCreate for why (AAPT2 resource-linking
        // issue on this AGP 9.3.1 toolchain).
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val backIcon = ContextCompat.getDrawable(this, R.drawable.ic_back)?.mutate()?.also {
            DrawableCompat.setTint(it, getColor(R.color.text_primary))
        }
        toolbar.navigationIcon = backIcon
        toolbar.title = getString(R.string.account_title)
        toolbar.setTitleTextColor(getColor(R.color.text_primary))
        toolbar.setNavigationOnClickListener { finish() }

        textDisplayName = findViewById(R.id.textDisplayName)
        textNumberValue = findViewById(R.id.textNumberValue)
        textServerAddress = findViewById(R.id.textServerAddress)
        btnChangeServer = findViewById(R.id.btnChangeServer)

        btnChangeServer.setOnClickListener {
            startActivity(Intent(this, ServerSetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        textDisplayName.text = SessionStore.getDisplayName(this).orEmpty()
        textNumberValue.text = SessionStore.getNumberValue(this).orEmpty()

        val host = SessionStore.getServerHost(this).orEmpty()
        val port = SessionStore.getServerPort(this)
        textServerAddress.text = "$host:$port"
    }
}
