package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.FIROAuthProvider
import cocoapods.FirebaseAuth.FIRPhoneAuthProvider

public actual class PhoneAuthProvider(public val ios: FIRPhoneAuthProvider) {

    public actual constructor(auth: FirebaseAuth) : this(ios = error("PhoneAuthProvider is not supported on macos"))

    public actual fun credential(verificationId: String, smsCode: String): PhoneAuthCredential = error("PhoneAuthProvider is not supported on macos")

    public actual suspend fun verifyPhoneNumber(
        phoneNumber: String,
        verificationProvider: PhoneVerificationProvider,
    ): AuthCredential = error("PhoneAuthProvider is not supported on macos")
}

public actual interface PhoneVerificationProvider {
    public suspend fun getVerificationCode(): String
}

public actual class OAuthProvider(public val ios: FIROAuthProvider) {

    public actual constructor(
        provider: String,
        scopes: List<String>,
        customParameters: Map<String, String>,
        auth: FirebaseAuth,
    ) : this(FIROAuthProvider.providerWithProviderID(provider, auth.ios)) {
        ios.setScopes(scopes)
        @Suppress("UNCHECKED_CAST")
        ios.setCustomParameters(customParameters as Map<Any?, *>)
    }

    public actual companion object {
        public actual fun credential(providerId: String, accessToken: String?, idToken: String?, rawNonce: String?): OAuthCredential {
            val credential = when {
                idToken == null -> FIROAuthProvider.credentialWithProviderID(providerID = providerId, accessToken = accessToken!!)
                accessToken == null -> FIROAuthProvider.credentialWithProviderID(providerID = providerId, IDToken = idToken, rawNonce = rawNonce!!)
                rawNonce == null -> FIROAuthProvider.credentialWithProviderID(providerID = providerId, IDToken = idToken, accessToken = accessToken)
                else -> FIROAuthProvider.credentialWithProviderID(providerID = providerId, IDToken = idToken, rawNonce = rawNonce, accessToken = accessToken)
            }
            return OAuthCredential(credential)
        }
    }
}
