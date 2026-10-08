package `in`.vageesh.smsledger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import android.content.res.Configuration
import android.location.LocationManager
import android.telephony.SubscriptionManager
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import org.apache.poi.ss.usermodel.WorkbookFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.security.MessageDigest
import kotlin.concurrent.thread

class MainActivity : FragmentActivity() {

    private lateinit var web: WebView
    private lateinit var rootFrame: FrameLayout
    private var lockOverlay: View? = null
    private var pageReady = false
    private var appUnlocked = false
    private var lockPromptShowing = false
    private var pendingSave: String? = null
    private val prefs by lazy { getSharedPreferences("ledger", MODE_PRIVATE) }

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            pushStatus()
            if (res[Manifest.permission.READ_SMS] == true) {
                web.evaluateJavascript("window.smsPermissionGranted && window.smsPermissionGranted()", null)
            }
        }

    // <input type="file"> in the page (restore backup, FinArt import) needs a native picker.
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var statementCallback: ValueCallback<String>? = null
    private var loanDocumentCallback: ValueCallback<String>? = null
    private var insurancePolicyCallback: ValueCallback<String>? = null
    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        fileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        fileCallback = null
    }

    private val pickStatement = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val cb = statementCallback
        statementCallback = null
        if (uris.isNullOrEmpty()) { cb?.onReceiveValue("[]"); return@registerForActivityResult }
        lifecycleScope.launch {
            val payload = JSONArray()
            uris.forEach { uri ->
                try {
                    val text = readStatementText(uri)
                    payload.put(JSONObject().put("name", uri.lastPathSegment ?: "statement").put("text", text))
                } catch (e: Throwable) {
                    payload.put(JSONObject().put("name", uri.lastPathSegment ?: "statement").put("error", e.message ?: e.javaClass.simpleName))
                }
            }
            cb?.onReceiveValue(payload.toString())
        }
    }

    private val pickInsurancePolicyFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val cb = insurancePolicyCallback
        insurancePolicyCallback = null
        if (uris.isNullOrEmpty()) { cb?.onReceiveValue("[]"); return@registerForActivityResult }
        lifecycleScope.launch {
            val payload = JSONArray()
            uris.forEach { uri ->
                try {
                    val text = readStatementText(uri)
                    payload.put(JSONObject().put("name", uri.lastPathSegment ?: "policy-document").put("text", text))
                } catch (e: Throwable) {
                    payload.put(JSONObject().put("name", uri.lastPathSegment ?: "policy-document").put("error", e.message ?: e.javaClass.simpleName))
                }
            }
            cb?.onReceiveValue(payload.toString())
        }
    }

    private val pickLoanDocumentFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val cb = loanDocumentCallback
        loanDocumentCallback = null
        if (uris.isNullOrEmpty()) { cb?.onReceiveValue("[]"); return@registerForActivityResult }
        lifecycleScope.launch {
            val payload = JSONArray()
            uris.forEach { uri ->
                try {
                    val text = readStatementText(uri)
                    payload.put(JSONObject().put("name", uri.lastPathSegment ?: "loan-document").put("text", text))
                } catch (e: Throwable) {
                    payload.put(JSONObject().put("name", uri.lastPathSegment ?: "loan-document").put("error", e.message ?: e.javaClass.simpleName))
                }
            }
            cb?.onReceiveValue(payload.toString())
        }
    }

    private fun readStatementText(uri: Uri): String {
        val mime = contentResolver.getType(uri) ?: ""
        val name = uri.lastPathSegment?.lowercase() ?: ""
        if (mime.contains("pdf") || name.endsWith(".pdf")) {
            PDFBoxResourceLoader.init(applicationContext)
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to open statement." }
                PDDocument.load(input).use { doc ->
                    val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                    return stripper.getText(doc)
                }
            }
        }
        if (mime.contains("excel") || mime.contains("spreadsheet") || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".xlsm")) {
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to open statement." }
                WorkbookFactory.create(input).use { workbook ->
                    val sheet = workbook.getSheetAt(0)
                    val out = StringBuilder()
                    val formatter = org.apache.poi.ss.usermodel.DataFormatter()
                    for (row in sheet) {
                        val cells = (0 until row.lastCellNum.toInt().coerceAtLeast(0)).map { c ->
                            val value = formatter.formatCellValue(row.getCell(c))
                            "\"" + value.replace("\"", "\"\"") + "\""
                        }
                        if (cells.isNotEmpty()) out.append(cells.joinToString(",")).append('\n')
                    }
                    return out.toString()
                }
            }
        }
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open statement." }
            return InputStreamReader(input, Charsets.UTF_8).readText()
        }
    }

    private val cloudAuth by lazy { CloudAuth(this) }

    private val smsQueueReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            if (intent.action == ACTION_SMS_QUEUED) runOnUiThread { drainPendingSms() }
        }
    }

    private val googleDiagnosticLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            val json = try {
                cloudAuth.handleLegacyResult(result.data).fold(
                    onSuccess = { it.put("ok", true).put("stage", "complete").toString() },
                    onFailure = { e ->
                        JSONObject().put("ok", false)
                            .put("cancelled", result.resultCode != RESULT_OK)
                            .put("stage", "legacy_google_or_firebase")
                            .put("error", e.message ?: e.javaClass.simpleName).toString()
                    }
                )
            } catch (e: Throwable) {
                JSONObject().put("ok", false).put("stage", "diagnostic_callback")
                    .put("error", e.message ?: e.javaClass.simpleName).toString()
            }
            web.evaluateJavascript("window.googleDiagnosticResult && window.googleDiagnosticResult(${JSONObject.quote(json)})", null)
        }
    }

    private val legacyGoogleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            val json = try {
                cloudAuth.handleLegacyResult(result.data).fold(
                    onSuccess = { it.put("ok", true).toString() },
                    onFailure = { e ->
                        JSONObject().put("ok", false).put("cancelled", result.resultCode != RESULT_OK)
                            .put("error", e.message ?: e.javaClass.simpleName).toString()
                    }
                )
            } catch (e: Throwable) {
                JSONObject().put("ok", false).put("cancelled", result.resultCode != RESULT_OK)
                    .put("error", e.message ?: e.javaClass.simpleName).toString()
            }
            web.evaluateJavascript("window.cloudSignInResult && window.cloudSignInResult(${JSONObject.quote(json)})", null)
        }
    }
    private val familyCloud by lazy { FamilyCloud(this,
        { json -> runOnUiThread { web.evaluateJavascript("window.familyCloudArrived && window.familyCloudArrived(${JSONObject.quote(json)})", null) } },
        { msg -> runOnUiThread { web.evaluateJavascript("window.familyCloudError && window.familyCloudError(${JSONObject.quote(msg)})", null) } }
    ) }

    private val family by lazy {
        FamilySync(this, { msg -> runOnUiThread { toastJs(msg) } },
            { _ -> runOnUiThread { web.evaluateJavascript("window.familyArrived && window.familyArrived()", null) } })
    }
    private var pendingFamily: Pair<String, String>? = null
    private val familyPerms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        val job = pendingFamily; pendingFamily = null
        if (res.values.all { it } && job != null) family.start(job.first, job.second)
        else toastJs(Lang.t(this, "Family sync needs the Nearby devices permission" + (if (Build.VERSION.SDK_INT <= 32) " and Location" else "") + ".",
            "ಕುಟುಂಬ ಸಿಂಕ್‌ಗೆ ಹತ್ತಿರದ ಸಾಧನಗಳ ಅನುಮತಿ" + (if (Build.VERSION.SDK_INT <= 32) " ಮತ್ತು ಸ್ಥಳದ ಅನುಮತಿ" else "") + " ಬೇಕು."))
    }

    private fun familyPermissions(): Array<String> {
        val p = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= 32) p += Manifest.permission.ACCESS_FINE_LOCATION
        if (Build.VERSION.SDK_INT >= 31) p += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= 33) p += Manifest.permission.NEARBY_WIFI_DEVICES
        return p.toTypedArray()
    }

    /**
     * Nearby Connections needs Bluetooth switched on, and on Android 12 and below it also needs
     * the phone's Location toggle on (the OS ties Bluetooth scanning to it), even though the app
     * itself never reads your location. Checking first turns a silent 2-minute failure into a
     * clear message telling the person exactly what to switch on.
     */
    private fun familyPreflight(): String? {
        val bt = (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (bt == null || !bt.isEnabled) return Lang.t(this,
            "Turn on Bluetooth, then try Sync with family nearby again.",
            "ಬ್ಲೂಟೂತ್ ಆನ್ ಮಾಡಿ, ನಂತರ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.")
        if (Build.VERSION.SDK_INT <= 32) {
            val lm = getSystemService(LOCATION_SERVICE) as? LocationManager
            if (lm != null && !lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                return Lang.t(this,
                    "Turn on Location in your phone's settings (Android needs it for this kind of Bluetooth search, even though the app doesn't use your location), then try again.",
                    "ಫೋನ್ ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಲೊಕೇಶನ್ ಆನ್ ಮಾಡಿ (ಆ್ಯಪ್ ನಿಮ್ಮ ಸ್ಥಳ ಬಳಸದಿದ್ದರೂ, ಈ ಬಗೆಯ ಬ್ಲೂಟೂತ್ ಹುಡುಕಾಟಕ್ಕೆ Android ಇದನ್ನು ಕೇಳುತ್ತದೆ), ನಂತರ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.")
        }
        return null
    }

    private fun isNight() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** "system" (default), "light" or "dark" — the person's choice in Settings > Theme. */
    private fun themePref() = prefs.getString("theme", "system") ?: "system"

    /** Whether the app should currently render dark, honouring an explicit choice over the phone's setting. */
    private fun resolvedNight() = when (themePref()) {
        "dark" -> true
        "light" -> false
        else -> isNight()
    }

    /** Colours the status bar, navigation bar and WebView background to match [resolvedNight]. */
    private fun applyChrome() {
        val night = resolvedNight()
        window.statusBarColor = Color.parseColor(if (night) "#17140F" else "#F4EFE4")
        window.navigationBarColor = Color.parseColor(if (night) "#201C16" else "#FBF8F1")
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
        if (::web.isInitialized) web.setBackgroundColor(Color.parseColor(if (night) "#17140F" else "#F4EFE4"))
    }

    private val saveCsv =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { writeSave(it) }
    private val saveJson =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { writeSave(it) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // v1.45 resets the SMS flow once so an upgrade cannot scan or display old SMS-derived
        // data before the user sees the new explicit consent/import flow.
        if (prefs.getInt("smsFlowVersion", 0) < 5) {
            prefs.edit().putInt("smsFlowVersion", 5)
                .putBoolean("smsConsentConfirmed", false)
                .putBoolean("onboardingComplete", false)
                .putBoolean("initialImportDone", false)
                .putBoolean("smsDataReady", false)
                .remove(SmsReceiver.PENDING_KEY)
                .apply()
        }
        val assets = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
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
                    view.evaluateJavascript("document.documentElement.dataset.theme='${if (resolvedNight()) "dark" else "light"}'", null)
                    onPageReady()
                }
            }
        }
        applyChrome()
        rootFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(if (resolvedNight()) "#17140F" else "#F4EFE4"))
            addView(web, FrameLayout.LayoutParams(-1, -1))
        }
        setContentView(rootFrame)
        appUnlocked = !prefs.getBoolean("appLockEnabled", true)
        if (!appUnlocked) showAppLockOverlay()
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        ContextCompat.registerReceiver(this, smsQueueReceiver, IntentFilter(ACTION_SMS_QUEUED), ContextCompat.RECEIVER_NOT_EXPORTED)

        onBackPressedDispatcher.addCallback(this) {
            web.evaluateJavascript("window.appBack ? window.appBack() : false") { handled ->
                if (handled != "true") finish()
            }
        }
    }

    private fun canUseDeviceAuth(): Boolean {
        val mgr = BiometricManager.from(this)
        val result = mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showAppLockOverlay() {
        if (!::rootFrame.isInitialized || !prefs.getBoolean("appLockEnabled", true)) return
        if (lockOverlay == null) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(32, 48, 32, 48)
                setBackgroundColor(Color.parseColor(if (resolvedNight()) "#17140F" else "#F4EFE4"))
            }
            val title = TextView(this).apply {
                text = "Unlock SMS Ledger"
                textSize = 25f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(if (resolvedNight()) "#F7F1E6" else "#241F19"))
                gravity = Gravity.CENTER
            }
            val subtitle = TextView(this).apply {
                text = "Use your fingerprint, face or device PIN to continue."
                textSize = 15f
                setTextColor(Color.parseColor(if (resolvedNight()) "#B8B0A2" else "#665F55"))
                gravity = Gravity.CENTER
                setPadding(0, 14, 0, 24)
            }
            val button = Button(this).apply {
                text = "Unlock"
                setOnClickListener { authenticateApp() }
            }
            box.addView(title, LinearLayout.LayoutParams(-1, -2))
            box.addView(subtitle, LinearLayout.LayoutParams(-1, -2))
            box.addView(button, LinearLayout.LayoutParams(-1, 56))
            lockOverlay = box
            rootFrame.addView(box, FrameLayout.LayoutParams(-1, -1))
        }
        lockOverlay?.visibility = View.VISIBLE
        if (!lockPromptShowing) authenticateApp()
    }

    private fun authenticateApp() {
        if (lockPromptShowing || !prefs.getBoolean("appLockEnabled", true)) {
            if (!prefs.getBoolean("appLockEnabled", true)) { appUnlocked = true; lockOverlay?.visibility = View.GONE }
            return
        }
        if (!canUseDeviceAuth()) {
            prefs.edit().putBoolean("appLockEnabled", false).apply()
            appUnlocked = true
            lockOverlay?.visibility = View.GONE
            toastJs("Your phone does not have a secure screen lock configured, so app lock was turned off.")
            onPageReady()
            return
        }
        lockPromptShowing = true
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                lockPromptShowing = false
                appUnlocked = true
                lockOverlay?.visibility = View.GONE
                onPageReady()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                lockPromptShowing = false
                if (errorCode == BiometricPrompt.ERROR_CANCELED || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    toastJs("SMS Ledger is locked. Tap Unlock to try again.")
                } else toastJs(errString.toString())
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock SMS Ledger")
            .setSubtitle("Your financial data is protected on this device.")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }

    private fun applyAppLockEnabled(enabled: Boolean) {
        if (!enabled) { prefs.edit().putBoolean("appLockEnabled", false).apply(); appUnlocked = true; return }
        if (!canUseDeviceAuth()) { toastJs("Set a fingerprint, face or device PIN on your phone first."); return }
        prefs.edit().putBoolean("appLockEnabled", true).apply(); appUnlocked = false; showAppLockOverlay()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(smsQueueReceiver) }
        runCatching { family.stop() }
        runCatching { familyCloud.stop() }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        NotificationManagerCompat.from(this).cancel(SmsReceiver.NOTIFY_ID)
        if (prefs.getBoolean("appLockEnabled", true) && !appUnlocked) {
            showAppLockOverlay()
            return
        }
        if (pageReady) {
            pushStatus()
            if (prefs.getBoolean("smsDataReady", false)) drainPendingSms()
            // New SMS are received immediately by SmsReceiver. Avoid rescanning the entire inbox
            // on every resume; this was a major source of the few-second startup delay.
            val smsConsentConfirmed = prefs.getBoolean("smsConsentConfirmed", false)
            val lastScan = prefs.getLong("lastScan", 0L)
            val recentlyScanned = System.currentTimeMillis() - lastScan < 5L * 60L * 1000L
            if (smsConsentConfirmed && hasSms() && prefs.getBoolean("smsDataReady", false) && !recentlyScanned) {
                if (!prefs.getBoolean("fullSmsImportV47Done", false)) scanInbox(0L, announce = true)
                else if (prefs.getBoolean("initialImportDone", false)) web.postDelayed({ scanInbox() }, 1200L)
            }
            web.evaluateJavascript("window.appOnline && window.appOnline()", null)
        }
    }

    private var waitingForLanguage = false

    private fun onPageReady() {
        if (prefs.getBoolean("appLockEnabled", true) && !appUnlocked) return
        pushStatus()
        web.evaluateJavascript("window.appOnline && window.appOnline()", null)
        if (prefs.getBoolean("smsDataReady", false)) drainPendingSms()
        // The WebView owns first-run sequencing: language -> Google account/skip -> SIM names -> SMS permission.
        web.evaluateJavascript("window.needsLanguage ? window.needsLanguage() : false") { needs ->
            if (needs != "true") web.evaluateJavascript("window.runOnboardingChecks && window.runOnboardingChecks()", null)
        }
    }

    private fun startupSms() {
        if (prefs.getBoolean("smsConsentConfirmed", false) && hasSms()) firstImportOrScan()
        else if (!hasSms()) askPermissions()
    }

    /** First import reads the complete SMS history, with bank-transaction filtering in SmsReader. */
    private fun firstImportOrScan() {
        scanInbox(0L, announce = true)
    }

    private fun hasSms() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    private fun askPermissions() {
        val requested = mutableListOf<String>()
        listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE).forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) requested += it
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requested += Manifest.permission.POST_NOTIFICATIONS
        if (requested.isNotEmpty()) permLauncher.launch(requested.toTypedArray())
        else web.evaluateJavascript("window.smsPermissionGranted && window.smsPermissionGranted()", null)
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
            saveMark(); prefs.edit().putBoolean("initialImportDone", true).putBoolean("fullSmsImportV47Done", true).apply(); scanning = false
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
                    if (accepted) { saveMark(); prefs.edit().putBoolean("initialImportDone", true).putBoolean("fullSmsImportV47Done", true).apply() }   // move the mark only once the page has taken them
                    scanning = false
                    if (announce || added > 0)
                        web.evaluateJavascript("window.importFinished && window.importFinished($added, $total)", null)
                    if (accepted) { prefs.edit().putBoolean("smsDataReady", true).apply(); web.postDelayed({ drainPendingSms() }, 250) }
                }
            }
            start = end
        }
    }

    /** Imports SMS received while the app/WebView was not open. */
    private fun drainPendingSms() {
        if (!pageReady || !hasSms() || !prefs.getBoolean("smsConsentConfirmed", false) || !prefs.getBoolean("smsDataReady", false)) return
        val raw = prefs.getString(SmsReceiver.PENDING_KEY, "[]") ?: "[]"
        val pending = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        if (pending.length() == 0) return
        web.evaluateJavascript("window.ingestSms ? window.ingestSms($pending, false) : -1") { result ->
            val n = result?.toIntOrNull()
            if (n != null && n >= 0) {
                prefs.edit().remove(SmsReceiver.PENDING_KEY).apply()
                if (n > 0) web.evaluateJavascript("window.importFinished && window.importFinished($n, ${pending.length()})", null)
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
        fun getSigningFingerprints(): String {
            fun hex(bytes: ByteArray): String = bytes.joinToString(":") { "%02X".format(it) }
            return try {
                val pm = packageManager
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                }
                val signatures: Array<android.content.pm.Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures
                }
                val sig = signatures?.firstOrNull()
                    ?: return JSONObject().put("ok", false).put("error", "No signing certificate found").toString()
                val sha1 = hex(MessageDigest.getInstance("SHA-1").digest(sig.toByteArray()))
                val sha256 = hex(MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()))
                val versionName = packageInfo.versionName ?: "unknown"
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                JSONObject().put("ok", true).put("package", packageName).put("sha1", sha1).put("sha256", sha256)
                    .put("version", versionName).put("versionCode", versionCode).toString()
            } catch (e: Exception) {
                JSONObject().put("ok", false).put("error", e.message ?: e.javaClass.simpleName).toString()
            }
        }

        @JavascriptInterface
        fun hasSmsAccess(): Boolean = hasSms()

        @JavascriptInterface
        fun isSmsDataUnlocked(): Boolean = prefs.getBoolean("smsConsentConfirmed", false) && prefs.getBoolean("smsDataReady", false)

        @JavascriptInterface
        fun hasPhoneStateAccess(): Boolean = ContextCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        @JavascriptInterface
        fun requestSms() = runOnUiThread {
            // Do not silently read or scan here. Android must show the runtime permission prompt
            // whenever the permission has not been granted.
            if (!hasSms() || !hasPhoneStateAccess()) {
                askPermissions()
            } else {
                web.evaluateJavascript("window.smsPermissionGranted && window.smsPermissionGranted()", null)
            }
        }

        /**
         * Import (or re-import) bank SMS from the last [days]; 0 means every SMS on the phone.
         * Entries already in the ledger are skipped by the page, so rescanning is safe.
         */
        @JavascriptInterface
        fun importRange(days: Int) = runOnUiThread {
            val since = if (days <= 0) 0L else System.currentTimeMillis() - days.coerceAtMost(MAX_DAYS) * DAY
            prefs.edit().putBoolean("initialImportStarted", true).putBoolean("initialImportDone", false).apply()
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
            val problem = familyPreflight()
            if (problem != null) { toastJs(problem); return@runOnUiThread }
            val need = familyPermissions().filter {
                ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
            }
            if (need.isEmpty()) family.start(name, json)
            else { pendingFamily = name to json; familyPerms.launch(need.toTypedArray()) }
        }

        /** Reset the native SMS onboarding gate when a new onboarding flow is introduced. */
        @JavascriptInterface
        fun resetSmsOnboarding() {
            prefs.edit().putBoolean("smsConsentConfirmed", false)
                .putBoolean("onboardingComplete", false)
                .putBoolean("initialImportDone", false)
                .putBoolean("smsDataReady", false)
                .remove(SmsReceiver.PENDING_KEY)
                .apply()
        }

        /** The language the person picked, kept for notifications and dialogs. */
        @JavascriptInterface
        fun setLanguage(lang: String) { prefs.edit().putString("lang", if (lang == "kn") "kn" else "en").apply() }

        @JavascriptInterface
        fun setOnboardingComplete() { prefs.edit().putBoolean("onboardingComplete", true).apply() }

        @JavascriptInterface
        fun setSmsConsentConfirmed() {
            prefs.edit().putBoolean("smsConsentConfirmed", true).putBoolean("onboardingComplete", true).putBoolean("initialImportDone", false).putBoolean("smsDataReady", false).apply()
        }

        @JavascriptInterface
        fun setInitialImportDone() { prefs.edit().putBoolean("initialImportDone", true).putBoolean("smsDataReady", true).apply(); runOnUiThread { drainPendingSms() } }

        @JavascriptInterface
        fun setBackupSchedule(frequency: String) { BackupScheduler.set(this@MainActivity, frequency) }

        @JavascriptInterface
        fun getBackupSchedule(): String = BackupScheduler.get(this@MainActivity)

        @JavascriptInterface
        fun backupNow(): Boolean = BackupScheduler.backupNow(this@MainActivity)

        @JavascriptInterface
        fun isAppLockEnabled(): Boolean = prefs.getBoolean("appLockEnabled", true)

        @JavascriptInterface
        fun setAppLockEnabled(enabled: Boolean) = runOnUiThread { applyAppLockEnabled(enabled) }

        /** The theme the person picked in Settings: "system", "light" or "dark". Applied immediately. */
        @JavascriptInterface
        fun setTheme(theme: String) = runOnUiThread {
            prefs.edit().putString("theme", if (theme in listOf("light", "dark")) theme else "system").apply()
            applyChrome()
        }

        /** The signed-in Google account, or null if not signed in (or if anything's not ready yet). */
        @JavascriptInterface
        fun getCurrentUser(): String = try { cloudAuth.userJson()?.toString() ?: "null" } catch (e: Throwable) { "null" }

        /**
         * Starts Google Sign-In. Answers asynchronously through window.cloudSignInResult(json),
         * where json is {"ok":true,"uid":...,"name":...,"email":...,"photo":...} on success, or
         * {"ok":false,"error":"...", "cancelled":true|false} otherwise.
         */
        @JavascriptInterface
        fun googleConfigDiagnostic(): String = try {
            val fp = JSONObject(getSigningFingerprints())
            JSONObject()
                .put("ok", true)
                .put("package", packageName)
                .put("webClientId", getString(R.string.default_web_client_id))
                .put("sha1", fp.optString("sha1"))
                .put("sha256", fp.optString("sha256"))
                .put("version", fp.optString("version"))
                .put("versionCode", fp.optLong("versionCode"))
                .toString()
        } catch (e: Exception) {
            JSONObject().put("ok", false).put("error", e.message ?: e.javaClass.simpleName).toString()
        }

        @JavascriptInterface
        fun googleSignInDiagnostic() = runOnUiThread {
            try {
                googleDiagnosticLauncher.launch(cloudAuth.legacySignInIntent())
            } catch (e: Throwable) {
                val json = JSONObject().put("ok", false).put("stage", "launch")
                    .put("error", e.message ?: e.javaClass.simpleName).toString()
                web.evaluateJavascript("window.googleDiagnosticResult && window.googleDiagnosticResult(${JSONObject.quote(json)})", null)
            }
        }

        @JavascriptInterface
        fun pickBankStatement(callbackName: String) = runOnUiThread {
            statementCallback = ValueCallback { payload ->
                val safe = JSONObject.quote(payload ?: "[]")
                web.evaluateJavascript("window.bankStatementFilesResult && window.bankStatementFilesResult($safe)", null)
            }
            pickStatement.launch(arrayOf("application/pdf", "text/csv", "text/plain", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream"))
        }

        @JavascriptInterface
        fun pickInsurancePolicy(callbackName: String) = runOnUiThread {
            insurancePolicyCallback = ValueCallback { payload ->
                val safe = JSONObject.quote(payload ?: "[]")
                web.evaluateJavascript("window.insurancePolicyFilesResult && window.insurancePolicyFilesResult($safe)", null)
            }
            pickInsurancePolicyFiles.launch(arrayOf("application/pdf", "text/plain", "text/csv", "application/octet-stream"))
        }

        @JavascriptInterface
        fun pickLoanDocuments(callbackName: String) = runOnUiThread {
            loanDocumentCallback = ValueCallback { payload ->
                val safe = JSONObject.quote(payload ?: "[]")
                web.evaluateJavascript("window.loanDocumentFilesResult && window.loanDocumentFilesResult($safe)", null)
            }
            pickLoanDocumentFiles.launch(arrayOf("application/pdf", "text/csv", "text/plain", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream"))
        }

        @JavascriptInterface
        fun signIn() {
            lifecycleScope.launch {
                try {
                    val result = cloudAuth.signIn()
                    if (result.isSuccess) {
                        val json = result.getOrThrow().put("ok", true).toString()
                        web.evaluateJavascript("window.cloudSignInResult && window.cloudSignInResult(${JSONObject.quote(json)})", null)
                        return@launch
                    }
                    // Credential Manager gets 15 seconds to complete. If the device provider
                    // hangs, use the Google Play services account picker instead of leaving the
                    // user waiting indefinitely.
                    try {
                        legacyGoogleLauncher.launch(cloudAuth.legacySignInIntent())
                    } catch (fallback: Throwable) {
                        val json = JSONObject().put("ok", false).put("cancelled", false)
                            .put("error", result.exceptionOrNull()?.message ?: fallback.message ?: "Google sign-in could not start").toString()
                        web.evaluateJavascript("window.cloudSignInResult && window.cloudSignInResult(${JSONObject.quote(json)})", null)
                    }
                } catch (e: Throwable) {
                    try {
                        legacyGoogleLauncher.launch(cloudAuth.legacySignInIntent())
                    } catch (fallback: Throwable) {
                        val json = JSONObject().put("ok", false).put("cancelled", false)
                            .put("error", fallback.message ?: e.message ?: "Google sign-in could not start").toString()
                        web.evaluateJavascript("window.cloudSignInResult && window.cloudSignInResult(${JSONObject.quote(json)})", null)
                    }
                }
            }
        }

        /** Signs out and notifies the page via window.cloudSignInResult({"ok":false,"signedOut":true}). */
        @JavascriptInterface
        fun createFamily(name: String) {
            lifecycleScope.launch {
                val json = try { familyCloud.createFamily(name).toString() } catch (e: Throwable) { JSONObject().put("ok", false).put("error", e.message ?: "Could not create family").toString() }
                web.evaluateJavascript("window.familyCloudResult && window.familyCloudResult(${JSONObject.quote(json)})", null)
            }
        }

        @JavascriptInterface
        fun joinFamily(code: String, name: String) {
            lifecycleScope.launch {
                val json = try { familyCloud.joinFamily(code, name).toString() } catch (e: Throwable) { JSONObject().put("ok", false).put("error", e.message ?: "Could not join family").toString() }
                web.evaluateJavascript("window.familyCloudResult && window.familyCloudResult(${JSONObject.quote(json)})", null)
            }
        }

        @JavascriptInterface
        fun publishFamily(familyId: String, payload: String) {
            lifecycleScope.launch {
                val json = try { familyCloud.publish(familyId, JSONObject(payload)); JSONObject().put("ok", true).toString() } catch (e: Throwable) { JSONObject().put("ok", false).put("error", e.message ?: "Could not sync family data").toString() }
                web.evaluateJavascript("window.familyCloudPublishResult && window.familyCloudPublishResult(${JSONObject.quote(json)})", null)
            }
        }

        @JavascriptInterface
        fun listenFamily(familyId: String) {
            if (familyId.isBlank()) return
            familyCloud.listen(familyId)
        }

        @JavascriptInterface
        fun getFamilyInfo(familyId: String) {
            lifecycleScope.launch {
                val json = try { familyCloud.familyInfo(familyId).toString() } catch (e: Throwable) { JSONObject().put("ok", false).put("error", e.message ?: "Could not load family").toString() }
                web.evaluateJavascript("window.familyInfoResult && window.familyInfoResult(${JSONObject.quote(json)})", null)
            }
        }

        @JavascriptInterface
        fun signOutCloud() {
            lifecycleScope.launch {
                val json: String = try {
                    cloudAuth.signOut()
                    org.json.JSONObject().put("ok", false).put("signedOut", true).toString()
                } catch (e: Throwable) {
                    org.json.JSONObject().put("ok", false).put("error", e.message ?: "Couldn't sign out").toString()
                }
                web.evaluateJavascript("window.cloudSignInResult && window.cloudSignInResult(${JSONObject.quote(json)})", null)
            }
        }

        /**
         * The distinct SIMs bank SMS have actually arrived on, each with a best-effort label.
         * Reads only the subscription id already visible via READ_SMS — no extra permission
         * is requested just to name a SIM; if the carrier name isn't available it falls back
         * to "SIM 1"/"SIM 2" and the person can rename it themselves in Settings.
         */
        @JavascriptInterface
        fun getSimList(): String {
            val arr = JSONArray()
            try {
                val sm = getSystemService(SubscriptionManager::class.java)
                val active = sm?.activeSubscriptionInfoList.orEmpty()
                    .filter { it.simSlotIndex >= 0 }
                    .sortedBy { it.simSlotIndex }
                    .distinctBy { it.simSlotIndex }
                active.forEachIndexed { index, info ->
                    val slot = info.simSlotIndex
                    val label = info.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${index + 1}"
                    arr.put(JSONObject().put("id", info.subscriptionId).put("slot", slot).put("label", label))
                }
            } catch (_: SecurityException) {
                // Do not infer SIM count from historical SMS. If Android withholds active
                // subscription details, report none rather than inventing stale SIMs.
            } catch (_: Exception) { }
            return arr.toString()
        }

        /** First-launch language picked: carry on with SMS permission. */
        @JavascriptInterface
        fun languageChosen() = runOnUiThread {
            if (waitingForLanguage) { waitingForLanguage = false; web.evaluateJavascript("window.runOnboardingChecks && window.runOnboardingChecks()", null) }
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
        const val ACTION_SMS_QUEUED = "in.vageesh.smsledger.ACTION_SMS_QUEUED"
    }

}
