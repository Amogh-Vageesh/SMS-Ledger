package `in`.vageesh.smsledger

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Fires the moment an SMS arrives. The SMS itself stays in the inbox; the app reads it
 * from there when opened, so nothing is lost even if the notification is dismissed.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Long SMS arrive split into parts; join them per sender.
        parts.groupBy { it.originatingAddress }.forEach { (sender, msgs) ->
            val body = msgs.joinToString("") { it.messageBody ?: "" }
            if (SmsFilter.accept(sender, body)) notify(context, SmsFilter.summary(body), body)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun notify(context: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "New transactions", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "A bank SMS was captured for your ledger" }
            )
        }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("$title · tap to log")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, n)
        } catch (_: SecurityException) { }
    }

    companion object {
        const val CHANNEL = "txns"
        const val NOTIFY_ID = 1001
    }
}
