package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.FIRAuthUIDelegateProtocol
import cocoapods.FirebaseAuth.FIROAuthProvider
import cocoapods.FirebaseAuth.FIRPhoneAuthProvider

public actual class PhoneAuthProvider(public val tvos: FIRPhoneAuthProvider) {

    public actual constructor(auth: FirebaseAuth) : this(tvos = error("PhoneAuthProvider is not supported on tvos"))

    public actual fun credential(verificationId: String, smsCode: String): PhoneAuthCredential = error("PhoneAuthProvider is not supported on tvos")

    public actual suspend fun verifyPhoneNumber(
        phoneNumber: String,
        verificationProvider: PhoneVerificationProvider,
    ): AuthCredential = error("PhoneAuthProvider is not supported on tvos")
}

public actual interface PhoneVerificationProvider {
    public val delegate: FIRAuthUIDelegateProtocol?
    public suspend fun getVerificationCode(): String
}

public actual class OAuthProvider(public val tvos: FIROAuthProvider) {

    public actual constructor(
        provider: String,
        scopes: List<String>,
        customParameters: Map<String, String>,
        auth: FirebaseAuth,
    ) : this(FIROAuthProvider.providerWithProviderID(provider, auth.tvos)) {
        tvos.setScopes(scopes)
        @Suppress("UNCHECKED_CAST")
        tvos.setCustomParameters(customParameters as Map<Any?, *>)
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
