package `in`.vageesh.smsledger

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Cloud family sharing. Private SMS ledger data stays local unless explicitly published. */
class FamilyCloud(private val context: Context, private val onData: (String) -> Unit, private val onError: (String) -> Unit) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listeners = mutableListOf<ListenerRegistration>()

    fun stop() { listeners.forEach { runCatching { it.remove() } }; listeners.clear() }

    suspend fun createFamily(name: String): JSONObject {
        val uid = requireUid()
        val familyId = UUID.randomUUID().toString()
        val code = randomCode()
        val now = System.currentTimeMillis()
        val family = mapOf("ownerUid" to uid, "createdAt" to now, "inviteCode" to code, "name" to (name.ifBlank { "My Family" }))
        db.collection("families").document(familyId).set(family).await()
        db.collection("familyInvites").document(code).set(mapOf("familyId" to familyId, "createdAt" to now)).await()
        db.collection("families").document(familyId).collection("members").document(uid).set(
            mapOf("uid" to uid, "name" to (name.ifBlank { auth.currentUser?.displayName ?: "Member" }), "email" to (auth.currentUser?.email ?: ""), "role" to "owner", "joinedAt" to now)
        ).await()
        return JSONObject().put("ok", true).put("familyId", familyId).put("inviteCode", code)
    }

    suspend fun joinFamily(codeRaw: String, name: String): JSONObject {
        val uid = requireUid(); val code = codeRaw.trim().uppercase()
        if (code.length < 6) throw IllegalArgumentException("Enter the family code.")
        val invite = db.collection("familyInvites").document(code).get().await()
        if (!invite.exists()) throw IllegalArgumentException("Family code not found.")
        val familyId = invite.getString("familyId") ?: throw IllegalArgumentException("Invalid family code.")
        val now = System.currentTimeMillis()
        db.collection("families").document(familyId).collection("members").document(uid).set(
            mapOf("uid" to uid, "name" to (name.ifBlank { auth.currentUser?.displayName ?: "Member" }), "email" to (auth.currentUser?.email ?: ""), "role" to "member", "joinedAt" to now), SetOptions.merge()
        ).await()
        return JSONObject().put("ok", true).put("familyId", familyId).put("inviteCode", code)
    }

    suspend fun publish(familyId: String, payload: JSONObject) {
        requireUid()
        val root = db.collection("families").document(familyId)
        val uid = auth.currentUser!!.uid
        val now = System.currentTimeMillis()
        val member = JSONObject().apply { put("uid", uid); put("name", auth.currentUser?.displayName ?: "Member"); put("email", auth.currentUser?.email ?: ""); put("updatedAt", now) }
        root.collection("members").document(uid).set(mapOf("uid" to uid, "name" to member.getString("name"), "email" to member.getString("email"), "updatedAt" to now), SetOptions.merge()).await()
        replaceArray(root.collection("events"), payload.optJSONArray("events"), uid, now)
        replaceArray(root.collection("calendar"), payload.optJSONArray("calendar"), uid, now)
        replaceArray(root.collection("shopping"), payload.optJSONArray("shopping"), uid, now)
        replaceArray(root.collection("transactions"), payload.optJSONArray("transactions"), uid, now)
        replaceCompleteData(root.collection("completeData"), payload.optString("completeData", ""), payload.optString("completeShareWith", "all"), payload.optJSONArray("completeAudienceUids"), uid, now)
    }


    private suspend fun replaceCompleteData(
        col: com.google.firebase.firestore.CollectionReference,
        raw: String,
        shareWith: String,
        audience: JSONArray?,
        uid: String,
        now: Long
    ) {
        val existing = col.whereEqualTo("ownerUid", uid).get().await()
        existing.documents.forEach { it.reference.delete().await() }
        if (raw.isBlank()) return
        val chunkSize = 450_000
        val count = ((raw.length() + chunkSize - 1) / chunkSize).coerceAtLeast(1)
        val aud = mutableListOf<String>()
        if (audience != null) for (i in 0 until audience.length()) {
            audience.optString(i, "").takeIf { it.isNotBlank() }?.let { aud += it }
        }
        for (i in 0 until count) {
            val start = i * chunkSize
            val end = minOf(raw.length(), start + chunkSize)
            val data = mutableMapOf<String, Any>(
                "ownerUid" to uid,
                "updatedAt" to now,
                "shareWith" to shareWith,
                "audienceUids" to aud,
                "chunkIndex" to i,
                "chunkCount" to count,
                "dataChunk" to raw.substring(start, end)
            )
            col.document("${uid}_$i").set(data, SetOptions.merge()).await()
        }
    }

    private suspend fun replaceArray(col: com.google.firebase.firestore.CollectionReference, arr: JSONArray?, uid: String, now: Long) {
        val keep = mutableSetOf<String>()
        val count = arr?.length() ?: 0
        for (i in 0 until count) {
            val o = arr?.optJSONObject(i) ?: continue
            val id = o.optString("id").ifBlank { UUID.randomUUID().toString() }
            val map = mutableMapOf<String, Any>("ownerUid" to uid, "updatedAt" to now)
            o.keys().forEach { k ->
                val v = o.opt(k); if (v != null && v != JSONObject.NULL) map[k] = when (v) {
                    is Number, is Boolean, is String -> v
                    is JSONArray -> (0 until v.length()).mapNotNull { v.optString(it, null) }
                    else -> v.toString()
                }
            }
            keep += id
            col.document(id).set(map, SetOptions.merge()).await()
        }
        val existing = col.whereEqualTo("ownerUid", uid).get().await()
        for (doc in existing.documents) if (doc.id !in keep) doc.reference.delete().await()
    }

    fun listen(familyId: String) {
        stop()
        val root = db.collection("families").document(familyId)
        val buckets = mutableMapOf<String, MutableMap<String, JSONObject>>()
        fun watch(kind: String) {
            val collection = root.collection(kind)
            val queries = if (kind == "members") listOf(collection) else listOf(
                collection.whereEqualTo("shareWith", "all"),
                collection.whereArrayContains("audienceUids", auth.currentUser?.uid ?: "")
            )
            queries.forEach { query ->
                val reg = query.addSnapshotListener { snap, e ->
                    if (e != null) { onError(e.message ?: "Family sync error"); return@addSnapshotListener }
                    val b = buckets.getOrPut(kind) { mutableMapOf() }
                    snap?.documentChanges?.forEach { ch ->
                        val id = ch.document.id
                        if (ch.type.name == "REMOVED") b.remove(id) else b[id] = JSONObject(ch.document.data)
                    }
                    val out = JSONObject()
                    buckets.forEach { (k,v) -> val a=JSONArray(); v.values.forEach { a.put(it) }; out.put(k,a) }
                    onData(out.toString())
                }
                listeners += reg
            }
        }
        listOf("events","calendar","shopping","transactions","completeData","members").forEach(::watch)
    }

    suspend fun familyInfo(familyId: String): JSONObject {
        val root = db.collection("families").document(familyId).get().await()
        if (!root.exists()) throw IllegalArgumentException("Family no longer exists.")
        val members = db.collection("families").document(familyId).collection("members").get().await()
        val a = JSONArray()
        members.documents.forEach { doc ->
            val obj = JSONObject()
            doc.data?.forEach { (key, value) -> obj.put(key, value) }
            a.put(obj)
        }
        return JSONObject().put("ok",true).put("familyId",familyId).put("inviteCode",root.getString("inviteCode") ?: "").put("name",root.getString("name") ?: "My Family").put("members",a)
    }

    private fun requireUid(): String = auth.currentUser?.uid ?: throw IllegalStateException("Sign in with Google first.")
    private fun randomCode(): String = UUID.randomUUID().toString().replace("-","").take(8).uppercase()
}
