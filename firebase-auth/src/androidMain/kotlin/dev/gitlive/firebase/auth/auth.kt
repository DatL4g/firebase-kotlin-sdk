/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("android")

package dev.gitlive.firebase.auth

import com.google.firebase.auth.ActionCodeEmailInfo
import com.google.firebase.auth.ActionCodeMultiFactorInfo
import com.google.firebase.auth.ActionCodeResult.*
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

public actual val Firebase.auth: FirebaseAuth
    get() = FirebaseAuth(com.google.firebase.auth.FirebaseAuth.getInstance())

public actual fun Firebase.auth(app: FirebaseApp): FirebaseAuth = FirebaseAuth(com.google.firebase.auth.FirebaseAuth.getInstance(app.android))

public actual class FirebaseAuth internal constructor(internal val _android: com.google.firebase.auth.FirebaseAuth) {
    public val android: com.google.firebase.auth.FirebaseAuth
        get() = com.google.firebase.auth.FirebaseAuth.getInstance(_android.app)

    public actual val currentUser: FirebaseUser?
        get() = _android.currentUser?.let { FirebaseUser(it) }

    public actual val authStateChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val listener = AuthStateListener { auth -> trySend(auth.currentUser?.let { FirebaseUser(it) }) }
        _android.addAuthStateListener(listener)
        awaitClose { _android.removeAuthStateListener(listener) }
    }

    public actual val idTokenChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val listener = com.google.firebase.auth.FirebaseAuth.IdTokenListener { auth -> trySend(auth.currentUser?.let { FirebaseUser(it) }) }
        _android.addIdTokenListener(listener)
        awaitClose { _android.removeIdTokenListener(listener) }
    }

    public actual var languageCode: String
        get() = _android.languageCode ?: ""
        set(value) {
            _android.setLanguageCode(value)
        }

    public actual suspend fun applyActionCode(code: String) {
        _android.applyActionCode(code).await()
    }
    public actual suspend fun confirmPasswordReset(code: String, newPassword: String) {
        _android.confirmPasswordReset(code, newPassword).await()
    }

    public actual suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult = AuthResult(_android.createUserWithEmailAndPassword(email, password).await())

    @Suppress("DEPRECATION")
    public actual suspend fun fetchSignInMethodsForEmail(email: String): List<String> = _android.fetchSignInMethodsForEmail(email).await().signInMethods.orEmpty()

    public actual suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?) {
        _android.sendPasswordResetEmail(email, actionCodeSettings?.toAndroid()).await()
    }

    public actual suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings) {
        _android.sendSignInLinkToEmail(email, actionCodeSettings.toAndroid()).await()
    }

    public actual fun isSignInWithEmailLink(link: String): Boolean = _android.isSignInWithEmailLink(link)

    public actual suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult = AuthResult(_android.signInWithEmailAndPassword(email, password).await())

    public actual suspend fun signInWithCustomToken(token: String): AuthResult = AuthResult(_android.signInWithCustomToken(token).await())

    public actual suspend fun signInAnonymously(): AuthResult = AuthResult(_android.signInAnonymously().await())

    public actual suspend fun signInWithCredential(authCredential: AuthCredential): AuthResult = AuthResult(_android.signInWithCredential(authCredential.android).await())

    public actual suspend fun signInWithEmailLink(email: String, link: String): AuthResult = AuthResult(_android.signInWithEmailLink(email, link).await())

    public actual suspend fun signOut(): Unit = _android.signOut()

    public actual suspend fun updateCurrentUser(user: FirebaseUser) {
        _android.updateCurrentUser(user.android).await()
    }
    public actual suspend fun verifyPasswordResetCode(code: String): String = _android.verifyPasswordResetCode(code).await()

    public actual suspend fun <T : ActionCodeResult> checkActionCode(code: String): T {
        val result = _android.checkActionCode(code).await()
        @Suppress("UNCHECKED_CAST")
        return when (result.operation) {
            SIGN_IN_WITH_EMAIL_LINK -> ActionCodeResult.SignInWithEmailLink
            VERIFY_EMAIL -> ActionCodeResult.VerifyEmail(result.info!!.email)
            PASSWORD_RESET -> ActionCodeResult.PasswordReset(result.info!!.email)
            RECOVER_EMAIL -> (result.info as ActionCodeEmailInfo).run {
                ActionCodeResult.RecoverEmail(email, previousEmail)
            }
            VERIFY_BEFORE_CHANGE_EMAIL -> (result.info as ActionCodeEmailInfo).run {
                ActionCodeResult.VerifyBeforeChangeEmail(email, previousEmail)
            }
            REVERT_SECOND_FACTOR_ADDITION -> (result.info as ActionCodeMultiFactorInfo).run {
                ActionCodeResult.RevertSecondFactorAddition(email, MultiFactorInfo(multiFactorInfo))
            }
            ERROR -> throw UnsupportedOperationException(result.operation.toString())
            else -> throw UnsupportedOperationException(result.operation.toString())
        } as T
    }

    public actual fun useEmulator(host: String, port: Int): Unit = _android.useEmulator(host, port)
}

