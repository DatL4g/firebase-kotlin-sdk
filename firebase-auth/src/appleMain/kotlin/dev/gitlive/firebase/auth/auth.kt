/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.*
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.FirebaseNetworkException
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSURL

public actual class AdditionalUserInfo(
    public val apple: FIRAdditionalUserInfo,
) {
    public actual val providerId: String?
        get() = apple.providerID()
    public actual val username: String?
        get() = apple.username()
    public actual val profile: Map<String, Any?>?
        get() = apple.profile()
            ?.mapNotNull { (key, value) ->
                if (key is NSString && value != null) {
                    key.toString() to value
                } else {
                    null
                }
            }
            ?.toMap()
    public actual val isNewUser: Boolean
        get() = apple.newUser()
}

public actual class AuthTokenResult(public val apple: FIRAuthTokenResult) {
//    actual val authTimestamp: Long
//        get() = ios.authDate
    public actual val claims: Map<String, Any>
        get() = apple.claims().map { it.key.toString() to it.value as Any }.toMap()

//    actual val expirationTimestamp: Long
//        get() = ios.expirationDate
//    actual val issuedAtTimestamp: Long
//        get() = ios.issuedAtDate
    public actual val signInProvider: String?
        get() = apple.signInProvider()
    public actual val token: String?
        get() = apple.token()
}

internal fun ActionCodeSettings.toIos() = FIRActionCodeSettings().also {
    it.setURL(NSURL.URLWithString(url))
    androidPackageName?.run { it.setAndroidPackageName(packageName, installIfNotAvailable, minimumVersion) }
    it.setHandleCodeInApp(canHandleCodeInApp)
    iOSBundleId?.run { it.setIOSBundleID(this) }
}

public actual open class FirebaseAuthException(message: String) : FirebaseException(message)
public actual open class FirebaseAuthActionCodeException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthEmailException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthInvalidCredentialsException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthWeakPasswordException(message: String) : FirebaseAuthInvalidCredentialsException(message)
public actual open class FirebaseAuthInvalidUserException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthMultiFactorException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthRecentLoginRequiredException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthUserCollisionException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthWebException(message: String) : FirebaseAuthException(message)

internal fun <T, R> T.throwError(block: T.(errorPointer: CPointer<ObjCObjectVar<NSError?>>) -> R): R {
    memScoped {
        val errorPointer: CPointer<ObjCObjectVar<NSError?>> = alloc<ObjCObjectVar<NSError?>>().ptr
        val result = block(errorPointer)
        val error: NSError? = errorPointer.pointed.value
        if (error != null) {
            throw error.toException()
        }
        return result
    }
}

internal suspend inline fun <T, reified R> T.awaitResult(function: T.(callback: (R?, NSError?) -> Unit) -> Unit): R {
    val job = CompletableDeferred<R?>()
    function { result, error ->
        if (error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    return job.await() as R
}

internal suspend inline fun <T> T.await(function: T.(callback: (NSError?) -> Unit) -> Unit) {
    val job = CompletableDeferred<Unit>()
    function { error ->
        if (error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    job.await()
}

private fun NSError.toException() = when (domain) {
    // codes from AuthErrors.swift: https://github.com/firebase/firebase-ios-sdk/blob/
    // 2f6ac4c2c61cd57c7ea727009e187b7e1163d613/FirebaseAuth/Sources/Swift/Utilities/
    // AuthErrors.swift#L51
    FIRAuthErrorDomain -> when (code) {
        17030L, // AuthErrorCode.invalidActionCode
        17029L, // AuthErrorCode.expiredActionCode
        -> FirebaseAuthActionCodeException(toString())

        17008L, // AuthErrorCode.invalidEmail
        -> FirebaseAuthEmailException(toString())

        17056L, // AuthErrorCode.captchaCheckFailed
        17042L, // AuthErrorCode.invalidPhoneNumber
        17041L, // AuthErrorCode.missingPhoneNumber
        17046L, // AuthErrorCode.invalidVerificationID
        17044L, // AuthErrorCode.invalidVerificationCode
        17045L, // AuthErrorCode.missingVerificationID
        17043L, // AuthErrorCode.missingVerificationCode
        17021L, // AuthErrorCode.userTokenExpired
        17004L, // AuthErrorCode.invalidCredential
        -> FirebaseAuthInvalidCredentialsException(toString())

        17026L, // AuthErrorCode.weakPassword
        -> FirebaseAuthWeakPasswordException(toString())

        17017L, // AuthErrorCode.invalidUserToken
        -> FirebaseAuthInvalidUserException(toString())

        17014L, // AuthErrorCode.requiresRecentLogin
        -> FirebaseAuthRecentLoginRequiredException(toString())

        17087L, // AuthErrorCode.secondFactorAlreadyEnrolled
        17078L, // AuthErrorCode.secondFactorRequired
        17088L, // AuthErrorCode.maximumSecondFactorCountExceeded
        17084L, // AuthErrorCode.multiFactorInfoNotFound
        -> FirebaseAuthMultiFactorException(toString())

        17007L, // AuthErrorCode.emailAlreadyInUse
        17012L, // AuthErrorCode.accountExistsWithDifferentCredential
        17025L, // AuthErrorCode.credentialAlreadyInUse
        -> FirebaseAuthUserCollisionException(toString())

        17057L, // AuthErrorCode.webContextAlreadyPresented
        17058L, // AuthErrorCode.webContextCancelled
        17062L, // AuthErrorCode.webInternalError
        -> FirebaseAuthWebException(toString())

        17020L, // AuthErrorCode.networkError
        -> FirebaseNetworkException(toString())

        else -> FirebaseAuthException(toString())
    }
    else -> FirebaseAuthException(toString())
}
