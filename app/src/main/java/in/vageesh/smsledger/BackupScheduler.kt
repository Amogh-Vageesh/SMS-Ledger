package `in`.vageesh.smsledger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.io.File

/** Lightweight rotating automatic backups of the private ledger file. */
object BackupScheduler {
    private const val PREF = "backup_schedule"
    private const val KEY = "frequency"
    private const val ACTION = "in.vageesh.smsledger.AUTO_BACKUP"
    private const val REQUEST = 8146

    fun set(context: Context, frequency: String) {
        val f = if (frequency in setOf("off", "daily", "weekly")) frequency else "off"
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, f).apply()
        cancel(context)
        if (f != "off") schedule(context, f)
    }

    fun get(context: Context): String = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "off") ?: "off"

    fun reschedule(context: Context) {
        val f = get(context)
        if (f != "off") { cancel(context); schedule(context, f) }
    }

    private fun schedule(context: Context, frequency: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(context, REQUEST, Intent(context, Receiver::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val interval = if (frequency == "weekly") 7L * 24 * 60 * 60 * 1000 else 24L * 60 * 60 * 1000
        val first = System.currentTimeMillis() + interval
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, interval, pi)
    }

    private fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(context, REQUEST, Intent(context, Receiver::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
    }

    fun backupNow(context: Context): Boolean {
        return try {
            val source = File(context.filesDir, "ledger.json")
            if (!source.exists()) return false
            val dir = File(context.filesDir, "automatic-backups")
            dir.mkdirs()
            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date())
            File(dir, "ledger-$stamp.json").writeBytes(source.readBytes())
            val files = dir.listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()
            files.drop(7).forEach { it.delete() }
            true
        } catch (_: Exception) { false }
    }

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == ACTION) backupNow(context)
        }
    }

    fun latestBackup(context: Context): File? = File(context.filesDir, "automatic-backups").listFiles()?.maxByOrNull { it.lastModified() }
}