public actual class AuthResult(public val android: com.google.firebase.auth.AuthResult) {
    public actual val user: FirebaseUser?
        get() = android.user?.let { FirebaseUser(it) }
    public actual val credential: AuthCredential?
        get() = android.credential?.let { AuthCredential(it) }
    public actual val additionalUserInfo: AdditionalUserInfo?
        get() = android.additionalUserInfo?.let { AdditionalUserInfo(it) }
}

public actual class AdditionalUserInfo(
    public val android: com.google.firebase.auth.AdditionalUserInfo,
) {
    public actual val providerId: String?
        get() = android.providerId
    public actual val username: String?
        get() = android.username
    public actual val profile: Map<String, Any?>?
        get() = android.profile
    public actual val isNewUser: Boolean
        get() = android.isNewUser
}

public actual class AuthTokenResult(public val android: com.google.firebase.auth.GetTokenResult) {
//    actual val authTimestamp: Long
//        get() = android.authTimestamp
    public actual val claims: Map<String, Any>
        get() = android.claims

//    actual val expirationTimestamp: Long
//        get() = android.expirationTimestamp
//    actual val issuedAtTimestamp: Long
//        get() = android.issuedAtTimestamp
    public actual val signInProvider: String?
        get() = android.signInProvider
    public actual val token: String?
        get() = android.token
}

internal fun ActionCodeSettings.toAndroid() = com.google.firebase.auth.ActionCodeSettings.newBuilder()
    .setUrl(url)
    .also { androidPackageName?.run { it.setAndroidPackageName(packageName, installIfNotAvailable, minimumVersion) } }
    .also { dynamicLinkDomain?.run { it.setDynamicLinkDomain(this) } }
    .setHandleCodeInApp(canHandleCodeInApp)
    .also { iOSBundleId?.run { it.iosBundleId = this } }
    .build()

public actual typealias FirebaseAuthException = com.google.firebase.auth.FirebaseAuthException
public actual typealias FirebaseAuthActionCodeException = com.google.firebase.auth.FirebaseAuthActionCodeException
public actual typealias FirebaseAuthEmailException = com.google.firebase.auth.FirebaseAuthEmailException
public actual typealias FirebaseAuthInvalidCredentialsException = com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
public actual typealias FirebaseAuthWeakPasswordException = com.google.firebase.auth.FirebaseAuthWeakPasswordException
public actual typealias FirebaseAuthInvalidUserException = com.google.firebase.auth.FirebaseAuthInvalidUserException
public actual typealias FirebaseAuthMultiFactorException = com.google.firebase.auth.FirebaseAuthMultiFactorException
public actual typealias FirebaseAuthRecentLoginRequiredException = com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
public actual typealias FirebaseAuthUserCollisionException = com.google.firebase.auth.FirebaseAuthUserCollisionException
public actual typealias FirebaseAuthWebException = com.google.firebase.auth.FirebaseAuthWebException
