package `in`.vageesh.smsledger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray

/**
 * Bill-due and renewal reminders. The page sends the full list each time it changes;
 * we cancel the old alarms and set the new ones. Alarms are inexact (no special
 * permission needed) and are restored after a reboot.
 */
object Reminders {
    private const val PREFS = "reminders"

    fun set(context: Context, json: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        cancelAll(context, prefs.getString("list", "[]") ?: "[]")
        prefs.edit().putString("list", json).apply()
        schedule(context, json)
    }

    fun restore(context: Context) {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("list", "[]") ?: "[]"
        schedule(context, json)
    }

    private fun schedule(context: Context, json: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        val arr = try { JSONArray(json) } catch (e: Exception) { return }
        val now = System.currentTimeMillis()
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            val at = r.optLong("at")
            if (at <= now) continue
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, r.optString("id"), r.optString("title"), r.optString("text")))
        }
    }

    private fun cancelAll(context: Context, json: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        val arr = try { JSONArray(json) } catch (e: Exception) { return }
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            am.cancel(pending(context, r.optString("id"), "", ""))
        }
    }

    private fun pending(context: Context, id: String, title: String, text: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("in.vageesh.smsledger.REMIND.$id")
            .putExtra("title", title).putExtra("text", text).putExtra("nid", id.hashCode())
        return PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifier.show(
            context, Notifier.CH_BILLS, intent.getIntExtra("nid", 2000),
            intent.getStringExtra("title") ?: "Reminder", intent.getStringExtra("text") ?: ""
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) { Reminders.restore(context); BackupScheduler.reschedule(context) }
    }
}
