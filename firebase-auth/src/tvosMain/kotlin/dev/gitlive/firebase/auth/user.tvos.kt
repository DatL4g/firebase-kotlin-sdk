package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIRUser
import cocoapods.FirebaseAuth.FIRUserInfoProtocol
import platform.Foundation.NSURL

public actual class FirebaseUser internal constructor(public val tvos: FIRUser) {
    public actual val uid: String
        get() = tvos.uid()
    public actual val displayName: String?
        get() = tvos.displayName()
    public actual val email: String?
        get() = tvos.email()
    public actual val phoneNumber: String?
        get() = tvos.phoneNumber()
    public actual val photoURL: String?
        get() = tvos.photoURL()?.absoluteString
    public actual val isAnonymous: Boolean
        get() = tvos.anonymous()
    public actual val isEmailVerified: Boolean
        get() = tvos.emailVerified()
    public actual val metaData: UserMetaData?
        get() = UserMetaData(tvos.metadata())
    public actual val multiFactor: MultiFactor
        get() = MultiFactor()
    public actual val providerData: List<UserInfo>
        get() = tvos.providerData().mapNotNull { provider -> (provider as? FIRUserInfoProtocol)?.let { UserInfo(it) } }
    public actual val providerId: String
        get() = tvos.providerID()

    public actual suspend fun delete(): Unit = tvos.await { deleteWithCompletion(it) }

    public actual suspend fun reload(): Unit = tvos.await { reloadWithCompletion(it) }

    public actual suspend fun getIdToken(forceRefresh: Boolean): String? = tvos.awaitResult { getIDTokenForcingRefresh(forceRefresh, it) }

    public actual suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult = AuthTokenResult(tvos.awaitResult { getIDTokenResultForcingRefresh(forceRefresh, it) })

    public actual suspend fun linkWithCredential(credential: AuthCredential): AuthResult = AuthResult(tvos.awaitResult { linkWithCredential(credential.apple, it) })

    public actual suspend fun reauthenticate(credential: AuthCredential) {
        tvos.awaitResult<FIRUser, FIRAuthDataResult?> { reauthenticateWithCredential(credential.apple, it) }
    }

    public actual suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult = AuthResult(tvos.awaitResult { reauthenticateWithCredential(credential.apple, it) })

    public actual suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?): Unit = tvos.await {
        actionCodeSettings?.let { settings -> sendEmailVerificationWithActionCodeSettings(settings.toIos(), it) }
            ?: sendEmailVerificationWithCompletion(it)
    }

    public actual suspend fun unlink(provider: String): FirebaseUser? {
        val user: FIRUser? = tvos.awaitResult { unlinkFromProvider(provider, it) }
        return user?.let {
            FirebaseUser(it)
        }
    }
    public actual suspend fun updateEmail(email: String): Unit = tvos.await { updateEmail(email, it) }
    public actual suspend fun updatePassword(password: String): Unit = tvos.await { updatePassword(password, it) }
    public actual suspend fun updatePhoneNumber(credential: PhoneAuthCredential): Unit = error("PhoneAuthCredential not supported on tvos")
    public actual suspend fun updateProfile(displayName: String?, photoUrl: String?) {
        val request = tvos.profileChangeRequest()
            .apply { setDisplayName(displayName) }
            .apply { setPhotoURL(photoUrl?.let { NSURL.URLWithString(it) }) }
        tvos.await { request.commitChangesWithCompletion(it) }
    }
    public actual suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?): Unit = tvos.await {
        actionCodeSettings?.let { actionSettings -> sendEmailVerificationBeforeUpdatingEmail(newEmail, actionSettings.toIos(), it) } ?: sendEmailVerificationBeforeUpdatingEmail(newEmail, it)
    }
}
