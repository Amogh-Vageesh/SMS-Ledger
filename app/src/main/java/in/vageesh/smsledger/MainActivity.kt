package `in`.vageesh.smsledger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var pageReady = false
    private var pendingSave: String? = null
    private val prefs by lazy { getSharedPreferences("ledger", MODE_PRIVATE) }

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            pushStatus()
            if (res[Manifest.permission.READ_SMS] == true) firstImportOrScan()
        }

    // <input type="file"> in the page (restore backup, FinArt import) needs a native picker.
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        fileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        fileCallback = null
    }

    private val family by lazy {
        FamilySync(this, { msg -> runOnUiThread { toastJs(msg) } },
            { _ -> runOnUiThread { web.evaluateJavascript("window.familyArrived && window.familyArrived()", null) } })
    }
    private var pendingFamily: Pair<String, String>? = null
    private val familyPerms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        val job = pendingFamily; pendingFamily = null
        if (res.values.all { it } && job != null) family.start(job.first, job.second)
        else toastJs("Family sync needs the Nearby devices permission" + (if (Build.VERSION.SDK_INT <= 32) " and Location" else "") + ".")
    }

    private fun familyPermissions(): Array<String> {
        val p = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= 32) p += Manifest.permission.ACCESS_FINE_LOCATION
        if (Build.VERSION.SDK_INT >= 31) p += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= 33) p += Manifest.permission.NEARBY_WIFI_DEVICES
        return p.toTypedArray()
    }

    private fun isNight() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private val saveCsv =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { writeSave(it) }
    private val saveJson =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { writeSave(it) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Follow the phone's dark / light setting.
        val night = isNight()
        window.statusBarColor = Color.parseColor(if (night) "#17140F" else "#F4EFE4")
        window.navigationBarColor = Color.parseColor(if (night) "#201C16" else "#FBF8F1")
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }

        val assets = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor(if (night) "#17140F" else "#F4EFE4"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true      // the ledger lives in the page's localStorage
            settings.allowFileAccess = false
            addJavascriptInterface(Bridge(), "Android")
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams
                ): Boolean {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = callback
                    return try {
                        pickFile.launch(arrayOf("text/*", "application/json", "application/octet-stream", "*/*")); true
                    } catch (e: Exception) {
                        fileCallback = null; false
                    }
                }
            }
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView, request: WebResourceRequest
                ): WebResourceResponse? = assets.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView, url: String) {
                    if (pageReady) return
                    pageReady = true
                    view.evaluateJavascript("document.documentElement.dataset.theme='${if (isNight()) "dark" else "light"}'", null)
                    onPageReady()
                }
            }
        }
        setContentView(web)
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        onBackPressedDispatcher.addCallback(this) {
            web.evaluateJavascript("window.appBack ? window.appBack() : false") { handled ->
                if (handled != "true") finish()
            }
        }
    }

    override fun onDestroy() {
        runCatching { family.stop() }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        NotificationManagerCompat.from(this).cancel(SmsReceiver.NOTIFY_ID)
        if (pageReady) {
            pushStatus()
            if (hasSms()) scanInbox()
            web.evaluateJavascript("window.appOnline && window.appOnline()", null)
        }
    }

    private fun onPageReady() {
        pushStatus()
        web.evaluateJavascript("window.appOnline && window.appOnline()", null)
        when {
            hasSms() -> firstImportOrScan()
            !prefs.getBoolean("asked", false) -> askPermissions()
        }
    }

    /** On the very first run, let the person pick how far back to read (3 months to 5 years). */
    private fun firstImportOrScan() {
        if (prefs.contains("lastScan")) scanInbox()
        else web.evaluateJavascript("window.chooseImportRange && window.chooseImportRange()", null)
    }

    private fun hasSms() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    private fun askPermissions() {
        prefs.edit().putBoolean("asked", true).apply()
        val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        permLauncher.launch(perms.toTypedArray())
    }

    @Volatile private var scanning = false

    /**
     * Reads bank SMS newer than [sinceOverride] (or since the last scan) and hands them to the
     * page in batches, so a 5-year import of thousands of messages doesn't freeze the screen.
     * [announce] shows progress and a summary; background top-ups on app open stay quiet
     * unless they find something.
     */
    private fun scanInbox(sinceOverride: Long? = null, announce: Boolean = false) {
        if (scanning) return
        scanning = true
        val since = sinceOverride ?: prefs.getLong("lastScan", System.currentTimeMillis() - 90L * DAY)
        if (announce) toastJs("Reading your SMS inbox…")
        thread {
            val (msgs, newest) = try {
                SmsReader.read(this, since)
            } catch (e: SecurityException) {
                scanning = false
                return@thread
            }
            runOnUiThread { deliver(msgs, newest, announce) }
        }
    }

    private fun deliver(msgs: JSONArray, newest: Long, announce: Boolean) {
        val total = msgs.length()
        val saveMark = {
            prefs.edit().putLong("lastScan", maxOf(newest, prefs.getLong("lastScan", 0L))).apply()
        }
        if (total == 0) {
            saveMark(); scanning = false
            if (announce) web.evaluateJavascript("window.importFinished && window.importFinished(0, 0)", null)
            return
        }
        var added = 0
        var accepted = true
        var start = 0
        while (start < total) {
            val end = minOf(start + BATCH, total)
            val batch = JSONArray()
            for (i in start until end) batch.put(msgs.get(i))
            val isLast = end == total
            val progress = if (announce && total > BATCH)
                "window.importProgress && window.importProgress($end, $total);" else ""
            // evaluateJavascript runs in order on the UI thread, so batches land sequentially.
            web.evaluateJavascript("${progress}window.ingestSms ? window.ingestSms($batch, true) : -1") { r ->
                val n = r?.toIntOrNull() ?: -1
                if (n < 0) accepted = false else added += n
                if (isLast) {
                    if (accepted) saveMark()   // move the mark only once the page has taken them
                    scanning = false
                    if (announce || added > 0)
                        web.evaluateJavascript("window.importFinished && window.importFinished($added, $total)", null)
                }
            }
            start = end
        }
    }

    private fun pushStatus() {
        web.evaluateJavascript("window.appStatus && window.appStatus(${hasSms()})", null)
    }

    private fun toastJs(msg: String) {
        web.evaluateJavascript(
            "(function(){var t=document.createElement('div');t.className='toast';" +
                "t.textContent=${JSONObject.quote(msg)};document.body.appendChild(t);" +
                "setTimeout(function(){t.remove()},2400)})()", null
        )
    }

    private fun writeSave(uri: Uri?) {
        val data = pendingSave
        pendingSave = null
        if (uri == null || data == null) return
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
            toastJs("File saved.")
        } catch (e: Exception) {
            toastJs("Couldn't save the file.")
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun hasSmsAccess(): Boolean = hasSms()

        @JavascriptInterface
        fun requestSms() = runOnUiThread {
            val canAsk = !prefs.getBoolean("asked", false) ||
                shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)
            if (canAsk) askPermissions()
            else startActivity(   // permanently denied: open app settings instead
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        }

        /**
         * Import (or re-import) bank SMS from the last [days]; 0 means every SMS on the phone.
         * Entries already in the ledger are skipped by the page, so rescanning is safe.
         */
        @JavascriptInterface
        fun importRange(days: Int) = runOnUiThread {
            val since = if (days <= 0) 0L else System.currentTimeMillis() - days.coerceAtMost(MAX_DAYS) * DAY
            if (hasSms()) scanInbox(since, announce = true) else requestSms()
        }

        /** The ledger is kept in a private app file rather than WebView storage, so years of entries fit. */
        @JavascriptInterface
        fun saveState(json: String) {
            synchronized(stateLock) {
                val tmp = File(filesDir, "ledger.json.tmp")
                tmp.writeText(json)
                tmp.renameTo(File(filesDir, "ledger.json"))
            }
        }

        /** Exchange rate for a currency on a date; answers through window.rateResult(id, rate). */
        @JavascriptInterface
        fun getRate(id: String, cur: String, date: String) {
            thread {
                val r = Rates.get(cur, date) ?: 0.0
                runOnUiThread { web.evaluateJavascript("window.rateResult && window.rateResult(${JSONObject.quote(id)}, $r)", null) }
            }
        }

        /** Start a Bluetooth / nearby Wi-Fi sync, sending [json] (this phone's family data) as [name]. */
        @JavascriptInterface
        fun familySync(name: String, json: String) = runOnUiThread {
            val need = familyPermissions().filter {
                ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
            }
            if (need.isEmpty()) family.start(name, json)
            else { pendingFamily = name to json; familyPerms.launch(need.toTypedArray()) }
        }

        /** Hands the page the ledger received from the other phone, once. */
        @JavascriptInterface
        fun takeFamilyInbox(): String {
            val f = File(filesDir, "family-inbox.json")
            if (!f.exists()) return ""
            val t = f.readText(); f.delete(); return t
        }

        /** Bill-due and renewal reminders, as JSON [{id, at, title, text}]. */
        @JavascriptInterface
        fun setReminders(json: String) = Reminders.set(this@MainActivity, json)

        @JavascriptInterface
        fun loadState(): String = synchronized(stateLock) {
            File(filesDir, "ledger.json").takeIf { it.exists() }?.readText() ?: ""
        }

        @JavascriptInterface
        fun saveFile(name: String, data: String) = runOnUiThread {
            pendingSave = data
            if (name.endsWith(".csv")) saveCsv.launch(name) else saveJson.launch(name)
        }
    }

    private val stateLock = Any()

    companion object {
        const val DAY = 24L * 60 * 60 * 1000
        const val MAX_DAYS = 36500         // ranges are capped at 100 years; 0 means everything
        const val BATCH = 400
    }
}
