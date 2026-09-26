package `in`.vageesh.smsledger

import android.content.Context

/** The language picked in the app (English or Kannada), for the few texts shown outside the page. */
object Lang {
    fun kn(context: Context) =
        context.getSharedPreferences("ledger", Context.MODE_PRIVATE).getString("lang", "en") == "kn"
    fun t(context: Context, en: String, kn: String) = if (kn(context)) kn else en
}
