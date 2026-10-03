package `in`.vageesh.smsledger

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

/** Receives new SMS broadcasts and queues accepted bank SMS for the WebView ledger. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return
        val prefs = context.getSharedPreferences("ledger", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("smsConsentConfirmed", false)) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        parts.groupBy { it.originatingAddress ?: "" }.forEach { (sender, msgs) ->
            val body = msgs.joinToString("") { it.messageBody ?: "" }
            if (!SmsFilter.accept(sender, body)) return@forEach
            val date = msgs.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
            val sim = runCatching { msgs.firstOrNull()?.subscriptionId ?: -1 }.getOrDefault(-1)
            enqueue(context, JSONObject().put("body", body).put("date", date).put("address", sender).put("sim", sim))
            context.sendBroadcast(Intent(ACTION_SMS_QUEUED).setPackage(context.packageName))
            Notifier.show(
                context, Notifier.CH_TXNS, NOTIFY_ID,
                "${SmsFilter.summary(body, Lang.kn(context))} · ${Lang.t(context, "tap to log", "ಸೇರಿಸಲು ಒತ್ತಿ")}", body
            )
        }
    }

    private fun enqueue(context: Context, item: JSONObject) {
        val prefs = context.getSharedPreferences("ledger", Context.MODE_PRIVATE)
        val existing = runCatching { JSONArray(prefs.getString(PENDING_KEY, "[]")) }.getOrElse { JSONArray() }
        val out = JSONArray()
        val start = maxOf(0, existing.length() - 499)
        for (i in start until existing.length()) out.put(existing.get(i))
        out.put(item)
        prefs.edit().putString(PENDING_KEY, out.toString()).apply()
    }

    companion object {
        const val NOTIFY_ID = 1001
        const val PENDING_KEY = "pendingSmsQueue"
        const val ACTION_SMS_QUEUED = "in.vageesh.smsledger.ACTION_SMS_QUEUED"
    }
}
