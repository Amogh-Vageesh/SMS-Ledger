package in.vageesh.smsledger

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread
import in.vageesh.smsledger.R

class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var pageReady = false
    private var pendingSave: String? = null
    private val prefs by lazy { getSharedPreferences("ledger", MODE_PRIVATE) }

    // Firebase instances
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            pushStatus()
            if (res[Manifest.permission.READ_SMS] == true) firstImportOrScan()
        }

    // Picker for <input type="file"> in WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        fileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        fileCallback = null
    }

    // Google Sign-In Result Launcher
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!, account)
                } catch (e: Exception) {
                    toastJs("Google Sign-In failed: ${e.message}")
                    web.evaluateJavascript("window.onGoogleSignInFailed && window.onGoogleSignInFailed('${e.message}')", null)
                }
            }
        }

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

    private fun themePref() = prefs.getString("theme", "system") ?: "system"

    private fun resolvedNight() = when (themePref()) {
        "dark" -> true
        "light" -> false
        else -> isNight()
    }

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
        val assets = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
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

    private var waitingForLanguage = false

    private fun onPageReady() {
        pushStatus()
        web.evaluateJavascript("window.appOnline && window.appOnline()", null)
        web.evaluateJavascript("window.needsLanguage ? window.needsLanguage() : false") { needs ->
            if (needs == "true") waitingForLanguage = true else startupSms()
        }
        checkFirebaseCurrentUser()
    }

    private fun startupSms() {
        when {
            hasSms() -> firstImportOrScan()
            !prefs.getBoolean("asked", false) -> askPermissions()
        }
    }

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
            web.evaluateJavascript("${progress}window.ingestSms ? window.ingestSms($batch, true) : -1") { r ->
                val n = r?.toIntOrNull() ?: -1
                if (n < 0) accepted = false else added += n
                if (isLast) {
                    if (accepted) saveMark()
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

    // Google & Firebase Authentication Helpers
    private fun checkFirebaseCurrentUser() {
        val user = auth.currentUser
        if (user != null) {
            val jsonUser = JSONObject().apply {
                put("uid", user.uid)
                put("displayName", user.displayName ?: "")
                put("email", user.email ?: "")
                put("photoUrl", user.photoUrl?.toString() ?: "")
            }
            web.evaluateJavascript("window.onGoogleSignInSuccess && window.onGoogleSignInSuccess($jsonUser)", null)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val jsonUser = JSONObject().apply {
                    put("uid", user?.uid)
                    put("displayName", account.displayName ?: "")
                    put("email", account.email ?: "")
                    put("photoUrl", account.photoUrl?.toString() ?: "")
                }
                web.evaluateJavascript("window.onGoogleSignInSuccess && window.onGoogleSignInSuccess($jsonUser)", null)
            } else {
                val err = task.exception?.message ?: "Firebase authentication failed"
                toastJs(err)
                web.evaluateJavascript("window.onGoogleSignInFailed && window.onGoogleSignInFailed('$err')", null)
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun triggerGoogleSignIn() = runOnUiThread {
            val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
            val defaultWebClientId = if (resId != 0) getString(resId) else ""

            if (defaultWebClientId.isBlank()) {
                toastJs("Missing default_web_client_id from google-services.json")
                return@runOnUiThread
            }
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(defaultWebClientId)
                .requestEmail()
                .build()
            val signInClient = GoogleSignIn.getClient(this@MainActivity, gso)
            googleSignInLauncher.launch(signInClient.signInIntent)
        }

        @JavascriptInterface
        fun signOutGoogle() = runOnUiThread {
            auth.signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(this@MainActivity, gso).signOut().addOnCompleteListener {
                web.evaluateJavascript("window.onGoogleSignOutSuccess && window.onGoogleSignOutSuccess()", null)
            }
        }

        @JavascriptInterface
        fun syncSharedEntryToCloud(groupId: String, entryJson: String) {
            val user = auth.currentUser
            if (user == null) {
                toastJs("Please sign in with Google first.")
                return
            }
            try {
                val entryObj = JSONObject(entryJson)
                val entryMap = hashMapOf(
                    "title" to entryObj.optString("title"),
                    "amount" to entryObj.optDouble("amount"),
                    "category" to entryObj.optString("category"),
                    "paidBy" to entryObj.optString("paidBy"),
                    "recipient" to entryObj.optString("recipient"),
                    "eventId" to entryObj.optString("eventId"),
                    "date" to entryObj.optString("date"),
                    "createdBy" to user.email
                )

                db.collection("familyGroups").document(groupId)
                    .collection("sharedEntries")
                    .add(entryMap)
                    .addOnSuccessListener { ref ->
                        runOnUiThread {
                            web.evaluateJavascript("window.onEntrySynced && window.onEntrySynced('${ref.id}')", null)
                        }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            toastJs("Sync failed: ${e.message}")
                        }
                    }
            } catch (e: Exception) {
                toastJs("Invalid entry JSON")
            }
        }

        @JavascriptInterface
        fun hasSmsAccess(): Boolean = hasSms()

        @JavascriptInterface
        fun requestSms() = runOnUiThread {
            val canAsk = !prefs.getBoolean("asked", false) ||
                shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)
            if (canAsk) askPermissions()
            else startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        }

        @JavascriptInterface
        fun importRange(days: Int) = runOnUiThread {
            val since = if (days <= 0) 0L else System.currentTimeMillis() - days.coerceAtMost(MAX_DAYS) * DAY
            if (hasSms()) scanInbox(since, announce = true) else requestSms()
        }

        @JavascriptInterface
        fun saveState(json: String) {
            synchronized(stateLock) {
                val tmp = File(filesDir, "ledger.json.tmp")
                tmp.writeText(json)
                tmp.renameTo(File(filesDir, "ledger.json"))
            }
        }

        @JavascriptInterface
        fun getRate(id: String, cur: String, date: String) {
            thread {
                val r = Rates.get(cur, date) ?: 0.0
                runOnUiThread { web.evaluateJavascript("window.rateResult && window.rateResult(${JSONObject.quote(id)}, $r)", null) }
            }
        }

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

        @JavascriptInterface
        fun setLanguage(lang: String) { prefs.edit().putString("lang", if (lang == "kn") "kn" else "en").apply() }

        @JavascriptInterface
        fun setTheme(theme: String) = runOnUiThread {
            prefs.edit().putString("theme", if (theme in listOf("light", "dark")) theme else "system").apply()
            applyChrome()
        }

        @JavascriptInterface
        fun getSimList(): String {
            val ids = linkedSetOf<Int>()
            try {
                contentResolver.query(
                    android.provider.Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(android.provider.Telephony.Sms.SUBSCRIPTION_ID), null, null, null
                )?.use { c ->
                    val i = c.getColumnIndex(android.provider.Telephony.Sms.SUBSCRIPTION_ID)
                    if (i >= 0) while (c.moveToNext()) { val v = c.getInt(i); if (v >= 0) ids.add(v) }
                }
            } catch (e: Exception) { }
            val arr = JSONArray()
            var n = 0
            ids.forEach { id ->
                n++
                val label = try {
                    (getSystemService(SubscriptionManager::class.java))
                        ?.getActiveSubscriptionInfo(id)?.displayName?.toString()?.takeIf { it.isNotBlank() }
                } catch (e: Exception) { null } ?: "SIM $n"
                arr.put(JSONObject().put("id", id).put("label", label))
            }
            return arr.toString()
        }

        @JavascriptInterface
        fun languageChosen() = runOnUiThread {
            if (waitingForLanguage) { waitingForLanguage = false; startupSms() }
        }

        @JavascriptInterface
        fun takeFamilyInbox(): String {
            val f = File(filesDir, "family-inbox.json")
            if (!f.exists()) return ""
            val t = f.readText(); f.delete(); return t
        }

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
        const val MAX_DAYS = 36500
        const val BATCH = 400
    }
}