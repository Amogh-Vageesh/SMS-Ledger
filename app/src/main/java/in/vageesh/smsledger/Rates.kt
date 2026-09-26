package `in`.vageesh.smsledger

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Rupees per one unit of a currency on a given day.
 * Sources, in order: currencies pegged to the US dollar (AED, SAR, QAR, OMR, BHD) are worked out
 * from that day's USD rate; NPR has a fixed peg to INR; everything else comes from the European
 * Central Bank's published rates (via Frankfurter), then a community daily-rates feed as backup.
 */
object Rates {
    private val USD_PEG = mapOf("AED" to 3.6725, "SAR" to 3.75, "QAR" to 3.64, "OMR" to 0.3845, "BHD" to 0.376)
    private val cache = mutableMapOf<String, Double>()

    fun get(cur: String, dateIn: String): Double? {
        val c = cur.uppercase()
        if (c == "INR") return 1.0
        if (c == "NPR") return 0.625
        val date = if (dateIn > LocalDate.now().toString()) "latest" else dateIn
        cache["$c@$date"]?.let { return it }
        val r = USD_PEG[c]?.let { peg -> get("USD", date)?.div(peg) }
            ?: frankfurter(c, date)
            ?: communityFeed(c, date)
        if (r != null && r > 0) cache["$c@$date"] = r
        return r
    }

    private fun frankfurter(c: String, date: String): Double? {
        for (url in listOf(
            "https://api.frankfurter.dev/v1/$date?base=$c&symbols=INR",
            "https://api.frankfurter.app/$date?from=$c&to=INR"
        )) {
            val v = http(url)?.let { runCatching { JSONObject(it).getJSONObject("rates").optDouble("INR") }.getOrNull() }
            if (v != null && !v.isNaN() && v > 0) return v
        }
        return null
    }

    private fun communityFeed(c: String, date: String): Double? {
        val lc = c.lowercase()
        for (url in listOf(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$date/v1/currencies/$lc.json",
            "https://$date.currency-api.pages.dev/v1/currencies/$lc.json"
        )) {
            val v = http(url)?.let { runCatching { JSONObject(it).getJSONObject(lc).optDouble("inr") }.getOrNull() }
            if (v != null && !v.isNaN() && v > 0) return v
        }
        return null
    }

    private fun http(url: String): String? = try {
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = 8000; con.readTimeout = 8000
        con.setRequestProperty("User-Agent", "SMSLedger/1.3")
        if (con.responseCode == 200) con.inputStream.bufferedReader().use { it.readText() } else null
    } catch (e: Exception) { null }
}
