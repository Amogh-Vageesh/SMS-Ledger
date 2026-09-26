package `in`.vageesh.smsledger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var pageReady = false
    private var pendingSave: String? = null
    private val prefs by lazy { getSharedPreferences("ledger", MODE_PRIVATE) }

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            pushStatus()
            if (res[Manifest.permission.READ_SMS] == true) scanInbox(firstRunSince())
        }

    private val saveCsv =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { writeSave(it) }
    private val saveJson =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { writeSave(it) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#17140F")
        window.navigationBarColor = Color.parseColor("#201C16")

        val assets = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#17140F"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true      // the ledger lives in the page's localStorage
            settings.allowFileAccess = false
            addJavascriptInterface(Bridge(), "Android")
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView, request: WebResourceRequest
                ): WebResourceResponse? = assets.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView, url: String) {
                    if (pageReady) return
                    pageReady = true
                    view.evaluateJavascript("document.documentElement.dataset.theme='dark'", null)
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

    override fun onResume() {
        super.onResume()
        NotificationManagerCompat.from(this).cancel(SmsReceiver.NOTIFY_ID)
        if (pageReady) {
            pushStatus()
            if (hasSms()) scanInbox()
        }
    }

    private fun onPageReady() {
        pushStatus()
        when {
            hasSms() -> scanInbox(firstRunSince())
            !prefs.getBoolean("asked", false) -> askPermissions()
        }
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

    /** First run imports the last 90 days; after that, only what's new. */
    private fun firstRunSince(): Long? =
        if (prefs.contains("lastScan")) null else System.currentTimeMillis() - 90L * DAY

    private fun scanInbox(sinceOverride: Long? = null) {
        val since = sinceOverride ?: prefs.getLong("lastScan", System.currentTimeMillis() - 90L * DAY)
        thread {
            val (msgs, newest) = try {
                SmsReader.read(this, since)
            } catch (e: SecurityException) {
                return@thread
            }
            runOnUiThread {
                val saveMark = {
                    prefs.edit().putLong("lastScan", maxOf(newest, prefs.getLong("lastScan", 0L))).apply()
                }
                if (msgs.length() == 0) {
                    saveMark()
                } else {
                    web.evaluateJavascript("window.ingestSms ? window.ingestSms($msgs) : -1") { r ->
                        if (r != "-1") saveMark()   // move the mark only once the page has taken them
                    }
                }
            }
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

        @JavascriptInterface
        fun rescan(days: Int) = runOnUiThread {
            if (hasSms()) scanInbox(System.currentTimeMillis() - days.coerceIn(1, 365) * DAY)
        }

        @JavascriptInterface
        fun saveFile(name: String, data: String) = runOnUiThread {
            pendingSave = data
            if (name.endsWith(".csv")) saveCsv.launch(name) else saveJson.launch(name)
        }
    }

    companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
