package `in`.vageesh.smsledger

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifier {
    const val CH_TXNS = "txns"
    const val CH_BILLS = "bills"

    @SuppressLint("MissingPermission")
    fun show(context: Context, channel: String, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channel) == null) {
            val (name, desc) = if (channel == CH_BILLS)
                "Bills and renewals" to "Reminders before bills are due and subscriptions renew"
            else "New transactions" to "A bank SMS was captured for your ledger"
            nm.createNotificationChannel(
                NotificationChannel(channel, name, NotificationManager.IMPORTANCE_DEFAULT).apply { description = desc }
            )
        }
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(id, n) } catch (_: SecurityException) { }
    }
}
