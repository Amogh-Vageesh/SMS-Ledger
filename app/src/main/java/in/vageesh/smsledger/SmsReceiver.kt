package `in`.vageesh.smsledger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

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
            if (SmsFilter.accept(sender, body)) notify(context, SmsFilter.summary(body, Lang.kn(context)), body)
        }
    }

    private fun notify(context: Context, title: String, body: String) =
        Notifier.show(context, Notifier.CH_TXNS, NOTIFY_ID,
            "$title · " + Lang.t(context, "tap to log", "ಸೇರಿಸಲು ಒತ್ತಿ"), body)

    companion object {
        const val NOTIFY_ID = 1001
    }
}
