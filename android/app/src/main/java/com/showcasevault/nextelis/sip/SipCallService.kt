package com.showcasevault.nextelis.sip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.showcasevault.nextelis.MainActivity
import com.showcasevault.nextelis.R
import com.showcasevault.nextelis.session.SessionStore

/**
 * Keeps the SIP registration alive while the app isn't in the foreground —
 * required for incoming calls to actually ring. Started from HomeActivity
 * once a number + SIP password are available.
 */
class SipCallService : Service() {

    companion object {
        private const val CHANNEL_ID = "nextelis_sip"
        private const val NOTIFICATION_ID = 1001

        fun intent(context: android.content.Context) = Intent(context, SipCallService::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = SessionStore.getNumberValue(this)
        val sipPassword = SessionStore.getSipPassword(this)

        if (number != null && sipPassword != null) {
            startForeground(NOTIFICATION_ID, buildNotification(number))
            // Must be installed before/with the registration: an inbound
            // INVITE can arrive the moment we're registered, and without a
            // listener it would be silently dropped.
            SipCallRouter.install(this)
            SipManager.start(this, number, sipPassword)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        SipManager.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(number: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.sip_notification_title))
        .setContentText(getString(R.string.sip_notification_text, number))
        .setSmallIcon(R.mipmap.ic_launcher)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sip_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
