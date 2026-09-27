package `in`.vageesh.smsledger

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.io.File

/**
 * Phone-to-phone family sync using Google Nearby Connections (Bluetooth / direct Wi-Fi, no internet).
 * Both phones advertise and discover at once; the one with the "smaller" name asks to connect,
 * both people confirm the same 4-digit code, then each sends its ledger file and we disconnect.
 */
class FamilySync(
    private val act: MainActivity,
    private val status: (String) -> Unit,
    private val received: (File) -> Unit
) {
    private val client by lazy { Nearby.getConnectionsClient(act) }
    private val service = "in.vageesh.smsledger.family"
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val main = Handler(Looper.getMainLooper())
    private var myName = ""
    private var outFile: File? = null
    private var running = false
    private val incoming = mutableMapOf<Long, Payload>()
    private var outgoingId: Long? = null
    private var sent = false
    private var got = false
    private var peerName = ""

    fun start(name: String, json: String) {
        stop()
        running = true; sent = false; got = false; incoming.clear(); outgoingId = null
        myName = name.take(24) + "#" + (1000..9999).random()
        outFile = File(act.cacheDir, "family-out.json").apply { writeText(json) }
        client.startAdvertising(myName, service, lifecycle, AdvertisingOptions.Builder().setStrategy(strategy).build())
            .addOnFailureListener { status("Couldn't start Bluetooth sharing. Check Bluetooth is on."); stop() }
        client.startDiscovery(service, discovery, DiscoveryOptions.Builder().setStrategy(strategy).build())
            .addOnFailureListener { status("Couldn't look for nearby phones. Check Bluetooth and Location are on."); stop() }
        status("Looking for your family's phone… keep both phones on this screen.")
        main.postDelayed({ if (running && !sent && !got) { status("No phone found nearby. Try again with both phones open on Settings."); stop() } }, 120_000)
    }

    fun stop() {
        running = false
        runCatching { client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints() }
        main.removeCallbacksAndMessages(null)
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
            if (info.serviceId != service) return
            // Only one side asks, so the two phones don't both request at once.
            if (myName < info.endpointName) {
                client.requestConnection(myName, id, lifecycle)
                    .addOnFailureListener { status("Couldn't connect. Try again.") }
            }
        }
        override fun onEndpointLost(id: String) {}
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            peerName = info.endpointName.substringBefore('#')
            act.runOnUiThread {
                val code = info.authenticationDigits
                AlertDialog.Builder(act)
                    .setTitle(Lang.t(act, "Sync with $peerName?", "$peerName ಅವರೊಂದಿಗೆ ಸಿಂಕ್ ಮಾಡಬೇಕೇ?"))
                    .setMessage(Lang.t(act,
                        "Check that both phones show the code $code. Your entries will be shared with $peerName, and theirs with you.",
                        "ಎರಡೂ ಫೋನ್‌ಗಳಲ್ಲಿ $code ಸಂಖ್ಯೆ ಕಾಣಿಸುತ್ತಿದೆಯೇ ನೋಡಿ. ನಿಮ್ಮ ನಮೂದುಗಳು $peerName ಅವರಿಗೆ, ಅವರದು ನಿಮಗೆ ಹಂಚಿಕೆಯಾಗುತ್ತವೆ."))
                    .setPositiveButton(Lang.t(act, "Codes match, sync", "ಸಂಖ್ಯೆ ಹೊಂದುತ್ತದೆ, ಸಿಂಕ್ ಮಾಡಿ")) { _, _ -> client.acceptConnection(id, payloads) }
                    .setNegativeButton(Lang.t(act, "Cancel", "ರದ್ದುಮಾಡಿ")) { _, _ -> client.rejectConnection(id); status("Sync cancelled.") }
                    .setCancelable(false)
                    .show()
            }
        }

        override fun onConnectionResult(id: String, res: ConnectionResolution) {
            if (res.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                runCatching { client.stopAdvertising(); client.stopDiscovery() }
                status("Connected to $peerName. Swapping ledgers…")
                val f = outFile ?: return
                val p = Payload.fromFile(f)
                outgoingId = p.id
                client.sendPayload(id, p)
            } else if (res.status.statusCode != ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                status("Couldn't connect to $peerName. Try again.")
            }
        }

        override fun onDisconnected(id: String) {
            if (!(sent && got)) status("The connection dropped before the sync finished. Try again.")
            running = false
        }
    }

    @Suppress("DEPRECATION")
    private fun copyPayload(p: Payload, dest: File) {
        val file = p.asFile() ?: error("no file")
        val uri = file.asUri()
        if (uri != null) {
            act.contentResolver.openInputStream(uri)!!.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
            runCatching { act.contentResolver.delete(uri, null, null) }
        } else {
            file.asJavaFile()!!.copyTo(dest, overwrite = true)
        }
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(id: String, payload: Payload) {
            if (payload.type == Payload.Type.FILE) incoming[payload.id] = payload
        }

        override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    if (update.payloadId == outgoingId) sent = true
                    incoming.remove(update.payloadId)?.let { p ->
                        val dest = File(act.filesDir, "family-inbox.json")
                        val ok = runCatching { copyPayload(p, dest) }.isSuccess
                        if (ok) { got = true; received(dest) } else status("The other phone's data couldn't be saved.")
                    }
                    if (sent && got) {
                        status("Synced with $peerName.")
                        main.postDelayed({ stop() }, 1500)
                    }
                }
                PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED ->
                    status("The transfer didn't finish. Try again.")
                else -> {}
            }
        }
    }
}
