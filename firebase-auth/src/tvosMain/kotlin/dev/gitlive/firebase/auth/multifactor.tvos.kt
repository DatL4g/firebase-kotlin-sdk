package dev.gitlive.firebase.auth

public actual class MultiFactor {
    public actual val enrolledFactors: List<MultiFactorInfo> = emptyList()
    public actual suspend fun enroll(multiFactorAssertion: MultiFactorAssertion, displayName: String?): Unit = error("MultiFactor is not supported on tvos")
    public actual suspend fun getSession(): MultiFactorSession = MultiFactorSession()
    public actual suspend fun unenroll(multiFactorInfo: MultiFactorInfo): Unit = error("MultiFactor is not supported on tvos")
    public actual suspend fun unenroll(factorUid: String): Unit = error("MultiFactor is not supported on tvos")
}

public actual class MultiFactorInfo {
    public actual val displayName: String?
        get() = error("MultiFactorInfo is not supported on tvos")
    public actual val enrollmentTime: Double
        get() = error("MultiFactorInfo is not supported on tvos")
    public actual val factorId: String
        get() = error("MultiFactorInfo is not supported on tvos")
    public actual val uid: String
        get() = error("MultiFactorInfo is not supported on tvos")
}

public actual class MultiFactorAssertion {
    public actual val factorId: String
        get() = error("MultiFactorAssertion is not supported on tvos")
}

public actual class MultiFactorSession

public actual class MultiFactorResolver {
    public actual val auth: FirebaseAuth = error("MultiFactorResolver is not supported on tvos")
    public actual val hints: List<MultiFactorInfo> = emptyList()
    public actual val session: MultiFactorSession = MultiFactorSession()

    public actual suspend fun resolveSignIn(assertion: MultiFactorAssertion): AuthResult = error("MultiFactorResolver is not supported on tvos")
}
