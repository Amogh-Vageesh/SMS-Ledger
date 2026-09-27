package `in`.vageesh.smsledger

import android.content.Context
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject

object SmsReader {
    /**
     * Reads bank transaction SMS received after [sinceMs].
     * Returns them as JSON (body + timestamp only) and the newest timestamp seen.
     */
    fun read(context: Context, sinceMs: Long): Pair<JSONArray, Long> {
        val out = JSONArray()
        var newest = sinceMs
        // SUBSCRIPTION_ID tells which SIM received the message, on dual-SIM phones; -1 if unavailable.
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.SUBSCRIPTION_ID)
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} > ?",
            arrayOf(sinceMs.toString()),
            "${Telephony.Sms.DATE} ASC"
        )?.use { c ->
            val iAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iSub = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
            while (c.moveToNext()) {
                val date = c.getLong(iDate)
                if (date > newest) newest = date
                val address = c.getString(iAddr)
                val body = c.getString(iBody)
                if (SmsFilter.accept(address, body)) {
                    val sub = if (iSub >= 0) c.getInt(iSub) else -1
                    out.put(JSONObject().put("body", body).put("date", date).put("address", address ?: "").put("sim", sub))
                }
            }
        }
        return out to newest
    }
}
