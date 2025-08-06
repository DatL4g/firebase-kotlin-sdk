package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.FIRActionCodeInfo
import cocoapods.FirebaseAuth.FIRActionCodeOperationEmailLink
import cocoapods.FirebaseAuth.FIRActionCodeOperationPasswordReset
import cocoapods.FirebaseAuth.FIRActionCodeOperationRecoverEmail
import cocoapods.FirebaseAuth.FIRActionCodeOperationRevertSecondFactorAddition
import cocoapods.FirebaseAuth.FIRActionCodeOperationUnknown
import cocoapods.FirebaseAuth.FIRActionCodeOperationVerifyAndChangeEmail
import cocoapods.FirebaseAuth.FIRActionCodeOperationVerifyEmail
import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRAuthDataResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.auth.ActionCodeResult.PasswordReset
import dev.gitlive.firebase.auth.ActionCodeResult.RecoverEmail
import dev.gitlive.firebase.auth.ActionCodeResult.RevertSecondFactorAddition
import dev.gitlive.firebase.auth.ActionCodeResult.SignInWithEmailLink
import dev.gitlive.firebase.auth.ActionCodeResult.VerifyBeforeChangeEmail
import dev.gitlive.firebase.auth.ActionCodeResult.VerifyEmail
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

public actual class FirebaseAuth internal constructor(public val tvos: FIRAuth) {

    public actual val currentUser: FirebaseUser?
        get() = tvos.currentUser()?.let { FirebaseUser(it) }

    public actual val authStateChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val handle = tvos.addAuthStateDidChangeListener { _, user -> trySend(user?.let { FirebaseUser(it) }) }
        awaitClose { tvos.removeAuthStateDidChangeListener(handle) }
    }

    public actual val idTokenChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val handle = tvos.addIDTokenDidChangeListener { _, user -> trySend(user?.let { FirebaseUser(it) }) }
        awaitClose { tvos.removeIDTokenDidChangeListener(handle) }
    }

    public actual var languageCode: String
        get() = tvos.languageCode() ?: ""
        set(value) {
            tvos.setLanguageCode(value)
        }

    public actual suspend fun applyActionCode(code: String): Unit = tvos.await { applyActionCode(code, it) }
    public actual suspend fun confirmPasswordReset(code: String, newPassword: String): Unit = tvos.await { confirmPasswordResetWithCode(code, newPassword, it) }

    public actual suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult = AuthResult(tvos.awaitResult { createUserWithEmail(email = email, password = password, completion = it) })

    @Suppress("UNCHECKED_CAST")
    public actual suspend fun fetchSignInMethodsForEmail(email: String): List<String> = tvos.awaitResult<FIRAuth, List<*>?> { fetchSignInMethodsForEmail(email, it) }.orEmpty() as List<String>

    public actual suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?) {
        tvos.await { actionCodeSettings?.let { actionSettings -> sendPasswordResetWithEmail(email, actionSettings.toIos(), it) } ?: sendPasswordResetWithEmail(email = email, completion = it) }
    }

    public actual suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings): Unit = tvos.await { sendSignInLinkToEmail(email, actionCodeSettings.toIos(), it) }

    public actual fun isSignInWithEmailLink(link: String): Boolean = tvos.isSignInWithEmailLink(link)

    public actual suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult = AuthResult(tvos.awaitResult { signInWithEmail(email = email, password = password, completion = it) })

    public actual suspend fun signInWithCustomToken(token: String): AuthResult = AuthResult(tvos.awaitResult { signInWithCustomToken(token, it) })

    public actual suspend fun signInAnonymously(): AuthResult = AuthResult(tvos.awaitResult { signInAnonymouslyWithCompletion(it) })

    public actual suspend fun signInWithCredential(authCredential: AuthCredential): AuthResult = AuthResult(tvos.awaitResult { signInWithCredential(authCredential.apple, it) })

    public actual suspend fun signInWithEmailLink(email: String, link: String): AuthResult = AuthResult(tvos.awaitResult { signInWithEmail(email = email, link = link, completion = it) })

    public actual suspend fun signOut(): Unit = tvos.throwError { signOut(it) }

    public actual suspend fun updateCurrentUser(user: FirebaseUser): Unit = tvos.await { updateCurrentUser(user.tvos, it) }
    public actual suspend fun verifyPasswordResetCode(code: String): String = tvos.awaitResult { verifyPasswordResetCode(code, it) }

    public actual suspend fun <T : ActionCodeResult> checkActionCode(code: String): T {
        val result: FIRActionCodeInfo = tvos.awaitResult { checkActionCode(code, it) }
        @Suppress("UNCHECKED_CAST")
        return when (result.operation()) {
            FIRActionCodeOperationEmailLink -> SignInWithEmailLink
            FIRActionCodeOperationVerifyEmail -> VerifyEmail(result.email())
            FIRActionCodeOperationPasswordReset -> PasswordReset(result.email())
            FIRActionCodeOperationRecoverEmail -> RecoverEmail(result.email(), result.previousEmail()!!)
            FIRActionCodeOperationVerifyAndChangeEmail -> VerifyBeforeChangeEmail(result.email(), result.previousEmail()!!)
            FIRActionCodeOperationRevertSecondFactorAddition -> RevertSecondFactorAddition(result.email(), null)
            FIRActionCodeOperationUnknown -> throw UnsupportedOperationException(result.operation().toString())
            else -> throw UnsupportedOperationException(result.operation().toString())
        } as T
    }

    public actual fun useEmulator(host: String, port: Int): Unit = tvos.useEmulatorWithHost(host, port.toLong())
}

public actual val Firebase.auth: FirebaseAuth
    get() = FirebaseAuth(FIRAuth.auth())

public actual fun Firebase.auth(app: FirebaseApp): FirebaseAuth = FirebaseAuth(
    FIRAuth.authWithApp(app.apple as objcnames.classes.FIRApp),
)

public actual class AuthResult(public val tvos: FIRAuthDataResult) {
    public actual val user: FirebaseUser?
        get() = FirebaseUser(tvos.user())
    public actual val credential: AuthCredential?
        get() = tvos.credential()?.let { AuthCredential(it) }
    public actual val additionalUserInfo: AdditionalUserInfo?
        get() = tvos.additionalUserInfo()?.let { AdditionalUserInfo(it) }
}
