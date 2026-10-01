package `in`.vageesh.smsledger

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Google Sign-In backed by Firebase Auth, using the modern Credential Manager API.
 * This ties into the SAME Google account each family member already has — nothing new to
 * remember, no password this app ever sees. Only used to turn on sharing; the app's own
 * expense-reading and local data are unaffected either way.
 */
class CloudAuth(private val activity: ComponentActivity) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager by lazy { CredentialManager.create(activity) }

    // The "web" OAuth client from google-services.json — Credential Manager needs this specific
    // one (not the Android client) to hand back a verifiable ID token.
    private val webClientId: String by lazy { activity.getString(R.string.default_web_client_id) }

    fun userJson(): JSONObject? {
        val u = auth.currentUser ?: return null
        return JSONObject()
            .put("uid", u.uid)
            .put("name", u.displayName ?: "")
            .put("email", u.email ?: "")
            .put("photo", (u.photoUrl?.toString() ?: ""))
    }

    /** Explicit Google button flow. This uses the provider's dedicated Google sign-in option,
     * which opens the account chooser instead of relying on an already-authorized credential. */
    suspend fun signIn(): Result<JSONObject> {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(google.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val u = authResult.user ?: return Result.failure(Exception("Signed in but no user returned"))
                Result.success(JSONObject().put("uid", u.uid).put("name", u.displayName ?: "")
                    .put("email", u.email ?: "").put("photo", u.photoUrl?.toString() ?: ""))
            } else Result.failure(Exception("Google returned an unsupported credential type"))
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun trySignIn(filterByAuthorizedAccounts: Boolean): Result<JSONObject> {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(google.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val u = authResult.user ?: return Result.failure(Exception("Signed in but no user returned"))
                Result.success(
                    JSONObject().put("uid", u.uid).put("name", u.displayName ?: "")
                        .put("email", u.email ?: "").put("photo", (u.photoUrl?.toString() ?: ""))
                )
            } else {
                Result.failure(Exception("Unexpected credential type from Google"))
            }
        } catch (e: NoCredentialException) {
            Result.failure(e) // no saved Google account for the quiet attempt; caller retries with the full picker
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e) // person backed out of the picker
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fallback for devices where Credential Manager cannot complete the Google account picker. */
    fun legacySignInIntent(): android.content.Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, options).signInIntent
    }

    suspend fun handleLegacyResult(data: android.content.Intent?): Result<JSONObject> {
        if (data == null) return Result.failure(Exception("Google sign-in was cancelled."))
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val token = account.idToken ?: return Result.failure(Exception("Google did not return an ID token. Check the Firebase Web OAuth client configuration."))
            val credential = GoogleAuthProvider.getCredential(token, null)
            val authResult = auth.signInWithCredential(credential).await()
            val u = authResult.user ?: return Result.failure(Exception("Signed in but Firebase returned no user."))
            Result.success(JSONObject().put("uid", u.uid).put("name", u.displayName ?: "").put("email", u.email ?: "").put("photo", u.photoUrl?.toString() ?: ""))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Suspend, so callers run it from a coroutine rather than blocking the calling thread. */
    suspend fun signOut() {
        auth.signOut()
        // Also clears Credential Manager's own remembered state, so the next sign-in
        // shows the account picker again instead of silently reusing the old session.
        try { credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest()) }
        catch (e: Exception) { }
    }
}
