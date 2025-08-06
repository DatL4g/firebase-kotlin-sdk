package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIRUser
import cocoapods.FirebaseAuth.FIRUserInfoProtocol
import platform.Foundation.NSURL

public actual class FirebaseUser internal constructor(public val macos: FIRUser) {
    public actual val uid: String
        get() = macos.uid()
    public actual val displayName: String?
        get() = macos.displayName()
    public actual val email: String?
        get() = macos.email()
    public actual val phoneNumber: String?
        get() = macos.phoneNumber()
    public actual val photoURL: String?
        get() = macos.photoURL()?.absoluteString
    public actual val isAnonymous: Boolean
        get() = macos.anonymous()
    public actual val isEmailVerified: Boolean
        get() = macos.emailVerified()
    public actual val metaData: UserMetaData?
        get() = UserMetaData(macos.metadata())
    public actual val multiFactor: MultiFactor
        get() = MultiFactor()
    public actual val providerData: List<UserInfo>
        get() = macos.providerData().mapNotNull { provider -> (provider as? FIRUserInfoProtocol)?.let { UserInfo(it) } }
    public actual val providerId: String
        get() = macos.providerID()

    public actual suspend fun delete(): Unit = macos.await { deleteWithCompletion(it) }

    public actual suspend fun reload(): Unit = macos.await { reloadWithCompletion(it) }

    public actual suspend fun getIdToken(forceRefresh: Boolean): String? = macos.awaitResult { getIDTokenForcingRefresh(forceRefresh, it) }

    public actual suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult = AuthTokenResult(macos.awaitResult { getIDTokenResultForcingRefresh(forceRefresh, it) })

    public actual suspend fun linkWithCredential(credential: AuthCredential): AuthResult = AuthResult(macos.awaitResult { linkWithCredential(credential.apple, it) })

    public actual suspend fun reauthenticate(credential: AuthCredential) {
        macos.awaitResult<FIRUser, FIRAuthDataResult?> { reauthenticateWithCredential(credential.apple, it) }
    }

    public actual suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult = AuthResult(macos.awaitResult { reauthenticateWithCredential(credential.apple, it) })

    public actual suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?): Unit = macos.await {
        actionCodeSettings?.let { settings -> sendEmailVerificationWithActionCodeSettings(settings.toIos(), it) }
            ?: sendEmailVerificationWithCompletion(it)
    }

    public actual suspend fun unlink(provider: String): FirebaseUser? {
        val user: FIRUser? = macos.awaitResult { unlinkFromProvider(provider, it) }
        return user?.let {
            FirebaseUser(it)
        }
    }
    public actual suspend fun updateEmail(email: String): Unit = macos.await { updateEmail(email, it) }
    public actual suspend fun updatePassword(password: String): Unit = macos.await { updatePassword(password, it) }
    public actual suspend fun updatePhoneNumber(credential: PhoneAuthCredential): Unit = error("PhoneAuthCredential not supported on macos")
    public actual suspend fun updateProfile(displayName: String?, photoUrl: String?) {
        val request = macos.profileChangeRequest()
            .apply { setDisplayName(displayName) }
            .apply { setPhotoURL(photoUrl?.let { NSURL.URLWithString(it) }) }
        macos.await { request.commitChangesWithCompletion(it) }
    }
    public actual suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?): Unit = macos.await {
        actionCodeSettings?.let { actionSettings -> sendEmailVerificationBeforeUpdatingEmail(newEmail, actionSettings.toIos(), it) } ?: sendEmailVerificationBeforeUpdatingEmail(newEmail, it)
    }
}
