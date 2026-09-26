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
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
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
            while (c.moveToNext()) {
                val date = c.getLong(iDate)
                if (date > newest) newest = date
                val address = c.getString(iAddr)
                val body = c.getString(iBody)
                if (SmsFilter.accept(address, body)) {
                    out.put(JSONObject().put("body", body).put("date", date))
                }
            }
        }
        return out to newest
    }
}
