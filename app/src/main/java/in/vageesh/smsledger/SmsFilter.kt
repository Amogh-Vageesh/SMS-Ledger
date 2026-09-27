package `in`.vageesh.smsledger

/**
 * Decides which SMS look like bank transaction alerts.
 * Only messages passing both checks are handed to the ledger,
 * so SMS from people (phone-number senders) are never read into it.
 */
object SmsFilter {
    // Indian DLT headers: "AD-HDFCBK", "VM-ICICIT-S", "JX-SBIUPI", or bare "HDFCBK".
    private val bankSender = Regex("^([A-Z]{2}-)?[A-Z0-9]{5,9}(-[A-Z])?$", RegexOption.IGNORE_CASE)
    private val amount = Regex("(rs\\.?|inr|₹)\\s*[\\d,]+(\\.\\d{1,2})?", RegexOption.IGNORE_CASE)
    // whole words, so "presents" doesn't match "sent"
    private val action = Regex(
        "\\b(debited|credited|spent|sent|paid|withdrawn|received|refund|deducted|purchase|" +
            "due|payable|statement|balance|avl\\.? ?bal|available limit)\\b",
        RegexOption.IGNORE_CASE
    )
    // marketing texts that quote a price ("designs starting at Rs.30,000", "flat 20% off")
    private val promo = Regex(
        "starting (at|from)|exciting offers?|shop now|buy now|book (now|in advance)|explore:|click here|" +
            "limited period|flat \\d+% off|up ?to \\d+% off|t&c apply|pre-?approved|apply now|lock (gold|the) rates?|" +
            "new collection|use code|coupon",
        RegexOption.IGNORE_CASE
    )
    private val hasAccount = Regex("(a/?c|acct|account|card)\\s*(no\\.?\\s*)?[:\\s]*[x*]+\\s?\\d{3,6}|\\b[x*]{2,}\\d{3,6}\\b|upi ref|ref no|utr", RegexOption.IGNORE_CASE)

    fun isBankSender(address: String?): Boolean {
        val a = address?.trim() ?: return false
        if (a.none { it.isLetter() }) return false   // phone numbers are people, not banks
        return bankSender.matches(a)
    }

    fun looksLikeTransaction(body: String?): Boolean {
        val b = body ?: return false
        return amount.containsMatchIn(b) && action.containsMatchIn(b)
    }

    fun accept(address: String?, body: String?) =
        isBankSender(address) && looksLikeTransaction(body) &&
            !(body != null && promo.containsMatchIn(body) && !hasAccount.containsMatchIn(body))

    /** Short text for the notification, e.g. "₹486.00 spent". */
    fun summary(body: String, kn: Boolean = false): String {
        val amt = amount.find(body)?.value
            ?.replace(Regex("(?i)rs\\.?|inr|₹"), "")?.trim()
        val lower = body.lowercase()
        val di = listOf("debited", "spent", "sent", "paid", "withdrawn", "deducted")
            .map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
        val ci = listOf("credited", "received", "refund")
            .map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
        val kind = if (ci < di) (if (kn) "ಬಂದಿದೆ" else "received") else (if (kn) "ಖರ್ಚಾಗಿದೆ" else "spent")
        return if (amt != null) "₹$amt $kind" else if (kn) "ಬ್ಯಾಂಕ್ ವಹಿವಾಟು" else "Bank transaction"
    }
}
