package com.example.docmanager.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    // TODO: Replace with your actual Web Client ID from Google Cloud Console
    private val WEB_CLIENT_ID = com.example.docmanager.BuildConfig.GOOGLE_WEB_CLIENT_ID

    suspend fun signIn(): AuthResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            
            val signInResult = handleSignIn(result)
            if (signInResult is AuthResult.Success) {
                try {
                    val account = android.accounts.Account(signInResult.email, "com.google")
                    val scope = "oauth2:${com.google.api.services.drive.DriveScopes.DRIVE_FILE}"
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account, scope)
                    }
                } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                    return AuthResult.Error("Drive access authorization required", e.intent)
                } catch (e: Exception) {
                    return AuthResult.Error("Drive access check failed: ${e.message}")
                }
            }
            signInResult
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-in failed", e)
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun signOut() {
        try {
            val request = androidx.credentials.ClearCredentialStateRequest()
            credentialManager.clearCredentialState(request)
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-out failed", e)
        }
    }

    private fun handleSignIn(result: GetCredentialResponse): AuthResult {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val email = getEmailFromJwt(googleIdTokenCredential.idToken)
                        
                        if (email.isEmpty()) {
                            return AuthResult.Error("Could not extract email from token")
                        }
                        
                        return AuthResult.Success(
                            idToken = googleIdTokenCredential.idToken,
                            email = email,
                            displayName = googleIdTokenCredential.displayName ?: "User",
                            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("AuthManager", "Received an invalid google id token response", e)
                        return AuthResult.Error("Invalid token")
                    }
                }
            }
            else -> {
                Log.e("AuthManager", "Unexpected type of credential")
                return AuthResult.Error("Unexpected credential type")
            }
        }
        return AuthResult.Error("Unknown credential type")
    }

    private fun getEmailFromJwt(jwt: String): String {
        try {
            val parts = jwt.split(".")
            if (parts.size == 3) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                val jsonObject = org.json.JSONObject(payload)
                return jsonObject.optString("email", "")
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error parsing JWT", e)
        }
        return ""
    }
}

sealed class AuthResult {
    data class Success(val idToken: String, val email: String, val displayName: String, val photoUrl: String?) : AuthResult()
    data class Error(val message: String, val intent: android.content.Intent? = null) : AuthResult()
}
