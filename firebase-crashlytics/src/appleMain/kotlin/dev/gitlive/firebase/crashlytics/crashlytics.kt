package dev.gitlive.firebase.crashlytics

import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException

public actual val Firebase.crashlytics: FirebaseCrashlytics get() =
    FirebaseCrashlytics(FIRCrashlytics.crashlytics())

public actual fun Firebase.crashlytics(app: FirebaseApp): FirebaseCrashlytics = FirebaseCrashlytics(FIRCrashlytics.crashlytics())

public actual class FirebaseCrashlytics internal constructor(public val apple: FIRCrashlytics) {

    public actual fun recordException(exception: Throwable) {
        apple.recordError(exception.asNSError())
    }
    public actual fun log(message: String) {
        apple.log(message)
    }
    public actual fun setUserId(userId: String) {
        apple.setUserID(userId)
    }
    public actual fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        apple.setCrashlyticsCollectionEnabled(enabled)
    }
    public actual fun sendUnsentReports() {
        apple.sendUnsentReports()
    }
    public actual fun deleteUnsentReports() {
        apple.deleteUnsentReports()
    }
    public actual fun didCrashOnPreviousExecution(): Boolean = apple.didCrashDuringPreviousExecution()
    public actual fun setCustomKey(key: String, value: String) {
        apple.setCustomValue(value, key)
    }
    public actual fun setCustomKey(key: String, value: Boolean) {
        apple.setCustomValue(value.toString(), key)
    }
    public actual fun setCustomKey(key: String, value: Double) {
        apple.setCustomValue(value.toString(), key)
    }
    public actual fun setCustomKey(key: String, value: Float) {
        apple.setCustomValue(value.toString(), key)
    }
    public actual fun setCustomKey(key: String, value: Int) {
        apple.setCustomValue(value.toString(), key)
    }
    public actual fun setCustomKey(key: String, value: Long) {
        apple.setCustomValue(value.toString(), key)
    }

    @Suppress("UNCHECKED_CAST")
    public actual fun setCustomKeys(customKeys: Map<String, Any>) {
        apple.setCustomKeysAndValues(customKeys as Map<Any?, *>)
    }
}

public actual open class FirebaseCrashlyticsException internal constructor(message: String) : FirebaseException(message)

private fun Throwable.asNSError(): NSError {
    val userInfo = mutableMapOf<Any?, Any>()
    userInfo["KotlinException"] = this
    val message = message
    if (message != null) {
        userInfo[NSLocalizedDescriptionKey] = message
    }
    return NSError.errorWithDomain(this::class.qualifiedName, 0, userInfo)
}
