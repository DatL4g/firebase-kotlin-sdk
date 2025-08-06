/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.*

public actual open class AuthCredential(public open val ios: FIRAuthCredential) {
    public actual val providerId: String
        get() = ios.provider()
}

public actual class PhoneAuthCredential(override val ios: FIRPhoneAuthCredential) : AuthCredential(ios)
public actual class OAuthCredential(override val ios: FIROAuthCredential) : AuthCredential(ios)

public actual object EmailAuthProvider {
    public actual fun credential(
        email: String,
        password: String,
    ): AuthCredential = AuthCredential(FIREmailAuthProvider.credentialWithEmail(email = email, password = password))

    public actual fun credentialWithLink(
        email: String,
        emailLink: String,
    ): AuthCredential = AuthCredential(FIREmailAuthProvider.credentialWithEmail(email = email, link = emailLink))
}

public actual object FacebookAuthProvider {
    public actual fun credential(accessToken: String): AuthCredential = AuthCredential(FIRFacebookAuthProvider.credentialWithAccessToken(accessToken))
}

public actual object GithubAuthProvider {
    public actual fun credential(token: String): AuthCredential = AuthCredential(FIRGitHubAuthProvider.credentialWithToken(token))
}

public actual object GoogleAuthProvider {
    public actual fun credential(idToken: String?, accessToken: String?): AuthCredential {
        requireNotNull(idToken) { "idToken must not be null" }
        requireNotNull(accessToken) { "accessToken must not be null" }
        return AuthCredential(FIRGoogleAuthProvider.credentialWithIDToken(idToken, accessToken))
    }
}

public actual object TwitterAuthProvider {
    public actual fun credential(token: String, secret: String): AuthCredential = AuthCredential(FIRTwitterAuthProvider.credentialWithToken(token, secret))
}
