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
    private val action = Regex(
        "debited|credited|spent|sent|paid|withdrawn|received|refund|deducted|purchase|" +
            "due|payable|statement|balance|avl\\.? ?bal|available limit",
        RegexOption.IGNORE_CASE
    )

    fun isBankSender(address: String?): Boolean {
        val a = address?.trim() ?: return false
        if (a.none { it.isLetter() }) return false   // phone numbers are people, not banks
        return bankSender.matches(a)
    }

    fun looksLikeTransaction(body: String?): Boolean {
        val b = body ?: return false
        return amount.containsMatchIn(b) && action.containsMatchIn(b)
    }

    fun accept(address: String?, body: String?) = isBankSender(address) && looksLikeTransaction(body)

    /** Short text for the notification, e.g. "₹486.00 spent". */
    fun summary(body: String): String {
        val amt = amount.find(body)?.value
            ?.replace(Regex("(?i)rs\\.?|inr|₹"), "")?.trim()
        val lower = body.lowercase()
        val di = listOf("debited", "spent", "sent", "paid", "withdrawn", "deducted")
            .map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
        val ci = listOf("credited", "received", "refund")
            .map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
        val kind = if (ci < di) "received" else "spent"
        return if (amt != null) "₹$amt $kind" else "Bank transaction"
    }
}
