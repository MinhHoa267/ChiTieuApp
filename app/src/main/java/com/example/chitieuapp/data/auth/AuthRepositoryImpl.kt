package com.example.chitieuapp.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.chitieuapp.R
import com.example.chitieuapp.data.local.AppDatabase
import com.example.chitieuapp.data.repository.AuthState
import com.example.chitieuapp.data.repository.AuthUser
import com.example.chitieuapp.data.sync.SyncManager
import com.example.chitieuapp.data.sync.SyncScheduler
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit>
    suspend fun signOut()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val database: AppDatabase,
    private val syncManager: SyncManager,
    private val scheduler: SyncScheduler,
) : AuthRepository {

    override val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser.toAuthState())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    /** Credential Manager (bottom sheet chọn tài khoản Google) -> Firebase Auth. */
    override suspend fun signInWithGoogle(activityContext: Context): Result<Unit> = try {
        val option = GetSignInWithGoogleOption.Builder(
            appContext.getString(R.string.default_web_client_id)
        ).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = credentialManager.getCredential(activityContext, request).credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(googleToken, null)).await()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Loại tài khoản không được hỗ trợ"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun signOut() {

        runCatching { syncManager.sync() }
        scheduler.cancelAll()
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            File(appContext.filesDir, "receipts").deleteRecursively()
        }
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        auth.signOut()
    }

    private fun FirebaseUser?.toAuthState(): AuthState =
        if (this == null) {
            AuthState.SignedOut
        } else {
            AuthState.SignedIn(AuthUser(uid, displayName, email, photoUrl?.toString()))
        }
}
